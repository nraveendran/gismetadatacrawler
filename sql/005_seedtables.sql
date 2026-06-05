create table gis_discovery_target (
    id bigserial primary key,
    target_type text not null, -- city, county
    source_table text not null, -- us_places, us_counties
    source_ogc_fid bigint not null,
    statefp text,
    countyfp text,
    geoid text,
    name text not null,
    namelsad text,
    intptlat numeric,
    intptlon numeric,
    created_at timestamptz default now(),
    unique (target_type, source_table, source_ogc_fid)
);

alter table gis_discovery_target
add column if not exists discovery_status text,
add column if not exists discovery_error text,
add column if not exists last_discovery_attempt_at timestamptz;


create table gis_url_candidate (
    id bigserial primary key,

    source_table text not null,
    -- us_counties, us_places

    source_ogc_fid bigint not null,

    entity_name text not null,
    state text not null,
    entity_type text not null,
    -- city, county

    rank int not null,
    -- 1 = best candidate

    url text not null,
    normalized_url text not null,

    endpoint_type text not null,
    -- arcgis_rest_root, arcgis_hub_catalog,
    -- arcgis_service, open_data_portal,
    -- gis_page, map_viewer, unknown

    ownership_type text,
    -- official, regional_government,
    -- vendor_hosted_official,
    -- third_party, unknown

    validation_status text,
    -- validated, reachable_but_not_validated,
    -- inferred_not_validated, invalid

    http_status int,

    json_keys_found text[],

    sample_title_or_service_name text,

    found_from text,
    -- search_result, official_page,
    -- data_json, map_viewer,
    -- service_metadata, inferred_pattern

    source_url text,
    source_title text,

    confidence text,
    -- high, medium, low

    reasoning text,

    raw_response jsonb,

    created_at timestamptz default now(),

    unique (source_table, source_ogc_fid, normalized_url)
);


create table if not exists gis_search_raw_result (
    id bigserial primary key,

    source_table text not null,
    source_ogc_fid bigint not null,

    entity_name text not null,
    state text not null,
    entity_type text not null,

    provider text not null,
    model text not null,
    prompt text not null,
    raw_result text not null,

    created_at timestamptz default now(),
    updated_at timestamptz default now(),

    unique (source_table, source_ogc_fid, provider, model)
);
