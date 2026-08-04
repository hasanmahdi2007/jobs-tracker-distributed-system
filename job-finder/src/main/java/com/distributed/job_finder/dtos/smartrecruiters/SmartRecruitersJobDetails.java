package com.distributed.job_finder.dtos.smartrecruiters;

public record SmartRecruitersJobDetails(JobAd jobAd) {
    public record JobAd(Sections sections) {}
    public record Sections(Section jobDescription, Section qualifications, Section companyDescription) {}
    public record Section(String title, String text) {}
}