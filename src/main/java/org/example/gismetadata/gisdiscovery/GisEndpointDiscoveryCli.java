package org.example.gismetadata.gisdiscovery;

import org.example.gismetadata.GisMetadataCrawlerApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class GisEndpointDiscoveryCli {
    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GisMetadataCrawlerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            context.getBean(GisEndpointDiscoveryRunner.class).run();
        }
    }
}
