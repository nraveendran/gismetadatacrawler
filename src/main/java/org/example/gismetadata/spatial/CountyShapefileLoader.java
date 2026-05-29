package org.example.gismetadata.spatial;

import org.example.gismetadata.config.CountyShapefileLoadProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class CountyShapefileLoader {
    private static final Logger log = LoggerFactory.getLogger(CountyShapefileLoader.class);

    private final CountyShapefileLoadProperties properties;
    private final DataSourceProperties dataSourceProperties;
    private final JdbcTemplate jdbcTemplate;

    public CountyShapefileLoader(
            CountyShapefileLoadProperties properties,
            DataSourceProperties dataSourceProperties,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.dataSourceProperties = dataSourceProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void load() throws IOException, InterruptedException {
        properties.validate();
        load(properties.shapefilePath(), properties.tableName());
    }

    public void load(String shapefilePath, String tableName) throws IOException, InterruptedException {
        properties.validate();
        Path shapefile = Path.of(shapefilePath);
        if (!Files.isRegularFile(shapefile)) {
            throw new IllegalStateException("Shapefile does not exist: " + shapefile);
        }

        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");

        PostgresConnectionInfo connectionInfo = PostgresConnectionInfo.fromJdbcUrl(
                dataSourceProperties.getUrl(),
                dataSourceProperties.getUsername(),
                dataSourceProperties.getPassword());

        List<String> command = ogr2ogrCommand(connectionInfo, shapefile, tableName);
        log.info("Loading shapefile {} into table {} using ogr2ogr.", shapefile, tableName);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("ogr2ogr failed with exit code " + exitCode + ":\n" + output);
        }

        log.info("Shapefile loaded into {}. ogr2ogr output: {}", tableName, output.strip());
    }

    private List<String> ogr2ogrCommand(PostgresConnectionInfo connectionInfo, Path shapefile, String tableName) {
        List<String> command = new ArrayList<>();
        command.add(properties.ogr2ogrPath());
        command.add("-f");
        command.add("PostgreSQL");
        command.add(connectionInfo.toOgrConnectionString());
        command.add(shapefile.toString());
        command.add("-nln");
        command.add(tableName);
        command.add("-nlt");
        command.add("PROMOTE_TO_MULTI");
        command.add("-a_srs");
        command.add("EPSG:4269");
        command.add("-lco");
        command.add("GEOMETRY_NAME=geom");
        command.add("-lco");
        command.add("SPATIAL_INDEX=GIST");
        command.add("-lco");
        command.add("PRECISION=NO");
        command.add(properties.mode().equals("append") ? "-append" : "-overwrite");
        return command;
    }
}
