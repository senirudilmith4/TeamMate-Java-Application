import java.util.ArrayList;
import java.util.List;

public class TeamBuilder {

    // --- Attributes ---
    private List<Participant> participants;
    private int teamSize;

    // --- Constructor ---
    public TeamBuilder(List<Participant> participants, int teamSize) {
        this.participants = new ArrayList<>(participants); // deep copy of participants list
        this.teamSize = teamSize;
    }


    // simple team formation
    public List<Team> build() {
        List<Team> teams = new ArrayList<>();

        preprocess(participants);

        // create required teams
        int numberOfTeams = (int) Math.ceil((double) participants.size() / teamSize);
        for (int i = 0; i < numberOfTeams; i++) {
            teams.add(new Team("Team-" + i, teamSize));
        }
        // Distribute participants one by one to teams
        int teamIndex = 0;
        for (Participant p : participants) {
            Team currentTeam = teams.get(teamIndex);
            if (!currentTeam.addMember(p)) {
                teamIndex++;
                currentTeam = teams.get(teamIndex);
                currentTeam.addMember(p);
            }
            // move to the next team
            teamIndex++;
            if (teamIndex >= teams.size()) teamIndex = 0;
        }
        return teams;
    }

    public void preprocess(List<Participant> participants) {
        participants.sort((a, b) -> b.getSkill_level() - a.getSkill_level());
    }

//    public void setTeamSize(int teamSize) {
//        this.teamSize = teamSize;
//    }
//
//    // --- Core Methods ---
//    public List<Team> formTeams() {
//
//        return null;
//    }
//
//    public List<Team> balanceByRole(List<Participant> list) {
//        // Logic to balance participants in teams by roles
//        return null;
//    }
//
//    public List<Team> balanceByPersonality(List<Team> teams) {
//        // Logic to balance teams based on personality types
//        return null;
//    }
//
//    public List<Team> balanceByInterests(List<Team> teams) {
//
//        return null;
//    }
//
//    public List<Participant> preprocessData(List<Participant> participants) {
//
//        return null;
//    }
//
//    public boolean isTeamBalanced(Team t) {
//
//        return false;
//    }
//
//    public void assignParticipantToBestTeam(Participant p, List<Team> teams) {
//
//    }
//
//    // Optional: method to get the formed teams after calling formTeams
//    public List<Team> getFormedTeams() {
//        return this.formedTeams;
//    }
}
