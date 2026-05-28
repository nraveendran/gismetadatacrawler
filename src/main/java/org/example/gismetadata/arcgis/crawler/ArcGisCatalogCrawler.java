package org.example.gismetadata.arcgis.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gismetadata.arcgis.model.ArcGisFolderMetadata;
import org.example.gismetadata.arcgis.model.ArcGisServiceMetadata;
import org.example.gismetadata.arcgis.repository.ArcGisMetadataRepository;
import org.example.gismetadata.config.ArcGisCrawlerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArcGisCatalogCrawler {
    private static final Logger log = LoggerFactory.getLogger(ArcGisCatalogCrawler.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ArcGisCrawlerProperties properties;
    private final ArcGisMetadataRepository repository;

    public ArcGisCatalogCrawler(
            ObjectMapper objectMapper,
            ArcGisCrawlerProperties properties,
            ArcGisMetadataRepository repository) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.repository = repository;
    }

    public void crawl() throws IOException, InterruptedException {
        log.info("Starting ArcGIS root crawl for {}", properties.rootUrl());
        JsonNode rootJson = fetchJson(properties.rootUrl());
        repository.insertCrawlSource(properties.rootUrl(), rootJson);

        CrawlCounts counts = crawlFolder("", properties.rootUrl(), rootJson, new HashSet<>());

        log.info(
                "Crawled {}. Discovered {} folders and {} services.",
                properties.rootUrl(),
                counts.folders(),
                counts.services());
    }

    private CrawlCounts crawlFolder(
            String folderName,
            String folderUrl,
            JsonNode folderJson,
            Set<String> visitedFolderNames) throws IOException, InterruptedException {
        if (!visitedFolderNames.add(folderName)) {
            return new CrawlCounts(0, 0);
        }

        if (!folderName.isBlank()) {
            repository.saveFolder(properties.rootUrl(), folderName, folderUrl, folderJson);
        }

        List<ArcGisServiceMetadata> services = extractServices(folderJson);
        List<ArcGisFolderMetadata> folders = extractFolders(folderName, folderJson);
        log.info(
                "Discovered {} folders and {} services under {}",
                folders.size(),
                services.size(),
                folderName.isBlank() ? properties.rootUrl() : folderName);
        repository.saveDiscoveredResources(properties.rootUrl(), services, folders);

        int folderCount = folders.size();
        int serviceCount = services.size();

        for (ArcGisFolderMetadata folder : folders) {
            log.info("Fetching folder metadata from {}", folder.url());
            JsonNode childFolderJson = fetchJson(folder.url());
            CrawlCounts childCounts = crawlFolder(folder.name(), folder.url(), childFolderJson, visitedFolderNames);
            folderCount += childCounts.folders();
            serviceCount += childCounts.services();
        }

        return new CrawlCounts(folderCount, serviceCount);
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

    List<ArcGisServiceMetadata> extractServices(JsonNode rootJson) {
        JsonNode servicesNode = rootJson.path("services");
        if (!servicesNode.isArray()) {
            return List.of();
        }

        List<ArcGisServiceMetadata> services = new ArrayList<>();
        for (JsonNode serviceNode : servicesNode) {
            String name = serviceNode.path("name").asText(null);
            String type = serviceNode.path("type").asText(null);
            if (name == null || name.isBlank() || type == null || type.isBlank()) {
                continue;
            }
            services.add(new ArcGisServiceMetadata(name, type, serviceNode));
        }
        return services;
    }

    List<ArcGisFolderMetadata> extractFolders(String parentFolderName, JsonNode folderJson) {
        JsonNode foldersNode = folderJson.path("folders");
        if (!foldersNode.isArray()) {
            return List.of();
        }

        List<ArcGisFolderMetadata> folders = new ArrayList<>();
        for (JsonNode folderNode : foldersNode) {
            String childName = folderNode.asText(null);
            if (childName == null || childName.isBlank()) {
                continue;
            }

            String folderName = parentFolderName.isBlank() ? childName : parentFolderName + "/" + childName;
            String folderUrl = properties.rootUrl() + "/" + encodeFolderName(folderName);
            folders.add(new ArcGisFolderMetadata(folderName, folderUrl, folderNode));
        }
        return folders;
    }

    private static String encodeFolderName(String folderName) {
        String[] parts = folderName.split("/");
        List<String> encodedParts = new ArrayList<>(parts.length);
        for (String part : parts) {
            encodedParts.add(URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"));
        }
        return String.join("/", encodedParts);
    }

    private record CrawlCounts(int folders, int services) {
    }
}
