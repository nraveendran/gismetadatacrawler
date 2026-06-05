package org.example.gismetadata.gisdiscovery;

import java.net.http.HttpClient;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gismetadata.config.GisDiscoveryProperties;
import org.example.gismetadata.config.OpenAiEmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GisEndpointDiscoveryRunner {
    private static final Logger log = LoggerFactory.getLogger(GisEndpointDiscoveryRunner.class);

    private final GisDiscoveryProperties discoveryProperties;
    private final OpenAiEmbeddingProperties openAiProperties;
    private final GisEndpointDiscoveryRepository repository;
    private final ObjectMapper objectMapper;

    public GisEndpointDiscoveryRunner(
            GisDiscoveryProperties discoveryProperties,
            OpenAiEmbeddingProperties openAiProperties,
            GisEndpointDiscoveryRepository repository,
            ObjectMapper objectMapper) {
        this.discoveryProperties = discoveryProperties;
        this.openAiProperties = openAiProperties;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void run() throws Exception {
        discoveryProperties.validate();
        GisDiscoveryLlmClient client = createClient();

        int seeded = repository.seedCountyTargets(discoveryProperties.statefp());
        List<GisDiscoveryTargetWithResearchContext> targets = repository.findCountyTargetsWithRawSearchResults(
                discoveryProperties.statefp(),
                "vertex",
                discoveryProperties.vertexModel());

        log.info(
                "Starting GIS endpoint discovery. provider={}, statefp={}, stateName={}, seededTargets={}, model={}",
                discoveryProperties.provider(),
                discoveryProperties.statefp(),
                discoveryProperties.stateName(),
                seeded,
                modelName());

        int totalCandidates = 0;
        for (GisDiscoveryTargetWithResearchContext targetWithContext : targets) {
            GisDiscoveryTarget target = targetWithContext.target();
            String prompt = GisEndpointDiscoveryPrompt.forTarget(
                    target.name(),
                    discoveryProperties.stateName(),
                    targetWithContext.researchContext());
            log.info("Discovering GIS endpoints for {} {} using raw research context.", target.name(), target.targetType());
            try {
                JsonNode response = client.discover(prompt);
                int candidates = repository.upsertCandidates(target, discoveryProperties.stateName(), response);
                repository.markDiscoverySucceeded(target.id());
                totalCandidates += candidates;
                log.info("Upserted {} GIS URL candidates for {}.", candidates, target.name());
            } catch (Exception exception) {
                repository.markDiscoveryFailed(target.id(), exception);
                log.warn("Skipping {} after GIS endpoint discovery failed: {}", target.name(), exception.getMessage());
            }
        }

        log.info("GIS endpoint discovery finished. targetsProcessed={}, candidatesUpserted={}",
                targets.size(),
                totalCandidates);
    }

    private GisDiscoveryLlmClient createClient() {
        if ("vertex".equalsIgnoreCase(discoveryProperties.provider())) {
            return new VertexAiGoogleSearchClient(
                    discoveryProperties.vertexProjectId(),
                    discoveryProperties.vertexLocation(),
                    discoveryProperties.vertexModel(),
                    discoveryProperties.vertexApiKey(),
                    objectMapper);
        }

        openAiProperties.validate();
        return new OpenAiWebSearchClient(
                openAiProperties.apiKey(),
                discoveryProperties.model(),
                HttpClient.newHttpClient(),
                objectMapper);
    }

    private String modelName() {
        if ("vertex".equalsIgnoreCase(discoveryProperties.provider())) {
            return discoveryProperties.vertexModel();
        }
        return discoveryProperties.model();
    }
}
