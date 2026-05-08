# Bilingual Information Retrieval Search Engine
## English + Arabic | JavaFX GUI

---

## 📁 Project Structure

```
src/
├── engine/
│   ├── Document.java           → Corpus document model
│   ├── LanguageDetector.java   → Arabic vs English detector
│   ├── EnglishProcessor.java   → Tokenizer + stop words + Porter Stemmer
│   ├── ArabicProcessor.java    → Normalizer + stop words + ISRI Stemmer
│   ├── PorterStemmer.java      → Full Porter Stemmer implementation
│   ├── PositionalIndex.java    → Positional Inverted Index
│   ├── KGramIndex.java         → K-gram index (wildcards + spelling)
│   ├── VectorSpaceModel.java   → TF-IDF + Cosine Similarity
│   ├── SpellingCorrector.java  → Levenshtein + Jaccard suggestions
│   ├── SearchEngine.java       → Main engine (load + index + query)
│   ├── SearchResult.java       → Result model
│   └── Evaluator.java          → Precision & Recall evaluation
│
├── ui/
│   ├── MainApp.java            → JavaFX GUI Application
│   └── styles.css              → Dark theme stylesheet
│
├── data/
│   ├── english/                → *.txt English documents (UTF-8)
│   └── arabic/                 → *.txt Arabic documents (UTF-8)
│
├── pom.xml                     → Maven build file
├── module-info.java            → Java module descriptor
├── run.bat                     → Windows build & run script
└── run.sh                      → Linux/Mac build & run script
```

---

## ⚙️ Requirements

| Tool        | Version    |
|-------------|------------|
| Java JDK    | 17+        |
| JavaFX SDK  | 21+        |
| Maven       | 3.8+ (optional) |

---

## 🚀 How to Run

### Option 1: Using the Script

**Windows:**
1. Edit `run.bat` and set `JAVA_HOME` and `JAVAFX_LIB`
2. Double-click `run.bat` or run from CMD

**Linux/Mac:**
```bash
chmod +x run.sh
# Edit run.sh to set JAVA_HOME and JAVAFX_LIB
./run.sh
```

### Option 2: Using Maven
```bash
cd src
mvn javafx:run
```

### Option 3: IntelliJ IDEA (Recommended)
1. Open the `src/` folder as a project
2. Go to **File → Project Structure → Libraries**
3. Add JavaFX SDK lib folder
4. Add VM Options to run config:
   ```
   --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml
   ```
5. Run `ui.MainApp`

---

## 📄 Adding Documents

Place `.txt` files (UTF-8 encoded) in:
- `src/data/english/` for English documents
- `src/data/arabic/` for Arabic documents

Minimum corpus: **10 English + 10 Arabic** documents, **10,000+ total words**

You can also change the corpus paths at runtime via the **Settings** tab in the GUI.

---

## 🔍 Supported Query Types

| Type | Example | Description |
|------|---------|-------------|
| Ranked (TF-IDF) | `information retrieval` | Cosine similarity ranking |
| Boolean AND | `machine learning` | Documents containing ALL terms |
| Phrase | `"natural language processing"` | Exact word sequence |
| Proximity | `employment /3 place` | Terms within k words |
| Wildcard | `comput*` or `جامع*` | Prefix/suffix matching |

---

## 📊 System Features

### English Pipeline
1. Tokenization (lowercase + alphabetic)
2. Stop-word removal (80+ words)
3. Porter Stemmer (maps variations → root)

### Arabic Pipeline
1. Normalization: آ أ إ → ا, ى → ي, remove Tashkeel
2. Stop-word removal (50+ Arabic stop words)
3. ISRI Light Stemmer (strips prefixes/suffixes)

### Indexing
- **Positional Inverted Index**: term → {docId → [positions]}
- **K-gram Index (bigrams)**: for wildcard queries and spelling correction

### Ranking
- **TF-IDF**: tf = 1 + log₁₀(freq), idf = log₁₀(N/df)
- **Cosine Similarity**: normalized dot product of query & doc vectors

### Spelling Correction
- K-gram **Jaccard Similarity** for candidate generation
- **Levenshtein Edit Distance** for final ranking
- "Did you mean?" suggestions shown in UI

### Evaluation
- **Precision** = relevant retrieved / total retrieved
- **Recall** = relevant retrieved / total relevant
- **F1** = 2 × P × R / (P + R)
- Index build time and query retrieval time are recorded

---

## 📈 Performance Notes

- Index build time: typically **50–200ms** for a 10,000-word corpus
- Query time: typically **< 5ms** for all query types
- VSM norms are precomputed once after indexing for fast cosine similarity

---

## 👤 Information Retrieval Course — Final Project
