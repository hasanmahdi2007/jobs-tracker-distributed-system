package com.distributed.job_finder.utils;

public class JobDataParser {

    public static String extractExperienceLevel(String title) {
        if (title == null) return "Mid";
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.matches(".*\\b(jr|junior|entry|intern|graduate)\\b.*")) return "Junior";
        if (lowerTitle.matches(".*\\b(sr|senior|lead|principal|staff|head|director|vp)\\b.*")) {
            if (lowerTitle.contains("lead")) return "Lead";
            if (lowerTitle.contains("principal") || lowerTitle.contains("staff")) return "Principal/Staff";
            if (lowerTitle.matches(".*\\b(director|vp|head)\\b.*")) return "Executive";
            return "Senior";
        }
        return "Mid";
    }

    public static String extractEmploymentType(String title, String description) {
        String combined = (title + " " + description).toLowerCase();
        if (combined.matches(".*\\b(contract|contractor|freelance)\\b.*")) return "Contract";
        if (combined.matches(".*\\b(intern|internship|co-op)\\b.*")) return "Internship";
        if (combined.matches(".*\\b(part-time|part time)\\b.*")) return "Part-time";
        return "Full-time";
    }
    
    public static String extractDepartment(String title) {
        if (title == null) return "General";
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("engineer") || lowerTitle.contains("developer") || lowerTitle.contains("data")) return "Engineering";
        if (lowerTitle.contains("product") || lowerTitle.contains("design") || lowerTitle.contains("ui/ux")) return "Product & Design";
        if (lowerTitle.contains("market") || lowerTitle.contains("sales") || lowerTitle.contains("growth")) return "Sales & Marketing";
        if (lowerTitle.contains("hr") || lowerTitle.contains("talent") || lowerTitle.contains("recruit")) return "Human Resources";
        return "General";
    }
}