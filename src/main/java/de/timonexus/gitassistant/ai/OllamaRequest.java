
package de.timonexus.gitassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record OllamaRequest(
        String model,
        List<ChatMessage> messages,
        boolean stream,
        JsonNode format,
        Boolean think
) {
}
