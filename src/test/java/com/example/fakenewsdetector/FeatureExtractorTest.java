package com.example.fakenewsdetector;

import com.example.fakenewsdetector.service.FeatureExtractor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FeatureExtractorTest {

    private final FeatureExtractor extractor = new FeatureExtractor();

    @Test
    public void testExtractFeatures() {
        // Text loaded with typical fake-style markers
        String text = "SCANDAL: You won't believe what happens next! The secret bases are exposed!!!";
        FeatureExtractor.ExtractedFeatures features = extractor.extract(text);

        // Verify punctuation counts
        assertEquals(4, features.getExclamationCount()); // '!' from happens next! and '!!!'
        assertEquals(0, features.getQuestionCount());

        // Verify capitalization (SCANDAL)
        assertEquals(1, features.getCapitalizedWordsCount());

        // Clickbait phrase match ("you won't believe what" or "what happens next")
        assertTrue(features.getClickbaitPhrasesCount() > 0);

        // Sensational words ("scandal", "secret", "exposed")
        assertTrue(features.getSensationalWordsCount() >= 2);

        // Character count
        assertEquals(text.length(), features.getCharCount());

        // Word count
        assertTrue(features.getWordCount() > 5);
    }

    @Test
    public void testAttributionAndUrls() {
        String text = "According to researchers at Oxford, a new vaccine was confirmed by officials today. Visit https://oxford.edu/news";
        FeatureExtractor.ExtractedFeatures features = extractor.extract(text);

        // Positive indicators
        assertTrue(features.getAttributionSignalsCount() >= 1);
        assertEquals(1, features.getUrlCount());
    }
}
