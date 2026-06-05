package org.example.gismetadata.gisdiscovery;

import com.fasterxml.jackson.databind.JsonNode;

public interface GisDiscoveryLlmClient {
    JsonNode discover(String prompt) throws Exception;
}
