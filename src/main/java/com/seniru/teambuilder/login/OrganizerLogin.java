package com.seniru.teambuilder.login;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class OrganizerLogin {

    private static Map<String,String> organizers = new HashMap<>();
    static {
        try{
            BufferedReader br = new BufferedReader(new FileReader("resources/organizers.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    organizers.put(parts[0].trim(), parts[1].trim());
                }
            }
        }catch (IOException e) {
            System.out.println("Error loading organizer credentials: " + e.getMessage());
        }

    }
    public static boolean authenticate() {
        if (organizers.isEmpty()) {
            System.out.println("No organizers found. Contact admin.");
            return false;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("===== Organizer Login =====");
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (organizers.containsKey(username) && organizers.get(username).equals(password)) {
            System.out.println("Login successful!\n");
            return true;
        } else {
            System.out.println("Incorrect username or password.\n");
            return false;
        }
    }

}