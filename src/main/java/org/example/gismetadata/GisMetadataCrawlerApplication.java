package org.example.gismetadata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GisMetadataCrawlerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GisMetadataCrawlerApplication.class, args);
    }
}
