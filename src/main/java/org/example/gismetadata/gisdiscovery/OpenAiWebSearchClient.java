package org.example.gismetadata.gisdiscovery;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpenAiWebSearchClient implements GisDiscoveryLlmClient {
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiWebSearchClient(
            String apiKey,
            String model,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode discover(String prompt) throws IOException, InterruptedException {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "tools", List.of(Map.of("type", "web_search")),
                "tool_choice", "auto",
                "input", prompt);

        HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI Responses request failed with HTTP "
                    + response.statusCode() + ": " + response.body());
        }

        String outputText = extractOutputText(objectMapper.readTree(response.body()));
        try {
            return objectMapper.readTree(outputText);
        } catch (JsonProcessingException exception) {
            return repairAndParseJson(outputText, exception);
        }
    }

    private JsonNode repairAndParseJson(String malformedJson, JsonProcessingException originalException)
            throws IOException, InterruptedException {
        String repairPrompt = """
                The following text was intended to be valid JSON, but JSON parsing failed.

                Fix only the JSON syntax. Preserve the same object shape, keys, values, and ordering as much as possible.
                Do not add new URLs or facts.
                Return only valid JSON. No markdown. No explanation.

                Malformed JSON:
                %s
                """.formatted(malformedJson);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", repairPrompt);

        HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI JSON repair request failed with HTTP "
                    + response.statusCode() + ": " + response.body(), originalException);
        }

        String repairedText = extractOutputText(objectMapper.readTree(response.body()));
        try {
            return objectMapper.readTree(repairedText);
        } catch (JsonProcessingException repairException) {
            IOException parseException = new IOException("OpenAI response was invalid JSON, and one repair attempt also failed.",
                    repairException);
            parseException.addSuppressed(originalException);
            throw parseException;
        }
    }

    private String extractOutputText(JsonNode responseNode) throws IOException {
        JsonNode outputTextNode = responseNode.path("output_text");
        if (outputTextNode.isTextual()) {
            return outputTextNode.asText();
        }

        JsonNode outputNode = responseNode.path("output");
        if (outputNode.isArray()) {
            for (JsonNode itemNode : outputNode) {
                JsonNode contentNode = itemNode.path("content");
                if (!contentNode.isArray()) {
                    continue;
                }
                for (JsonNode contentItemNode : contentNode) {
                    JsonNode textNode = contentItemNode.path("text");
                    if (textNode.isTextual()) {
                        return textNode.asText();
                    }
                }
            }
        }

        throw new IOException("OpenAI Responses output did not contain output_text.");
    }
}
