package de.timonexus.gitassistant.ai;

public record ChatMessage(
        String role,
        String content
) {
}