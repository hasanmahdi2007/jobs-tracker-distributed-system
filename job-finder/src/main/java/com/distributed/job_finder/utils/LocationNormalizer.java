package com.distributed.job_finder.utils;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class LocationNormalizer {

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
        
        // --- FRANCE ---
        String france = "France";
        CITY_TO_COUNTRY.put("france", france);
        CITY_TO_COUNTRY.put("paris", france);
        CITY_TO_COUNTRY.put("nice", france);
        CITY_TO_COUNTRY.put("lyon", france);
        CITY_TO_COUNTRY.put("marseille", france);

        // --- UNITED KINGDOM ---
        String uk = "United Kingdom";
        CITY_TO_COUNTRY.put("uk", uk);
        CITY_TO_COUNTRY.put("london", uk);
        CITY_TO_COUNTRY.put("manchester", uk);
        
        // --- LEBANON ---
        String lebanon = "Lebanon";
        CITY_TO_COUNTRY.put("beirut", lebanon);
    }

    /**
     * Use this method right before saving the job to the database!
     * It transforms "Paris" -> "Paris, France" so your SQL LIKE queries work.
     */
    public String normalizeLocationForDatabase(String rawLocation) {
        if (rawLocation == null || rawLocation.trim().isEmpty()) {
            return "Unknown";
        }

        String lowerLoc = rawLocation.toLowerCase().trim();
        String detectedCountry = null;

        // 1. Best Case: Fast O(1) exact match (e.g., they typed "London")
        if (CITY_TO_COUNTRY.containsKey(lowerLoc)) {
            detectedCountry = CITY_TO_COUNTRY.get(lowerLoc);
        } else {
            // 2. Tokenized O(1) Lookup: Handle dirty strings like "Remote - San Francisco, CA"
            String[] tokens = lowerLoc.split("[,\\-/|]");
            for (String token : tokens) {
                String cleanToken = token.trim();
                if (CITY_TO_COUNTRY.containsKey(cleanToken)) {
                    detectedCountry = CITY_TO_COUNTRY.get(cleanToken);
                    break;
                }
            }
        }

        // 3. Append the country if it was found and isn't already in the string!
        if (detectedCountry != null && !lowerLoc.contains(detectedCountry.toLowerCase())) {
            // Transforms "Paris" into "Paris, France"
            return rawLocation.trim() + ", " + detectedCountry;
        }

        // Fallback: Return raw string if no country match was found or if it already contained the country
        return rawLocation.trim();
    }
}