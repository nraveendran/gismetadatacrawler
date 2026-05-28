package org.example.gismetadata.embedding.layers;

import org.example.gismetadata.embedding.PgVector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Repository
public class LayerEmbeddingRepository {
    private final JdbcTemplate jdbcTemplate;

    public LayerEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LayerEmbeddingInput> findRowsWithoutEmbeddings(int limit) {
        String sql = """
                SELECT
                    l.id,
                    l.layer_name,
                    ARRAY_REMOVE(ARRAY_AGG(f.field_name ORDER BY f.field_name), NULL) AS field_names
                FROM arcgis_layers l
                LEFT JOIN arcgis_fields f ON f.layer_table_id = l.id
                WHERE l.embedding_vector IS NULL
                  AND l.layer_name IS NOT NULL
                GROUP BY l.id, l.layer_name
                ORDER BY l.id
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new LayerEmbeddingInput(
                resultSet.getLong("id"),
                resultSet.getString("layer_name"),
                stringArray(resultSet.getArray("field_names"))), limit);
    }

    @Transactional
    public void updateEmbeddings(
            List<LayerEmbeddingInput> rows,
            List<List<BigDecimal>> embeddings,
            String embeddingModel) {
        if (rows.size() != embeddings.size()) {
            throw new IllegalStateException("Expected " + rows.size()
                    + " embeddings, but OpenAI returned " + embeddings.size());
        }

        String sql = """
                UPDATE arcgis_layers
                SET embedding_text = ?,
                    embedding_vector = ?::vector,
                    embedding_model = ?,
                    embedding_created_at = NOW()
                WHERE id = ?
                """;

        List<Object[]> batchArgs = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            LayerEmbeddingInput row = rows.get(i);
            batchArgs.add(new Object[] {
                    row.embeddingText(),
                    PgVector.toLiteral(embeddings.get(i)),
                    embeddingModel,
                    row.id()
            });
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private static List<String> stringArray(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }

        Object value = array.getArray();
        if (value instanceof String[] strings) {
            return Arrays.asList(strings);
        }
        if (value instanceof Object[] objects) {
            return Arrays.stream(objects)
                    .map(String.class::cast)
                    .toList();
        }

        return List.of();
    }
}
