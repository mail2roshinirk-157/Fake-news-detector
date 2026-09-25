package com.example.fakenewsdetector;

import com.example.fakenewsdetector.dto.AnalysisResponse;
import com.example.fakenewsdetector.service.ClassificationEngine;
import com.example.fakenewsdetector.service.FeatureExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ClassificationEngineTest {

    @Autowired
    private ClassificationEngine engine;

    @Autowired
    private FeatureExtractor featureExtractor;

    @Test
    public void testAnalyzeGenuineText() {
        String title = "Federal Reserve Raises Rates";
        String text = "The Federal Reserve announced a quarter-point rate increase today. Stated by Chair Jerome Powell, the committee decided to raise interest rates to lower inflation. According to economists, the decision was expected.";
        
        FeatureExtractor.ExtractedFeatures features = featureExtractor.extract(text);
        AnalysisResponse response = engine.analyze(title, text, features);

        // Genuine indicators (attribution, numbers, low style flags) should result in GENUINE
        assertEquals("LIKELY_GENUINE", response.getPrediction());
        assertEquals("LOW", response.getRiskLevel());
        assertTrue(response.getConfidenceScore() >= 60.0);
        
        // Explanations should contain attribution
        assertTrue(response.getIndicators().stream().anyMatch(i -> i.toLowerCase().contains("attribution")));
    }

    @Test
    public void testAnalyzeFakeText() {
        String title = "SECRET ALIEN BASES DETECTED!";
        String text = "You won't believe what they are hiding from us!!! Bombshell whistleblower exposes that the moon is completely hollow and alien fleets are hovering over Chicago! Don't trust the lies of NASA and mainstream media! Share this miracle truth before it gets censored!";
        
        FeatureExtractor.ExtractedFeatures features = featureExtractor.extract(text);
        AnalysisResponse response = engine.analyze(title, text, features);

        // Fake indicators (clickbait, exclamations, sensational keywords, all caps) should result in FAKE
        assertEquals("LIKELY_FAKE", response.getPrediction());
        assertEquals("HIGH", response.getRiskLevel());
        assertTrue(response.getConfidenceScore() <= 39.0);
        
        // Explanations should include clickbait and sensationalism
        assertTrue(response.getIndicators().stream().anyMatch(i -> i.toLowerCase().contains("clickbait")));
        assertTrue(response.getIndicators().stream().anyMatch(i -> i.toLowerCase().contains("sensational")));
    }
}
