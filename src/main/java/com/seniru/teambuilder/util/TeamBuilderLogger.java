package com.seniru.teambuilder.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TeamBuilderLogger {

    private final String fileName;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public TeamBuilderLogger(String fileName) {
        this.fileName = fileName;
    }

    public synchronized void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(fmt);
        String line = String.format("%s | %s | %s", timestamp, level, message);

        try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("[TeamBuilderLogger] Error writing to file: " + e.getMessage());
        }
    }
}
