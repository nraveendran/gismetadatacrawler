package org.example.gismetadata.arcgis.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.example.gismetadata.arcgis.model.ArcGisLayerMetadata;
import org.example.gismetadata.arcgis.model.ArcGisServiceRow;
import org.example.gismetadata.arcgis.repository.ArcGisMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FeatureServerLayerCrawler {
    private static final Logger log = LoggerFactory.getLogger(FeatureServerLayerCrawler.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ArcGisMetadataRepository repository;

    public FeatureServerLayerCrawler(ObjectMapper objectMapper, ArcGisMetadataRepository repository) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    public void crawl() throws IOException, InterruptedException {
        List<ArcGisServiceRow> services = repository.findFeatureServerServices();
        log.info("Found {} FeatureServer services to enrich.", services.size());
        int layerCount = 0;

        for (ArcGisServiceRow service : services) {
            log.info("Fetching FeatureServer metadata from {}", service.serviceUrl());
            JsonNode serviceMetadata = fetchJson(service.serviceUrl());
            List<ArcGisLayerMetadata> layers = extractLayers(service.serviceUrl(), serviceMetadata);
            repository.saveServiceMetadataAndLayers(service.id(), serviceMetadata, layers);
            log.info("Saved metadata and {} layers for service id {}", layers.size(), service.id());
            layerCount += layers.size();
        }

        log.info(
                "Updated metadata for {} FeatureServer services and upserted {} layers.",
                services.size(),
                layerCount);
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

    List<ArcGisLayerMetadata> extractLayers(String serviceUrl, JsonNode serviceMetadata) {
        JsonNode layersNode = serviceMetadata.path("layers");
        if (!layersNode.isArray()) {
            return List.of();
        }

        List<ArcGisLayerMetadata> layers = new ArrayList<>();
        for (JsonNode layerNode : layersNode) {
            if (!layerNode.path("id").canConvertToInt()) {
                continue;
            }

            int layerId = layerNode.path("id").asInt();
            String name = layerNode.path("name").asText(null);
            if (name == null || name.isBlank()) {
                continue;
            }

            layers.add(new ArcGisLayerMetadata(
                    layerId,
                    name,
                    serviceUrl + "/" + layerId,
                    layerNode.path("type").asText(null),
                    layerNode.path("geometryType").asText(null),
                    layerNode));
        }
        return layers;
    }
}

