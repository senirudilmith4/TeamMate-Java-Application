

import com.seniru.teambuilder.model.Participant;
import com.seniru.teambuilder.model.PersonalityType;
import com.seniru.teambuilder.service.PersonalityClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonalityClassifierTest {

    private PersonalityClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new PersonalityClassifier();
    }

    // Test validateResponses
    @Test
    void testValidateResponses_Valid() {
        int[] responses = {3, 4, 5, 2, 1};
        assertTrue(classifier.validateResponses(responses));
    }

    @Test
    void testValidateResponses_InvalidLength() {
        int[] responses = {1, 2, 3};
        assertFalse(classifier.validateResponses(responses));
    }

    @Test
    void testValidateResponses_InvalidValues() {
        int[] responses = {0, 6, 3, 4, 2};
        assertFalse(classifier.validateResponses(responses));
    }

    // Test computeTotalScore
    @Test
    void testComputeTotalScore() {
        int[] responses = {1, 2, 3, 4, 5};
        assertEquals(15, classifier.computeTotalScore(responses));
    }

    // Test computeScaledScore
    @Test
    void testComputeScaledScore() {
        assertEquals(60, classifier.computeScaledScore(15)); // 15*4=60
    }

    // Test classifyPersonality
    @Test
    void testClassifyPersonality_Leader() {
        assertEquals(PersonalityType.LEADER, classifier.classifyPersonality(95));
    }

    @Test
    void testClassifyPersonality_Balanced() {
        assertEquals(PersonalityType.BALANCED, classifier.classifyPersonality(75));
    }

    @Test
    void testClassifyPersonality_Thinker() {
        assertEquals(PersonalityType.THINKER, classifier.classifyPersonality(60));
    }

    @Test
    void testClassifyPersonality_Analyst() {
        assertEquals(PersonalityType.ANALYST, classifier.classifyPersonality(30));
    }

    @Test
    void testClassifyPersonality_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> classifier.classifyPersonality(10));
    }

    // Test classifyParticipant
    @Test
    void testClassifyParticipant_Valid() {
        Participant p = new Participant();
        p.setResponses(new int[]{5, 5, 5, 5, 5}); // total=25, scaled=100
        classifier.classifyParticipant(p);
        assertEquals(100, p.getPersonalityScore());
        assertEquals(PersonalityType.LEADER, p.getPersonalityType());
    }

    @Test
    void testClassifyParticipant_InvalidResponses() {
        Participant p = new Participant();
        p.setResponses(new int[]{0, 6, 3, 4, 2}); // invalid
        classifier.classifyParticipant(p);
        assertEquals(0, p.getPersonalityScore());
        assertNull(p.getPersonalityType());
    }
}
