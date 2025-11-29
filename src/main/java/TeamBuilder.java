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

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int numTeams = participants.size() / teamSize;

        // Determine optimal thread count
        int numThreads = Math.min(availableProcessors, Math.max(1, numTeams / 2));
        logger.log("INFO", "Using " + numThreads + " threads for concurrent team formation.");


        System.out.println("🔧 Using " + numThreads + " threads for team formation...");

        // Pre-sort participants for better distribution
        List<Participant> sortedPool = preprocessParticipants(participants);
        logger.log("INFO", "Participants preprocessed and sorted by personality type");
        // ✅ FIX: Use BlockingQueue for thread-safe participant access
        BlockingQueue<Participant> participantQueue = new LinkedBlockingQueue<>(sortedPool);

        // ✅ FIX: Thread-safe counter
        AtomicInteger teamCounter = new AtomicInteger(1);

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Team> allTeams = Collections.synchronizedList(new ArrayList<>());

        // ✅ FIX: Calculate team count ONCE before submitting tasks
        int maxPossibleTeams = sortedPool.size() / teamSize;
        logger.log("INFO", "Max possible teams to form: " + maxPossibleTeams);

        // Submit exactly the number of teams we can form
        List<Future<Team>> futures = new ArrayList<>();
        for (int i = 0; i < maxPossibleTeams; i++) {
            Future<Team> future = executor.submit(() -> {
                return formSingleTeamFromQueue(participantQueue,
                        "Team-" + teamCounter.getAndIncrement());
            });
            futures.add(future);

        }

        // Collect all formed teams
        int successfulTeams = 0;
        int failedTeams = 0;
        for (Future<Team> future : futures) {
            try {
                Team team = future.get();
                if (team != null && team.getCurrentSize() > 0) {
                    allTeams.add(team);
                    successfulTeams++;
                }else {
                    failedTeams++;
                }
            } catch (Exception e) {
                failedTeams++;
                logger.log("ERROR", "Thread execution failed: " + e.getMessage());
                System.err.println("⚠ Thread error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        logger.log("INFO", String.format("Team formation complete: %d successful, %d failed",
                successfulTeams, failedTeams));
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        logger.log("INFO", "Thread pool shutdown complete");

        // Handle remaining participants
        List<Participant> remaining = new ArrayList<>();
        participantQueue.drainTo(remaining);

        if (!remaining.isEmpty() && !allTeams.isEmpty()) {
            logger.log("INFO", "Distributing " + remaining.size() + " remaining participants");
            System.out.println("📝 Distributing " + remaining.size() + " remaining participants...");
            distributeRemaining(remaining, allTeams);
        }
        logger.log("INFO", "Concurrent team building finished: " + allTeams.size() + " teams created");
        return allTeams;
    }

    /**
     * ✅ NEW METHOD: Form team from queue (no race conditions)
     */
    private Team formSingleTeamFromQueue(BlockingQueue<Participant> queue, String teamId) {
        logger.log("INFO", "Thread starting formation of " + teamId);
        Team team = new Team(teamId, teamSize);
        List<Participant> selectedMembers = new ArrayList<>();

        try {
            // PHASE 1: Try to get a Leader
            Participant leader = pollMatching(queue, p ->
                    p.getPersonalityType() == PersonalityType.LEADER, 500);

            if (leader != null) {
                team.addMember(leader);
                selectedMembers.add(leader);
            }else {
                logger.log("WARN", teamId + ": No leader found");
            }

            // PHASE 2: Get 1-2 Thinkers
            int thinkersNeeded = teamSize > 5 ? 2 : 1;
            int thinkersAdded = 0;
            for (int i = 0; i < thinkersNeeded && team.getCurrentSize() < teamSize; i++) {
                Participant thinker = pollMatching(queue, p ->
                        p.getPersonalityType() == PersonalityType.THINKER &&
                                canAddToTeam(team, p), 500);

                if (thinker != null) {
                    team.addMember(thinker);
                    selectedMembers.add(thinker);
                    thinkersAdded++;
                }
            }
            logger.log("INFO", teamId + ": Added " + thinkersAdded + "/" + thinkersNeeded + " thinkers");
            // PHASE 3: Fill remaining slots with best fit
            int fillCount = 0;
            while (team.getCurrentSize() < teamSize) {
                Participant best = pollBestFit(queue, team, 500);

                if (best == null) {
                    // Fallback: take any valid participant
                    best = pollMatching(queue, p -> canAddToTeam(team, p), 500);
                }

                if (best == null) {
                    // Can't complete this team
                    logger.log("WARN", teamId + " incomplete (" + team.getCurrentSize() +
                            "/" + teamSize + ") - returning " + selectedMembers.size() + " members to queue");
                    System.err.println("⚠ " + teamId + " incomplete (" + team.getCurrentSize() + "/" + teamSize + ")");
                    System.err.println("⚠ " + teamId + " incomplete (" + team.getCurrentSize() + "/" + teamSize + ")");

                    // Return participants to queue
                    for (Participant member : selectedMembers) {
                        queue.offer(member);
                    }
                    return null;
                }

                team.addMember(best);
                selectedMembers.add(best);
                fillCount++;
            }
            logger.log("INFO", teamId + ": Filled " + fillCount + " remaining slots - team complete");
            System.out.println("✓ " + teamId + " formed successfully");
            return team;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log("ERROR", teamId + " formation interrupted");
            System.err.println("⚠ " + teamId + " formation interrupted");

            // Return participants to queue
            for (Participant member : selectedMembers) {
                queue.offer(member);
            }
            return null;
        }
    }

    /**
     * Poll queue for participant matching condition (thread-safe)
     */
    private Participant pollMatching(BlockingQueue<Participant> queue,
                                     java.util.function.Predicate<Participant> condition,
                                     long timeoutMs) throws InterruptedException {

        List<Participant> checked = new ArrayList<>();
        long startTime = System.currentTimeMillis();


        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                Participant p = queue.poll(10, TimeUnit.MILLISECONDS);
                if (p == null) break;

                if (condition.test(p)) {
                    return p;
                }

                checked.add(p);
            }
            return null;
        } finally {
            // Return non-matching participants to queue
            for (Participant p : checked) {
                queue.offer(p);
            }
        }
    }

    /**
     * Poll queue for best fitting participant (thread-safe)
     */
    private Participant pollBestFit(BlockingQueue<Participant> queue,
                                    Team team,
                                    long timeoutMs) throws InterruptedException {
        List<Participant> candidates = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        // Collect candidates
        while (System.currentTimeMillis() - startTime < timeoutMs && candidates.size() < 10) {
            Participant p = queue.poll(10, TimeUnit.MILLISECONDS);
            if (p == null) break;

            if (canAddToTeam(team, p)) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) return null;

        // Find best fit
        Participant best = candidates.stream()
                .max(Comparator.comparingDouble(p -> calculateFitScore(team, p)))
                .orElse(null);

        // Return others to queue
        for (Participant p : candidates) {
            if (p != best) {
                queue.offer(p);
            }
        }

        return best;
    }

    /**
     * Form a single team using concurrent-safe operations.
     * This method is called by multiple threads simultaneously.
     */
    private synchronized Team formSingleTeamConcurrent(List<Participant> sharedPool, String teamId) {
        if (sharedPool.size() < teamSize) return null;

        Team team = new Team(teamId, teamSize);
        List<Participant> selectedMembers = new ArrayList<>();

        // PHASE 1: Select 1 Leader (CRITICAL for team dynamics)
        Participant leader = findAndRemove(sharedPool, p ->
                p.getPersonalityType() == PersonalityType.LEADER);

        if (leader != null) {
            team.addMember(leader);
            selectedMembers.add(leader);
        }

        // PHASE 2: Select 1-2 Thinkers (strategic minds)
        int thinkersNeeded = teamSize > 5 ? 2 : 1;
        for (int i = 0; i < thinkersNeeded && team.getCurrentSize() < teamSize; i++) {
            Participant thinker = findAndRemove(sharedPool, p ->
                    p.getPersonalityType() == PersonalityType.THINKER &&
                            canAddToTeam(team, p));

            if (thinker != null) {
                team.addMember(thinker);
                selectedMembers.add(thinker);
            }
        }

        // PHASE 3: Fill remaining slots with BEST FIT participants
        while (team.getCurrentSize() < teamSize && !sharedPool.isEmpty()) {
            Participant best = findBestFitAndRemove(sharedPool, team);

            if (best != null) {
                team.addMember(best);
                selectedMembers.add(best);
            } else {
                // Fallback: take any valid participant
                for (int i = 0; i < sharedPool.size(); i++) {
                    Participant fallback = sharedPool.get(i);
                    if (canAddToTeam(team, fallback)) {
                        sharedPool.remove(i);
                        team.addMember(fallback);
                        selectedMembers.add(fallback);
                        break;
                    }
                }
                break;
            }
        }

        // If team formation failed, return participants to pool
        if (team.getCurrentSize() < teamSize) {
            sharedPool.addAll(selectedMembers);
            return null;
        }

        return team;
    }

    /**
     * Build teams using SINGLE-THREADED approach (simpler, for small datasets).
     */
    public List<Team> buildTeams(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participant list cannot be empty");
        }

        List<Participant> pool = preprocessParticipants(participants);
        List<Team> teams = new ArrayList<>();
        int teamCount = 0;

        while (pool.size() >= teamSize) {
            Team team = new Team("Team-" + (++teamCount), teamSize);
            List<Participant> selected = selectOptimalTeamMembers(pool, team);

            if (selected.size() == teamSize) {
                pool.removeAll(selected);
                teams.add(team);
            } else {
                break; // Cannot form more complete teams
            }
        }

        // Distribute remaining participants
        if (!pool.isEmpty() && !teams.isEmpty()) {
            distributeRemaining(pool, teams);
        }

        return teams;
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
     * Select optimal team members using greedy algorithm with constraints.
     */
    private List<Participant> selectOptimalTeamMembers(List<Participant> pool, Team team) {
        List<Participant> selected = new ArrayList<>();
        List<Participant> workingPool = new ArrayList<>(pool);

        // Step 1: Add 1 Leader
        Participant leader = findByPersonality(workingPool, PersonalityType.LEADER);
        if (leader != null) {
            team.addMember(leader);
            selected.add(leader);
            workingPool.remove(leader);
        }

        // Step 2: Add 1-2 Thinkers
        int thinkersTarget = teamSize > 5 ? 2 : 1;
        int thinkersAdded = 0;

        Iterator<Participant> iter = workingPool.iterator();
        while (iter.hasNext() && thinkersAdded < thinkersTarget && team.getCurrentSize() < teamSize) {
            Participant p = iter.next();
            if (p.getPersonalityType() == PersonalityType.THINKER && canAddToTeam(team, p)) {
                team.addMember(p);
                selected.add(p);
                iter.remove();
                thinkersAdded++;
            }
        }

        // Step 3: Fill remaining with best fits
        while (team.getCurrentSize() < teamSize && !workingPool.isEmpty()) {
            Participant best = findBestFit(workingPool, team);

            if (best != null) {
                team.addMember(best);
                selected.add(best);
                workingPool.remove(best);
            } else {
                // No optimal candidate, take first valid one
                boolean added = false;
                for (Participant p : workingPool) {
                    if (canAddToTeam(team, p)) {
                        team.addMember(p);
                        selected.add(p);
                        workingPool.remove(p);
                        added = true;
                        break;
                    }
                }
                if (!added) break; // Cannot add anyone
            }
        }

        return selected;
    }

    /**
     * Check if participant can be added to team (validates ALL constraints).
     */
    private boolean canAddToTeam(Team team, Participant p) {
        // Constraint 1: Game variety (max 2 from same game)
        if (team.countGame(p.getPreferredSport()) >= maxSameGame) {
            return false;
        }

        // Constraint 2: Personality balance
        PersonalityType pt = p.getPersonalityType();
        long leaderCount = team.countPersonalityType(PersonalityType.LEADER);
        long thinkerCount = team.countPersonalityType(PersonalityType.THINKER);

        // Max 1 leader per team (strict)
        if (pt == PersonalityType.LEADER && leaderCount >= 1) {
            return false;
        }

        // Max 2 thinkers per team
        if (pt == PersonalityType.THINKER && thinkerCount >= 2) {
            return false;
        }

        return true;
    }

    /**
     * Find best fitting participant for the team (highest score).
     */
    private Participant findBestFit(List<Participant> candidates, Team team) {
        return candidates.stream()
                .filter(p -> canAddToTeam(team, p))
                .max(Comparator.comparingDouble(p -> calculateFitScore(team, p)))
                .orElse(null);
    }

    /**
     * Thread-safe version: find and REMOVE best fit from shared pool.
     */
    private synchronized Participant findBestFitAndRemove(List<Participant> sharedPool, Team team) {
        Participant best = null;
        double bestScore = -1;
        int bestIndex = -1;

        for (int i = 0; i < sharedPool.size(); i++) {
            Participant p = sharedPool.get(i);
            if (canAddToTeam(team, p)) {
                double score = calculateFitScore(team, p);
                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                    bestIndex = i;
                }
            }
        }

        if (bestIndex >= 0) {
            sharedPool.remove(bestIndex);
        }

        return best;
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
     * Find participant by personality type.
     */
    private Participant findByPersonality(List<Participant> pool, PersonalityType type) {
        List<Participant> matches = pool.stream()
                .filter(p -> p.getPersonalityType() == type)
                .collect(Collectors.toList());

        if (matches.isEmpty()) return null;
        return matches.get(random.nextInt(matches.size()));
    }

    /**
     * Thread-safe: find and remove by personality type.
     */
    private synchronized Participant findAndRemove(List<Participant> sharedPool,
                                                   java.util.function.Predicate<Participant> condition) {
        for (int i = 0; i < sharedPool.size(); i++) {
            if (condition.test(sharedPool.get(i))) {
                return sharedPool.remove(i);
            }
        }
        return null;
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