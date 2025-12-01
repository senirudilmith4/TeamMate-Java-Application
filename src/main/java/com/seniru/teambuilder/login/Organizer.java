package com.seniru.teambuilder.login;

import com.seniru.teambuilder.app.AppController;
import com.seniru.teambuilder.model.Participant;
import com.seniru.teambuilder.model.Team;
import com.seniru.teambuilder.service.CSVHandler;
import com.seniru.teambuilder.service.TeamBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Organizer {

    private int teamSize;
    private CSVHandler fileHandler;
    private AppController controller = new AppController();
    private List<Team> formedTeams;

    public Organizer(int teamSize) {
        this.fileHandler = new CSVHandler();
        this.formedTeams = new ArrayList<>();
        this.teamSize = teamSize;
    }

    // Set team size
    public void setTeamSize(int teamSize) {
        this.teamSize = teamSize;
    }

    public void uploadCSV() { }

    public List<Team> initiateTeamFormation() {
        if (controller.getParticipants().isEmpty()) {
            System.out.println("No participants available to form teams.");
            return new ArrayList<>();
        }

        try {
            // Create com.seniru.teambuilder.service.TeamBuilder instance with team size (example: 5 per team)
            TeamBuilder builder = new TeamBuilder(teamSize); // adjust teamSize as needed

            // Form teams concurrently
            formedTeams = builder.buildTeamsWithConcurrency(controller.getParticipants());
            fileHandler.saveFormedTeams(formedTeams);
            // Display teams
            for (Team team : formedTeams) {
                System.out.println("\n=== " + //team.get()
                        " ===");
                for (Participant p : team.getMembers()) {
                    System.out.println(" - " + p.getName()
                            + " | Role: " + p.getPreferredRole()
                            + " | Game: " + p.getPreferredSport()
                            + " | Personality: " + p.getPersonalityType()
                            + " | Skill: " + p.getSkillLevel());
                }
            }

        } catch (InterruptedException e) {
            System.err.println("Team formation was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            System.err.println("Execution error during team formation: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }

        return formedTeams;
    }


    public void exportTeams() { }
}
