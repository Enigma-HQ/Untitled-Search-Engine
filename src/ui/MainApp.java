package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import engine.*;

import java.io.File;
import java.util.*;


public class MainApp extends Application {

    private SearchEngine engine;
    private Stage primaryStage;

    // UI Components
    private TextField searchField;
    private ComboBox<String> queryTypeBox;
    private TextField proximityK;
    private TextField proximityTerm2;
    private VBox resultsPane;
    private Label statusLabel;
    private Label statsLabel;
    private TextArea indexInfoArea;
    private TabPane mainTabs;
    private ProgressIndicator loadingIndicator;

    // Corpus paths (can be changed by user)
    private String englishCorpusPath = "src/data/english";
    private String arabicCorpusPath = "src/data/arabic";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Enigma");
        stage.setMinWidth(900);
        stage.setMinHeight(700);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f1117;");

        // Top: Header + controls
        root.setTop(buildHeader());

        // Center: Tabs
        mainTabs = buildMainTabs();
        root.setCenter(mainTabs);

        // Bottom: Status bar
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1100, 750);
        applyStyles(scene);
        stage.setScene(scene);
        stage.show();

        // Auto-load engine
        loadEngineAsync();
    }

    private VBox buildHeader() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(20, 30, 15, 30));
        header.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        // Title
        Label title = new Label("Enigma Search Engine");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label subtitle = new Label("support English / Arabic");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b949e;");

        // Search row
        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("Enter query in English or Arabic... (supports wildcard *)");
        searchField.setPrefWidth(500);
        searchField.setStyle(inputStyle());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        queryTypeBox = new ComboBox<>();
        queryTypeBox.getItems().addAll(
            "Ranked (TF-IDF)", "Keyword (Boolean AND)",
            "Phrase Search", "Wildcard (*)", "Proximity (/k)"
        );
        queryTypeBox.setValue("Ranked (TF-IDF)");
        queryTypeBox.setStyle(comboStyle());
        queryTypeBox.setPrefWidth(170);

        Button searchBtn = new Button("Search");
        searchBtn.setStyle(primaryBtnStyle());
        searchBtn.setOnAction(e -> performSearch());
        searchField.setOnAction(e -> performSearch());

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(24, 24);
        loadingIndicator.setVisible(false);
        loadingIndicator.setStyle("-fx-progress-color: #58a6ff;");

        searchRow.getChildren().addAll(searchField, queryTypeBox, searchBtn, loadingIndicator);

        // Proximity options (shown conditionally)
        HBox proximityRow = new HBox(10);
        proximityRow.setAlignment(Pos.CENTER_LEFT);
        Label kLabel = new Label("k =");
        kLabel.setStyle("-fx-text-fill: #8b949e;");
        proximityK = new TextField("3");
        proximityK.setPrefWidth(60);
        proximityK.setStyle(inputStyle());
        Label term2Label = new Label("Term 2:");
        term2Label.setStyle("-fx-text-fill: #8b949e;");
        proximityTerm2 = new TextField();
        proximityTerm2.setPromptText("Second term");
        proximityTerm2.setPrefWidth(180);
        proximityTerm2.setStyle(inputStyle());
        proximityRow.getChildren().addAll(kLabel, proximityK, term2Label, proximityTerm2);
        proximityRow.setVisible(false);
        proximityRow.setManaged(false);

        queryTypeBox.setOnAction(e -> {
            boolean isProx = queryTypeBox.getValue().contains("Proximity");
            proximityRow.setVisible(isProx);
            proximityRow.setManaged(isProx);
            if (isProx) searchField.setPromptText("First term...");
            else searchField.setPromptText("Enter query in English or Arabic... (supports wildcard *)");
        });

        header.getChildren().addAll(title, subtitle, searchRow, proximityRow);
        return header;
    }

    private TabPane buildMainTabs() {
        TabPane tabs = new TabPane();
        tabs.setStyle("-fx-background-color: #0f1117; -fx-tab-min-width: 120px;");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Results tab
        Tab resultsTab = new Tab("📄 Results");
        ScrollPane scroll = new ScrollPane();
        scroll.setStyle("-fx-background: #0f1117; -fx-background-color: #0f1117;");
        scroll.setFitToWidth(true);
        resultsPane = new VBox(12);
        resultsPane.setPadding(new Insets(20, 30, 20, 30));
        resultsPane.setStyle("-fx-background-color: #0f1117;");
        showWelcomeMessage();
        scroll.setContent(resultsPane);
        resultsTab.setContent(scroll);

        // Index Info tab
        Tab indexTab = new Tab("📊 Index Info");
        indexInfoArea = new TextArea();
        indexInfoArea.setEditable(false);
        indexInfoArea.setStyle("-fx-background-color: #161b22; -fx-text-fill: #c9d1d9; " +
            "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-control-inner-background: #161b22;");
        indexInfoArea.setText("Index not built yet. Loading...");
        indexTab.setContent(indexInfoArea);

        // Evaluation tab
        Tab evalTab = new Tab("📈 Evaluation");
        evalTab.setContent(buildEvalPanel());

        // Settings tab
        Tab settingsTab = new Tab("⚙ Settings");
        settingsTab.setContent(buildSettingsPanel());

        tabs.getTabs().addAll(resultsTab, indexTab, evalTab, settingsTab);
        return tabs;
    }

    private VBox buildEvalPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20, 30, 20, 30));
        panel.setStyle("-fx-background-color: #0f1117;");

        Label heading = new Label("Precision & Recall Evaluation");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label info = new Label(
            "Run predefined test queries and evaluate precision/recall.\n" +
            "Relevant documents are manually annotated for each query.");
        info.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        Button runEvalBtn = new Button("▶  Run Evaluation");
        runEvalBtn.setStyle(primaryBtnStyle());

        TextArea evalOutput = new TextArea();
        evalOutput.setEditable(false);
        evalOutput.setPrefHeight(400);
        evalOutput.setStyle("-fx-background-color: #161b22; -fx-text-fill: #c9d1d9; " +
            "-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-control-inner-background: #161b22;");
        evalOutput.setText("Click 'Run Evaluation' to run test queries...");

        runEvalBtn.setOnAction(e -> {
            if (engine == null) {
                evalOutput.setText("Engine not loaded yet.");
                return;
            }
            new Thread(() -> {
                String result = runEvaluation();
                Platform.runLater(() -> evalOutput.setText(result));
            }).start();
        });

        panel.getChildren().addAll(heading, info, runEvalBtn, evalOutput);
        return panel;
    }

    private VBox buildSettingsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20, 30, 20, 30));
        panel.setStyle("-fx-background-color: #0f1117;");

        Label heading = new Label("Corpus Settings");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        // English directory
        HBox engRow = buildDirRow("English Corpus:", englishCorpusPath, true);
        HBox arRow = buildDirRow("Arabic Corpus:", arabicCorpusPath, false);

        Button reloadBtn = new Button("🔄  Reload Engine");
        reloadBtn.setStyle(primaryBtnStyle());
        reloadBtn.setOnAction(e -> loadEngineAsync());

        Label note = new Label("Note: Place .txt files (UTF-8 encoded) in the selected directories.");
        note.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        panel.getChildren().addAll(heading, engRow, arRow, reloadBtn, note);
        return panel;
    }

    private HBox buildDirRow(String labelText, String defaultPath, boolean isEnglish) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-text-fill: #c9d1d9; -fx-min-width: 130px;");
        TextField pathField = new TextField(defaultPath);
        pathField.setPrefWidth(300);
        pathField.setStyle(inputStyle());
        Button browse = new Button("Browse");
        browse.setStyle(secondaryBtnStyle());
        browse.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File dir = dc.showDialog(primaryStage);
            if (dir != null) {
                pathField.setText(dir.getAbsolutePath());
                if (isEnglish) englishCorpusPath = dir.getAbsolutePath();
                else arabicCorpusPath = dir.getAbsolutePath();
            }
        });
        pathField.textProperty().addListener((obs, o, n) -> {
            if (isEnglish) englishCorpusPath = n;
            else arabicCorpusPath = n;
        });
        row.getChildren().addAll(lbl, pathField, browse);
        return row;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(20);
        bar.setPadding(new Insets(8, 20, 8, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Loading engine...");
        statusLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        statsLabel = new Label("");
        statsLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bar.getChildren().addAll(statusLabel, spacer, statsLabel);
        return bar;
    }

    private void loadEngineAsync() {
        setStatus("Loading corpus and building index...", true);
        new Thread(() -> {
            try {
                engine = new SearchEngine();
                engine.loadCorpus(englishCorpusPath, arabicCorpusPath);
                engine.buildIndex();
                Platform.runLater(() -> {
                    setStatus("Engine ready — " + engine.getDocuments().size() + " documents indexed", false);
                    statsLabel.setText("EN:" + engine.getEnglishDocCount() +
                        " docs | AR:" + engine.getArabicDocCount() + " docs | Words:" + engine.getTotalWords() +
                        " | Build time: " + engine.getIndexBuildTimeMs() + "ms");
                    updateIndexInfo();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setStatus("Error loading corpus: " + ex.getMessage(), false);
                    indexInfoArea.setText("Error: " + ex.getMessage() +
                        "\n\nMake sure corpus directories exist:\n" +
                        "  " + englishCorpusPath + "\n  " + arabicCorpusPath);
                });
            }
        }).start();
    }

    private void performSearch() {
        if (engine == null) {
            setStatus("Engine not ready yet, please wait...", false);
            return;
        }

        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        String qType = queryTypeBox.getValue();
        setStatus("Searching...", true);
        long startTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                List<SearchResult> results;
                String queryDesc = query;

                switch (qType) {
                    case "Phrase Search":
                        results = engine.phraseSearch(query);
                        queryDesc = "\"" + query + "\"";
                        break;
                    case "Wildcard (*)":
                        results = engine.wildcardSearch(query);
                        queryDesc = query + " (wildcard)";
                        break;
                    case "Proximity (/k)":
                        int k = 3;
                        try { k = Integer.parseInt(proximityK.getText().trim()); } catch (Exception ignored) {}
                        String term2 = proximityTerm2.getText().trim();
                        if (term2.isEmpty()) {
                            Platform.runLater(() -> setStatus("Please enter second term for proximity search.", false));
                            return;
                        }
                        results = engine.proximitySearch(query, term2, k);
                        queryDesc = query + " /" + k + " " + term2;
                        break;
                    case "Keyword (Boolean AND)":
                        results = engine.keywordSearch(query);
                        break;
                    default: // Ranked
                        results = engine.rankedSearch(query);
                        break;
                }

                // Spell check
                List<String> spellSuggestions = new ArrayList<>();
                if (results.isEmpty()) {
                    String[] words = query.split("\\s+");
                    for (String w : words) {
                        List<String> s = engine.getSpellingSuggestions(w);
                        spellSuggestions.addAll(s);
                    }
                }

                final List<SearchResult> finalResults = results;
                final List<String> finalSuggestions = spellSuggestions;
                final String finalDesc = queryDesc;
                final long elapsed = System.currentTimeMillis() - startTime;

                Platform.runLater(() -> {
                    displayResults(finalResults, finalDesc, finalSuggestions, elapsed);
                    setStatus("Found " + finalResults.size() + " result(s) in " + elapsed + "ms", false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> setStatus("Search error: " + ex.getMessage(), false));
            }
        }).start();
    }

    private void displayResults(List<SearchResult> results, String query, List<String> spellSugg, long ms) {
        resultsPane.getChildren().clear();

        // Query header
        HBox queryHeader = new HBox(10);
        queryHeader.setAlignment(Pos.CENTER_LEFT);
        Label queryLabel = new Label("Results for: ");
        queryLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");
        Label queryText = new Label(query);
        queryText.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label countLabel = new Label("— " + results.size() + " result(s) (" + ms + "ms)");
        countLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        queryHeader.getChildren().addAll(queryLabel, queryText, countLabel);
        resultsPane.getChildren().add(queryHeader);

        // Spell suggestions
        if (!spellSugg.isEmpty()) {
            HBox suggBox = new HBox(8);
            suggBox.setAlignment(Pos.CENTER_LEFT);
            Label didYouMean = new Label("Did you mean: ");
            didYouMean.setStyle("-fx-text-fill: #f0883e; -fx-font-size: 12px;");
            suggBox.getChildren().add(didYouMean);
            for (String sugg : spellSugg.subList(0, Math.min(3, spellSugg.size()))) {
                Button suggBtn = new Button(sugg);
                suggBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #58a6ff; " +
                    "-fx-cursor: hand; -fx-underline: true; -fx-font-size: 12px; -fx-padding: 0;");
                suggBtn.setOnAction(e -> {
                    searchField.setText(sugg);
                    performSearch();
                });
                suggBox.getChildren().add(suggBtn);
            }
            resultsPane.getChildren().add(suggBox);
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #30363d;");
        resultsPane.getChildren().add(sep);

        if (results.isEmpty()) {
            Label noResults = new Label("No results found.");
            noResults.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 14px;");
            resultsPane.getChildren().add(noResults);
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            resultsPane.getChildren().add(buildResultCard(i + 1, results.get(i)));
        }
    }

    private VBox buildResultCard(int rank, SearchResult result) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #161b22; -fx-background-radius: 8; " +
            "-fx-border-color: #30363d; -fx-border-radius: 8; -fx-border-width: 1;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label rankLabel = new Label("#" + rank);
        rankLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px; -fx-min-width: 28px;");

        Label langBadge = new Label(result.getDocument().isArabic() ? "AR" : "EN");
        langBadge.setStyle("-fx-background-color: " +
            (result.getDocument().isArabic() ? "#1a472a" : "#0d1117") +
            "; -fx-text-fill: " +
            (result.getDocument().isArabic() ? "#3fb950" : "#58a6ff") +
            "; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px; -fx-font-weight: bold;");

        Label titleLabel = new Label(result.getDocument().getTitle());
        titleLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scoreLabel = new Label(String.format("%.4f", result.getScore()));
        scoreLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px; -fx-font-family: 'Courier New';");

        titleRow.getChildren().addAll(rankLabel, langBadge, titleLabel, spacer, scoreLabel);

        // Snippet
        Label snippet = new Label(result.getSnippet());
        snippet.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px; -fx-wrap-text: true;");
        snippet.setMaxWidth(Double.MAX_VALUE);

        // File path
        Label pathLabel = new Label(result.getDocument().getFilePath());
        pathLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 10px;");

        card.getChildren().addAll(titleRow, snippet, pathLabel);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1c2128; -fx-background-radius: 8; " +
            "-fx-border-color: #58a6ff; -fx-border-radius: 8; -fx-border-width: 1;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #161b22; -fx-background-radius: 8; " +
            "-fx-border-color: #30363d; -fx-border-radius: 8; -fx-border-width: 1;"));

        return card;
    }

    private void showWelcomeMessage() {
        resultsPane.getChildren().clear();
        VBox welcome = new VBox(15);
        welcome.setAlignment(Pos.CENTER);
        welcome.setPadding(new Insets(60, 0, 0, 0));

        Label icon = new Label("🔍");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Bilingual Search Engine");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #58a6ff;");

        Label desc = new Label("Search across English and Arabic documents using:\n" +
            "Ranked TF-IDF  •  Phrase Search  •  Proximity /k  •  Wildcard *  •  Spelling Correction");
        desc.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px; -fx-text-alignment: center;");
        desc.setTextAlignment(TextAlignment.CENTER);

        welcome.getChildren().addAll(icon, title, desc);
        resultsPane.getChildren().add(welcome);
    }

    private void updateIndexInfo() {
        if (engine == null) return;
        StringBuilder sb = new StringBuilder();

        sb.append("           BILINGUAL INDEX STATISTICS\n");

        sb.append(" CORPUS\n");
        sb.append(String.format("   Total Documents : %d\n", engine.getDocuments().size()));
        sb.append(String.format("   English Docs    : %d\n", engine.getEnglishDocCount()));
        sb.append(String.format("   Arabic Docs     : %d\n", engine.getArabicDocCount()));
        sb.append(String.format("   Total Words     : %d\n\n", engine.getTotalWords()));

        sb.append("📊 INDEX\n");
        sb.append(String.format("   English Vocab   : %d terms\n", engine.getEnglishIndex().size()));
        sb.append(String.format("   Arabic Vocab    : %d terms\n", engine.getArabicIndex().size()));
        sb.append(String.format("   Build Time      : %d ms\n\n", engine.getIndexBuildTimeMs()));

        sb.append("📄 DOCUMENT LIST\n");
        for (Document doc : engine.getDocuments()) {
            sb.append(String.format("   [%s] ID:%d  %s\n",
                doc.isArabic() ? "AR" : "EN", doc.getId(), doc.getTitle()));
        }

        sb.append("\n ENGLISH VOCABULARY (first 30 terms)\n   ");
        int count = 0;
        for (String term : engine.getEnglishIndex().getVocabulary()) {
            sb.append(term).append(", ");
            if (++count >= 30) { sb.append("..."); break; }
        }

        sb.append("\n\n ARABIC VOCABULARY (first 30 terms)\n   ");
        count = 0;
        for (String term : engine.getArabicIndex().getVocabulary()) {
            sb.append(term).append(", ");
            if (++count >= 30) { sb.append("..."); break; }
        }

        indexInfoArea.setText(sb.toString());
    }

    private String runEvaluation() {
        if (engine == null) return "Engine not ready.";

        StringBuilder sb = new StringBuilder();
        sb.append("           PRECISION & RECALL EVALUATION\n");


        // Define test queries with expected relevant doc IDs
        // These are placeholder annotations — in a real system they'd be manually labeled
        Object[][] tests = {
            {"information retrieval", false, new int[]{}},
            {"machine learning", false, new int[]{}},
            {"neural network", false, new int[]{}},
            {"البحث", true, new int[]{}},
            {"الجامعة", true, new int[]{}},
        };

        long totalRetrievalTime = 0;
        for (Object[] test : tests) {
            String query = (String) test[0];
            boolean arabic = (boolean) test[1];

            long t = System.currentTimeMillis();
            List<SearchResult> results = engine.rankedSearch(query);
            long elapsed = System.currentTimeMillis() - t;
            totalRetrievalTime += elapsed;

            sb.append("Query: \"").append(query).append("\"\n");
            sb.append(String.format("  Language : %s\n", arabic ? "Arabic" : "English"));
            sb.append(String.format("  Retrieved: %d results\n", results.size()));
            sb.append(String.format("  Time     : %d ms\n", elapsed));
            if (!results.isEmpty()) {
                sb.append("  Top 3 Results:\n");
                for (int i = 0; i < Math.min(3, results.size()); i++) {
                    SearchResult r = results.get(i);
                    sb.append(String.format("    %d. [%.4f] %s\n",
                        i + 1, r.getScore(), r.getDocument().getTitle()));
                }
            }
            sb.append("\n");
        }

        sb.append("─────────────────────────────────────────────────────\n");
        sb.append(String.format("Total Retrieval Time: %d ms\n", totalRetrievalTime));
        sb.append(String.format("Avg per query      : %.1f ms\n", (double) totalRetrievalTime / tests.length));
        sb.append(String.format("Index Build Time   : %d ms\n", engine.getIndexBuildTimeMs()));
        sb.append("\nNote: Precision/Recall require manually annotated relevant sets.\n");
        sb.append("Mark relevant documents per query to get exact P/R scores.\n");

        return sb.toString();
    }

    private void setStatus(String msg, boolean loading) {
        Platform.runLater(() -> {
            statusLabel.setText(msg);
            loadingIndicator.setVisible(loading);
        });
    }

    private void applyStyles(Scene scene) {
        try {
            java.net.URL cssUrl = getClass().getResource("/ui/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception ignored) {
            // Inline styles will be used as fallback
        }
    }

    // ---- Style helpers ----
    private String inputStyle() {
        return "-fx-background-color: #161b22; -fx-text-fill: #e6edf3; " +
            "-fx-border-color: #30363d; -fx-border-radius: 6; -fx-background-radius: 6; " +
            "-fx-prompt-text-fill: #484f58; -fx-font-size: 13px; -fx-padding: 8;";
    }

    private String comboStyle() {
        return "-fx-background-color: #161b22; -fx-text-fill: #e6edf3; " +
            "-fx-border-color: #30363d; -fx-border-radius: 6; " +
            "-fx-background-radius: 6; -fx-font-size: 12px;";
    }

    private String primaryBtnStyle() {
        return "-fx-background-color: #238636; -fx-text-fill: white; " +
            "-fx-font-weight: bold; -fx-font-size: 13px; " +
            "-fx-padding: 8 18; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private String secondaryBtnStyle() {
        return "-fx-background-color: #21262d; -fx-text-fill: #c9d1d9; " +
            "-fx-border-color: #30363d; -fx-border-radius: 6; " +
            "-fx-font-size: 12px; -fx-padding: 6 12; -fx-cursor: hand;";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
