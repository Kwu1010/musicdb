import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Scanner;
import Model.*;

class Controller {
    static Scanner sc;

    private static void print_help() {
        System.out.println("Actions avaliable:");
        System.out.println("\t0. Exit Account");
        System.out.println("\t1. Add Song to Collection");
        System.out.println("\t2. Delete Song to Collection");
        System.out.println("\t3. Add Album to Collection");
        System.out.println("\t4. Delete Album to Collection");
        System.out.println("\t5. Follow User");
        System.out.println("\t6. Unfollow User");
        System.out.println("\t7. View Collection Of Songs");
        System.out.println("\t8. Change name of Collection");
        System.out.println("\t9. Delete whole Collection");
    }

    private static String ask(String var_name) {
        String var;
        while (true) {
            System.out.println(var_name + ": ");
            var = sc.next();
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
        String artist = ask("Artist");
        Song song = new Song(title, artist, -1, "-1");
        return song;
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

    public static void main(String[] args) throws FileNotFoundException, SQLException {
        sc = new Scanner(System.in);
        PostgresSSH.connect_to_database();

        boolean logged = false;
        User user = null;
        outer: while (!logged) {
            System.out.println("Login/Register/Quit (L/R/Q): ");
            String op = sc.next();
            System.out.println("");

            switch (op) {
            case "L":
                user = try_to_log();
                user = PostgresSSH.findUser(user);
                if (user == null) {
                    System.out.println("Invalid Username/Password. Please try again.\n");
                } else {
                    logged = true;
                }
                break;
            case "R":
                user = register_user();
                PostgresSSH.addUser(user);
                break;
            case "Q":
                break outer;
            default:
                System.out.println("Invalid. Only choose either L(ogin) or R(egister) or Q(uit).\n");
            }
        }
        
        if (logged) {
            System.out.println(user);
            boolean get_out = false;
            Song song;
            while (!get_out) {
                print_help();
                
                try {
                    int op = sc.nextInt();
                    switch (op) {
                    case 0:
                        get_out = true;
                        break;
                    case 1:
                        song = ask_for_song();
                        break;
                    case 2:
                        song = ask_for_song();
                        break;
                    // case 3:
                    //     album = ask_for_album();
                    //     break;                                           c                                                                                                         
                    // case 4:
                    //     album = ask_for_album();
                    //     break;
                    default:
                        System.out.println("No such operation. Please select your desired operation.");
                    }
                } catch (Exception e) {
                    System.out.println("Please enter a number.");
                }
            }
        }

        sc.close();
        PostgresSSH.close_connection();
    }
}