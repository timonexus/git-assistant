
package de.timonexus.gitassistant.commit;

import de.timonexus.gitassistant.ai.OllamaService;
import de.timonexus.gitassistant.ai.StructuredResponseGenerator;
import de.timonexus.gitassistant.git.GitService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

class CommitSuggestionServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void generateSuggestionShouldReturnValidCommit()
            throws Exception {

        // Arrange
        CommitSuggestionService service =
                new CommitSuggestionService(new OllamaService());

        String diff = """
                diff --git a/ClockService.java b/ClockService.java
                --- a/ClockService.java
                +++ b/ClockService.java
                @@ -1,3 +1,8 @@
                 public class ClockService {
                +    private LocalDate currentDate;
                +
                +    public void updateDate() {
                +        currentDate = LocalDate.now();
                +    }
                 }
                """;

        // Act
        CommitSuggestion result =
                service.generateSuggestion(diff);

        // Assert
        assertNotNull(result);
        assertNotNull(result.category());
        assertNotNull(result.title());
        assertFalse(result.title().isBlank());

        assertTrue(
                java.util.List.of(
                        "feat", "fix", "refactor", "test",
                        "docs", "style", "perf", "build",
                        "ci", "chore"
                ).contains(result.category())
        );

        System.out.println(result);
    }

    @Test
    void generateSuggestionFromGitShouldAnalyzeRepositoryChanges()
            throws Exception {

        // Arrange
        runGit("init");
        runGit("config", "user.name", "Test");
        runGit("config", "user.email", "test@example.com");

        Files.writeString(
                tempDirectory.resolve("initial.txt"),
                "Initial content"
        );

        runGit("add", ".");
        runGit("commit", "-m", "Initial commit");

        // Create an untracked file
        Files.writeString(
                tempDirectory.resolve("NewFeature.java"),
                "public class NewFeature {}"
        );

        GitService gitService = new GitService(tempDirectory);

        StructuredResponseGenerator fakeAI = (prompt, schema) -> {

            assertTrue(prompt.contains("public class NewFeature"));

            return """
                    {
                        "category": "feat",
                        "scope": "",
                        "title": "Add NewFeature class",
                        "description": "Added a new Java class.",
                        "reason": "The class introduces new functionality."
                    }
                    """;
        };

        CommitSuggestionService service =
                new CommitSuggestionService(fakeAI, gitService);

        // Act
        CommitSuggestion result = service.generateSuggestionFromGit();

        // Assert
        assertNotNull(result);
        assertEquals("feat", result.category());
        assertEquals("Add NewFeature class", result.title());
    }

    private void runGit(String... arguments)
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

        String output = new String(
                process.getInputStream().readAllBytes()
        );

        int exitCode = process.waitFor();

        assertEquals(
                0,
                exitCode,
                "Git command failed: " + output
        );
    }
}
