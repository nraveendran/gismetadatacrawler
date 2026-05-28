package org.example.gismetadata.embedding.fields;

import org.example.gismetadata.embedding.PgVector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FieldEmbeddingRepository {
    private final JdbcTemplate jdbcTemplate;

    public FieldEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FieldEmbeddingInput> findRowsWithoutEmbeddings(int limit) {
        String sql = """
                SELECT f.id,
                       l.layer_name,
                       f.field_name,
                       f.field_alias,
                       f.field_type
                FROM arcgis_fields f
                JOIN arcgis_layers l ON l.id = f.layer_table_id
                WHERE f.embedding_vector IS NULL
                ORDER BY f.id
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new FieldEmbeddingInput(
                resultSet.getLong("id"),
                resultSet.getString("layer_name"),
                resultSet.getString("field_name"),
                resultSet.getString("field_alias"),
                resultSet.getString("field_type")), limit);
    }

    @Transactional
    public void updateEmbeddings(
            List<FieldEmbeddingInput> rows,
            List<List<BigDecimal>> embeddings,
            String embeddingModel) {
        if (rows.size() != embeddings.size()) {
            throw new IllegalStateException("Expected " + rows.size()
                    + " embeddings, but OpenAI returned " + embeddings.size());
        }

        String sql = """
                UPDATE arcgis_fields
                SET embedding_text = ?,
                    embedding_vector = ?::vector,
                    embedding_model = ?,
                    embedding_created_at = NOW()
                WHERE id = ?
                """;

        List<Object[]> batchArgs = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            FieldEmbeddingInput row = rows.get(i);
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
