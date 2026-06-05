package org.example.gismetadata.spatial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CountyCityRelationshipBuilder {
    private static final Logger log = LoggerFactory.getLogger(CountyCityRelationshipBuilder.class);

    private final JdbcTemplate jdbcTemplate;

    public CountyCityRelationshipBuilder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void build() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");

        int countyCityUpdated = buildCountyCityRelationships();
        int cogCountyUpdated = buildCogCountyRelationships();
        log.info(
                "Spatial relationship build finished. countyCityRelationships={}, cogCountyRelationships={}",
                countyCityUpdated,
                cogCountyUpdated);
    }

    private int buildCountyCityRelationships() {
        String sql = """
                INSERT INTO county_city_relationships
                    (county_id, city_id, relationship_type)
                SELECT
                    c.ogc_fid,
                    p.ogc_fid,
                    CASE
                        WHEN ST_Contains(c.geom, ST_PointOnSurface(p.geom)) THEN 'contains'
                        ELSE 'intersects'
                    END
                FROM us_counties c
                JOIN us_places p
                    ON ST_Intersects(c.geom, p.geom)
                ON CONFLICT (county_id, city_id)
                DO UPDATE SET
                    relationship_type = EXCLUDED.relationship_type,
                    created_at = NOW()
                """;

        int updated = jdbcTemplate.update(sql);
        log.info("Upserted {} county-city spatial relationships.", updated);
        return updated;
    }

    private int buildCogCountyRelationships() {
        String sql = """
                INSERT INTO cog_county_relationships
                    (cog_id, county_id, relationship_type)
                SELECT
                    cog.ogc_fid,
                    county.ogc_fid,
                    CASE
                        WHEN ST_Contains(cog.geom, ST_PointOnSurface(county.geom)) THEN 'contains'
                        ELSE 'intersects'
                    END
                FROM us_cogs cog
                JOIN us_counties county
                    ON ST_Intersects(cog.geom, county.geom)
                ON CONFLICT (cog_id, county_id)
                DO UPDATE SET
                    relationship_type = EXCLUDED.relationship_type,
                    created_at = NOW()
                """;

        int updated = jdbcTemplate.update(sql);
        log.info("Upserted {} COG-county spatial relationships.", updated);
        return updated;
    }
}
