package engine;
import java.io.File;
import org.apache.pdfbox.Loader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class DocumentLoader {

    public static Map<String, String> loadDocs(String folderPath) {
        Map<String, String> docs = new HashMap<>();
        File folder = new File(folderPath);

        for (File file : folder.listFiles()) {
            String content = "";

            if (file.getName().endsWith(".pdf")) {
                content = readPDF(file);
            } else if (file.getName().endsWith(".txt")) {
                try {
                    content = new String(
                            Files.readAllBytes(file.toPath()),
                            StandardCharsets.UTF_8
                    );
                } catch (IOException e) {
                    System.err.println("Error reading: " + file.getName());
                }
            }

            if (!content.isEmpty()) {
                docs.put(file.getName(), content);
            }
        }
        return docs;
    }

    private static String readPDF(File file) {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (IOException e) {
            System.err.println("Error reading PDF: " + file.getName());
            return "";
        }
    }
}