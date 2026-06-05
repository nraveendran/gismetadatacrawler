package org.example.gismetadata.graph;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresGraphRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresGraphRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findTexasCounties() {
        String sql = """
                SELECT
                    'county:' || c.ogc_fid AS id,
                    c.ogc_fid AS postgres_id,
                    c.name,
                    'us_counties' AS source_table
                FROM us_counties c
                ORDER BY c.name
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findTexasCities() {
        String sql = """
                SELECT
                    'city:' || p.ogc_fid AS id,
                    p.ogc_fid AS postgres_id,
                    p.name,
                    'us_places' AS source_table
                FROM us_places p
                ORDER BY p.name
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findUsCogs() {
        String sql = """
                SELECT
                    'cog:' || c.ogc_fid AS id,
                    c.ogc_fid AS postgres_id,
                    c.cog_nm AS name,
                    c.cog_nm AS cog_name,
                    c.cog_abrvn AS cog_abbreviation,
                    c.juris_typ AS jurisdiction_type,
                    c.globalid,
                    c.website,
                    'us_cogs' AS source_table
                FROM us_cogs c
                ORDER BY c.cog_nm
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findArcGisServices() {
        String sql = """
                SELECT
                    'service:' || s.id AS id,
                    s.id AS postgres_id,
                    s.server_url,
                    s.folder_name,
                    s.service_name,
                    s.service_type,
                    s.discovered_at::text AS discovered_at
                FROM arcgis_services s
                ORDER BY s.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findArcGisLayers() {
        String sql = """
                SELECT
                    'layer:' || l.id AS id,
                    l.id AS postgres_id,
                    l.service_id AS postgres_service_id,
                    l.layer_id,
                    l.layer_name,
                    l.layer_url,
                    l.layer_type,
                    l.geometry_type,
                    l.object_id_field,
                    l.display_field,
                    l.type_id_field,
                    l.capabilities,
                    l.supported_query_formats,
                    l.max_record_count,
                    l.spatial_reference_wkid,
                    l.dataset_type,
                    l.semantic_confidence::text AS semantic_confidence,
                    l.semantic_mapping_method,
                    l.discovered_at::text AS discovered_at
                FROM arcgis_layers l
                ORDER BY l.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findArcGisFields() {
        String sql = """
                SELECT
                    'field:' || f.id AS id,
                    f.id AS postgres_id,
                    f.layer_table_id AS postgres_layer_id,
                    f.field_name,
                    f.field_alias,
                    f.field_type,
                    f.model_name,
                    f.nullable,
                    f.editable,
                    f.semantic_role,
                    f.semantic_confidence::text AS semantic_confidence,
                    f.semantic_mapping_method,
                    f.discovered_at::text AS discovered_at
                FROM arcgis_fields f
                ORDER BY f.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findLayerSemanticRoles() {
        String sql = """
                SELECT
                    'layer-role:' || r.id AS id,
                    r.id AS postgres_id,
                    r.role_name,
                    r.description,
                    r.category,
                    r.is_active,
                    r.created_at::text AS created_at,
                    r.updated_at::text AS updated_at
                FROM layer_semantic_roles r
                ORDER BY r.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findFieldSemanticRoles() {
        String sql = """
                SELECT
                    'field-role:' || r.id AS id,
                    r.id AS postgres_id,
                    r.role_name,
                    r.description,
                    r.category,
                    r.is_active,
                    r.created_at::text AS created_at,
                    r.updated_at::text AS updated_at
                FROM semantic_roles r
                ORDER BY r.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findCogCountyRelationships() {
        String sql = """
                SELECT
                    'cog:' || r.cog_id AS from_id,
                    'county:' || r.county_id AS to_id,
                    r.relationship_type,
                    r.created_at::text AS created_at
                FROM cog_county_relationships r
                ORDER BY r.cog_id, r.county_id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findCountyCityRelationships() {
        String sql = """
                SELECT
                    'county:' || r.county_id AS from_id,
                    'city:' || r.city_id AS to_id,
                    r.relationship_type,
                    r.created_at::text AS created_at
                FROM county_city_relationships r
                ORDER BY r.county_id, r.city_id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findServiceLayerRelationships() {
        String sql = """
                SELECT
                    'service:' || l.service_id AS from_id,
                    'layer:' || l.id AS to_id
                FROM arcgis_layers l
                ORDER BY l.service_id, l.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findLayerFieldRelationships() {
        String sql = """
                SELECT
                    'layer:' || f.layer_table_id AS from_id,
                    'field:' || f.id AS to_id
                FROM arcgis_fields f
                ORDER BY f.layer_table_id, f.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findLayerSemanticRoleRelationships() {
        String sql = """
                SELECT
                    'layer:' || l.id AS from_id,
                    'layer-role:' || l.layer_semantic_role_id AS to_id,
                    l.semantic_confidence::text AS semantic_confidence,
                    l.semantic_mapping_method
                FROM arcgis_layers l
                WHERE l.layer_semantic_role_id IS NOT NULL
                ORDER BY l.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findFieldSemanticRoleRelationships() {
        String sql = """
                SELECT
                    'field:' || f.id AS from_id,
                    'field-role:' || r.id AS to_id,
                    f.semantic_confidence::text AS semantic_confidence,
                    f.semantic_mapping_method
                FROM arcgis_fields f
                JOIN semantic_roles r
                    ON r.role_name = f.semantic_role
                WHERE f.semantic_role IS NOT NULL
                ORDER BY f.id
                """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> findServiceJurisdictionRelationships() {
        String sql = """
                SELECT
                    CASE
                        WHEN lower(m.jurisdiction_type) = 'city' THEN 'City'
                        WHEN lower(m.jurisdiction_type) = 'county' THEN 'County'
                        WHEN lower(m.jurisdiction_type) = 'cog'
                            THEN 'Cog'
                    END AS from_label,
                    CASE
                        WHEN lower(m.jurisdiction_type) = 'city' THEN 'city:' || m.jurisdiction_id
                        WHEN lower(m.jurisdiction_type) = 'county' THEN 'county:' || m.jurisdiction_id
                        WHEN lower(m.jurisdiction_type) = 'cog'
                            THEN 'cog:' || m.jurisdiction_id
                    END AS from_id,
                    'OWNS_SERVICE' AS relationship_type,
                    'ArcGisService' AS to_label,
                    'service:' || m.service_id AS to_id,
                    m.id AS postgres_id,
                    m.jurisdiction_type,
                    m.relationship_type AS source_relationship_type,
                    m.match_method,
                    m.confidence::text AS confidence,
                    m.is_primary,
                    m.created_at::text AS created_at
                FROM service_jurisdiction_matches m
                WHERE lower(m.jurisdiction_type) IN ('city', 'county', 'cog')
                ORDER BY m.service_id, m.jurisdiction_type, m.jurisdiction_id
                """;
        return jdbcTemplate.queryForList(sql);
    }
}
