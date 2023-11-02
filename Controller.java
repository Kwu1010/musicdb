import java.util.Scanner;
import Model.*;

class Controller {
    static Scanner sc;

    private static void print_help() {
        System.out.println("1. Add Music");
        System.out.println("2. Delete Music");
        System.out.println("3. Add Playlist");
        System.out.println("4. Delete Playist");
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
        String author = ask("Author");
        Song song = new Song(title, 0);
        return song;
    }

    private static boolean ask_for_account() {
        String first_name = ask("First Name");
        String last_name = ask("Last Name");
        String username = ask("Username");
        String password = ask("Password");
        String email = ask("Email");
        User user = new User(first_name, last_name, username, password, email);
        return true;
    }

    private static boolean try_to_log() {
        String username = ask("Username");
        String password = ask("Password");
        return true;
    }

    public static void main(String[] args) {
        sc = new Scanner(System.in);

        boolean logged = false;
        while (!logged) {
            System.out.println("Login/Register (L/R): ");
            String op = sc.next();

            switch (op) {
            case "L":
                logged = try_to_log();
                break;
            case "R":
                ask_for_account();
                break;
            default:
                System.out.println("Invalid. Only choose either L(ogin) or R(egister).");
            }
        }
        
        boolean get_out = false;
        while (!get_out) {
            print_help();

            int op = sc.nextInt();

            switch (op) {
            case 0:
                get_out = true;
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                print_help();
            }
        }

        sc.close();
    }
}