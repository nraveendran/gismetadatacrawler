package org.example.gismetadata.gisdiscovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gismetadata.config.GisDiscoveryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GisRawSearchRunner {
    private static final Logger log = LoggerFactory.getLogger(GisRawSearchRunner.class);

    private final GisDiscoveryProperties discoveryProperties;
    private final GisEndpointDiscoveryRepository repository;
    private final ObjectMapper objectMapper;

    public GisRawSearchRunner(
            GisDiscoveryProperties discoveryProperties,
            GisEndpointDiscoveryRepository repository,
            ObjectMapper objectMapper) {
        this.discoveryProperties = discoveryProperties;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void run() throws Exception {
        discoveryProperties.validate();

        VertexAiGoogleSearchClient client = new VertexAiGoogleSearchClient(
                discoveryProperties.vertexProjectId(),
                discoveryProperties.vertexLocation(),
                discoveryProperties.vertexModel(),
                discoveryProperties.vertexApiKey(),
                objectMapper);

        int seeded = repository.seedCountyTargets(discoveryProperties.statefp());
        List<GisDiscoveryTarget> targets = repository.findCountyTargetsWithoutRawSearchResults(
                discoveryProperties.statefp(),
                "vertex",
                discoveryProperties.vertexModel());

        log.info(
                "Starting raw GIS web search. statefp={}, stateName={}, seededTargets={}, model={}",
                discoveryProperties.statefp(),
                discoveryProperties.stateName(),
                seeded,
                discoveryProperties.vertexModel());

        int totalStored = 0;
        for (GisDiscoveryTarget target : targets) {
            String prompt = GisRawSearchPrompt.forCounty(target.name(), discoveryProperties.stateName());
            log.info("Collecting raw GIS search results for {} {}.", target.name(), target.targetType());
            String rawResult = client.searchRaw(prompt);
            repository.upsertRawSearchResult(
                    target,
                    discoveryProperties.stateName(),
                    "vertex",
                    discoveryProperties.vertexModel(),
                    prompt,
                    rawResult);
            totalStored++;
            log.info("Stored raw GIS search result for {}.", target.name());
        }

        log.info("Raw GIS web search finished. targetsProcessed={}, rawResultsStored={}",
                targets.size(),
                totalStored);
    }
}
