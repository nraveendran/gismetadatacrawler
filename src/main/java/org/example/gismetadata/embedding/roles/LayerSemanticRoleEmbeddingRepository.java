package org.example.gismetadata.embedding.roles;

import org.example.gismetadata.embedding.PgVector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class LayerSemanticRoleEmbeddingRepository {
    private final JdbcTemplate jdbcTemplate;

    public LayerSemanticRoleEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LayerSemanticRoleEmbeddingInput> findRowsWithoutEmbeddings(int limit) {
        String sql = """
                SELECT id,
                       role_name,
                       description,
                       category,
                       example_layer_names::text AS example_layer_names
                FROM layer_semantic_roles
                WHERE embedding_vector IS NULL
                  AND is_active = TRUE
                ORDER BY id
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new LayerSemanticRoleEmbeddingInput(
                resultSet.getLong("id"),
                resultSet.getString("role_name"),
                resultSet.getString("description"),
                resultSet.getString("category"),
                resultSet.getString("example_layer_names")), limit);
    }

    @Transactional
    public void updateEmbeddings(
            List<LayerSemanticRoleEmbeddingInput> rows,
            List<List<BigDecimal>> embeddings,
            String embeddingModel) {
        if (rows.size() != embeddings.size()) {
            throw new IllegalStateException("Expected " + rows.size()
                    + " embeddings, but OpenAI returned " + embeddings.size());
        }

        String sql = """
                UPDATE layer_semantic_roles
                SET embedding_text = ?,
                    embedding_vector = ?::vector,
                    embedding_model = ?,
                    updated_at = NOW()
                WHERE id = ?
                """;

        List<Object[]> batchArgs = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            LayerSemanticRoleEmbeddingInput row = rows.get(i);
            batchArgs.add(new Object[] {
                    row.embeddingText(),
                    PgVector.toLiteral(embeddings.get(i)),
                    embeddingModel,
                    row.id()
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
