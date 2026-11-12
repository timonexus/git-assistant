
package de.timonexus.gitassistant.commit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.timonexus.gitassistant.ai.StructuredResponseGenerator;
import de.timonexus.gitassistant.git.GitService;

import java.io.IOException;
import java.util.List;

public class CommitSuggestionService {

    private final StructuredResponseGenerator responseGenerator;
    private final GitService gitService;
    private final ObjectMapper objectMapper;

    public CommitSuggestionService(
            StructuredResponseGenerator responseGenerator
    ) {
        this(responseGenerator, null);
    }

    public CommitSuggestionService(
            StructuredResponseGenerator responseGenerator,
            GitService gitService
    ) {
        this.responseGenerator = responseGenerator;
        this.gitService = gitService;
        this.objectMapper = new ObjectMapper();
    }

    public CommitSuggestion generateSuggestion(String diff)
            throws IOException, InterruptedException {

        if (diff == null || diff.isBlank()) {
            throw new IllegalArgumentException(
                    "Git diff must not be empty."
            );
        }

        String prompt = """
                Analyze this Git diff and generate a commit suggestion.

                Requirements:
                - Use English.
                - Follow Conventional Commits.
                - Select one allowed category.
                - Do not invent changes.
                - Keep title short and precise.
                - Use an empty scope if no scope applies.
                - Keep description to one or two sentences.
                - Keep reason to one sentence.

                Allowed categories:
                feat, fix, refactor, test, docs,
                style, perf, build, ci, chore

                Return only the requested structured data.

                Git diff:
                %s
                """.formatted(diff);

        String response = responseGenerator.generateStructuredResponse(
                prompt,
                createSchema()
        );

        return parseSuggestion(response);
    }

    public CommitSuggestion generateSuggestionFromGit()
            throws IOException, InterruptedException {

        if (gitService == null) {
            throw new IllegalStateException(
                    "GitService has not been configured."
            );
        }

        String diff = gitService.getDiff();

        if (diff.isBlank()) {
            throw new IllegalStateException(
                    "No changes found in the Git repository."
            );
        }

        return generateSuggestion(diff);
    }

    private JsonNode createSchema() throws IOException {

        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "category": {
                      "type": "string",
                      "enum": [
                        "feat", "fix", "refactor", "test",
                        "docs", "style", "perf", "build",
                        "ci", "chore"
                      ]
                    },
                    "scope": {
                      "type": "string"
                    },
                    "title": {
                      "type": "string"
                    },
                    "description": {
                      "type": "string"
                    },
                    "reason": {
                      "type": "string"
                    }
                  },
                  "required": [
                    "category",
                    "scope",
                    "title",
                    "description",
                    "reason"
                  ],
                  "additionalProperties": false
                }
                """;

        return objectMapper.readTree(schema);
    }

    private CommitSuggestion parseSuggestion(String response)
            throws IOException {

        CommitSuggestion suggestion = objectMapper.readValue(
                response,
                CommitSuggestion.class
        );

        if (suggestion.category() == null
                || suggestion.title() == null
                || suggestion.title().isBlank()
                || suggestion.scope() == null
                || suggestion.description() == null
                || suggestion.reason() == null) {

            throw new IOException(
                    "AI returned an incomplete commit suggestion."
            );
        }

        return suggestion;
    }

    public CommitSuggestion generateSuggestionFromGit(
            List<String> selectedFiles
    ) throws IOException, InterruptedException {

        if (gitService == null) {
            throw new IllegalStateException(
                    "GitService has not been configured."
            );
        }

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No files selected for analysis."
            );
        }

        String diff = gitService.getDiff(selectedFiles);

        if (diff.isBlank()) {
            throw new IllegalStateException(
                    "No changes found in the selected files."
            );
        }

        return generateSuggestion(diff);
    }
}
