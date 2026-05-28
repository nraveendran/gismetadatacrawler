package org.example.gismetadata.embedding.fields;

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
public class FieldEmbeddingBackfill {
    private static final Logger log = LoggerFactory.getLogger(FieldEmbeddingBackfill.class);

    private final OpenAiEmbeddingProperties openAiProperties;
    private final EmbeddingBackfillProperties backfillProperties;
    private final FieldEmbeddingRepository repository;
    private final ObjectMapper objectMapper;

    public FieldEmbeddingBackfill(
            OpenAiEmbeddingProperties openAiProperties,
            EmbeddingBackfillProperties backfillProperties,
            FieldEmbeddingRepository repository,
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
                "Starting ArcGIS field embedding backfill. batchSize={}, maxTotalRecords={}, model={}",
                backfillProperties.batchSize(),
                backfillProperties.maxTotalRecords(),
                openAiProperties.embeddingModel());

        while (totalUpdated < backfillProperties.maxTotalRecords()) {
            int remainingRecords = backfillProperties.maxTotalRecords() - totalUpdated;
            int fetchLimit = Math.min(backfillProperties.batchSize(), remainingRecords);
            List<FieldEmbeddingInput> rows = repository.findRowsWithoutEmbeddings(fetchLimit);
            if (rows.isEmpty()) {
                log.info("No arcgis_fields rows left without embeddings.");
                break;
            }

            log.info("Fetched {} arcgis_fields rows without embeddings.", rows.size());
            List<List<BigDecimal>> embeddings = embeddingClient.createEmbeddings(
                    rows.stream()
                            .map(FieldEmbeddingInput::embeddingText)
                            .toList());

            repository.updateEmbeddings(rows, embeddings, openAiProperties.embeddingModel());
            totalUpdated += rows.size();
            log.info("Updated {} rows in this batch; {} total.", rows.size(), totalUpdated);
        }

        log.info("ArcGIS field embedding backfill finished. totalUpdated={}", totalUpdated);
    }
}
