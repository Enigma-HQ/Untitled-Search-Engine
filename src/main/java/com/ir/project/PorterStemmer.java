package com.ir.project;

public class PorterStemmer {
    public String stem(String word) {
        if (word == null || word.length() < 3) return word;
        
        String stemmed = word.toLowerCase();
        // Basic suffix stripping rules for English
        if (stemmed.endsWith("ing")) return stemmed.substring(0, stemmed.length() - 3);
        if (stemmed.endsWith("ed")) return stemmed.substring(0, stemmed.length() - 2);
        if (stemmed.endsWith("es")) return stemmed.substring(0, stemmed.length() - 2);
        if (stemmed.endsWith("s") && !stemmed.endsWith("ss")) return stemmed.substring(0, stemmed.length() - 1);
        if (stemmed.endsWith("ly")) return stemmed.substring(0, stemmed.length() - 2);
        if (stemmed.endsWith("ment")) return stemmed.substring(0, stemmed.length() - 4);
        
        return stemmed;
    }
}