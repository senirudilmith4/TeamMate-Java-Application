import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class
Team {
    private String id;
    private final List<Participant> members;
    private final int maxMembers;   // teams cannot be changed after formation

    public Team(String id,int maxMembers) {
        if (maxMembers < 2) {
            throw new IllegalArgumentException("Team size must be at least 2");
        }
        this.id = id;
        this.maxMembers = maxMembers;
        this.members = new ArrayList<>();  // Aggregation
    }

    public boolean addMember(Participant p) {
        if (members.size() >= maxMembers) return false;
        if (members.contains(p)) return false;
        members.add(p);
        return true;
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public List<Participant> getMembers() {
        return new ArrayList<>(members);
    }

    public List<String> getRolesInTeam() {
        return null;
    }

//    public List<String> getInterestsInTeam() {
//        return null;
//    }

    public Map<String, Integer> getPersonalityDistribution() {
        Map<String, Integer> map = new HashMap<>();
        for (Participant p : members) {
            String personality_type = p.getPersonality_type();
            map.put(personality_type,map.getOrDefault(personality_type,0) + 1);
        }
        return map;
    }

    public double getAverageSkill() {
        if (members.isEmpty()) return 0;
        return members.stream()
                .mapToInt(Participant::getSkill_level)
                .average().orElse(0);
    }

    public String getTeamInfo() {
        return "";
    }
}
