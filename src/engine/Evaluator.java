package engine;

import java.util.*;

// Evaluation module: Precision, Recall, F1.
public class Evaluator {

    public static class EvalResult {
        public final String query;
        public final double precision;
        public final double recall;
        public final double f1;
        public final int retrieved;
        public final int relevant;
        public final int relevantRetrieved;

        public EvalResult(String query, int retrieved, int relevant, int relevantRetrieved) {
            this.query = query;
            this.retrieved = retrieved;
            this.relevant = relevant;
            this.relevantRetrieved = relevantRetrieved;
            this.precision = retrieved == 0 ? 0 : (double) relevantRetrieved / retrieved;
            this.recall = relevant == 0 ? 0 : (double) relevantRetrieved / relevant;
            this.f1 = (precision + recall) == 0 ? 0 :
                      2 * precision * recall / (precision + recall);
        }

        @Override
        public String toString() {
            return String.format("Query: '%s'\n  Retrieved=%d | Relevant=%d | Relevant∩Retrieved=%d\n" +
                "  Precision=%.3f | Recall=%.3f | F1=%.3f",
                query, retrieved, relevant, relevantRetrieved, precision, recall, f1);
        }
    }


      //Evaluate a set of results against known relevant document IDs.
    public static EvalResult evaluate(String query,
                                       List<SearchResult> results,
                                       Set<Integer> relevantDocIds) {
        int retrieved = results.size();
        int relevant = relevantDocIds.size();
        int relevantRetrieved = 0;
        for (SearchResult r : results) {
            if (relevantDocIds.contains(r.getDocument().getId())) relevantRetrieved++;
        }
        return new EvalResult(query, retrieved, relevant, relevantRetrieved);
    }
}
