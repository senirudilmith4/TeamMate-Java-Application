import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Organizer {

    private int teamSize;
    private CSVHandler fileHandler;

    private List<Team> formedTeams;

     //public void startProcess() { }

    // Set team size
    public void setTeamSize(int teamSize) {
        this.teamSize = teamSize;
    }

    public void uploadCSV() { }

    public List<Team> initiateTeamFormation() {
        if (AppController.participants.isEmpty()) {
            System.out.println("No participants available to form teams.");
            return new ArrayList<>();
        }

        try {
            // Create TeamBuilder instance with team size (example: 5 per team)
            TeamBuilder builder = new TeamBuilder(5); // adjust teamSize as needed

            // Form teams concurrently
            formedTeams = builder.buildTeamsWithConcurrency(AppController.participants);

            // Display teams
            for (Team team : formedTeams) {
                System.out.println("\n=== " + //team.get()
                        " ===");
                for (Participant p : team.getMembers()) {
                    System.out.println(" - " + p.getName()
                            + " | Role: " + p.getPreferredRole()
                            + " | Game: " + p.getPreferredSport()
                            + " | Personality: " + p.getPersonalityType()
                            + " | Skill: " + p.getSkillLevel());
                }
            }

        } catch (InterruptedException e) {
            System.err.println("Team formation was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("Execution error during team formation: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }

        return formedTeams;
    }


    public void exportTeams() { }
}
