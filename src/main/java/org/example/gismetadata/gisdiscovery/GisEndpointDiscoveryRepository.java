package org.example.gismetadata.gisdiscovery;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GisEndpointDiscoveryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public GisEndpointDiscoveryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public int seedCountyTargets(String statefp) {
        String sql = """
                INSERT INTO gis_discovery_target
                    (target_type, source_table, source_ogc_fid, statefp, countyfp, geoid,
                     name, namelsad, intptlat, intptlon)
                SELECT
                    'county',
                    'us_counties',
                    c.ogc_fid,
                    c.statefp,
                    c.countyfp,
                    c.geoid,
                    c.name,
                    c.namelsad,
                    NULLIF(c.intptlat::text, '')::numeric,
                    NULLIF(c.intptlon::text, '')::numeric
                FROM us_counties c
                WHERE c.statefp = ?
                ORDER BY c.name
                ON CONFLICT (target_type, source_table, source_ogc_fid)
                DO UPDATE SET
                    statefp = EXCLUDED.statefp,
                    countyfp = EXCLUDED.countyfp,
                    geoid = EXCLUDED.geoid,
                    name = EXCLUDED.name,
                    namelsad = EXCLUDED.namelsad,
                    intptlat = EXCLUDED.intptlat,
                    intptlon = EXCLUDED.intptlon
                """;
        return jdbcTemplate.update(sql, statefp);
    }

    public List<GisDiscoveryTarget> findCountyTargets(String statefp) {
        String sql = """
                SELECT
                    t.id,
                    t.target_type,
                    t.source_table,
                    t.source_ogc_fid,
                    t.statefp,
                    t.countyfp,
                    t.geoid,
                    t.name,
                    t.namelsad
                FROM gis_discovery_target t
                WHERE t.target_type = 'county'
                  AND t.source_table = 'us_counties'
                  AND t.statefp = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM gis_url_candidate c
                      WHERE c.source_table = t.source_table
                        AND c.source_ogc_fid = t.source_ogc_fid
                  )
                  AND COALESCE(t.discovery_status, '') <> 'failed'
                ORDER BY t.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GisDiscoveryTarget(
                rs.getLong("id"),
                rs.getString("target_type"),
                rs.getString("source_table"),
                rs.getLong("source_ogc_fid"),
                rs.getString("statefp"),
                rs.getString("countyfp"),
                rs.getString("geoid"),
                rs.getString("name"),
                rs.getString("namelsad")), statefp);
    }

    public List<GisDiscoveryTargetWithResearchContext> findCountyTargetsWithRawSearchResults(
            String statefp,
            String provider,
            String model) {
        String sql = """
                SELECT
                    t.id,
                    t.target_type,
                    t.source_table,
                    t.source_ogc_fid,
                    t.statefp,
                    t.countyfp,
                    t.geoid,
                    t.name,
                    t.namelsad,
                    r.raw_result
                FROM gis_discovery_target t
                JOIN gis_search_raw_result r
                    ON r.source_table = t.source_table
                   AND r.source_ogc_fid = t.source_ogc_fid
                   AND r.provider = ?
                   AND r.model = ?
                WHERE t.target_type = 'county'
                  AND t.source_table = 'us_counties'
                  AND t.statefp = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM gis_url_candidate c
                      WHERE c.source_table = t.source_table
                        AND c.source_ogc_fid = t.source_ogc_fid
                  )
                  AND COALESCE(t.discovery_status, '') <> 'failed'
                ORDER BY t.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GisDiscoveryTargetWithResearchContext(
                new GisDiscoveryTarget(
                        rs.getLong("id"),
                        rs.getString("target_type"),
                        rs.getString("source_table"),
                        rs.getLong("source_ogc_fid"),
                        rs.getString("statefp"),
                        rs.getString("countyfp"),
                        rs.getString("geoid"),
                        rs.getString("name"),
                        rs.getString("namelsad")),
                rs.getString("raw_result")), provider, model, statefp);
    }

    public List<GisDiscoveryTarget> findCountyTargetsWithoutRawSearchResults(
            String statefp,
            String provider,
            String model) {
        String sql = """
                SELECT
                    t.id,
                    t.target_type,
                    t.source_table,
                    t.source_ogc_fid,
                    t.statefp,
                    t.countyfp,
                    t.geoid,
                    t.name,
                    t.namelsad
                FROM gis_discovery_target t
                WHERE t.target_type = 'county'
                  AND t.source_table = 'us_counties'
                  AND t.statefp = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM gis_search_raw_result r
                      WHERE r.source_table = t.source_table
                        AND r.source_ogc_fid = t.source_ogc_fid
                        AND r.provider = ?
                        AND r.model = ?
                  )
                ORDER BY t.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GisDiscoveryTarget(
                rs.getLong("id"),
                rs.getString("target_type"),
                rs.getString("source_table"),
                rs.getLong("source_ogc_fid"),
                rs.getString("statefp"),
                rs.getString("countyfp"),
                rs.getString("geoid"),
                rs.getString("name"),
                rs.getString("namelsad")), statefp, provider, model);
    }

    @Transactional
    public int upsertRawSearchResult(
            GisDiscoveryTarget target,
            String stateName,
            String provider,
            String model,
            String prompt,
            String rawResult) {
        String sql = """
                INSERT INTO gis_search_raw_result
                    (source_table, source_ogc_fid, entity_name, state, entity_type,
                     provider, model, prompt, raw_result)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_table, source_ogc_fid, provider, model)
                DO UPDATE SET
                    entity_name = EXCLUDED.entity_name,
                    state = EXCLUDED.state,
                    entity_type = EXCLUDED.entity_type,
                    prompt = EXCLUDED.prompt,
                    raw_result = EXCLUDED.raw_result,
                    updated_at = NOW()
                """;
        return jdbcTemplate.update(
                sql,
                target.sourceTable(),
                target.sourceOgcFid(),
                target.name(),
                stateName,
                target.targetType(),
                provider,
                model,
                prompt,
                rawResult);
    }

    @Transactional
    public int upsertCandidates(GisDiscoveryTarget target, String stateName, JsonNode rawResponse) {
        JsonNode urlsNode = rawResponse.path("top_ranked_urls");
        if (!urlsNode.isArray()) {
            throw new IllegalArgumentException("LLM response for " + target.name()
                    + " did not contain top_ranked_urls[].");
        }

        int updated = 0;
        for (JsonNode urlNode : urlsNode) {
            updated += upsertCandidate(target, stateName, rawResponse, urlNode);
        }
        return updated;
    }

    @Transactional
    public int markDiscoverySucceeded(long targetId) {
        String sql = """
                UPDATE gis_discovery_target
                SET discovery_status = 'succeeded',
                    discovery_error = NULL,
                    last_discovery_attempt_at = NOW()
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, targetId);
    }

    @Transactional
    public int markDiscoveryFailed(long targetId, Exception exception) {
        String sql = """
                UPDATE gis_discovery_target
                SET discovery_status = 'failed',
                    discovery_error = ?,
                    last_discovery_attempt_at = NOW()
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, truncateError(exception), targetId);
    }

    private String truncateError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getName();
        }
        if (message.length() > 4000) {
            return message.substring(0, 4000);
        }
        return message;
    }

    private int upsertCandidate(
            GisDiscoveryTarget target,
            String stateName,
            JsonNode rawResponse,
            JsonNode urlNode) {
        String rawResponseJson;
        try {
            rawResponseJson = objectMapper.writeValueAsString(rawResponse);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize LLM raw response.", exception);
        }

        String sql = """
                INSERT INTO gis_url_candidate
                    (source_table, source_ogc_fid, entity_name, state, entity_type,
                     rank, url, normalized_url, endpoint_type, ownership_type,
                     validation_status, http_status, json_keys_found, sample_title_or_service_name,
                     found_from, source_url, source_title, confidence, reasoning, raw_response)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (source_table, source_ogc_fid, normalized_url)
                DO UPDATE SET
                    entity_name = EXCLUDED.entity_name,
                    state = EXCLUDED.state,
                    entity_type = EXCLUDED.entity_type,
                    rank = EXCLUDED.rank,
                    url = EXCLUDED.url,
                    endpoint_type = EXCLUDED.endpoint_type,
                    ownership_type = EXCLUDED.ownership_type,
                    validation_status = EXCLUDED.validation_status,
                    http_status = EXCLUDED.http_status,
                    json_keys_found = EXCLUDED.json_keys_found,
                    sample_title_or_service_name = EXCLUDED.sample_title_or_service_name,
                    found_from = EXCLUDED.found_from,
                    source_url = EXCLUDED.source_url,
                    source_title = EXCLUDED.source_title,
                    confidence = EXCLUDED.confidence,
                    reasoning = EXCLUDED.reasoning,
                    raw_response = EXCLUDED.raw_response
                """;

        return jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, target.sourceTable());
            statement.setLong(2, target.sourceOgcFid());
            statement.setString(3, target.name());
            statement.setString(4, stateName);
            statement.setString(5, target.targetType());
            statement.setInt(6, urlNode.path("rank").asInt());
            statement.setString(7, requiredText(urlNode, "url"));
            statement.setString(8, requiredText(urlNode, "normalized_url"));
            statement.setString(9, requiredText(urlNode, "endpoint_type"));
            statement.setString(10, nullableText(urlNode, "ownership_type"));
            statement.setString(11, nullableText(urlNode, "validation_status"));
            setNullableInteger(statement, 12, urlNode.path("validation_evidence").path("http_status"));
            Array jsonKeysArray = connection.createArrayOf(
                    "text",
                    jsonKeys(urlNode.path("validation_evidence").path("json_keys_found")).toArray(String[]::new));
            statement.setArray(13, jsonKeysArray);
            statement.setString(14, nullableText(urlNode.path("validation_evidence"), "sample_title_or_service_name"));
            statement.setString(15, nullableText(urlNode.path("source_evidence"), "found_from"));
            statement.setString(16, nullableText(urlNode.path("source_evidence"), "source_url"));
            statement.setString(17, nullableText(urlNode.path("source_evidence"), "source_title"));
            statement.setString(18, nullableText(urlNode, "confidence"));
            statement.setString(19, nullableText(urlNode, "reasoning"));
            statement.setString(20, rawResponseJson);
            return statement;
        });
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = nullableText(node, fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("LLM response is missing required field: " + fieldName);
        }
        return value;
    }

    private String nullableText(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        return valueNode.asText();
    }

    private void setNullableInteger(PreparedStatement statement, int parameterIndex, JsonNode valueNode)
            throws java.sql.SQLException {
        if (valueNode.isInt() || valueNode.isLong()) {
            statement.setInt(parameterIndex, valueNode.asInt());
            return;
        }
        if (valueNode.isTextual()) {
            try {
                statement.setInt(parameterIndex, Integer.parseInt(valueNode.asText()));
                return;
            } catch (NumberFormatException ignored) {
                // Store unknown/non-numeric status as null.
            }
        }
        statement.setNull(parameterIndex, java.sql.Types.INTEGER);
    }

    private List<String> jsonKeys(JsonNode keysNode) {
        List<String> keys = new ArrayList<>();
        if (!keysNode.isArray()) {
            return keys;
        }
        for (JsonNode keyNode : keysNode) {
            if (keyNode.isTextual()) {
                keys.add(keyNode.asText());
            }
        }
        return keys;
    }
}
