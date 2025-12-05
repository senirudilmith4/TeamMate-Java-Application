package com.seniru.teambuilder.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Team {
    private String id;
    private final List<Participant> members;
    private  int maxMembers;   // teams cannot be changed after formation

    public Team(String id,int maxMembers) {
        if (maxMembers < 2) {
            throw new IllegalArgumentException("Team size must be at least 2");
        }
        this.id = id;
        this.maxMembers = maxMembers;
        this.members = new ArrayList<>();
    }

    public Team(String id) {
        this.id=id;
        this.maxMembers=20;
        this.members = new ArrayList<>();
    }

    public boolean addMember(Participant p) {
        if (members.size() >= maxMembers) return false;
        if (members.contains(p)) return false;
        members.add(p);    // team receives an existing participant(Aggregation)
        return true;
    }


    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public List<Participant> getMembers() {
        return new ArrayList<>(members);
    }


    public Map<PersonalityType, Integer> getPersonalityDistribution() {
        Map<PersonalityType, Integer> map = new HashMap<>();
        for (Participant p : members) {
            PersonalityType personalityType = p.getPersonalityType();
            map.put(personalityType,map.getOrDefault(personalityType,0) + 1);
        }
        return map;
    }

    public double getAverageSkill() {
        if (members.isEmpty()) return 0;
        return members.stream()
                .mapToInt(Participant::getSkillLevel)
                .average().orElse(0);
    }

    public String getTeamInfo() {
        return "";
    }

    public int getCurrentSize() {
        return members.size();
    }

    public long countPersonalityType(PersonalityType personalityType) {
        if (personalityType == null) return 0;
        return members.stream()
                .filter(p -> p.getPersonalityType() != null &&
                        p.getPersonalityType()==personalityType)
                .count();
    }

    public long countGame(String gameName) {
        if (gameName == null) return 0;
        return members.stream()
                .filter(p -> p.getPreferredSport() != null &&
                        p.getPreferredSport().equalsIgnoreCase(gameName))
                .count();
    }

    public boolean hasRole(Role role) {
        if (members == null || members.isEmpty()) return false;

        for (Participant p : members) {
            if (p.getPreferredRole() == role) return true;
        }
        return false;
    }

    public String getID(){
        return id;
    }
}
