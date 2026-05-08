package engine;


public class LanguageDetector {


    // Returns true if the text is predominantly Arabic.
    public static boolean isArabic(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        int arabicCount = 0, latinCount = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u0600' && c <= '\u06FF') arabicCount++;
            else if (Character.isLetter(c)) latinCount++;
        }
        return arabicCount > latinCount;
    }

    public static String detect(String text) {
        return isArabic(text) ? "Arabic" : "English";
    }
}
