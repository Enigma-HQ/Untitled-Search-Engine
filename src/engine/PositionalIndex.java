package engine;

import java.util.*;


 // Maps: term -> { docId -> [positions] }
public class PositionalIndex {

    // term -> docId -> list of positions
    private final Map<String, Map<Integer, List<Integer>>> index = new TreeMap<>();

    // total number of docs
    private int docCount = 0;

    public void addTerm(String term, int docId, int position) {
        index.computeIfAbsent(term, k -> new HashMap<>())
             .computeIfAbsent(docId, k -> new ArrayList<>())
             .add(position);
    }

    public void setDocCount(int count) {
        this.docCount = count;
    }

    public int getDocCount() {
        return docCount;
    }

    // Returns postings for a term docId -> positions
    public Map<Integer, List<Integer>> getPostings(String term) {
        return index.getOrDefault(term, Collections.emptyMap());
    }

    // Returns set of docIds containing the term
    public Set<Integer> getDocIds(String term) {
        Map<Integer, List<Integer>> postings = index.get(term);
        return postings == null ? new HashSet<>() : postings.keySet();
    }

    // Document frequency of a term
    public int df(String term) {
        Map<Integer, List<Integer>> postings = index.get(term);
        return postings == null ? 0 : postings.size();
    }

    // Term frequency of term in a document
    public int tf(String term, int docId) {
        Map<Integer, List<Integer>> postings = index.get(term);
        if (postings == null) return 0;
        List<Integer> positions = postings.get(docId);
        return positions == null ? 0 : positions.size();
    }

    // All indexed terms
    public Set<String> getVocabulary() {
        return Collections.unmodifiableSet(index.keySet());
    }

    /*
      Phrase query: find docs where terms appear consecutively.
      terms[0] at pos p, terms[1] at pos p+1, ..., ..
     */
    public Set<Integer> phraseQuery(List<String> terms) {
        if (terms.isEmpty()) return Collections.emptySet();
        if (terms.size() == 1) return getDocIds(terms.get(0));

        Set<Integer> candidates = new HashSet<>(getDocIds(terms.get(0)));
        for (int i = 1; i < terms.size(); i++) {
            candidates.retainAll(getDocIds(terms.get(i)));
        }

        Set<Integer> result = new HashSet<>();
        for (int docId : candidates) {
            // Check if term[0] has position p such that term[i] has position p+i
            List<Integer> pos0 = getPostings(terms.get(0)).get(docId);
            for (int startPos : pos0) {
                boolean found = true;
                for (int i = 1; i < terms.size(); i++) {
                    List<Integer> posI = getPostings(terms.get(i)).get(docId);
                    if (posI == null || !posI.contains(startPos + i)) {
                        found = false;
                        break;
                    }
                }
                if (found) { result.add(docId); break; }
            }
        }
        return result;
    }


     // Proximity query: find docs where term1 and term2 appear within k positions.
    public Set<Integer> proximityQuery(String term1, String term2, int k) {
        Set<Integer> candidates = new HashSet<>(getDocIds(term1));
        candidates.retainAll(getDocIds(term2));

        Set<Integer> result = new HashSet<>();
        for (int docId : candidates) {
            List<Integer> pos1 = getPostings(term1).get(docId);
            List<Integer> pos2 = getPostings(term2).get(docId);
            if (pos1 == null || pos2 == null) continue;

            outer:
            for (int p1 : pos1) {
                for (int p2 : pos2) {
                    if (Math.abs(p1 - p2) <= k) {
                        result.add(docId);
                        break outer;
                    }
                }
            }
        }
        return result;
    }

   //  Boolean AND of multiple term sets
    public Set<Integer> andQuery(List<String> terms) {
        if (terms.isEmpty()) return Collections.emptySet();
        Set<Integer> result = new HashSet<>(getDocIds(terms.get(0)));
        for (int i = 1; i < terms.size(); i++) {
            result.retainAll(getDocIds(terms.get(i)));
        }
        return result;
    }

    // Boolean OR of multiple term sets
    public Set<Integer> orQuery(List<String> terms) {
        Set<Integer> result = new HashSet<>();
        for (String term : terms) result.addAll(getDocIds(term));
        return result;
    }

    public boolean containsTerm(String term) {
        return index.containsKey(term);
    }

    public int size() {
        return index.size();
    }

    @Override
    public String toString() {
        return "PositionalIndex{terms=" + index.size() + ", docs=" + docCount + "}";
    }
}
