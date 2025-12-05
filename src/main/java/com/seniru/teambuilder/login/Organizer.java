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


    private CSVHandler fileHandler;
    private AppController controller = new AppController();
    private List<Team> formedTeams;

    public Organizer() {
        this.fileHandler = new CSVHandler();
        this.formedTeams = new ArrayList<>();

    }

    // Set team size


    public List<Team> initiateTeamFormation(int teamSize) {

        try {
            // Create TeamBuilder instance with team size
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


}
