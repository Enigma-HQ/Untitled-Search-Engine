package engine;

import java.util.*;

// Vector Space Model with TF-IDF weighting and Cosine Similarity.
public class VectorSpaceModel {

    private final PositionalIndex index;
    private final int docCount;

    // Precomputed document vector norms
    private final Map<Integer, Double> docNorms = new HashMap<>();

    public VectorSpaceModel(PositionalIndex index) {
        this.index = index;
        this.docCount = index.getDocCount();
    }


     // TF weight: 1 + log10(tf) if tf > 0, else 0
    public double tfWeight(int tf) {
        return tf > 0 ? 1.0 + Math.log10(tf) : 0.0;
    }


      // IDF weight: log10(N / df)
    public double idfWeight(String term) {
        int df = index.df(term);
        if (df == 0) return 0.0;
        return Math.log10((double) docCount / df);
    }


      //TF-IDF weight for a term in a document.
    public double tfidf(String term, int docId) {
        int tf = index.tf(term, docId);
        return tfWeight(tf) * idfWeight(term);
    }

    /*
      Precompute document vector norms for efficient cosine similarity.
      Call once after indexing is complete.
     */
    public void precomputeNorms(Collection<Integer> docIds) {
        docNorms.clear();
        for (String term : index.getVocabulary()) {
            double idf = idfWeight(term);
            for (Map.Entry<Integer, List<Integer>> entry : index.getPostings(term).entrySet()) {
                int docId = entry.getKey();
                int tf = entry.getValue().size();
                double weight = tfWeight(tf) * idf;
                docNorms.merge(docId, weight * weight, Double::sum);
            }
        }
        docNorms.replaceAll((id, sumSq) -> Math.sqrt(sumSq));
    }

    /*
      Ranked retrieval: compute cosine similarity of query vs all documents.
      Returns list of (docId, score) sorted by score descending.
     */
    public List<Map.Entry<Integer, Double>> rankedQuery(List<String> queryTerms) {
        if (queryTerms.isEmpty()) return Collections.emptyList();

        // Build query vector (tf-idf for query terms)
        Map<String, Double> queryVector = new HashMap<>();
        Map<String, Integer> queryTF = new HashMap<>();
        for (String term : queryTerms) {
            queryTF.merge(term, 1, Integer::sum);
        }
        double queryNormSq = 0;
        for (Map.Entry<String, Integer> e : queryTF.entrySet()) {
            String term = e.getKey();
            double w = tfWeight(e.getValue()) * idfWeight(term);
            queryVector.put(term, w);
            queryNormSq += w * w;
        }
        double queryNorm = Math.sqrt(queryNormSq);
        if (queryNorm == 0) return Collections.emptyList();

        // Accumulate dot products
        Map<Integer, Double> scores = new HashMap<>();
        for (Map.Entry<String, Double> qe : queryVector.entrySet()) {
            String term = qe.getKey();
            double qWeight = qe.getValue();
            for (Map.Entry<Integer, List<Integer>> posting : index.getPostings(term).entrySet()) {
                int docId = posting.getKey();
                int tf = posting.getValue().size();
                double dWeight = tfWeight(tf) * idfWeight(term);
                scores.merge(docId, qWeight * dWeight, Double::sum);
            }
        }

        // Normalize by document and query norms
        final double qNorm = queryNorm;
        scores.replaceAll((docId, dotProduct) -> {
            double dNorm = docNorms.getOrDefault(docId, 1.0);
            return dotProduct / (dNorm * qNorm);
        });

        // Sort by score descending
        List<Map.Entry<Integer, Double>> results = new ArrayList<>(scores.entrySet());
        results.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return results;
    }

    public double getDocNorm(int docId) {
        return docNorms.getOrDefault(docId, 0.0);
    }
}
