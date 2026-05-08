package engine;

import java.util.*;


public class ArabicProcessor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "في","من","إلى","على","عن","مع","هذا","هذه","ذلك","تلك",
        "التي","الذي","الذين","اللاتي","اللواتي","كان","كانت","يكون",
        "هو","هي","هم","هن","أنا","نحن","أنت","أنتم","ما","لا","إن",
        "أن","قد","لم","لن","ال","و","أو","ثم","حتى","لكن","بل",
        "إذا","لو","بعد","قبل","فوق","تحت","بين","عند","منذ","خلال",
        "حول","ضد","رغم","بسبب","نحو","مثل","غير","سوى","فقط",
        "كل","بعض","جميع","معظم","أكثر","أقل","جدا","أيضا","دائما",
        "أحيانا","هنا","هناك","الآن","اليوم","أمس","غدا","كيف","لماذا",
        "متى","أين","من","ماذا","أي","كم","إذ","إذن","لقد","سوف"
    )); // hashSet search O(1)

    // Arabic prefixes and suffixes for light stemming
    private static final String[] PREFIXES_3 = {"ال", "بال", "كال", "فال", "لل"};
    private static final String[] PREFIXES_2 = {"ال", "وا", "فا", "بي", "لي"};
    private static final String[] PREFIXES_1 = {"و", "ف", "ب", "ل", "ك", "س"};
    private static final String[] SUFFIXES_3 = {"تها", "تهم", "تهن", "ونه", "انه", "اته", "ينه"};
    private static final String[] SUFFIXES_2 = {"ون", "ين", "ان", "ات", "تم", "تن", "هم", "هن", "نا", "ها", "وا", "ية", "يا"};
    private static final String[] SUFFIXES_1 = {"ة", "ه", "ي", "ك", "ن", "ت", "ا"};


    public String normalize(String text) {
        if (text == null) return "";
        text = text.replaceAll("[آأإٱ]", "ا");
        text = text.replaceAll("ى", "ي");
        text = text.replaceAll("ة", "ه");
        text = text.replaceAll("[\u064B-\u065F\u0670]", "");
        text = text.replaceAll("\u0640", "");
        text = text.replaceAll("[^\\u0600-\\u06FF\\s]", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    public List<String> tokenize(String text) {
        String normalized = normalize(text);
        String[] parts = normalized.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.length() >= 2) tokens.add(part); // ignore small words
        }
        return tokens;
    }


    public boolean isStopWord(String token) {
        return STOP_WORDS.contains(token);
    }


    public String stem(String word) {
        if (word == null || word.length() <= 2) return word;

        String stem = word;

        // Step 1: Remove 3-char prefixes
        for (String prefix : PREFIXES_3) {
            if (stem.startsWith(prefix) && stem.length() - prefix.length() >= 3) {
                stem = stem.substring(prefix.length());
                break;
            }
        }

        // Step 2: Remove 2-char prefixes
        for (String prefix : PREFIXES_2) {
            if (stem.startsWith(prefix) && stem.length() - prefix.length() >= 3) {
                stem = stem.substring(prefix.length());
                break;
            }
        }

        // Step 3: Remove 1-char prefixes
        for (String prefix : PREFIXES_1) {
            if (stem.startsWith(prefix) && stem.length() - prefix.length() >= 3) {
                stem = stem.substring(prefix.length());
                break;
            }
        }

        // Step 4: Remove 3-char suffixes
        for (String suffix : SUFFIXES_3) {
            if (stem.endsWith(suffix) && stem.length() - suffix.length() >= 3) {
                stem = stem.substring(0, stem.length() - suffix.length());
                break;
            }
        }

        // Step 5: Remove 2-char suffixes
        for (String suffix : SUFFIXES_2) {
            if (stem.endsWith(suffix) && stem.length() - suffix.length() >= 3) {
                stem = stem.substring(0, stem.length() - suffix.length());
                break;
            }
        }

        // Step 6: Remove 1-char suffixes
        for (String suffix : SUFFIXES_1) {
            if (stem.endsWith(suffix) && stem.length() - suffix.length() >= 2) {
                stem = stem.substring(0, stem.length() - suffix.length());
                break;
            }
        }

        return stem.length() >= 2 ? stem : word;
    }

    // normalize  tokenize  remove stop words  stem
    public List<String> process(String text) {
        List<String> tokens = tokenize(text);
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (!isStopWord(token)) {
                result.add(stem(token));
            }
        }
        return result;
    }

    public Set<String> getStopWords() {
        return Collections.unmodifiableSet(STOP_WORDS);
    } // readc stopping words from any were
}
