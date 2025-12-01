package com.seniru.teambuilder.app;

import com.seniru.teambuilder.login.OrganizerLogin;
import com.seniru.teambuilder.model.Team;

import java.util.*;

/**
 * TeamMate: Intelligent com.seniru.teambuilder.model.Team Formation System
 * Main application class with console interface.
 */
public class TeamMateApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static List<Team> formedTeams = new ArrayList<>();
    private static AppController appController = new AppController();
    private static OrganizerLogin organizerLogin = new OrganizerLogin();



    public static void main(String[] args) {
        printBanner();

        boolean running = true;
        while (running) {
            printLogin();
            int choice = getIntInput("Enter choice: ",1,3);
            switch (choice) {
                case 1:
                    try {
                        if (organizerLogin.authenticate()==true) {
                            organizerInterface();
                        }else {
                            System.out.println("You are not authenticated");
                        }
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                    break;
                case 2:
                    participantInterface();
                    break;
                case 3:
                    running = false;
                    break;
            }
        }
        System.out.println("\nThank you for using TeamMate! Goodbye.");
        scanner.close();
    }

    public static void participantInterface() {

        boolean running = true;
        while (running) {

            participantMenu();
            int choice = getIntInput("Enter choice: ", 1, 3);

            switch (choice) {
                case 1:
                    appController.completeSurvey();
                    break;
                case 2:
                    //  view Survey Results();
                    break;
                case 3:
                    running = false;
                    break;
            }
        }

        System.out.println("\nThank you for using TeamMate! Goodbye.");
        scanner.close();
    }

    public static void organizerInterface() {

        boolean running = true;
        while (running) {

            organizerMenu();
            int choice = getIntInput("Enter choice: ", 1, 6);

            switch (choice) {
                case 1:
                    appController.viewParticipants();
                    break;
                case 2:
                    appController.loadAllParticipantsAtStart();
                    break;
                case 3:
                    appController.formTeams();
                    break;
                case 4:
                   // appController.viewFormedTeams();
                case 6:
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

    private static void participantMenu() {
        System.out.println("\n┌─────────────── MAIN MENU ───────────────┐");
        System.out.println("│  1. Add Participant                     │");
        System.out.println("│  2. View Survey Results                 │");
        System.out.println("│  7. Exit                                │");
        System.out.println("└─────────────────────────────────────────┘");
    }

    private static void organizerMenu() {
        System.out.println("\n┌─────────────── MAIN MENU ───────────────┐");
        System.out.println("│  1. View added participants             │");
        System.out.println("│  2. Load participants from CSV          │");
        System.out.println("│  3. Form teams                          │");
        System.out.println("│  4. View formed teams                   │");
        System.out.println("│  5. Save results to file                │");
        System.out.println("│  6. Exit                                │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.printf("  [Loaded: %d participants, %d teams]\n",
                appController.getParticipants().size(), formedTeams.size());
    }


    private static void printLogin(){
        System.out.println("\n┌───────────── Login ───────────────┐");
        System.out.println("│  1. Organizer                     │");
        System.out.println("│  2. Participant                   │");
        System.out.println("│  3. Exit                          │");
        System.out.println("└───────────────────────────────────┘");
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
