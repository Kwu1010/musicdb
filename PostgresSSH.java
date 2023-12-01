import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import Model.*;

public class PostgresSSH {
    private static final int lport = 5432;
    private static final String rhost = "starbug.cs.rit.edu";
    private static final String driverName = "org.postgresql.Driver";
    private static final int rport = 5432;
    private static final String databaseName = "p320_19"; // change to your database name
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
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.connect();
            // System.out.println("Connected");
            int assigned_port = session.setPortForwardingL(lport, "127.0.0.1", rport);
            // System.out.println("Port Forwarded");

            // Assigned port could be different from 5432 but rarely happens
            String url = "jdbc:postgresql://127.0.0.1:" + assigned_port + "/" + databaseName;

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
        // Retrieving the list of column names
        int count = rsMetaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
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
            INSERT INTO USERS (
                username, password, first_name, last_name, email, creation_date, last_access_date
            ) VALUES (
                '%s', '%s', '%s', '%s', '%s', '%s', '%s'
            )
        """, un, pass, fn, ln, email, cd, lad);

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sql);
        } catch (SQLException ex) {
        }

        return true;
    }

    // APPROVED
    public static User findUser(User user) {
        String un = user.get_username();
        String pass = user.get_password();
        String sql = String.format("""
            SELECT * FROM users
            WHERE username = '%s' AND password = '%s';

        """, un, pass);
        User returned_user = null;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                returned_user = new User(
                    rs.getString("USERNAME"),
                    rs.getString("PASSWORD"),
                    rs.getString("FIRST_NAME"),
                    rs.getString("LAST_NAME"),
                    rs.getString("EMAIL"),
                    rs.getInt("USER_ID"),
                    rs.getString("CREATION_DATE")
                );
                try {
                    sql = String.format("""
                        UPDATE users
                        SET last_access_date = '%s'
                        WHERE username = '%s'
                    """, LocalDateTime.now(), un);
                    stmt.executeQuery(sql);
                } catch (SQLException e) {}
            }
        } catch (SQLException ex) {}
        return returned_user;
    }

    // APPROVED
    public static boolean createCollection(Collection collection) {
        int uid = collection.get_userid();
        String name = collection.get_collectionname();

        String sql = String.format("""
            INSERT INTO
                COLLECTIONS (collection_name, user_id)
                VALUES ('%s', '%s')
        """, name, uid);

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sql);
        } catch (SQLException ex) {
        }
        return true;
    }

    // APPROVED
    public static void listCollection(User user) {
        int uid = user.get_id();

        String sql = String.format("""
            SELECT 
                COLLECTIONS.COLLECTION_ID AS "COLLECTION_ID",
                COLLECTIONS.COLLECTION_NAME AS "Collection Name",
                COALESCE(COUNT(COLLECTIONSONG.SONG_ID), 0) AS "Number of Songs",
                COALESCE(SUM(SONGS.SONG_LENGTH), 0) AS "Total Duration"
            FROM COLLECTIONS
            LEFT JOIN COLLECTIONSONG ON COLLECTIONSONG.COLLECTION_ID = COLLECTIONS.COLLECTION_ID
            LEFT JOIN SONGS ON COLLECTIONSONG.SONG_ID = SONGS.SONG_ID
            WHERE USER_ID = '%d'
            GROUP BY COLLECTIONS.COLLECTION_NAME, COLLECTIONS.COLLECTION_ID
            ORDER BY COLLECTIONS.COLLECTION_NAME ASC;
        """, uid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String name = rs.getString("Collection Name");
                System.out.printf("%d: %s\n", rs.getInt("COLLECTION_ID"), name);
                int sec = rs.getInt("Total Duration");
                int hour = sec / 3600;
                sec %= 3600;
                int min = sec / 60;
                sec %= 60;
                System.out.println(String.format("Total Duration: %02d:%02d:%02d", hour, min, sec));
                System.out.println(String.format("# of songs: %d", rs.getInt("Number of Songs")));
                System.out.println("");
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }

    // APPROVED
    public static boolean searchSongName(Song song) {
        String sn = song.get_title();

        String sql = String.format("""
                    SELECT
                        SONGS.SONG_ID as "song_id",
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
                    System.out.println(rs.getString("song_id") + ", " + rs.getString("artist_name"));
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

    // APPROVED
    public static boolean searchSongArtist(Artist artist) {
        String aname = artist.get_artist();
        String sql = String.format("""
            SELECT
                SONGS.SONG_ID as "song_id",
                ARTISTS.ARTIST_NAME,
                SONGS.SONG_TITLE as "song_title",
                ALBUMS.ALBUM_NAME,
                SONGS.SONG_LENGTH,
                SONGS.RELEASE_DATE
            FROM ARTISTS
            JOIN SONGARTIST ON SONGARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
            JOIN SONGS ON SONGS.SONG_ID = SONGARTIST.SONG_ID
            JOIN ALBUMARTIST ON ALBUMARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
            JOIN ALBUMS ON ALBUMS.ALBUM_ID = ALBUMARTIST.ALBUM_ID
            WHERE ARTISTS.ARTIST_NAME = '%s'
        """, aname);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("The song is sang by the following artists. Which one are you referring to: ");
                do {
                    System.out.println(rs.getString("song_id") + ": " + rs.getString("song_title"));
                } while (rs.next());
                return true;
            } else {
                System.out.println("The artist does not have any songs under their name.");
            }
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean searchSongAlbum(Album album) {
        String aname = album.get_albumname();

        String sql = String.format("""
            SELECT
                SONGS.SONG_ID as "song_id",
                ARTISTS.ARTIST_NAME,
                SONGS.SONG_TITLE as "song_title",
                ALBUMS.ALBUM_NAME,
                SONGS.SONG_LENGTH,
                SONGS.RELEASE_DATE
            FROM ALBUMS
            JOIN ALBUMARTIST ON ALBUMARTIST.ALBUM_ID = ALBUMS.ALBUM_ID
            JOIN ARTISTS ON ARTISTS.ARTIST_ID  = ALBUMARTIST.ARTIST_ID

            JOIN SONGALBUM ON SONGALBUM.ALBUM_ID  = ALBUMS.ALBUM_ID
            JOIN SONGS ON SONGS.SONG_ID  = SONGALBUM.SONG_ID
            WHERE ALBUMS.ALBUM_NAME = '%s'
        """, aname);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("The following song is in the album. Which one are you referring to: ");
                do {
                    System.out.println(rs.getString("song_id") + ": " + rs.getString("song_title"));
                } while (rs.next());
                return true;
            } else {
                System.out.println("There's no songs in the album.");
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    // APPROVED
    public static boolean searchSongGenre(Genre genre) {
        String type = genre.get_genre();
        String sql = String.format("""
            SELECT
                SONGS.SONG_ID as "song_id",
                ARTISTS.ARTIST_NAME,
                SONGS.SONG_TITLE as "song_title",
                ALBUMS.ALBUM_NAME,
                SONGS.SONG_LENGTH,
                SONGS.RELEASE_DATE
            FROM GENRES
            JOIN SONGGENRE ON SONGGENRE.GENRE_ID = GENRES.GENRE_ID
            JOIN SONGS ON SONGS.SONG_ID = SONGGENRE.SONG_ID
            JOIN SONGARTIST ON SONGS.SONG_ID = SONGARTIST.SONG_ID
            JOIN ARTISTS ON SONGARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
            JOIN SONGALBUM ON SONGS.SONG_ID = SONGALBUM.SONG_ID
            JOIN ALBUMS ON SONGALBUM.ALBUM_ID = ALBUMS.ALBUM_ID
            WHERE GENRES.TYPE = '%s'
        """, type);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.printf("The following songs are %s. Which one do you want to add: \n", type);
                do {
                    System.out.println(rs.getString("song_id") + ", " + rs.getString("song_title"));
                } while (rs.next());
                return true;
            } else {
                System.out.println("There are no songs under this genre.");
            }
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean insertSong(int cid, int sid, int uid) {
        String sb = String.format("""
                    INSERT INTO collectionsong (collection_id, song_id)
                    VALUES (
                        (SELECT collection_id
                        FROM collections
                        WHERE collection_id = %d AND user_id = %d), %d)
                """, cid, uid, sid);
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sb);
        } catch (SQLException ex) {
        }
        return true;
    }

    // APPROVED
    public static boolean lookIntoCollectionSong(int cid) {
        String sb = String.format("""
                    SELECT
                        SONGS.SONG_TITLE AS "song_title",
                        SONGS.SONG_ID as "song_id"
                    FROM COLLECTIONS
                    JOIN COLLECTIONSONG ON COLLECTIONSONG.COLLECTION_ID = COLLECTIONS.COLLECTION_ID
                    JOIN SONGS ON SONGS.SONG_ID = COLLECTIONSONG.SONG_ID
                    WHERE COLLECTIONS.COLLECTION_ID = %d
                """, cid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            if (rs.next()) {
                System.out.println("These are the songs in this collection: ");
                do {
                    // check whether it is a song or an album
                    System.out.printf("%d: %s\n", rs.getInt("song_id"), rs.getString("song_title"));
                } while (rs.next());
                System.out.println("");
                return true;
            }
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean lookIntoCollectionAlbum(int cid) {
        String sb = String.format("""
                    SELECT
                        ALBUMS.ALBUM_NAME AS "album_name",
                        ALBUMS.ALBUM_ID as "album_id"
                    FROM COLLECTIONS
                        JOIN COLLECTIONALBUM ON COLLECTIONALBUM.COLLECTION_ID = COLLECTIONS.COLLECTION_ID
                        JOIN ALBUMS ON ALBUMS.ALBUM_ID = COLLECTIONALBUM.ALBUM_ID
                    WHERE COLLECTIONS.COLLECTION_ID = %d
                """, cid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            if (rs.next()) {
                System.out.println("These are the albums in this collection: ");
                do {
                    System.out.printf("%d: %s\n", rs.getInt("album_id"), rs.getString("album_name"));
                } while (rs.next());
                System.out.println("");
                return true;
            }
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean deleteSong(int cid, int sid, int uid) {
        String sql = String.format("""
                    DELETE FROM collectionsong
                    WHERE collection_id IN
                        (SELECT collection_id
                        FROM collections
                        WHERE collection_id = %d AND user_id = %d) AND SONG_ID = %d
                """, cid, uid, sid);

        try {
            Statement stm = conn.createStatement();
            int rowsAffected = stm.executeUpdate(sql);

            if (rowsAffected > 0) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    // APPROVED
    public static boolean insertAlbum(int cid, int aid, int uid) {
        String sb = String.format("""
            INSERT INTO collectionalbum (collection_id, album_id)
            VALUES (
                (SELECT collection_id
                FROM collections
                WHERE collection_id = %d AND user_id = %d), %d)
        """, cid, uid, aid);

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sb.toString());
        } catch (SQLException ex) {
        }
        return true;
    }

    // APPROVED
    public static boolean deleteAlbum(int cid, int aid, int uid) {
        String sb = String.format("""
                    DELETE FROM collectionalbum
                    WHERE collection_id IN(
                        SELECT collection_id
                        FROM collections
                        WHERE collection_id = %d AND user_id = %d
                    ) AND ALBUM_ID = %d
                """, cid, uid, aid);
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sb.toString());
        } catch (SQLException ex) {
        }

        return true;
    }

    // APPROVED
    public static boolean updateCollectionName(String name, int cid, int uid) {
        String sb = String.format("""
                    UPDATE COLLECTIONS
                    SET COLLECTION_NAME = '%s'
                    WHERE COLLECTION_ID = %d
                    AND USER_ID = %d
                """, name, cid, uid);

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sb.toString());
        } catch (SQLException ex) {}
        return true;
    }

    // APPROVED
    public static boolean deleteCollection(int cid, int uid) {
        String sb = String.format("""
                    DELETE
                    FROM COLLECTIONS
                    WHERE COLLECTION_ID = %d
                    AND USER_ID = %d
                """, cid, uid);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()) {
                return true;
            }
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean follow(int follower, int followee) {
        String sql = String.format("""
            INSERT INTO FOLLOWERS (
                FOLLOWER_ID, FOLLOWEE_ID
            ) VALUES (
                %d, %d
            )
        """, follower, followee);
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sql);
        } catch (SQLException ex) {}
        return true;
    }

    // APPROVED
    public static boolean follow(String email, int uid) {
        String sql = String.format("""
            SELECT
                USERS.USER_ID as "uid"
            FROM USERS 
            WHERE USERS.EMAIL = '%s'
        """, email);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                follow(uid, rs.getInt("uid"));
            }
            return true;
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static int searchEmail(String email) {
        String sb = String.format("""
            SELECT user_id, username 
            FROM users 
            WHERE email = '%s'
        """, email);

        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
            if (rs.next()){
                return rs.getInt("user_id");
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return -1;
    }

    // APPROVED
    public static boolean unfollow(String email, int uid) {
        String sb = String.format("""
            DELETE FROM followers
            WHERE follower_id = %d
            AND followee_id = (SELECT user_id FROM users WHERE email = '%s');
        """, uid, email);
        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sb.toString());
        } catch (SQLException ex) {}
        return true;
    }

    // APPROVED
    public static boolean listenToSong(int sid, int uid) {
        LocalDateTime now = LocalDateTime.now();

        String sb = String.format("""
            INSERT INTO LISTENHISTORY (USER_ID, SONG_ID, DATETIME) VALUES
            (%d, %d, '%s')
        """, uid, sid, now);

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(sb.toString());
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean listenToSongCollection(int cid, int uid) {
        String sql = String.format("""
            SELECT COLLECTIONSONG.SONG_ID AS "sid"
            FROM COLLECTIONSONG
            WHERE COLLECTIONSONG.COLLECTION_ID = %d
        """, cid);
        try {
            Statement stm = conn.createStatement();
            ResultSet result = stm.executeQuery(sql);
            while (result.next()) {
                listenToSong(result.getInt("sid"), uid);
            }
        } catch (SQLException ex) {}
        return true;
    }

    // APPROVED
    public static boolean collectionNum(int uid) {
        String sb = String.format("""
            SELECT COUNT(collections) FROM collections WHERE user_id = %d
        """, uid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            rs.next();
            System.out.printf("# of Collections: %d\n", rs.getInt(1));
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    // APPROVED
    public static boolean viewFollowed(int uid) {
        String sb = String.format("""
            SELECT COUNT(follower_id) FROM followers WHERE followee_id = %d
        """, uid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            rs.next();
            System.out.printf("Followers: %d\n", rs.getInt(1));
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean viewFollowing(int uid) {
        String sb = String.format("""
            SELECT COUNT(followee_id) FROM followers WHERE follower_id = %d
        """, uid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            rs.next();
            System.out.printf("Following: %d\n", rs.getInt(1));
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean topTenArtists(int uid) {
        String sql = String.format("""
            SELECT ARTISTS.ARTIST_NAME, COUNT(*) as numSongs FROM LISTENHISTORY
            INNER JOIN SONGS ON LISTENHISTORY.SONG_ID = SONGS.SONG_ID
            INNER JOIN SONGARTIST ON SONGS.SONG_ID = SONGARTIST.SONG_ID
            INNER JOIN ARTISTS ON SONGARTIST.ARTIST_ID = ARTISTS.ARTIST_ID
            WHERE LISTENHISTORY.USER_ID = %d
            GROUP BY ARTISTS.ARTIST_NAME
            ORDER BY numSongs DESC
            LIMIT 10
        """, uid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("Top 10 artists listened to by user " + uid);
                do {
                    System.out.printf("%s\n", rs.getString("ARTIST_NAME"));
                } while (rs.next());
                System.out.println("");
                return true;
            } else {
                System.out.println("You haven't listened to a single song yet. I suggest song id 4242.");
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return false;
    }

    // APPROVED
    public static boolean topFiveGenres(String month, String year) {
        String sb = String.format("""
                SELECT type, COUNT(*) as numSongs FROM songs
                INNER JOIN (
                    SELECT * FROM listenhistory WHERE EXTRACT(MONTH FROM datetime) = '%s' AND EXTRACT(YEAR FROM datetime) = '%s'
                ) as calendarMonth ON songs.song_id = calendarMonth.song_id
                INNER JOIN songgenre ON songs.song_id = songgenre.song_id
                INNER JOIN genres ON songgenre.genre_id = genres.genre_id
                GROUP BY type
                ORDER BY numSongs DESC
                LIMIT 5
            """, month, year);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            if(rs.next()) {
                System.out.println("These are the top 5 genres this month: ");
                do {
                    System.out.printf("%s\n", rs.getString("type"));
                } while (rs.next());
                System.out.println("");
                return true;
            }
        } catch (SQLException ex) {}
        return false;
    }

    // APPROVED
    public static boolean recommendSongs(int uid) {
        String sql1 = String.format("""
            SELECT DISTINCT 
                SONG_ID AS songtitles 
            FROM 
                LISTENHISTORY 
            WHERE 
                USER_ID = %d
        """, uid);
    
        try {
            Statement stmt1 = conn.createStatement();
            ResultSet rs1 = stmt1.executeQuery(sql1);
    
            String listofids = "(";
            boolean firstIteration = true;
            while (rs1.next()) {
                if (!firstIteration) {
                    listofids += ",";
                } else {
                    firstIteration = false;
                }
                listofids += rs1.getString("songtitles");
            }
            listofids += ")";

            if (listofids.equals("()")) {
                System.out.println("You haven't listened to any songs yet. I suggest song id: 4242.");
                return false;
            }
    
            Set<String> sb = new HashSet<>();
    
            // Query 1
            String sql2 = String.format("""
                SELECT DISTINCT SONG_TITLE AS SONGTITLES, GENRES.TYPE AS TYPES
                FROM SONGS
                JOIN SONGGENRE ON SONGGENRE.SONG_ID = SONGS.SONG_ID
                JOIN GENRES ON GENRES.GENRE_ID = SONGGENRE.GENRE_ID
                WHERE SONGS.SONG_ID IN %s
            """, listofids);
    
            Statement stmt2 = conn.createStatement();
            ResultSet rs2 = stmt2.executeQuery(sql2);
            while (rs2.next()) {
                sb.add(rs2.getString("SONGTITLES"));
            }
    
            // Query 2
            String sql3 = String.format("""
                SELECT DISTINCT SONG_TITLE AS SONGTITLES 
                FROM SONGS
                WHERE SONG_ID IN (
                    SELECT SONG_ID 
                    FROM SONGALBUM
                    WHERE ALBUM_ID IN (
                        SELECT ALBUM_ID 
                        FROM ALBUMARTIST 
                        WHERE ALBUMARTIST.ARTIST_ID IN (
                            SELECT ARTISTS.ARTIST_ID 
                            FROM ARTISTS 
                            WHERE ARTISTS.ARTIST_ID IN (
                                SELECT ARTIST_ID 
                                FROM SONGARTIST
                                WHERE SONG_ID IN %s
                            )
                        )
                    )
                )
            """, listofids);
    
            Statement stmt3 = conn.createStatement();
            ResultSet rs3 = stmt3.executeQuery(sql3);
            while (rs3.next()) {
                sb.add(rs3.getString("SONGTITLES"));
            }
    
            for (String song : sb) {
                System.out.println(song);
            }
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        }
        return true;
    }

    // APPROVED
    public static boolean top50SongsInLast30Days() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Calendar cal = Calendar.getInstance();
        String currentDate = dateFormat.format(cal.getTime()); // today
        cal.add(Calendar.DATE, -30);
        String thirtydaysago = dateFormat.format(cal.getTime()); // 30 days ago

        String sb = String.format("""
            SELECT SONGS.SONG_TITLE as "song_title", count (LISTENHISTORY.SONG_ID) AS "number_of_listens"
            FROM SONGS
            JOIN LISTENHISTORY
            ON 
            LISTENHISTORY.SONG_ID = SONGS.SONG_ID
            WHERE DATETIME BETWEEN '%s' AND '%s'
            GROUP BY SONGS.SONG_TITLE
            ORDER BY number_of_listens DESC
            LIMIT 50
        """, thirtydaysago, currentDate);


        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            if(rs.next()) {
                System.out.println("These are the top 50 songs within the last 30 days: ");
                do {
                    System.out.printf("%s\n", rs.getString("song_title"));
                } while (rs.next());
                System.out.println("");
                return true;
            }
            else {
                System.out.println("You haven't listened to any songs");
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return false;
    }

    // APPROVED
    public static boolean top50FollowerSongs(int uid) {
        String sb = String.format("""
                SELECT song_title, COUNT(*) as count
                FROM followers
                JOIN listenhistory on follower_id = listenhistory.user_id
                JOIN songs ON listenhistory.song_id = songs.song_id
                WHERE followee_id = %d
                GROUP BY song_title
                ORDER BY count DESC
                LIMIT 50
            """, uid);
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb);
            if(rs.next()) {
                System.out.println("These are the top 50 follower songs: ");
                do {
                    System.out.printf("%s\n", rs.getString("song_title"));
                } while (rs.next());
                System.out.println("");
                return true;
            }
            else {
                System.out.println("You have no followers. :(");
            }
        } catch (SQLException ex) {}
        return false;
    }
}