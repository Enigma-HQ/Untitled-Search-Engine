package com.ir.project;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryProcessor {

    private Map<String, Map<String, List<Integer>>> positionalIndex;
    private EnglishProcessor englishProcessor;
    private ArabicProcessor arabicProcessor;

    public QueryProcessor(
            Map<String, Map<String, List<Integer>>> positionalIndex,
            EnglishProcessor englishProcessor,
            ArabicProcessor arabicProcessor) {
        this.positionalIndex = positionalIndex;
        this.englishProcessor = englishProcessor;
        this.arabicProcessor = arabicProcessor;
    }

    /**
     * Detects if a specific token contains Arabic characters.
     */
    public String detectLanguage(String text) {
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ARABIC) {
                return "arabic";
            }
        }
        return "english";
    }

    private List<String> preprocessToken(String token) {
        if (detectLanguage(token).equals("arabic")) {
            return arabicProcessor.process(token);
        } else {
            return englishProcessor.process(token);
        }
    }

    public List<String> processQuery(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();

        // Proximity query check: looks for "word /number word"
        Pattern proximityPattern = Pattern.compile("(\\S+)\\s*/(\\d+)\\s+(\\S+)");
        Matcher matcher = proximityPattern.matcher(query);

        if (matcher.find()) {
            return handleProximityQuery(matcher);
        }

        // Standard Multi-word or Single-word query
        String[] rawTokens = query.split("\\s+");
        List<String> processedTerms = new ArrayList<>();

        for (String raw : rawTokens) {
            processedTerms.addAll(preprocessToken(raw));
        }

        if (processedTerms.isEmpty()) return new ArrayList<>();
        if (processedTerms.size() == 1) return singleWordQuery(processedTerms.get(0));

        return multipleWordQuery(processedTerms);
    }

    public List<String> singleWordQuery(String term) {
        if (positionalIndex.containsKey(term)) {
            return new ArrayList<>(positionalIndex.get(term).keySet());
        }
        return new ArrayList<>();
    }

    public List<String> multipleWordQuery(List<String> terms) {
        // Initial set from the first term
        if (!positionalIndex.containsKey(terms.get(0))) return new ArrayList<>();

        Set<String> resultDocs = new HashSet<>(positionalIndex.get(terms.get(0)).keySet());

        // Intersect (AND) with subsequent terms
        for (int i = 1; i < terms.size(); i++) {
            String term = terms.get(i);
            if (!positionalIndex.containsKey(term)) {
                return new ArrayList<>(); // One term missing = zero results for AND query
            }
            resultDocs.retainAll(positionalIndex.get(term).keySet());
        }

        return new ArrayList<>(resultDocs);
    }

    private List<String> handleProximityQuery(Matcher matcher) {
        String t1Raw = matcher.group(1);
        int k = Integer.parseInt(matcher.group(2));
        String t2Raw = matcher.group(3);

        // Preprocess both terms (handling language automatically)
        List<String> t1Processed = preprocessToken(t1Raw);
        List<String> t2Processed = preprocessToken(t2Raw);

        if (t1Processed.isEmpty() || t2Processed.isEmpty()) return new ArrayList<>();

        return proximitySearch(t1Processed.get(0), t2Processed.get(0), k);
    }

    /**
     * Optimized Two-Pointer Proximity Search
     */
    public List<String> proximitySearch(String term1, String term2, int k) {
        List<String> results = new ArrayList<>();

        if (!positionalIndex.containsKey(term1) || !positionalIndex.containsKey(term2)) {
            return results;
        }

        Map<String, List<Integer>> docs1 = positionalIndex.get(term1);
        Map<String, List<Integer>> docs2 = positionalIndex.get(term2);

        // Find documents containing both terms
        Set<String> commonDocs = new HashSet<>(docs1.keySet());
        commonDocs.retainAll(docs2.keySet());

        for (String docID : commonDocs) {
            List<Integer> pos1 = docs1.get(docID);
            List<Integer> pos2 = docs2.get(docID);

            // Linear Merge (Two-Pointer) approach
            int i = 0, j = 0;
            while (i < pos1.size() && j < pos2.size()) {
                int p1 = pos1.get(i);
                int p2 = pos2.get(j);

                if (Math.abs(p1 - p2) <= k) {
                    results.add(docID);
                    break; // Move to the next document
                } else if (p1 < p2) {
                    i++;
                } else {
                    j++;
                }
            }
        }

        return results;
    }
}