package com.ir.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class EnglishProcessor {
    private PorterStemmer stemmer = new PorterStemmer();
    private Set<String> stopWords = new HashSet<>();

    public EnglishProcessor() {
        try {
            // Loading stop-words using UTF-8 to ensure consistency
            List<String> lines = Files.readAllLines(Paths.get("data/english_stop_words.txt"), StandardCharsets.UTF_8);
            for (String line : lines) {
                stopWords.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load English stop-words file.");
        }
    }

    public List<String> process(String text) {
        if (text == null) return new ArrayList<>();
        // Tokenization and cleaning non-alphabet characters [cite: 7]
        String[] tokens = text.toLowerCase().replaceAll("[^a-zA-Z ]", "").split("\\s+");
        List<String> result = new ArrayList<>();

        for (String token : tokens) {
            // Stop-word removal and Stemming [cite: 8, 9]
            if (!token.isEmpty() && !stopWords.contains(token)) {
                result.add(stemmer.stem(token));
            }
        }
        return result;
    }
}