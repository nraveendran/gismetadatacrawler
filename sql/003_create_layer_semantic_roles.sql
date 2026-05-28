CREATE TABLE IF NOT EXISTS layer_semantic_roles (
    id BIGSERIAL PRIMARY KEY,
    role_name TEXT NOT NULL UNIQUE,
    description TEXT NOT NULL,
    category TEXT,
    embedding_model TEXT,
    embedding_text TEXT,
    embedding_vector VECTOR(1536),
    aliases JSONB DEFAULT '[]'::jsonb,
    example_layer_names JSONB DEFAULT '[]'::jsonb,
    keywords JSONB DEFAULT '[]'::jsonb,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_layer_semantic_roles_role_name
    ON layer_semantic_roles(role_name);

CREATE INDEX IF NOT EXISTS idx_layer_semantic_roles_category
    ON layer_semantic_roles(category);

CREATE INDEX IF NOT EXISTS idx_layer_semantic_roles_aliases
    ON layer_semantic_roles
    USING GIN (aliases);

CREATE INDEX IF NOT EXISTS idx_layer_semantic_roles_example_layer_names
    ON layer_semantic_roles
    USING GIN (example_layer_names);

CREATE INDEX IF NOT EXISTS idx_layer_semantic_roles_keywords
    ON layer_semantic_roles
    USING GIN (keywords);

-- CREATE INDEX idx_layer_semantic_roles_embedding
--     ON layer_semantic_roles
--     USING ivfflat (embedding_vector vector_cosine_ops);

INSERT INTO layer_semantic_roles
(role_name, description, category, example_layer_names)
VALUES
('address_points_dataset', 'Point dataset containing geocodable address locations or 911 address points.', 'address', '["ADDRESS POINTS","911 Addresses","CHANDLER ADDRESSES"]'::jsonb),
('address_verification_dataset', 'Dataset used for validating or reviewing addresses and field verification work.', 'address', '["Address - Field Verifications","Address Verification Points by Michelle"]'::jsonb),
('parcel_dataset', 'Polygon dataset containing parcel/property boundaries and identifiers.', 'property', '["Anderson County Parcels","CAD Parcels","Camp Parcels"]'::jsonb),
('zoning_dataset', 'Polygon dataset containing zoning classifications or zoning districts.', 'zoning', '["ZONING DISTRICTS","Brownsboro Zoning Districts"]'::jsonb),
('land_use_dataset', 'Dataset containing current or proposed land use classifications.', 'zoning', '["CURRENT LANDUSE"]'::jsonb),
('building_footprint_dataset', 'Polygon dataset representing building outlines or structures.', 'buildings', '["BUILDING FOOTPRINT","Building Footprints"]'::jsonb),
('building_permit_dataset', 'Dataset containing building permits, permit status, or permit history.', 'permits', '["BUILDING PERMIT","Building Permits"]'::jsonb),
('city_boundary_dataset', 'Polygon dataset defining municipal/city boundaries.', 'boundaries', '["CITY BOUNDARY","City Boundaries","Brownsboro City Boundary"]'::jsonb),
('county_boundary_dataset', 'Polygon dataset defining county boundaries.', 'boundaries', '["COUNTY BOUNDARY"]'::jsonb),
('etj_boundary_dataset', 'Dataset representing extraterritorial jurisdiction boundaries.', 'boundaries', '["Brownsboro ETJ","ETJ"]'::jsonb),
('subdivision_dataset', 'Dataset containing subdivision boundaries or plats.', 'property', '["Brownsboro City Subdivisions"]'::jsonb),
('overlay_district_dataset', 'Dataset containing overlay districts or special district polygons.', 'zoning', '["Brownsboro District Overlays"]'::jsonb),
('special_use_permit_dataset', 'Dataset containing special use permits or related zoning exceptions.', 'zoning', '["Brownsboro Special Use Permit"]'::jsonb),
('road_dataset', 'Dataset containing roads, streets, or transportation centerlines.', 'transportation', '["CITY STREET","City Streets","Camp County Roads"]'::jsonb),
('bridge_dataset', 'Dataset containing bridges or bridge infrastructure.', 'transportation', '["Bridges"]'::jsonb),
('trail_dataset', 'Dataset containing trails or recreational paths.', 'recreation', '["All Trails"]'::jsonb),
('bus_stop_dataset', 'Dataset containing bus stop locations.', 'transportation', '["Bus Stop","Bus Stops","Canton Bus Stops"]'::jsonb),
('bus_route_dataset', 'Dataset containing transit routes or transit points of interest.', 'transportation', '["Bus Route Points of Interest","Canton Routes"]'::jsonb),
('park_dataset', 'Dataset containing parks, recreational facilities, or campsites.', 'recreation', '["City Park","Camp Sites"]'::jsonb),
('cemetery_dataset', 'Dataset containing cemeteries, lots, roads, or sections.', 'places', '["Cemeteries","Cemetery Lots","Cemetery Sections"]'::jsonb),
('utility_boundary_dataset', 'Dataset defining utility service boundaries or CCN boundaries.', 'utilities', '["CCN Boundary"]'::jsonb),
('utility_boundary_change_dataset', 'Dataset containing utility boundary change lines.', 'utilities', '["CCN Boundary Change Line"]'::jsonb),
('hydrant_dataset', 'Dataset containing fire hydrants.', 'utilities', '["HYDRANTS"]'::jsonb),
('valve_dataset', 'Dataset containing valves or valve infrastructure.', 'utilities', '["Air Relief Valve","Blow Off Valve","VALVES"]'::jsonb),
('pump_station_dataset', 'Dataset containing lift stations, booster pumps, or pump infrastructure.', 'utilities', '["Sewer Lift Station","Booster Pump"]'::jsonb),
('sewer_main_dataset', 'Dataset containing sewer mains or sewer utility infrastructure.', 'utilities', '["SEWER MAIN"]'::jsonb),
('water_main_dataset', 'Dataset containing water mains or water distribution infrastructure.', 'utilities', '["WATER MAIN"]'::jsonb),
('cleanout_dataset', 'Dataset containing sewer cleanouts.', 'utilities', '["CLEANOUTS"]'::jsonb),
('meter_dataset', 'Dataset containing utility or electrical meters.', 'utilities', '["City Electrical Meters","METERS"]'::jsonb),
('streetlight_dataset', 'Dataset containing streetlights or lamp infrastructure.', 'utilities', '["CITY STREETLAMPS","City Lamp Posts"]'::jsonb),
('power_pole_dataset', 'Dataset containing utility or power poles.', 'utilities', '["City Power Poles"]'::jsonb),
('street_sign_dataset', 'Dataset containing traffic or street signage.', 'transportation', '["City Street Signs"]'::jsonb),
('floodplain_dataset', 'Dataset containing FEMA flood zones or floodplain boundaries.', 'environment', '["FEMA FLOODZONE"]'::jsonb),
('contour_dataset', 'Elevation contour dataset.', 'geospatial', '["2'' Contours","4'' Contours"]'::jsonb),
('emergency_service_dataset', 'Dataset related to emergency response districts or facilities.', 'public_safety', '["Ambulance Districts","Camp County EMS"]'::jsonb),
('fire_service_dataset', 'Dataset related to fire response districts or facilities.', 'public_safety', '["Camp County Fire"]'::jsonb),
('law_enforcement_dataset', 'Dataset related to law enforcement districts or facilities.', 'public_safety', '["Camp County Law"]'::jsonb),
('historical_site_dataset', 'Dataset containing historic registry or preservation locations.', 'places', '["Brownsboro National Registry of Historical Places"]'::jsonb),
('municipal_asset_dataset', 'Dataset containing miscellaneous city-owned assets or furniture.', 'municipal', '["City Furniture","City Hand Rails","City Trash Recepticals"]'::jsonb),
('recreational_access_dataset', 'Dataset containing recreational access points or canoe launches.', 'recreation', '["Canoe Launch"]'::jsonb),
('chlorination_infrastructure_dataset', 'Dataset containing chlorination infrastructure or water treatment components.', 'utilities', '["Chlorinator Assembly","Chlorinator Tank","Chlorine Injection Station"]'::jsonb),
('church_dataset', 'Dataset containing church or worship locations.', 'places', '["Churches"]'::jsonb),
('city_dataset', 'Dataset containing city-level polygons or municipality records.', 'administrative', '["CITY","Cities"]'::jsonb),
('administrative_district_dataset', 'Dataset containing council districts or administrative regions.', 'administrative', '["City Council Districts"]'::jsonb),
('property_inventory_dataset', 'Dataset containing available or managed properties.', 'property', '["Available Properties"]'::jsonb),
('billboard_dataset', 'Dataset containing billboard locations.', 'transportation', '["BILLBOARDS"]'::jsonb),
('review_notes_dataset', 'Dataset containing GIS review notes or boundary review comments.', 'administrative', '["Boundary Review Notes"]'::jsonb),
('unknown_dataset', 'Placeholder semantic role for unclear or experimental layers.', 'unknown', '["Bugs Bunny"]'::jsonb)
ON CONFLICT (role_name) DO NOTHING;


ALTER TABLE arcgis_layers
ADD COLUMN IF NOT EXISTS layer_semantic_role_id BIGINT REFERENCES layer_semantic_roles(id),
ADD COLUMN IF NOT EXISTS semantic_mapping_method TEXT,
ADD COLUMN IF NOT EXISTS embedding_model TEXT,
ADD COLUMN IF NOT EXISTS embedding_text TEXT,
ADD COLUMN IF NOT EXISTS embedding_vector VECTOR(1536),
ADD COLUMN IF NOT EXISTS embedding_created_at TIMESTAMP;


CREATE TABLE IF NOT EXISTS layer_semantic_candidates (
    id BIGSERIAL PRIMARY KEY,
    layer_id BIGINT NOT NULL REFERENCES arcgis_layers(id),
    layer_semantic_role_id BIGINT NOT NULL REFERENCES layer_semantic_roles(id),
    similarity_score NUMERIC NOT NULL,
    rank INTEGER NOT NULL,
    method TEXT DEFAULT 'embedding',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (layer_id, layer_semantic_role_id)
);


INSERT INTO layer_semantic_roles
(role_name, description, category, example_layer_names)
VALUES
('work_order_dataset', 'Dataset containing work orders, maintenance tasks, repairs, or operational service requests.', 'operations', '["Rusk Workorders","Harrison Workorders","ETCOG Work Orders","PANOLA COUNTY WORKORDER"]'::jsonb),
('cell_tower_dataset', 'Dataset containing cell towers or telecommunications infrastructure.', 'telecommunications', '["GREGG Cell Towers","Harrison Cell Towers","Marion Cell Towers","NW ETCOG Cell Towers"]'::jsonb),
('railroad_crossing_dataset', 'Dataset containing railroad crossings, crossing protection, mileposts, or railway infrastructure.', 'transportation', '["Crossing Protection","One-Mile Post","One-Tenth Mile Post","Non-electrified IHB Main Track"]'::jsonb),
('business_dataset', 'Dataset containing business locations or commercial entities.', 'business', '["Terry Businesses","Hale Businesses","Hockley Businesses","Henderson County Business Pts"]'::jsonb),
('address_dataset', 'Dataset containing address points or geocodable addresses.', 'address', '["Harrison Addresses","Rusk Addresses","Carthage Addresses","Anderson Addresses"]'::jsonb),
('dropoff_location_dataset', 'Dataset containing waste, recycling, or collection drop-off locations.', 'waste_management', '["January 2023 Drop Off Locations"]'::jsonb),
('pickup_location_dataset', 'Dataset containing waste, recycling, or collection pickup locations.', 'waste_management', '["January 2023 Pickup Locations"]'::jsonb),
('response_location_dataset', 'Dataset containing emergency response or structure response locations.', 'public_safety', '["Structure/Jaws Response","Structure Jaws Response","Mineola Rescue Locations"]'::jsonb),
('consumer_site_dataset', 'Dataset containing customer, consumer, or service site locations.', 'utilities', '["Consumer Sites"]'::jsonb),
('treatment_area_dataset', 'Dataset containing wastewater or sewer treatment service areas.', 'utilities', '["WW Treatment Area","SS Treatment Area"]'::jsonb),
('water_treatment_facility_dataset', 'Dataset containing water or wastewater treatment plants and facilities.', 'utilities', '["WW TreatmentPlant"]'::jsonb),
('source_water_dataset', 'Dataset containing water source entry points or source locations.', 'utilities', '["Entry Point","Entry Points","Entry Point Source Location"]'::jsonb),
('signal_dataset', 'Dataset containing transportation or utility signal infrastructure.', 'transportation', '["Signal"]'::jsonb),
('regional_connectivity_dataset', 'Dataset containing regional connectivity or regional corridor layers.', 'transportation', '["ETCOG RCL","Rusk County RCL"]'::jsonb),
('hazmat_dataset', 'Dataset containing hazardous material response or hazmat facilities.', 'public_safety', '["Rusk County Haz Mat"]'::jsonb),
('urban_area_dataset', 'Dataset containing urbanized area boundaries or urban study regions.', 'planning', '["Tyler UZA 2023","LongviewTylerNewUrban","TylerUZA2012"]'::jsonb),
('service_area_dataset', 'Dataset containing operational or paid/on-call service coverage areas.', 'operations', '["Paid and On-Call"]'::jsonb),
('nutrition_center_dataset', 'Dataset containing nutrition centers or social service facilities.', 'community_services', '["Nutrition Centers"]'::jsonb),
('centerline_dataset', 'Dataset containing road or transportation centerlines.', 'transportation', '["Rusk Centerlines","Edgewood Centerlines","Van Zandt County Centerlines"]'::jsonb),
('stormwater_infrastructure_dataset', 'Dataset containing stormwater infrastructure, fittings, discharge points, or junction boxes.', 'utilities', '["Stormwater Junction Box","Stormwater Fitting","Stormwater Discharge Pt"]'::jsonb),
('voting_district_dataset', 'Dataset containing voting districts, precincts, or electoral boundaries.', 'administrative', '["Gilmer Voting Districts"]'::jsonb),
('polling_place_dataset', 'Dataset containing polling places or election locations.', 'administrative', '["Polling Places"]'::jsonb),
('hotel_dataset', 'Dataset containing hotel or lodging locations.', 'business', '["Hotels"]'::jsonb),
('main_street_district_dataset', 'Dataset containing downtown or main street improvement districts.', 'planning', '["THC Main Street District"]'::jsonb),
('annexation_dataset', 'Dataset containing proposed annexations or annexation boundaries.', 'planning', '["Proposed Annexations"]'::jsonb),
('well_dataset', 'Dataset containing wells, well houses, standpipes, or groundwater infrastructure.', 'utilities', '["Wells","Well House","Standpipe"]'::jsonb),
('storage_facility_dataset', 'Dataset containing storage tanks, clear wells, or ground storage facilities.', 'utilities', '["Clear Well","Ground Storage"]'::jsonb),
('garbage_collection_dataset', 'Dataset containing garbage collection schedules or service zones.', 'waste_management', '["Garbage Collection Days"]'::jsonb),
('medical_facility_dataset', 'Dataset containing hospitals, clinics, or medical facilities.', 'healthcare', '["Medical Facility"]'::jsonb),
('fence_dataset', 'Dataset containing fences or enclosure infrastructure.', 'infrastructure', '["Fences"]'::jsonb),
('livestock_facility_dataset', 'Dataset containing horse stalls or livestock-related facilities.', 'agriculture', '["Horse Stall"]'::jsonb),
('railroad_track_dataset', 'Dataset containing railroad tracks or rail infrastructure.', 'transportation', '["Electrified Tracks","Non-electrified Track Not IHB"]'::jsonb),
('tunnel_dataset', 'Dataset containing tunnels or underground transportation infrastructure.', 'transportation', '["Tunnel"]'::jsonb),
('residential_dataset', 'Dataset containing residential properties or residential classifications.', 'property', '["Hockley Residential"]'::jsonb)
ON CONFLICT (role_name) DO NOTHING;