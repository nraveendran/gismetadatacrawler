package org.example.gismetadata.semantic;

import org.example.gismetadata.config.EmbeddingBackfillProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FieldSemanticMappingBackfill {
    private static final Logger log = LoggerFactory.getLogger(FieldSemanticMappingBackfill.class);

    private final EmbeddingBackfillProperties properties;
    private final FieldSemanticMappingRepository repository;

    public FieldSemanticMappingBackfill(
            EmbeddingBackfillProperties properties,
            FieldSemanticMappingRepository repository) {
        this.properties = properties;
        this.repository = repository;
    }

    public void run() {
        properties.validate();

        int totalProcessed = 0;
        long lastFieldId = 0;
        log.info(
                "Starting semantic field candidate backfill. batchSize={}, maxTotalRecords={}",
                properties.batchSize(),
                properties.maxTotalRecords());

        while (totalProcessed < properties.maxTotalRecords()) {
            int remainingRecords = properties.maxTotalRecords() - totalProcessed;
            int fetchLimit = Math.min(properties.batchSize(), remainingRecords);
            List<Long> fieldIds = repository.findFieldIdsWithEmbeddingsAfter(lastFieldId, fetchLimit);
            if (fieldIds.isEmpty()) {
                log.info("No more arcgis_fields rows with embeddings to process.");
                break;
            }

            repository.storeCandidatesAndApplyAutoMappings(fieldIds);
            totalProcessed += fieldIds.size();
            lastFieldId = fieldIds.get(fieldIds.size() - 1);
            log.info(
                    "Processed {} fields in this batch; {} total. lastFieldId={}",
                    fieldIds.size(),
                    totalProcessed,
                    lastFieldId);
        }

        log.info("Semantic field candidate backfill finished. totalProcessed={}", totalProcessed);
    }
}
