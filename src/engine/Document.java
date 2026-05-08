package engine;


public class Document {
    private final int id;
    private final String filePath;
    private final String title;
    private final String content;
    private final boolean arabic;

    public Document(int id, String filePath, String title, String content, boolean arabic) {
        this.id = id;
        this.filePath = filePath;
        this.title = title;
        this.content = content;
        this.arabic = arabic;
    }

    public int getId() { return id; }
    public String getFilePath() { return filePath; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isArabic() { return arabic; }

    @Override
    public String toString() {
        return "[" + (arabic ? "AR" : "EN") + "] " + title;
    }
}
