import java.util.ArrayList;
import java.util.List;

public class TeamBuilder {

    // --- Attributes ---
    private List<Participant> participants;
    private List<Team> formedTeams;
    private int teamSize;

    // --- Constructor ---
    public TeamBuilder(List<Participant> participants, int teamSize) {
        this.participants = new ArrayList<>(participants); // deep copy of participants list
        this.formedTeams = new ArrayList<>();
        this.teamSize = teamSize;
    }


    public void setParticipants(List<Participant> participants) {
        this.participants = new ArrayList<>(participants);
    }

    public void setTeamSize(int teamSize) {
        this.teamSize = teamSize;
    }

    // --- Core Methods ---
    public List<Team> formTeams() {

        return null;
    }

    public List<Team> balanceByRole(List<Participant> list) {
        // Logic to balance participants in teams by roles
        return null;
    }

    public List<Team> balanceByPersonality(List<Team> teams) {
        // Logic to balance teams based on personality types
        return null;
    }

    public List<Team> balanceByInterests(List<Team> teams) {

        return null;
    }

    public List<Participant> preprocessData(List<Participant> participants) {

        return null;
    }

    public boolean isTeamBalanced(Team t) {

        return false;
    }

    public void assignParticipantToBestTeam(Participant p, List<Team> teams) {

    }

    // Optional: method to get the formed teams after calling formTeams
    public List<Team> getFormedTeams() {
        return this.formedTeams;
    }
}
