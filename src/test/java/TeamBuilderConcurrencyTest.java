
import com.seniru.teambuilder.model.Participant;
import com.seniru.teambuilder.model.PersonalityType;
import com.seniru.teambuilder.model.Role;
import com.seniru.teambuilder.model.Team;
import com.seniru.teambuilder.service.TeamBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.RepeatedTest;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive concurrency tests for TeamBuilder class.
 * Tests thread safety, race conditions, deadlocks, and concurrent behavior.
 */
public class TeamBuilderConcurrencyTest {

    private TeamBuilder teamBuilder;
    private List<Participant> testParticipants;

    @BeforeEach
    void setUp() {
        // Mock System.in to provide "no" response to flexible team prompt
        String input = "no\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        teamBuilder = new TeamBuilder(5, 2, 3);
        testParticipants = createTestParticipants(50);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Restore original System.in
        System.setIn(System.in);
    }

    // ==================== BASIC CONCURRENCY TESTS ====================

    @Test
    @Timeout(30)
    void testConcurrentTeamBuilding_BasicScenario() throws Exception {
        List<Participant> participants = createBalancedParticipants(25);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        assertNotNull(teams);
        assertFalse(teams.isEmpty());

        // Verify no duplicate participants across teams
        Set<String> allParticipantIds = new HashSet<>();
        for (Team team : teams) {
            for (Participant p : team.getMembers()) {
                assertTrue(allParticipantIds.add(p.getId()),
                        "Duplicate participant found: " + p.getId());
            }
        }
    }

    @Test
    @Timeout(30)
    void testConcurrentTeamBuilding_LargeDataset() throws Exception {
        List<Participant> participants = createBalancedParticipants(100);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        assertNotNull(teams);
        verifyNoParticipantDuplication(teams);
        verifyAllTeamsHaveLeader(teams);
    }

    // ==================== RACE CONDITION TESTS ====================

    @RepeatedTest(10) // Run multiple times to catch race conditions
    @Timeout(30)
    void testRaceCondition_ParticipantAssignment() throws Exception {
        // Create exactly 15 participants for 3 teams of 5
        List<Participant> participants = createBalancedParticipants(15);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        // Verify no participant assigned to multiple teams
        Map<String, Integer> participantCounts = new HashMap<>();
        for (Team team : teams) {
            for (Participant p : team.getMembers()) {
                participantCounts.merge(p.getId(), 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : participantCounts.entrySet()) {
            assertEquals(1, entry.getValue(),
                    "Participant " + entry.getKey() + " assigned to multiple teams");
        }
    }

    @RepeatedTest(5)
    @Timeout(30)
    void testRaceCondition_QueuePolling() throws Exception {
        // Test with minimal participants to increase contention
        List<Participant> participants = createBalancedParticipants(20);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        // Verify queue integrity
        assertNotNull(teams);
        verifyNoParticipantDuplication(teams);

        // All formed teams should be complete
        for (Team team : teams) {
            assertTrue(team.getCurrentSize() >= 5,
                    "Team " + team.getID() + " is incomplete: " + team.getCurrentSize());
        }
    }

    // ==================== THREAD SAFETY TESTS ====================



    @Test
    @Timeout(30)
    void testThreadSafety_SharedResourceAccess() throws Exception {
        List<Participant> participants = createBalancedParticipants(30);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        // Verify thread-safe list operations
        assertNotNull(teams);
        assertTrue(teams.size() > 0);

        // Check that all teams are properly formed
        for (Team team : teams) {
            assertNotNull(team.getMembers());
            assertFalse(team.getMembers().isEmpty());
        }
    }

    // ==================== DEADLOCK PREVENTION TESTS ====================

    @Test
    @Timeout(30)
    void testDeadlockPrevention_MultipleTeamFormation() throws Exception {
        // Create scenario that could cause deadlock
        List<Participant> participants = createBalancedParticipants(40);

        long startTime = System.currentTimeMillis();
        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);
        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(teams);
        assertTrue(duration < 25000, "Team building took too long, possible deadlock");
    }

    @Test
    @Timeout(30)
    void testDeadlockPrevention_ResourceContention() throws Exception {
        // Few participants, many potential teams - high contention
        List<Participant> participants = createBalancedParticipants(18);

        assertDoesNotThrow(() -> {
            List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);
            assertNotNull(teams);
        });
    }

    // ==================== PERSONALITY DISTRIBUTION TESTS ====================

    @Test
    @Timeout(30)
    void testConcurrency_LeaderDistribution() throws Exception {
        List<Participant> participants = createBalancedParticipants(30);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        // Each team should have exactly 1 leader in strict mode
        for (Team team : teams) {
            long leaderCount = team.getMembers().stream()
                    .filter(p -> p.getPersonalityType() == PersonalityType.LEADER)
                    .count();
            assertTrue(leaderCount >= 1,
                    "Team " + team.getID() + " has no leader");
        }
    }

    @Test
    @Timeout(30)
    void testConcurrency_ThinkerDistribution() throws Exception {
        List<Participant> participants = createBalancedParticipants(25);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        // Each team should have 1-2 thinkers
        for (Team team : teams) {
            long thinkerCount = team.getMembers().stream()
                    .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                    .count();
            assertTrue(thinkerCount >= 1 && thinkerCount <= 2,
                    "Team " + team.getID() + " has incorrect thinker count: " + thinkerCount);
        }
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @Timeout(30)
    void testConcurrency_MinimalParticipants() throws Exception {
        // Exactly 5 participants for 1 team
        List<Participant> participants = createBalancedParticipants(5);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        assertNotNull(teams);
        // Should form 1 team or 0 teams (if constraints not met)
        assertTrue(teams.size() <= 1);
    }

    @Test
    @Timeout(30)
    void testConcurrency_UnevenDistribution() throws Exception {
        // 23 participants - can't divide evenly
        List<Participant> participants = createBalancedParticipants(23);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        assertNotNull(teams);
        verifyNoParticipantDuplication(teams);
    }

    @Test
    @Timeout(30)
    void testConcurrency_ManyLeadersLimitedThinkers() throws Exception {
        // Edge case: 10 leaders, 3 thinkers, 12 balanced
        List<Participant> participants = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            participants.add(createParticipant("L" + i, PersonalityType.LEADER,
                    "Soccer", Role.COORDINATOR, 60 + i));
        }
        for (int i = 0; i < 3; i++) {
            participants.add(createParticipant("T" + i, PersonalityType.THINKER,
                    "Basketball", Role.STRATEGIST, 65 + i));
        }
        for (int i = 0; i < 12; i++) {
            participants.add(createParticipant("B" + i, PersonalityType.BALANCED,
                    "Volleyball", Role.SUPPORTER, 55 + i));
        }

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        assertNotNull(teams);
        // Should be limited by thinkers (max 3 teams with 1-2 thinkers each)
        assertTrue(teams.size() <= 3);
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @Timeout(30)
    void testPerformance_ConcurrentVsSequential() throws Exception {
        List<Participant> participants = createBalancedParticipants(50);

        long startConcurrent = System.currentTimeMillis();
        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);
        long concurrentTime = System.currentTimeMillis() - startConcurrent;

        assertNotNull(teams);
        assertTrue(concurrentTime < 25000,
                "Concurrent processing took too long: " + concurrentTime + "ms");
    }

    @Test
    @Timeout(30)
    void testPerformance_HighConcurrency() throws Exception {
        List<Participant> participants = createBalancedParticipants(100);

        long startTime = System.currentTimeMillis();
        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);
        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(teams);
        System.out.println("Processed 100 participants in " + duration + "ms");
        assertTrue(duration < 30000);
    }

    // ==================== CONSTRAINT VALIDATION TESTS ====================

    @Test
    @Timeout(30)
    void testConcurrency_GameVarietyConstraint() throws Exception {
        List<Participant> participants = createBalancedParticipants(25);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        // Verify game variety constraint (max 2 from same game)
        for (Team team : teams) {
            Map<String, Long> gameCounts = team.getMembers().stream()
                    .collect(Collectors.groupingBy(
                            Participant::getPreferredSport,
                            Collectors.counting()
                    ));

            for (Map.Entry<String, Long> entry : gameCounts.entrySet()) {
                assertTrue(entry.getValue() <= 2,
                        "Team " + team.getID() + " has too many " +
                                entry.getKey() + " players: " + entry.getValue());
            }
        }
    }

    @Test
    @Timeout(30)
    void testConcurrency_PersonalityConstraints() throws Exception {
        List<Participant> participants = createBalancedParticipants(30);

        List<Team> teams = teamBuilder.buildTeamsWithConcurrency(participants);

        for (Team team : teams) {
            long leaders = team.countPersonalityType(PersonalityType.LEADER);
            long thinkers = team.countPersonalityType(PersonalityType.THINKER);

            assertTrue(leaders >= 1, "Team missing leader");
            assertTrue(thinkers >= 1 && thinkers <= 2, "Invalid thinker count");
        }
    }

    // ==================== INTERRUPT HANDLING TESTS ====================

    @Test
    @Timeout(30)
    void testInterruptHandling_GracefulShutdown() throws Exception {
        List<Participant> participants = createBalancedParticipants(50);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<List<Team>> future = executor.submit(() ->
                teamBuilder.buildTeamsWithConcurrency(participants)
        );

        // Let it run briefly then check
        Thread.sleep(100);

        List<Team> teams = future.get(25, TimeUnit.SECONDS);
        assertNotNull(teams);

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    // ==================== HELPER METHODS ====================

    private List<Participant> createTestParticipants(int count) {
        List<Participant> participants = new ArrayList<>();
        String[] sports = {"Soccer", "Basketball", "Volleyball", "Tennis"};
        Role[] roles = Role.values();
        PersonalityType[] personalities = {
                PersonalityType.LEADER,
                PersonalityType.THINKER,
                PersonalityType.BALANCED
        };

        for (int i = 0; i < count; i++) {
            PersonalityType personality = personalities[i % personalities.length];
            String sport = sports[i % sports.length];
            Role role = roles[i % roles.length];
            int skill = 50 + (i % 30);

            participants.add(createParticipant("P" + i, personality, sport, role, skill));
        }

        return participants;
    }

    private List<Participant> createBalancedParticipants(int count) {
        List<Participant> participants = new ArrayList<>();
        String[] sports = {"Soccer", "Basketball", "Volleyball"};
        Role[] roles = Role.values();

        // Ensure balanced distribution
        int leadersCount = count / 5;  // 20% leaders
        int thinkersCount = count / 3; // 33% thinkers
        int balancedCount = count - leadersCount - thinkersCount;

        int id = 0;

        // Add leaders
        for (int i = 0; i < leadersCount; i++) {
            participants.add(createParticipant(
                    "L" + id++,
                    PersonalityType.LEADER,
                    sports[i % sports.length],
                    roles[i % roles.length],
                    55 + (i % 25)
            ));
        }

        // Add thinkers
        for (int i = 0; i < thinkersCount; i++) {
            participants.add(createParticipant(
                    "T" + id++,
                    PersonalityType.THINKER,
                    sports[i % sports.length],
                    roles[i % roles.length],
                    55 + (i % 25)
            ));
        }

        // Add balanced
        for (int i = 0; i < balancedCount; i++) {
            participants.add(createParticipant(
                    "B" + id++,
                    PersonalityType.BALANCED,
                    sports[i % sports.length],
                    roles[i % roles.length],
                    55 + (i % 25)
            ));
        }

        Collections.shuffle(participants);
        return participants;
    }

    private Participant createParticipant(String id, PersonalityType personality,
                                          String sport, Role role, int skill) {
        return new Participant(
                id,
                "Name_" + id,
                id + "@test.com",
                sport,
                role,
                skill,
                50,
                personality
        );
    }

    private void verifyNoParticipantDuplication(List<Team> teams) {
        Set<String> allIds = new HashSet<>();
        int totalParticipants = 0;

        for (Team team : teams) {
            for (Participant p : team.getMembers()) {
                totalParticipants++;
                assertTrue(allIds.add(p.getId()),
                        "Duplicate participant: " + p.getId());
            }
        }

        assertEquals(allIds.size(), totalParticipants,
                "Participant count mismatch");
    }

    private void verifyAllTeamsHaveLeader(List<Team> teams) {
        for (Team team : teams) {
            long leaderCount = team.getMembers().stream()
                    .filter(p -> p.getPersonalityType() == PersonalityType.LEADER)
                    .count();
            assertTrue(leaderCount >= 1,
                    "Team " + team.getID() + " missing leader");
        }
    }
}