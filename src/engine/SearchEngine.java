package engine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/*
  Core Search Engine:
  - Loads corpus (English + Arabic documents)
  - Builds Positional Inverted Index
  - Builds K-gram index for wildcards/spelling
  - Supports all query types
  - TF-IDF ranked retrieval
 */
public class SearchEngine {

    private final List<Document> documents = new ArrayList<>();
    private final Map<Integer, Document> docMap = new HashMap<>();

    // Separate indexes for English and Arabic
    private final PositionalIndex englishIndex = new PositionalIndex();
    private final PositionalIndex arabicIndex = new PositionalIndex();
    private final KGramIndex englishKgram = new KGramIndex(2);
    private final KGramIndex arabicKgram = new KGramIndex(2);

    private VectorSpaceModel englishVSM;
    private VectorSpaceModel arabicVSM;

    private final EnglishProcessor englishProc = new EnglishProcessor();
    private final ArabicProcessor arabicProc = new ArabicProcessor();

    private SpellingCorrector englishCorrector;
    private SpellingCorrector arabicCorrector;

    // Build statistics
    private long indexBuildTimeMs = 0;
    private int totalWords = 0;

     // Load all documents from English and Arabic directories.
    public void loadCorpus(String englishDir, String arabicDir) throws IOException {
        int id = 0;
        id = loadDirectory(englishDir, id, false);
        id = loadDirectory(arabicDir, id, true);

        if (documents.isEmpty()) {
            throw new IOException("No documents found in corpus directories: " + englishDir + ", " + arabicDir);
        }
    }

    private int loadDirectory(String dir, int startId, boolean arabic) throws IOException {
        Path dirPath = Paths.get(dir);
        if (!Files.exists(dirPath)) return startId;

        int id = startId;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.txt")) {
            for (Path file : stream) {
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                String title = file.getFileName().toString().replace(".txt", "");
                Document doc = new Document(id, file.toString(), title, content, arabic);
                documents.add(doc);
                docMap.put(id, doc);
                id++;
            }
        }
        return id;
    }

    /**
     * Build all indexes from loaded corpus.
     */
    public void buildIndex() {
        long start = System.currentTimeMillis(); // time for build

        int engCount = 0, arCount = 0; // no of docs
        for (Document doc : documents) {
            if (doc.isArabic()) arCount++;
            else engCount++;
        }

        englishIndex.setDocCount(engCount);
        arabicIndex.setDocCount(arCount);

        for (Document doc : documents) {
            if (doc.isArabic()) {
                indexArabic(doc);
            } else {
                indexEnglish(doc);
            }
        }

        // Build k-gram indexes from vocabulary
        for (String term : englishIndex.getVocabulary()) englishKgram.addTerm(term);
        for (String term : arabicIndex.getVocabulary()) arabicKgram.addTerm(term);

        // Build VSM
        englishVSM = new VectorSpaceModel(englishIndex);
        arabicVSM = new VectorSpaceModel(arabicIndex);

        Set<Integer> engIds = documents.stream().filter(d -> !d.isArabic())
            .map(Document::getId).collect(Collectors.toSet());
        Set<Integer> arIds = documents.stream().filter(Document::isArabic)
            .map(Document::getId).collect(Collectors.toSet());

        englishVSM.precomputeNorms(engIds);
        arabicVSM.precomputeNorms(arIds);

        // Spelling correctors
        englishCorrector = new SpellingCorrector(englishKgram);
        arabicCorrector = new SpellingCorrector(arabicKgram);

        indexBuildTimeMs = System.currentTimeMillis() - start;
    }

    private void indexEnglish(Document doc) {
        List<String> rawTokens = englishProc.tokenizeOnly(doc.getContent());
        for (int pos = 0; pos < rawTokens.size(); pos++) {
            String raw = rawTokens.get(pos);
            totalWords++;
            if (englishProc.isStopWord(raw)) continue;
            String stemmed = englishProc.stem(raw);
            if (stemmed.length() > 1) {
                englishIndex.addTerm(stemmed, doc.getId(), pos);
            }
        }
    }

    private void indexArabic(Document doc) {
        List<String> rawTokens = arabicProc.tokenize(doc.getContent());
        for (int pos = 0; pos < rawTokens.size(); pos++) {
            String raw = rawTokens.get(pos);
            totalWords++;
            if (arabicProc.isStopWord(raw)) continue;
            String stemmed = arabicProc.stem(raw);
            if (stemmed.length() >= 2) {
                arabicIndex.addTerm(stemmed, doc.getId(), pos);
            }
        }
    }


     // Process a raw query term through appropriate pipeline.
    public String processTerm(String term, boolean arabic) {
        if (arabic) {
            String norm = arabicProc.normalize(term);
            return arabicProc.stem(norm);
        } else {
            return englishProc.stem(term.toLowerCase());
        }
    }


     // Boolean keyword search (AND by default).
    public List<SearchResult> keywordSearch(String query) {
        long start = System.currentTimeMillis();
        boolean arabic = LanguageDetector.isArabic(query);
        PositionalIndex idx = arabic ? arabicIndex : englishIndex;

        List<String> terms = processQuery(query, arabic);
        if (terms.isEmpty()) return Collections.emptyList();

        Set<Integer> docIds = idx.andQuery(terms);
        return buildResults(docIds, 0.5, query, System.currentTimeMillis() - start);
    }


    // Phrase search: exact word order.
    public List<SearchResult> phraseSearch(String query) {
        boolean arabic = LanguageDetector.isArabic(query);
        PositionalIndex idx = arabic ? arabicIndex : englishIndex;

        List<String> terms = processQuery(query, arabic);
        if (terms.isEmpty()) return Collections.emptyList();

        Set<Integer> docIds = idx.phraseQuery(terms);
        return buildResults(docIds, 1.0, query, 0);
    }


    //  Proximity search: term1 /k term2
    public List<SearchResult> proximitySearch(String term1, String term2, int k) {
        boolean arabic = LanguageDetector.isArabic(term1 + term2);
        PositionalIndex idx = arabic ? arabicIndex : englishIndex;

        String t1 = processTerm(term1, arabic);
        String t2 = processTerm(term2, arabic);

        Set<Integer> docIds = idx.proximityQuery(t1, t2, k);
        return buildResults(docIds, 0.8, term1 + " /" + k + " " + term2, 0);
    }


     // Wildcard search: comput* or جامع*
    public List<SearchResult> wildcardSearch(String pattern) {
        boolean arabic = LanguageDetector.isArabic(pattern);
        PositionalIndex idx = arabic ? arabicIndex : englishIndex;
        KGramIndex kgram = arabic ? arabicKgram : englishKgram;

        // Process the non-wildcard part
        String stemmedPattern = pattern.replace("*", "");
        stemmedPattern = processTerm(stemmedPattern, arabic) + "*";

        Set<String> matchingTerms = kgram.wildcardQuery(stemmedPattern);

        // Collect all docs containing any matching term
        Set<Integer> docIds = new HashSet<>();
        for (String term : matchingTerms) {
            docIds.addAll(idx.getDocIds(term));
        }
        return buildResults(docIds, 0.6, pattern, 0);
    }


     // Ranked retrieval using TF-IDF + Cosine Similarity.
    public List<SearchResult> rankedSearch(String query) {
        long start = System.currentTimeMillis();
        boolean arabic = LanguageDetector.isArabic(query);
        VectorSpaceModel vsm = arabic ? arabicVSM : englishVSM;

        List<String> terms = processQuery(query, arabic);
        if (terms.isEmpty()) return Collections.emptyList();

        List<Map.Entry<Integer, Double>> ranked = vsm.rankedQuery(terms);
        List<SearchResult> results = new ArrayList<>();
        long elapsed = System.currentTimeMillis() - start;

        for (Map.Entry<Integer, Double> entry : ranked) {
            Document doc = docMap.get(entry.getKey());
            if (doc != null && entry.getValue() > 0) {
                results.add(new SearchResult(doc, entry.getValue(),
                    generateSnippet(doc.getContent(), terms, arabic, elapsed)));
            }
        }
        return results;
    }


     // Spelling correction: returns suggestions for unknown terms.
    public List<String> getSpellingSuggestions(String term) {
        boolean arabic = LanguageDetector.isArabic(term);
        PositionalIndex idx = arabic ? arabicIndex : englishIndex;
        SpellingCorrector corrector = arabic ? arabicCorrector : englishCorrector;

        String processed = processTerm(term, arabic);
        if (idx.containsTerm(processed)) return Collections.emptyList();
        return corrector.getSuggestions(processed, 5);
    }

    private List<String> processQuery(String query, boolean arabic) {
        if (arabic) return arabicProc.process(query);
        else return englishProc.process(query);
    }

    private List<SearchResult> buildResults(Set<Integer> docIds, double defaultScore, String query, long elapsed) {
        List<SearchResult> results = new ArrayList<>();
        boolean arabic = LanguageDetector.isArabic(query);
        List<String> terms = processQuery(query, arabic);

        for (int docId : docIds) {
            Document doc = docMap.get(docId);
            if (doc != null) {
                results.add(new SearchResult(doc, defaultScore,
                    generateSnippet(doc.getContent(), terms, arabic, elapsed)));
            }
        }
        return results;
    }

    private String generateSnippet(String content, List<String> terms, boolean arabic, long elapsed) {
        if (content == null || content.isEmpty()) return "";
        int snippetLen = 200;
        String lower = content.toLowerCase();

        // Try to find a relevant position
        int bestPos = 0;
        for (String term : terms) {
            int idx2 = lower.indexOf(term);
            if (idx2 != -1) { bestPos = Math.max(0, idx2 - 50); break; }
        }
        int end = Math.min(bestPos + snippetLen, content.length());
        String snippet = content.substring(bestPos, end).trim();
        if (bestPos > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";
        return snippet;
    }

    // Getters
    public List<Document> getDocuments() { return Collections.unmodifiableList(documents); }
    public Document getDocument(int id) { return docMap.get(id); }
    public PositionalIndex getEnglishIndex() { return englishIndex; }
    public PositionalIndex getArabicIndex() { return arabicIndex; }
    public long getIndexBuildTimeMs() { return indexBuildTimeMs; }
    public int getTotalWords() { return totalWords; }
    public int getEnglishDocCount() { return (int) documents.stream().filter(d -> !d.isArabic()).count(); }
    public int getArabicDocCount() { return (int) documents.stream().filter(Document::isArabic).count(); }
    public VectorSpaceModel getEnglishVSM() { return englishVSM; }
    public VectorSpaceModel getArabicVSM() { return arabicVSM; }
}
