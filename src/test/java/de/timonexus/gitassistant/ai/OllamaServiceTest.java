package de.timonexus.gitassistant.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaServiceTest {

    @Test
    void generateResponseShouldReturnValidResponse()
            throws Exception {

        // Arrange
        OllamaService service = new OllamaService();

        String prompt = "Say exactly: Hello GitAssistant";

        // Act
        String response = service.generateResponse(prompt);

        // Assert
        assertNotNull(response);
        assertFalse(response.isBlank());

        System.out.println("KI-Antwort: " + response);
    }
}