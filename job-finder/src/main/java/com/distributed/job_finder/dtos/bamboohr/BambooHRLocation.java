package com.distributed.job_finder.dtos.bamboohr;

public record BambooHRLocation(
    String city,
    String state,
    String country
) {
    // Helper method to flatten BambooHR's nested location object
    public String getFullLocation() {
        if (city == null && state == null) return "Remote / Unspecified";
        if (city != null && state != null) return city + ", " + state;
        return city != null ? city : state;
    }
}