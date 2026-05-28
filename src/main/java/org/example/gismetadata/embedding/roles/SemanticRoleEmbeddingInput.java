package org.example.gismetadata.embedding.roles;

record SemanticRoleEmbeddingInput(long id, String roleName, String description, String category) {
    String embeddingText() {
        return """
                role_name: %s
                description: %s
                category: %s
                """.formatted(
                nullToEmpty(roleName),
                nullToEmpty(description),
                nullToEmpty(category));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
