public class Participant {
    private String id;
    private String name;
    private String preferred_sport;
    private String preferred_role;
    private int skill_level;
    private  int personality_score;
    private  String personality_type;

    // parameterized constructor
    public Participant(String id,String name,String preferred_sport,String preferred_role,int skill_level,int personality_score,String personality_type) {
        this.id = id;
        this.name = name;
        this.preferred_sport = preferred_sport;
        this.preferred_role = preferred_role;
        this.skill_level = skill_level;
        this.personality_score = 0;
        this.personality_type = "pending";
    }

    // default constructor
//    public Participant(){
//        this.id = "P001";
//        this.name = "Unknown";
//        this.preferred_sport = "Unknown";
//        this.skill_level = 0;
//
//    }
    public String getParticipantId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getPreferred_sport() {
        return preferred_sport;
    }
    public int getSkill_level() {
        return skill_level;
    }
    public int getPersonality_score() {
        return personality_score;
    }
    public String getPersonality_type() {
        return personality_type;
    }
    public String getPreferred_role(){
        return preferred_role;
    }
    public void setPreferred_role(String preferred_role) {
        this.preferred_role = preferred_role;
    }
    public void setParticipantId(String participantId) {
        this.id = participantId;
    }

    public void setName(String name) {
        if (name == null||name.trim().isEmpty()){
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name;
    }
    public void setPreferred_sport(String preferred_sport) {
        if (preferred_sport == null||preferred_sport.trim().isEmpty()){
            throw new IllegalArgumentException("Preferred sport cannot be empty");
        }
        this.preferred_sport = preferred_sport;
    }
    public void setSkill_level(int skill_level) {
        if (skill_level < 0||skill_level > 5){
            throw new IllegalArgumentException("Skill level must be between 0 and 5");
        }
        this.skill_level = skill_level;
    }
    public void setPersonality_score(int personality_score) {
        this.personality_score=personality_score;
    }
     public void setPersonality_type(String personality_type) {
        this.personality_type=personality_type;
     }

    @Override
    public String toString() {
        return "Participant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sport='" + preferred_sport + '\'' +
                ", role='" +preferred_role + '\''+
                ", skill=" + skill_level + '\''+
                ", personality=" + personality_score + " (" + personality_type + ")" +
                '}';
    }


}
