package de.timonexus.gitassistant.commit;

public record CommitSuggestion(
        String category,
        String scope,
        String title,
        String description,
        String reason
) {
}