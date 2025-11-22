import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AppController {
    private static List<Participant> participants = new ArrayList<>();
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
        System.out.println("Participant added successfully!");
    }

    public void viewParticipants() {
        if (participants.isEmpty()) {
            System.out.println("No participants have been added yet.");
            return;
        }

        System.out.println("\n---- List of Participants ----");
        for (Participant p : participants) {
            System.out.println(p.getName() + " | Personality: " + p.getPersonality_type());
        }
    }

}
