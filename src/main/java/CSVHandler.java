import java.util.List;

public class CSVHandler {

    // --- Attributes ---
    private String inputFilePath;
    private String outputFilePath;

    // --- Constructor ---
    public CSVHandler(String inputFilePath, String outputFilePath) {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
    }

    // --- Methods ---
    public List<Participant> loadParticipants() { return null; }
    public boolean validateCSVRow(/*row data*/) { return true; }
    public void saveTeams(List<Team> teams) { }
    public void handleFileErrors(Exception e) { }
}
