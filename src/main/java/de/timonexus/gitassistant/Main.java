package de.timonexus.gitassistant;

import de.timonexus.gitassistant.ai.OllamaService;
import de.timonexus.gitassistant.cli.CommitCommand;
import de.timonexus.gitassistant.commit.CommitSuggestionService;
import de.timonexus.gitassistant.git.GitService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {

        if (args.length == 0) {
            printHelp();
            return;
        }

        try {
            switch (args[0].toLowerCase()) {

                case "commit" -> executeCommit(args);

                case "help", "--help", "-h" -> {
                    if (args.length > 1) {
                        System.err.println("Der Hilfebefehl akzeptiert keine weiteren Argumente.");
                        return;
                    }
                    printHelp();
                }

                default -> {
                    System.err.println("Unbekannter Befehl: " + args[0]);
                    printHelp();
                }
            }

        } catch (Exception exception) {
            System.err.println("Fehler: " + exception.getMessage());
        }
    }

    private static void executeCommit(String[] args) throws Exception {

        if (args.length > 2) {
            throw new IllegalArgumentException(
                    "Zu viele Argumente. Verwende: commit [Repository-Pfad]"
            );
        }

        Path repositoryPath = args.length == 2
                ? Path.of(args[1])
                : Path.of(".");

        if (!java.nio.file.Files.isDirectory(repositoryPath)) {
            throw new IllegalArgumentException(
                    "Das angegebene Verzeichnis existiert nicht: "
                            + repositoryPath.toAbsolutePath()
            );
        }

        GitService gitService = new GitService(repositoryPath);

        if (!gitService.isGitRepository()) {
            throw new IllegalStateException(
                    "Das angegebene Verzeichnis ist kein Git-Repository: "
                            + repositoryPath.toAbsolutePath()
            );
        }

        if (gitService.getDiff().isBlank()) {
            System.out.println("Keine Änderungen gefunden.");
            System.out.println("Es gibt nichts zu committen.");
            return;
        }

        OllamaService ollamaService = new OllamaService();

        CommitSuggestionService suggestionService =
                new CommitSuggestionService(ollamaService, gitService);

        BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in)
        );

        CommitCommand command = new CommitCommand(
                gitService,
                suggestionService,
                input,
                System.out
        );

        command.execute();
    }

    private static void printHelp() {

        System.out.println("""
                Git Assistant
                ----------------------------

                Verfügbare Befehle:

                commit [Pfad]    Commit-Nachricht generieren
                help             Hilfe anzeigen

                Beispiele:

                git-assistant commit
                git-assistant commit "C:\\Projekte\\MeinProjekt"
                git-assistant help
                """);
    }
}