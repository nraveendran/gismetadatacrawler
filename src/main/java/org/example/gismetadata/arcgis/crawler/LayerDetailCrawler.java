package org.example.gismetadata.arcgis.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gismetadata.arcgis.model.ArcGisFieldMetadata;
import org.example.gismetadata.arcgis.model.ArcGisLayerRow;
import org.example.gismetadata.arcgis.repository.ArcGisMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class LayerDetailCrawler {
    private static final Logger log = LoggerFactory.getLogger(LayerDetailCrawler.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ArcGisMetadataRepository repository;

    public LayerDetailCrawler(ObjectMapper objectMapper, ArcGisMetadataRepository repository) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    public void crawl() throws IOException, InterruptedException {
        List<ArcGisLayerRow> layers = repository.findLayers();
        int fieldCount = 0;
        log.info("Found {} ArcGIS layers to enrich.", layers.size());

        for (ArcGisLayerRow layer : layers) {
            log.info("Fetching layer metadata from {}", layer.layerUrl());
            JsonNode layerMetadata = fetchJson(layer.layerUrl());
            List<ArcGisFieldMetadata> fields = extractFields(layerMetadata);
            repository.saveLayerMetadataAndFields(layer.id(), layerMetadata, fields);
            fieldCount += fields.size();
            log.info("Saved metadata and {} fields for layer table id {}", fields.size(), layer.id());
        }

        log.info("Updated metadata for {} layers and upserted {} fields.", layers.size(), fieldCount);
    }

    JsonNode fetchJson(String url) throws IOException, InterruptedException {
        URI uri = URI.create(url + "?f=pjson");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GET " + uri + " failed with HTTP " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    List<ArcGisFieldMetadata> extractFields(JsonNode layerMetadata) {
        JsonNode fieldsNode = layerMetadata.path("fields");
        if (!fieldsNode.isArray()) {
            return List.of();
        }

        List<ArcGisFieldMetadata> fields = new ArrayList<>();
        for (JsonNode fieldNode : fieldsNode) {
            String name = fieldNode.path("name").asText(null);
            if (name == null || name.isBlank()) {
                continue;
            }

            fields.add(new ArcGisFieldMetadata(
                    name,
                    fieldNode.path("alias").asText(null),
                    fieldNode.path("type").asText(null),
                    fieldNode.path("modelName").asText(null),
                    optionalBoolean(fieldNode.path("nullable")),
                    optionalBoolean(fieldNode.path("editable")),
                    fieldNode));
        }
        return fields;
    }

    private static Boolean optionalBoolean(JsonNode node) {
        return node.isBoolean() ? node.asBoolean() : null;
    }
}
