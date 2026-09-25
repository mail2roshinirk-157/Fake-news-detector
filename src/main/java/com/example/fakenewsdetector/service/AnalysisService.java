package com.example.fakenewsdetector.service;

import com.example.fakenewsdetector.dto.AnalysisRequest;
import com.example.fakenewsdetector.dto.AnalysisResponse;
import com.example.fakenewsdetector.dto.DashboardStats;
import com.example.fakenewsdetector.model.AnalysisHistory;
import com.example.fakenewsdetector.model.Prediction;
import com.example.fakenewsdetector.model.RiskLevel;
import com.example.fakenewsdetector.repository.AnalysisHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalysisService {

    @Autowired
    private AnalysisHistoryRepository repository;

    @Autowired
    private UrlExtractionService urlService;

    @Autowired
    private FeatureExtractor featureExtractor;

    @Autowired
    private ClassificationEngine classificationEngine;

    /**
     * Conducts news analysis and persists the result to the history log.
     */
    public AnalysisResponse analyzeNews(AnalysisRequest request) {
        String inputType = "TEXT";
        String originalInput = "";
        String title = "";
        String textToAnalyze = "";

        if (request.getUrl() != null && !request.getUrl().trim().isEmpty()) {
            inputType = "URL";
            originalInput = request.getUrl().trim();
            
            // Scrap web article
            UrlExtractionService.ExtractedWebPage page = urlService.extract(originalInput);
            title = page.getTitle();
            textToAnalyze = page.getBodyText();
        } else {
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("News content text must not be empty.");
            }
            originalInput = request.getText().trim();
            textToAnalyze = originalInput;
            
            // Derive title/headline from the first line or first 80 characters
            String firstLine = originalInput.split("\\n")[0].trim();
            if (firstLine.length() > 80) {
                title = firstLine.substring(0, 77) + "...";
            } else {
                title = firstLine;
            }
            if (title.isEmpty()) {
                title = "News Headline Analysis";
            }
        }

        // 1. Extract linguistic style features
        FeatureExtractor.ExtractedFeatures features = featureExtractor.extract(textToAnalyze);

        // 2. Score news and compile explanation
        AnalysisResponse response = classificationEngine.analyze(title, textToAnalyze, features);
        response.setInputType(inputType);
        response.setOriginalText(textToAnalyze);

        // 3. Save details to database
        AnalysisHistory history = new AnalysisHistory();
        history.setInputType(inputType);
        history.setOriginalInput(originalInput);
        history.setTitle(title);
        history.setAnalyzedText(textToAnalyze);
        history.setPrediction(Prediction.valueOf(response.getPrediction()));
        history.setConfidenceScore(response.getConfidenceScore());
        history.setRiskLevel(RiskLevel.valueOf(response.getRiskLevel()));
        history.setExplanationList(response.getIndicators());
        history.setWordCount(response.getWordCount());
        history.setCharacterCount(response.getCharacterCount());

        AnalysisHistory saved = repository.save(history);
        response.setId(saved.getId());
        response.setFormattedTimestamp(saved.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return response;
    }

    /**
     * Retrieve analysis details by history ID.
     */
    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysisDetails(Long id) {
        AnalysisHistory entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Analysis history not found for ID: " + id));
        return mapToResponse(entity);
    }

    /**
     * Retrieve paginated and filtered analysis logs.
     */
    @Transactional(readOnly = true)
    public Page<AnalysisResponse> getHistory(String query, String prediction, String riskLevel, Pageable pageable) {
        Prediction predEnum = (prediction != null && !prediction.trim().isEmpty()) ? Prediction.valueOf(prediction) : null;
        RiskLevel riskEnum = (riskLevel != null && !riskLevel.trim().isEmpty()) ? RiskLevel.valueOf(riskLevel) : null;
        String searchTerm = (query != null && !query.trim().isEmpty()) ? query : null;

        Page<AnalysisHistory> page = repository.searchHistory(searchTerm, predEnum, riskEnum, pageable);
        return page.map(this::mapToResponse);
    }

    /**
     * Retrieve top 10 recent analyses.
     */
    @Transactional(readOnly = true)
    public List<AnalysisResponse> getRecentHistory() {
        return repository.findTop10ByOrderByTimestampDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a single history record.
     */
    public void deleteHistory(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Analysis history not found for ID: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Delete all history records.
     */
    public void deleteAllHistory() {
        repository.deleteAll();
    }

    /**
     * Compiles statistical aggregates for the dashboard panels and graphics.
     */
    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();
        
        long total = repository.count();
        stats.setTotalAnalyses(total);
        
        if (total == 0) {
            stats.setFakeCount(0L);
            stats.setGenuineCount(0L);
            stats.setUncertainCount(0L);
            stats.setAverageConfidence(0.0);
            stats.setTimelineLabels(new ArrayList<>());
            stats.setTimelineCounts(new ArrayList<>());
            stats.setRiskLabels(Arrays.asList("HIGH", "MEDIUM", "LOW"));
            stats.setRiskCounts(Arrays.asList(0L, 0L, 0L));
            return stats;
        }

        stats.setFakeCount(repository.countByPrediction(Prediction.LIKELY_FAKE));
        stats.setGenuineCount(repository.countByPrediction(Prediction.LIKELY_GENUINE));
        stats.setUncertainCount(repository.countByPrediction(Prediction.UNCERTAIN));
        
        Double avgConf = repository.getAverageConfidenceScore();
        stats.setAverageConfidence(avgConf != null ? Math.round(avgConf * 10.0) / 10.0 : 0.0);

        // Group by risk levels
        long highRisk = repository.countByRiskLevel(RiskLevel.HIGH);
        long medRisk = repository.countByRiskLevel(RiskLevel.MEDIUM);
        long lowRisk = repository.countByRiskLevel(RiskLevel.LOW);
        stats.setRiskLabels(Arrays.asList("HIGH", "MEDIUM", "LOW"));
        stats.setRiskCounts(Arrays.asList(highRisk, medRisk, lowRisk));

        // Group past 7 days activity (Dialect-independent: compile in-memory from query list)
        List<AnalysisHistory> allHistory = repository.findAll();
        Map<LocalDate, Long> dailyCounts = allHistory.stream()
                .map(h -> h.getTimestamp().toLocalDate())
                .collect(Collectors.groupingBy(date -> date, Collectors.counting()));

        // Fill in past 7 days (even if 0 records exist) to make graphs look nice and populated
        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            labels.add(date.format(formatter));
            counts.add(dailyCounts.getOrDefault(date, 0L));
        }
        
        stats.setTimelineLabels(labels);
        stats.setTimelineCounts(counts);

        return stats;
    }

    private AnalysisResponse mapToResponse(AnalysisHistory entity) {
        AnalysisResponse response = new AnalysisResponse();
        response.setId(entity.getId());
        response.setInputType(entity.getInputType());
        response.setPrediction(entity.getPrediction().name());
        response.setConfidenceScore(entity.getConfidenceScore());
        response.setRiskLevel(entity.getRiskLevel().name());
        response.setIndicators(entity.getExplanationList());
        response.setWordCount(entity.getWordCount());
        response.setCharacterCount(entity.getCharacterCount());
        response.setTitle(entity.getTitle());
        response.setOriginalText(entity.getAnalyzedText());

        // Extract style statistics dynamically for UI metrics
        if (entity.getAnalyzedText() != null) {
            FeatureExtractor.ExtractedFeatures features = featureExtractor.extract(entity.getAnalyzedText());
            response.setSentenceCount(features.getSentenceCount());
            response.setExclamationCount(features.getExclamationCount());
            response.setQuestionCount(features.getQuestionCount());
            response.setCapitalizedWordsCount(features.getCapitalizedWordsCount());
        }

        response.setFormattedTimestamp(entity.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return response;
    }
}
