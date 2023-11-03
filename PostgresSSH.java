import com.jcraft.jsch.*;

import java.io.File;
import java.io.FileNotFoundException;
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
            System.out.println("Connected");
            int assigned_port = session.setPortForwardingL(lport, "127.0.0.1", rport);
            System.out.println("Port Forwarded");

            // Assigned port could be different from 5432 but rarely happens
            String url = "jdbc:postgresql://127.0.0.1:"+ assigned_port + "/" + databaseName;

            System.out.println("database Url: " + url);
            Properties props = new Properties();
            props.put("user", user);
            props.put("password", password);

            Class.forName(driverName);
            conn = DriverManager.getConnection(url, props);
            System.out.println("Database connection established");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void close_connection() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            System.out.println("Closing Database Connection");
            conn.close();
        }
        if (session != null && session.isConnected()) {
            System.out.println("Closing SSH Connection");
            session.disconnect();
        }
    }

    public static void addUser(User user){
        String un = user.get_username();
        String pass = user.get_password();
        String fn = user.get_first_name();
        String ln = user.get_last_name();
        String email = user.get_email();
        String cd = user.get_creation_date();
        String lad = user.get_last_access_date();

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO users (username, password, first_name,");
        sb.append("last_name, email, creation_date, last_access_date) VALUES (");
        sb.append("'" + un + "', ");
        sb.append("'" + pass + "', ");
        sb.append("'" + fn + "', ");
        sb.append("'" + ln + "' ,");
        sb.append("'" + email + "', ");
        sb.append("'" + cd + "', ");
        sb.append("'" + lad + "')");
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sb.toString());
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }

    public static boolean findUser(User user){
        int uid = user.get_id();
        String un = user.get_username();
        String pass = user.get_password();
        String lad = user.get_last_access_date();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT user_id, username, FROM users WHERE username = <");
        sb.append("'" + un + "'" + "> AND password = <");
        sb.append("'" + pass + "'" + ">\n");
        sb.append("UPDATE users SET last_access_date = <");
        sb.append("'" + lad + "'" + "> WHERE user_id = <");
        sb.append(uid + ">");
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
        sb.append("(<'" + name + "'>, <'" + uid + "'>)");
    }

}