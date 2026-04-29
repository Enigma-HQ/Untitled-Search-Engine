package com.ir.project;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            EnglishProcessor enProc = new EnglishProcessor();
            ArabicProcessor arProc = new ArabicProcessor();
            CorpusManager manager = new CorpusManager();

            // Create output folder
            new File("output").mkdir();

            // 1. Process English
            Map<String, List<String>> enResults = manager.processFolder("data/corpus/english", enProc);
            saveResult("output/english_results.txt", enResults);

            // 2. Process Arabic
            Map<String, List<String>> arResults = manager.processFolder("data/corpus/arabic", arProc);
            saveResult("output/arabic_results.txt", arResults);

            System.out.println("DONE! Check the 'output' folder to see your clean terms.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to save results to a file with UTF-8
    private static void saveResult(String fileName, Map<String, List<String>> data) {
        try (PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8)) {
            data.forEach((name, terms) -> {
                writer.println("File: " + name);
                writer.println("Terms: " + terms);
                writer.println("-----------------------------------");
            });
        } catch (Exception e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }
}