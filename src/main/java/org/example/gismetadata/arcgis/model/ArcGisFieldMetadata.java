package org.example.gismetadata.arcgis.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ArcGisFieldMetadata(
        String name,
        String alias,
        String type,
        String modelName,
        Boolean nullable,
        Boolean editable,
        JsonNode metadata) {
}
