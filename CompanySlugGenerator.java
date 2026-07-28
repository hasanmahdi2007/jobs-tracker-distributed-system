import java.util.HashSet;
import java.util.Set;

public class CompanySlugGenerator {

    public static Set<String> generateSlugs(String rawName) {
        Set<String> slugs = new HashSet<>();

        if (rawName == null || rawName.trim().isEmpty()) {
            return slugs;
        }

        // 1. Lowercase everything
        String cleanName = rawName.toLowerCase();

        // 2. Strip out Arabic characters completely (using Unicode block)
        cleanName = cleanName.replaceAll("\\p{IsArabic}", "");

        // 3. Remove common corporate suffixes (LLC, LTD, SAL, WLL, FZCO, etc.)
        cleanName = cleanName.replaceAll("\\b(llc|ltd|co|inc|sal|wll|fzco|fz-llc|group|company)\\b", "");

        // 4. Remove anything that isn't a letter, number, or space
        cleanName = cleanName.replaceAll("[^a-z0-9\\s]", " ");

        // 5. Clean up multiple spaces into a single space, and trim edges
        cleanName = cleanName.replaceAll("\\s+", " ").trim();

        if (cleanName.isEmpty()) {
            return slugs;
        }

        // PERMUTATION A: Spaces replaced with hyphens (e.g., "starbucks-coffee")
        slugs.add(cleanName.replace(" ", "-"));

        // PERMUTATION B: Spaces completely removed (e.g., "starbuckscoffee")
        slugs.add(cleanName.replace(" ", ""));

        // PERMUTATION C: Only the first word (e.g., "starbucks")
        if (cleanName.contains(" ")) {
            slugs.add(cleanName.split(" ")[0]);
        }

        return slugs;
    }

    public static void main(String[] args) {
        // Test it right now to see the magic
        String testName1 = "Starbucks Coffee (LLC) - ستاربكس";
        String testName2 = "Talabat Middle East WLL";
        String testName3 = "Anghami SAL"; // Classic Lebanese corporate suffix

        System.out.println("Test 1: " + generateSlugs(testName1));
        System.out.println("Test 2: " + generateSlugs(testName2));
        System.out.println("Test 3: " + generateSlugs(testName3));
    }
}