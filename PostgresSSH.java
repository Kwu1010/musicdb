import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.CookieHandler;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

import Model.*;

public class PostgresSSH {
    private static final int lport = 5432;
    private static final String rhost = "starbug.cs.rit.edu";
    private static final String driverName = "org.postgresql.Driver";
    private static final int rport = 5432;
    private static final String databaseName = "p320_19"; //change to your database name
    private static File credentialsFile = new File("Credentials");
    private static Connection conn;
    private static Scanner credentials;
    private static Session session;

    public static void connect_to_database() throws FileNotFoundException {
        credentials = new Scanner(credentialsFile);
        String user = credentials.nextLine(); // store username in first line of Credentials
        String password = credentials.nextLine(); // store password in second line of Credentials
        credentials.close();
        conn = null;
        session = null;

        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            session = jsch.getSession(user, rhost, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.setConfig("PreferredAuthentications","publickey,keyboard-interactive,password");
            session.connect();
            // System.out.println("Connected");
            int assigned_port = session.setPortForwardingL(lport, "127.0.0.1", rport);
            // System.out.println("Port Forwarded");

            // Assigned port could be different from 5432 but rarely happens
            String url = "jdbc:postgresql://127.0.0.1:"+ assigned_port + "/" + databaseName;

            // System.out.println("database Url: " + url);
            Properties props = new Properties();
            props.put("user", user);
            props.put("password", password);

            Class.forName(driverName);
            conn = DriverManager.getConnection(url, props);
            // System.out.println("Database connection established");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void close_connection() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            // System.out.println("Closing Database Connection");
            conn.close();
        }
        if (session != null && session.isConnected()) {
            // System.out.println("Closing SSH Connection");
            session.disconnect();
        }
    }

    public static boolean exist(String username) {
        String sql = String.format("""
            SELECT username FROM users
            WHERE username = '%s'
        """, username);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            return false;
        }
        return false;
    }

    public static boolean addUser(User user) {
        String un = user.get_username();
        String pass = user.get_password();
        String fn = user.get_first_name();
        String ln = user.get_last_name();
        String email = user.get_email();
        String cd = user.get_creation_date();
        String lad = user.get_last_access_date();

        if (exist(un)) {
            System.out.println("EXIST");
            return false;
        }

        String sql = String.format("""
            INSERT INTO
                USERS (
                    username, password, first_name, last_name, email, creation_date, last_access_date
                ) VALUES (
                    '%s', '%s', '%s', '%s', '%s', '%s', '%s'
                )
        """, un, pass, fn, ln, email, cd, lad);

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sql);
        } catch (SQLException ex) {}
        return true;
    }

    public static User findUser(User user) {
        String un = user.get_username();
        String pass = user.get_password();
        String sql = String.format("""
        SELECT * FROM users
        WHERE username = '%s' AND password = '%s'
        """, un, pass);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return new User(
                    rs.getString("USERNAME"),
                    rs.getString("PASSWORD"),
                    rs.getString("FIRST_NAME"),
                    rs.getString("LAST_NAME"),
                    rs.getString("EMAIL"),
                    rs.getInt("USER_ID"),
                    rs.getString("CREATION_DATE")
                );
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return null;
    }

    public static boolean createCollection(Collection collection) {
        int cid = collection.get_id();
        int uid = collection.get_userid();
        String name = collection.get_collectionname();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO collections (collection_name, user_id) VALUES ");
        sb.append("('" + name + "', " + uid + ");");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static void listCollection(User user) {
        int uid = user.get_id();

        String sb = String.format("""
                    SELECT
                        COLLECTIONS.COLLECTION_NAME AS "Collection Name",
                        COUNT(COLLECTIONSONG.SONG_ID) AS "Number of Songs",
                        COALESCE(SUM(SONGS.SONG_LENGTH), 0) / 60 AS "Total Duration (minutes)"
                    FROM COLLECTIONS
                    LEFT JOIN COLLECTIONSONG ON COLLECTIONS.COLLECTION_ID = COLLECTIONSONG.COLLECTION_ID
                    LEFT JOIN SONGS ON COLLECTIONSONG.SONG_ID = SONGS.SONG_ID
                    WHERE COLLECTIONS.USER_ID = %d
                    GROUP BY COLLECTIONS.COLLECTION_NAME
                    ORDER BY COLLECTIONS.COLLECTION_NAME ASC;
                    """, uid);
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            String name = rs.getString("collection_name\n");
            while (rs.next()){
                name += "Number of Songs: " + rs.getString("number_of_songs") + "\n";
                name += "Total Duration: " + rs.getString("total_duration") + "\n";
            }
            System.out.println(name);

        } catch (SQLException ex) {
            System.out.println(ex);
        }
        // int uid = user.get_id();

        // StringBuilder sb = new StringBuilder();
        // sb.append("SELECT * FROM (SELECT collections.collection_name, COUNT(*) AS ");
        // sb.append("number_of_songs, SUM(songs.song_length) AS total_duration FROM ");
        // sb.append("collectionsong INNER JOIN collections ON ");
        // sb.append("collectionsong.collection_id = collections.collection_id INNER ");
        // sb.append("JOIN songs ON collectionsong.song_id = songs.song_id WHERE user_id = ");
        // sb.append("" + uid + " GROUP BY collections.collection_id) AS subquery ");
        // sb.append("ORDER BY subquery.collection_name ASC;");
       
        // collection.get_collection();
    }

    public static boolean searchSongName(Song song) {
        String sn = song.get_title();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT song_title FROM songs WHERE song_title = '");
        sb.append(sb + "';");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean searchSongArtist(Artist artist) {
        String singer = artist.get_artist();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT s.song_title FROM songartist AS r INNER JOIN songs ");
        sb.append("AS s ON r.song_id = s.song_id INNER JOIN artists ON ");
        sb.append("r.artist_id = artists.artist_id WHERE artist_name LIKE ");
        sb.append("'" + singer + "';");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean searchSongAlbum(Album album) {
        String name = album.get_albumname();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT s.song_title FROM songalbum AS a INNER JOIN songs ");
        sb.append("AS s ON a.song_id = s.song_id INNER JOIN albums ON ");
        sb.append("a.album_id = albums.album_id WHERE album_name LIKE ");
        sb.append("'" + name + "';");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean searchSongGenre(Genre genre) {
        String type = genre.get_genre();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT s.song_title FROM songgenre AS g INNER JOIN songs ");
        sb.append("AS s ON g.song_id = s.song_id INNER JOIN genres ON ");
        sb.append("g.genre_id = genres.genre_id WHERE type LIKE ");
        sb.append("'" + type + "';");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean insertSong(Collection collection, Song song) {
        int cid = collection.get_id();
        int sid = song.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO collectionsong WHERE collection_id = ");
        sb.append(cid + " AND song_id = " + sid + ";");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean deleteSong(Collection collection, Song song) {
        int cid = collection.get_id();
        int sid = song.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM collectionsong WHERE collection_id = ");
        sb.append(cid + " AND song_id = " + sid + ";");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean insertAlbum(Collection collection, Album album) {
        int cid = collection.get_id();
        int aid = album.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO collectionalbum WHERE collection_id = ");
        sb.append(cid + " AND song_id = " + aid + ";");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean deleteAlbum(Collection collection, Album album) {
        int cid = collection.get_id();
        int aid = album.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM collectionalbum WHERE collection_id = '");
        sb.append(cid + "' AND song_id = " + aid + ";");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean updateCollectionName(Collection collection, User user) {
        int cid = collection.get_id();
        int uid = user.get_id();
        String name = collection.get_collectionname();
        
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE collections SET collection_name = '" + name + "' WHERE ");
        sb.append("collection_id = " + cid + " AND user_id = " + uid + ";");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean deleteCollection(Collection collection, User user) {
        int cid = collection.get_id();
        int uid = user.get_id();
        String name = collection.get_collectionname();
        
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM collections WHERE collection_id = " + cid + " AND ");
        sb.append("user_id = " + uid + ";");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean listenHistory(User user, Song song) {
        int uid = user.get_id();
        int sid = song.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO listenhistory (user_id, song_id) VALUES (" + uid + ", ");
        sb.append(sid + ");");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean follow(User user, User follower) {
        int uid = user.get_id();
        int fid = follower.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO followers (follower_id, followee_id) VALUES (" + uid + ", ");
        sb.append(fid + ");");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean searchEmail(User user) {
        String email = user.get_email();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT user_id, username FROM users WHERE email = '");
        sb.append(email + "');");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean unfollow(User user, User follower) {
        int uid = user.get_id();
        int fid = follower.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM followers WHERE follower_id = " + uid + " AND followee_id = ");
        sb.append(fid + ";");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }
}