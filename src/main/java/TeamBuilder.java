import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Builds balanced teams using the matching algorithm with full concurrency support.
 * Implements all matching constraints for game variety, role diversity, personality mix,
 * and skill balance.
 */
public class TeamBuilder {
    private final int teamSize;
    private final int maxSameGame;        // Max players from same game per team
    private final int minRoles;           // Minimum role diversity required
    private final Random random;
    private TeamBuilderLogger  logger = new TeamBuilderLogger("teamBuilder_log.txt");

    // Constructor with defaults
    public TeamBuilder(int teamSize) {
        this(teamSize, 2, teamSize > 5 ? 4 : 3);
    }

    // Full constructor
    public TeamBuilder(int teamSize, int maxSameGame, int minRoles) {
        if (teamSize < 2) throw new IllegalArgumentException("Team size must be >= 2");
        if (maxSameGame < 1) throw new IllegalArgumentException("Max same game must be >= 1");
        if (minRoles < 1) throw new IllegalArgumentException("Min roles must be >= 1");

        this.teamSize = teamSize;
        this.maxSameGame = maxSameGame;
        this.minRoles = minRoles;
        this.random = new Random();
        logger.log("INFO", "TeamBuilder initialized: teamSize=" + teamSize +
                ", maxSameGame=" + maxSameGame + ", minRoles=" + minRoles);
    }

    /**
     * Build teams from participants using CONCURRENT processing.
     * This is the main method that uses threads to form teams in parallel.
     */

    public List<Team> buildTeamsWithConcurrency(List<Participant> participants)
            throws InterruptedException, ExecutionException {

        logger.log("INFO", "Starting concurrent team building with " + participants.size() + " participants");

        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participant list cannot be empty");
        }

        // Pre-sort by personality (leaders first)
        List<Participant> sortedPool = preprocessParticipants(participants);
        logger.log("INFO", "Participants preprocessed and sorted by personality type");

        // ===========================
        // 1️⃣ Personality-Based Queues
        // ===========================
        Map<PersonalityType, BlockingQueue<Participant>> personalityQueues = new HashMap<>();
        personalityQueues.put(PersonalityType.LEADER,   new LinkedBlockingQueue<>());
        personalityQueues.put(PersonalityType.THINKER,  new LinkedBlockingQueue<>());
        personalityQueues.put(PersonalityType.BALANCED, new LinkedBlockingQueue<>());

        // Fill queues
        for (Participant p : sortedPool) {
            personalityQueues.get(p.getPersonalityType()).offer(p);
        }

        int totalLeaders = personalityQueues.get(PersonalityType.LEADER).size();
        int maxPossibleTeams = Math.min(sortedPool.size() / teamSize, totalLeaders);

        logger.log("INFO", "Leaders available: " + totalLeaders);
        logger.log("INFO", "Max possible teams (based on leader constraint): " + maxPossibleTeams);


        // ===========================
        // 2️⃣ Thread Setup
        // ===========================
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numThreads = Math.min(availableProcessors, Math.max(1, maxPossibleTeams / 2));

        logger.log("INFO", "Using " + numThreads + " threads for concurrent team formation");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicInteger teamCounter = new AtomicInteger(1);
        List<Future<Team>> futures = new ArrayList<>();
        List<Team> allTeams = Collections.synchronizedList(new ArrayList<>());


        // ===========================
        // 3️⃣ Submit Team Builders
        // ===========================
        for (int i = 0; i < maxPossibleTeams; i++) {
            Future<Team> f = executor.submit(() ->
                    formSingleTeamFromQueues(
                            personalityQueues,
                            "Team-" + teamCounter.getAndIncrement()
                    )
            );
            futures.add(f);
        }


        // ===========================
        // 4️⃣ Collect Built Teams
        // ===========================
        int successful = 0, failed = 0;

        for (Future<Team> f : futures) {
            try {
                Team t = f.get();
                if (t != null && t.getCurrentSize() > 0) {
                    allTeams.add(t);
                    successful++;
                } else {
                    failed++;
                }

            } catch (Exception e) {
                failed++;
                logger.log("ERROR", "Team creation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        logger.log("INFO", "Team building summary: " + successful + " successful, " + failed + " failed");

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);


        // ===========================
        // 5️⃣ Distribute Remaining Participants
        // ===========================
        List<Participant> remaining = new ArrayList<>();
        personalityQueues.values().forEach(q -> q.drainTo(remaining));

        if (!remaining.isEmpty() && !allTeams.isEmpty()) {
            logger.log("INFO", "Distributing " + remaining.size() + " remaining participants");
            distributeRemaining(remaining, allTeams);
            System.out.println("📝 Distributing " + remaining.size() + " remaining participants...");
        }

        logger.log("INFO", "Concurrent team building finished with " + allTeams.size() + " final teams");
        return allTeams;
    }

    /**
     * ✅ NEW METHOD: Form team from queue (no race conditions)
     */

    private Team formSingleTeamFromQueues(
            Map<PersonalityType, BlockingQueue<Participant>> queues,
            String teamId) {

        logger.log("INFO", "Thread forming " + teamId);

        Team team = new Team(teamId, teamSize);
        List<Participant> selected = new ArrayList<>();

        BlockingQueue<Participant> leaders   = queues.get(PersonalityType.LEADER);
        BlockingQueue<Participant> thinkers  = queues.get(PersonalityType.THINKER);
        BlockingQueue<Participant> balanced  = queues.get(PersonalityType.BALANCED);

        try {
            // 1️⃣ Always get a Leader
            Participant leader = leaders.poll(500, TimeUnit.MILLISECONDS);
            if (leader == null) {
                logger.log("WARN", teamId + ": No leader available, aborting team");
                return null;
            }
            team.addMember(leader);
            selected.add(leader);

            // 2️⃣ Add Thinkers (1–2)
            int thinkersNeeded = teamSize > 5 ? 2 : 1;
            for (int i = 0; i < thinkersNeeded; i++) {
                Participant t = thinkers.poll(300, TimeUnit.MILLISECONDS);
                if (t != null && canAddToTeam(team, t)) {
                    team.addMember(t);
                    selected.add(t);
                }
            }

            // 3️⃣ Fill remaining slots
            int attempts = 0;
            int MAX_ATTEMPTS = balanced.size() * 2;

            while (!team.isFull() && attempts < MAX_ATTEMPTS) {
                attempts++;
                Participant p = balanced.poll();
                if (p == null) break;

                if (canAddToTeam(team, p)) {
                    team.addMember(p);
                } else {
                    balanced.offer(p);
                }


        }

            if (team.getCurrentSize() < teamSize) {
                // Incomplete team → return members to queues
                logger.log("WARN", teamId + " incomplete, returning members");
                System.err.println("⚠ " + teamId + " incomplete (" + team.getCurrentSize() + "/" + teamSize + ")");
                selected.forEach(p -> queues.get(p.getPersonalityType()).offer(p));
                return null;
            }

            logger.log("INFO", teamId + " formed successfully");
            System.out.println("✓ " + teamId + " formed successfully");
            return team;

        } catch (Exception e) {
            logger.log("ERROR", teamId + " formation interrupted");
            System.err.println("⚠ " + teamId + " formation interrupted");

            // return all members
            selected.forEach(p -> queues.get(p.getPersonalityType()).offer(p));

            return null;
        }
    }

    /**
     * Pre-process participants: stratify by personality, shuffle within groups.
     * This ensures better distribution across teams.
     */
    private List<Participant> preprocessParticipants(List<Participant> participants) {
        Map<PersonalityType, List<Participant>> grouped = participants.stream()
                .collect(Collectors.groupingBy(Participant::getPersonalityType));

        List<Participant> result = new ArrayList<>();

        // Shuffle each group and add in order: Leaders, Thinkers, Balanced
        for (PersonalityType type : Arrays.asList(
                PersonalityType.LEADER,
                PersonalityType.THINKER,
                PersonalityType.BALANCED)) {

            List<Participant> group = grouped.getOrDefault(type, new ArrayList<>());
            Collections.shuffle(group, random);
            result.addAll(group);
        }
        logger.log("INFO", String.format("Preprocessed participants: %d leaders, %d thinkers, %d balanced",
                grouped.getOrDefault(PersonalityType.LEADER, new ArrayList<>()).size(),
                grouped.getOrDefault(PersonalityType.THINKER, new ArrayList<>()).size(),
                grouped.getOrDefault(PersonalityType.BALANCED, new ArrayList<>()).size()));
        return result;
    }


    /**
     * Check if participant can be added to team (validates ALL constraints).
     */
    private boolean canAddToTeam(Team team, Participant p) {
        // Constraint 1: Game variety (max 2 from same game)
        if (team.countGame(p.getPreferredSport()) >= maxSameGame) {
            logger.log("DEBUG", String.format("Reject %s for team %s: too many players from game %s",
                    p.getName(), team.getID(), p.getPreferredSport()));
            return false;
        }

        // Constraint 2: Personality balance
        PersonalityType pt = p.getPersonalityType();
        long leaderCount = team.countPersonalityType(PersonalityType.LEADER);
        long thinkerCount = team.countPersonalityType(PersonalityType.THINKER);

        // Max 1 leader per team (strict)
        if (pt == PersonalityType.LEADER && leaderCount >= 1) {
            logger.log("DEBUG", String.format("Reject %s for team %s: leader limit reached", p.getName(), team.getID()));
            return false;
        }

        // Max 2 thinkers per team
        if (pt == PersonalityType.THINKER && thinkerCount >= 2) {
            logger.log("DEBUG", String.format("Reject %s for team %s: thinker limit reached", p.getName(), team.getID()));
            return false;
        }

        return true;
    }

    /**
     * Calculate fit score for a participant to a team (0-100).
     * Higher score = better fit.
     */
    private double calculateFitScore(Team team, Participant p) {
        double score = 0;

        // CRITERION 1: Role Diversity (30 points)
        if (!team.hasRole(p.getPreferredRole())) {
            score += 30; // New role adds significant value
        } else {
            score += 5; // Duplicate role is okay but less valuable
        }

        // CRITERION 2: Game Variety (25 points)
        long sameGameCount = team.countGame(p.getPreferredSport());
        if (sameGameCount == 0) {
            score += 25; // New game is excellent
        } else if (sameGameCount == 1) {
            score += 12; // Second from same game is acceptable
        }
        // sameGameCount >= 2 already filtered out by canAddToTeam

        // CRITERION 3: Personality Balance (25 points)
        PersonalityType pt = p.getPersonalityType();
        long leaderCount = team.countPersonalityType(PersonalityType.LEADER);
        long thinkerCount = team.countPersonalityType(PersonalityType.THINKER);
        long balancedCount = team.countPersonalityType(PersonalityType.BALANCED);

        if (pt == PersonalityType.LEADER && leaderCount == 0) {
            score += 25; // First leader is CRITICAL
        } else if (pt == PersonalityType.THINKER && thinkerCount < 2) {
            score += 20; // Thinkers are valuable (1-2 needed)
        } else if (pt == PersonalityType.BALANCED) {
            score += 15; // Balanced members fill gaps well
        }

        // CRITERION 4: Skill Balance (15 points)
        double currentAvg = team.getAverageSkill();
        if (team.getCurrentSize() == 0) {
            // First member: prefer mid-range skills
            score += 15 - Math.abs(p.getSkillLevel() - 65) / 5.0;
        } else {
            double targetAvg = 60; // Target average skill
            double newAvg = (currentAvg * team.getCurrentSize() + p.getSkillLevel())
                    / (team.getCurrentSize() + 1);
            double deviation = Math.abs(newAvg - targetAvg);
            score += Math.max(0, 15 - deviation / 3.0); // Penalize large deviations
        }

        // CRITERION 5: Randomization Factor (5 points)
        // Ensures fairness when multiple candidates have similar scores
        score += random.nextDouble() * 5;

        return score;
    }


    /**
     * Distribute remaining participants to existing teams.
     * Tries to maintain balance while ensuring no one is left out.
     */
    private void distributeRemaining(List<Participant> remaining, List<Team> teams) {
        // Sort teams by current size (smallest first)
        teams.sort(Comparator.comparingInt(Team::getCurrentSize));
        logger.log("INFO", "Starting distribution of " + remaining.size() + " remaining participants");

        int distributed = 0;
        int lastResort = 0;
        for (Participant p : remaining) {
            // Try to find a team that can accept this participant
            Team bestTeam = null;
            double bestScore = -1;

            for (Team team : teams) {
                if (team.getCurrentSize() < teamSize + 2) { // Allow slight overflow
                    double score = calculateFitScore(team, p);
                    if (score > bestScore) {
                        bestScore = score;
                        bestTeam = team;
                    }
                }
            }

            if (bestTeam != null) {
                bestTeam.addMember(p);
                distributed++;
            } else {
                // Last resort: add to smallest team regardless
                teams.get(0).addMember(p);
                lastResort ++;
                logger.log("WARN", "Participant " + p.getName() + " added to smallest team as last resort");
            }
        }
        logger.log("INFO", String.format("Distribution complete: %d optimal placement, %d last resort",
                distributed, lastResort));
    }
}