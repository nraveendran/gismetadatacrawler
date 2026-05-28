package org.example.gismetadata.embedding.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class OpenAiEmbeddingClient {
    private static final URI EMBEDDINGS_URI = URI.create("https://api.openai.com/v1/embeddings");

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingClient(
            String apiKey,
            String model,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<List<BigDecimal>> createEmbeddings(List<String> inputs) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(new EmbeddingRequest(model, inputs));
        HttpRequest request = HttpRequest.newBuilder(EMBEDDINGS_URI)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI embeddings request failed with HTTP "
                    + response.statusCode() + ": " + response.body());
        }

        JsonNode dataNode = objectMapper.readTree(response.body()).path("data");
        if (!dataNode.isArray()) {
            throw new IOException("OpenAI embeddings response did not contain data[].");
        }

        List<List<BigDecimal>> embeddings = new ArrayList<>(dataNode.size());
        for (JsonNode itemNode : dataNode) {
            JsonNode embeddingNode = itemNode.path("embedding");
            if (!embeddingNode.isArray()) {
                throw new IOException("OpenAI embeddings response item did not contain embedding[].");
            }

            List<BigDecimal> embedding = new ArrayList<>(embeddingNode.size());
            for (JsonNode valueNode : embeddingNode) {
                embedding.add(valueNode.decimalValue());
            }
            embeddings.add(embedding);
        }
        return embeddings;
    }

    private record EmbeddingRequest(String model, List<String> input) {
    }
}
