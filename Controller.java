import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Scanner;

import Model.*;

class Controller {
    static Scanner sc;

    private static void print_help() {
        System.out.println("Actions avaliable:");
        System.out.println("\t0. Exit Account");
        System.out.println("\t1. Search song by song name");
        System.out.println("\t2. Search song by artist name");
        System.out.println("\t3. Search song by album name");
        System.out.println("\t4. Search song by genre");
        System.out.println("\t5. View collections");
        System.out.println("\t6. Create collection");
        System.out.println("\t7. Change name of Collection");
        System.out.println("\t8. Delete whole Collection");
        System.out.println("\t9. Add song to collection");
        System.out.println("\t10. Delete song from collection");
        System.out.println("\t11. Add album to collection");
        System.out.println("\t12. Delete album from collection");
        System.out.println("\t13. Listen to a song");
        System.out.println("\t14. Search for another user");
        System.out.println("\t15. Follow User");
        System.out.println("\t16. Unfollow User");
    }

    private static String ask(String var_name) {
        String var;
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

    private static Collection ask_for_collection(int uid) {
        String name = ask("Collection");
        Collection collection = new Collection(name, uid);
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
            // PRE DEFINED VARIABLES
            boolean get_out = false;
            Song song;
            Artist artist;
            Album album;
            Genre genre;
            Collection collection;
            int cid, sid;

            while (!get_out) {
                try {
                    print_help();
                    String s = ask("owo");
                    int op = Integer.parseInt(s);
                    switch (op) {
                    case 0: // QUIT
                        get_out = true;
                        break;
                    case 1: // search song by name
                        song = ask_for_song();
                        PostgresSSH.searchSongName(song);
                        break;
                    case 2: // search song by artist
                        artist = ask_for_artist();
                        PostgresSSH.searchSongArtist(artist);
                        break;
                    case 3: // search song by album
                        album = ask_for_album();
                        PostgresSSH.searchSongAlbum(album);
                        break;
                    case 4: // search song by genre
                        genre = ask_for_genre();
                        PostgresSSH.searchSongGenre(genre);
                        break;
                    case 5: // look into collection based on ID
                        PostgresSSH.listCollection(user);
                        cid = Integer.parseInt(ask("collection id"));
                        boolean isEmpty = !PostgresSSH.lookIntoCollectionSong(cid);
                        isEmpty = isEmpty || !PostgresSSH.lookIntoCollectionAlbum(cid);
                        if (isEmpty) {
                            System.out.println("This collection is empty.");
                        }
                        break;
                    case 6: // create collection
                        collection = ask_for_collection(user.get_id());
                        boolean success = PostgresSSH.createCollection(collection);
                        if (success) {
                            System.out.println("Collection has successfully been created!");
                        } else {
                            System.out.println("Collection is not created");
                        }
                        break;
                    case 7: // Change name of collection
                        break;
                    case 8: // Delete collection
                        break;
                    case 9: // Add song to collection
                        cid = Integer.parseInt(ask("collection id"));
                        sid = Integer.parseInt(ask("song id"));
                        PostgresSSH.insertSong(cid, sid, user.get_id());
                        break;
                    case 10: // Delete song from collection
                        cid = Integer.parseInt(ask("collection id"));
                        sid = Integer.parseInt(ask("song id"));
                        PostgresSSH.deleteSong(cid, sid, user.get_id());
                        break;
                    case 11: // Add album to collection
                        break;
                    case 12: // Delte album from collection
                        break;
                    case 13: // Listen to a song
                        break;
                    case 14: // Search for another user
                        break;
                    case 15: // Follow user
                        break;
                    case 16: // Unfollow user
                        break;
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