package org.example.gismetadata.arcgis.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ArcGisLayerMetadata(int layerId, String name, String layerUrl, String layerType, String geometryType, JsonNode metadata) {
}
