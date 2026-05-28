-- Active: 1778714631124@@127.0.0.1@5432@gismetadata@public
-- Run this after connecting to the gismetadata database:
-- psql -d gismetadata -f sql/001_create_crawl_sources.sql

CREATE TABLE IF NOT EXISTS crawl_sources (
    id BIGSERIAL PRIMARY KEY,
    source_url TEXT NOT NULL UNIQUE,
    crawled_at TIMESTAMP NOT NULL DEFAULT NOW(),
    raw_json JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS arcgis_services (
    id BIGSERIAL PRIMARY KEY,
    server_url TEXT,
    folder_name TEXT,
    service_name TEXT,
    service_type TEXT,
    metadata JSONB,
    discovered_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (service_name, service_type)
);

CREATE TABLE IF NOT EXISTS arcgis_folders (
    id BIGSERIAL PRIMARY KEY,
    server_url TEXT NOT NULL,
    folder_name TEXT NOT NULL,
    folder_url TEXT NOT NULL,
    metadata JSONB,
    discovered_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (server_url, folder_name)
);

CREATE TABLE IF NOT EXISTS arcgis_layers (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES arcgis_services(id),
    layer_id INTEGER NOT NULL,
    layer_name TEXT NOT NULL,
    layer_url TEXT NOT NULL,
    layer_type TEXT,
    geometry_type TEXT,
    object_id_field TEXT,
    display_field TEXT,
    type_id_field TEXT,
    capabilities TEXT,
    supported_query_formats TEXT,
    max_record_count INTEGER,
    spatial_reference_wkid INTEGER,
    dataset_type TEXT,
    semantic_confidence NUMERIC,
    metadata JSONB,
    discovered_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (service_id, layer_id)
);

CREATE TABLE IF NOT EXISTS arcgis_fields (
    id BIGSERIAL PRIMARY KEY,
    layer_table_id BIGINT NOT NULL REFERENCES arcgis_layers(id),
    field_name TEXT NOT NULL,
    field_alias TEXT,
    field_type TEXT,
    model_name TEXT,
    nullable BOOLEAN,
    editable BOOLEAN,
    semantic_role TEXT,
    semantic_confidence NUMERIC,
    semantic_mapping_method TEXT,
    embedding_model TEXT,
    embedding_text TEXT,
    embedding_vector VECTOR(1536),
    embedding_created_at TIMESTAMP,
    metadata JSONB,
    discovered_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (layer_table_id, field_name)
);


CREATE TABLE IF NOT EXISTS semantic_field_dictionary (
    id BIGSERIAL PRIMARY KEY,
    normalized_field_name TEXT NOT NULL UNIQUE,
    semantic_role TEXT NOT NULL,
    confidence NUMERIC,
    method TEXT,
    field_examples JSONB,
    reviewed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
