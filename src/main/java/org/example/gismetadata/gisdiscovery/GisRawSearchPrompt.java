package org.example.gismetadata.gisdiscovery;

public final class GisRawSearchPrompt {
    private static final String TEMPLATE = """
            You are an expert GIS research assistant.

            Use Google Search grounding to locate active GIS resources, REST endpoints,
            and map viewers for {{COUNTY_NAME}} County, {{STATE_NAME}}.

            Focus on identifying GIS resources that would be useful for:

            - property intelligence
            - real estate analysis
            - development feasibility
            - zoning analysis
            - permit research
            - land-use research
            - municipal infrastructure analysis

            Prefer references to:

            - datasets
            - GIS portals
            - ArcGIS organizations
            - map viewers
            - service names
            - department names
            - open-data catalogs

            These resources should be likely to lead to discoverable ArcGIS REST services.

            Deprioritize third-party sites unless they clearly host official GIS services
            for the county.

            Structure your response exactly like this:

            1. Provide a concise, 2-to-3 sentence executive summary of where the
               county's data is hosted and how it is accessed.
            2. Use inline citation numbers, such as [1] or [2], at the end of
               statements to anchor them to your search sources.
            3. Provide a bulleted list under this exact header:
               Key REST service and GIS resources include:
            4. Each bullet must follow this format:
               **Resource Name**: One-sentence description of what it maps or does. [Citation if applicable]

            Keep the tone professional, direct, and scannable.
            Do not include introductory filler or chatty transitions.
            """;

    private GisRawSearchPrompt() {
    }

    public static String forCounty(String countyName, String stateName) {
        return TEMPLATE
                .replace("{{COUNTY_NAME}}", countyName)
                .replace("{{STATE_NAME}}", stateName);
    }
}
