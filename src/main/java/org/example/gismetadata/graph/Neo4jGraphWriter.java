package org.example.gismetadata.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.gismetadata.config.Neo4jGraphExportProperties;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.QueryConfig;
import org.springframework.stereotype.Component;

@Component
public class Neo4jGraphWriter {
    private static final Set<String> ALLOWED_LABELS = Set.of(
            "City",
            "County",
            "Cog",
            "ArcGisService",
            "ArcGisLayer",
            "ArcGisField",
            "LayerSemanticRole",
            "FieldSemanticRole");

    private static final Set<String> ALLOWED_RELATIONSHIP_TYPES = Set.of(
            "COVERS",
            "CONTAINS",
            "HAS_LAYER",
            "HAS_FIELD",
            "REPRESENTS",
            "OWNS_SERVICE");

    private final Driver driver;
    private final QueryConfig queryConfig;
    private final int batchSize;

    public Neo4jGraphWriter(Neo4jGraphExportProperties properties) {
        this.driver = GraphDatabase.driver(
                properties.uri(),
                AuthTokens.basic(properties.username(), properties.password()));
        this.queryConfig = QueryConfig.builder().withDatabase(properties.database()).build();
        this.batchSize = properties.batchSize();
    }

    public void createConstraints() {
        for (String label : ALLOWED_LABELS) {
            String constraintName = toConstraintName(label);
            run("""
                    CREATE CONSTRAINT %s IF NOT EXISTS
                    FOR (n:%s)
                    REQUIRE n.id IS UNIQUE
                    """.formatted(constraintName, label), Map.of());
        }
    }

    public void writeNodes(String label, List<Map<String, Object>> rows) {
        validateLabel(label);
        String cypher = """
                UNWIND $rows AS row
                MERGE (n:%s {id: row.id})
                SET n += row
                """.formatted(label);
        writeBatches(cypher, rows);
    }

    public void writeRelationships(
            String fromLabel,
            String relationshipType,
            String toLabel,
            List<Map<String, Object>> rows) {
        validateLabel(fromLabel);
        validateLabel(toLabel);
        validateRelationshipType(relationshipType);

        String cypher = """
                UNWIND $rows AS row
                MATCH (from:%s {id: row.from_id})
                MATCH (to:%s {id: row.to_id})
                MERGE (from)-[relationship:%s]->(to)
                SET relationship += row
                REMOVE relationship.from_id
                REMOVE relationship.to_id
                """.formatted(fromLabel, toLabel, relationshipType);
        writeBatches(cypher, rows);
    }

    public void writeDynamicRelationships(List<Map<String, Object>> rows) {
        Map<RelationshipKey, List<Map<String, Object>>> groupedRows = rows.stream()
                .collect(Collectors.groupingBy(row -> new RelationshipKey(
                        row.get("from_label").toString(),
                        row.get("relationship_type").toString(),
                        row.get("to_label").toString())));

        for (Map.Entry<RelationshipKey, List<Map<String, Object>>> entry : groupedRows.entrySet()) {
            RelationshipKey key = entry.getKey();
            validateLabel(key.fromLabel());
            validateLabel(key.toLabel());
            validateRelationshipType(key.relationshipType());

            List<Map<String, Object>> relationshipRows = entry.getValue().stream()
                    .map(this::stripDynamicRelationshipColumns)
                    .toList();
            writeRelationships(key.fromLabel(), key.relationshipType(), key.toLabel(), relationshipRows);
        }
    }

    @PreDestroy
    public void close() {
        driver.close();
    }

    private Map<String, Object> stripDynamicRelationshipColumns(Map<String, Object> row) {
        Map<String, Object> stripped = new HashMap<>(row);
        stripped.remove("from_label");
        stripped.remove("to_label");
        stripped.remove("relationship_type");
        return stripped;
    }

    private void writeBatches(String cypher, List<Map<String, Object>> rows) {
        for (List<Map<String, Object>> batch : partition(rows)) {
            run(cypher, Map.of("rows", batch));
        }
    }

    private void run(String cypher, Map<String, Object> parameters) {
        driver.executableQuery(cypher)
                .withParameters(parameters)
                .withConfig(queryConfig)
                .execute();
    }

    private List<List<Map<String, Object>>> partition(List<Map<String, Object>> rows) {
        List<List<Map<String, Object>>> partitions = new ArrayList<>();
        for (int start = 0; start < rows.size(); start += batchSize) {
            partitions.add(rows.subList(start, Math.min(start + batchSize, rows.size())));
        }
        return partitions;
    }

    private void validateLabel(String label) {
        if (!ALLOWED_LABELS.contains(label)) {
            throw new IllegalArgumentException("Unsupported Neo4j label: " + label);
        }
    }

    private void validateRelationshipType(String relationshipType) {
        if (!ALLOWED_RELATIONSHIP_TYPES.contains(relationshipType)) {
            throw new IllegalArgumentException("Unsupported Neo4j relationship type: " + relationshipType);
        }
    }

    private String toConstraintName(String label) {
        return label.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase() + "_id";
    }

    private record RelationshipKey(String fromLabel, String relationshipType, String toLabel) {
    }
}
