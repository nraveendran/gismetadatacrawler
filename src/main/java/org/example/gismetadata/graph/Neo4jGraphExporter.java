package org.example.gismetadata.graph;

import java.util.List;
import java.util.Map;

import org.example.gismetadata.config.Neo4jGraphExportProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Neo4jGraphExporter {
    private static final Logger log = LoggerFactory.getLogger(Neo4jGraphExporter.class);

    private final PostgresGraphRepository repository;
    private final Neo4jGraphWriter writer;
    private final Neo4jGraphExportProperties properties;

    public Neo4jGraphExporter(
            PostgresGraphRepository repository,
            Neo4jGraphWriter writer,
            Neo4jGraphExportProperties properties) {
        this.repository = repository;
        this.writer = writer;
        this.properties = properties;
    }

    public void export() {
        properties.validate();

        log.info("Starting Neo4j graph export.");
        writer.createConstraints();

        writeNodes("County", repository.findTexasCounties());
        writeNodes("City", repository.findTexasCities());
        writeNodes("Cog", repository.findUsCogs());
        writeNodes("ArcGisService", repository.findArcGisServices());
        writeNodes("ArcGisLayer", repository.findArcGisLayers());
        writeNodes("ArcGisField", repository.findArcGisFields());
        writeNodes("LayerSemanticRole", repository.findLayerSemanticRoles());
        writeNodes("FieldSemanticRole", repository.findFieldSemanticRoles());

        writeRelationships("Cog", "COVERS", "County", repository.findCogCountyRelationships());
        writeRelationships("County", "CONTAINS", "City", repository.findCountyCityRelationships());
        writeRelationships("ArcGisService", "HAS_LAYER", "ArcGisLayer", repository.findServiceLayerRelationships());
        writeRelationships("ArcGisLayer", "HAS_FIELD", "ArcGisField", repository.findLayerFieldRelationships());
        writeRelationships("ArcGisLayer", "REPRESENTS", "LayerSemanticRole", repository.findLayerSemanticRoleRelationships());
        writeRelationships("ArcGisField", "REPRESENTS", "FieldSemanticRole", repository.findFieldSemanticRoleRelationships());

        List<Map<String, Object>> serviceJurisdictionRelationships = repository.findServiceJurisdictionRelationships();
        writer.writeDynamicRelationships(serviceJurisdictionRelationships);
        log.info("Wrote {} service-jurisdiction relationships.", serviceJurisdictionRelationships.size());

        log.info("Neo4j graph export finished.");
    }

    private void writeNodes(String label, List<Map<String, Object>> rows) {
        writer.writeNodes(label, rows);
        log.info("Wrote {} {} nodes.", rows.size(), label);
    }

    private void writeRelationships(
            String fromLabel,
            String relationshipType,
            String toLabel,
            List<Map<String, Object>> rows) {
        writer.writeRelationships(fromLabel, relationshipType, toLabel, rows);
        log.info("Wrote {} {} relationships from {} to {}.", rows.size(), relationshipType, fromLabel, toLabel);
    }
}
