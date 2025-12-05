// TeamBuilderTest.java


import com.seniru.teambuilder.model.Participant;
import com.seniru.teambuilder.model.PersonalityType;
import com.seniru.teambuilder.model.Role;
import com.seniru.teambuilder.model.Team;
import com.seniru.teambuilder.service.TeamBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class TeamBuilderTest {

    TeamBuilder builder;
    List<Participant> participants;

    @BeforeEach
    void setUp() {
        // Initialize builder with small team size for testing
        builder = new TeamBuilder(3);

        // Create a small participant list
        participants = Arrays.asList(
                new Participant("Alice", "alice@example.com", "Football",Role.COORDINATOR, 10, 70,PersonalityType.LEADER),
                new Participant("Bob", "bob@example.com", "Football", Role.DEFENDER, 8, 60,PersonalityType.THINKER),
                new Participant("Charlie", "charlie@example.com", "Basketball", Role.STRATEGIST, 6, 65,PersonalityType.BALANCED),
                new Participant("Diana", "diana@example.com", "Tennis", Role.SUPPORTER, 9, 75,PersonalityType.LEADER),
                new Participant("Eve", "Eve", "Volleyball", Role.ATTACKER, 55, 7, PersonalityType.THINKER),
                new Participant("Frank", "Frank", "Basketball", Role.DEFENDER, 68, 7, PersonalityType.BALANCED)

        );
    }

    @Test
    void testBuildTeamsWithConcurrency_createsTeams() throws ExecutionException, InterruptedException {
        List<Team> teams = builder.buildTeamsWithConcurrency(participants);

        assertNotNull(teams, "Teams should not be null");
        assertFalse(teams.isEmpty(), "Teams should be formed");
        int totalParticipantsPlaced = teams.stream().mapToInt(Team::getCurrentSize).sum();
        assertEquals(participants.size(), totalParticipantsPlaced, "All participants should be placed");
    }

    @Test
    void testBuildTeamsWithConcurrency_emptyList_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                builder.buildTeamsWithConcurrency(Collections.emptyList())
        );

        assertEquals("Participant list cannot be empty", exception.getMessage());
    }

    @Test
    void testCanAddToTeam_strictMode_constraints() {
        Team team = new Team("Team-1", 3);

        Participant leader=new Participant("Alice", "alice@example.com", "Football",Role.COORDINATOR, 10, 70,PersonalityType.LEADER);
        Participant thinker=new Participant("Bob", "bob@example.com", "Football", Role.DEFENDER, 8, 60,PersonalityType.THINKER);
        Participant balanced=new Participant("Charlie", "charlie@example.com", "Basketball", Role.STRATEGIST, 6, 65,PersonalityType.BALANCED);
        team.addMember(leader);
        team.addMember(thinker);


        List<Participant> testParticipants = Arrays.asList(leader, thinker, balanced);
        try {
            List<Team> teams = builder.buildTeamsWithConcurrency(testParticipants);
            assertTrue(teams.stream().allMatch(t -> t.getCurrentSize() <= 3), "Teams must respect teamSize constraint");
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    void testPreprocessParticipants_groupsByPersonality() throws Exception {
        // Use reflection to test private method (optional)
        var method = TeamBuilder.class.getDeclaredMethod("preprocessParticipants", List.class);
        method.setAccessible(true);
        List<Participant> preprocessed = (List<Participant>) method.invoke(builder, participants);

        assertNotNull(preprocessed);
        long leaders = preprocessed.stream().filter(p -> p.getPersonalityType() == PersonalityType.LEADER).count();
        long thinkers = preprocessed.stream().filter(p -> p.getPersonalityType() == PersonalityType.THINKER).count();
        long balanced = preprocessed.stream().filter(p -> p.getPersonalityType() == PersonalityType.BALANCED).count();

        assertEquals(2, leaders);
        assertEquals(2, thinkers);
        assertEquals(2, balanced);
    }
}
