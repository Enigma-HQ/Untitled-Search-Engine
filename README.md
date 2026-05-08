#  Bilingual Information Retrieval System
### English & Arabic Search Engine — Java 21 + JavaFX

---

##  Table of Contents
1. [Project Overview](#project-overview)
2. [Features](#features)
3. [Project Structure](#project-structure)
4. [System Architecture](#system-architecture)
5. [Class Documentation](#class-documentation)
6. [NLP Pipelines](#nlp-pipelines)
7. [Query Types](#query-types)
8. [Setup & Installation](#setup--installation)
9. [How to Add Your Corpus](#how-to-add-your-corpus)
10. [Running the Project](#running-the-project)
11. [Common Errors & Fixes](#common-errors--fixes)
12. [Performance & Complexity](#performance--complexity)
13. [Evaluation](#evaluation)

---

## Project Overview

A full-featured bilingual search engine that processes **English and Arabic** `.txt` documents. It goes beyond simple keyword matching by implementing:

- A **Positional Inverted Index** for phrase and proximity searching
- A **Vector Space Model (TF-IDF)** for relevance ranking
- **Language-specific NLP pipelines** for both English and Arabic
- A **K-gram Index** for wildcard queries and spelling correction
- A **JavaFX dark-themed GUI** with real-time search

---

## Features

| Feature | Description |
|---|---|
| **Ranked Search** | TF-IDF + Cosine Similarity, results sorted by relevance |
| **Boolean AND** | Returns documents containing all query terms |
| **Phrase Search** | Exact word-order matching via positional index |
| **Proximity /k** | Finds terms within k positions of each other |
| **Wildcard \*** | `comput*` or `جامع*` via K-gram index |
| **Spelling Correction** | "Did you mean?" using Levenshtein + Jaccard |
| **Arabic NLP** | Normalization, ISRI stemming, Arabic stop words |
| **English NLP** | Porter Stemmer, 80+ stop words |
| **Evaluation Tab** | Precision, Recall, F1, build time, query time |

---

## Project Structure

```
src/
├── engine/
│   ├── Document.java             # Corpus document model
│   ├── LanguageDetector.java     # Arabic vs English detection
│   ├── PorterStemmer.java        # Full Porter Stemmer (English)
│   ├── EnglishProcessor.java     # Tokenizer + stop words + stemming
│   ├── ArabicProcessor.java      # Normalizer + stop words + ISRI stemmer
│   ├── PositionalIndex.java      # Positional Inverted Index
│   ├── KGramIndex.java           # K-gram index (wildcards + spelling)
│   ├── VectorSpaceModel.java     # TF-IDF + Cosine Similarity
│   ├── SpellingCorrector.java    # Levenshtein + Jaccard suggestions
│   ├── SearchEngine.java         # Main facade / orchestrator
│   ├── SearchResult.java         # Result model (doc + score + snippet)
│   └── Evaluator.java            # Precision, Recall, F1
├── ui/
│   ├── MainApp.java              # JavaFX Application
│   └── styles.css                # Dark theme stylesheet
├── data/
│   ├── english/                  # ← Put English .txt files here
│   └── arabic/                   # ← Put Arabic .txt files here
├── pom.xml                       # Maven build file
├── run.bat                       # Windows quick-run script
└── run.sh                        # Linux/Mac quick-run script
```

---

## System Architecture

```
┌─────────────────────────────────────────────┐
│             JavaFX UI (MainApp)             │
│  Search Bar | Query Type | Results | Eval  │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│          SearchEngine  (Facade)             │
│  loadCorpus() | buildIndex() | search()    │
└────┬──────────────────────────┬─────────────┘
     │ English docs             │ Arabic docs
┌────▼────────────┐    ┌────────▼────────────┐
│ EnglishProcessor│    │  ArabicProcessor    │
│ PorterStemmer   │    │  ISRI Stemmer       │
└────┬────────────┘    └────────┬────────────┘
     └──────────────┬───────────┘
┌─────────────────────────────────────────────┐
│  PositionalIndex | KGramIndex              │
│  VectorSpaceModel | SpellingCorrector      │
└─────────────────────────────────────────────┘
```

**Flow:**
1. Load `.txt` files → create `Document` objects
2. Detect language per document
3. Route through English or Arabic NLP pipeline
4. Add `(stemmed_term, docId, position)` to `PositionalIndex`
5. Build `KGramIndex` from vocabulary
6. Precompute TF-IDF norms in `VectorSpaceModel`
7. On query → same pipeline → consult index → return ranked results

---

## Class Documentation

### `Document.java`
Simple immutable model for one corpus document.

| Field | Type | Description |
|---|---|---|
| `id` | `int` | Unique sequential ID |
| `filePath` | `String` | Absolute path to `.txt` file |
| `title` | `String` | Filename without extension |
| `content` | `String` | Full raw text (UTF-8) |
| `arabic` | `boolean` | `true` = Arabic, `false` = English |

---

### `LanguageDetector.java`
Detects language by counting Unicode codepoints. Arabic = U+0600–U+06FF. Returns Arabic if Arabic chars > Latin letters.

- **Time:** O(n) where n = string length
- **Space:** O(1)

---

### `PorterStemmer.java`
Full implementation of the Porter Stemming Algorithm (1980). Reduces English words to their morphological root through 6 sequential suffix-stripping steps.

```
"running"       → "run"
"universities"  → "univers"
"computational" → "comput"
```

| Step | What it removes | Example |
|---|---|---|
| 1a | Plurals: -sses, -ies, -s | caresses → caress |
| 1b | Past/progressive: -ed, -ing | matting → mat |
| 1c | y → i when vowel in stem | happy → happi |
| 2 | Derivational: -ational, -enci | relational → relate |
| 3 | More derivational: -icate, -ness | electriciti → electr |
| 4 | Residual: -al, -ance, -er | revival → reviv |
| 5a | Trailing -e if m > 1 | probate → probat |
| 5b | Double consonant l | controll → control |

- **Time:** O(L) — L = word length
- **Space:** O(L)

---

### `EnglishProcessor.java`
Orchestrates the full English NLP pipeline.

| Method | Description | Complexity |
|---|---|---|
| `tokenize(text)` | Lowercase + extract alphabetic tokens | O(n) |
| `isStopWord(token)` | HashSet lookup | O(1) |
| `stem(word)` | Delegates to PorterStemmer | O(L) |
| `process(text)` | tokenize → filter stop words → stem | O(n·L) |
| `tokenizeOnly(text)` | Tokenize only, no filtering (for positional index) | O(n) |

Stop words: `a, an, the, and, or, but, in, on, at, to, for, of, with, by, is, was, are...` (80+ words)

---

### `ArabicProcessor.java`
Full Arabic NLP pipeline: normalization → stop words → ISRI light stemmer.

**Normalization:**
```
آ أ إ ٱ  →  ا   (unify Alif variants)
ى        →  ي   (unify Ya)
ة        →  ه   (unify Ta Marbuta)
Remove Tashkeel (diacritics)  U+064B–U+065F
Remove Tatweel (kashida)      U+0640
```

**ISRI Light Stemmer — prefix/suffix stripping:**

| Step | Strips | Example |
|---|---|---|
| 3-char prefixes | بال كال فال | بالجامعة → جامعة |
| 2-char prefixes | ال وا فا | الكتاب → كتاب |
| 1-char prefixes | و ف ب ل | وذهب → ذهب |
| 3-char suffixes | تها تهم تهن | كتبتهم → كتب |
| 2-char suffixes | ون ين ان ات | المعلمون → معلم |
| 1-char suffixes | ة ه ي ك | جامعة → جامع |

- **Time:** O(n·P) ≈ O(n)
- **Space:** O(n)

---

### `PositionalIndex.java`
The core index data structure.

```
Map<String, Map<Integer, List<Integer>>>
       │              │           └── positions [2, 7, 45...]
       │              └── docId
       └── stemmed_term
```

| Method | Algorithm | Complexity |
|---|---|---|
| `addTerm(term, docId, pos)` | HashMap put | O(1) amortized |
| `getDocIds(term)` | HashMap lookup | O(1) |
| `df(term)` | HashMap + size() | O(1) |
| `tf(term, docId)` | Two lookups | O(1) |
| `phraseQuery(terms)` | Intersect → check consecutive positions | O(d·p) |
| `proximityQuery(t1,t2,k)` | Intersect → pairwise distance | O(d·p²) |
| `andQuery(terms)` | Progressive intersection | O(t·d) |
| `orQuery(terms)` | Union of posting lists | O(t·d) |

- **Space:** O(N) — N = total term occurrences in corpus

---

### `KGramIndex.java`
Splits vocabulary terms into overlapping bigrams with `$` padding.

```
"cat"   →  "$cat$"   →  { $c, ca, at, t$ }
"كتاب"  →  "$كتاب$"  →  { $ك, كت, تا, اب, ب$ }
```

**Wildcard query:** generate k-grams from pattern parts → intersect candidate sets → regex post-filter.

**Jaccard Similarity:** `J(A,B) = |A∩B| / |A∪B|` on k-gram sets.

| Method | Complexity |
|---|---|
| `addTerm(term)` | O(L) |
| `wildcardQuery(pattern)` | O(V·L) |
| `jaccardSimilarity(a, b)` | O(L) |

---

### `VectorSpaceModel.java`
TF-IDF weighted Vector Space Model with Cosine Similarity.

```
TF weight:  tf_w(t,d) = 1 + log₁₀(tf(t,d))
IDF weight: idf(t)    = log₁₀(N / df(t))
TF-IDF:     w(t,d)    = tf_w × idf

Cosine:  score(q,d) = (q⃗ · d⃗) / (‖q⃗‖ × ‖d⃗‖)
```

Document norms `‖d⃗‖` are **precomputed once** after indexing and cached.

| Method | Complexity |
|---|---|
| `precomputeNorms(docIds)` | O(V·D) — called once |
| `rankedQuery(terms)` | O(t·d + D log D) |
| `tfidf(term, docId)` | O(1) |

---

### `SpellingCorrector.java`
Two-phase "Did you mean?" suggestions.

1. **Phase 1 (Jaccard):** filter vocabulary to candidates with Jaccard > 0.1
2. **Phase 2 (Levenshtein):** sort candidates by edit distance, return top N

```
Levenshtein DP:
dp[i][j] = min(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1]+cost)
```

| Method | Complexity |
|---|---|
| `editDistance(s1, s2)` | O(m·n) |
| `getSuggestions(term, N)` | O(V·L + K·m·n) |

---

### `SearchEngine.java`
Main facade — the UI only ever calls this class.

| Method | Description | Complexity |
|---|---|---|
| `loadCorpus(engDir, arDir)` | Read all `.txt` files | O(C) |
| `buildIndex()` | Full indexing pipeline | O(N·L) |
| `rankedSearch(query)` | TF-IDF cosine ranking | O(t·d + D log D) |
| `keywordSearch(query)` | Boolean AND | O(t·d) |
| `phraseSearch(query)` | Exact phrase | O(d·p) |
| `proximitySearch(t1,t2,k)` | Proximity /k | O(d·p²) |
| `wildcardSearch(pattern)` | Wildcard expansion | O(V·L) |
| `getSpellingSuggestions(t)` | Spelling correction | O(V·L + K·mn) |

> Documents and queries go through the **exact same NLP pipeline** — guaranteeing symmetric matching.

---

### `Evaluator.java`

```
Precision = |Relevant ∩ Retrieved| / |Retrieved|
Recall    = |Relevant ∩ Retrieved| / |Relevant|
F1        = 2 × P × R / (P + R)
```

- **Time:** O(R) — R = retrieved results

---

### `MainApp.java` (UI)
JavaFX app. Engine calls run on a background thread, UI updates via `Platform.runLater()`.

**Tabs:** Results · Index Info · Evaluation · Settings

---

## NLP Pipelines

### English
```
Raw text → tokenize() → remove stop words → Porter stem → index
```

### Arabic
```
Raw text → normalize() → tokenize() → remove stop words → ISRI stem → index
```

---

## Query Types

| Type | Example | How it works |
|---|---|---|
| **Ranked** | `information retrieval` | TF-IDF vectors → cosine similarity → sorted |
| **Keyword AND** | `machine learning` | Posting list intersection |
| **Phrase** | `natural language processing` | Consecutive position check |
| **Proximity** | term1=`job` k=`3` term2=`work` | `|pos1 - pos2| ≤ k` |
| **Wildcard** | `comput*` / `جامع*` | K-gram expansion → posting union |

Spelling correction triggers automatically on 0 results.

---

## Setup & Installation

### Requirements

| Tool | Version | Link |
|---|---|---|
| Java JDK | 17 or 21+ | https://adoptium.net |
| JavaFX SDK | 21+ | https://gluonhq.com/products/javafx |
| IntelliJ IDEA | 2023+ | https://www.jetbrains.com/idea/download |

### IntelliJ Setup

```
1. File → Open → select src/ folder
2. File → Project Structure → Project → set SDK to JDK 21
3. File → Project Structure → Libraries → + → Java
        → select: C:\javafx-sdk-21.0.11\lib
4. Run → Edit Configurations → + → Application
        Main class: ui.MainApp
        VM options: --module-path "C:\javafx-sdk-21.0.11\lib"
                    --add-modules javafx.controls,javafx.fxml
5. Delete any module-info.java file
6. Build → Rebuild Project → Run
```

---

## How to Add Your Corpus

```
src/data/english/    ←  doc1.txt, doc2.txt, ...  (min 10 files)
src/data/arabic/     ←  doc1.txt, doc2.txt, ...  (min 10 files)
```

- Extension must be `.txt`
- Encoding must be **UTF-8** (critical for Arabic)
- Minimum: 10 English + 10 Arabic, total ≥ 10,000 words
- Or change paths at runtime via **Settings tab → Browse → Reload Engine**

---

## Running the Project

```bat
# Windows
run.bat
```
```bash
# Linux / Mac
chmod +x run.sh && ./run.sh
```
```bash
# Maven
cd src && mvn javafx:run
```

---

## Common Errors & Fixes

| Error | Cause | Fix |
|---|---|---|
| `duplicate module` | `module-info.java` in project | Delete all `module-info.java` files |
| `Module javafx.controls not found` | VM options path wrong | Match path to actual folder name (e.g. `21.0.11`) |
| `No documents found` | Wrong corpus path or empty folder | Check Settings tab |
| Arabic text garbled | File not UTF-8 | Re-save with UTF-8 encoding |
| `NullPointerException` on search | Engine still loading | Wait for "Engine ready ✅" in status bar |

---

## Performance & Complexity

| Operation | Complexity | Typical Time |
|---|---|---|
| Corpus loading | O(C) | 10–50 ms |
| Index building | O(N·L) | 50–150 ms |
| Ranked query | O(t·d + D log D) | < 5 ms |
| Boolean AND | O(t·d) | < 2 ms |
| Phrase query | O(d·p) | < 3 ms |
| Proximity query | O(d·p²) | < 5 ms |
| Wildcard query | O(V·L) | < 10 ms |
| Spelling correction | O(V·L + K·mn) | 10–50 ms |

> C=corpus size, N=total tokens, L=avg word length, V=vocab size, D=doc count, t=query terms, d=matching docs, p=positions per doc

---

## Evaluation

**Evaluation tab** reports:
- Precision, Recall, F1 per query
- Index build time (ms)
- Per-query retrieval time (ms)

**Sample test queries:**

| # | Query | Language | Type |
|---|---|---|---|
| 1 | `information retrieval` | English | Ranked |
| 2 | `machine learning` | English | Ranked |
| 3 | `natural language` | English | Phrase |
| 4 | `البحث العلمي` | Arabic | Ranked |
| 5 | `جامع*` | Arabic | Wildcard |

---

## Class Dependency Map

```
MainApp (UI)
    └── SearchEngine
             ├── EnglishProcessor → PorterStemmer
             ├── ArabicProcessor
             ├── LanguageDetector
             ├── PositionalIndex  ←── VectorSpaceModel
             ├── KGramIndex       ←── SpellingCorrector
             ├── Document
             └── SearchResult

Evaluator  (standalone utility)
```

No circular dependencies. Strictly layered architecture.

---

*Information Retrieval Course — Final Project*
