package org.example.gismetadata.gisdiscovery;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Candidate;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Tool;
import com.google.genai.types.Type;

public class VertexAiGoogleSearchClient implements GisDiscoveryLlmClient {
    private final String projectId;
    private final String location;
    private final String model;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public VertexAiGoogleSearchClient(
            String projectId,
            String location,
            String model,
            String apiKey,
            ObjectMapper objectMapper) {
        this.projectId = projectId;
        this.location = location;
        this.model = model;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode discover(String prompt) throws Exception {
        String outputText = generateText(prompt, true);
        try {
            return objectMapper.readTree(outputText);
        } catch (JsonProcessingException exception) {
            return repairAndParseJson(outputText, exception);
        }
    }

    public String searchRaw(String prompt) throws IOException {
        return generateText(prompt, true);
    }

    private JsonNode repairAndParseJson(String malformedJson, JsonProcessingException originalException)
            throws Exception {
        String repairPrompt = """
                The following text was intended to be valid JSON, but JSON parsing failed.

                Fix only the JSON syntax. Preserve the same object shape, keys, values, and ordering as much as possible.
                Do not add new URLs or facts.
                Return only valid JSON. No markdown. No explanation.

                Malformed JSON:
                %s
                """.formatted(malformedJson);

        String repairedText = generateText(repairPrompt, false);
        try {
            return objectMapper.readTree(repairedText);
        } catch (JsonProcessingException repairException) {
            IOException parseException = new IOException(
                    "Vertex AI response was invalid JSON, and one repair attempt also failed.",
                    repairException);
            parseException.addSuppressed(originalException);
            throw parseException;
        }
    }

    private String generateText(String prompt, boolean enableGoogleSearch) throws IOException {
        try (Client client = createClient()) {
            GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
                    .maxOutputTokens(16384)
                    .thinkingConfig(ThinkingConfig.builder()
                            .includeThoughts(false)
                            .thinkingBudget(0)
                            .build());
            if (enableGoogleSearch) {
                configBuilder.tools(List.of(Tool.builder()
                        .googleSearch(GoogleSearch.builder().build())
                        .build()));
            } else {
                configBuilder
                        .responseMimeType("application/json")
                        .responseSchema(discoveryResponseSchema());
            }
            GenerateContentConfig config = configBuilder.build();

            GenerateContentResponse response = client.models.generateContent(model, prompt, config);
            String text = extractText(response);
            if (text == null || text.isBlank()) {
                throw new IOException("Vertex AI SDK response did not contain text output. response="
                        + response.toJson());
            }
            return text;
        }
    }

    private String extractText(GenerateContentResponse response) {
        String text = response.text();
        if (text != null && !text.isBlank()) {
            return text;
        }

        StringBuilder textBuilder = new StringBuilder();
        for (Candidate candidate : response.candidates().orElse(List.of())) {
            for (Part part : candidate.content()
                    .flatMap(content -> content.parts())
                    .orElse(List.of())) {
                part.text().ifPresent(textBuilder::append);
            }
        }

        return textBuilder.toString();
    }

    private Client createClient() {
        if (apiKey != null && !apiKey.isBlank()) {
            return Client.builder()
                    .apiKey(apiKey)
                    .build();
        }

        return Client.builder()
                .vertexAI(true)
                .project(projectId)
                .location(location)
                .build();
    }

    private Schema discoveryResponseSchema() {
        Schema inputSchema = objectSchema(
                Map.of(
                        "entity_name", stringSchema(),
                        "state", stringSchema(),
                        "entity_type_guess", enumSchema("city", "county", "unknown")),
                List.of("entity_name", "state", "entity_type_guess"));

        Schema validationEvidenceSchema = objectSchema(
                Map.of(
                        "http_status", Schema.builder()
                                .anyOf(List.of(
                                        Schema.builder().type(Type.Known.INTEGER).build(),
                                        enumSchema("unknown")))
                                .build(),
                        "json_keys_found", arraySchema(stringSchema()),
                        "sample_title_or_service_name", nullableStringSchema()),
                List.of("http_status", "json_keys_found", "sample_title_or_service_name"));

        Schema sourceEvidenceSchema = objectSchema(
                Map.of(
                        "found_from", enumSchema(
                                "search_result",
                                "official_page",
                                "data_json",
                                "map_viewer",
                                "service_metadata",
                                "inferred_pattern"),
                        "source_url", nullableStringSchema(),
                        "source_title", nullableStringSchema()),
                List.of("found_from", "source_url", "source_title"));

        Schema candidateSchema = objectSchema(
                orderedMap(
                        "rank", Schema.builder().type(Type.Known.INTEGER).build(),
                        "url", stringSchema(),
                        "normalized_url", stringSchema(),
                        "endpoint_type", enumSchema(
                                "arcgis_rest_root",
                                "arcgis_hub_catalog",
                                "arcgis_service",
                                "open_data_portal",
                                "gis_page",
                                "map_viewer",
                                "unknown"),
                        "ownership_type", enumSchema(
                                "official",
                                "regional_government",
                                "vendor_hosted_official",
                                "third_party",
                                "unknown"),
                        "validation_status", enumSchema(
                                "validated",
                                "reachable_but_not_validated",
                                "inferred_not_validated",
                                "invalid"),
                        "validation_evidence", validationEvidenceSchema,
                        "source_evidence", sourceEvidenceSchema,
                        "confidence", enumSchema("high", "medium", "low"),
                        "reasoning", stringSchema()),
                List.of(
                        "rank",
                        "url",
                        "normalized_url",
                        "endpoint_type",
                        "ownership_type",
                        "validation_status",
                        "validation_evidence",
                        "source_evidence",
                        "confidence",
                        "reasoning"));

        return objectSchema(
                Map.of(
                        "input", inputSchema,
                        "top_ranked_urls", Schema.builder()
                                .type(Type.Known.ARRAY)
                                .items(candidateSchema)
                                .minItems(0L)
                                .maxItems(3L)
                                .build()),
                List.of("input", "top_ranked_urls"));
    }

    private Schema objectSchema(Map<String, Schema> properties, List<String> required) {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(required)
                .propertyOrdering(new java.util.ArrayList<>(properties.keySet()))
                .build();
    }

    private Schema arraySchema(Schema itemSchema) {
        return Schema.builder()
                .type(Type.Known.ARRAY)
                .items(itemSchema)
                .build();
    }

    private Schema stringSchema() {
        return Schema.builder()
                .type(Type.Known.STRING)
                .build();
    }

    private Schema nullableStringSchema() {
        return Schema.builder()
                .type(Type.Known.STRING)
                .nullable(true)
                .build();
    }

    private Schema enumSchema(String... values) {
        return Schema.builder()
                .type(Type.Known.STRING)
                .enum_(List.of(values))
                .build();
    }

    private Map<String, Schema> orderedMap(Object... keyValuePairs) {
        Map<String, Schema> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], (Schema) keyValuePairs[i + 1]);
        }
        return map;
    }
}
