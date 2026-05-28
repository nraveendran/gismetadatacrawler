package org.example.gismetadata.embedding.layers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.gismetadata.config.EmbeddingBackfillProperties;
import org.example.gismetadata.config.OpenAiEmbeddingProperties;
import org.example.gismetadata.embedding.openai.OpenAiEmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.util.List;

@Service
public class LayerEmbeddingBackfill {
    private static final Logger log = LoggerFactory.getLogger(LayerEmbeddingBackfill.class);

    private final OpenAiEmbeddingProperties openAiProperties;
    private final EmbeddingBackfillProperties backfillProperties;
    private final LayerEmbeddingRepository repository;
    private final ObjectMapper objectMapper;

    public LayerEmbeddingBackfill(
            OpenAiEmbeddingProperties openAiProperties,
            EmbeddingBackfillProperties backfillProperties,
            LayerEmbeddingRepository repository,
            ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.backfillProperties = backfillProperties;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void run() throws Exception {
        openAiProperties.validate();
        backfillProperties.validate();

        OpenAiEmbeddingClient embeddingClient = new OpenAiEmbeddingClient(
                openAiProperties.apiKey(),
                openAiProperties.embeddingModel(),
                HttpClient.newHttpClient(),
                objectMapper);

        int totalUpdated = 0;
        log.info(
                "Starting ArcGIS layer embedding backfill. batchSize={}, maxTotalRecords={}, model={}",
                backfillProperties.batchSize(),
                backfillProperties.maxTotalRecords(),
                openAiProperties.embeddingModel());

        while (totalUpdated < backfillProperties.maxTotalRecords()) {
            int remainingRecords = backfillProperties.maxTotalRecords() - totalUpdated;
            int fetchLimit = Math.min(backfillProperties.batchSize(), remainingRecords);
            List<LayerEmbeddingInput> rows = repository.findRowsWithoutEmbeddings(fetchLimit);
            if (rows.isEmpty()) {
                log.info("No arcgis_layers rows left without embeddings.");
                break;
            }

            log.info("Fetched {} arcgis_layers rows without embeddings.", rows.size());
            List<List<BigDecimal>> embeddings = embeddingClient.createEmbeddings(
                    rows.stream()
                            .map(LayerEmbeddingInput::embeddingText)
                            .toList());

            repository.updateEmbeddings(rows, embeddings, openAiProperties.embeddingModel());
            totalUpdated += rows.size();
            log.info("Updated {} rows in this batch; {} total.", rows.size(), totalUpdated);
        }

        log.info("ArcGIS layer embedding backfill finished. totalUpdated={}", totalUpdated);
    }
}
