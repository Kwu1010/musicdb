import java.util.Scanner;

class Controller {
    static Scanner sc;

    private static void print_help() {
        System.out.println("Actions avaliable:");
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

    private static boolean create_account() {
        String username;
        while (true) {
            System.out.println("Username: ");
            username = sc.next();
            if (!username.isEmpty()) {
                break;
            } else {
                System.out.println("Username cannot be empty. Try again.");
            }
        }
        String password;
        while (true) {
            System.out.println("Password: ");
            password = sc.next();
            if (!password.isEmpty()) {
                break;
            } else {
                System.out.println("Password cannot be empty. Try again.");
            }
        }
        
        // TODO: try to put current account information in database
        
        return true;
    }

    private static boolean try_to_log() {
        String username;
        while (true) {
            System.out.println("Username: ");
            username = sc.next();
            if (!username.isEmpty()) {
                break;
            } else {
                System.out.println("Username cannot be empty. Try again.");
            }
        }
        String password;
        while (true) {
            System.out.println("Password: ");
            password = sc.next();
            if (!password.isEmpty()) {
                break;
            } else {
                System.out.println("Password cannot be empty. Try again.");
            }
        }

        // TODO: Check our current JSON DB whether current account exist
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
                create_account();
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