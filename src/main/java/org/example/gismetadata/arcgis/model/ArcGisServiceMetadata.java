package org.example.gismetadata.arcgis.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ArcGisServiceMetadata(String name, String type, JsonNode metadata) {
}
