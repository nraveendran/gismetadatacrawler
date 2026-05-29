package org.example.gismetadata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gismetadatacrawler.county-loader")
public record CountyShapefileLoadProperties(
        String shapefilePath,
        String tableName,
        String ogr2ogrPath,
        String mode) {
    public CountyShapefileLoadProperties {
        if (shapefilePath == null || shapefilePath.isBlank()) {
            shapefilePath = "/Users/nidhishnair/Documents/spatial/tl_2025_us_county/tl_2025_us_county.shp";
        }
        if (tableName == null || tableName.isBlank()) {
            tableName = "us_counties";
        }
        if (ogr2ogrPath == null || ogr2ogrPath.isBlank()) {
            ogr2ogrPath = "/opt/homebrew/bin/ogr2ogr";
        }
        if (mode == null || mode.isBlank()) {
            mode = "overwrite";
        }
    }

    public void validate() {
        if (!mode.equals("overwrite") && !mode.equals("append")) {
            throw new IllegalStateException("gismetadatacrawler.county-loader.mode must be overwrite or append.");
        }
    }
}
