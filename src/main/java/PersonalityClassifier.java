public class PersonalityClassifier {

    public static int calculateScore(int q1, int q2, int q3, int q4, int q5) {
        if (!isValidRating(q1) || !isValidRating(q2) || !isValidRating(q3)
                || !isValidRating(q4) || !isValidRating(q5)) {
            throw new IllegalArgumentException(
                    "All ratings must be between 1 and 5");
        }

        int total = q1 + q2 + q3 + q4 + q5;
        return total * 4; // Scale to 100
    }

    private static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    public static String classify(int score) {
        if (score >= 90 && score <= 100) {
            return "Leader";
        } else if (score >= 70 && score <= 89) {
            return "Balanced";
        } else if (score >= 50 && score <= 69) {
            return "Thinker";
        } else if (score >= 20 && score <= 49) {
            return "Normal";
        } else {
            throw new IllegalArgumentException("Invalid personality score");
        }
    }
}

