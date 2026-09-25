package com.example.fakenewsdetector.dto;

import java.util.List;

public class DashboardStats {
    private Long totalAnalyses;
    private Long fakeCount;
    private Long genuineCount;
    private Long uncertainCount;
    private Double averageConfidence;
    
    // Timeline metrics (e.g. past 7 days activity)
    private List<String> timelineLabels;
    private List<Long> timelineCounts;
    
    // Risk breakdown metrics
    private List<String> riskLabels;
    private List<Long> riskCounts;

    public Long getTotalAnalyses() {
        return totalAnalyses;
    }

    public void setTotalAnalyses(Long totalAnalyses) {
        this.totalAnalyses = totalAnalyses;
    }

    public Long getFakeCount() {
        return fakeCount;
    }

    public void setFakeCount(Long fakeCount) {
        this.fakeCount = fakeCount;
    }

    public Long getGenuineCount() {
        return genuineCount;
    }

    public void setGenuineCount(Long genuineCount) {
        this.genuineCount = genuineCount;
    }

    public Long getUncertainCount() {
        return uncertainCount;
    }

    public void setUncertainCount(Long uncertainCount) {
        this.uncertainCount = uncertainCount;
    }

    public Double getAverageConfidence() {
        return averageConfidence;
    }

    public void setAverageConfidence(Double averageConfidence) {
        this.averageConfidence = averageConfidence;
    }

    public List<String> getTimelineLabels() {
        return timelineLabels;
    }

    public void setTimelineLabels(List<String> timelineLabels) {
        this.timelineLabels = timelineLabels;
    }

    public List<Long> getTimelineCounts() {
        return timelineCounts;
    }

    public void setTimelineCounts(List<Long> timelineCounts) {
        this.timelineCounts = timelineCounts;
    }

    public List<String> getRiskLabels() {
        return riskLabels;
    }

    public void setRiskLabels(List<String> riskLabels) {
        this.riskLabels = riskLabels;
    }

    public List<Long> getRiskCounts() {
        return riskCounts;
    }

    public void setRiskCounts(List<Long> riskCounts) {
        this.riskCounts = riskCounts;
    }
}
