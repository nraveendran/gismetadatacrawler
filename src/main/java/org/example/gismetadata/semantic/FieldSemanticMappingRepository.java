package org.example.gismetadata.semantic;

import java.sql.Array;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class FieldSemanticMappingRepository {
    private final JdbcTemplate jdbcTemplate;

    public FieldSemanticMappingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> findFieldIdsWithEmbeddingsAfter(long lastFieldId, int limit) {
        String sql = """
                SELECT id
                FROM arcgis_fields
                WHERE embedding_vector IS NOT NULL
                  AND id > ?
                ORDER BY id
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> resultSet.getLong("id"), lastFieldId, limit);
    }

    @Transactional
    public void storeCandidatesAndApplyAutoMappings(List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return;
        }

        storeTopCandidates(fieldIds);
        applyAutomaticMappings(fieldIds);
    }

    private void storeTopCandidates(List<Long> fieldIds) {
        String sql = """
                WITH ranked_candidates AS (
                    SELECT
                        f.id AS field_id,
                        r.id AS semantic_role_id,
                        (1 - (f.embedding_vector <=> r.embedding_vector))::numeric AS similarity_score,
                        ROW_NUMBER() OVER (
                            PARTITION BY f.id
                            ORDER BY f.embedding_vector <=> r.embedding_vector
                        ) AS candidate_rank
                    FROM arcgis_fields f
                    CROSS JOIN semantic_roles r
                    WHERE f.id = ANY (?)
                      AND f.embedding_vector IS NOT NULL
                      AND r.embedding_vector IS NOT NULL
                      AND r.is_active = TRUE
                )
                INSERT INTO field_semantic_candidates
                    (field_id, semantic_role_id, similarity_score, rank, method)
                SELECT field_id, semantic_role_id, similarity_score, candidate_rank, 'embedding'
                FROM ranked_candidates
                WHERE candidate_rank <= 3
                ON CONFLICT (field_id, semantic_role_id)
                DO UPDATE SET
                    similarity_score = EXCLUDED.similarity_score,
                    rank = EXCLUDED.rank,
                    method = EXCLUDED.method,
                    created_at = NOW()
                """;

        executeWithFieldIdArray(sql, fieldIds);
    }

    private void applyAutomaticMappings(List<Long> fieldIds) {
        String sql = """
                WITH ranked AS (
                    SELECT
                        c.field_id,
                        r.role_name,
                        c.similarity_score,
                        c.rank,
                        LEAD(c.similarity_score) OVER (
                            PARTITION BY c.field_id
                            ORDER BY c.rank
                        ) AS next_similarity_score
                    FROM field_semantic_candidates c
                    JOIN semantic_roles r ON r.id = c.semantic_role_id
                    WHERE c.field_id = ANY (?)
                      AND c.rank <= 2
                ),
                winning AS (
                    SELECT
                        field_id,
                        role_name,
                        similarity_score,
                        similarity_score - COALESCE(next_similarity_score, 0) AS margin
                    FROM ranked
                    WHERE rank = 1
                )
                UPDATE arcgis_fields f
                SET semantic_role = w.role_name,
                    semantic_confidence = w.similarity_score,
                    semantic_mapping_method = 'embedding_auto'
                FROM winning w
                WHERE f.id = w.field_id
                  AND w.similarity_score >= 0.5
                """;

        executeWithFieldIdArray(sql, fieldIds);
    }

    private void executeWithFieldIdArray(String sql, List<Long> fieldIds) {
        PreparedStatementCreator statementCreator = connection -> {
            Array fieldIdArray = connection.createArrayOf("int8", fieldIds.toArray());
            var statement = connection.prepareStatement(sql);
            statement.setArray(1, fieldIdArray);
            return statement;
        };
        PreparedStatementCallback<Void> callback = statement -> {
            statement.executeUpdate();
            return null;
        };

        jdbcTemplate.execute(statementCreator, callback);
    }
}
