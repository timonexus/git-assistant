package de.timonexus.gitassistant.cli;

import de.timonexus.gitassistant.ai.StructuredResponseGenerator;
import de.timonexus.gitassistant.commit.CommitSuggestionService;
import de.timonexus.gitassistant.git.GitService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CommitCommandTest {

    @TempDir
    Path tempDirectory;

    @Test
    void executeShouldCreateCommitWhenConfirmed()
            throws Exception {

        // Arrange
        initializeGitRepository();

        Files.writeString(
                tempDirectory.resolve("NewFeature.java"),
                "public class NewFeature {}"
        );

        CommitCommand command = createCommand("all\ny\n");

        // Act
        command.execute();

        // Assert
        String commitMessage = runGit("log", "-1", "--pretty=%s");

        assertEquals(
                "feat: Add NewFeature class",
                commitMessage.trim()
        );

        assertTrue(
                runGit("status", "--porcelain").isBlank()
        );
    }

    @Test
    void executeShouldNotCreateCommitWhenRejected()
            throws Exception {

        // Arrange
        initializeGitRepository();

        Files.writeString(
                tempDirectory.resolve("NewFeature.java"),
                "public class NewFeature {}"
        );

        CommitCommand command = createCommand("all\nn\n");

        // Act
        command.execute();

        // Assert
        assertEquals(
                1,
                getCommitCount()
        );

        assertFalse(
                runGit("status", "--porcelain").isBlank()
        );
    }

    @Test
    void executeShouldNotCreateCommitWhenInputIsEmpty()
            throws Exception {

        // Arrange
        initializeGitRepository();

        Files.writeString(
                tempDirectory.resolve("NewFeature.java"),
                "public class NewFeature {}"
        );

        CommitCommand command = createCommand("all\n\n");

        // Act
        command.execute();

        // Assert
        assertEquals(
                1,
                getCommitCount()
        );

        assertFalse(
                runGit("status", "--porcelain").isBlank()
        );
    }

    private CommitCommand createCommand(String userInput) {

        GitService gitService = new GitService(tempDirectory);

        StructuredResponseGenerator fakeAI = (prompt, schema) -> """
                {
                    "category": "feat",
                    "scope": "",
                    "title": "Add NewFeature class",
                    "description": "Added a new Java class.",
                    "reason": "Introduces new functionality."
                }
                """;

        CommitSuggestionService suggestionService =
                new CommitSuggestionService(fakeAI, gitService);

        BufferedReader input = new BufferedReader(
                new StringReader(userInput)
        );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        PrintStream output = new PrintStream(
                outputStream,
                true,
                StandardCharsets.UTF_8
        );

        return new CommitCommand(
                gitService,
                suggestionService,
                input,
                output
        );
    }

    private void initializeGitRepository()
            throws Exception {

        runGit("init");
        runGit("config", "user.name", "Test");
        runGit("config", "user.email", "test@example.com");

        Files.writeString(
                tempDirectory.resolve("initial.txt"),
                "Initial content"
        );

        runGit("add", ".");
        runGit("commit", "-m", "Initial commit");
    }

    private int getCommitCount() throws Exception {

        return Integer.parseInt(
                runGit("rev-list", "--count", "HEAD").trim()
        );
    }

    private String runGit(String... arguments)
            throws Exception {

        String[] command = new String[arguments.length + 1];
        command[0] = "git";

        System.arraycopy(
                arguments,
                0,
                command,
                1,
                arguments.length
        );

        Process process = new ProcessBuilder(command)
                .directory(tempDirectory.toFile())
                .redirectErrorStream(true)
                .start();

        String result;

        try (InputStreamReader reader = new InputStreamReader(
                process.getInputStream(),
                StandardCharsets.UTF_8
        )) {
            result = reader.readAllAsString();
        }

        int exitCode = process.waitFor();

        assertEquals(
                0,
                exitCode,
                "Git command failed: " + result
        );

        return result;
    }

    @Test
    void executeShouldOnlyCommitSelectedFiles()
            throws Exception {

        // Arrange
        initializeGitRepository();

        Files.writeString(
                tempDirectory.resolve("First.java"),
                "public class First {}"
        );

        Files.writeString(
                tempDirectory.resolve("Second.java"),
                "public class Second {}"
        );

        CommitCommand command = createCommand("1\ny\n");

        // Act
        command.execute();

        // Assert
        String committedFiles = runGit(
                "show",
                "--pretty=format:",
                "--name-only",
                "HEAD"
        );

        assertTrue(committedFiles.contains("First.java"));
        assertFalse(committedFiles.contains("Second.java"));

        assertFalse(
                runGit("status", "--porcelain").isBlank()
        );
    }
}