package com.ir.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ArabicProcessor {
    private Set<String> stopWords = new HashSet<>();

    public ArabicProcessor() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("data/arabic_stop_words.txt"), StandardCharsets.UTF_8);
            for (String line : lines) {
                stopWords.add(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Warning: Arabic stop-words file not found.");
        }
    }

    public List<String> process(String text) {
        if (text == null) return new ArrayList<>();
        
        // 1. Normalization: unify Alif shapes and remove Tashkeel [cite: 12]
        String normalized = text.replaceAll("[\\u064B-\\u0652]", ""); // Remove Diacritics
        normalized = normalized.replaceAll("[أإآ]", "ا").replaceAll("ى", "ي").replaceAll("ة", "ه");
        
        // 2. Tokenization [cite: 7]
        String[] tokens = normalized.split("\\s+");
        List<String> result = new ArrayList<>();

        for (String token : tokens) {
            String clean = token.replaceAll("[^\\u0621-\\u064A]", "");
            
            // 3. Stop-word removal [cite: 14]
            if (!clean.isEmpty() && !stopWords.contains(clean)) {
                // 4. Enhanced Light Stemming 
                result.add(enhancedLightStem(clean));
            }
        }
        return result;
    }

    private String enhancedLightStem(String w) {
        if (w.length() <= 3) return w; // Don't stem very short words to avoid losing meaning

        // A. Remove Prefixes (From longest to shortest)
        if (w.startsWith("الاست")) w = w.substring(5);
        else if (w.startsWith("ال") || w.startsWith("لل") || w.startsWith("بال")) w = w.substring(2);
        else if (w.startsWith("و") || w.startsWith("ف") || w.startsWith("ب") || w.startsWith("ك")) w = w.substring(1);

        // B. Remove Suffixes (From longest to shortest)
        if (w.endsWith("ات") || w.endsWith("ون") || w.endsWith("ين") || w.endsWith("هم") || w.endsWith("كم")) {
            w = w.substring(0, w.length() - 2);
        } else if (w.endsWith("ه") || w.endsWith("ي") || w.endsWith("نا") || w.endsWith("ت")) {
            // Check length again before removing single character suffix
            if (w.length() > 3) w = w.substring(0, w.length() - 1);
        }
        
        return w;
    }
}