import java.util.List;

public class Organizer {

    private int teamSize;
    private CSVHandler fileHandler;
    private List<Participant> participants;
    private List<Team> teams;

    //public void startProcess() { }

    // Set team size
    public void setTeamSize(int teamSize) {
        this.teamSize = teamSize;
    }

    public void uploadCSV() { }

    public void initiateTeamFormation() {
        TeamBuilder builder = new TeamBuilder(participants, teamSize);
        teams = builder.build();
    }

    public void exportTeams() { }
}
