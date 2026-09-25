package com.example.fakenewsdetector.service;

import com.example.fakenewsdetector.dto.AnalysisResponse;
import com.example.fakenewsdetector.model.Prediction;
import com.example.fakenewsdetector.model.RiskLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClassificationEngine {

    @Autowired
    private NaiveBayesClassifier nbClassifier;

    // Configurable thresholds
    private double fakeThreshold = 39.0;
    private double uncertainThreshold = 59.0;
    private double genuineThreshold = 79.0;

    /**
     * Combines machine learning probability with rule-based features to compute the final analysis response.
     */
    public AnalysisResponse analyze(String title, String text, FeatureExtractor.ExtractedFeatures features) {
        String combined = (title != null ? title : "") + " " + (text != null ? text : "");
        
        // 1. Get Naive Bayes probability of FAKE
        double nbFakeProb = nbClassifier.predictFakeProbability(combined);
        double nbRealProb = 1.0 - nbFakeProb;

        // 2. Start base score from Naive Bayes Real probability (0 to 100)
        double score = nbRealProb * 100.0;

        List<String> indicators = new ArrayList<>();

        // 3. Apply rule-based adjustments and document explainable indicators
        
        // Sensationalism adjustment
        if (features.getSensationalWordsCount() > 0) {
            double penalty = Math.min(features.getSensationalWordsCount() * 6.0, 24.0);
            score -= penalty;
            indicators.add("Sensational language detected (" + features.getSensationalWordsCount() + " terms)");
        }

        // Clickbait adjustment
        if (features.getClickbaitPhrasesCount() > 0) {
            double penalty = Math.min(features.getClickbaitPhrasesCount() * 15.0, 30.0);
            score -= penalty;
            indicators.add("Clickbait-style phrasing detected");
        }

        // Emotional/Dramatic language adjustment
        if (features.getEmotionalWordsCount() > 0) {
            double penalty = Math.min(features.getEmotionalWordsCount() * 5.0, 15.0);
            score -= penalty;
            indicators.add("Emotional/dramatic wording found");
        }

        // Excessive capitalization
        if (features.getCapitalizedRatio() > 0.15) {
            score -= 15.0;
            indicators.add("Excessive capitalization (ALL_CAPS words)");
        }

        // Excessive exclamation marks
        if (features.getExclamationCount() > 2 || features.getExclamationRatio() > 0.3) {
            score -= 12.0;
            indicators.add("Excessive exclamation punctuation (" + features.getExclamationCount() + "!)");
        }

        // Positive signal: Source attribution
        if (features.getAttributionSignalsCount() > 0) {
            double bonus = Math.min(features.getAttributionSignalsCount() * 10.0, 25.0);
            score += bonus;
            indicators.add("Contains verifiable source attribution signals (" + features.getAttributionSignalsCount() + ")");
        } else {
            // Negative signal: Lack of attribution
            score -= 10.0;
            indicators.add("Lack of clear source attribution");
        }

        // Positive signal: Factual numbers/dates
        if (features.getNumberCount() > 0) {
            double bonus = Math.min(features.getNumberCount() * 3.0, 10.0);
            score += bonus;
            indicators.add("Includes factual numbers/dates");
        }

        // Positive signal: Extracted links
        if (features.getUrlCount() > 0) {
            score += 5.0;
            indicators.add("Contains external web citations/URLs");
        }

        // Clamping score between 0.0 and 100.0
        score = Math.max(0.0, Math.min(100.0, score));

        // 4. Map score to prediction & risk level
        Prediction prediction;
        RiskLevel riskLevel;

        if (score <= fakeThreshold) {
            prediction = Prediction.LIKELY_FAKE;
            riskLevel = RiskLevel.HIGH;
        } else if (score <= uncertainThreshold) {
            prediction = Prediction.UNCERTAIN;
            riskLevel = RiskLevel.MEDIUM;
        } else if (score <= genuineThreshold) {
            prediction = Prediction.LIKELY_GENUINE;
            riskLevel = RiskLevel.LOW;
        } else {
            prediction = Prediction.LIKELY_GENUINE; // Strongly likely genuine
            riskLevel = RiskLevel.LOW;
        }

        // If no indicators were added (should be rare), add a neutral indicator
        if (indicators.isEmpty()) {
            indicators.add("Text characteristics fall within normal baseline ratios");
        }

        // 5. Construct Response
        AnalysisResponse response = new AnalysisResponse();
        response.setPrediction(prediction.name());
        response.setConfidenceScore(Math.round(score * 10.0) / 10.0); // round to 1 decimal place
        response.setRiskLevel(riskLevel.name());
        response.setIndicators(indicators);
        
        response.setWordCount(features.getWordCount());
        response.setCharacterCount(features.getCharCount());
        response.setSentenceCount(features.getSentenceCount());
        response.setExclamationCount(features.getExclamationCount());
        response.setQuestionCount(features.getQuestionCount());
        response.setCapitalizedWordsCount(features.getCapitalizedWordsCount());
        response.setTitle(title);
        
        return response;
    }

    // Setters to allow custom dynamic thresholds
    public void setFakeThreshold(double fakeThreshold) {
        this.fakeThreshold = fakeThreshold;
    }

    public void setUncertainThreshold(double uncertainThreshold) {
        this.uncertainThreshold = uncertainThreshold;
    }

    public void setGenuineThreshold(double genuineThreshold) {
        this.genuineThreshold = genuineThreshold;
    }
}
