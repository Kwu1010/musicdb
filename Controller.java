import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Scanner;
import java.lang.Thread;

import org.postgresql.osgi.PGDataSourceFactory;

import Model.*;

class Controller {
    static Scanner sc;

    private static void print_help() {
        System.out.println("Actions avaliable:");
        System.out.println("\t0. Exit Account");
        System.out.println("\t1. Add Song by song name to Collection");
        System.out.println("\t2. Add Song by artist name to Collection");
        System.out.println("\t3. Delete Song to Collection");
        System.out.println("\t4. Add Album to Collection");
        System.out.println("\t5. Delete Album to Collection");
        System.out.println("\t6. Follow User");
        System.out.println("\t7. Unfollow User");
        System.out.println("\t8. View Collection Of Songs");
        System.out.println("\t9. Change name of Collection");
        System.out.println("\t10. Delete whole Collection");
    }

    private static String ask(String var_name) {
        String var;
        boolean first = true;
        while (true) {
            System.out.print(var_name + ": ");
            var = sc.nextLine();
            if (!var.isEmpty()) {
                break;
            } else {
                System.out.println(var_name + " cannot be empty. Try again.");
            }
        }
        return var;
    }

    private static Song ask_for_song() {
        String title = ask("Title");
        Song song = new Song(title, "TEMP", -1, "TEMP");
        return song;
    }

    private static Artist ask_for_artist(){
        String name = ask("Artist Name");
        Artist artist = new Artist(name);
        return artist;
    }

    private static Album ask_for_album(){
        String name = ask("Album name");
        Album album = new Album(name, "TEMP");
        return album;
    }

    private static Genre ask_for_genre(){
        String type = ask("Genre");
        Genre genre = new Genre(type);
        return genre;
    }

    private static Collection ask_for_collection(){
        String name = ask("Collection");
        Collection collection = new Collection(name, 0);
        return collection;
    }
    

    private static User register_user() {
        String first_name = ask("First Name");
        String last_name = ask("Last Name");
        String username = ask("Username");
        String password = ask("Password");
        String email = ask("Email");
        User user = new User(username, password, first_name, last_name, email);
        return user;
    }

    private static User try_to_log() {
        String username = ask("Username");
        String password = ask("Password");
        User user = new User(username, password, "TEMP", "TEMP", "TEMP", 1, "TEMP");
        return user;
    }

    public static void main(String[] args) throws FileNotFoundException, SQLException, InterruptedException {
        sc = new Scanner(System.in);
        PostgresSSH.connect_to_database();

        boolean logged = false;
        User user = null;
        outer: while (!logged) {
            String op = ask("Login/Register/Quit (L/R/Q)");
            System.out.println("");

            switch (op) {
            case "L":
                user = try_to_log();
                user = PostgresSSH.findUser(user);
                if (user == null) {
                    System.out.println("Invalid Username/Password. Please try again.");
                } else {
                    logged = true;
                }
                break;
            case "R":
                user = register_user();
                boolean success = PostgresSSH.addUser(user);
                if (success) {
                    System.out.println("Your account has been successfully created! Please go to the login page.");
                    User cur = PostgresSSH.findUser(user);
                    Collection col = new Collection("Favorites", cur.get_id());
                    PostgresSSH.createCollection(col);
                } else {
                    System.out.println("The creation of your account was unsuccessful. Please try again.");
                }
                break;
            case "Q":
                break outer;
            default:
                System.out.println("Invalid. Only choose either L(ogin) or R(egister) or Q(uit).");
            }
            System.out.println("");
        }
        
        if (logged) {
            boolean get_out = false;
            Song song;
            Artist artist;
            Album album;
            Genre genre;
            Collection collection;
            while (!get_out) {
                try {
                    print_help();
                    String s = ask("owo");
                    int op = Integer.parseInt(s);
                    switch (op) {
                    case 0:
                        get_out = true;
                        break;
                    case 1:
                        song = ask_for_song();
                        PostgresSSH.searchSongName(song);
                        break;
                    case 2:
                        artist = ask_for_artist();
                        PostgresSSH.searchSongArtist(artist);
                        break;
                    case 3:
                        album = ask_for_album();
                        PostgresSSH.searchSongAlbum(album);
                        break;
                    case 4:
                        genre = ask_for_genre();
                        PostgresSSH.searchSongGenre(genre);
                        break;
                    case 5:
                        collection = ask_for_collection();
                        song = ask_for_song();
                        PostgresSSH.insertSong(collection, song);
                    // case 6:
                    //     collection = ask_for_collection();
                    //     song = ask_for_song();
                    //     PostgresSSH.deleteSong(collection, song);
                    // case 7:
                    //     collection = ask_for_collection();
                    //     album = ask_for_album();
                    //     PostgresSSH.deleteAlbum(collection, album);
                    default:
                        System.out.println("No such operation. Please select your desired operation.");
                    }
                } catch (Exception e) {
                    System.out.println("Please enter a number.");
                }
                System.out.println("");
            }
        }

        sc.close();
        PostgresSSH.close_connection();
    }
}