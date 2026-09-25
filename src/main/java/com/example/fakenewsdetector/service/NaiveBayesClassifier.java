package com.example.fakenewsdetector.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class NaiveBayesClassifier {

    private static final Logger logger = LoggerFactory.getLogger(NaiveBayesClassifier.class);

    @Autowired
    private TextPreprocessor preprocessor;

    // Counts for Naive Bayes calculation
    private final Map<String, Map<String, Integer>> wordCounts = new HashMap<>();
    private final Map<String, Integer> totalWordsInClass = new HashMap<>();
    private final Map<String, Integer> classDocumentCounts = new HashMap<>();
    private final Set<String> vocabulary = new HashSet<>();
    private int totalDocuments = 0;
    private boolean trained = false;

    public static class TrainingItem {
        private String title;
        private String text;
        private String label; // "FAKE" or "REAL"

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public NaiveBayesClassifier() {
        wordCounts.put("FAKE", new HashMap<>());
        wordCounts.put("REAL", new HashMap<>());
        totalWordsInClass.put("FAKE", 0);
        totalWordsInClass.put("REAL", 0);
        classDocumentCounts.put("FAKE", 0);
        classDocumentCounts.put("REAL", 0);
    }

    @PostConstruct
    public void init() {
        try {
            logger.info("Initializing Naive Bayes Classifier with sample dataset...");
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = new ClassPathResource("sample-dataset.json").getInputStream();
            List<TrainingItem> dataset = mapper.readValue(is, new TypeReference<List<TrainingItem>>() {});
            train(dataset);
            logger.info("Naive Bayes Classifier successfully trained on {} samples. Vocabulary size: {}", totalDocuments, vocabulary.size());
        } catch (Exception e) {
            logger.error("Failed to train Naive Bayes Classifier on startup. Falling back to empty model.", e);
        }
    }

    /**
     * Train the Naive Bayes model on a list of training items.
     */
    public synchronized void train(List<TrainingItem> items) {
        if (items == null || items.isEmpty()) return;

        for (TrainingItem item : items) {
            String combinedText = (item.getTitle() != null ? item.getTitle() : "") + " " + (item.getText() != null ? item.getText() : "");
            String label = item.getLabel().toUpperCase();
            if (!"FAKE".equals(label) && !"REAL".equals(label)) {
                continue; // skip invalid labels
            }

            // Preprocess and tokenize
            String cleanText = preprocessor.preprocess(combinedText);
            List<String> tokens = preprocessor.tokenize(cleanText);
            List<String> filteredTokens = preprocessor.removeStopWords(tokens);

            // Increment doc count
            classDocumentCounts.put(label, classDocumentCounts.get(label) + 1);
            totalDocuments++;

            // Update word frequency maps
            Map<String, Integer> labelWordMap = wordCounts.get(label);
            for (String token : filteredTokens) {
                labelWordMap.put(token, labelWordMap.getOrDefault(token, 0) + 1);
                totalWordsInClass.put(label, totalWordsInClass.get(label) + 1);
                vocabulary.add(token);
            }
        }
        trained = true;
    }

    /**
     * Calculates the probability (0.0 to 1.0) of the text being FAKE.
     */
    public double predictFakeProbability(String text) {
        if (!trained || totalDocuments == 0 || text == null || text.trim().isEmpty()) {
            return 0.5; // Neutral default if not trained or empty input
        }

        String cleanText = preprocessor.preprocess(text);
        List<String> tokens = preprocessor.tokenize(cleanText);
        List<String> filteredTokens = preprocessor.removeStopWords(tokens);

        if (filteredTokens.isEmpty()) {
            return 0.5;
        }

        // Prior probabilities log P(Class)
        double pFakePrior = (double) classDocumentCounts.get("FAKE") / totalDocuments;
        double pRealPrior = (double) classDocumentCounts.get("REAL") / totalDocuments;

        double logScoreFake = Math.log(pFakePrior);
        double logScoreReal = Math.log(pRealPrior);

        // Vocabulary size (V) for Laplace smoothing
        int vocabSize = vocabulary.size();
        int totalFakeWords = totalWordsInClass.get("FAKE");
        int totalRealWords = totalWordsInClass.get("REAL");

        Map<String, Integer> fakeWordMap = wordCounts.get("FAKE");
        Map<String, Integer> realWordMap = wordCounts.get("REAL");

        // Log likelihood: sum of log P(word | Class)
        for (String token : filteredTokens) {
            // Laplace smoothing: P(word|Class) = (count(word, Class) + 1) / (totalWords(Class) + V)
            int countInFake = fakeWordMap.getOrDefault(token, 0);
            double pWordGivenFake = (double) (countInFake + 1) / (totalFakeWords + vocabSize);
            logScoreFake += Math.log(pWordGivenFake);

            int countInReal = realWordMap.getOrDefault(token, 0);
            double pWordGivenReal = (double) (countInReal + 1) / (totalRealWords + vocabSize);
            logScoreReal += Math.log(pWordGivenReal);
        }

        // Softmax conversion from log-space to probability:
        // P(Fake | document) = 1 / (1 + e^(logScoreReal - logScoreFake))
        double diff = logScoreReal - logScoreFake;
        
        // Prevent overflow/underflow bounds
        if (diff > 20) {
            return 0.0;
        } else if (diff < -20) {
            return 1.0;
        }

        return 1.0 / (1.0 + Math.exp(diff));
    }

    public boolean isTrained() {
        return trained;
    }
}
