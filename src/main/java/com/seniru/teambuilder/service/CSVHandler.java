package com.seniru.teambuilder.service;

import com.seniru.teambuilder.model.*;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVHandler {

    private static  String filePath = "participants.csv";

    public CSVHandler() {
        this.filePath = "participants.csv";
    }

    public CSVHandler(String filePath) {
        this.filePath = filePath;
    }

    // Save list of participants (overwrite file)
    public void saveAllParticipants(List<Participant> participants) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // write header
            writer.write("id,name,preferredSport,preferredRole,skillLevel,personalityScore,personalityType\n");

            for (Participant p : participants) {
                writer.write(formatParticipant(p) + "\n");
            }

            System.out.println("📁 All participants saved to CSV successfully!");

        } catch (IOException e) {
            System.out.println("❌ Error writing CSV: " + e.getMessage());
        }
    }

    // Append only 1 participant (when adding one)
    public void appendParticipant(Participant p) {
        boolean fileExists = new java.io.File(filePath).exists();

        try (FileWriter writer = new FileWriter(filePath, true)) {

            // write header only if file didn't exist
            if (!fileExists) {
                writer.write("id,name,email,preferredSport,skillLevel,preferredRole,personalityScore,personalityType\n");
            }

            writer.write(formatParticipant(p) + "\n");

            System.out.println("📁 Participant stored in participants.csv successfully!");

        } catch (IOException e) {
            System.out.println("❌ Error writing CSV: " + e.getMessage());
        }
    }

    // Convert Participant → CSV line
    private String formatParticipant(Participant p) {
        return p.getParticipantId() + "," +
                p.getName() + "," +
                p.getEmail() + "," +
                p.getPreferredSport() + "," +
                p.getSkillLevel() + "," +
                p.getPreferredRole() + "," +
                p.getPersonalityScore() + "," +
                p.getPersonalityType();
    }

    public List<Participant> loadParticipants(String filePath) {
        List<Participant> participants = new ArrayList<>();

        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("⚠ No CSV file found. Returning empty list.");
            return participants;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {

                // Skip the header line
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] data = line.split(",");

                // must match your CSV format
                if (data.length != 8) {
                    System.out.println("⚠ Skipping invalid CSV row: " + line);
                    continue;
                }

                Participant p = new Participant();

                p.setParticipantId(data[0]);          // id
                p.setName(data[1]);                   // name
                p.setEmail(data[2]);                  // email
                p.setPreferredSport(data[3]);         // sport

                // Convert preferredRole (String → Enum com.seniru.teambuilder.model.Role)
                try {
                    Role role = Role.valueOf(data[5].toUpperCase());
                    p.setPreferredRole(role);
                } catch (Exception e) {
                    System.out.println("⚠ Invalid role in CSV: " + data[5]);
                    continue;
                }

                p.setSkillLevel(Integer.parseInt(data[4])); // skill level
                p.setPersonalityScore(Integer.parseInt(data[6])); // personality score

                // Convert personalityType (String → Enum com.seniru.teambuilder.model.PersonalityType)
                try {
                    PersonalityType pt = PersonalityType.valueOf(data[7].toUpperCase());
                    p.setPersonalityType(pt);
                } catch (Exception e) {
                    System.out.println("⚠ Invalid personality type in CSV: " + data[6]);
                    continue;
                }

                participants.add(p);
            }

        } catch (IOException e) {
            System.out.println("❌ Error reading CSV: " + e.getMessage());
        }

        System.out.println("📥 Loaded " + participants.size() + " participants from CSV.");
        return participants;
    }

    public List<Team> loadFormedTeams(String filePath) {
        Map<String, Team> teamMap = new HashMap<>();
        File file = new File(filePath);
        if (!file.exists()||file.length() == 0) {
            System.out.println("\uD83D\uDCED No formed teams found. File empty or missing.");
            return new ArrayList<>();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {

                // Skip the header line
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] data = line.split(",");

                // must match your CSV format
                if (data.length != 8) {
                    System.out.println("⚠ Skipping invalid CSV row: " + line);
                    continue;
                }

                String teamId = data[0];
                Team team = teamMap.get(teamId);  //uses the key teamID and returns the object stored
                if (team == null) {
                    team = new Team(teamId);
                    teamMap.put(teamId, team);
                }

                Participant p = new Participant();
                //p.setParticipantId(data[1]);
                p.setName(data[1]);
                p.setEmail(data[2]);
                p.setPreferredSport(data[3]);
                // Convert preferredRole (String → Enum com.seniru.teambuilder.model.Role)
                try{
                    Role role = Role.valueOf(data[5].toUpperCase());
                    p.setPreferredRole(role);
                } catch (Exception e) {
                    System.out.println("⚠ Invalid role in CSV: " + data[5]);
                    continue;
                }

                p.setSkillLevel(Integer.parseInt(data[4])); // skill level
                p.setPersonalityScore(Integer.parseInt(data[6])); // personality score

                try {
                    PersonalityType pt = PersonalityType.valueOf(data[7].toUpperCase());
                    p.setPersonalityType(pt);
                } catch (Exception e) {
                    System.out.println("⚠ Invalid personality type in CSV: " + data[6]);
                    continue;
                }

                team.addMember(p);
            }

        } catch (IOException e) {
            System.out.println("❌ Error reading CSV: " + e.getMessage());
        }

        return new ArrayList<>(teamMap.values());

    }

    public void saveFormedTeams(List<Team> teams) {
        String fileName = "formedTeams.csv";

        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {

            // Write header
            pw.println("TeamID,Name,Email,Sport,Skill,Role,PersonalityScore,PersonalityType");

            // Write each team's members
            for (Team team : teams) {
                for (Participant p : team.getMembers()) {
                    pw.printf("%s,%s,%s,%s,%d,%s,%d,%s%n",
                            team.getID(),
                            p.getName() ,
                            p.getEmail() ,
                            p.getPreferredSport() ,
                            p.getSkillLevel() ,
                            p.getPreferredRole() ,
                            p.getPersonalityScore(),
                            p.getPersonalityType()
                    );
                }
            }

            System.out.println("📁 formedTeams.csv saved successfully!");

        } catch (IOException e) {
            System.out.println("❌ Error writing formedTeams.csv: " + e.getMessage());
        }
    }



}
