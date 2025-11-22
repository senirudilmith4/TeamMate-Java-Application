import java.util.*;

/**
 * TeamMate: Intelligent Team Formation System
 * Main application class with console interface.
 */
public class TeamMateApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static List<Participant> participants = new ArrayList<>();
    private static List<Team> formedTeams = new ArrayList<>();
    private static AppController appController = new AppController();

    public static void main(String[] args) {
        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = getIntInput("Enter choice: ", 1, 7);

            switch (choice) {
                case 1:
                    appController.completeSurvey();
                    break;
                case 2:
                   // generateSampleData();
                    break;
                case 3:
                   appController.viewParticipants();
                    break;
                case 4:
                 //   formTeams();
                    break;
                case 5:
                 //   viewTeams();
                    break;
                case 6:
                  //  saveResults();
                    break;
                case 7:
                    running = false;
                    break;
            }
        }

        System.out.println("\nThank you for using TeamMate! Goodbye.");
        scanner.close();
    }

    private static void printBanner() {
        System.out.println("\n" +
                "╔══════════════════════════════════════════════════════╗\n" +
                "║     ████████╗███████╗ █████╗ ███╗   ███╗             ║\n" +
                "║        ██╔══╝██╔════╝██╔══██╗████╗ ████║             ║\n" +
                "║        ██║   █████╗  ███████║██╔████╔██║             ║\n" +
                "║        ██║   ██╔══╝  ██╔══██║██║╚██╔╝██║             ║\n" +
                "║        ██║   ███████╗██║  ██║██║ ╚═╝ ██║             ║\n" +
                "║        ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝  MATE       ║\n" +
                "║                                                      ║\n" +
                "║       Intelligent Team Formation System              ║\n" +
                "║       University Gaming Club Edition                 ║\n" +
                "╚══════════════════════════════════════════════════════╝\n");
    }

    private static void printMenu() {
        System.out.println("\n┌─────────────── MAIN MENU ───────────────┐");
        System.out.println("│  1. Load participants from CSV          │");
        System.out.println("│  2. Generate sample data                │");
        System.out.println("│  3. View participants                   │");
        System.out.println("│  4. Form teams                          │");
        System.out.println("│  5. View formed teams                   │");
        System.out.println("│  6. Save results to file                │");
        System.out.println("│  7. Exit                                │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.printf("  [Loaded: %d participants, %d teams]\n",
                participants.size(), formedTeams.size());
    }

    private static int getIntInput(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
                System.out.printf("Please enter a number between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}
