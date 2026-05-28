package org.example.gismetadata.arcgis.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ArcGisFolderMetadata(String name, String url, JsonNode metadata) {
}
