
package de.timonexus.gitassistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class OllamaService implements StructuredResponseGenerator {

    private static final String API_URL =
            "http://localhost:11434/api/chat";

    private static final String MODEL = "qwen3:4b";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaService() {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper()
                .configure(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        false
                );
    }

    public String generateResponse(String prompt)
            throws IOException, InterruptedException {

        return sendRequest(prompt, null);
    }

    @Override
    public String generateStructuredResponse(
            String prompt,
            JsonNode schema
    ) throws IOException, InterruptedException {
        return sendRequest(prompt, schema);
    }


    private String sendRequest(String prompt, JsonNode schema)
            throws IOException, InterruptedException {

        OllamaRequest requestData = new OllamaRequest(
                MODEL,
                List.of(new ChatMessage("user", prompt)),
                false,
                schema,
                false
        );

        String requestBody = objectMapper.writeValueAsString(
                requestData
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofMinutes(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        long startTime = System.currentTimeMillis();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        long duration = System.currentTimeMillis() - startTime;

        System.out.println(
                "Ollama request completed in " + duration + " ms"
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ollama API error: HTTP "
                            + response.statusCode()
                            + " - "
                            + response.body()
            );
        }

        OllamaResponse responseData = objectMapper.readValue(
                response.body(),
                OllamaResponse.class
        );

        if (responseData.message() == null
                || responseData.message().content() == null) {
            throw new IOException("Ollama returned an empty response.");
        }

        return responseData.message().content();
    }


    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
