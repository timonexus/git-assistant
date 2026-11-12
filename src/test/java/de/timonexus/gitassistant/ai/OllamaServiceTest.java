package de.timonexus.gitassistant.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class OllamaServiceTest {

    @Test
    void generateResponseShouldReturnValidResponse()
            throws Exception {

        // Arrange
        HttpServer server = HttpServer.create(
                new InetSocketAddress("localhost", 0),
                0
        );

        server.createContext("/api/chat", exchange -> {

            String response = """
                    {
                        "message": {
                            "role": "assistant",
                            "content": "Hello GitAssistant"
                        }
                    }
                    """;

            byte[] responseBytes =
                    response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );

            exchange.sendResponseHeaders(
                    200,
                    responseBytes.length
            );

            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        server.start();

        try {
            URI testUri = URI.create(
                    "http://localhost:"
                            + server.getAddress().getPort()
                            + "/api/chat"
            );

            OllamaService service = new OllamaService(testUri);

            // Act
            String response = service.generateResponse(
                    "Say exactly: Hello GitAssistant"
            );

            // Assert
            assertEquals("Hello GitAssistant", response);

        } finally {
            server.stop(0);
        }
    }
}