package org.example.gismetadata.embedding.roles;

import org.example.gismetadata.embedding.PgVector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SemanticRoleEmbeddingRepository {
    private final JdbcTemplate jdbcTemplate;

    public SemanticRoleEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SemanticRoleEmbeddingInput> findRowsWithoutEmbeddings(int limit) {
        String sql = """
                SELECT id, role_name, description, category
                FROM semantic_roles
                WHERE embedding_vector IS NULL
                  AND is_active = TRUE
                ORDER BY id
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new SemanticRoleEmbeddingInput(
                resultSet.getLong("id"),
                resultSet.getString("role_name"),
                resultSet.getString("description"),
                resultSet.getString("category")), limit);
    }

    @Transactional
    public void updateEmbeddings(
            List<SemanticRoleEmbeddingInput> rows,
            List<List<BigDecimal>> embeddings,
            String embeddingModel) {
        if (rows.size() != embeddings.size()) {
            throw new IllegalStateException("Expected " + rows.size()
                    + " embeddings, but OpenAI returned " + embeddings.size());
        }

        String sql = """
                UPDATE semantic_roles
                SET embedding_vector = ?::vector,
                    embedding_model = ?,
                    updated_at = NOW()
                WHERE id = ?
                """;

        List<Object[]> batchArgs = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            batchArgs.add(new Object[] {
                    PgVector.toLiteral(embeddings.get(i)),
                    embeddingModel,
                    rows.get(i).id()
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
