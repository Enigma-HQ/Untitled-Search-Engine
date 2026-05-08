package engine;

import java.util.*;


public class KGramIndex {

    private final int k;
    private final Map<String, Set<String>> kgramToTerms = new HashMap<>();
   //com → computer, community, company
    private final Map<String, Set<String>> termToKgrams = new HashMap<>();

    public KGramIndex(int k) {
        this.k = k;
    }

    public void addTerm(String term) {
        if (term == null || term.isEmpty()) return;
        String padded = "$" + term + "$";
        Set<String> kgrams = new HashSet<>();
        for (int i = 0; i <= padded.length() - k; i++) {
            kgrams.add(padded.substring(i, i + k));
        } // generate grams
        termToKgrams.put(term, kgrams);
        for (String kg : kgrams) {
            kgramToTerms.computeIfAbsent(kg, x -> new HashSet<>()).add(term);
        }
    }


     // Get kgrams for a given string (query token).

    public Set<String> getKgrams(String token) {
        String padded = "$" + token + "$";
        Set<String> kgrams = new HashSet<>();
        for (int i = 0; i <= padded.length() - k; i++) {
            kgrams.add(padded.substring(i, i + k));
        }
        return kgrams;
    }

    /**
     * Wildcard query: returns candidate terms matching the pattern.
     * Supports trailing wildcard like "comput*" or prefix "جامع*"
     */
    public Set<String> wildcardQuery(String pattern) {
        // Split on '*' comp*er -> ["comp", "er"]
        String[] parts = pattern.split("\\*", -1);
        Set<String> candidates = null;

        for (String part : parts) {
            if (part.isEmpty()) continue;
            String padded; // comput* -> $comput
            if (pattern.startsWith(part)) padded = "$" + part;
            else if (pattern.endsWith(part)) padded = part + "$";
            else padded = part;

            // Generate kgrams for this part
            Set<String> partKgrams = new HashSet<>();
            for (int i = 0; i <= padded.length() - k; i++) {
                if (i + k <= padded.length()) {
                    partKgrams.add(padded.substring(i, i + k));
                }
            }

            //  use shorter grams if padded part is too short
            if (partKgrams.isEmpty() && part.length() >= 1) {
                for (int i = 0; i < part.length(); i++) {
                    partKgrams.add(String.valueOf(part.charAt(i)));
                }
            }

            Set<String> matching = new HashSet<>();
            for (String kg : partKgrams) {
                Set<String> terms = kgramToTerms.get(kg);
                if (terms != null) matching.addAll(terms);
            }

            if (candidates == null) candidates = matching;
            else candidates.retainAll(matching);
        }

        if (candidates == null) return new HashSet<>();

        // Post-filter: term must actually match the pattern
        String regex = pattern.replace("*", ".*");
        candidates.removeIf(t -> !t.matches(regex));
        return candidates;
    }

    /**
      Jaccard similarity between query token and a vocabulary term.
      J(A,B) = |A∩B| / |A∪B|
     */
    public double jaccardSimilarity(String queryToken, String vocabTerm) {
        Set<String> qGrams = getKgrams(queryToken);
        Set<String> tGrams = termToKgrams.getOrDefault(vocabTerm, Collections.emptySet());
        if (qGrams.isEmpty() && tGrams.isEmpty()) return 1.0;
        if (qGrams.isEmpty() || tGrams.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(qGrams);
        intersection.retainAll(tGrams);

        Set<String> union = new HashSet<>(qGrams);
        union.addAll(tGrams);

        return (double) intersection.size() / union.size();
    }

    public Set<String> getAllTerms() {
        return Collections.unmodifiableSet(termToKgrams.keySet());
    }

    public int getK() { return k; }
}
