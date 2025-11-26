import java.util.Scanner;

public class SurveyProcess {
    private Scanner scanner = new Scanner(System.in);

    public Participant conductSurvey() {
        System.out.println("---- Participant Registration ----");

        System.out.print("Enter Participant Name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Preferred Sport: ");
        String sport = scanner.nextLine();

        System.out.print("Enter Preferred Role (Strategist,\n" +
                "        Attacker,\n" +
                "        Defender,\n" +
                "        Supporter,\n" +
                "        Coordinator): ");
        Role role = null;
        while (role == null) {
            String input = scanner.nextLine().trim();  // get input and trim spaces

            // Try to match with enum values ignoring case
            for (Role r : Role.values()) {
                if (r.name().equalsIgnoreCase(input)) {
                    role = r;
                    break;
                }
            }

            if (role == null) {
                System.out.print("Invalid role. Please enter again: ");
            }

        }

        System.out.print("Enter Skill Level (1-5): ");
        int skillLevel = readNumberInRange(1, 5);

        // Now ask 5 personality questions
        System.out.println("\n---- 5-Question Personality Survey ----");

        int[] responses = new int[5];
        responses[0] = askQuestion("I enjoy taking the lead and guiding others.");
        responses[1] = askQuestion("I prefer analyzing situations.");
        responses[2] = askQuestion("I work well with others.");
        responses[3] = askQuestion("I am calm under pressure.");
        responses[4] = askQuestion("I make quick decisions.");

        // Create participant
        Participant p = new Participant();
        p.setName(name);
        p.setPreferredSport(sport);
        p.setPreferredRole(role);
        p.setSkillLevel(skillLevel);
        p.setResponses(responses);

        return p;
    }

    private int askQuestion(String question) {
        System.out.println(question);
        System.out.print("Rate (1-5): ");
        return readNumberInRange(1, 5);
    }

    private int readNumberInRange(int min, int max) {
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine());
                if (value >= min && value <= max) return value;
            } catch (Exception ignored) {}
            System.out.print("Invalid input! Enter a number between " + min + " and " + max + ": ");
        }
    }
}
