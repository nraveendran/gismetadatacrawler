package org.example.gismetadata.embedding.roles;

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
public class SemanticRoleEmbeddingBackfill {
    private static final Logger log = LoggerFactory.getLogger(SemanticRoleEmbeddingBackfill.class);

    private final OpenAiEmbeddingProperties openAiProperties;
    private final EmbeddingBackfillProperties backfillProperties;
    private final SemanticRoleEmbeddingRepository repository;
    private final ObjectMapper objectMapper;

    public SemanticRoleEmbeddingBackfill(
            OpenAiEmbeddingProperties openAiProperties,
            EmbeddingBackfillProperties backfillProperties,
            SemanticRoleEmbeddingRepository repository,
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
                "Starting semantic role embedding backfill. batchSize={}, maxTotalRecords={}, model={}",
                backfillProperties.batchSize(),
                backfillProperties.maxTotalRecords(),
                openAiProperties.embeddingModel());

        while (totalUpdated < backfillProperties.maxTotalRecords()) {
            int remainingRecords = backfillProperties.maxTotalRecords() - totalUpdated;
            int fetchLimit = Math.min(backfillProperties.batchSize(), remainingRecords);
            List<SemanticRoleEmbeddingInput> rows = repository.findRowsWithoutEmbeddings(fetchLimit);
            if (rows.isEmpty()) {
                log.info("No semantic_roles rows left without embeddings.");
                break;
            }

            log.info("Fetched {} semantic_roles rows without embeddings.", rows.size());
            List<List<BigDecimal>> embeddings = embeddingClient.createEmbeddings(
                    rows.stream()
                            .map(SemanticRoleEmbeddingInput::embeddingText)
                            .toList());

            repository.updateEmbeddings(rows, embeddings, openAiProperties.embeddingModel());
            totalUpdated += rows.size();
            log.info("Updated {} rows in this batch; {} total.", rows.size(), totalUpdated);
        }

        log.info("Semantic role embedding backfill finished. totalUpdated={}", totalUpdated);
    }
}
