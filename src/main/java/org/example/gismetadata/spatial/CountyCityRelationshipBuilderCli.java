package org.example.gismetadata.spatial;

import org.example.gismetadata.GisMetadataCrawlerApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CountyCityRelationshipBuilderCli {
    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GisMetadataCrawlerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            context.getBean(CountyCityRelationshipBuilder.class).build();
        }
    }
}
