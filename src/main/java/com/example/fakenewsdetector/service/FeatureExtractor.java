package com.example.fakenewsdetector.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FeatureExtractor {

    // Clickbait indicators (lowercase)
    private static final List<String> CLICKBAIT_PHRASES = Arrays.asList(
        "you won t believe", "won t believe what", "shocking truth", "what happens next", 
        "will blow your mind", "blow your mind", "doctors are shocked", "secret the government", 
        "make you cry", "see this before it", "easy way to", "make money fast", 
        "this is why", "what they don t want", "you need to know", "goes viral"
    );

    // Sensational / Suspicious terms
    private static final Set<String> SENSATIONAL_WORDS = new HashSet<>(Arrays.asList(
        "unbelievable", "miracle", "conspiracy", "exposed", "scam", "hoax", "agenda", 
        "hiding", "secret", "coincidence", "proof", "insider", "shocking", "bombshell", 
        "propaganda", "fake", "scandal", "lie", "lies", "rigged", "censored", "truth"
    ));

    // Emotional / Dramatic language
    private static final Set<String> EMOTIONAL_WORDS = new HashSet<>(Arrays.asList(
        "terrible", "horrific", "furious", "outrage", "disaster", "destroys", "slams", 
        "rips", "eviscerates", "devastating", "awesome", "amazing", "incredible", 
        "heartbreaking", "panicked", "terrifying", "hate", "love", "scared", "shocked"
    ));

    // Attribution signals (indicate source verification/reporting)
    private static final List<String> ATTRIBUTION_PHRASES = Arrays.asList(
        "according to", "stated that", "reported by", "spokesperson", "study published", 
        "officials said", "confirmed by", "researchers at", "source says", "announced today",
        "in an interview", "released a statement", "press release"
    );

    public static class ExtractedFeatures {
        private int charCount;
        private int wordCount;
        private int sentenceCount;
        private int exclamationCount;
        private int questionCount;
        private int capitalizedWordsCount;
        private int sensationalWordsCount;
        private int clickbaitPhrasesCount;
        private int emotionalWordsCount;
        private int urlCount;
        private int attributionSignalsCount;
        private int numberCount;
        private int repeatedWordsCount;

        // Getters and Setters
        public int getCharCount() { return charCount; }
        public void setCharCount(int charCount) { this.charCount = charCount; }
        public int getWordCount() { return wordCount; }
        public void setWordCount(int wordCount) { this.wordCount = wordCount; }
        public int getSentenceCount() { return sentenceCount; }
        public void setSentenceCount(int sentenceCount) { this.sentenceCount = sentenceCount; }
        public int getExclamationCount() { return exclamationCount; }
        public void setExclamationCount(int exclamationCount) { this.exclamationCount = exclamationCount; }
        public int getQuestionCount() { return questionCount; }
        public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
        public int getCapitalizedWordsCount() { return capitalizedWordsCount; }
        public void setCapitalizedWordsCount(int capitalizedWordsCount) { this.capitalizedWordsCount = capitalizedWordsCount; }
        public int getSensationalWordsCount() { return sensationalWordsCount; }
        public void setSensationalWordsCount(int sensationalWordsCount) { this.sensationalWordsCount = sensationalWordsCount; }
        public int getClickbaitPhrasesCount() { return clickbaitPhrasesCount; }
        public void setClickbaitPhrasesCount(int clickbaitPhrasesCount) { this.clickbaitPhrasesCount = clickbaitPhrasesCount; }
        public int getEmotionalWordsCount() { return emotionalWordsCount; }
        public void setEmotionalWordsCount(int emotionalWordsCount) { this.emotionalWordsCount = emotionalWordsCount; }
        public int getUrlCount() { return urlCount; }
        public void setUrlCount(int urlCount) { this.urlCount = urlCount; }
        public int getAttributionSignalsCount() { return attributionSignalsCount; }
        public void setAttributionSignalsCount(int attributionSignalsCount) { this.attributionSignalsCount = attributionSignalsCount; }
        public int getNumberCount() { return numberCount; }
        public void setNumberCount(int numberCount) { this.numberCount = numberCount; }
        public int getRepeatedWordsCount() { return repeatedWordsCount; }
        public void setRepeatedWordsCount(int repeatedWordsCount) { this.repeatedWordsCount = repeatedWordsCount; }

        public double getCapitalizedRatio() {
            return wordCount == 0 ? 0.0 : (double) capitalizedWordsCount / wordCount;
        }

        public double getExclamationRatio() {
            return sentenceCount == 0 ? 0.0 : (double) exclamationCount / sentenceCount;
        }
    }

    /**
     * Extracts grammatical, punctuation, lexical, and stylistic features from raw text.
     */
    public ExtractedFeatures extract(String text) {
        ExtractedFeatures features = new ExtractedFeatures();
        if (text == null || text.trim().isEmpty()) {
            return features;
        }

        features.setCharCount(text.length());

        // 1. Punctuation counts (exclamation & question marks)
        int exclamations = countOccurrences(text, "!");
        int questions = countOccurrences(text, "?");
        features.setExclamationCount(exclamations);
        features.setQuestionCount(questions);

        // 2. Sentence count estimation
        // Split by standard end-of-sentence punctuation (., !, ?)
        String[] sentences = text.split("[.!?]+");
        features.setSentenceCount(Math.max(1, sentences.length));

        // 3. Word tokenization (keeping case for capitalization count)
        // Split by whitespace and remove non-alphabetic/numeric boundary chars
        String[] words = text.split("\\s+");
        List<String> cleanWords = new ArrayList<>();
        int capitalizedCount = 0;
        int numCount = 0;

        for (String w : words) {
            String cleanW = w.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "");
            if (!cleanW.isEmpty()) {
                cleanWords.add(cleanW);
                // Check if word is fully capitalized (e.g. URGENT, BREAKING) and at least 2 chars long
                if (cleanW.length() >= 2 && cleanW.matches("[A-Z0-9]+")) {
                    capitalizedCount++;
                }
                // Check if it's a number
                if (cleanW.matches("\\d+")) {
                    numCount++;
                }
            }
        }
        features.setWordCount(cleanWords.size());
        features.setCapitalizedWordsCount(capitalizedCount);
        features.setNumberCount(numCount);

        // 4. Repeated words count (vocabulary diversity check)
        Set<String> uniqueWords = new HashSet<>();
        int repeatedCount = 0;
        for (String w : cleanWords) {
            String lower = w.toLowerCase();
            if (uniqueWords.contains(lower)) {
                repeatedCount++;
            } else {
                uniqueWords.add(lower);
            }
        }
        features.setRepeatedWordsCount(repeatedCount);

        // 5. Lowercase and normalize punctuation/whitespace for keyword/phrase searching
        String lowerText = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ");

        // 6. Clickbait phrases detection
        int clickbaitCount = 0;
        for (String phrase : CLICKBAIT_PHRASES) {
            if (lowerText.contains(phrase)) {
                clickbaitCount++;
            }
        }
        features.setClickbaitPhrasesCount(clickbaitCount);

        // 7. Sensational and Emotional word counts
        int sensationalCount = 0;
        int emotionalCount = 0;
        for (String w : cleanWords) {
            String lowerW = w.toLowerCase();
            if (SENSATIONAL_WORDS.contains(lowerW)) {
                sensationalCount++;
            }
            if (EMOTIONAL_WORDS.contains(lowerW)) {
                emotionalCount++;
            }
        }
        features.setSensationalWordsCount(sensationalCount);
        features.setEmotionalWordsCount(emotionalCount);

        // 8. Attribution signals
        int attributionCount = 0;
        for (String phrase : ATTRIBUTION_PHRASES) {
            if (lowerText.contains(phrase)) {
                attributionCount++;
            }
        }
        features.setAttributionSignalsCount(attributionCount);

        // 9. URLs in text (basic regex check)
        int urlCount = 0;
        Pattern urlPattern = Pattern.compile("https?://\\S+\\.\\S+");
        Matcher urlMatcher = urlPattern.matcher(text);
        while (urlMatcher.find()) {
            urlCount++;
        }
        features.setUrlCount(urlCount);

        return features;
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }
}
