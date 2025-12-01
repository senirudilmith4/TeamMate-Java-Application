package com.seniru.teambuilder.service;

import com.seniru.teambuilder.exception.SurveyException;
import com.seniru.teambuilder.model.Participant;
import com.seniru.teambuilder.model.Role;

import java.util.Scanner;

public class SurveyProcess {
    private Scanner scanner;
    private static final int maxAttempts = 3;

    public SurveyProcess() {
        this.scanner = new Scanner(System.in);
    }

    public SurveyProcess(Scanner scanner) {
        this.scanner = scanner;
    }

    public Participant conductSurvey() throws SurveyException {
        try {
            System.out.println("---- Participant Registration ----");

            String name = readName();
            String email = readEmail();
            String sport = readSport();
            Role role = readRole();
            int skillLevel = readSkillLevel();

            // Now ask 5 personality questions
            System.out.println("\n---- 5-Question Personality Survey ----");

            int[] responses = readPersonalityResponses();

            // Create and return participant
            Participant p = new Participant();
            p.setName(name);
            p.setEmail(email);
            p.setPreferredSport(sport);
            p.setPreferredRole(role);
            p.setSkillLevel(skillLevel);
            p.setResponses(responses);

            return p;

        } catch (Exception e) {
            throw new SurveyException("Error conducting survey: " + e.getMessage(), e);
        }
    }


    private String readName() throws SurveyException {
        try {
            System.out.print("Enter Participant Name: ");
            String name = scanner.nextLine().trim();

            if (name.isEmpty()) {
                throw new SurveyException("Name cannot be empty");
            }

            return name;
        } catch (Exception e) {
            if (e instanceof SurveyException) throw (SurveyException) e;
            throw new SurveyException("Error reading name: " + e.getMessage(), e);
        }
    }

    private String readEmail() throws SurveyException {
        try {
            System.out.print("Enter Participant Email: ");
            String email = scanner.nextLine().trim();

            if (email.isEmpty()) {
                throw new SurveyException("Email cannot be empty");
            }

            if (!isValidEmail(email)) {
                throw new SurveyException("Invalid email format");
            }

            return email;
        } catch (Exception e) {
            if (e instanceof SurveyException) throw (SurveyException) e;
            throw new SurveyException("Error reading email: " + e.getMessage(), e);
        }
    }

    private boolean isValidEmail(String email) {
        // Basic email validation pattern
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private String readSport() throws SurveyException {
        try {
            System.out.print("Enter Preferred Sport: ");
            String sport = scanner.nextLine().trim();

            if (sport.isEmpty()) {
                throw new SurveyException("Sport cannot be empty");
            }

            return sport;
        } catch (Exception e) {
            if (e instanceof SurveyException) throw (SurveyException) e;
            throw new SurveyException("Error reading sport: " + e.getMessage(), e);
        }
    }

    private Role readRole() throws SurveyException {
        System.out.print("Enter Preferred Role (Strategist, Attacker, Defender, Supporter, Coordinator): ");

        int attempts = 0;
        Role role = null;

        while (role == null && attempts < maxAttempts) {
            try {
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    throw new IllegalArgumentException("Role cannot be empty");
                }

                // Try to match with enum values ignoring case
                for (Role r : Role.values()) {
                    if (r.name().equalsIgnoreCase(input)) {
                        role = r;
                        break;
                    }
                }

                if (role == null) {
                    attempts++;
                    if (attempts < maxAttempts) {
                        System.out.print("Invalid role. Please enter again (" +
                                (maxAttempts - attempts) + " attempts remaining): ");
                    }
                }

            } catch (Exception e) {
                attempts++;
                if (attempts < maxAttempts) {
                    System.out.print("Error reading input. Please try again: ");
                }
            }
        }

        if (role == null) {
            throw new SurveyException("Maximum retry attempts exceeded for role selection");
        }

        return role;
    }

    private int readSkillLevel() throws SurveyException {
        System.out.print("Enter Skill Level (1-10): ");
        return readNumberInRange(1, 10, "skill level");
    }

    private int[] readPersonalityResponses() throws SurveyException {
        int[] responses = new int[5];
        String[] questions = {
                "I enjoy taking the lead and guiding others.",
                "I prefer analyzing situations.",
                "I work well with others.",
                "I am calm under pressure.",
                "I make quick decisions."
        };

        for (int i = 0; i < questions.length; i++) {
            try {
                responses[i] = askQuestion(questions[i], i + 1);
            } catch (Exception e) {
                throw new SurveyException("Error reading question " + (i + 1) + ": " + e.getMessage(), e);
            }
        }

        return responses;
    }

    private int askQuestion(String question, int questionNumber) throws SurveyException {
        System.out.println("Question " + questionNumber + ": " + question);
        System.out.print("Rate (1-5): ");
        return readNumberInRange(1, 5, "rating for question " + questionNumber);
    }

    private int readNumberInRange(int min, int max, String fieldName) throws SurveyException {
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    throw new IllegalArgumentException("Input cannot be empty");
                }

                int value = Integer.parseInt(input);

                if (value >= min && value <= max) {
                    return value;
                }

                attempts++;
                if (attempts < maxAttempts) {
                    System.out.print("Number must be between " + min + " and " + max +
                            ". Please try again (" + (maxAttempts - attempts) +
                            " attempts remaining): ");
                }

            } catch (NumberFormatException e) {
                attempts++;
                if (attempts < maxAttempts) {
                    System.out.print("Invalid number format. Please enter a valid integer (" +
                            (maxAttempts - attempts) + " attempts remaining): ");
                }
            } catch (Exception e) {
                attempts++;
                if (attempts < maxAttempts) {
                    System.out.print("Error reading input. Please try again: ");
                }
            }
        }

        throw new SurveyException("Maximum retry attempts exceeded for " + fieldName);
    }
}
