package engine;


  // search result  Document + Score + Snippet

public class SearchResult {
    private final Document document;
    private final double score;
    private final String snippet;

    public SearchResult(Document document, double score, String snippet) {
        this.document = document;
        this.score = score;
        this.snippet = snippet;
    }

    public Document getDocument() { return document; }
    public double getScore() { return score; }
    public String getSnippet() { return snippet; }

    @Override
    public String toString() {
        return String.format("[%.4f] %s", score, document.getTitle());
    }
}
