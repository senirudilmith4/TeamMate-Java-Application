import java.util.*;


public class TeamBuilder {
    private List<Participant> participants;
    private List<Team> formedTeams;
    private int teamSize;

    public TeamBuilder(List<Participant> participants, int teamSize) {
        this.participants = new ArrayList<>(participants);
        this.formedTeams = new ArrayList<>();
        this.teamSize = teamSize;
    }
}
