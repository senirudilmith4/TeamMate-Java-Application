

import com.seniru.teambuilder.model.*;
import com.seniru.teambuilder.service.CSVHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CSVHandlerTest {

    private CSVHandler csvHandler;
    private String testFilePath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("test_participants.csv").toString();
        csvHandler = new CSVHandler(testFilePath);
    }

    @AfterEach
    void tearDown() {
        // Cleanup is automatic with @TempDir
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

    // Test saveAllParticipants
    @Test
    @DisplayName("Should save all participants to CSV successfully")
    void testSaveAllParticipants() throws IOException {
        List<Participant> participants = new ArrayList<>();
        participants.add(createSampleParticipant("P001", "John Doe", "john@example.com"));
        participants.add(createSampleParticipant("P002", "Jane Smith", "jane@example.com"));

        csvHandler.saveAllParticipants(participants);

        // Verify file exists
        File file = new File(testFilePath);
        assertTrue(file.exists(), "CSV file should exist");

        // Verify content
        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(3, lines.size(), "Should have header + 2 data rows");
        assertTrue(lines.get(0).contains("id,name"), "Should have correct header");
        assertTrue(lines.get(1).contains("P001"), "Should contain first participant");
        assertTrue(lines.get(2).contains("P002"), "Should contain second participant");
    }

    @Test
    @DisplayName("Should save empty list without errors")
    void testSaveAllParticipantsEmptyList() {
        List<Participant> participants = new ArrayList<>();

        assertDoesNotThrow(() -> csvHandler.saveAllParticipants(participants));

        File file = new File(testFilePath);
        assertTrue(file.exists(), "CSV file should exist even with empty list");
    }

    @Test
    @DisplayName("Should overwrite existing file when saving all participants")
    void testSaveAllParticipantsOverwrite() throws IOException {
        // First save
        List<Participant> firstBatch = new ArrayList<>();
        firstBatch.add(createSampleParticipant("P001", "First", "first@example.com"));
        csvHandler.saveAllParticipants(firstBatch);

        // Second save (should overwrite)
        List<Participant> secondBatch = new ArrayList<>();
        secondBatch.add(createSampleParticipant("P002", "Second", "second@example.com"));
        csvHandler.saveAllParticipants(secondBatch);

        // Verify only second batch exists
        List<String> lines = Files.readAllLines(Path.of(testFilePath));
        assertEquals(2, lines.size(), "Should only have header + 1 row");
        assertFalse(lines.toString().contains("P001"), "Should not contain first participant");
        assertTrue(lines.toString().contains("P002"), "Should contain second participant");
    }

    // Test appendParticipant
    @Test
    @DisplayName("Should append participant to new file with header")
    void testAppendParticipantNewFile() throws IOException {
        Participant p = createSampleParticipant("P001", "John Doe", "john@example.com");

        csvHandler.appendParticipant(p);

        File file = new File(testFilePath);
        assertTrue(file.exists(), "CSV file should be created");

        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(2, lines.size(), "Should have header + 1 data row");
        assertTrue(lines.get(0).contains("id,name,email"), "Should have header");
        assertTrue(lines.get(1).contains("P001,John Doe"), "Should contain participant data");
    }

    @Test
    @DisplayName("Should append participant to existing file without adding header")
    void testAppendParticipantExistingFile() throws IOException {
        // Create file with first participant
        Participant p1 = createSampleParticipant("P001", "First", "first@example.com");
        csvHandler.appendParticipant(p1);

        // Append second participant
        Participant p2 = createSampleParticipant("P002", "Second", "second@example.com");
        csvHandler.appendParticipant(p2);

        // Verify both participants exist and header appears only once
        List<String> lines = Files.readAllLines(Path.of(testFilePath));
        assertEquals(3, lines.size(), "Should have header + 2 data rows");

        long headerCount = lines.stream().filter(line -> line.contains("id,name,email")).count();
        assertEquals(1, headerCount, "Header should appear only once");
    }

    // Test loadParticipants
    @Test
    @DisplayName("Should load participants from CSV successfully")
    void testLoadParticipantsSuccess() throws IOException {
        // Create test CSV file
        String csvContent = """
                id,name,email,preferredSport,skillLevel,preferredRole,personalityScore,personalityType
                P001,John Doe,john@example.com,Football,7,ATTACKER,85,LEADER
                P002,Jane Smith,jane@example.com,Basketball,8,DEFENDER,90,THINKER
                """;
        Files.writeString(Path.of(testFilePath), csvContent);

        List<Participant> participants = csvHandler.loadParticipants(testFilePath);

        assertEquals(2, participants.size(), "Should load 2 participants");

        Participant p1 = participants.get(0);
        assertEquals("P001", p1.getParticipantId());
        assertEquals("John Doe", p1.getName());
        assertEquals("john@example.com", p1.getEmail());
        assertEquals("Football", p1.getPreferredSport());
        assertEquals(7, p1.getSkillLevel());
        assertEquals(Role.ATTACKER, p1.getPreferredRole());
        assertEquals(85, p1.getPersonalityScore());
        assertEquals(PersonalityType.LEADER, p1.getPersonalityType());
    }

    @Test
    @DisplayName("Should return empty list when file does not exist")
    void testLoadParticipantsFileNotExists() {
        String nonExistentFile = tempDir.resolve("nonexistent.csv").toString();

        List<Participant> participants = csvHandler.loadParticipants(nonExistentFile);

        assertNotNull(participants, "Should return non-null list");
        assertTrue(participants.isEmpty(), "Should return empty list");
    }

    @Test
    @DisplayName("Should skip invalid CSV rows")
    void testLoadParticipantsSkipInvalidRows() throws IOException {
        String csvContent = """
                id,name,email,preferredSport,skillLevel,preferredRole,personalityScore,personalityType
                P001,John,john@example.com,Football,7,ATTACKER,85,LEADER
                P002,Invalid,Row
                P003,Jane,jane@example.com,Basketball,8,DEFENDER,90,THINKER
                """;
        Files.writeString(Path.of(testFilePath), csvContent);

        List<Participant> participants = csvHandler.loadParticipants(testFilePath);

        assertEquals(2, participants.size(), "Should load only valid rows");
        assertEquals("P001", participants.get(0).getParticipantId());
        assertEquals("P003", participants.get(1).getParticipantId());
    }

    @Test
    @DisplayName("Should skip rows with invalid role enum")
    void testLoadParticipantsInvalidRole() throws IOException {
        String csvContent = """
                id,name,email,preferredSport,skillLevel,preferredRole,personalityScore,personalityType
                P001,John,john@example.com,Football,7,INVALID_ROLE,85,LEADER
                P002,Jane,jane@example.com,Basketball,8,DEFENDER,90,THINKER
                """;
        Files.writeString(Path.of(testFilePath), csvContent);

        List<Participant> participants = csvHandler.loadParticipants(testFilePath);

        assertEquals(1, participants.size(), "Should skip row with invalid role");
        assertEquals("P002", participants.get(0).getParticipantId());
    }

    @Test
    @DisplayName("Should skip rows with invalid personality type")
    void testLoadParticipantsInvalidPersonalityType() throws IOException {
        String csvContent = """
                id,name,email,preferredSport,skillLevel,preferredRole,personalityScore,personalityType
                P001,John,john@example.com,Football,7,ATTACKER,85,INVALID_TYPE
                P002,Jane,jane@example.com,Basketball,8,DEFENDER,90,THINKER
                """;
        Files.writeString(Path.of(testFilePath), csvContent);

        List<Participant> participants = csvHandler.loadParticipants(testFilePath);

        assertEquals(1, participants.size(), "Should skip row with invalid personality type");
        assertEquals("P002", participants.get(0).getParticipantId());
    }



    @Test
    @DisplayName("Should return empty list when formed teams file does not exist")
    void testLoadFormedTeamsFileNotExists() {
        String nonExistentFile = tempDir.resolve("nonexistent_teams.csv").toString();

        List<Team> teams = csvHandler.loadFormedTeams(nonExistentFile);

        assertNotNull(teams, "Should return non-null list");
        assertTrue(teams.isEmpty(), "Should return empty list");
    }

    @Test
    @DisplayName("Should return empty list when formed teams file is empty")
    void testLoadFormedTeamsEmptyFile() throws IOException {
        String teamsFilePath = tempDir.resolve("empty_teams.csv").toString();
        Files.createFile(Path.of(teamsFilePath));

        List<Team> teams = csvHandler.loadFormedTeams(teamsFilePath);

        assertNotNull(teams, "Should return non-null list");
        assertTrue(teams.isEmpty(), "Should return empty list for empty file");
    }

    // Test saveFormedTeams
    @Test
    @DisplayName("Should save formed teams successfully")
    void testSaveFormedTeamsSuccess() throws IOException {
        List<Team> teams = new ArrayList<>();

        Team team1 = new Team("T001", 3);
        team1.addMember(createSampleParticipant("P001", "John", "john@example.com"));
        team1.addMember(createSampleParticipant("P002", "Jane", "jane@example.com"));
        teams.add(team1);

        Team team2 = new Team("T002", 2);
        team2.addMember(createSampleParticipant("P003", "Bob", "bob@example.com"));
        teams.add(team2);

        csvHandler.saveFormedTeams(teams);

        // Verify file creation
        File file = new File("formedTeams.csv");
        assertTrue(file.exists(), "formedTeams.csv should be created");

        // Verify content
        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(4, lines.size(), "Should have header + 3 participant rows");
        assertTrue(lines.get(0).contains("TeamID"), "Should have correct header");

        // Cleanup
        file.delete();
    }

    @Test
    @DisplayName("Should save empty teams list without errors")
    void testSaveFormedTeamsEmptyList() {
        List<Team> teams = new ArrayList<>();

        assertDoesNotThrow(() -> csvHandler.saveFormedTeams(teams));

        File file = new File("formedTeams.csv");
        assertTrue(file.exists(), "File should be created even with empty list");

        // Cleanup
        file.delete();
    }

    @Test
    @DisplayName("Should handle teams with no members")
    void testSaveFormedTeamsEmptyTeam() throws IOException {
        List<Team> teams = new ArrayList<>();
        teams.add(new Team("T001", 5));

        csvHandler.saveFormedTeams(teams);

        File file = new File("formedTeams.csv");
        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals(1, lines.size(), "Should only have header for empty team");

        // Cleanup
        file.delete();
    }

    // Integration test
    @Test
    @DisplayName("Integration: Save and load participants round-trip")
    void testSaveAndLoadRoundTrip() {
        List<Participant> originalParticipants = new ArrayList<>();
        originalParticipants.add(createSampleParticipant("P001", "Alice", "alice@example.com"));
        originalParticipants.add(createSampleParticipant("P002", "Bob", "bob@example.com"));

        csvHandler.saveAllParticipants(originalParticipants);
        List<Participant> loadedParticipants = csvHandler.loadParticipants(testFilePath);

        assertEquals(originalParticipants.size(), loadedParticipants.size());

        for (int i = 0; i < originalParticipants.size(); i++) {
            Participant original = originalParticipants.get(i);
            Participant loaded = loadedParticipants.get(i);

            assertEquals(original.getParticipantId(), loaded.getParticipantId());
            assertEquals(original.getName(), loaded.getName());
            assertEquals(original.getEmail(), loaded.getEmail());
            assertEquals(original.getPreferredSport(), loaded.getPreferredSport());
            assertEquals(original.getSkillLevel(), loaded.getSkillLevel());
            assertEquals(original.getPreferredRole(), loaded.getPreferredRole());
            assertEquals(original.getPersonalityScore(), loaded.getPersonalityScore());
            assertEquals(original.getPersonalityType(), loaded.getPersonalityType());
        }
    }

    @Test
    @DisplayName("Should handle special characters in participant names")
    void testSpecialCharactersInNames() throws IOException {
        Participant p = new Participant(
                "P001",
                "O'Brien",
                "obrien@example.com",
                "Football",
                Role.ATTACKER,
                7,
                85,
                PersonalityType.LEADER
        );

        csvHandler.appendParticipant(p);
        List<Participant> loaded = csvHandler.loadParticipants(testFilePath);

        assertEquals(1, loaded.size());
        assertEquals("O'Brien", loaded.get(0).getName());
    }
}