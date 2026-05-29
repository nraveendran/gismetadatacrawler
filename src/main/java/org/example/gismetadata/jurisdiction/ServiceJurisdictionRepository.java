package org.example.gismetadata.jurisdiction;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ServiceJurisdictionRepository {
    private final JdbcTemplate jdbcTemplate;

    public ServiceJurisdictionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public int insertCityMatches() {
        String sql = """
                INSERT INTO service_jurisdiction_matches
                    (service_id, jurisdiction_type, jurisdiction_id, relationship_type, match_method,
                     confidence, evidence)
                SELECT DISTINCT
                    s.id,
                    'city',
                    p.ogc_fid,
                    'mentioned',
                    'service_name_place_name_contains',
                    0.9,
                    jsonb_build_object(
                        'place_name', p.name,
                        'folder_name', s.folder_name,
                        'service_name', s.service_name,
                        'matched_text', service_text.normalized_text
                    )
                FROM arcgis_services s
                CROSS JOIN LATERAL (
                    SELECT lower(regexp_replace(concat_ws(' ', s.folder_name, s.service_name), '[^a-zA-Z0-9]+', '', 'g')) AS normalized_text
                ) service_text
                JOIN us_places p
                    ON service_text.normalized_text LIKE
                       ('%' || lower(regexp_replace(p.name, '[^a-zA-Z0-9]+', '', 'g')) || '%')
                WHERE service_text.normalized_text NOT LIKE '%county%'
                  AND p.name IS NOT NULL
                  AND length(trim(p.name)) > 1
                ON CONFLICT (service_id, jurisdiction_type, jurisdiction_id, relationship_type, match_method)
                DO UPDATE SET
                    confidence = EXCLUDED.confidence,
                    evidence = EXCLUDED.evidence
                """;

        return jdbcTemplate.update(sql);
    }

    @Transactional
    public int insertCountyMatches() {
        String sql = """
                INSERT INTO service_jurisdiction_matches
                    (service_id, jurisdiction_type, jurisdiction_id, relationship_type, match_method,
                     confidence, evidence)
                SELECT DISTINCT
                    s.id,
                    'county',
                    c.ogc_fid,
                    'mentioned',
                    'service_name_county_name_contains',
                    0.9,
                    jsonb_build_object(
                        'county_name', c.name,
                        'folder_name', s.folder_name,
                        'service_name', s.service_name,
                        'matched_text', service_text.normalized_text
                    )
                FROM arcgis_services s
                CROSS JOIN LATERAL (
                    SELECT lower(regexp_replace(concat_ws(' ', s.folder_name, s.service_name), '[^a-zA-Z0-9]+', '', 'g')) AS normalized_text
                ) service_text
                JOIN us_counties c
                    ON service_text.normalized_text LIKE
                       ('%' || lower(regexp_replace(c.name, '[^a-zA-Z0-9]+', '', 'g')) || '%')
                WHERE service_text.normalized_text NOT LIKE '%city%'
                  AND c.name IS NOT NULL
                  AND length(trim(c.name)) > 1
                ON CONFLICT (service_id, jurisdiction_type, jurisdiction_id, relationship_type, match_method)
                DO UPDATE SET
                    confidence = EXCLUDED.confidence,
                    evidence = EXCLUDED.evidence
                """;

        return jdbcTemplate.update(sql);
    }

    @Transactional
    public int insertCogMatchesForUnmatchedServices() {
        String sql = """
                INSERT INTO service_jurisdiction_matches
                    (service_id, jurisdiction_type, jurisdiction_id, relationship_type, match_method,
                     confidence, evidence)
                SELECT DISTINCT
                    s.id,
                    'COG',
                    c.id,
                    'mentioned',
                    'service_text_cog_name_contains',
                    0.85,
                    jsonb_build_object(
                        'cog_name', c.cog_name,
                        'cog_abbreviation', c.cog_abbreviation,
                        'folder_name', s.folder_name,
                        'service_name', s.service_name,
                        'service_url', s.server_url,
                        'matched_text', service_text.normalized_text
                    )
                FROM arcgis_services s
                CROSS JOIN LATERAL (
                    SELECT lower(regexp_replace(concat_ws(' ', s.folder_name, s.service_name, s.server_url), '[^a-zA-Z0-9]+', '', 'g')) AS normalized_text
                ) service_text
                JOIN texas_cogs c
                    ON service_text.normalized_text LIKE
                       ('%' || lower(regexp_replace(c.cog_abbreviation, '[^a-zA-Z0-9]+', '', 'g')) || '%')
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM service_jurisdiction_matches existing
                    WHERE existing.service_id = s.id
                )
                  AND c.cog_name IS NOT NULL
                  AND length(trim(c.cog_name)) > 1
                ON CONFLICT (service_id, jurisdiction_type, jurisdiction_id, relationship_type, match_method)
                DO UPDATE SET
                    confidence = EXCLUDED.confidence,
                    evidence = EXCLUDED.evidence
                """;

        return jdbcTemplate.update(sql);
    }
}
