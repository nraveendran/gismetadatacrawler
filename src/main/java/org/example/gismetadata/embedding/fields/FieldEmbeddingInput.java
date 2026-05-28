package org.example.gismetadata.embedding.fields;

record FieldEmbeddingInput(
        long id,
        String layerName,
        String fieldName,
        String fieldAlias,
        String fieldType) {
    String embeddingText() {
        return """
                field_name: %s
                field_alias: %s
                field_type: %s
                """.formatted(
                nullToEmpty(fieldName),
                nullToEmpty(fieldAlias),
                nullToEmpty(fieldType));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
