import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class AppController {
    static List<Participant> participants = new ArrayList<>();
    private static List<Team> formedTeams = new ArrayList<>();
    private SurveyProcess survey = new SurveyProcess();
    private PersonalityClassifier classifier = new PersonalityClassifier();
    private CSVHandler csvHandler = new CSVHandler();
    Scanner scanner = new Scanner(System.in);



    public void loadAllParticipantsAtStart() {
        System.out.println("Enter file path: ");
        String path = scanner.nextLine();
        participants = csvHandler.loadParticipants(path);
        System.out.println("Participants loaded: " + participants.size()); }


    public void completeSurvey(){
        Participant p = survey.conductSurvey(); // conducts survey
        classifier.classifyParticipant(p);  // classify personality
        participants.add(p); // store participant
        csvHandler.appendParticipant(p);


        // generate ID if needed
       // p.setId(UUID.randomUUID().toString());


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
