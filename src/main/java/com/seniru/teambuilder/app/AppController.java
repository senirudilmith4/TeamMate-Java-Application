package com.seniru.teambuilder.app;

import com.seniru.teambuilder.model.Participant;
import com.seniru.teambuilder.model.Team;
import com.seniru.teambuilder.exception.SurveyException;
import com.seniru.teambuilder.service.CSVHandler;
import com.seniru.teambuilder.service.SurveyProcess;
import com.seniru.teambuilder.service.PersonalityClassifier;
import com.seniru.teambuilder.login.Organizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.seniru.teambuilder.model.Participant;

public class AppController {
    private static List<Participant> participants = new ArrayList<>();
    private static List<Team> formedTeams = new ArrayList<>();
    private SurveyProcess survey = new SurveyProcess();
    private PersonalityClassifier classifier = new PersonalityClassifier();
    private CSVHandler csvHandler = new CSVHandler();
    Scanner scanner = new Scanner(System.in);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private Participant lastSurveyParticipant;



    public List<Participant> getParticipants() {
        return participants;
    }



    public void loadAllParticipantsAtStart() {
        System.out.println("Enter file path: ");
        String path = scanner.nextLine();
        participants = csvHandler.loadParticipants(path);
        System.out.println("Participants loaded: " + participants.size()); }


    public void addParticipant() {

        try {
            Participant p = survey.conductSurvey();
            lastSurveyParticipant = p;

            // Classification first
            Future<Void> classificationTask = executor.submit(() -> {
                classifier.classifyParticipant(p);
                return null;
            });

            // Wait for classification to complete
            classificationTask.get(5, TimeUnit.SECONDS);

            // Add to list (personality is now set)
            synchronized (participants) {
                participants.add(p);
            }

            // CSV writes complete participant
            Future<Void> csvTask = executor.submit(() -> {
                csvHandler.appendParticipant(p);
                return null;
            });

            csvTask.get(5, TimeUnit.SECONDS);
            System.out.println("✅ Complete for: " + p.getName());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public void viewParticipants() {
        if (participants.isEmpty()) {
            System.out.println("No participants have been added yet.");
            return;
        }

        System.out.println("\n---- List of Participants ----");
        for (Participant p : participants) {
            System.out.println(p.getName() + " | Personality: " + p.getPersonalityType());
        }
    }
    public void viewFormedTeams() {
        // Load teams from CSV
        formedTeams = csvHandler.loadFormedTeams("formedTeams.csv");

        if (formedTeams.isEmpty()) {
            System.out.println("📭 No formed teams found.");
            return;
        }

        System.out.println("\n📋 Formed Teams (Showing up to 6 teams):");

        // Display up to 6 teams
        int displayCount = Math.min(formedTeams.size(), 6);
        for (int i = 0; i < displayCount; i++) {
            Team team = formedTeams.get(i);
            System.out.println("──────── TEAM " + team.getID() + " ────────");

            List<Participant> members = team.getMembers();
            int count = 1;
            for (Participant p : members) {
                System.out.printf("%d. %-15s | %-10s | %-12s | %-12s | %d\n",
                        count,
                        p.getName(),
                        p.getPreferredRole(),
                        p.getPreferredSport(),
                        p.getPersonalityType(),
                        p.getSkillLevel());
                count++;
            }
            System.out.println();  // empty line after each team
        }

        // Show message if more than 6 teams exist
        if (formedTeams.size() > 6) {
            System.out.println("...and " + (formedTeams.size() - 6) + " more teams not displayed.");
        }
    }

    //    public void viewFormedTeams() {
//        formedTeams = csvHandler.loadFormedTeams("formedTeams.csv");
//
//        if (formedTeams.isEmpty()) {
//            System.out.println("📭 No formed teams found.");
//            return;
//        }
//
//        for (Team t : formedTeams) {
//            for (Participant p : t.getMembers()) {
//                System.out.println(" - " + p.getName()
//                        + " | Role: " + p.getPreferredRole()
//                        + " | Game: " + p.getPreferredSport()
//                        + " | Personality: " + p.getPersonalityType()
//                        + " | Skill: " + p.getSkillLevel());
//            }
//        }
//        System.out.println("\n📋 Formed Teams (Showing up to 6 teams):");
//
//        int displayCount = Math.min(formedTeams.size(), 6);
//
//        for (int i = 0; i < displayCount; i++) {
//            Team team = formedTeams.get(i);
//            System.out.println("──────── TEAM " + team.getID() + " ────────");
//
//            List<Participant> members = team.getMembers();
//            int count = 1;
//            for (Participant p : members) {
//                System.out.printf("%d. %-10s | %-10s | %-10s | %s\n",
//                        count,
//                        p.getName(),
//                        p.getPreferredRole(),
//                        p.getPreferredSport(),
//                        p.getPersonalityType());
//                count++;
//            }
//            System.out.println();  // empty line after each team
//        }
//
//        // Show message if there are more teams
//        if (formedTeams.size() > 6) {
//            System.out.println("...and " + (formedTeams.size() - 6) + " more teams not displayed.");
//        }
//   }
    public void viewSurveyResults() {
        try {
            if (lastSurveyParticipant == null) {
                System.out.println("❌ No participant has been added yet.");
                return;
            }

            Participant p = lastSurveyParticipant;

            System.out.println("\n--- SURVEY RESULTS ---");
            System.out.println("Name: " + p.getName());
            System.out.println("Personality Type: " + p.getPersonalityType());
            lastSurveyParticipant = null;

        } catch (Exception e) {
            System.err.println("❌ Error displaying survey results: " + e.getMessage());
        }
    }
    public void formTeams() {

        if (getParticipants().isEmpty()) {
            System.out.println("❌ No participants available to form teams.");
            return;
        }

        System.out.print("Enter desired team size (minimum 2): ");
        int teamSize;
        Organizer organizer = new Organizer();
        while (true) {   // repeat until it stop explicitly from break
            try {
                teamSize = Integer.parseInt(scanner.nextLine().trim());

                if (teamSize < 2) {
                    System.out.print("❌ Team size must be at least 2. Enter again: ");
                    continue;  // go back to the top and ask for input again
                }
                organizer.initiateTeamFormation(teamSize);
                System.out.println("✅ Team formation started with team size: " + teamSize);
                break;

            } catch (NumberFormatException e) {
                System.out.print("❌ Invalid number. Enter a valid team size: ");
            }
        }
    }


}
