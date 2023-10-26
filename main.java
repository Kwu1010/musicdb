import java.util.Scanner;

class main {
    static Scanner sc;

    private static void print_help() {
        System.out.println("");
    }

    private static void create_account(){
        
    }

    private static boolean try_to_log() {
        System.out.print("Enter your username: ");
        String username = sc.next();
        System.out.print("Enter your password: ");
        String password = sc.next();
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