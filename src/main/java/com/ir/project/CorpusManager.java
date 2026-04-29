package com.ir.project;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class CorpusManager {
    // This method handles the batch processing of the corpus [cite: 4, 56]
    public Map<String, List<String>> processFolder(String folderPath, Object processor) {
        Map<String, List<String>> dataset = new HashMap<>();
        try {
            File folder = new File(folderPath);
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        // Crucial: Use UTF-8 to prevent '????' symbols 
                        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                        
                        List<String> terms;
                        if (processor instanceof EnglishProcessor) {
                            terms = ((EnglishProcessor) processor).process(content);
                        } else {
                            terms = ((ArabicProcessor) processor).process(content);
                        }
                        dataset.put(file.getName(), terms);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading corpus files: " + e.getMessage());
        }
        return dataset;
    }
}