package de.timonexus.gitassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

@FunctionalInterface
public interface StructuredResponseGenerator {

    String generateStructuredResponse(
            String prompt,
            JsonNode schema
    ) throws IOException, InterruptedException;
}