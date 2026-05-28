package org.example.gismetadata.semantic;

import org.example.gismetadata.config.EmbeddingBackfillProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LayerSemanticMappingBackfill {
    private static final Logger log = LoggerFactory.getLogger(LayerSemanticMappingBackfill.class);

    private final EmbeddingBackfillProperties properties;
    private final LayerSemanticMappingRepository repository;

    public LayerSemanticMappingBackfill(
            EmbeddingBackfillProperties properties,
            LayerSemanticMappingRepository repository) {
        this.properties = properties;
        this.repository = repository;
    }

    public void run() {
        properties.validate();

        int totalProcessed = 0;
        long lastLayerId = 0;
        log.info(
                "Starting semantic layer candidate backfill. batchSize={}, maxTotalRecords={}",
                properties.batchSize(),
                properties.maxTotalRecords());

        while (totalProcessed < properties.maxTotalRecords()) {
            int remainingRecords = properties.maxTotalRecords() - totalProcessed;
            int fetchLimit = Math.min(properties.batchSize(), remainingRecords);
            List<Long> layerIds = repository.findLayerIdsWithEmbeddingsAfter(lastLayerId, fetchLimit);
            if (layerIds.isEmpty()) {
                log.info("No more arcgis_layers rows with embeddings to process.");
                break;
            }

            repository.storeCandidatesAndApplyAutoMappings(layerIds);
            totalProcessed += layerIds.size();
            lastLayerId = layerIds.get(layerIds.size() - 1);
            log.info(
                    "Processed {} layers in this batch; {} total. lastLayerId={}",
                    layerIds.size(),
                    totalProcessed,
                    lastLayerId);
        }

        log.info("Semantic layer candidate backfill finished. totalProcessed={}", totalProcessed);
    }
}
