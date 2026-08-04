package com.distributed.job_finder.dtos.smartrecruiters;

public record SmartRecruitersPosting(
    String id,
    String name,
    SmartRecruitersCompany company,
    SmartRecruitersLocation location,
    SmartRecruitersDictionary department,
    SmartRecruitersDictionary experienceLevel,
    SmartRecruitersDictionary typeOfEmployment
) {
    // Nested records to match the SmartRecruiters JSON structure cleanly
    public record SmartRecruitersCompany(String identifier, String name) {}
    public record SmartRecruitersLocation(String city, String region, String country, Boolean remote) {}
    public record SmartRecruitersDictionary(String id, String label) {}
}