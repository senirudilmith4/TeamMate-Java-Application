import java.util.ArrayList;
import java.util.List;

public class Team {
    private List<Participant> members;
    private int maxMembers;

    public Team(int maxMembers) {
        this.maxMembers = maxMembers;
        this.members = new ArrayList<Participant>();
    }

    public List<Participant> getMembers() {
        return members;
    }

    public int getTeamMembers() {
        return members.size();
    }
}
