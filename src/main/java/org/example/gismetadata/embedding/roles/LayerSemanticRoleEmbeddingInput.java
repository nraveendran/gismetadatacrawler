package org.example.gismetadata.embedding.roles;

record LayerSemanticRoleEmbeddingInput(
        long id,
        String roleName,
        String description,
        String category,
        String exampleLayerNames) {
    String embeddingText() {
        return """
                role_name: %s
                description: %s
                category: %s
                example_layer_names: %s
                """.formatted(
                nullToEmpty(roleName),
                nullToEmpty(description),
                nullToEmpty(category),
                nullToEmpty(exampleLayerNames));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
