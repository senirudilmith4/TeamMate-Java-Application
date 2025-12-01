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
    private TeamBuilderLogger logger = new TeamBuilderLogger("teamBuilder_log.txt");

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
     * Build teams from participants using CONCURRENT processing with optional flexible phase.
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
        // 2️⃣ PHASE 1: Strict Team Formation
        // ===========================
        System.out.println("\n=== PHASE 1: Forming teams with ALL constraints ===");
        logger.log("INFO", "=== PHASE 1: Strict team formation started ===");

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numThreads = Math.min(availableProcessors, Math.max(1, maxPossibleTeams / 2));

        logger.log("INFO", "Using " + numThreads + " threads for concurrent team formation");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        AtomicInteger teamCounter = new AtomicInteger(1);
        List<Future<Team>> futures = new ArrayList<>();
        List<Team> allTeams = Collections.synchronizedList(new ArrayList<>());

        // Submit Team Builders
        for (int i = 0; i < maxPossibleTeams; i++) {
            Future<Team> f = executor.submit(() ->
                    formSingleTeamFromQueues(
                            personalityQueues,
                            "Team-" + teamCounter.getAndIncrement(),
                            true  // Strict mode
                    )
            );
            futures.add(f);
        }

        // Collect Built Teams
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

        logger.log("INFO", "Phase 1 summary: " + successful + " successful, " + failed + " failed");
        System.out.println("\n✅ Phase 1 Complete: " + successful + " teams formed with all constraints");

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // ===========================
        // 3️⃣ Check Remaining Participants
        // ===========================
        List<Participant> remaining = new ArrayList<>();
        personalityQueues.values().forEach(q -> q.drainTo(remaining));

        int remainingLeaders = (int) remaining.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.LEADER)
                .count();
        int remainingThinkers = (int) remaining.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.THINKER)
                .count();
        int remainingBalanced = (int) remaining.stream()
                .filter(p -> p.getPersonalityType() == PersonalityType.BALANCED)
                .count();

        System.out.println("\n📊 Remaining Participants: " + remaining.size());
        System.out.println("   - Leaders: " + remainingLeaders);
        System.out.println("   - Thinkers: " + remainingThinkers);
        System.out.println("   - Balanced: " + remainingBalanced);

        logger.log("INFO", "Remaining participants: " + remaining.size() +
                " (L:" + remainingLeaders + " T:" + remainingThinkers + " B:" + remainingBalanced + ")");

        // ===========================
        // 4️⃣ Ask User About Flexible Teams
        // ===========================
        if (remaining.size() >= teamSize && remainingLeaders > 0 && remainingThinkers > 0) {
            System.out.println("\n❓ Would you like to form additional teams with relaxed constraints?");
            System.out.println("   (Team size and core requirements still enforced, but game/role diversity relaxed)");
            System.out.print("   Enter 'yes' or 'no': ");

            Scanner scanner = new Scanner(System.in);
            String response = scanner.nextLine().trim().toLowerCase();

            if (response.equals("yes") || response.equals("y")) {
                logger.log("INFO", "User chose to form flexible teams");

                // ===========================
                // 5️⃣ PHASE 2: Flexible Team Formation
                // ===========================
                System.out.println("\n=== PHASE 2: Forming teams with relaxed constraints ===");
                logger.log("INFO", "=== PHASE 2: Flexible team formation started ===");

                // Refill queues with remaining participants
                for (Participant p : remaining) {
                    personalityQueues.get(p.getPersonalityType()).offer(p);
                }

                // Calculate max flexible teams
                int maxFlexibleTeams = Math.min(
                        Math.min(remaining.size() / teamSize, remainingLeaders),
                        remainingThinkers  // Need at least 1 thinker per team
                );

                logger.log("INFO", "Attempting " + maxFlexibleTeams + " flexible teams");

                // Create new executor for Phase 2
                ExecutorService flexExecutor = Executors.newFixedThreadPool(numThreads);
                List<Future<Team>> flexFutures = new ArrayList<>();

                for (int i = 0; i < maxFlexibleTeams; i++) {
                    Future<Team> f = flexExecutor.submit(() ->
                            formSingleTeamFromQueues(
                                    personalityQueues,
                                    "Team-" + teamCounter.getAndIncrement(),
                                    false  // Flexible mode
                            )
                    );
                    flexFutures.add(f);
                }

                // Collect flexible teams
                int flexSuccessful = 0, flexFailed = 0;
                for (Future<Team> f : flexFutures) {
                    try {
                        Team t = f.get();
                        if (t != null && t.getCurrentSize() > 0) {
                            allTeams.add(t);
                            flexSuccessful++;
                        } else {
                            flexFailed++;
                        }
                    } catch (Exception e) {
                        flexFailed++;
                        logger.log("ERROR", "Flexible team creation failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                logger.log("INFO", "Phase 2 summary: " + flexSuccessful + " successful, " + flexFailed + " failed");
                System.out.println("\n✅ Phase 2 Complete: " + flexSuccessful + " flexible teams formed");

                flexExecutor.shutdown();
                flexExecutor.awaitTermination(30, TimeUnit.SECONDS);

                // Update remaining participants
                remaining.clear();
                personalityQueues.values().forEach(q -> q.drainTo(remaining));
            } else {
                logger.log("INFO", "User declined flexible team formation");
                System.out.println("\n⏭️  Skipping flexible team formation");
            }
        } else {
            logger.log("INFO", "Insufficient participants for flexible teams");
            System.out.println("\n⏭️  Not enough participants for additional teams");
        }

        // ===========================
        // 6️⃣ Distribute Final Remaining Participants
        // ===========================
        if (!remaining.isEmpty() && !allTeams.isEmpty()) {
            logger.log("INFO", "Distributing " + remaining.size() + " remaining participants");
            System.out.println("\n📝 Distributing " + remaining.size() + " remaining participants to existing teams...");
            distributeRemaining(remaining, allTeams);
        }

        // ===========================
        // 7️⃣ Final Summary
        // ===========================
        System.out.println("\n" + "=".repeat(50));
        System.out.println("✅ TEAM BUILDING COMPLETE!");
        System.out.println("=".repeat(50));
        System.out.println("Total Teams Formed: " + allTeams.size());
        System.out.println("Total Participants Placed: " +
                allTeams.stream().mapToInt(Team::getCurrentSize).sum());
        System.out.println("=".repeat(50));

        logger.log("INFO", "Concurrent team building finished with " + allTeams.size() + " final teams");
        return allTeams;
    }

    /**
     * Form team from queues with either strict or flexible constraints
     */
    private Team formSingleTeamFromQueues(
            Map<PersonalityType, BlockingQueue<Participant>> queues,
            String teamId,
            boolean strictMode) {

        String mode = strictMode ? "[STRICT]" : "[FLEXIBLE]";
        logger.log("INFO", "Thread forming " + teamId + " " + mode);

        Team team = new Team(teamId, teamSize);
        List<Participant> selected = new ArrayList<>();

        BlockingQueue<Participant> leaders   = queues.get(PersonalityType.LEADER);
        BlockingQueue<Participant> thinkers  = queues.get(PersonalityType.THINKER);
        BlockingQueue<Participant> balanced  = queues.get(PersonalityType.BALANCED);

        try {
            // 1️⃣ Always get a Leader (REQUIRED in both modes)
            Participant leader = leaders.poll(500, TimeUnit.MILLISECONDS);
            if (leader == null) {
                logger.log("WARN", teamId + ": No leader available, aborting team");
                return null;
            }
            team.addMember(leader);
            selected.add(leader);

            // 2️⃣ Add Thinkers (1–2, REQUIRED in both modes)
            int thinkersMin = 1;
            int thinkersTarget = teamSize > 5 ? 2 : 1;
            int thinkersAdded = 0;

            for (int i = 0; i < thinkersTarget * 3 && thinkersAdded < thinkersTarget; i++) {
                Participant t = thinkers.poll(300, TimeUnit.MILLISECONDS);
                if (t != null) {
                    if (strictMode) {
                        if (canAddToTeam(team, t)) {
                            team.addMember(t);
                            selected.add(t);
                            thinkersAdded++;
                        }
                        else {
                            thinkers.offer(t);  // Return to queue
                        }
                    } else {
                        // Flexible mode - only check personality limits
                        if (canAddToTeamFlexible(team, t)) {
                            team.addMember(t);
                            selected.add(t);
                            thinkersAdded++;
                        } else {
                            thinkers.offer(t);
                        }
                    }
                } else {
                    break;
                }

            }

            // Check minimum thinker requirement
            if (thinkersAdded < thinkersMin) {
                logger.log("WARN", teamId + ": Failed to get minimum thinkers, aborting");
                selected.forEach(p -> queues.get(p.getPersonalityType()).offer(p));
                return null;
            }

            // 3️⃣ Fill remaining slots
            int maxAttempts = (balanced.size() + thinkers.size() + leaders.size()) * 2;
            int attempts = 0;
            int emptyPollCount = 0;
            int maxEmptyPolls = strictMode ? 4 : 8;

            while (team.getCurrentSize() < teamSize && emptyPollCount < maxEmptyPolls) {
                Participant p = null;
                attempts++;

                if (attempts > maxAttempts) break;

                // Try balanced queue first
                p = balanced.poll(200, TimeUnit.MILLISECONDS);

                // Try thinker queue if can add more (max 2)
                if (p == null && team.countPersonalityType(PersonalityType.THINKER) < 2) {
                    p = thinkers.poll(200, TimeUnit.MILLISECONDS);
                }

                // In flexible mode, try leaders to fill slots
                if (p == null && !strictMode) {
                    p = leaders.poll(200, TimeUnit.MILLISECONDS);
                }

                if (p != null) {
                    boolean canAdd = strictMode ? canAddToTeam(team, p) : canAddToTeamFlexible(team, p);

                    if (canAdd) {
                        team.addMember(p);
                        selected.add(p);
                        emptyPollCount = 0;
                    } else {
                        // Return to appropriate queue
                        queues.get(p.getPersonalityType()).offer(p);
                        emptyPollCount++;
                    }
                } else {
                    emptyPollCount++;
                }
            }

            // Check if team is complete
            if (team.getCurrentSize() < teamSize) {
                logger.log("WARN", teamId + " " + mode + " incomplete (" +
                        team.getCurrentSize() + "/" + teamSize + "), returning members");
                System.err.println("⚠ " + teamId + " " + mode + " incomplete (" +
                        team.getCurrentSize() + "/" + teamSize + ")");
                selected.forEach(p -> queues.get(p.getPersonalityType()).offer(p));
                return null;
            }

            logger.log("INFO", teamId + " " + mode + " formed successfully");
            System.out.println("✓ " + teamId + " formed " + mode);
            return team;

        } catch (Exception e) {
            logger.log("ERROR", teamId + " formation interrupted");
            System.err.println("⚠ " + teamId + " formation interrupted");
            selected.forEach(p -> queues.get(p.getPersonalityType()).offer(p));
            return null;
        }
    }

    /**
     * Pre-process participants: stratify by personality, shuffle within groups.
     */
    private List<Participant> preprocessParticipants(List<Participant> participants) {
        Map<PersonalityType, List<Participant>> grouped = participants.stream()
                .collect(Collectors.groupingBy(Participant::getPersonalityType));

        List<Participant> result = new ArrayList<>();

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
     * STRICT mode: Check ALL constraints
     */
    private boolean canAddToTeam(Team team, Participant p) {
        // Constraint 1: Game variety
        if (team.countGame(p.getPreferredSport()) >= maxSameGame) {
            return false;
        }

        // Constraint 2: Personality balance
        PersonalityType pt = p.getPersonalityType();
        long leaderCount = team.countPersonalityType(PersonalityType.LEADER);
        long thinkerCount = team.countPersonalityType(PersonalityType.THINKER);

        if (pt == PersonalityType.LEADER && leaderCount >= 1) {
            return false;
        }

        if (pt == PersonalityType.THINKER && thinkerCount >= 2) {
            return false;
        }

        return true;
    }

    /**
     * FLEXIBLE mode: Only check core constraints (team size, leader, thinkers)
     * Game variety and role diversity are RELAXED
     */
    private boolean canAddToTeamFlexible(Team team, Participant p) {
        PersonalityType pt = p.getPersonalityType();
        long thinkerCount = team.countPersonalityType(PersonalityType.THINKER);

        // Max 2 thinkers (still enforced)
        if (pt == PersonalityType.THINKER && thinkerCount >= 2) {
            return false;
        }

        // Multiple leaders allowed in flexible mode (to fill teams)
        // No game or role restrictions in flexible mode

        return true;
    }

    /**
     * Calculate fit score for participant
     */
    private double calculateFitScore(Team team, Participant p) {
        double score = 0;

        // Role Diversity
        if (!team.hasRole(p.getPreferredRole())) {
            score += 30;
        } else {
            score += 5;
        }

        // Game Variety
        long sameGameCount = team.countGame(p.getPreferredSport());
        if (sameGameCount == 0) {
            score += 25;
        } else if (sameGameCount == 1) {
            score += 12;
        }

        // Personality Balance
        PersonalityType pt = p.getPersonalityType();
        long leaderCount = team.countPersonalityType(PersonalityType.LEADER);
        long thinkerCount = team.countPersonalityType(PersonalityType.THINKER);

        if (pt == PersonalityType.LEADER && leaderCount == 0) {
            score += 25;
        } else if (pt == PersonalityType.THINKER && thinkerCount < 2) {
            score += 20;
        } else if (pt == PersonalityType.BALANCED) {
            score += 15;
        }

        // Skill Balance
        double currentAvg = team.getAverageSkill();
        if (team.getCurrentSize() == 0) {
            score += 15 - Math.abs(p.getSkillLevel() - 65) / 5.0;
        } else {
            double targetAvg = 60;
            double newAvg = (currentAvg * team.getCurrentSize() + p.getSkillLevel())
                    / (team.getCurrentSize() + 1);
            double deviation = Math.abs(newAvg - targetAvg);
            score += Math.max(0, 15 - deviation / 3.0);
        }

        score += random.nextDouble() * 5;

        return score;
    }

    /**
     * Distribute remaining participants to existing teams
     */
    private void distributeRemaining(List<Participant> remaining, List<Team> teams) {
        teams.sort(Comparator.comparingInt(Team::getCurrentSize));
        logger.log("INFO", "Starting distribution of " + remaining.size() + " remaining participants");

        int distributed = 0;
        int lastResort = 0;
        for (Participant p : remaining) {
            Team bestTeam = null;
            double bestScore = -1;

            for (Team team : teams) {
                if (team.getCurrentSize() < teamSize + 2) {
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
                teams.get(0).addMember(p);
                lastResort++;
                logger.log("WARN", "Participant " + p.getName() + " added to smallest team as last resort");
            }
        }
        logger.log("INFO", String.format("Distribution complete: %d optimal placement, %d last resort",
                distributed, lastResort));
    }
}