public class Participant {
    private int participantId;
    private String name;
    private String preferred_sport;
    private int skill_level;
    private int personality_score;
    private String personality_type;

    // parameterized constructor
    public Participant(int participantId,String name,String preferred_sport,int skill_level,int personality_score,String personality_type) {
        this.participantId = participantId;
        this.name = name;
        this.preferred_sport = preferred_sport;
        this.skill_level = skill_level;
        this.personality_score = 0;
        this.personality_type = "pending";
    }

    // default constructor
    public Participant(){
        this.participantId = 0;
        this.name = "Unknown";
        this.preferred_sport = "Unknown";
        this.skill_level = 0;

    }
    public int getParticipantId() {
        return participantId;
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
    public void setParticipantId(int participantId) {
        if (participantId < 0){
            throw new IllegalArgumentException("Participant id cannot be negative");
        }
        this.participantId = participantId;
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

    @Override
    public String toString() {
        return "Participant{" +
                "id=" + participantId +
                ", name='" + name + '\'' +
                ", sport='" + preferred_sport + '\'' +
                ", skill=" + skill_level +
                ", personality=" + personality_score + " (" + personality_type + ")" +
                '}';
    }


}
