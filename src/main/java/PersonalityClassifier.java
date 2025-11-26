public class PersonalityClassifier {

  //Validate that the survey contains exactly 5 responses
  // and each is between 1 and 5.
    public boolean validateResponses(int[] responses) {
        if (responses == null || responses.length != 5) {
            return false;
        }

        for (int r : responses) {
            if (r < 1 || r > 5) {
                return false;
            }
        }
        return true;
    }

    // Compute total score from Q1–Q5
    public int computeTotalScore(int[] responses) {
        int total = 0;
        for (int r : responses) {
            total += r;
        }
        return total;
    }

    // Convert raw score (5–25) to scaled score (20–100)
    public int computeScaledScore(int totalScore) {
        return totalScore * 4;
    }

    // Determine personality type based on scoring rules
    public PersonalityType classifyPersonality(int scaledScore) {
        if (scaledScore >= 90 && scaledScore <= 100) {
            return PersonalityType.LEADER;
        } else if (scaledScore >= 70 && scaledScore <= 89) {
            return PersonalityType.BALANCED;
        } else if (scaledScore >= 50 && scaledScore <= 69) {
            return PersonalityType.THINKER;
        } else if (scaledScore >= 20 && scaledScore <= 49) {
            return PersonalityType.ANALYST;
        } else {
            throw new IllegalArgumentException("Invalid personality score");
        }
    }

    public void classifyParticipant(Participant p) {
        int[] responses = p.getResponses();

        if (!validateResponses(responses)) {
//            p.setPersonalityType("Invalid");
            p.setPersonalityScore(0);
            return;
        }

        int total = computeTotalScore(responses);
        int scaled = computeScaledScore(total);
        PersonalityType type = classifyPersonality(scaled);

        p.setPersonalityScore(scaled);
        p.setPersonalityType(type);
    }


    // High-level method: validate → compute → classify → update participant
//    public void processParticipant(Participant p, int[] responses) {
//        if (!validateResponses(responses)) {
//            p.setPersonality_type("Invalid");
//            p.setPersonality_score(0);
//            return;
//        }
//
//        int total = computeTotalScore(responses);
//        int scaled = computeScaledScore(total);
//        String type = classifyPersonality(scaled);
//
//        p.setPersonality_score(scaled);
//        p.setPersonality_type(type);
//    }


}

