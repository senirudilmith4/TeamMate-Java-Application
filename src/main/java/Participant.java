public class Participant {
    private String id;
    private String name;
    private String preferredSport;
    private Role preferredRole;
    private int skillLevel;
    private  int personalityScore;
    private  PersonalityType personalityType;
    private int[] responses;

    // parameterized constructor
    public Participant(String id,String name,String preferredSport,Role preferredRole,int skillLevel,int personalityScore) {
        this.id = id;
        this.name = name;
        this.preferredSport = preferredSport;
        this.preferredRole = preferredRole;
        this.skillLevel = skillLevel;
        this.personalityScore = personalityScore;

    }

    // default constructor
    public Participant(){
        this.id = "P001";
        this.name = "Unknown";
        this.preferredSport = "Unknown";
        this.skillLevel = 0;
    }

    public String getId() {
        return id;
    }
    public int[] getResponses() {
        return responses;
    }
    public void setResponses(int[] responses) {
        this.responses = responses;
    }
    public String getParticipantId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getPreferredSport() {
        return preferredSport;
    }
    public int getSkillLevel() {
        return skillLevel;
    }
    public int getPersonalityScore() {
        return personalityScore;
    }
    public PersonalityType getPersonalityType() {
        return personalityType;
    }
    public Role getPreferredRole(){
        return preferredRole;
    }
    public void setPreferredRole(Role preferredRole) {
        this.preferredRole = preferredRole;
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
    public void setPreferredSport(String preferredSport) {
        if (preferredSport == null||preferredSport.trim().isEmpty()){
            throw new IllegalArgumentException("Preferred sport cannot be empty");
        }
        this.preferredSport = preferredSport;
    }
        public void setSkillLevel(int skillLevel) {
        if (skillLevel < 0||skillLevel > 5){
            throw new IllegalArgumentException("Skill level must be between 0 and 5");
        }
        this.skillLevel = skillLevel;
    }
    public void setPersonalityScore(int personalityScore) {
        this.personalityScore=personalityScore;
    }
    public void setPersonalityType(PersonalityType personalityType) {
        this.personalityType=personalityType;
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sport='" + preferredSport + '\'' +
                ", role='" +preferredRole + '\''+
                ", skill=" + skillLevel + '\''+
                ", personality=" + personalityScore + " (" + personalityType + ")" +
                '}';
    }




}
