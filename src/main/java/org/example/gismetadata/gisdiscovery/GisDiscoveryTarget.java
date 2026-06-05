package org.example.gismetadata.gisdiscovery;

public record GisDiscoveryTarget(
        long id,
        String targetType,
        String sourceTable,
        long sourceOgcFid,
        String statefp,
        String countyfp,
        String geoid,
        String name,
        String namelsad) {
}
