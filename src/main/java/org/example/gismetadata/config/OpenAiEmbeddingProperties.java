package org.example.gismetadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gismetadatacrawler.openai")
public record OpenAiEmbeddingProperties(String apiKey, String embeddingModel) {
    public void validate() {
        if (apiKey == null || apiKey.isBlank() || "REPLACE_ME".equals(apiKey)) {
            throw new IllegalStateException("Set OPENAI_API_KEY or gismetadatacrawler.openai.api-key before running.");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalStateException("gismetadatacrawler.openai.embedding-model must not be blank.");
        }
    }
}
