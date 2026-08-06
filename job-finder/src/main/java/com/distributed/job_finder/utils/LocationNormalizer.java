package com.distributed.job_finder.utils;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class LocationNormalizer {

    private static final Map<String, String> CITY_TO_COUNTRY = new HashMap<>();

    // Helper method to keep the file clean: First arg is the Country, the rest are cities/aliases
    private static void add(String country, String... aliases) {
        CITY_TO_COUNTRY.put(country.toLowerCase(), country);
        for (String alias : aliases) {
            CITY_TO_COUNTRY.put(alias.toLowerCase(), country);
        }
    }

    static {
        // ==========================================
        // NORTH AMERICA
        // ==========================================
        add("United States of America", "united states", "us", "usa", "america", "new york", "ny", "nyc", "california", "ca", "san francisco", "bay area", "silicon valley", "los angeles", "la", "san diego", "texas", "tx", "austin", "dallas", "houston", "washington", "wa", "seattle", "massachusetts", "ma", "boston", "illinois", "il", "chicago", "colorado", "co", "denver", "georgia", "ga", "atlanta", "florida", "fl", "miami", "new jersey", "nj", "pennsylvania", "pa", "philadelphia", "ohio", "oh", "michigan", "mi", "detroit", "north carolina", "nc", "charlotte", "virginia", "va", "arizona", "az", "phoenix", "maryland", "md");
        add("Canada", "ontario", "toronto", "british columbia", "vancouver", "quebec", "montreal", "alberta", "calgary", "edmonton", "ottawa", "waterloo", "nova scotia", "halifax", "manitoba", "winnipeg");
        add("Mexico", "mexico city", "cdmx", "guadalajara", "monterrey", "tijuana", "puebla", "cancun", "queretaro");
        add("Costa Rica", "san jose", "san josé");
        add("Panama", "panama city");
        add("Guatemala", "guatemala city");
        add("Dominican Republic", "santo domingo");
        add("Jamaica", "kingston");
        add("El Salvador", "san salvador");
        add("Honduras", "tegucigalpa");
        add("Nicaragua", "managua");
        add("Puerto Rico", "san juan", "pr");

        // ==========================================
        // EUROPE
        // ==========================================
        add("United Kingdom", "uk", "great britain", "gb", "england", "london", "manchester", "birmingham", "leeds", "bristol", "liverpool", "scotland", "edinburgh", "glasgow", "wales", "cardiff", "northern ireland", "belfast", "cambridge", "oxford");
        add("France", "paris", "lyon", "marseille", "toulouse", "nice", "nantes", "strasbourg", "bordeaux", "lille");
        add("Germany", "berlin", "munich", "münchen", "frankfurt", "hamburg", "stuttgart", "cologne", "köln", "dusseldorf", "leipzig", "dresden");
        add("Netherlands", "the netherlands", "holland", "amsterdam", "rotterdam", "the hague", "utrecht", "eindhoven");
        add("Spain", "madrid", "barcelona", "valencia", "seville", "bilbao", "malaga");
        add("Italy", "rome", "roma", "milan", "milano", "naples", "napoli", "turin", "torino", "florence", "firenze", "venice", "genoa");
        add("Ireland", "dublin", "cork", "galway", "limerick");
        add("Switzerland", "zurich", "zürich", "geneva", "basel", "lausanne", "bern");
        add("Sweden", "stockholm", "gothenburg", "malmo", "malmö");
        add("Norway", "oslo", "bergen", "trondheim");
        add("Denmark", "copenhagen", "aarhus");
        add("Finland", "helsinki", "espoo", "tampere");
        add("Belgium", "brussels", "antwerp", "ghent");
        add("Austria", "vienna", "wien", "graz", "linz", "salzburg");
        add("Poland", "warsaw", "krakow", "kraków", "wroclaw", "poznan", "gdansk");
        add("Portugal", "lisbon", "porto", "braga");
        add("Czechia", "czech republic", "prague", "brno", "ostrava");
        add("Hungary", "budapest", "debrecen");
        add("Romania", "bucharest", "cluj-napoca", "timisoara");
        add("Greece", "athens", "thessaloniki");
        add("Ukraine", "kyiv", "kiev", "lviv", "kharkiv", "odesa");
        add("Russia", "moscow", "st petersburg", "saint petersburg", "novosibirsk");
        add("Bulgaria", "sofia", "plovdiv");
        add("Serbia", "belgrade", "novi sad");
        add("Croatia", "zagreb", "split");
        add("Estonia", "tallinn", "tartu");
        add("Latvia", "riga");
        add("Lithuania", "vilnius", "kaunas");
        add("Slovakia", "bratislava", "kosice");
        add("Slovenia", "ljubljana");
        add("Luxembourg", "luxembourg city");
        add("Cyprus", "nicosia", "limassol");
        add("Malta", "valletta", "sliema");

        // ==========================================
        // MIDDLE EAST
        // ==========================================
        add("United Arab Emirates", "uae", "dubai", "abu dhabi", "sharjah");
        add("Saudi Arabia", "ksa", "riyadh", "jeddah", "mecca", "medina", "dammam", "khobar");
        add("Lebanon", "beirut", "tripoli", "sidon", "tyre");
        add("Qatar", "doha");
        add("Kuwait", "kuwait city");
        add("Bahrain", "manama");
        add("Oman", "muscat");
        add("Jordan", "amman", "aqaba");
        add("Israel", "tel aviv", "jerusalem", "haifa");
        add("Palestine State", "palestine", "ramallah", "gaza");
        add("Turkey", "turkiye", "istanbul", "ankara", "izmir", "antalya");
        add("Egypt", "cairo", "alexandria", "giza");
        add("Iraq", "baghdad", "erbil", "basra");
        add("Iran", "tehran", "mashhad", "isfahan");
        add("Syria", "damascus", "aleppo");
        add("Yemen", "sanaa", "aden");

        // ==========================================
        // ASIA & OCEANIA
        // ==========================================
        add("India", "bangalore", "bengaluru", "mumbai", "new delhi", "delhi", "hyderabad", "pune", "chennai", "gurugram", "noida", "kolkata", "ahmedabad");
        add("Singapore", "singapore city"); // City-state
        add("Australia", "sydney", "melbourne", "brisbane", "perth", "adelaide", "nsw", "victoria", "queensland", "canberra");
        add("New Zealand", "nz", "auckland", "wellington", "christchurch");
        add("Japan", "tokyo", "osaka", "kyoto", "yokohama", "fukuoka", "nagoya");
        add("China", "beijing", "shanghai", "shenzhen", "guangzhou", "hangzhou", "chengdu", "wuhan");
        add("Hong Kong", "hk");
        add("Taiwan", "taipei", "hsinchu");
        add("South Korea", "korea", "seoul", "busan", "incheon", "pangyo");
        add("Philippines", "manila", "makati", "cebu", "quezon city", "bgc");
        add("Indonesia", "jakarta", "bali", "surabaya", "bandung");
        add("Malaysia", "kuala lumpur", "kl", "penang", "johor bahru");
        add("Vietnam", "ho chi minh city", "hcmc", "saigon", "hanoi", "da nang");
        add("Thailand", "bangkok", "chiang mai", "phuket");
        add("Pakistan", "karachi", "lahore", "islamabad");
        add("Bangladesh", "dhaka", "chittagong");
        add("Sri Lanka", "colombo", "kandy");
        add("Nepal", "kathmandu");
        add("Myanmar (formerly Burma)", "myanmar", "burma", "yangon", "mandalay");
        add("Cambodia", "phnom penh");

        // ==========================================
        // AFRICA
        // ==========================================
        add("South Africa", "rsa", "cape town", "johannesburg", "joburg", "pretoria", "durban");
        add("Nigeria", "lagos", "abuja", "port harcourt");
        add("Kenya", "nairobi", "mombasa");
        add("Ghana", "accra", "kumasi");
        add("Morocco", "casablanca", "rabat", "marrakesh");
        add("Algeria", "algiers", "oran");
        add("Tunisia", "tunis");
        add("Ethiopia", "addis ababa");
        add("Uganda", "kampala");
        add("Tanzania", "dar es salaam", "dodoma");
        add("Rwanda", "kigali");
        add("Senegal", "dakar");
        add("Côte d'Ivoire", "ivory coast", "abidjan");
        add("Cameroon", "douala", "yaounde");
        add("Zambia", "lusaka");
        add("Zimbabwe", "harare");
        add("Angola", "luanda");
        add("Mozambique", "maputo");
        add("Mauritius", "port louis");

        // ==========================================
        // LATIN AMERICA
        // ==========================================
        add("Brazil", "brasil", "são paulo", "sao paulo", "rio de janeiro", "curitiba", "belo horizonte", "florianopolis");
        add("Colombia", "bogota", "bogotá", "medellin", "medellín", "cali", "barranquilla");
        add("Argentina", "buenos aires", "caba", "cordoba", "rosario");
        add("Chile", "santiago", "valparaiso");
        add("Peru", "lima", "arequipa");
        add("Ecuador", "quito", "guayaquil");
        add("Venezuela", "caracas");
        add("Uruguay", "montevideo");
        add("Paraguay", "asuncion");
        add("Bolivia", "la paz", "santa cruz");
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