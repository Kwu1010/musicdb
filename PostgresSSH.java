import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.CookieHandler;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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

    public static boolean addUser(User user){
        String un = user.get_username();
        String pass = user.get_password();
        String fn = user.get_first_name();
        String ln = user.get_last_name();
        String email = user.get_email();
        String cd = user.get_creation_date();
        String lad = user.get_last_access_date();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO users (username, password, first_name, ");
        sb.append("last_name, email, creation_date, last_access_date) VALUES (");
        sb.append("'" + un + "', ");
        sb.append("'" + pass + "', ");
        sb.append("'" + fn + "', ");
        sb.append("'" + ln + "' ,");
        sb.append("'" + email + "', ");
        sb.append("'" + cd + "', ");
        sb.append("'" + lad + "');");
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sb.toString());
        } catch (SQLException ex) {
            return false;
        }
        return true;
    }

    public static boolean findUser(User user){
        int uid = user.get_id();
        String un = user.get_username();
        String pass = user.get_password();
        String lad = user.get_last_access_date();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT user_id, username, FROM users WHERE username = ");
        sb.append("'" + un + "'" + " AND password = ");
        sb.append("'" + pass + "'" + "\n");
        sb.append("UPDATE users SET last_access_date = ");
        sb.append("'" + lad + "'" + " WHERE user_id = ");
        sb.append(uid + ";");
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
            if (rs.next()){
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static void listCollection(User user) {
        int uid = user.get_id();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM (SELECT collections.collection_name, COUNT(*) AS ");
        sb.append("number_of_songs, SUM(songs.song_length) AS total_duration FROM ");
        sb.append("collectionsong INNER JOIN collections ON ");
        sb.append("collectionsong.collection_id = collections.collection_id INNER ");
        sb.append("JOIN songs ON collectionsong.song_id = songs.song_id WHERE user_id = ");
        sb.append("" + uid + " GROUP BY collections.collection_id) AS subquery ");
        sb.append("ORDER BY subquery.collection_name ASC;");
       
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

    public static boolean follow(User user) {
        int uid = user.get_id();
        int idk = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO followers (follower_id, followee_id) VALUES (" + uid + ", ");
        sb.append(idk + ");");
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

    public static boolean unfollow(User user) {
        int uid = user.get_id();
        int idk = 0;

        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM followers WHERE follower_id = " + uid + " AND followee_id = ");
        sb.append(idk + ";");
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