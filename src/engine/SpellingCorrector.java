package engine;

import java.util.*;

public class SpellingCorrector {

    private final KGramIndex kgramIndex;

    public SpellingCorrector(KGramIndex kgramIndex) {
        this.kgramIndex = kgramIndex;
    }


     // Levenshtein edit distance between two strings.
     public int editDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],
                                   Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[m][n];
    }

    /*
      Get top N spelling suggestions for an unknown term.
      Combines Jaccard similarity (k-gram based) and edit distance.
     */
    public List<String> getSuggestions(String misspelled, int topN) {
        // Step 1: Candidate generation via Jaccard similarity
        Set<String> vocab = kgramIndex.getAllTerms();
        List<Map.Entry<String, Double>> candidates = new ArrayList<>();

        for (String vocabTerm : vocab) {
            double jaccard = kgramIndex.jaccardSimilarity(misspelled, vocabTerm);
            if (jaccard > 0.1) { // threshold to reduce candidates
                candidates.add(new AbstractMap.SimpleEntry<>(vocabTerm, jaccard));
            }
        }

        // Step 2: Re-rank candidates by edit distance
        candidates.sort((a, b) -> {
            // Primary: edit distance (lower = better)
            int edA = editDistance(misspelled, a.getKey());
            int edB = editDistance(misspelled, b.getKey());
            if (edA != edB) return Integer.compare(edA, edB);
            // Secondary: jaccard (higher = better)
            return Double.compare(b.getValue(), a.getValue());
        });

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topN, candidates.size()); i++) {
            result.add(candidates.get(i).getKey());
        }
        return result;
    }
}
