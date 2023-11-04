import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileNotFoundException;
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

    private static void debug(ResultSet rs) throws SQLException {
        ResultSetMetaData rsMetaData = rs.getMetaData();
        System.out.println("List of column names in the current table: ");
        //Retrieving the list of column names
        int count = rsMetaData.getColumnCount();
        for(int i = 1; i<=count; i++) {
            System.out.println(rsMetaData.getColumnName(i));
        }
    }

    // APPROVED
    private static boolean exist(String username) {
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

    // APPROVED
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

    // APPROVED
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

    // APPROVED
    public static boolean createCollection(Collection collection) {
        int uid = collection.get_userid();
        String name = collection.get_collectionname();
        System.out.printf("%d %s\n", uid, name);

        String sql = String.format("""
            INSERT INTO 
                COLLECTIONS (collection_name, user_id)
                VALUES ('%s', '%s')
        """, name, uid);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static void listCollection(User user) {
        int uid = user.get_id();

        String sql = String.format("""
            SELECT
                COLLECTION_NAME,
                COUNT(COLLECTIONSONG.SONG_ID) AS number_of_songs,
                COALESCE(SUM(SONGS.SONG_LENGTH), 0) / 60 AS total_duration
            FROM COLLECTIONS
            LEFT JOIN COLLECTIONSONG ON COLLECTIONS.COLLECTION_ID = COLLECTIONSONG.COLLECTION_ID
            LEFT JOIN SONGS ON COLLECTIONSONG.SONG_ID = SONGS.SONG_ID
            WHERE USER_ID = '%d'
            GROUP BY COLLECTION_NAME
            ORDER BY COLLECTION_NAME ASC;
        """, uid);
        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String name = rs.getString("collection_name");
                System.out.println(name);
                System.out.println("Number of Songs: " + rs.getString("number_of_songs"));
                System.out.println("Total Duration: " + rs.getString("total_duration"));
                System.out.println("");
            }
        } catch (SQLException ex) {}
    }

    // APPROVED
    public static boolean searchSongName(Song song) {
        String sn = song.get_title();

        String sql = String.format("""
            SELECT 
                SONGS.SONG_TITLE,
                ARTISTS.ARTIST_NAME as "artist_name",
                ALBUMS.ALBUM_NAME,
                SONGS.SONG_LENGTH,
                SONGS.RELEASE_DATE
            FROM SONGS
            JOIN SONGARTIST ON SONGS.SONG_ID = SONGARTIST.SONG_ID
            JOIN ARTISTS ON SONGARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
            JOIN SONGALBUM ON SONGS.SONG_ID = SONGALBUM.SONG_ID
            JOIN ALBUMS ON SONGALBUM.ALBUM_ID = ALBUMS.ALBUM_ID
            WHERE SONGS.SONG_TITLE = '%s'
        """, sn);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("The song is sang by the following artists. Which one are you referring to: ");
                do {
                    System.out.println(rs.getString("artist_name"));
                } while (rs.next());
                return true;
            } else {
                System.out.println("The song does not exist.");
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean searchSongArtist(Artist artist) {
        String singer = artist.get_artist();
        String sql = String.format("""
            SELECT 
                ARTISTS.ARTIST_NAME,
                SONGS.SONG_TITLE as "song_title",
                ALBUMS.ALBUM_NAME,
                SONGS.SONG_LENGTH,
                SONGS.RELEASE_DATE
            FROM ARTISTS
            JOIN SONGARTIST ON SONGS.SONG_ID = SONGARTIST.SONG_ID
            JOIN ARTISTS ON SONGARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
            JOIN SONGALBUM ON SONGS.SONG_ID = SONGALBUM.SONG_ID
            JOIN ALBUMS ON SONGALBUM.ALBUM_ID = ALBUMS.ALBUM_ID
            WHERE ARTISTS.ARTIST_NAME = '%s'
        """, singer);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("The song is sang by the following artists. Which one are you referring to: ");
                do {
                    System.out.println(rs.getString("song_title"));
                } while (rs.next());
                return true;
            } else {
                System.out.println("The song does not exist.");
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    public static boolean searchSongAlbum(Album album) {
        String name = album.get_albumname();

        String sql = String.format("""
                    SELECT 
                        ARTISTS.ARTIST_NAME,
                        SONGS.SONG_TITLE as "song_title",
                        ALBUMS.ALBUM_NAME,
                        SONGS.SONG_LENGTH,
                        SONGS.RELEASE_DATE
                    FROM ALBUMS
                    JOIN SONGARTIST ON SONGS.SONG_ID = SONGARTIST.SONG_ID
                    JOIN ARTISTS ON SONGARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
                    JOIN SONGALBUM ON SONGS.SONG_ID = SONGALBUM.SONG_ID
                    JOIN ALBUMS ON SONGALBUM.ALBUM_ID = ALBUMS.ALBUM_ID
                    WHERE ALBUMS.ALBUM_NAME = '%s'
                """, name);

                try {
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);
                    if (rs.next()) {
                        System.out.println("The song is sang by the following artists. Which one are you referring to: ");
                        do {
                            System.out.println(rs.getString("song_title"));
                        } while (rs.next());
                        return true;
                    } else {
                        System.out.println("The song does not exist.");
                    }
                } catch (SQLException ex) {
                    System.out.println(ex);
                }
                return false;
    }

    public static boolean searchSongGenre(Genre genre) {
        String type = genre.get_genre();
        String sql = String.format("""
            SELECT 
                ARTISTS.ARTIST_NAME,
                SONGS.SONG_TITLE as "song_title",
                ALBUMS.ALBUM_NAME,
                SONGS.SONG_LENGTH,
                SONGS.RELEASE_DATE
            FROM GENRES
            JOIN SONGGENRE ON SONGGENRE.GENRE_ID = GENRES.GENRE_ID
            JOIN SONGGENRE.SONG_ID = SONG.SONG_ID
            JOIN SONGARTIST ON SONGS.SONG_ID = SONGARTIST.SONG_ID
            JOIN ARTISTS ON SONGARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
            JOIN SONGALBUM ON SONGS.SONG_ID = SONGALBUM.SONG_ID
            JOIN ALBUMS ON SONGALBUM.ALBUM_ID = ALBUMS.ALBUM_ID
            WHERE ALBUMS.ALBUM_NAME = '%s'
        """, type);
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
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