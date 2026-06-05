package org.example.gismetadata.graph;

import org.example.gismetadata.GisMetadataCrawlerApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class Neo4jGraphExportCli {
    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GisMetadataCrawlerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            context.getBean(Neo4jGraphExporter.class).export();
        }
    }
}
