import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class
Team {
    private String id;
    private final List<Participant> members;
    private final int maxMembers;   // teams cannot be changed after formation

    public Team(String id,int maxMembers) {
        this.id = id;
        this.maxMembers = maxMembers;
        this.members = new ArrayList<>();  // Aggregation
    }

    public void addMember(Participant p) { }

    public boolean isFull() {
        return false;
    }

    public List<String> getRolesInTeam() {
        return null;
    }

    public List<String> getInterestsInTeam() {
        return null;
    }

    public Map<String, Integer> getPersonalityDistribution() {
        return null;
    }

    public int getAverageSkill() {
        return 0;
    }

    public String getTeamInfo() {
        return "";
    }
}
