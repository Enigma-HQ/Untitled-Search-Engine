package engine;

import java.util.*;

public class EnglishProcessor {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "a","an","the","and","or","but","in","on","at","to","for","of","with",
        "by","from","as","is","was","are","were","be","been","being","have",
        "has","had","do","does","did","will","would","could","should","may",
        "might","shall","can","need","dare","ought","used","that","which",
        "who","whom","whose","what","when","where","why","how","this","these",
        "those","it","its","i","me","my","we","our","you","your","he","him",
        "his","she","her","they","them","their","not","no","nor","so","yet",
        "both","either","neither","each","every","all","any","few","more",
        "most","other","some","such","only","same","than","too","very","just",
        "if","then","else","while","although","because","since","unless",
        "until","after","before","during","about","above","below","between",
        "into","through","against","among","throughout","despite","however",
        "therefore","moreover","furthermore","consequently","nevertheless"
    ));

    private final PorterStemmer stemmer = new PorterStemmer();

//    Tokenize lowercase + extract alphabetic tokens only.

    public List<String> tokenize(String text) {

        if (text == null) return new ArrayList<>(); // Return empty list if text is null
        List<String> tokens = new ArrayList<>();
        String lower = text.toLowerCase(); // Convert text to lowercase
        StringBuilder sb = new StringBuilder(); // Build words character by character
        for (char c : lower.toCharArray()) { // Handles punctuation better than simple splitting
            if (Character.isLetter(c)) {
                sb.append(c);
            } else if (sb.length() > 0) { // Add completed word to tokens
                tokens.add(sb.toString());
                sb.setLength(0); // sb = ""
            }
        }
        // Add the last word if exists
        if (sb.length() > 0) tokens.add(sb.toString());
        return tokens;
    }

    public boolean isStopWord(String token) {
        return STOP_WORDS.contains(token.toLowerCase());
    }

    public String stem(String word) {
        return stemmer.stem(word.toLowerCase());
    }


//     tokenize   remove stop words   stem
    public List<String> process(String text) {
        List<String> tokens = tokenize(text);
        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() > 1 && !isStopWord(token)) {
                result.add(stem(token));
            }
        }
        return result;
    }

//      Tokenize only (no stop-word removal / stemming) - used for positional index

    public List<String> tokenizeOnly(String text) {
        return tokenize(text);
    }

    public Set<String> getStopWords() {
        return Collections.unmodifiableSet(STOP_WORDS);
    }
}
