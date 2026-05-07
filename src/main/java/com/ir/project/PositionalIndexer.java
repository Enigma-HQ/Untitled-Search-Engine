package com.ir.project;

import java.util.*;

public class PositionalIndexer {

    // Map<Term, Map<DocID, List<Positions>>>
    private Map<String, Map<String, List<Integer>>> positionalIndex;
    private long buildTime;

    public PositionalIndexer() {
        this.positionalIndex = new HashMap<>();
    }

    public void buildIndex(Map<String, List<String>> englishData, Map<String, List<String>> arabicData) {
        long startTime = System.currentTimeMillis();

        indexCorpus(englishData);
        indexCorpus(arabicData);

        long endTime = System.currentTimeMillis();
        this.buildTime = endTime - startTime;
    }

    private void indexCorpus(Map<String, List<String>> corpusData) {
        for (Map.Entry<String, List<String>> entry : corpusData.entrySet()) {
            String docId = entry.getKey();
            List<String> terms = entry.getValue();

            for (int position = 0; position < terms.size(); position++) {
                String term = terms.get(position);

                positionalIndex.putIfAbsent(term, new HashMap<>());

                Map<String, List<Integer>> docMap = positionalIndex.get(term);
                docMap.putIfAbsent(docId, new ArrayList<>());
                docMap.get(docId).add(position);
            }
        }
    }

    public Map<String, Map<String, List<Integer>>> getIndex() {
        return positionalIndex;
    }

    public Set<String> getVocabulary() {
        return positionalIndex.keySet();
    }

    public long getBuildTime() {
        return buildTime;
    }
}