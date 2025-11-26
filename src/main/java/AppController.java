import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class AppController {
    static List<Participant> participants = new ArrayList<>();
    private static List<Team> formedTeams = new ArrayList<>();
    private SurveyProcess survey = new SurveyProcess();
    private PersonalityClassifier classifier = new PersonalityClassifier();




    public void completeSurvey(){
        Participant p = survey.conductSurvey(); // conducts survey
        classifier.classifyParticipant(p);  // classify personality
        participants.add(p);                // store participant


        // generate ID if needed
       // p.setId(UUID.randomUUID().toString());

        participants.add(p);
        System.out.println("✅ Participant added successfully!");
        System.out.println("   Name: " + p.getName());
        System.out.println("   Personality: " + p.getPersonalityType());
        System.out.println("   Total participants: " + participants.size());
        System.out.println("Participant added successfully!");
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
    public void  formTeams() {
        Organizer organizer = new Organizer();
        organizer.initiateTeamFormation();
    }
//        if (participants.isEmpty()) {
//            System.out.println("No participants available to form teams.");
//            return new ArrayList<>();
//        }
//
//        try {
//            // Create TeamBuilder instance with team size (example: 5 per team)
//            TeamBuilder builder = new TeamBuilder(5); // adjust teamSize as needed
//
//            // Form teams concurrently
//            formedTeams = builder.buildTeamsWithConcurrency(participants);
//
//            // Display teams
//            for (Team team : formedTeams) {
//                System.out.println("\n=== " + //team.get()
//                         " ===");
//                for (Participant p : team.getMembers()) {
//                    System.out.println(" - " + p.getName()
//                            + " | Role: " + p.getPreferredRole()
//                            + " | Game: " + p.getPreferredSport()
//                            + " | Personality: " + p.getPersonalityType()
//                            + " | Skill: " + p.getSkillLevel());
//                }
//            }
//
//        } catch (InterruptedException e) {
//            System.err.println("Team formation was interrupted: " + e.getMessage());
//            Thread.currentThread().interrupt();
//        } catch (ExecutionException e) {
//            System.err.println("Execution error during team formation: " + e.getMessage());
//        } catch (IllegalArgumentException e) {
//            System.err.println("Error: " + e.getMessage());
//        }
//
//        return formedTeams;
//    }

}
