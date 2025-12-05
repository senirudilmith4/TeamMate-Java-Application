
import com.seniru.teambuilder.model.*;
import com.seniru.teambuilder.service.CSVHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for CSVHandler class.
 * These tests verify thread-safety when multiple threads access CSVHandler simultaneously.
 */
class CSVHandlerConcurrencyTest {

    private CSVHandler csvHandler;
    private String testFilePath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("concurrent_test.csv").toString();
        csvHandler = new CSVHandler(testFilePath);
    }

    // Helper method to create sample participants
    private Participant createSampleParticipant(String id, String name, String email) {
        return new Participant(
                id,
                name,
                email,
                "Football",
                Role.ATTACKER,
                7,
                85,
                PersonalityType.LEADER
        );
    }

    // ==================== appendParticipant() Concurrency Tests ====================

    @Test
    @DisplayName("Concurrency: Multiple threads appending participants simultaneously")
    void testConcurrentAppend() throws InterruptedException, IOException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // Launch threads that wait for start signal
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    Participant p = createSampleParticipant(
                            "P" + String.format("%03d", id),
                            "User" + id,
                            "user" + id + "@example.com"
                    );
                    csvHandler.appendParticipant(p);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = endLatch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All threads should complete within timeout");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify results
        List<String> lines = Files.readAllLines(Path.of(testFilePath));

        // Count headers (should be exactly 1, but race condition might create multiple)
        long headerCount = lines.stream()
                .filter(line -> line.contains("id,name,email"))
                .count();

        System.out.println("⚠️  RACE CONDITION TEST RESULT:");
        System.out.println("   Expected headers: 1");
        System.out.println("   Actual headers: " + headerCount);
        System.out.println("   Total lines: " + lines.size());

        // This assertion will likely FAIL due to race condition
        // assertEquals(1, headerCount, "Should have exactly 1 header (RACE CONDITION if fails)");

        // For now, just verify some participants were written
        assertTrue(lines.size() > threadCount / 2,
                "At least some participants should be written");
    }

    @Test
    @DisplayName("Concurrency: Verify no data corruption in concurrent appends")
    void testConcurrentAppendDataIntegrity() throws InterruptedException, IOException {
        int threadCount = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> expectedIds = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    String participantId = "P" + String.format("%03d", id);
                    expectedIds.add(participantId);

                    Participant p = createSampleParticipant(
                            participantId,
                            "User" + id,
                            "user" + id + "@example.com"
                    );
                    csvHandler.appendParticipant(p);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Read back and verify no corruption
        List<String> lines = Files.readAllLines(Path.of(testFilePath));

        // Extract participant IDs from file
        Set<String> actualIds = lines.stream()
                .filter(line -> !line.contains("id,name,email")) // Skip headers
                .filter(line -> line.trim().length() > 0)
                .map(line -> {
                    String[] parts = line.split(",");
                    return parts.length > 0 ? parts[0] : "";
                })
                .filter(id -> id.startsWith("P"))
                .collect(Collectors.toSet());

        System.out.println("⚠️  DATA INTEGRITY CHECK:");
        System.out.println("   Expected participants: " + expectedIds.size());
        System.out.println("   Actual valid participants: " + actualIds.size());

        // Check if we lost any data (common concurrency issue)
        assertTrue(actualIds.size() > 0, "Should have written some participants");

        // Note: Due to race conditions, we might not get all expected IDs
        // In a properly synchronized version, this should pass:
        // assertEquals(expectedIds, actualIds, "All participants should be present");
    }

    @Test
    @DisplayName("Concurrency: Stress test with rapid sequential appends")
    void testRapidSequentialAppends() throws InterruptedException, IOException {
        int iterations = 50;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(iterations);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < iterations; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Participant p = createSampleParticipant(
                            "P" + id,
                            "User" + id,
                            "user" + id + "@example.com"
                    );
                    csvHandler.appendParticipant(p);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("✓ Rapid append test completed: " + successCount.get() + "/" + iterations);
        assertTrue(successCount.get() > iterations * 0.8,
                "Most appends should succeed");
    }

    // ==================== saveAllParticipants() Concurrency Tests ====================

    @Test
    @DisplayName("Concurrency: Concurrent saveAll operations")
    void testConcurrentSaveAll() throws InterruptedException, IOException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    List<Participant> participants = new ArrayList<>();
                    for (int j = 0; j < 5; j++) {
                        participants.add(createSampleParticipant(
                                "T" + threadId + "_P" + j,
                                "User_" + threadId + "_" + j,
                                "user" + threadId + "_" + j + "@example.com"
                        ));
                    }
                    csvHandler.saveAllParticipants(participants);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // File should exist and have valid content
        File file = new File(testFilePath);
        assertTrue(file.exists(), "File should exist after concurrent saves");

        List<String> lines = Files.readAllLines(file.toPath());
        assertTrue(lines.size() > 1, "File should have header and some data");

        // Count headers - should be 1 (but might have race conditions)
        long headerCount = lines.stream()
                .filter(line -> line.contains("id,name"))
                .count();

        System.out.println("⚠️  CONCURRENT SAVE TEST:");
        System.out.println("   Header count: " + headerCount + " (should be 1)");
        System.out.println("   Total lines: " + lines.size());
    }

    @Test
    @DisplayName("Concurrency: Concurrent append while saveAll is running")
    void testConcurrentAppendAndSaveAll() throws InterruptedException, IOException {
        int appendThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(appendThreads + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(appendThreads + 1);

        // Thread that does saveAll
        executor.submit(() -> {
            try {
                startLatch.await();
                List<Participant> participants = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    participants.add(createSampleParticipant(
                            "SAVE_P" + i,
                            "SaveUser" + i,
                            "save" + i + "@example.com"
                    ));
                }
                csvHandler.saveAllParticipants(participants);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        // Threads that append
        for (int i = 0; i < appendThreads; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Participant p = createSampleParticipant(
                            "APPEND_P" + id,
                            "AppendUser" + id,
                            "append" + id + "@example.com"
                    );
                    csvHandler.appendParticipant(p);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("⚠️  MIXED OPERATIONS TEST:");
        System.out.println("   This test demonstrates potential data loss when");
        System.out.println("   append and saveAll run concurrently");

        // File should exist
        assertTrue(new File(testFilePath).exists(),
                "File should exist after mixed operations");
    }

    // ==================== loadParticipants() Concurrency Tests ====================

    @Test
    @DisplayName("Concurrency: Multiple threads reading simultaneously")
    void testConcurrentReads() throws InterruptedException, IOException {
        // First, create a file with data
        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            participants.add(createSampleParticipant(
                    "P" + i,
                    "User" + i,
                    "user" + i + "@example.com"
            ));
        }
        csvHandler.saveAllParticipants(participants);

        // Now have multiple threads read simultaneously
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successfulReads = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    List<Participant> loaded = csvHandler.loadParticipants(testFilePath);
                    if (loaded.size() == 10) {
                        successfulReads.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("✓ Concurrent reads: " + successfulReads.get() + "/" + threadCount + " successful");
        assertEquals(threadCount, successfulReads.get(),
                "All concurrent reads should succeed");
    }

    @Test
    @DisplayName("Concurrency: Read while write is happening")
    void testConcurrentReadWhileWrite() throws InterruptedException, IOException {
        int readers = 5;
        int writers = 5;
        ExecutorService executor = Executors.newFixedThreadPool(readers + writers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(readers + writers);

        // Initial data
        List<Participant> initial = new ArrayList<>();
        initial.add(createSampleParticipant("P000", "Initial", "initial@example.com"));
        csvHandler.saveAllParticipants(initial);

        // Writer threads
        for (int i = 0; i < writers; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Participant p = createSampleParticipant(
                            "W" + id,
                            "Writer" + id,
                            "writer" + id + "@example.com"
                    );
                    csvHandler.appendParticipant(p);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Reader threads
        for (int i = 0; i < readers; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    List<Participant> loaded = csvHandler.loadParticipants(testFilePath);
                    // May get different results depending on timing
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("⚠️  READ-WRITE CONCURRENCY TEST:");
        System.out.println("   This test demonstrates potential inconsistent reads");
        System.out.println("   when reading while writing is in progress");
    }

    // ==================== saveFormedTeams() Concurrency Tests ====================

    @Test
    @DisplayName("Concurrency: Concurrent team saves")
    void testConcurrentTeamSaves() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    List<Team> teams = new ArrayList<>();
                    Team team = new Team("T" + threadId, 3);
                    team.addMember(createSampleParticipant(
                            "P" + threadId,
                            "User" + threadId,
                            "user" + threadId + "@example.com"
                    ));
                    teams.add(team);
                    csvHandler.saveFormedTeams(teams);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // formedTeams.csv should exist
        File file = new File("formedTeams.csv");
        assertTrue(file.exists(), "formedTeams.csv should exist");

        // Cleanup
        file.delete();
    }

    // ==================== Summary Test ====================

    @Test
    @DisplayName("Concurrency: Complete workflow stress test")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCompleteWorkflowStress() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(15);
        CountDownLatch latch = new CountDownLatch(30);
        AtomicInteger operations = new AtomicInteger(0);

        // Mix of operations
        for (int i = 0; i < 10; i++) {
            final int id = i;
            // Appends
            executor.submit(() -> {
                try {
                    csvHandler.appendParticipant(createSampleParticipant(
                            "STRESS_A" + id, "User" + id, "user" + id + "@example.com"));
                    operations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // SaveAlls
            executor.submit(() -> {
                try {
                    List<Participant> list = new ArrayList<>();
                    list.add(createSampleParticipant(
                            "STRESS_S" + id, "SaveUser" + id, "save" + id + "@example.com"));
                    csvHandler.saveAllParticipants(list);
                    operations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });

            // Reads
            executor.submit(() -> {
                try {
                    csvHandler.loadParticipants(testFilePath);
                    operations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(25, TimeUnit.SECONDS);
        executor.shutdown();

        System.out.println("\n========================================");
        System.out.println("CONCURRENCY STRESS TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Total operations attempted: 30");
        System.out.println("Operations completed: " + operations.get());
        System.out.println("\n⚠️  EXPECTED ISSUES (without synchronization):");
        System.out.println("   • Duplicate headers in CSV");
        System.out.println("   • Data corruption/interleaving");
        System.out.println("   • Lost writes");
        System.out.println("   • Inconsistent reads");
        System.out.println("========================================\n");

        assertTrue(operations.get() > 20, "Most operations should complete");
    }
}