
package de.timonexus.gitassistant.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GitService {

    private final Path repositoryPath;

    public GitService(Path repositoryPath) {
        if (repositoryPath == null) {
            throw new IllegalArgumentException("Repository path must not be null.");
        }

        this.repositoryPath = repositoryPath.toAbsolutePath().normalize();
    }

    public boolean isGitRepository() throws IOException, InterruptedException {
        ProcessResult result = executeGit(List.of(
                "rev-parse", "--is-inside-work-tree"
        ));

        return result.exitCode() == 0
                && result.output().trim().equals("true");
    }

    private boolean hasHeadCommit() throws IOException, InterruptedException {
        ProcessResult result = executeGit(List.of(
                "rev-parse", "--verify", "HEAD"
        ));

        return result.exitCode() == 0;
    }

    public List<String> getChangedFiles()
            throws IOException, InterruptedException {

        if (!isGitRepository()) {
            throw new IllegalStateException("The directory is not a Git repository.");
        }

        Set<String> changedFiles = new LinkedHashSet<>();

        if (hasHeadCommit()) {
            ProcessResult tracked = executeGit(List.of(
                    "diff", "--name-only", "-z", "HEAD"
            ));

            requireSuccess(tracked, "Could not retrieve changed files.");
            changedFiles.addAll(parseNullSeparated(tracked.output()));
        } else {
            ProcessResult staged = executeGit(List.of(
                    "diff", "--cached", "--name-only", "-z"
            ));

            requireSuccess(staged, "Could not retrieve staged files.");
            changedFiles.addAll(parseNullSeparated(staged.output()));

            ProcessResult unstaged = executeGit(List.of(
                    "diff", "--name-only", "-z"
            ));

            requireSuccess(unstaged, "Could not retrieve unstaged files.");
            changedFiles.addAll(parseNullSeparated(unstaged.output()));
        }

        ProcessResult untracked = executeGit(List.of(
                "ls-files", "--others", "--exclude-standard", "-z"
        ));

        requireSuccess(untracked, "Could not retrieve untracked files.");
        changedFiles.addAll(parseNullSeparated(untracked.output()));

        return List.copyOf(changedFiles);
    }

    public String getDiff() throws IOException, InterruptedException {
        if (!isGitRepository()) {
            throw new IllegalStateException("The directory is not a Git repository.");
        }

        StringBuilder diff = new StringBuilder();

        if (hasHeadCommit()) {
            ProcessResult result = executeGit(List.of(
                    "diff", "--no-ext-diff", "HEAD"
            ));

            requireSuccess(result, "Git diff failed.");
            diff.append(result.output());
        } else {
            ProcessResult staged = executeGit(List.of(
                    "diff", "--cached", "--no-ext-diff"
            ));

            requireSuccess(staged, "Git staged diff failed.");
            diff.append(staged.output());

            ProcessResult unstaged = executeGit(List.of(
                    "diff", "--no-ext-diff"
            ));

            requireSuccess(unstaged, "Git unstaged diff failed.");
            diff.append(unstaged.output());
        }

        appendUntrackedFiles(diff, getUntrackedFiles());

        return diff.toString();
    }


    public String getDiff(Collection<String> selectedFiles)
            throws IOException, InterruptedException {

        if (!isGitRepository()) {
            throw new IllegalStateException(
                    "The directory is not a Git repository."
            );
        }

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            throw new IllegalArgumentException("No files selected.");
        }

        List<String> changedFiles = getChangedFiles();
        Set<String> selected = new LinkedHashSet<>();

        for (String file : selectedFiles) {
            validateFilePath(file);

            if (!changedFiles.contains(file)) {
                throw new IllegalArgumentException(
                        "File is not changed: " + file
                );
            }

            selected.add(file);
        }

        StringBuilder diff = new StringBuilder();

        if (hasHeadCommit()) {
            List<String> command = new ArrayList<>();
            command.add("--literal-pathspecs");
            command.add("diff");
            command.add("--no-ext-diff");
            command.add("HEAD");
            command.add("--");
            command.addAll(selected);

            ProcessResult result = executeGit(command);
            requireSuccess(result, "Git diff failed.");

            diff.append(result.output());

        } else {
            // Staged changes
            List<String> stagedCommand = new ArrayList<>();
            stagedCommand.add("--literal-pathspecs");
            stagedCommand.add("diff");
            stagedCommand.add("--cached");
            stagedCommand.add("--no-ext-diff");
            stagedCommand.add("--");
            stagedCommand.addAll(selected);

            ProcessResult staged = executeGit(stagedCommand);
            requireSuccess(staged, "Git staged diff failed.");

            diff.append(staged.output());

            // Unstaged changes
            List<String> unstagedCommand = new ArrayList<>();
            unstagedCommand.add("--literal-pathspecs");
            unstagedCommand.add("diff");
            unstagedCommand.add("--no-ext-diff");
            unstagedCommand.add("--");
            unstagedCommand.addAll(selected);

            ProcessResult unstaged = executeGit(unstagedCommand);
            requireSuccess(unstaged, "Git unstaged diff failed.");

            diff.append(unstaged.output());
        }

        // Include content of selected untracked files
        List<String> untrackedFiles = getUntrackedFiles();

        for (String file : selected) {
            if (untrackedFiles.contains(file)) {
                appendFileContent(diff, file, "NEW FILE: ");
            }
        }

        return diff.toString();
    }


    private List<String> getUntrackedFiles()
            throws IOException, InterruptedException {

        ProcessResult result = executeGit(List.of(
                "ls-files", "--others", "--exclude-standard", "-z"
        ));

        requireSuccess(result, "Could not retrieve untracked files.");

        return parseNullSeparated(result.output());
    }

    private void appendUntrackedFiles(
            StringBuilder diff,
            List<String> untrackedFiles
    ) throws IOException {

        for (String file : untrackedFiles) {
            appendFileContent(diff, file, "NEW FILE: ");
        }
    }

    private void appendFileContent(
            StringBuilder diff,
            String file,
            String prefix
    ) throws IOException {

        Path filePath = repositoryPath.resolve(file).normalize();

        if (!filePath.startsWith(repositoryPath)
                || Files.isSymbolicLink(filePath)
                || !Files.isRegularFile(filePath)) {
            return;
        }

        byte[] content = Files.readAllBytes(filePath);

        if (containsBinaryData(content)) {
            return;
        }

        diff.append("\n")
                .append(prefix)
                .append(file)
                .append("\n");

        diff.append(new String(content, StandardCharsets.UTF_8));
        diff.append("\n");
    }

    private boolean containsBinaryData(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return true;
            }
        }

        return false;
    }

    private void validateFilePath(String file) {
        if (file == null || file.isBlank()) {
            throw new IllegalArgumentException("Invalid file path.");
        }

        Path filePath = Path.of(file);

        if (filePath.isAbsolute()
                || filePath.normalize().startsWith("..")
                || !repositoryPath.resolve(filePath)
                        .normalize()
                        .startsWith(repositoryPath)) {
            throw new IllegalArgumentException("Invalid file path: " + file);
        }
    }

    private List<String> parseNullSeparated(String output) {
        if (output == null || output.isEmpty()) {
            return List.of();
        }

        return List.of(output.split("\0"));
    }

    public void createCommit(
            String subject,
            String description,
            List<String> selectedFiles
    ) throws IOException, InterruptedException {

        if (!isGitRepository()) {
            throw new IllegalStateException("The directory is not a Git repository.");
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Commit subject must not be empty.");
        }

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            throw new IllegalArgumentException("No files selected.");
        }

        for (String file : selectedFiles) {
            validateFilePath(file);
        }

        Path temporaryDirectory = Files.createTempDirectory("git-assistant-");
        Path temporaryIndex = temporaryDirectory.resolve("index");

        try {
            List<String> readTreeCommand = hasHeadCommit()
                    ? List.of("read-tree", "HEAD")
                    : List.of("read-tree", "--empty");

            ProcessResult readTree = executeGit(
                    readTreeCommand,
                    temporaryIndex
            );

            requireSuccess(readTree, "Could not initialize temporary Git index.");

            List<String> addCommand = new ArrayList<>();
            addCommand.add("--literal-pathspecs");
            addCommand.add("add");
            addCommand.add("-A");
            addCommand.add("--");
            addCommand.addAll(selectedFiles);

            ProcessResult add = executeGit(addCommand, temporaryIndex);
            requireSuccess(add, "Could not stage selected files.");

            List<String> commitCommand = new ArrayList<>();
            commitCommand.add("commit");
            commitCommand.add("-m");
            commitCommand.add(subject);

            if (description != null && !description.isBlank()) {
                commitCommand.add("-m");
                commitCommand.add(description);
            }

            ProcessResult commit = executeGit(
                    commitCommand,
                    temporaryIndex
            );

            requireSuccess(commit, "Could not create commit.");

            List<String> resetCommand = new ArrayList<>();
            resetCommand.add("--literal-pathspecs");
            resetCommand.add("reset");
            resetCommand.add("--quiet");
            resetCommand.add("HEAD");
            resetCommand.add("--");
            resetCommand.addAll(selectedFiles);

            ProcessResult reset = executeGit(resetCommand);

            requireSuccess(reset, "Commit created, but normal index could not be synchronized.");

        } finally {
            Files.deleteIfExists(temporaryIndex.resolveSibling("index.lock"));
            Files.deleteIfExists(temporaryIndex);
            Files.deleteIfExists(temporaryDirectory);
        }
    }


    private ProcessResult executeGit(List<String> arguments)
            throws IOException, InterruptedException {
        return executeGit(arguments, null);
    }

    private ProcessResult executeGit(
            List<String> arguments,
            Path temporaryIndex
    ) throws IOException, InterruptedException {

        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(arguments);

        Path outputFile = Files.createTempFile(
                "git-assistant-output-", ".tmp"
        );

        Path errorFile = Files.createTempFile(
                "git-assistant-error-", ".tmp"
        );

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(repositoryPath.toFile());

            builder.redirectOutput(outputFile.toFile());
            builder.redirectError(errorFile.toFile());

            if (temporaryIndex != null) {
                builder.environment().put(
                        "GIT_INDEX_FILE",
                        temporaryIndex.toString()
                );
            }

            Process process = builder.start();

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                process.waitFor();

                throw new IOException(
                        "Git command timed out: "
                                + String.join(" ", command)
                );
            }

            String output = Files.readString(
                    outputFile,
                    StandardCharsets.UTF_8
            );

            String errorOutput = Files.readString(
                    errorFile,
                    StandardCharsets.UTF_8
            );

            return new ProcessResult(
                    process.exitValue(),
                    output,
                    errorOutput
            );

        } finally {
            Files.deleteIfExists(outputFile);
            Files.deleteIfExists(errorFile);
        }
    }


    private void requireSuccess(
            ProcessResult result,
            String errorMessage
    ) throws IOException {

        if (result.exitCode() != 0) {

            String details = result.errorOutput().isBlank()
                    ? result.output()
                    : result.errorOutput();

            throw new IOException(errorMessage + " " + details);
        }
    }

    private record ProcessResult(
            int exitCode,
            String output,
            String errorOutput
    ) {
    }

    public Path getRepositoryPath() {
        return repositoryPath;
    }
}
