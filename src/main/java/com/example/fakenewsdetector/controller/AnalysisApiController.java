package com.example.fakenewsdetector.controller;

import com.example.fakenewsdetector.dto.AnalysisRequest;
import com.example.fakenewsdetector.dto.AnalysisResponse;
import com.example.fakenewsdetector.dto.DashboardStats;
import com.example.fakenewsdetector.service.AnalysisService;
import com.example.fakenewsdetector.service.NaiveBayesClassifier;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisApiController {

    @Autowired
    private AnalysisService service;

    @Autowired
    private NaiveBayesClassifier nbClassifier;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        AnalysisResponse response = service.analyzeNews(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<AnalysisResponse>> getHistory(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String prediction,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AnalysisResponse> history = service.getHistory(query, prediction, riskLevel, pageable);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<AnalysisResponse> getHistoryDetails(@PathVariable Long id) {
        AnalysisResponse response = service.getAnalysisDetails(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable Long id) {
        service.deleteHistory(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearAllHistory() {
        service.deleteAllHistory();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        DashboardStats stats = service.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("classifierTrained", nbClassifier.isTrained());
        return ResponseEntity.ok(health);
    }
}
