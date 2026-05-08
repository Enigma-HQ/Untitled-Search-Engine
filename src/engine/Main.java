package engine;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Map<String, String> docs = DocumentLoader.loadDocs("D:/IR");

        System.out.println("عدد الوثائق: " + docs.size());

        for (Map.Entry<String, String> entry : docs.entrySet()) {
            System.out.println("=== " + entry.getKey() + " ===");
            System.out.println(entry.getValue().substring(0, 200));
            System.out.println();
        }
    }
}