package com.example.fakenewsdetector;

import com.example.fakenewsdetector.service.TextPreprocessor;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TextPreprocessorTest {

    private final TextPreprocessor preprocessor = new TextPreprocessor();

    @Test
    public void testPreprocessText() {
        // Lowercasing and whitespace normalization
        String input1 = "  This   is a TEST  ";
        assertEquals("this is a test", preprocessor.preprocess(input1));

        // Punctuation removal
        String input2 = "Hello, world! How's it going? Code-123.";
        assertEquals("hello world how s it going code 123", preprocessor.preprocess(input2));

        // Character repetitions reduction (e.g. "soooo" -> "so", "goood" -> "good")
        String input3 = "This is sooooo goood and awesome!!!";
        // "sooooo" -> "soo", "goood" -> "good", "!!!" -> removed
        assertEquals("this is soo good and awesome", preprocessor.preprocess(input3));
    }

    @Test
    public void testTokenize() {
        String preprocessed = "this is a simple test";
        List<String> tokens = preprocessor.tokenize(preprocessed);
        assertEquals(5, tokens.size());
        assertEquals("this", tokens.get(0));
        assertEquals("simple", tokens.get(3));
    }

    @Test
    public void testRemoveStopWords() {
        List<String> tokens = Arrays.asList("this", "is", "a", "highly", "urgent", "and", "important", "report");
        List<String> filtered = preprocessor.removeStopWords(tokens);
        
        // Stop words like "this", "is", "a", "and" should be removed
        assertFalse(filtered.contains("this"));
        assertFalse(filtered.contains("is"));
        assertFalse(filtered.contains("a"));
        assertFalse(filtered.contains("and"));
        
        // Keep meaningful content
        assertTrue(filtered.contains("highly"));
        assertTrue(filtered.contains("urgent"));
        assertTrue(filtered.contains("important"));
        assertTrue(filtered.contains("report"));
    }

    @Test
    public void testIsFullyCapitalized() {
        assertTrue(preprocessor.isFullyCapitalized("URGENT"));
        assertTrue(preprocessor.isFullyCapitalized("BREAKING"));
        assertFalse(preprocessor.isFullyCapitalized("Breaking"));
        assertFalse(preprocessor.isFullyCapitalized("breaking"));
        assertFalse(preprocessor.isFullyCapitalized("A")); // too short
        assertFalse(preprocessor.isFullyCapitalized("123")); // not alphabetical
    }
}
