package com.example.fakenewsdetector.repository;

import com.example.fakenewsdetector.model.AnalysisHistory;
import com.example.fakenewsdetector.model.Prediction;
import com.example.fakenewsdetector.model.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistory, Long> {

    List<AnalysisHistory> findAllByOrderByTimestampDesc();

    List<AnalysisHistory> findTop10ByOrderByTimestampDesc();

    long countByPrediction(Prediction prediction);

    long countByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT AVG(h.confidenceScore) FROM AnalysisHistory h")
    Double getAverageConfidenceScore();

    // Query for text searches and filtering in history
    @Query("SELECT h FROM AnalysisHistory h WHERE " +
           "(:query IS NULL OR LOWER(h.originalInput) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(h.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:prediction IS NULL OR h.prediction = :prediction) AND " +
           "(:riskLevel IS NULL OR h.riskLevel = :riskLevel)")
    Page<AnalysisHistory> searchHistory(
            @Param("query") String query,
            @Param("prediction") Prediction prediction,
            @Param("riskLevel") RiskLevel riskLevel,
            Pageable pageable
    );
}
