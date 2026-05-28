package org.example.gismetadata.embedding.layers;

import java.util.List;
import java.util.stream.Collectors;

record LayerEmbeddingInput(long id, String layerName, List<String> fieldNames) {
    String embeddingText() {
        return """
                layer_name: %s
                field_names: [%s]
                """.formatted(
                nullToEmpty(layerName),
                fieldNames.stream()
                        .map(LayerEmbeddingInput::nullToEmpty)
                        .collect(Collectors.joining(", ")));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
