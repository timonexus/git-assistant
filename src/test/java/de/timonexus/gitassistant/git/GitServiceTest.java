package de.timonexus.gitassistant.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void constructorShouldRejectNullPath() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new GitService(null)
        );
    }

    @Test
    void constructorShouldNormalizePath() {

        GitService service = new GitService(tempDirectory);

        assertEquals(
                tempDirectory.toAbsolutePath().normalize(),
                service.getRepositoryPath()
        );
    }

    @Test
    void shouldRecognizeGitRepository()
            throws Exception {

        new ProcessBuilder("git", "init")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        GitService service = new GitService(tempDirectory);

        assertTrue(service.isGitRepository());
    }

    @Test
    void shouldRejectNonGitDirectory()
            throws Exception {

        GitService service = new GitService(tempDirectory);

        assertFalse(service.isGitRepository());
    }

    @Test
    void shouldReturnEmptyDiffForCleanRepository()
            throws Exception {

        new ProcessBuilder("git", "init")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder(
                "git", "config", "user.name", "Test"
        )
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder(
                "git", "config", "user.email", "test@example.com"
        )
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        Path testFile = tempDirectory.resolve("test.txt");

        java.nio.file.Files.writeString(testFile, "Initial content");

        new ProcessBuilder("git", "add", ".")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder("git", "commit", "-m", "Initial commit")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        GitService service = new GitService(tempDirectory);

        assertEquals("", service.getDiff());
    }

    @Test
    void shouldDetectUntrackedFiles()
            throws Exception {

        initializeGitRepository();

        Path newFile = tempDirectory.resolve("NewClass.java");

        java.nio.file.Files.writeString(
                newFile,
                "public class NewClass {}"
        );

        GitService service = new GitService(tempDirectory);

        assertTrue(
                service.getChangedFiles().contains("NewClass.java")
        );
    }

    private void initializeGitRepository()
            throws Exception {

        new ProcessBuilder("git", "init")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder(
                "git", "config", "user.name", "Test"
        )
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder(
                "git", "config", "user.email", "test@example.com"
        )
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        Path initialFile = tempDirectory.resolve("initial.txt");

        java.nio.file.Files.writeString(
                initialFile,
                "Initial content"
        );

        new ProcessBuilder("git", "add", ".")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder(
                "git", "commit", "-m", "Initial commit"
        )
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();
    }

    private void initializeGitRepositoryWithoutCommit()
            throws Exception {

        new ProcessBuilder("git", "init")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder(
                "git", "config", "user.name", "Test"
        )
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        new ProcessBuilder(
                "git", "config", "user.email", "test@example.com"
        )
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();
    }

    @Test
    void getDiffShouldIncludeUntrackedFileContent()
            throws Exception {

        initializeGitRepository();

        Path newFile = tempDirectory.resolve("NewClass.java");

        String content = """
                public class NewClass {
                    public void execute() {
                        System.out.println("Hello");
                    }
                }
                """;

        java.nio.file.Files.writeString(newFile, content);

        GitService service = new GitService(tempDirectory);

        String diff = service.getDiff();

        assertTrue(diff.contains("NEW FILE: NewClass.java"));
        assertTrue(diff.contains("public class NewClass"));
        assertTrue(diff.contains("System.out.println"));
    }

    @Test
    void getDiffShouldIncludeStagedChanges()
            throws Exception {

        initializeGitRepository();

        Path file = tempDirectory.resolve("initial.txt");

        java.nio.file.Files.writeString(
                file,
                "Modified content"
        );

        new ProcessBuilder("git", "add", "initial.txt")
                .directory(tempDirectory.toFile())
                .start()
                .waitFor();

        GitService service = new GitService(tempDirectory);

        String diff = service.getDiff();

        assertTrue(diff.contains("Modified content"));
        assertTrue(diff.contains("-Initial content"));
    }

    @Test
    void getDiffShouldIncludeModifiedFileContent()
            throws Exception {

        initializeGitRepository();

        Path file = tempDirectory.resolve("initial.txt");

        java.nio.file.Files.writeString(
                file,
                "Updated without staging"
        );

        GitService service = new GitService(tempDirectory);

        String diff = service.getDiff();

        assertTrue(diff.contains("Updated without staging"));
        assertTrue(diff.contains("-Initial content"));
    }

    @Test
    void createCommitShouldOnlyCommitSelectedFiles()
            throws Exception {

        initializeGitRepository();

        Files.writeString(
                tempDirectory.resolve("First.txt"),
                "First file"
        );

        Files.writeString(
                tempDirectory.resolve("Second.txt"),
                "Second file"
        );

        GitService service = new GitService(tempDirectory);

        service.createCommit(
                "feat: add first file",
                "Add selected file",
                List.of("First.txt")
        );

        String committedFiles = runGit(
                "show", "--pretty=format:", "--name-only", "HEAD"
        );

        String status = runGit("status", "--porcelain");

        assertTrue(committedFiles.contains("First.txt"));
        assertFalse(committedFiles.contains("Second.txt"));
        assertTrue(status.contains("Second.txt"));
    }

    @Test
    void createCommitShouldPreserveExistingStagedChanges()
            throws Exception {

        initializeGitRepository();

        // Modify and stage an existing file.
        Files.writeString(
                tempDirectory.resolve("initial.txt"),
                "Staged modification"
        );

        runGit("add", "initial.txt");

        // Create another file that will be committed.
        Files.writeString(
                tempDirectory.resolve("Selected.txt"),
                "Selected content"
        );

        GitService service = new GitService(tempDirectory);

        service.createCommit(
                "feat: add selected file",
                "",
                List.of("Selected.txt")
        );

        String committedFiles = runGit(
                "show", "--pretty=format:", "--name-only", "HEAD"
        );

        String status = runGit("status", "--porcelain");

        assertTrue(committedFiles.contains("Selected.txt"));
        assertFalse(committedFiles.contains("initial.txt"));

        // The original staging must remain untouched.
        assertTrue(status.contains("initial.txt"));
    }

    @Test
    void createCommitShouldCommitSelectedUntrackedFile()
            throws Exception {

        initializeGitRepository();

        Files.writeString(
                tempDirectory.resolve("NewFile.java"),
                "public class NewFile {}"
        );

        GitService service = new GitService(tempDirectory);

        service.createCommit(
                "feat: add new file",
                "",
                List.of("NewFile.java")
        );

        String committedFiles = runGit(
                "show", "--pretty=format:", "--name-only", "HEAD"
        );

        assertTrue(committedFiles.contains("NewFile.java"));
        assertFalse(runGit("status", "--porcelain")
                .contains("NewFile.java"));
    }

    @Test
    void createCommitShouldRejectInvalidFilePath()
            throws Exception {

        initializeGitRepository();

        GitService service = new GitService(tempDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createCommit(
                        "feat: invalid file",
                        "",
                        List.of("../outside.txt")
                )
        );
    }

    private String runGit(String... arguments)
            throws Exception {

        List<String> command = new java.util.ArrayList<>();

        command.add("git");
        command.addAll(List.of(arguments));

        Process process = new ProcessBuilder(command)
                .directory(tempDirectory.toFile())
                .redirectErrorStream(true)
                .start();

        String output = new String(
                process.getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8
        );

        if (!process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IOException("Git process timed out.");
        }

        if (process.exitValue() != 0) {
            throw new IOException("Git command failed: " + output);
        }

        return output;
    }


    @Test
    void getChangedFilesShouldDetectStagedFilesWithoutHead()
            throws Exception {

        initializeGitRepositoryWithoutCommit();

        Files.writeString(
                tempDirectory.resolve("Initial.java"),
                "public class Initial {}"
        );

        runGit("add", ".");

        GitService service = new GitService(tempDirectory);

        assertTrue(
                service.getChangedFiles().contains("Initial.java")
        );
    }

    @Test
    void getDiffShouldIncludeStagedFilesWithoutHead()
            throws Exception {

        initializeGitRepositoryWithoutCommit();

        Files.writeString(
                tempDirectory.resolve("Initial.java"),
                "public class Initial {}"
        );

        runGit("add", ".");

        GitService service = new GitService(tempDirectory);

        String diff = service.getDiff();

        assertTrue(diff.contains("Initial.java"));
        assertTrue(diff.contains("public class Initial"));
    }

    @Test
    void getSelectedDiffShouldIncludeStagedFilesWithoutHead()
            throws Exception {

        initializeGitRepositoryWithoutCommit();

        Files.writeString(
                tempDirectory.resolve("Initial.java"),
                "public class Initial {}"
        );

        runGit("add", ".");

        GitService service = new GitService(tempDirectory);

        String diff = service.getDiff(List.of("Initial.java"));

        assertTrue(diff.contains("public class Initial"));
    }

    @Test
    void createCommitShouldCreateFirstCommitWithoutHead()
            throws Exception {

        initializeGitRepositoryWithoutCommit();

        Files.writeString(
                tempDirectory.resolve("Initial.java"),
                "public class Initial {}"
        );

        runGit("add", ".");

        GitService service = new GitService(tempDirectory);

        service.createCommit(
                "feat: initialize project",
                "Create initial project files",
                List.of("Initial.java")
        );

        String committedFiles = runGit(
                "show", "--pretty=format:", "--name-only", "HEAD"
        );

        assertTrue(committedFiles.contains("Initial.java"));
        assertEquals("", runGit("status", "--porcelain"));
    }
}