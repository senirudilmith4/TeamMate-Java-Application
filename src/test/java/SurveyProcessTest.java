

import com.seniru.teambuilder.exception.SurveyException;
import com.seniru.teambuilder.model.Participant;
import com.seniru.teambuilder.model.Role;
import com.seniru.teambuilder.service.SurveyProcess;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class SurveyProcessTest {

    // Helper method to create a SurveyProcess with simulated input
    private SurveyProcess createSurveyProcessWithInput(String input) {
        Scanner scanner = new Scanner(new ByteArrayInputStream(input.getBytes()));
        return new SurveyProcess(scanner);
    }

    @Test
    void testConductSurvey_ValidInput() throws SurveyException {
        // Prepare input in order: name, email, sport, role, skill level, 5 personality responses
        String input = String.join("\n",
                "John Doe",                // name
                "john@example.com",        // email
                "Football",                // sport
                "Attacker",                // role
                "7",                       // skill level
                "5", "4", "3", "4", "5"   // personality responses
        );

        SurveyProcess survey = createSurveyProcessWithInput(input);
        Participant p = survey.conductSurvey();

        assertNotNull(p);
        assertEquals("John Doe", p.getName());
        assertEquals("john@example.com", p.getEmail());
        assertEquals("Football", p.getPreferredSport());
        assertEquals(Role.ATTACKER, p.getPreferredRole());
        assertEquals(7, p.getSkillLevel());
        assertArrayEquals(new int[]{5, 4, 3, 4, 5}, p.getResponses());
    }

    @Test
    void testConductSurvey_InvalidEmail_ThrowsSurveyException() {
        String input = String.join("\n",
                "Jane Doe",
                "invalid-email",   // invalid email
                "Basketball",
                "Defender",
                "5",
                "3", "3", "4", "4", "2"
        );

        SurveyProcess survey = createSurveyProcessWithInput(input);

        SurveyException exception = assertThrows(SurveyException.class, survey::conductSurvey);
        assertTrue(exception.getMessage().contains("Invalid email format"));
    }

    @Test
    void testConductSurvey_EmptyName_ThrowsSurveyException() {
        String input = String.join("\n",
                "",                      // empty name
                "jane@example.com",
                "Basketball",
                "Defender",
                "5",
                "3", "3", "4", "4", "2"
        );

        SurveyProcess survey = createSurveyProcessWithInput(input);

        SurveyException exception = assertThrows(SurveyException.class, survey::conductSurvey);
        assertTrue(exception.getMessage().contains("Name cannot be empty"));
    }

    @Test
    void testConductSurvey_InvalidRole_MaxAttemptsExceeded() {
        // provide 3 invalid roles to trigger max attempts
        String input = String.join("\n",
                "Alice",
                "alice@example.com",
                "Tennis",
                "InvalidRole1",
                "InvalidRole2",
                "InvalidRole3",
                "5",  // skill level
                "1", "2", "3", "4", "5"
        );

        SurveyProcess survey = createSurveyProcessWithInput(input);

        SurveyException exception = assertThrows(SurveyException.class, survey::conductSurvey);
        assertTrue(exception.getMessage().contains("Maximum retry attempts exceeded for role selection"));
    }

    @Test
    void testConductSurvey_InvalidSkillLevel_MaxAttemptsExceeded() {
        String input = String.join("\n",
                "Bob",
                "bob@example.com",
                "Cricket",
                "Supporter",
                "0", "11", "abc", // 3 invalid skill level attempts
                "5", // would-be valid input
                "1", "2", "3", "4", "5"
        );

        SurveyProcess survey = createSurveyProcessWithInput(input);

        SurveyException exception = assertThrows(SurveyException.class, survey::conductSurvey);
        assertTrue(exception.getMessage().contains("Maximum retry attempts exceeded for skill level"));
    }
}

