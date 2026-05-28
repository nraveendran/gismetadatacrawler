package org.example.gismetadata.arcgis.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.gismetadata.arcgis.model.ArcGisFieldMetadata;
import org.example.gismetadata.arcgis.model.ArcGisFolderMetadata;
import org.example.gismetadata.arcgis.model.ArcGisLayerMetadata;
import org.example.gismetadata.arcgis.model.ArcGisLayerRow;
import org.example.gismetadata.arcgis.model.ArcGisServiceMetadata;
import org.example.gismetadata.arcgis.model.ArcGisServiceRow;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ArcGisMetadataRepository {
    private static final Logger log = LoggerFactory.getLogger(ArcGisMetadataRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public ArcGisMetadataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveDiscoveredResources(
            String rootUrl,
            List<ArcGisServiceMetadata> services,
            List<ArcGisFolderMetadata> folders) {
        insertServices(rootUrl, services);
        insertFolders(rootUrl, folders);
    }

    public void insertCrawlSource(String sourceUrl, JsonNode rawJson) {
        String sql = """
                INSERT INTO crawl_sources (source_url, raw_json)
                VALUES (?, ?)
                ON CONFLICT (source_url)
                DO UPDATE SET
                    raw_json = EXCLUDED.raw_json,
                    crawled_at = NOW()
                """;

        jdbcTemplate.update(sql, sourceUrl, jsonb(rawJson));
    }

    public void saveFolder(String rootUrl, String folderName, String folderUrl, JsonNode metadata) {
        String sql = """
                INSERT INTO arcgis_folders
                    (server_url, folder_name, folder_url, metadata)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (server_url, folder_name)
                DO UPDATE SET
                    folder_url = EXCLUDED.folder_url,
                    metadata = EXCLUDED.metadata,
                    discovered_at = NOW()
                """;

        jdbcTemplate.update(sql, rootUrl, folderName, folderUrl, jsonb(metadata));
    }

    public List<ArcGisServiceRow> findFeatureServerServices() {
        String sql = """
                SELECT id, server_url
                FROM arcgis_services
                WHERE service_type = 'FeatureServer'
                  AND server_url IS NOT NULL
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new ArcGisServiceRow(
                resultSet.getLong("id"),
                resultSet.getString("server_url")));
    }

    public List<ArcGisLayerRow> findLayers() {
        String sql = """
                SELECT id, layer_url
                FROM arcgis_layers
                WHERE layer_url IS NOT NULL
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new ArcGisLayerRow(
                resultSet.getLong("id"),
                resultSet.getString("layer_url")));
    }

    @Transactional
    public void saveServiceMetadataAndLayers(long serviceId, JsonNode serviceMetadata, List<ArcGisLayerMetadata> layers) {
        jdbcTemplate.update(
                """
                UPDATE arcgis_services
                SET metadata = ?, discovered_at = NOW()
                WHERE id = ?
                """,
                jsonb(serviceMetadata),
                serviceId);

        insertLayers(serviceId, layers);
    }

    @Transactional
    public void saveLayerMetadataAndFields(long layerTableId, JsonNode layerMetadata, List<ArcGisFieldMetadata> fields) {
        jdbcTemplate.update(
                """
                UPDATE arcgis_layers
                SET layer_name = COALESCE(?, layer_name),
                    layer_type = ?,
                    geometry_type = ?,
                    object_id_field = ?,
                    display_field = ?,
                    type_id_field = ?,
                    capabilities = ?,
                    supported_query_formats = ?,
                    max_record_count = ?,
                    spatial_reference_wkid = ?,
                    dataset_type = ?,
                    metadata = ?,
                    discovered_at = NOW()
                WHERE id = ?
                """,
                textValue(layerMetadata, "name"),
                textValue(layerMetadata, "type"),
                textValue(layerMetadata, "geometryType"),
                textValue(layerMetadata, "objectIdField"),
                textValue(layerMetadata, "displayField"),
                textValue(layerMetadata, "typeIdField"),
                textValue(layerMetadata, "capabilities"),
                textValue(layerMetadata, "supportedQueryFormats"),
                intValue(layerMetadata, "maxRecordCount"),
                spatialReferenceWkid(layerMetadata),
                textValue(layerMetadata, "type"),
                jsonb(layerMetadata),
                layerTableId);

        insertFields(layerTableId, fields);
    }

    private void insertFolders(String rootUrl, List<ArcGisFolderMetadata> folders) {
        if (folders.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO arcgis_folders
                    (server_url, folder_name, folder_url, metadata)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (server_url, folder_name)
                DO UPDATE SET
                    folder_url = EXCLUDED.folder_url,
                    metadata = EXCLUDED.metadata,
                    discovered_at = NOW()
                """;

        List<Object[]> batchArgs = new ArrayList<>(folders.size());
        for (ArcGisFolderMetadata folder : folders) {
            batchArgs.add(new Object[] {
                    rootUrl,
                    folder.name(),
                    folder.url(),
                    jsonb(folder.metadata())
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("Upserted {} folders.", folders.size());
    }

    private void insertServices(String rootUrl, List<ArcGisServiceMetadata> services) {
        if (services.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO arcgis_services
                    (server_url, folder_name, service_name, service_type, metadata)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (service_name, service_type)
                DO UPDATE SET
                    server_url = EXCLUDED.server_url,
                    folder_name = EXCLUDED.folder_name,
                    metadata = EXCLUDED.metadata,
                    discovered_at = NOW()
                """;

        List<Object[]> batchArgs = new ArrayList<>(services.size());
        for (ArcGisServiceMetadata service : services) {
            batchArgs.add(new Object[] {
                    serviceUrl(rootUrl, service),
                    folderName(service.name()),
                    service.name(),
                    service.type(),
                    jsonb(service.metadata())
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("Upserted {} services.", services.size());
    }

    private void insertLayers(long serviceId, List<ArcGisLayerMetadata> layers) {
        if (layers.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO arcgis_layers
                    (service_id, layer_id, layer_name, layer_url, layer_type, geometry_type, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (service_id, layer_id)
                DO UPDATE SET
                    layer_name = EXCLUDED.layer_name,
                    layer_url = EXCLUDED.layer_url,
                    layer_type = EXCLUDED.layer_type,
                    geometry_type = EXCLUDED.geometry_type,
                    metadata = EXCLUDED.metadata,
                    discovered_at = NOW()
                """;

        List<Object[]> batchArgs = new ArrayList<>(layers.size());
        for (ArcGisLayerMetadata layer : layers) {
            batchArgs.add(new Object[] {
                    serviceId,
                    layer.layerId(),
                    layer.name(),
                    layer.layerUrl(),
                    layer.layerType(),
                    layer.geometryType(),
                    jsonb(layer.metadata())
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("Upserted {} layers for service id {}.", layers.size(), serviceId);
    }

    private void insertFields(long layerTableId, List<ArcGisFieldMetadata> fields) {
        if (fields.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO arcgis_fields
                    (layer_table_id, field_name, field_alias, field_type, model_name,
                     nullable, editable, metadata)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (layer_table_id, field_name)
                DO UPDATE SET
                    field_alias = EXCLUDED.field_alias,
                    field_type = EXCLUDED.field_type,
                    model_name = EXCLUDED.model_name,
                    nullable = EXCLUDED.nullable,
                    editable = EXCLUDED.editable,
                    metadata = EXCLUDED.metadata,
                    discovered_at = NOW()
                """;

        List<Object[]> batchArgs = new ArrayList<>(fields.size());
        for (ArcGisFieldMetadata field : fields) {
            batchArgs.add(new Object[] {
                    layerTableId,
                    field.name(),
                    field.alias(),
                    field.type(),
                    field.modelName(),
                    field.nullable(),
                    field.editable(),
                    jsonb(field.metadata())
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
        log.info("Upserted {} fields for layer table id {}.", fields.size(), layerTableId);
    }

    private static PGobject jsonb(JsonNode jsonNode) {
        try {
            PGobject object = new PGobject();
            object.setType("jsonb");
            object.setValue(jsonNode.toString());
            return object;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to convert JSON to PostgreSQL jsonb", exception);
        }
    }

    private static String serviceUrl(String rootUrl, ArcGisServiceMetadata service) {
        return rootUrl + "/" + encodeServiceName(service.name()) + "/" + urlEncode(service.type());
    }

    private static String folderName(String serviceName) {
        int slash = serviceName.lastIndexOf('/');
        return slash < 0 ? null : serviceName.substring(0, slash);
    }

    private static String encodeServiceName(String serviceName) {
        String[] parts = serviceName.split("/");
        List<String> encodedParts = new ArrayList<>(parts.length);
        for (String part : parts) {
            encodedParts.add(urlEncode(part));
        }
        return String.join("/", encodedParts);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static Integer intValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.canConvertToInt() ? value.asInt() : null;
    }

    private static Integer spatialReferenceWkid(JsonNode layerMetadata) {
        JsonNode spatialReference = layerMetadata.path("spatialReference");
        JsonNode latestWkid = spatialReference.path("latestWkid");
        if (latestWkid.canConvertToInt()) {
            return latestWkid.asInt();
        }

        JsonNode wkid = spatialReference.path("wkid");
        return wkid.canConvertToInt() ? wkid.asInt() : null;
    }
}
