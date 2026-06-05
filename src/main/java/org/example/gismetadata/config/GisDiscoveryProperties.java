package org.example.gismetadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gismetadatacrawler.gis-discovery")
public record GisDiscoveryProperties(
        String provider,
        String model,
        String stateName,
        String statefp,
        int maxTargets,
        String vertexProjectId,
        String vertexLocation,
        String vertexModel,
        String vertexApiKey) {
    public void validate() {
        if (provider == null || provider.isBlank()) {
            throw new IllegalStateException("gismetadatacrawler.gis-discovery.provider must not be blank.");
        }
        if (!"openai".equalsIgnoreCase(provider) && !"vertex".equalsIgnoreCase(provider)) {
            throw new IllegalStateException("gismetadatacrawler.gis-discovery.provider must be openai or vertex.");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("gismetadatacrawler.gis-discovery.model must not be blank.");
        }
        if (stateName == null || stateName.isBlank()) {
            throw new IllegalStateException("gismetadatacrawler.gis-discovery.state-name must not be blank.");
        }
        if (statefp == null || statefp.isBlank()) {
            throw new IllegalStateException("gismetadatacrawler.gis-discovery.statefp must not be blank.");
        }
        if (maxTargets < 1 || maxTargets > 500) {
            throw new IllegalStateException("gismetadatacrawler.gis-discovery.max-targets must be between 1 and 500.");
        }
        if ("vertex".equalsIgnoreCase(provider)) {
            boolean hasApiKey = vertexApiKey != null && !vertexApiKey.isBlank();
            if (!hasApiKey && (vertexProjectId == null || vertexProjectId.isBlank())) {
                throw new IllegalStateException(
                        "Set GOOGLE_GENAI_API_KEY, GOOGLE_CLOUD_PROJECT, or gismetadatacrawler.gis-discovery.vertex-project-id before running with provider=vertex.");
            }
            if (!hasApiKey && (vertexLocation == null || vertexLocation.isBlank())) {
                throw new IllegalStateException(
                        "Set GOOGLE_CLOUD_LOCATION or gismetadatacrawler.gis-discovery.vertex-location before running with provider=vertex.");
            }
            if (vertexModel == null || vertexModel.isBlank()) {
                throw new IllegalStateException(
                        "Set VERTEX_GIS_DISCOVERY_MODEL or gismetadatacrawler.gis-discovery.vertex-model before running with provider=vertex.");
            }
        }
    }
}
