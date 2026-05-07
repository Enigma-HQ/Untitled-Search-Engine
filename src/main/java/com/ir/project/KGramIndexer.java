package com.ir.project;

import java.util.*;

public class KGramIndexer {

    // Map<Kgram, Set<Terms>>
    private Map<String, Set<String>> kgramIndex;
    private int k;
    private long buildTime;

    public KGramIndexer(int k) {
        this.kgramIndex = new HashMap<>();
        this.k = k;
    }

    public void buildIndex(Set<String> vocabulary) {
        long startTime = System.currentTimeMillis();

        for (String term : vocabulary) {
            // Pad term with '$' for prefix and suffix handling
            String paddedTerm = "$" + term + "$";

            // Generate k-grams
            for (int i = 0; i <= paddedTerm.length() - k; i++) {
                String kgram = paddedTerm.substring(i, i + k);

                kgramIndex.putIfAbsent(kgram, new HashSet<>());
                kgramIndex.get(kgram).add(term);
            }
        }

        long endTime = System.currentTimeMillis();
        this.buildTime = endTime - startTime;
    }

    public Map<String, Set<String>> getIndex() {
        return kgramIndex;
    }

    public long getBuildTime() {
        return buildTime;
    }
}