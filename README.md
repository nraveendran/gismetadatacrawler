# GIS Metadata Crawler

Java/Spring Boot command-line tools for crawling ArcGIS REST metadata into PostgreSQL, enriching layer and field metadata, creating OpenAI embeddings, and mapping fields to semantic roles with pgvector similarity.

## Requirements

- Java 17+
- PostgreSQL database named `gismetadata`
- `pgvector` extension for embedding columns
- OpenAI API key for embedding tasks

## Configuration

Runtime configuration lives in `src/main/resources/application.properties`.

Important environment variables:

```sh
DB_URL=jdbc:postgresql://localhost:5432/gismetadata
DB_USER=postgres
DB_PASSWORD=your_password
ARCGIS_ROOT_URL=https://maps.etcog.org/arcgis/rest/services
OPENAI_API_KEY=your_key
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_BATCH_SIZE=1000
EMBEDDING_MAX_TOTAL_RECORDS=30000
```

SQL initialization is disabled in Spring:

```properties
spring.sql.init.mode=never
```

Run SQL scripts manually.

## Database Setup

Run the SQL files against `gismetadata`:

```sh
psql -d gismetadata -f sql/001_create_crawl_sources.sql
psql -d gismetadata -f sql/002_create_semantic_roles.sql
psql -d gismetadata -f sql/003_create_layer_semantic_roles.sql
```

Core tables created so far:

- `crawl_sources`: raw root ArcGIS JSON by source URL
- `arcgis_folders`: folders discovered from ArcGIS root/folder responses
- `arcgis_services`: service rows from `services[]`
- `arcgis_layers`: FeatureServer layer rows and enriched layer metadata
- `arcgis_fields`: field rows and field embeddings
- `semantic_roles`: field-level semantic role dictionary
- `field_semantic_candidates`: top semantic matches for fields
- `layer_semantic_roles`: layer-level semantic role dictionary
- `layer_semantic_candidates`: top semantic matches for layers

## CLI Workflow

1. Crawl the ArcGIS service catalog:

```sh
./gradlew crawlArcGis
```

This fetches the root URL, stores raw JSON in `crawl_sources`, recursively stores `folders[]` in `arcgis_folders`, and stores `services[]` in `arcgis_services`.

2. Enrich FeatureServer services and create layer rows:

```sh
./gradlew crawlFeatureServerLayers
```

This selects `arcgis_services` where `service_type = 'FeatureServer'`, fetches each service JSON, updates `arcgis_services.metadata`, and upserts `layers[]` into `arcgis_layers`.

3. Enrich layer details and create field rows:

```sh
./gradlew crawlLayerDetails
```

This fetches each `arcgis_layers.layer_url`, updates layer metadata columns and raw JSON, and upserts `fields[]` into `arcgis_fields`.

4. Embed semantic roles:

```sh
OPENAI_API_KEY=your_key ./gradlew embedSemanticRoles
```

This embeds `role_name + description + category` for rows in `semantic_roles`.

5. Embed ArcGIS fields:

```sh
OPENAI_API_KEY=your_key ./gradlew embedArcGisFields
```

This embeds field text built from:

```text
field_name
field_alias
field_type
```

The text is stored in `arcgis_fields.embedding_text`; vectors are stored in `arcgis_fields.embedding_vector`.

6. Map fields to semantic roles:

```sh
./gradlew mapArcGisFieldSemantics
```

This stores the top 3 semantic role candidates in `field_semantic_candidates`. If the top score is at least `0.7` and the margin over rank 2 is at least `0.05`, it writes:

- `arcgis_fields.semantic_role`
- `arcgis_fields.semantic_confidence`
- `arcgis_fields.semantic_mapping_method = 'embedding_auto'`

## Package Layout

```text
org.example.gismetadata
  GisMetadataCrawlerApplication

org.example.gismetadata.config
  Spring configuration and typed properties

org.example.gismetadata.arcgis.crawler
  ArcGIS catalog, FeatureServer, and layer-detail CLI crawlers

org.example.gismetadata.arcgis.model
  Small records used by crawler/repository code

org.example.gismetadata.arcgis.repository
  PostgreSQL persistence for ArcGIS crawl data

org.example.gismetadata.embedding
  pgvector helper

org.example.gismetadata.embedding.openai
  OpenAI embeddings HTTP client

org.example.gismetadata.embedding.roles
  semantic_roles embedding backfill

org.example.gismetadata.embedding.fields
  arcgis_fields embedding backfill

org.example.gismetadata.semantic
  field-to-semantic-role candidate generation and auto-mapping
```

## Useful Queries

View candidate field mappings:

```sql
SELECT
    fsc.field_id,
    af.field_name,
    af.field_alias,
    af.field_type,
    al.layer_name,
    sr.role_name AS semantic_role_name,
    sr.category AS semantic_role_category,
    fsc.similarity_score,
    fsc.rank,
    fsc.method,
    fsc.created_at
FROM field_semantic_candidates fsc
JOIN arcgis_fields af ON af.id = fsc.field_id
JOIN arcgis_layers al ON al.id = af.layer_table_id
JOIN semantic_roles sr ON sr.id = fsc.semantic_role_id
WHERE fsc.similarity_score > 0.7
ORDER BY fsc.field_id, fsc.rank;
```

Reset field embeddings:

```sql
UPDATE arcgis_fields
SET embedding_vector = NULL;
```

## Notes

- Spring Boot is used for configuration, dependency injection, JDBC, transactions, and Hikari connection pooling.
- The app does not run crawler jobs on normal Spring startup. Each workflow is an explicit Gradle task.
- `text-embedding-3-small` is the default embedding model because current vector columns are `VECTOR(1536)`.
