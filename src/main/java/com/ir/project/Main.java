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

            System.out.println("\nStarting Index Generation...");


            //Omar's indexing and kgram.
            PositionalIndexer posIndexer = new PositionalIndexer();
            posIndexer.buildIndex(enResults, arResults);

            KGramIndexer trigramIndexer = new KGramIndexer(3);
            trigramIndexer.buildIndex(posIndexer.getVocabulary());

            System.out.println("-----------------------------------");
            System.out.println("INDEXING METRICS:");
            System.out.println("Vocabulary Size: " + posIndexer.getVocabulary().size() + " unique terms");
            System.out.println("Positional Index Build Time: " + posIndexer.getBuildTime() + " ms");
            System.out.println("Trigram Index Build Time: " + trigramIndexer.getBuildTime() + " ms");
            System.out.println("Total Indexing Time: " + (posIndexer.getBuildTime() + trigramIndexer.getBuildTime()) + " ms");
            System.out.println("-----------------------------------");

            System.out.println("Saving index visualizations to the 'output' folder...");
            savePositionalIndex("output/positional_index.txt", posIndexer.getIndex());
            saveKGramIndex("output/trigram_index.txt", trigramIndexer.getIndex());
//            System.out.println("\nStarting Query Processing Tests...");
//            QueryProcessor qp =
//                    new QueryProcessor(
//                            posIndexer.getIndex(),
//                            enProc,
//                            arProc
//                    );
//            Scanner scanner = new Scanner(System.in);
//            System.out.println("\n--- IR System Ready! ---");
//            System.out.println("Enter query (or 'exit' to quit):");
//
//            while (true) {
//                System.out.print("Search> ");
//                String input = scanner.nextLine();
//                if (input.equalsIgnoreCase("exit")) break;
//
//                long startTime = System.nanoTime();
//                List<String> results = qp.processQuery(input);
//                long endTime = System.nanoTime();
//
//                System.out.println("Results found in (" + (endTime - startTime) / 1_000_000.0 + " ms):");
//                if (results.isEmpty()) {
//                    System.out.println("  [!] No documents matched your query.");
//                } else {
//                    results.forEach(doc -> System.out.println("  • " + doc));
//                }
//                System.out.println();
//            }
//            scanner.close();
//            System.out.println("-----------------------------------");
            System.out.println("Done! Open positional_index.txt and trigram_index.txt to view your work.");

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

    // these helper functions to see how it will appear in txt files.
    private static void savePositionalIndex(String fileName, Map<String, Map<String, List<Integer>>> index) {
        try (PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8)) {
            // Sort keys alphabetically for easier reading
            List<String> sortedTerms = new ArrayList<>(index.keySet());
            Collections.sort(sortedTerms);

            for (String term : sortedTerms) {
                writer.println("Term: '" + term + "'");
                Map<String, List<Integer>> docMap = index.get(term);

                for (Map.Entry<String, List<Integer>> docEntry : docMap.entrySet()) {
                    writer.println("  -> Doc: " + docEntry.getKey() + " | Positions: " + docEntry.getValue());
                }
                writer.println("-----------------------------------");
            }
        } catch (Exception e) {
            System.err.println("Error saving positional index: " + e.getMessage());
        }
    }
    // and this will save kgrams
    private static void saveKGramIndex(String fileName, Map<String, Set<String>> index) {
        try (PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8)) {
            List<String> sortedKgrams = new ArrayList<>(index.keySet());
            Collections.sort(sortedKgrams);

            for (String kgram : sortedKgrams) {
                writer.println("Trigram: '" + kgram + "' -> Terms: " + index.get(kgram));
            }
        } catch (Exception e) {
            System.err.println("Error saving trigram index: " + e.getMessage());
        }
    }
}