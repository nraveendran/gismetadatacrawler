package org.example.gismetadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gismetadatacrawler.embeddings")
public record EmbeddingBackfillProperties(int batchSize, int maxTotalRecords) {
    public void validate() {
        if (batchSize < 1 || batchSize > 2048) {
            throw new IllegalStateException("gismetadatacrawler.embeddings.batch-size must be between 1 and 2048.");
        }
        if (maxTotalRecords < 1) {
            throw new IllegalStateException("gismetadatacrawler.embeddings.max-total-records must be positive.");
        }
    }
}
