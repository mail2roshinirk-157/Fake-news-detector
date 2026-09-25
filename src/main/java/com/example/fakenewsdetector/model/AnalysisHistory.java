package com.example.fakenewsdetector.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "analysis_history")
public class AnalysisHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "input_type", nullable = false)
    private String inputType; // "TEXT" or "URL"

    @Column(name = "original_input", nullable = false, columnDefinition = "TEXT")
    private String originalInput;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "analyzed_text", columnDefinition = "TEXT")
    private String analyzedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prediction prediction;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "explanation", length = 1000)
    private String explanation; // Comma-separated list of explanations

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "character_count")
    private Integer characterCount;

    public AnalysisHistory() {
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public void setOriginalInput(String originalInput) {
        this.originalInput = originalInput;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAnalyzedText() {
        return analyzedText;
    }

    public void setAnalyzedText(String analyzedText) {
        this.analyzedText = analyzedText;
    }

    public Prediction getPrediction() {
        return prediction;
    }

    public void setPrediction(Prediction prediction) {
        this.prediction = prediction;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    // Helper methods to work with lists of explanation strings
    public List<String> getExplanationList() {
        if (explanation == null || explanation.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(explanation.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public void setExplanationList(List<String> explanations) {
        if (explanations == null || explanations.isEmpty()) {
            this.explanation = "";
        } else {
            this.explanation = String.join(",", explanations);
        }
    }
}
