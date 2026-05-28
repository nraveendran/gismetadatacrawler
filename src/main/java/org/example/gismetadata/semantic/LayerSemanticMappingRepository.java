package org.example.gismetadata.semantic;

import java.sql.Array;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LayerSemanticMappingRepository {
    private final JdbcTemplate jdbcTemplate;

    public LayerSemanticMappingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findLayerIdsWithEmbeddingsAfter(long lastLayerId, int limit) {
        String sql = """
                SELECT id
                FROM arcgis_layers
                WHERE embedding_vector IS NOT NULL
                  AND id > ?
                ORDER BY id
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> resultSet.getLong("id"), lastLayerId, limit);
    }

    @Transactional
    public void storeCandidatesAndApplyAutoMappings(List<Long> layerIds) {
        if (layerIds.isEmpty()) {
            return;
        }

        storeTopCandidates(layerIds);
        applyAutomaticMappings(layerIds);
    }

    private void storeTopCandidates(List<Long> layerIds) {
        String sql = """
                WITH ranked_candidates AS (
                    SELECT
                        l.id AS layer_id,
                        r.id AS layer_semantic_role_id,
                        (1 - (l.embedding_vector <=> r.embedding_vector))::numeric AS similarity_score,
                        ROW_NUMBER() OVER (
                            PARTITION BY l.id
                            ORDER BY l.embedding_vector <=> r.embedding_vector
                        ) AS candidate_rank
                    FROM arcgis_layers l
                    CROSS JOIN layer_semantic_roles r
                    WHERE l.id = ANY (?)
                      AND l.embedding_vector IS NOT NULL
                      AND r.embedding_vector IS NOT NULL
                      AND r.is_active = TRUE
                )
                INSERT INTO layer_semantic_candidates
                    (layer_id, layer_semantic_role_id, similarity_score, rank, method)
                SELECT layer_id, layer_semantic_role_id, similarity_score, candidate_rank, 'embedding'
                FROM ranked_candidates
                WHERE candidate_rank <= 3
                ON CONFLICT (layer_id, layer_semantic_role_id)
                DO UPDATE SET
                    similarity_score = EXCLUDED.similarity_score,
                    rank = EXCLUDED.rank,
                    method = EXCLUDED.method,
                    created_at = NOW()
                """;

        executeWithLayerIdArray(sql, layerIds);
    }

    private void applyAutomaticMappings(List<Long> layerIds) {
        String sql = """
                WITH winning AS (
                    SELECT
                        c.layer_id,
                        c.layer_semantic_role_id,
                        c.similarity_score
                    FROM layer_semantic_candidates c
                    WHERE c.layer_id = ANY (?)
                      AND c.rank = 1
                )
                UPDATE arcgis_layers l
                SET layer_semantic_role_id = w.layer_semantic_role_id,
                    semantic_confidence = w.similarity_score,
                    semantic_mapping_method = 'embedding_auto'
                FROM winning w
                WHERE l.id = w.layer_id
                  AND w.similarity_score >= 0.5
                """;

        executeWithLayerIdArray(sql, layerIds);
    }

    private void executeWithLayerIdArray(String sql, List<Long> layerIds) {
        PreparedStatementCreator statementCreator = connection -> {
            Array layerIdArray = connection.createArrayOf("int8", layerIds.toArray());
            var statement = connection.prepareStatement(sql);
            statement.setArray(1, layerIdArray);
            return statement;
        };
        PreparedStatementCallback<Void> callback = statement -> {
            statement.executeUpdate();
            return null;
        };

        jdbcTemplate.execute(statementCreator, callback);
    }
}
