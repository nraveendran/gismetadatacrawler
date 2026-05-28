package org.example.gismetadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gismetadatacrawler.arcgis")
public record ArcGisCrawlerProperties(String rootUrl) {
    public ArcGisCrawlerProperties {
        if (rootUrl == null || rootUrl.isBlank()) {
            throw new IllegalArgumentException("gismetadatacrawler.arcgis.root-url must not be blank");
        }
        rootUrl = normalizeRootUrl(rootUrl);
    }

    private static String normalizeRootUrl(String rootUrl) {
        while (rootUrl.endsWith("/")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
        }
        return rootUrl;
    }
}
