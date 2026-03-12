package pck;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class FlipGameBacktrackingGUI extends Application {
    
    // ======================================================================
    // BACKTRACKING GAME LOGIC - Adapted from FlipBacktracking.java
    // ======================================================================
    
    static class BacktrackingSolver {
        int H, W;
        int[][] board;
        int[][] bestClicks;
        int bestCost;
        List<BacktrackStep> allSteps;
        int currentStepIndex;
        boolean solutionFound;
        
        BacktrackingSolver(int[][] initialBoard) {
            this.H = initialBoard.length;
            this.W = initialBoard[0].length;
            this.board = copy(initialBoard);
            this.bestCost = Integer.MAX_VALUE;
            this.bestClicks = null;
            this.allSteps = new ArrayList<>();
            this.currentStepIndex = 0;
            this.solutionFound = false;
        }
        
        int[][] copy(int[][] g) {
            int[][] c = new int[H][W];
            for (int i = 0; i < H; i++)
                c[i] = g[i].clone();
            return c;
        }
        
        void printGrid(int[][] g) {
            for (int[] row : g) {
                for (int v : row)
                    System.out.print(v + " ");
                System.out.println();
            }
            System.out.println();
        }
        
        void click(int[][] g, int r, int c) {
            int[][] dir = {{0,0},{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : dir) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr >= 0 && nr < H && nc >= 0 && nc < W)
                    g[nr][nc] ^= 1;
            }
        }
        
        boolean rowSolved(int[][] g, int r) {
            for (int c = 0; c < W; c++)
                if (g[r][c] != 1) return false;
            return true;
        }
        
        int countClicks(int[][] clicks) {
            int count = 0;
            for (int[] row : clicks)
                for (int v : row)
                    count += v;
            return count;
        }
        
        void backtrack(int[][] g, int[][] clicks, int cell, List<BacktrackStep> steps, int depth) {
            String indent = "  ".repeat(depth);
            
            if (cell == H * W) {
                if (rowSolved(g, H - 1)) {
                    int cost = countClicks(clicks);
                    steps.add(new BacktrackStep(
                        copy(g), 
                        copy(clicks), 
                        -1, -1,
                        indent + "SUCCESS → Board solved with clicks=" + cost,
                        BacktrackStep.Type.SUCCESS
                    ));
                    
                    if (cost < bestCost) {
                        bestCost = cost;
                        bestClicks = copy(clicks);
                        solutionFound = true;
                        steps.add(new BacktrackStep(
                            copy(g), 
                            copy(clicks), 
                            -1, -1,
                            indent + "NEW BEST SOLUTION FOUND!",
                            BacktrackStep.Type.SOLUTION
                        ));
                    }
                } else {
                    steps.add(new BacktrackStep(
                        copy(g), 
                        copy(clicks), 
                        -1, -1,
                        indent + "FAIL → Last row not solved",
                        BacktrackStep.Type.FAILURE
                    ));
                }
                return;
            }
            
            int r = cell / W;
            int c = cell % W;
            
            steps.add(new BacktrackStep(
                copy(g), 
                copy(clicks), 
                r, c,
                indent + "Entering cell (" + r + "," + c + ")",
                BacktrackStep.Type.ENTER
            ));
            
            // ---------- OPTION 1 : DON'T CLICK ----------
            steps.add(new BacktrackStep(
                copy(g), 
                copy(clicks), 
                r, c,
                indent + "Try: NO CLICK (" + r + "," + c + ")",
                BacktrackStep.Type.TRY_NO_CLICK
            ));
            
            if (c == W - 1 && r > 0 && !rowSolved(g, r - 1)) {
                steps.add(new BacktrackStep(
                    copy(g), 
                    copy(clicks), 
                    r, c,
                    indent + "PRUNE → Row " + (r - 1) + " frozen but not solved",
                    BacktrackStep.Type.PRUNE
                ));
            } else {
                backtrack(g, clicks, cell + 1, steps, depth + 1);
            }
            
            // ---------- OPTION 2 : CLICK ----------
            steps.add(new BacktrackStep(
                copy(g), 
                copy(clicks), 
                r, c,
                indent + "Try: CLICK (" + r + "," + c + ")",
                BacktrackStep.Type.TRY_CLICK
            ));
            
            click(g, r, c);
            clicks[r][c] = 1;
            
            steps.add(new BacktrackStep(
                copy(g), 
                copy(clicks), 
                r, c,
                indent + "After CLICK at (" + r + "," + c + ")",
                BacktrackStep.Type.AFTER_CLICK
            ));
            
            boolean prune = false;
            if (c == W - 1 && r > 0 && !rowSolved(g, r - 1)) {
                steps.add(new BacktrackStep(
                    copy(g), 
                    copy(clicks), 
                    r, c,
                    indent + "PRUNE after click → Row " + (r - 1) + " frozen but incorrect",
                    BacktrackStep.Type.PRUNE
                ));
                prune = true;
            }
            
            int cost = countClicks(clicks);
            if (cost >= bestCost) {
                steps.add(new BacktrackStep(
                    copy(g), 
                    copy(clicks), 
                    r, c,
                    indent + "PRUNE cost=" + cost + " ≥ bestCost=" + bestCost,
                    BacktrackStep.Type.PRUNE
                ));
                prune = true;
            }
            
            if (!prune) {
                backtrack(g, clicks, cell + 1, steps, depth + 1);
            }
            
            // ---------- BACKTRACK ----------
            steps.add(new BacktrackStep(
                copy(g), 
                copy(clicks), 
                r, c,
                indent + "BACKTRACK → Undo click (" + r + "," + c + ")",
                BacktrackStep.Type.BACKTRACK
            ));
            
            click(g, r, c);
            clicks[r][c] = 0;
            
            steps.add(new BacktrackStep(
                copy(g), 
                copy(clicks), 
                r, c,
                indent + "After undo at (" + r + "," + c + ")",
                BacktrackStep.Type.AFTER_BACKTRACK
            ));
        }
        
        void solve() {
            int[][] g = copy(board);
            int[][] clicks = new int[H][W];
            allSteps.clear();
            System.out.println("===== INITIAL BOARD =====");
            printGrid(board);
            
            backtrack(g, clicks, 0, allSteps, 0);
            
            // Add final step
            if (bestClicks != null) {
                int[][] solutionBoard = copy(board);
                for (int r = 0; r < H; r++) {
                    for (int c = 0; c < W; c++) {
                        if (bestClicks[r][c] == 1) {
                            click(solutionBoard, r, c);
                        }
                    }
                }
                
                allSteps.add(new BacktrackStep(
                    solutionBoard,
                    bestClicks,
                    -1, -1,
                    "===== FINAL RESULT =====\nMinimum clicks = " + bestCost,
                    BacktrackStep.Type.FINAL
                ));
            } else {
                allSteps.add(new BacktrackStep(
                    board,
                    new int[H][W],
                    -1, -1,
                    "No solution exists.",
                    BacktrackStep.Type.FAILURE
                ));
            }
        }
        
        BacktrackStep getCurrentStep() {
            if (currentStepIndex >= 0 && currentStepIndex < allSteps.size()) {
                return allSteps.get(currentStepIndex);
            }
            return null;
        }
        
        boolean hasNext() {
            return currentStepIndex < allSteps.size() - 1;
        }
        
        boolean hasPrevious() {
            return currentStepIndex > 0;
        }
        
        void nextStep() {
            if (hasNext()) {
                currentStepIndex++;
            }
        }
        
        void previousStep() {
            if (hasPrevious()) {
                currentStepIndex--;
            }
        }
        
        void reset() {
            currentStepIndex = 0;
        }
        
        void goToLast() {
            currentStepIndex = allSteps.size() - 1;
        }
    }
    
    // ======================================================================
    // BACKTRACKING STEP DATA STRUCTURE
    // ======================================================================
    
    static class BacktrackStep {
        enum Type {
            ENTER, TRY_NO_CLICK, TRY_CLICK, AFTER_CLICK, 
            PRUNE, BACKTRACK, AFTER_BACKTRACK, SUCCESS, SOLUTION, FAILURE, FINAL
        }
        
        int[][] board;
        int[][] clicks;
        int row, col;
        String message;
        Type type;
        
        BacktrackStep(int[][] board, int[][] clicks, int row, int col, String message, Type type) {
            this.board = board;
            this.clicks = clicks;
            this.row = row;
            this.col = col;
            this.message = message;
            this.type = type;
        }
        
        Color getTypeColor() {
            switch (type) {
                case ENTER: return Color.rgb(52, 152, 219); // Blue
                case TRY_NO_CLICK: return Color.rgb(155, 89, 182); // Purple
                case TRY_CLICK: return Color.rgb(241, 196, 15); // Yellow
                case AFTER_CLICK: return Color.rgb(46, 204, 113); // Green
                case PRUNE: return Color.rgb(231, 76, 60); // Red
                case BACKTRACK: return Color.rgb(230, 126, 34); // Orange
                case AFTER_BACKTRACK: return Color.rgb(52, 73, 94); // Dark Blue
                case SUCCESS: return Color.rgb(39, 174, 96); // Dark Green
                case SOLUTION: return Color.rgb(46, 204, 113); // Green
                case FAILURE: return Color.rgb(192, 57, 43); // Dark Red
                case FINAL: return Color.rgb(155, 89, 182); // Purple
                default: return Color.BLACK;
            }
        }
        
        String getTypeName() {
            return type.toString().replace('_', ' ');
        }
    }
    
    // ======================================================================
    // GUI COMPONENTS AND STATE
    // ======================================================================
    
    private BacktrackingSolver solver;
    private Button[][] tiles;
    private Label statusLabel;
    private Label stepLabel;
    private Label statsLabel;
    private Label typeLabel;
    private GridPane gameGrid;
    private VBox controlPanel;
    private StackPane root;
    private Timeline autoPlayTimeline;
    private boolean autoPlaying = false;
    private double autoPlaySpeed = 2.0; // seconds per step
    
    // Color scheme
    private Color BACKGROUND = Color.rgb(248, 249, 250);
    private Color CARD_BACKGROUND = Color.WHITE;
    private Color PRIMARY_COLOR = Color.rgb(33, 37, 41);
    private Color ACCENT_COLOR = Color.rgb(13, 110, 253);
    private Color SUCCESS_COLOR = Color.rgb(25, 135, 84);
    private Color WARNING_COLOR = Color.rgb(255, 193, 7);
    private Color DANGER_COLOR = Color.rgb(220, 53, 69);
    
    // Original 6x6 board from FlipBacktracking.java
    private int[][] originalBoard = {
        {1,0,0,1,0,1},
        {0,0,0,1,0,0},
        {1,0,1,1,0,0},
        {1,1,1,0,0,0},
        {0,0,0,0,0,1},
        {1,1,0,0,1,0}
    };
    
    @Override
    public void start(Stage primaryStage) {
        showMainMenu(primaryStage);
    }
    
    // ======================================================================
    // MAIN MENU
    // ======================================================================
    
    private void showMainMenu(Stage stage) {
        VBox menu = new VBox(30);
        menu.setAlignment(Pos.CENTER);
        menu.setBackground(new Background(new BackgroundFill(
            BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        menu.setPadding(new Insets(40));
        
        Label title = new Label("FLIP GAME");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 72));
        title.setTextFill(PRIMARY_COLOR);
        
        Label subtitle = new Label("Backtracking Algorithm Visualization");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        subtitle.setTextFill(Color.GRAY);
        
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(500);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                     "-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 12;");
        
        Label description = new Label("6×6 Board - Original Problem");
        description.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        description.setTextFill(PRIMARY_COLOR);
        
        // Mini preview of the board
        GridPane preview = new GridPane();
        preview.setAlignment(Pos.CENTER);
        preview.setHgap(2);
        preview.setVgap(2);
        
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                Button cell = new Button();
                cell.setPrefSize(30, 30);
                if (originalBoard[r][c] == 1) {
                    cell.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1;");
                    cell.setText("●");
                    cell.setTextFill(PRIMARY_COLOR);
                } else {
                    cell.setStyle("-fx-background-color: #212529; -fx-border-color: #495057; -fx-border-width: 1;");
                    cell.setText("○");
                    cell.setTextFill(Color.WHITE);
                }
                preview.add(cell, c, r);
            }
        }
        
        Button startBtn = new Button("Start Visualization");
        startBtn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        startBtn.setPrefSize(250, 60);
        startBtn.setStyle("-fx-background-color: " + toHex(ACCENT_COLOR) + ";" +
                         "-fx-text-fill: white;" +
                         "-fx-background-radius: 30;");
        startBtn.setOnAction(e -> startVisualization(stage));
        
        card.getChildren().addAll(description, preview, startBtn);
        
        menu.getChildren().addAll(title, subtitle, card);
        
        Scene scene = new Scene(menu, 600, 700);
        stage.setScene(scene);
        stage.setTitle("Flip Game - Backtracking Visualization");
        stage.show();
    }
    
    // ======================================================================
    // VISUALIZATION SCREEN
    // ======================================================================
    
    private void startVisualization(Stage stage) {
        solver = new BacktrackingSolver(originalBoard);
        
        // Show loading message
        statusLabel = new Label("Solving... Please wait");
        
        // Run solver in background thread
        new Thread(() -> {
            solver.solve();
            javafx.application.Platform.runLater(() -> {
                updateDisplay();
                statusLabel.setText("Ready! " + solver.allSteps.size() + " steps recorded");
            });
        }).start();
        
        root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        
        BorderPane mainLayout = new BorderPane();
        
        // Top Panel - Title and Status
        VBox topPanel = createTopPanel();
        mainLayout.setTop(topPanel);
        
        // Center - Game Board
        int tileSize = 70; // Fixed size for 6x6 board
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(8);
        gameGrid.setVgap(8);
        gameGrid.setPadding(new Insets(20));
        
        createGameBoard(originalBoard, tileSize);
        mainLayout.setCenter(gameGrid);
        
        // Bottom - Control Panel
        controlPanel = createControlPanel();
        mainLayout.setBottom(controlPanel);
        
        root.getChildren().add(mainLayout);
        
        Scene scene = new Scene(root, 1000, 850);
        stage.setScene(scene);
        stage.setTitle("Backtracking Visualization - 6×6 Board");
    }
    
    private VBox createTopPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");
        
        Label titleLabel = new Label("Backtracking Algorithm in Action");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setTextFill(PRIMARY_COLOR);
        
        // Status area
        HBox statusBox = new HBox(20);
        statusBox.setAlignment(Pos.CENTER);
        
        statusLabel = new Label("Initializing...");
        statusLabel.setFont(Font.font("Arial", FontWeight.MEDIUM, 14));
        
        typeLabel = new Label("");
        typeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        statusBox.getChildren().addAll(statusLabel, typeLabel);
        
        // Step counter
        stepLabel = new Label("Step: 0 / 0");
        stepLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        stepLabel.setTextFill(ACCENT_COLOR);
        
        // Stats
        statsLabel = new Label("");
        statsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        statsLabel.setTextFill(Color.GRAY);
        
        panel.getChildren().addAll(titleLabel, statusBox, stepLabel, statsLabel);
        return panel;
    }
    
    private void createGameBoard(int[][] initialBoard, int tileSize) {
        int N = 6; // Fixed 6x6
        tiles = new Button[N][N];
        
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                Button tile = new Button();
                tile.setPrefSize(tileSize, tileSize);
                tile.setFont(Font.font("Arial", FontWeight.BOLD, tileSize / 3));
                updateTileStyle(tile, initialBoard[r][c], false, false);
                tiles[r][c] = tile;
                gameGrid.add(tile, c, r);
            }
        }
    }
    
    private void updateTileStyle(Button tile, int value, boolean isClick, boolean highlight) {
        if (value == 1) {
            tile.setStyle("-fx-background-color: white;" +
                         "-fx-background-radius: 8;" +
                         "-fx-border-color: " + (highlight ? "#0d6efd" : "#dee2e6") + ";" +
                         "-fx-border-width: " + (highlight ? "3" : "2") + ";" +
                         "-fx-border-radius: 8;" +
                         (isClick ? "-fx-effect: dropshadow(gaussian, #ffc107, 10, 0.5, 0, 0);" : ""));
            tile.setText("●");
            tile.setTextFill(PRIMARY_COLOR);
        } else {
            tile.setStyle("-fx-background-color: #212529;" +
                         "-fx-background-radius: 8;" +
                         "-fx-border-color: " + (highlight ? "#0d6efd" : "#495057") + ";" +
                         "-fx-border-width: " + (highlight ? "3" : "2") + ";" +
                         "-fx-border-radius: 8;" +
                         (isClick ? "-fx-effect: dropshadow(gaussian, #ffc107, 10, 0.5, 0, 0);" : ""));
            tile.setText("○");
            tile.setTextFill(Color.WHITE);
        }
    }
    
    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        // Step navigation
        HBox navBox = new HBox(10);
        navBox.setAlignment(Pos.CENTER);
        
        Button firstBtn = createControlButton("⏮ First", PRIMARY_COLOR);
        Button prevBtn = createControlButton("⏪ Previous", PRIMARY_COLOR);
        Button nextBtn = createControlButton("Next ⏩", PRIMARY_COLOR);
        Button lastBtn = createControlButton("Last ⏭", PRIMARY_COLOR);
        
        firstBtn.setOnAction(e -> {
            stopAutoPlay();
            solver.reset();
            updateDisplay();
        });
        
        prevBtn.setOnAction(e -> {
            stopAutoPlay();
            solver.previousStep();
            updateDisplay();
        });
        
        nextBtn.setOnAction(e -> {
            stopAutoPlay();
            solver.nextStep();
            updateDisplay();
        });
        
        lastBtn.setOnAction(e -> {
            stopAutoPlay();
            solver.goToLast();
            updateDisplay();
        });
        
        navBox.getChildren().addAll(firstBtn, prevBtn, nextBtn, lastBtn);
        
        // Auto play controls
        HBox autoBox = new HBox(10);
        autoBox.setAlignment(Pos.CENTER);
        
        Button playBtn = createControlButton("▶ Play", SUCCESS_COLOR);
        Button pauseBtn = createControlButton("⏸ Pause", WARNING_COLOR);
        Button slowerBtn = createControlButton("Slower", ACCENT_COLOR);
        Button fasterBtn = createControlButton("Faster", ACCENT_COLOR);
        
        playBtn.setOnAction(e -> startAutoPlay());
        pauseBtn.setOnAction(e -> stopAutoPlay());
        slowerBtn.setOnAction(e -> adjustSpeed(1.2));
        fasterBtn.setOnAction(e -> adjustSpeed(0.8));
        
        autoBox.getChildren().addAll(playBtn, pauseBtn, slowerBtn, fasterBtn);
        
        // Speed indicator
        Label speedLabel = new Label(String.format("Speed: %.1f seconds per step", autoPlaySpeed));
        speedLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        speedLabel.setId("speedLabel");
        
        // Message legend
        HBox legendBox = new HBox(15);
        legendBox.setAlignment(Pos.CENTER);
        legendBox.setPadding(new Insets(10, 0, 0, 0));
        
        legendBox.getChildren().addAll(
            createLegendItem("Enter", Color.rgb(52, 152, 219)),
            createLegendItem("Try", Color.rgb(241, 196, 15)),
            createLegendItem("Prune", Color.rgb(231, 76, 60)),
            createLegendItem("Backtrack", Color.rgb(230, 126, 34)),
            createLegendItem("Success", Color.rgb(46, 204, 113))
        );
        
        panel.getChildren().addAll(navBox, autoBox, speedLabel, legendBox);
        
        return panel;
    }
    
    private HBox createLegendItem(String text, Color color) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER);
        
        Label colorBox = new Label("  ");
        colorBox.setStyle("-fx-background-color: " + toHex(color) + ";" +
                         "-fx-background-radius: 3;");
        
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        
        box.getChildren().addAll(colorBox, label);
        return box;
    }
    
    private Button createControlButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.MEDIUM, 12));
        btn.setPrefSize(100, 35);
        btn.setStyle("-fx-background-color: " + toHex(color) + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 6;");
        
        btn.setOnMouseEntered(e -> btn.setOpacity(0.9));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        
        return btn;
    }
    
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }
    
    private void startAutoPlay() {
        if (autoPlaying) return;
        autoPlaying = true;
        
        autoPlayTimeline = new Timeline(
            new KeyFrame(Duration.seconds(autoPlaySpeed), e -> {
                if (solver.hasNext()) {
                    solver.nextStep();
                    updateDisplay();
                } else {
                    stopAutoPlay();
                }
            })
        );
        autoPlayTimeline.setCycleCount(Timeline.INDEFINITE);
        autoPlayTimeline.play();
    }
    
    private void stopAutoPlay() {
        if (autoPlayTimeline != null) {
            autoPlayTimeline.stop();
        }
        autoPlaying = false;
    }
    
    private void adjustSpeed(double factor) {
        autoPlaySpeed *= factor;
        autoPlaySpeed = Math.max(0.5, Math.min(5.0, autoPlaySpeed));
        
        Label speedLabel = (Label) controlPanel.lookup("#speedLabel");
        if (speedLabel != null) {
            speedLabel.setText(String.format("Speed: %.1f seconds per step", autoPlaySpeed));
        }
        
        if (autoPlaying) {
            stopAutoPlay();
            startAutoPlay();
        }
    }
    
    private void updateDisplay() {
        BacktrackStep step = solver.getCurrentStep();
        if (step == null) return;
        
        // Update board
        int N = 6;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                boolean isClick = step.clicks != null && step.clicks[r][c] == 1;
                boolean highlight = (step.row == r && step.col == c);
                updateTileStyle(tiles[r][c], step.board[r][c], isClick, highlight);
            }
        }
        
        // Update status
        statusLabel.setText(step.message);
        statusLabel.setTextFill(step.getTypeColor());
        
        typeLabel.setText("[" + step.getTypeName() + "]");
        typeLabel.setTextFill(step.getTypeColor());
        
        stepLabel.setText(String.format("Step: %d / %d", 
            solver.currentStepIndex + 1, solver.allSteps.size()));
        
        // Update stats
        int clickCount = 0;
        if (step.clicks != null) {
            for (int[] row : step.clicks) {
                for (int v : row) clickCount += v;
            }
        }
        
        String bestInfo = solver.bestCost == Integer.MAX_VALUE ? 
            "Not found yet" : String.valueOf(solver.bestCost);
        statsLabel.setText(String.format("Current clicks: %d | Best solution: %s", 
            clickCount, bestInfo));
        
        // Flash effect for the current cell
        if (step.row >= 0 && step.col >= 0) {
            flashTile(step.row, step.col);
        }
    }
    
    private void flashTile(int r, int c) {
        Button tile = tiles[r][c];
        
        ScaleTransition st = new ScaleTransition(Duration.millis(200), tile);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}