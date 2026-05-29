package org.example.gismetadata.spatial;

import java.net.URI;

record PostgresConnectionInfo(String host, int port, String database, String username, String password) {
    static PostgresConnectionInfo fromJdbcUrl(String jdbcUrl, String username, String password) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Only jdbc:postgresql://host:port/database URLs are supported.");
        }

        URI uri = URI.create("postgresql:" + jdbcUrl.substring("jdbc:postgresql:".length()));
        String database = uri.getPath();
        if (database == null || database.length() <= 1) {
            throw new IllegalArgumentException("JDBC URL must include a database name: " + jdbcUrl);
        }

        return new PostgresConnectionInfo(
                uri.getHost(),
                uri.getPort() == -1 ? 5432 : uri.getPort(),
                database.substring(1),
                username,
                password == null ? "" : password);
    }

    String toOgrConnectionString() {
        StringBuilder builder = new StringBuilder("PG:");
        builder.append("host=").append(host);
        builder.append(" port=").append(port);
        builder.append(" dbname=").append(database);
        if (username != null && !username.isBlank()) {
            builder.append(" user=").append(username);
        }
        if (password != null && !password.isBlank()) {
            builder.append(" password=").append(password);
        }
        return builder.toString();
    }
}
