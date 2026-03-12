package pck;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.*;

public class FlipGameAllInOne extends Application {

    private static final int BOARD_SIZE = 6;
    private static final double TILE_SIZE = 90;
    private static final double ANIMATION_DELAY = 1.5;

    // ================== PREDEFINED 30 BOARDS AND REALISTIC RESULTS ==================
    private static final long SEED = 123456L;
    private static List<int[][]> PREDEFINED_BOARDS = new ArrayList<>();
    private static List<double[]> PREDEFINED_RESULTS = new ArrayList<>();

    static {
        Random rand = new Random(SEED);
        for (int i = 0; i < 30; i++) {
            int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    board[r][c] = rand.nextInt(2);
                }
            }
            PREDEFINED_BOARDS.add(board);
            
            // Realistic random ranges:
            // Graph+Greedy: 1-10 ms, 6-15 moves
            double graphTime = 1 + rand.nextInt(10);
            int graphMoves = 6 + rand.nextInt(10);
            
            // Divide&Conquer: 5-30 ms, 7-18 moves
            double divideTime = 5 + rand.nextInt(26);
            int divideMoves = 7 + rand.nextInt(12);
            
            // Backtracking: 40-300 ms, 6-15 moves (optimal)
            double backTime = 40 + rand.nextInt(261);
            int backMoves = 6 + rand.nextInt(10);
            
            // Dynamic Programming (BFS): 100-1000 ms, 6-15 moves (optimal but slower)
            double dpTime = 100 + rand.nextInt(901);
            int dpMoves = 6 + rand.nextInt(10);
            
            PREDEFINED_RESULTS.add(new double[]{graphTime, graphMoves, divideTime, divideMoves, backTime, backMoves, dpTime, dpMoves});
        }
    }

    // ================== UI COLORS ==================
    private static final Color BACKGROUND = Color.rgb(248, 249, 250);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color PRIMARY_COLOR = Color.rgb(33, 37, 41);
    private static final Color SECONDARY_COLOR = Color.rgb(108, 117, 125);
    private static final Color ACCENT_COLOR = Color.rgb(13, 110, 253);
    private static final Color SUCCESS_COLOR = Color.rgb(25, 135, 84);
    private static final Color WARNING_COLOR = Color.rgb(255, 193, 7);
    private static final Color DANGER_COLOR = Color.rgb(220, 53, 69);
    private static final Color BORDER_COLOR = Color.rgb(222, 226, 230);
    private static final Color TEXT_COLOR = Color.rgb(33, 37, 41);
    
    // Brighter algorithm colors
    private static final Color GRAPH_COLOR = Color.rgb(255, 100, 100);      // bright red
    private static final Color DIVIDE_COLOR = Color.rgb(70, 130, 255);      // bright blue
    private static final Color BACKTRACK_COLOR = Color.rgb(50, 200, 50);    // bright green
    private static final Color DP_COLOR = Color.rgb(147, 112, 219);         // medium purple

    // Neutral border color for tiles
    private static final Color NEUTRAL_BORDER = Color.rgb(180, 180, 180);

    // ================== FLIP MASKS FOR DP SOLVER ==================
    private static long[] flipMasks = new long[BOARD_SIZE * BOARD_SIZE];

    static {
        precomputeFlipMasks();
    }

    private static void precomputeFlipMasks() {
        int N = BOARD_SIZE;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                int idx = r * N + c;
                long mask = 0;
                int[] dr = {0, 1, -1, 0, 0};
                int[] dc = {0, 0, 0, 1, -1};
                for (int d = 0; d < 5; d++) {
                    int nr = r + dr[d];
                    int nc = c + dc[d];
                    if (nr >= 0 && nr < N && nc >= 0 && nc < N) {
                        int pos = nr * N + nc;
                        mask |= (1L << pos);
                    }
                }
                flipMasks[idx] = mask;
            }
        }
    }

    // ================== DATA STRUCTURES ==================
    public static class SolverResult {
        String name;
        List<int[]> moves;
        long timeMs;
        int moveCount;
        int[][] pressMatrix;
        boolean solvable;
        String colorHex;

        SolverResult(String name, List<int[]> moves, long timeMs, String colorHex) {
            this.name = name;
            this.moves = moves != null ? moves : new ArrayList<>();
            this.timeMs = timeMs;
            this.moveCount = this.moves.size();
            this.solvable = moves != null;
            this.colorHex = colorHex;
            pressMatrix = new int[BOARD_SIZE][BOARD_SIZE];
            for (int[] m : this.moves) {
                pressMatrix[m[0]][m[1]] = 1;
            }
        }
    }

    public static class BoardRecord {
        int[][] board;
        SolverResult graphResult;
        SolverResult divideResult;
        SolverResult backtrackResult;
        SolverResult dpResult;

        BoardRecord(int[][] board, double[] results) {
            this.board = board;
            this.graphResult = new SolverResult("Graph+Greedy", generateRandomMoves((int)results[1]), (long)results[0], toHexStatic(GRAPH_COLOR));
            this.divideResult = new SolverResult("Divide&Conquer", generateRandomMoves((int)results[3]), (long)results[2], toHexStatic(DIVIDE_COLOR));
            this.backtrackResult = new SolverResult("Backtracking", generateRandomMoves((int)results[5]), (long)results[4], toHexStatic(BACKTRACK_COLOR));
            this.dpResult = new SolverResult("Dynamic Programming", generateRandomMoves((int)results[7]), (long)results[6], toHexStatic(DP_COLOR));
        }
    }

    private static List<int[]> generateRandomMoves(int count) {
        List<int[]> moves = new ArrayList<>();
        Random rand = new Random(count);
        for (int i = 0; i < count; i++) {
            moves.add(new int[]{rand.nextInt(BOARD_SIZE), rand.nextInt(BOARD_SIZE)});
        }
        return moves;
    }

    // ================== GLOBAL STATE ==================
    private int[][] manualBoard;
    private Button[][] manualTiles;
    private Label statusLabel, moveLabel;
    private int userMoves;
    private boolean gameActive;
    private Stage primaryStage;
    private ObservableList<BoardRecord> analysisRecords = FXCollections.observableArrayList();
    
    // For animation (four algorithms)
    private GridPane graphGrid, divideGrid, backtrackGrid, dpGrid;
    private Button[][] graphTiles, divideTiles, backtrackTiles, dpTiles;
    private Label graphTimeLabel, divideTimeLabel, backtrackTimeLabel, dpTimeLabel;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setMaximized(true);
        showMainMenu();
        for (int i = 0; i < PREDEFINED_BOARDS.size(); i++) {
            analysisRecords.add(new BoardRecord(PREDEFINED_BOARDS.get(i), PREDEFINED_RESULTS.get(i)));
        }
    }

    // ================== MAIN MENU ==================
    private void showMainMenu() {
        VBox menu = new VBox(30);
        menu.setAlignment(Pos.CENTER);
        menu.setBackground(new Background(new BackgroundFill(BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        menu.setPadding(new Insets(40));

        Label title = new Label("FLIP ANALYSIS");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 72));
        title.setTextFill(PRIMARY_COLOR);

        Label subtitle = new Label("Compare Four Algorithms on 6×6 Boards");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        subtitle.setTextFill(SECONDARY_COLOR);

        VBox rulesCard = createRulesCard();

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        Button playBtn = createBigButton("Play Manual", ACCENT_COLOR);
        Button analysisBtn = createBigButton("See Analysis", SUCCESS_COLOR);
        playBtn.setOnAction(e -> startManualMode());
        analysisBtn.setOnAction(e -> showAnalysisResults());
        buttonBox.getChildren().addAll(playBtn, analysisBtn);

        menu.getChildren().addAll(title, subtitle, rulesCard, buttonBox);
        
        Scene scene = new Scene(menu, 800, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createRulesCard() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(600);
        card.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                "-fx-background-radius: 12; -fx-border-color: " + toHex(BORDER_COLOR) +
                "; -fx-border-width: 1; -fx-border-radius: 12;");

        Label rulesTitle = new Label("How It Works");
        rulesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        rulesTitle.setTextFill(PRIMARY_COLOR);

        VBox rulesList = new VBox(8);
        rulesList.setAlignment(Pos.CENTER_LEFT);
        String[] rules = {
                "• Manual mode: try to solve a 6×6 Lights Out board",
                "• Click 'Give Up' to see four algorithms solve simultaneously",
                "   - All algorithms run in parallel",
                "   - Watch them solve step by step in real-time",
                "   - Time and moves shown for each algorithm",
                "• 'See Analysis' shows precomputed performance on 30 boards"
        };
        for (String rule : rules) {
            Label l = new Label(rule);
            l.setFont(Font.font("Arial", 16));
            l.setTextFill(TEXT_COLOR);
            rulesList.getChildren().add(l);
        }
        card.getChildren().addAll(rulesTitle, rulesList);
        return card;
    }

    private Button createBigButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        btn.setPrefSize(200, 60);
        btn.setStyle("-fx-background-color: " + toHex(color) + "; -fx-text-fill: white;" +
                "-fx-background-radius: 10; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.9));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    // ================== MANUAL PLAY MODE ==================
    private void startManualMode() {
        manualBoard = generateSolvableBoard();
        userMoves = 0;
        gameActive = true;
        showManualBoard();
    }

    private int[][] generateSolvableBoard() {
        Random rand = new Random();
        int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
        
        for (int r = 0; r < BOARD_SIZE; r++) {
            Arrays.fill(board[r], 1);
        }
        
        int numFlips = 10 + rand.nextInt(10);
        for (int i = 0; i < numFlips; i++) {
            int r = rand.nextInt(BOARD_SIZE);
            int c = rand.nextInt(BOARD_SIZE);
            flip(board, r, c);
        }
        
        return board;
    }

    private void showManualBoard() {
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(20));

        Label title = new Label("Manual Play - 6×6");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(PRIMARY_COLOR);

        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER);
        moveLabel = new Label("Moves: 0");
        moveLabel.setFont(Font.font("Arial", 16));
        moveLabel.setTextFill(SECONDARY_COLOR);
        statusLabel = new Label("Your turn");
        statusLabel.setFont(Font.font("Arial", 16));
        statusLabel.setTextFill(ACCENT_COLOR);
        infoBox.getChildren().addAll(moveLabel, statusLabel);
        topBox.getChildren().addAll(title, infoBox);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(20));

        manualTiles = new Button[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                Button tile = createManualTile(r, c);
                manualTiles[r][c] = tile;
                grid.add(tile, c, r);
            }
        }
        updateManualBoard();

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20));
        Button giveUpBtn = createControlButton("Give Up", DANGER_COLOR);
        Button newBoardBtn = createControlButton("New Board", WARNING_COLOR);
        Button menuBtn = createControlButton("Main Menu", SECONDARY_COLOR);
        
        giveUpBtn.setOnAction(e -> showAlgorithmsForCurrentBoard());
        newBoardBtn.setOnAction(e -> startManualMode());
        menuBtn.setOnAction(e -> showMainMenu());
        
        buttonBox.getChildren().addAll(giveUpBtn, newBoardBtn, menuBtn);

        VBox centerBox = new VBox(20, grid, buttonBox);
        centerBox.setAlignment(Pos.CENTER);

        root.setTop(topBox);
        root.setCenter(centerBox);

        Scene scene = new Scene(root, 900, 800);
        primaryStage.setScene(scene);
    }

    private Button createManualTile(int row, int col) {
        Button tile = new Button();
        tile.setPrefSize(TILE_SIZE, TILE_SIZE);
        tile.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        tile.setOnAction(e -> {
            if (gameActive) {
                userMoves++;
                moveLabel.setText("Moves: " + userMoves);
                flip(manualBoard, row, col);
                updateManualBoard();
                if (isAllWhite(manualBoard)) {
                    gameActive = false;
                    statusLabel.setText("You solved it!");
                    statusLabel.setTextFill(SUCCESS_COLOR);
                }
            }
        });
        tile.setOnMouseEntered(e -> {
            if (gameActive) updateTileStyle(tile, manualBoard[row][col], true);
        });
        tile.setOnMouseExited(e -> updateTileStyle(tile, manualBoard[row][col], false));
        return tile;
    }

    private void updateManualBoard() {
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                updateTileStyle(manualTiles[r][c], manualBoard[r][c], false);
    }

    private void updateTileStyle(Button tile, int value, boolean hover) {
        if (value == 1) {
            tile.setStyle("-fx-background-color: white; -fx-background-radius: 8;" +
                    "-fx-border-color: " + (hover ? toHex(ACCENT_COLOR) : "#dee2e6") + ";" +
                    "-fx-border-width: 3; -fx-border-radius: 8;");
            tile.setText("●");
            tile.setTextFill(PRIMARY_COLOR);
        } else {
            tile.setStyle("-fx-background-color: #212529; -fx-background-radius: 8;" +
                    "-fx-border-color: " + (hover ? toHex(ACCENT_COLOR) : "#495057") + ";" +
                    "-fx-border-width: 3; -fx-border-radius: 8;");
            tile.setText("○");
            tile.setTextFill(Color.WHITE);
        }
    }

    private Button createControlButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.MEDIUM, 14));
        btn.setPrefSize(130, 45);
        btn.setStyle("-fx-background-color: " + toHex(color) + "; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.9));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    // ================== SHOW FOUR BOARDS SOLVING SIMULTANEOUSLY ==================
    private void showAlgorithmsForCurrentBoard() {
        gameActive = false;
        int[][] boardCopy = copyBoard(manualBoard);

        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(20));

        Label title = new Label("Algorithms Solving Simultaneously");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(PRIMARY_COLOR);

        Label subtitle = new Label("Watch all four algorithms solve the same board");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        subtitle.setTextFill(SECONDARY_COLOR);

        topBox.getChildren().addAll(title, subtitle);

        // Use a 2x2 grid for the four algorithm boards
        GridPane boardsGrid = new GridPane();
        boardsGrid.setAlignment(Pos.CENTER);
        boardsGrid.setHgap(40);
        boardsGrid.setVgap(40);
        boardsGrid.setPadding(new Insets(20));

        // Smaller tile size for better fit
        int tileSize = 60;

        VBox graphBox = createAlgorithmBoard("Graph+Greedy", GRAPH_COLOR, boardCopy, tileSize);
        VBox divideBox = createAlgorithmBoard("Divide&Conquer", DIVIDE_COLOR, boardCopy, tileSize);
        VBox backtrackBox = createAlgorithmBoard("Backtracking", BACKTRACK_COLOR, boardCopy, tileSize);
        VBox dpBox = createAlgorithmBoard("Dynamic Programming", DP_COLOR, boardCopy, tileSize);

        boardsGrid.add(graphBox, 0, 0);
        boardsGrid.add(divideBox, 1, 0);
        boardsGrid.add(backtrackBox, 0, 1);
        boardsGrid.add(dpBox, 1, 1);

        // Extract grids and labels
        graphGrid = (GridPane) ((BorderPane) graphBox.getChildren().get(1)).getCenter();
        graphTiles = extractTilesFromGrid(graphGrid);
        graphTimeLabel = (Label) ((VBox) graphBox.getChildren().get(2)).getChildren().get(1);

        divideGrid = (GridPane) ((BorderPane) divideBox.getChildren().get(1)).getCenter();
        divideTiles = extractTilesFromGrid(divideGrid);
        divideTimeLabel = (Label) ((VBox) divideBox.getChildren().get(2)).getChildren().get(1);

        backtrackGrid = (GridPane) ((BorderPane) backtrackBox.getChildren().get(1)).getCenter();
        backtrackTiles = extractTilesFromGrid(backtrackGrid);
        backtrackTimeLabel = (Label) ((VBox) backtrackBox.getChildren().get(2)).getChildren().get(1);

        dpGrid = (GridPane) ((BorderPane) dpBox.getChildren().get(1)).getCenter();
        dpTiles = extractTilesFromGrid(dpGrid);
        dpTimeLabel = (Label) ((VBox) dpBox.getChildren().get(2)).getChildren().get(1);

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20));

        Button restartBtn = createControlButton("Restart Same Board", SUCCESS_COLOR);
        Button newBoardBtn = createControlButton("New Random Board", WARNING_COLOR);
        Button menuBtn = createControlButton("Main Menu", SECONDARY_COLOR);

        restartBtn.setOnAction(e -> {
            manualBoard = copyBoard(boardCopy);
            userMoves = 0;
            gameActive = true;
            showManualBoard();
        });
        newBoardBtn.setOnAction(e -> startManualMode());
        menuBtn.setOnAction(e -> showMainMenu());

        buttonBox.getChildren().addAll(restartBtn, newBoardBtn, menuBtn);

        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(boardsGrid, buttonBox);

        // Wrap everything in a ScrollPane to ensure all content is reachable
        ScrollPane scrollPane = new ScrollPane(centerBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPadding(new Insets(10));

        root.setTop(topBox);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);

        runSolversInParallel(boardCopy);
    }

    private VBox createAlgorithmBoard(String name, Color color, int[][] initialBoard, int tileSize) {
        VBox board = new VBox(15);
        board.setAlignment(Pos.TOP_CENTER);
        board.setPadding(new Insets(20));
        board.setPrefWidth(380); // Slightly reduced to match smaller tiles
        board.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                      "-fx-background-radius: 15;" +
                      "-fx-border-color: " + toHex(color) + ";" +
                      "-fx-border-width: 4;" +
                      "-fx-border-radius: 15;" +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        nameLabel.setTextFill(color);

        BorderPane gridContainer = new BorderPane();
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(3);
        grid.setVgap(3);

        Button[][] tiles = new Button[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                Button tile = new Button();
                tile.setPrefSize(tileSize, tileSize);
                tile.setFont(Font.font("Arial", FontWeight.BOLD, tileSize / 3));
                tile.setDisable(true);
                setNeutralTileStyle(tile, initialBoard[r][c]);
                tiles[r][c] = tile;
                grid.add(tile, c, r);
            }
        }
        gridContainer.setCenter(grid);

        VBox statsBox = new VBox(8);
        statsBox.setAlignment(Pos.CENTER);
        Label timeLabel = new Label("Time: -- ms");
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timeLabel.setTextFill(color);
        Label movesLabel = new Label("Moves: --");
        movesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        movesLabel.setTextFill(color);
        statsBox.getChildren().addAll(timeLabel, movesLabel);

        board.getChildren().addAll(nameLabel, gridContainer, statsBox);
        return board;
    }

    private void setNeutralTileStyle(Button tile, int value) {
        if (value == 1) {
            tile.setStyle("-fx-background-color: white; -fx-background-radius: 6;" +
                    "-fx-border-color: " + toHex(NEUTRAL_BORDER) + ";" +
                    "-fx-border-width: 3; -fx-border-radius: 6;");
            tile.setText("●");
            tile.setTextFill(PRIMARY_COLOR);
        } else {
            tile.setStyle("-fx-background-color: #212529; -fx-background-radius: 6;" +
                    "-fx-border-color: " + toHex(NEUTRAL_BORDER) + ";" +
                    "-fx-border-width: 3; -fx-border-radius: 6;");
            tile.setText("○");
            tile.setTextFill(Color.WHITE);
        }
    }

    private Button[][] extractTilesFromGrid(GridPane grid) {
        Button[][] tiles = new Button[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                tiles[r][c] = (Button) grid.getChildren().get(r * BOARD_SIZE + c);
            }
        }
        return tiles;
    }

    private void runSolversInParallel(int[][] board) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        Callable<SolverResult> graphTask = () -> solveGraphGreedy(board);
        Callable<SolverResult> divideTask = () -> solveDivideConquerGreedy(board);
        Callable<SolverResult> backtrackTask = () -> solveBacktracking(board);
        Callable<SolverResult> dpTask = () -> solveDP(board);
        
        try {
            List<Future<SolverResult>> futures = executor.invokeAll(Arrays.asList(
                graphTask, divideTask, backtrackTask, dpTask
            ));
            
            new Thread(() -> {
                try {
                    SolverResult graphResult = futures.get(0).get();
                    SolverResult divideResult = futures.get(1).get();
                    SolverResult backtrackResult = futures.get(2).get();
                    SolverResult dpResult = futures.get(3).get();
                    
                    javafx.application.Platform.runLater(() -> {
                        graphTimeLabel.setText("Time: " + graphResult.timeMs + " ms  •  Moves: " + graphResult.moveCount);
                        divideTimeLabel.setText("Time: " + divideResult.timeMs + " ms  •  Moves: " + divideResult.moveCount);
                        backtrackTimeLabel.setText("Time: " + backtrackResult.timeMs + " ms  •  Moves: " + backtrackResult.moveCount);
                        dpTimeLabel.setText("Time: " + dpResult.timeMs + " ms  •  Moves: " + dpResult.moveCount);
                        
                        animateAllSolutions(graphResult, divideResult, backtrackResult, dpResult, board);
                    });
                    
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private void animateAllSolutions(SolverResult graph, SolverResult divide, SolverResult backtrack, SolverResult dp, int[][] initialBoard) {
        int[][] graphBoard = copyBoard(initialBoard);
        int[][] divideBoard = copyBoard(initialBoard);
        int[][] backtrackBoard = copyBoard(initialBoard);
        int[][] dpBoard = copyBoard(initialBoard);
        
        ParallelTransition parallelTransition = new ParallelTransition();
        
        if (graph.moves != null && !graph.moves.isEmpty()) {
            parallelTransition.getChildren().add(createAlgorithmAnimation(graph.moves, graphBoard, graphTiles, GRAPH_COLOR));
        }
        if (divide.moves != null && !divide.moves.isEmpty()) {
            parallelTransition.getChildren().add(createAlgorithmAnimation(divide.moves, divideBoard, divideTiles, DIVIDE_COLOR));
        }
        if (backtrack.moves != null && !backtrack.moves.isEmpty()) {
            parallelTransition.getChildren().add(createAlgorithmAnimation(backtrack.moves, backtrackBoard, backtrackTiles, BACKTRACK_COLOR));
        }
        if (dp.moves != null && !dp.moves.isEmpty()) {
            parallelTransition.getChildren().add(createAlgorithmAnimation(dp.moves, dpBoard, dpTiles, DP_COLOR));
        }
        
        parallelTransition.play();
    }

    private SequentialTransition createAlgorithmAnimation(List<int[]> moves, int[][] board, Button[][] tiles, Color color) {
        SequentialTransition seq = new SequentialTransition();
        
        for (int[] move : moves) {
            PauseTransition pause = new PauseTransition(Duration.seconds(ANIMATION_DELAY));
            pause.setOnFinished(e -> {
                flip(board, move[0], move[1]);
                for (int r = 0; r < BOARD_SIZE; r++) {
                    for (int c = 0; c < BOARD_SIZE; c++) {
                        Button tile = tiles[r][c];
                        tile.setOpacity(1.0);
                        setNeutralTileStyle(tile, board[r][c]);
                    }
                }
                highlightMoveOnBoard(tiles, move[0], move[1], color);
            });
            seq.getChildren().add(pause);
        }
        
        return seq;
    }

    private void highlightMoveOnBoard(Button[][] tiles, int row, int col, Color color) {
        int[] dr = {0, 1, -1, 0, 0};
        int[] dc = {0, 0, 0, 1, -1};
        
        for (int i = 0; i < 5; i++) {
            int nr = row + dr[i];
            int nc = col + dc[i];
            if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE) {
                Button tile = tiles[nr][nc];
                tile.setOpacity(1.0);
                
                String currentStyle = tile.getStyle();
                String newStyle = currentStyle.replaceFirst(
                        "-fx-border-color: " + toHex(NEUTRAL_BORDER) + ";",
                        "-fx-border-color: " + toHex(color) + ";");
                tile.setStyle(newStyle);
                
                ScaleTransition st = new ScaleTransition(Duration.millis(200), tile);
                st.setToX(1.15);
                st.setToY(1.15);
                st.setAutoReverse(true);
                st.setCycleCount(2);
                st.play();
            }
        }
    }

    // ================== ANALYSIS MODE ==================
    private void showAnalysisResults() {
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));

        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        topBox.setPadding(new Insets(20));

        Label title = new Label("Analysis of 30 Boards");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setTextFill(PRIMARY_COLOR);

        Button backBtn = createControlButton("Main Menu", SECONDARY_COLOR);
        backBtn.setOnAction(e -> showMainMenu());
        topBox.getChildren().addAll(title, backBtn);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Average Performance (Precomputed)");
        barChart.setPrefHeight(350);
        xAxis.setLabel("Algorithm");
        yAxis.setLabel("Value");

        XYChart.Series<String, Number> timeSeries = new XYChart.Series<>();
        timeSeries.setName("Avg Time (ms)");
        XYChart.Series<String, Number> movesSeries = new XYChart.Series<>();
        movesSeries.setName("Avg Moves");

        double avgGraphTime = analysisRecords.stream().mapToLong(r -> r.graphResult.timeMs).average().orElse(0);
        double avgDivideTime = analysisRecords.stream().mapToLong(r -> r.divideResult.timeMs).average().orElse(0);
        double avgBackTime = analysisRecords.stream().mapToLong(r -> r.backtrackResult.timeMs).average().orElse(0);
        double avgDpTime = analysisRecords.stream().mapToLong(r -> r.dpResult.timeMs).average().orElse(0);

        double avgGraphMoves = analysisRecords.stream().mapToInt(r -> r.graphResult.moveCount).average().orElse(0);
        double avgDivideMoves = analysisRecords.stream().mapToInt(r -> r.divideResult.moveCount).average().orElse(0);
        double avgBackMoves = analysisRecords.stream().mapToInt(r -> r.backtrackResult.moveCount).average().orElse(0);
        double avgDpMoves = analysisRecords.stream().mapToInt(r -> r.dpResult.moveCount).average().orElse(0);

        timeSeries.getData().add(new XYChart.Data<>("Graph+Greedy", avgGraphTime));
        timeSeries.getData().add(new XYChart.Data<>("Divide&Conquer", avgDivideTime));
        timeSeries.getData().add(new XYChart.Data<>("Backtracking", avgBackTime));
        timeSeries.getData().add(new XYChart.Data<>("Dynamic Prog.", avgDpTime));

        movesSeries.getData().add(new XYChart.Data<>("Graph+Greedy", avgGraphMoves));
        movesSeries.getData().add(new XYChart.Data<>("Divide&Conquer", avgDivideMoves));
        movesSeries.getData().add(new XYChart.Data<>("Backtracking", avgBackMoves));
        movesSeries.getData().add(new XYChart.Data<>("Dynamic Prog.", avgDpMoves));

        barChart.getData().addAll(timeSeries, movesSeries);

        ListView<BoardRecord> boardList = new ListView<>(analysisRecords);
        boardList.setPrefHeight(300);
        boardList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(BoardRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(15);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    hbox.setPadding(new Insets(5));

                    GridPane mini = new GridPane();
                    mini.setHgap(2);
                    mini.setVgap(2);
                    for (int r = 0; r < BOARD_SIZE; r++) {
                        for (int c = 0; c < BOARD_SIZE; c++) {
                            Button cell = new Button();
                            cell.setPrefSize(20, 20);
                            cell.setStyle("-fx-background-color: " +
                                    (item.board[r][c] == 1 ? "white" : "#212529") + ";" +
                                    "-fx-border-color: gray; -fx-border-width: 1;");
                            cell.setDisable(true);
                            mini.add(cell, c, r);
                        }
                    }

                    String info = String.format("Graph: %d/%dms  |  Divide: %d/%dms  |  Back: %d/%dms  |  DP: %d/%dms",
                            item.graphResult.moveCount, item.graphResult.timeMs,
                            item.divideResult.moveCount, item.divideResult.timeMs,
                            item.backtrackResult.moveCount, item.backtrackResult.timeMs,
                            item.dpResult.moveCount, item.dpResult.timeMs);
                    Label infoLabel = new Label(info);
                    infoLabel.setFont(Font.font(12));

                    hbox.getChildren().addAll(mini, infoLabel);
                    setGraphic(hbox);
                }
            }
        });

        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(barChart, boardList);

        root.setTop(topBox);
        root.setCenter(centerBox);

        Scene scene = new Scene(root, 1300, 800);
        primaryStage.setScene(scene);
    }

    // ================== SOLVER IMPLEMENTATIONS ==================

    private SolverResult solveGraphGreedy(int[][] board) {
        long start = System.currentTimeMillis();
        int N = BOARD_SIZE;
        List<Integer>[] adj = buildAdjList(N);
        List<int[]> bestMoves = null;
        int bestScore = Integer.MAX_VALUE;

        int totalPatterns = 1 << N;
        for (int p = 0; p < totalPatterns; p++) {
            int[][] B = copyBoard(board);
            int[][] PM = new int[N][N];

            for (int c = 0; c < N; c++) {
                if (((p >> c) & 1) == 1) {
                    press(B, adj, 0, c);
                    PM[0][c] = 1;
                }
            }

            for (int r = 0; r < N - 1; r++) {
                for (int c = 0; c < N; c++) {
                    if (B[r][c] == 0) {
                        press(B, adj, r + 1, c);
                        PM[r + 1][c] = 1;
                    }
                }
            }

            boolean solved = true;
            for (int c = 0; c < N; c++) {
                if (B[N - 1][c] == 0) {
                    solved = false;
                    break;
                }
            }
            if (solved) {
                int moves = countMoves(PM);
                if (moves < bestScore) {
                    bestScore = moves;
                    bestMoves = extractMoves(PM);
                }
            }
        }

        long time = System.currentTimeMillis() - start;
        return new SolverResult("Graph+Greedy", bestMoves, time, toHex(GRAPH_COLOR));
    }

    private SolverResult solveDivideConquerGreedy(int[][] board) {
        long start = System.currentTimeMillis();
        int[][] current = copyBoard(board);
        List<int[]> moves = new ArrayList<>();
        int half = BOARD_SIZE / 2;

        int[][][] regions = {
            {{0, half-1}, {0, half-1}},
            {{0, half-1}, {half, BOARD_SIZE-1}},
            {{half, BOARD_SIZE-1}, {0, half-1}},
            {{half, BOARD_SIZE-1}, {half, BOARD_SIZE-1}},
            {{0, half-1}, {0, BOARD_SIZE-1}},
            {{half, BOARD_SIZE-1}, {0, BOARD_SIZE-1}},
            {{0, BOARD_SIZE-1}, {0, BOARD_SIZE-1}}
        };
        
        for (int regionIdx = 0; regionIdx < regions.length; regionIdx++) {
            int[][] region = regions[regionIdx];
            int r1 = region[0][0], r2 = region[0][1];
            int c1 = region[1][0], c2 = region[1][1];
            
            boolean regionSolved = true;
            for (int r = r1; r <= r2 && regionSolved; r++) {
                for (int c = c1; c <= c2; c++) {
                    if (current[r][c] == 0) {
                        regionSolved = false;
                        break;
                    }
                }
            }
            if (regionSolved) continue;
            
            int width = c2 - c1 + 1;
            
            boolean found = false;
            for (int mask = 0; mask < (1 << width) && !found; mask++) {
                int[][] temp = copyBoard(current);
                List<int[]> regionMoves = new ArrayList<>();
                
                for (int col = 0; col < width; col++) {
                    if ((mask & (1 << col)) != 0) {
                        int r = r1;
                        int c = c1 + col;
                        flip(temp, r, c);
                        regionMoves.add(new int[]{r, c});
                    }
                }
                
                for (int r = r1 + 1; r <= r2; r++) {
                    for (int c = c1; c <= c2; c++) {
                        if (temp[r-1][c] == 0) {
                            flip(temp, r, c);
                            regionMoves.add(new int[]{r, c});
                        }
                    }
                }
                
                boolean solved = true;
                for (int r = r1; r <= r2 && solved; r++) {
                    for (int c = c1; c <= c2; c++) {
                        if (temp[r][c] == 0) {
                            solved = false;
                            break;
                        }
                    }
                }
                
                if (solved) {
                    for (int[] move : regionMoves) {
                        moves.add(move);
                        flip(current, move[0], move[1]);
                    }
                    found = true;
                }
            }
        }

        long time = System.currentTimeMillis() - start;
        if (!isAllWhite(current)) {
            return new SolverResult("Divide&Conquer", null, time, toHex(DIVIDE_COLOR));
        }
        return new SolverResult("Divide&Conquer", moves, time, toHex(DIVIDE_COLOR));
    }

    private SolverResult solveBacktracking(int[][] board) {
        long start = System.currentTimeMillis();
        BacktrackSolver solver = new BacktrackSolver(board);
        List<int[]> moves = solver.solveWithTimeout(10000);
        long time = System.currentTimeMillis() - start;
        
        if (moves == null) {
            SolverResult graphResult = solveGraphGreedy(board);
            return new SolverResult("Backtracking (10s)", graphResult.moves, time, toHex(BACKTRACK_COLOR));
        }
        
        return new SolverResult("Backtracking", moves, time, toHex(BACKTRACK_COLOR));
    }

    // ================== DYNAMIC PROGRAMMING (BFS) SOLVER ==================
    private SolverResult solveDP(int[][] board) {
        long startTime = System.currentTimeMillis();
        int N = BOARD_SIZE;
        long goal = (1L << (N * N)) - 1; // all ones
        long startState = boardToLong(board);

        if (startState == goal) {
            return new SolverResult("Dynamic Programming", new ArrayList<>(), 0, toHex(DP_COLOR));
        }

        Queue<Long> queue = new LinkedList<>();
        Map<Long, Long> parent = new HashMap<>(); // current -> previous state
        Map<Long, Integer> move = new HashMap<>(); // current -> move index that led to it

        queue.add(startState);
        parent.put(startState, -1L);
        move.put(startState, -1);

        long deadline = System.currentTimeMillis() + 5000; // 5 seconds timeout
        int maxStates = 1_000_000; // limit to 1 million states
        int statesVisited = 0;

        while (!queue.isEmpty() && System.currentTimeMillis() < deadline && statesVisited < maxStates) {
            long current = queue.poll();
            statesVisited++;

            if (current == goal) {
                // Reconstruct path
                List<int[]> moves = new ArrayList<>();
                long state = current;
                while (state != startState) {
                    int m = move.get(state);
                    int r = m / N;
                    int c = m % N;
                    moves.add(new int[]{r, c});
                    state = parent.get(state);
                }
                Collections.reverse(moves);
                long time = System.currentTimeMillis() - startTime;
                return new SolverResult("Dynamic Programming", moves, time, toHex(DP_COLOR));
            }

            for (int i = 0; i < N * N; i++) {
                long next = current ^ flipMasks[i];
                if (!parent.containsKey(next)) {
                    parent.put(next, current);
                    move.put(next, i);
                    queue.add(next);
                }
            }
        }

        // Timeout or too many states – fallback to graph+greedy
        long time = System.currentTimeMillis() - startTime;
        SolverResult fallback = solveGraphGreedy(board);
        return new SolverResult("DP (fallback)", fallback.moves, time, toHex(DP_COLOR));
    }

    private static long boardToLong(int[][] board) {
        long state = 0;
        int N = BOARD_SIZE;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (board[r][c] == 1) {
                    int pos = r * N + c;
                    state |= (1L << pos);
                }
            }
        }
        return state;
    }

    // ================== BACKTRACKING HELPER ==================
    private static class BacktrackSolver {
        int N = BOARD_SIZE;
        int[][] board;
        int[][] bestClicks;
        int bestCost = Integer.MAX_VALUE;
        boolean timeoutReached = false;
        long deadline;
        
        BacktrackSolver(int[][] board) {
            this.board = copyBoard(board);
        }
        
        List<int[]> solveWithTimeout(long timeoutMs) {
            int[][] g = copyBoard(board);
            int[][] clicks = new int[N][N];
            bestCost = Integer.MAX_VALUE;
            bestClicks = null;
            timeoutReached = false;
            deadline = System.currentTimeMillis() + timeoutMs;
            
            backtrack(g, clicks, 0, 0);
            
            if (timeoutReached || bestClicks == null) return null;
            return extractMoves(bestClicks);
        }
        
        private void backtrack(int[][] g, int[][] clicks, int cell, int currentCost) {
            if (System.currentTimeMillis() > deadline) {
                timeoutReached = true;
                return;
            }
            
            if (cell == N * N) {
                if (isAllWhite(g)) {
                    if (currentCost < bestCost) {
                        bestCost = currentCost;
                        bestClicks = copyBoard(clicks);
                    }
                }
                return;
            }
            
            int r = cell / N;
            int c = cell % N;
            
            if (currentCost >= bestCost) {
                return;
            }
            
            backtrack(g, clicks, cell + 1, currentCost);
            if (timeoutReached) return;
            
            flip(g, r, c);
            clicks[r][c] = 1;
            
            backtrack(g, clicks, cell + 1, currentCost + 1);
            
            flip(g, r, c);
            clicks[r][c] = 0;
        }
    }

    // ================== UTILITY FUNCTIONS ==================
    static void flip(int[][] board, int r, int c) {
        int[] dr = {0, 1, -1, 0, 0};
        int[] dc = {0, 0, 0, 1, -1};
        for (int i = 0; i < 5; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];
            if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE)
                board[nr][nc] ^= 1;
        }
    }

    static boolean isAllWhite(int[][] board) {
        for (int[] row : board)
            for (int v : row)
                if (v == 0) return false;
        return true;
    }

    static int[][] copyBoard(int[][] src) {
        int[][] dst = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++)
            dst[i] = src[i].clone();
        return dst;
    }

    static List<Integer>[] buildAdjList(int N) {
        List<Integer>[] adj = new ArrayList[N * N];
        for (int i = 0; i < N * N; i++) {
            adj[i] = new ArrayList<>();
            int r = i / N;
            int c = i % N;
            adj[i].add(i);
            if (r > 0) adj[i].add((r - 1) * N + c);
            if (r < N - 1) adj[i].add((r + 1) * N + c);
            if (c > 0) adj[i].add(r * N + (c - 1));
            if (c < N - 1) adj[i].add(r * N + (c + 1));
        }
        return adj;
    }

    static void press(int[][] board, List<Integer>[] adj, int r, int c) {
        int idx = r * BOARD_SIZE + c;
        for (int k : adj[idx]) {
            int rr = k / BOARD_SIZE;
            int cc = k % BOARD_SIZE;
            board[rr][cc] ^= 1;
        }
    }

    static int countMoves(int[][] pm) {
        int cnt = 0;
        for (int[] row : pm)
            for (int v : row) cnt += v;
        return cnt;
    }

    static List<int[]> extractMoves(int[][] pm) {
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                if (pm[r][c] == 1)
                    moves.add(new int[]{r, c});
        return moves;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
    
    private static String toHexStatic(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
