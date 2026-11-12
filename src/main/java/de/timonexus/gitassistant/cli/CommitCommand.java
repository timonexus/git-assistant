package de.timonexus.gitassistant.cli;

import de.timonexus.gitassistant.commit.CommitSuggestion;
import de.timonexus.gitassistant.commit.CommitSuggestionService;
import de.timonexus.gitassistant.git.GitService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CommitCommand {

    private final GitService gitService;
    private final CommitSuggestionService suggestionService;
    private final BufferedReader input;
    private final PrintStream output;

    public CommitCommand(
            GitService gitService,
            CommitSuggestionService suggestionService,
            BufferedReader input,
            PrintStream output
    ) {
        this.gitService = gitService;
        this.suggestionService = suggestionService;
        this.input = input;
        this.output = output;
    }

    public void execute()
            throws IOException, InterruptedException {

        output.println("Analysiere Git-Änderungen...");

        List<String> changedFiles = gitService.getChangedFiles();

        if (changedFiles.isEmpty()) {
            output.println("Keine Änderungen gefunden.");
            return;
        }

        List<String> selectedFiles = selectFiles(changedFiles);

        if (selectedFiles.isEmpty()) {
            output.println("Dateiauswahl abgebrochen.");
            return;
        }

        output.println();
        output.println("Analysiere ausgewählte Dateien...");

        CommitSuggestion suggestion =
                suggestionService.generateSuggestionFromGit(selectedFiles);

        String subject = buildSubject(suggestion);

        output.println();
        output.println("Commit-Vorschlag:");
        output.println("------------------------------");
        output.println(subject);
        output.println();
        output.println(suggestion.description());
        output.println();
        output.println("Begründung: " + suggestion.reason());
        output.println("------------------------------");

        output.print("Commit erstellen? [y/N]: ");
        output.flush();

        String confirmation = input.readLine();

        if (!"y".equalsIgnoreCase(confirmation)
                && !"yes".equalsIgnoreCase(confirmation)) {

            output.println("Commit abgebrochen.");
            return;
        }

        gitService.createCommit(
                subject,
                suggestion.description(),
                selectedFiles
        );

        output.println("Commit erfolgreich erstellt.");
    }

    private List<String> selectFiles(List<String> changedFiles)
            throws IOException {

        output.println();
        output.println("Geänderte Dateien:");

        for (int i = 0; i < changedFiles.size(); i++) {
            output.printf("[%d] %s%n", i + 1, changedFiles.get(i));
        }

        output.println();
        output.println("Welche Dateien sollen committet werden?");
        output.println("Dateinummern durch Kommas trennen (z. B. 1,2).");
        output.println("Mit 'all' alle Dateien auswählen.");
        output.print("Auswahl: ");
        output.flush();

        String selection = input.readLine();

        if (selection == null || selection.isBlank()) {
            return List.of();
        }

        if (selection.trim().equalsIgnoreCase("all")) {
            return List.copyOf(changedFiles);
        }

        List<String> selectedFiles = new ArrayList<>();

        try {
            String[] numbers = selection.split(",");

            for (String number : numbers) {

                int index = Integer.parseInt(number.trim()) - 1;

                if (index < 0 || index >= changedFiles.size()) {
                    throw new IllegalArgumentException();
                }

                String file = changedFiles.get(index);

                if (!selectedFiles.contains(file)) {
                    selectedFiles.add(file);
                }
            }

        } catch (IllegalArgumentException exception) {
            output.println("Ungültige Dateiauswahl.");
            return List.of();
        }

        return List.copyOf(selectedFiles);
    }

    private String buildSubject(CommitSuggestion suggestion) {

        String scope = suggestion.scope();

        if (scope == null || scope.isBlank()) {
            return suggestion.category() + ": "
                    + suggestion.title();
        }

        return suggestion.category()
                + "(" + scope + "): "
                + suggestion.title();
    }
}