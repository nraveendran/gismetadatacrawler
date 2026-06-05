package org.example.gismetadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gismetadatacrawler.neo4j")
public record Neo4jGraphExportProperties(
        String uri,
        String username,
        String password,
        String database,
        int batchSize) {
    public void validate() {
        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException("Set NEO4J_URI or gismetadatacrawler.neo4j.uri before running.");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("Set NEO4J_USERNAME or gismetadatacrawler.neo4j.username before running.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set NEO4J_PASSWORD or gismetadatacrawler.neo4j.password before running.");
        }
        if (database == null || database.isBlank()) {
            throw new IllegalStateException("Set NEO4J_DATABASE or gismetadatacrawler.neo4j.database before running.");
        }
        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalStateException("gismetadatacrawler.neo4j.batch-size must be between 1 and 10000.");
        }
    }
}
