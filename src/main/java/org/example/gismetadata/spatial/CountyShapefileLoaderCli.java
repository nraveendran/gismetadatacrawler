package org.example.gismetadata.spatial;

import org.example.gismetadata.GisMetadataCrawlerApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CountyShapefileLoaderCli {
    public static void main(String[] args) throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(GisMetadataCrawlerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            CountyShapefileLoader loader = context.getBean(CountyShapefileLoader.class);
            if (args.length >= 2) {
                loader.load(args[0], args[1]);
            } else {
                loader.load();
            }
        }
    }
}
