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

public class AppController {
    private static List<Participant> participants = new ArrayList<>();
    private static List<Team> formedTeams = new ArrayList<>();
    private SurveyProcess survey = new SurveyProcess();
    private PersonalityClassifier classifier = new PersonalityClassifier();
    private CSVHandler csvHandler = new CSVHandler();
    Scanner scanner = new Scanner(System.in);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);



    public List<Participant> getParticipants() {
        return participants;
    }


    public void loadAllParticipantsAtStart() {
        System.out.println("Enter file path: ");
        String path = scanner.nextLine();
        participants = csvHandler.loadParticipants(path);
        System.out.println("Participants loaded: " + participants.size()); }


//    public void completeSurvey() {
//        try {
//            Participant p = survey.conductSurvey(); // conducts survey
//            classifier.classifyParticipant(p);      // classify personality
//            participants.add(p);                     // store participant
//            csvHandler.appendParticipant(p);         // save to CSV
//
//            System.out.println("Survey completed successfully for: " + p.getName());
//
//        } catch (SurveyException e) {
//            System.err.println("Survey failed: " + e.getMessage());
//            // Log the error or take appropriate action
//        } catch (Exception e) {
//            System.err.println("Unexpected error during survey: " + e.getMessage());
//            e.printStackTrace();
//        }
//
////        System.out.println("✅ com.seniru.teambuilder.model.Participant added successfully!");
////        System.out.println("   Name: " + p.getName());
////        System.out.println("   Personality: " + p.getPersonalityType());
////        System.out.println("   Total participants: " + participants.size());
////        System.out.println("com.seniru.teambuilder.model.Participant added successfully!");
//    }
    public void completeSurvey() {

        try {
            Participant p = survey.conductSurvey();

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

            // CSV writes complete participant (including personality)
            Future<Void> csvTask = executor.submit(() -> {
                csvHandler.appendParticipant(p);
                return null;
            });

            csvTask.get(10, TimeUnit.SECONDS);
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
    public void formTeams() {


        System.out.print("Enter desired team size (minimum 2): ");
        int teamSize;

        while (true) {
            try {
                teamSize = Integer.parseInt(scanner.nextLine().trim());

                if (teamSize < 2) {
                    System.out.print("❌ Team size must be at least 2. Enter again: ");
                    continue;
                }
                break;

            } catch (NumberFormatException e) {
                System.out.print("❌ Invalid number. Enter a valid team size: ");
            }
        }

        Organizer organizer = new Organizer(teamSize);
        organizer.initiateTeamFormation();  // 🔥 pass team size

        System.out.println("✅ Team formation started with team size: " + teamSize);
    }





}
