package com.distributed.job_finder.utils;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class LocationNormalizer {

    // Now designed for O(1) direct lookups
    private static final Map<String, String> CITY_TO_COUNTRY = new HashMap<>();

    static {
        // --- UNITED STATES ---
        String usa = "United States of America";
        CITY_TO_COUNTRY.put("united states", usa);
        CITY_TO_COUNTRY.put("us", usa);
        CITY_TO_COUNTRY.put("usa", usa);
        CITY_TO_COUNTRY.put("new york", usa);
        CITY_TO_COUNTRY.put("ny", usa);
        CITY_TO_COUNTRY.put("new jersey", usa);
        CITY_TO_COUNTRY.put("nj", usa);
        CITY_TO_COUNTRY.put("san francisco", usa);
        CITY_TO_COUNTRY.put("bay area", usa);
        // Add thousands more here...

        // --- UNITED KINGDOM ---
        String uk = "United Kingdom";
        CITY_TO_COUNTRY.put("uk", uk);
        CITY_TO_COUNTRY.put("london", uk);
        CITY_TO_COUNTRY.put("manchester", uk);
        
        // --- LEBANON ---
        String lebanon = "Lebanon";
        CITY_TO_COUNTRY.put("beirut", lebanon);
    }

    public String normalizeCountry(String rawLocation) {
        if (rawLocation == null || rawLocation.trim().isEmpty()) {
            return "Unknown";
        }

        String lowerLoc = rawLocation.toLowerCase().trim();

        // 1. Best Case: Fast O(1) exact match (e.g., they literally just typed "London")
        if (CITY_TO_COUNTRY.containsKey(lowerLoc)) {
            return CITY_TO_COUNTRY.get(lowerLoc);
        }

        // 2. Tokenized O(1) Lookup: Handle dirty strings like "Remote - San Francisco, CA"
        // We split by commas, dashes, slashes, or pipes.
        String[] tokens = lowerLoc.split("[,\\-/|]");
        
        for (String token : tokens) {
            String cleanToken = token.trim();
            // O(1) lookup on the extracted piece
            if (CITY_TO_COUNTRY.containsKey(cleanToken)) {
                return CITY_TO_COUNTRY.get(cleanToken);
            }
        }

        // 3. Fallback if no parts of the string exist in the map
        return rawLocation.trim();
    }
}