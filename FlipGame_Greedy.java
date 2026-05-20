package demo;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class FlipGameFX extends Application {
    
    // ======================================================================
    // ORIGINAL GAME LOGIC CORE - MATHEMATICAL SOLVER IMPLEMENTATION
    // ======================================================================
    
    /**
     * Result container for simulation operations.
     * Contains whether the board was solved and the corresponding press matrix.
     */
    static class Result {
        boolean solved;
        int[][] PM;
        Result(boolean s, int[][] pm) {
            solved = s;
            PM = pm;
        }
    }
    
    /**
     * Solution container storing pattern, press matrix, and score.
     * Used for sorting and comparing different solving strategies.
     */
    static class Solution {
        int pattern;
        int[][] PM;
        int score;
        Solution(int p, int[][] pm, int s) {
            pattern = p;
            PM = pm;
            score = s;
        }
    }
    
    // Game configuration variables
    private static int N;                  // Board dimension (N x N)
    private static int SIZE;               // Total cells (N * N)
    private static List<Integer>[] adj;    // Adjacency list for each cell
    
    /**
     * Builds adjacency list for the grid.
     * Each cell is connected to itself and its 4 orthogonal neighbors.
     * This defines the "flip" pattern for each position.
     */
    static void buildAdjList() {
        SIZE = N * N;
        adj = new ArrayList[SIZE];

        for (int i = 0; i < SIZE; i++) {
            adj[i] = new ArrayList<>();

            int r = i / N;
            int c = i % N;

            adj[i].add(i);
            if (r > 0)     adj[i].add((r - 1) * N + c);
            if (r < N - 1) adj[i].add((r + 1) * N + c);
            if (c > 0)     adj[i].add(r * N + (c - 1));
            if (c < N - 1) adj[i].add(r * N + (c + 1));
        }
    }

    /**
     * Applies a press at position (r,c) toggling the cell and its neighbors.
     * Uses the adjacency list to determine which cells to flip.
     */
    static void press(int[][] B, int r, int c) {
        int idx = r * N + c;
        for (int k : adj[idx]) {
            int rr = k / N;
            int cc = k % N;
            B[rr][cc] ^= 1;
        }
    }

    /**
     * Creates a copy of a 2D integer array.
     */
    static int[][] copy(int[][] A) {
        int[][] B = new int[N][N];
        for (int i = 0; i < N; i++)
            B[i] = A[i].clone();
        return B;
    }

    /**
     * Simulates solving the board with a given first-row pattern.
     * Uses the "light chasing" algorithm: fix first row pattern,
     * then determine subsequent rows based on the state above.
     * Returns whether solution exists and the corresponding press matrix.
     */
    static Result simulatePattern(int[][] board, int pattern) {
        int[][] B = copy(board);
        int[][] PM = new int[N][N];

        for (int c = 0; c < N; c++) {
            if (((pattern >> c) & 1) == 1) {
                press(B, 0, c);
                PM[0][c] = 1;
            }
        }

        for (int r = 0; r < N - 1; r++) {
            for (int c = 0; c < N; c++) {
                if (B[r][c] == 0) {
                    press(B, r + 1, c);
                    PM[r + 1][c] = 1;
                }
            }
        }

        for (int c = 0; c < N; c++)
            if (B[N - 1][c] == 0)
                return new Result(false, PM);

        return new Result(true, PM);
    }

    /**
     * Calculates score for a press matrix.
     * Lower (more negative) scores indicate more presses.
     * Used to prefer solutions with fewer moves.
     */
    static int score(int[][] PM) {
        int count = 0;
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (PM[r][c] == 1)
                    count++;
        return -count;
    }

    /**
     * Merge sort implementation for sorting solutions by score.
     * Sorts in descending order (best score first).
     */
    static void mergeSort(List<Solution> arr) {
        if (arr.size() <= 1)
            return;

        int mid = arr.size() / 2;
        List<Solution> L = new ArrayList<>(arr.subList(0, mid));
        List<Solution> R = new ArrayList<>(arr.subList(mid, arr.size()));

        mergeSort(L);
        mergeSort(R);

        merge(arr, L, R);
    }

    static void merge(List<Solution> arr, List<Solution> L, List<Solution> R) {
        int i = 0, j = 0, k = 0;

        while (i < L.size() && j < R.size()) {
            if (L.get(i).score >= R.get(j).score)
                arr.set(k++, L.get(i++));
            else
                arr.set(k++, R.get(j++));
        }

        while (i < L.size())
            arr.set(k++, L.get(i++));
        while (j < R.size())
            arr.set(k++, R.get(j++));
    }

    /**
     * Finds all possible solutions for the given board state.
     * Tests all 2^N first-row patterns and returns valid solutions.
     */
    static List<Solution> findSolutions(int[][] board) {
        List<Solution> sols = new ArrayList<>();
        int totalPatterns = 1 << N;

        for (int p = 0; p < totalPatterns; p++) {
            Result r = simulatePattern(board, p);
            if (r.solved) {
                sols.add(new Solution(p, r.PM, score(r.PM)));
            }
        }
        return sols;
    }

    /**
     * Extracts individual moves from a press matrix.
     * Converts the matrix representation to a list of (row,col) coordinates.
     */
    static List<int[]> extractMoves(int[][] PM) {
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (PM[r][c] == 1)
                    moves.add(new int[]{r, c});
        return moves;
    }

    /**
     * Checks if the board is completely solved (all cells are 1).
     */
    static boolean isSolved(int[][] B) {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (B[r][c] == 0)
                    return false;
        return true;
    }

    /**
     * Tests if pressing at (r,c) would immediately solve the board.
     * Used by computer to avoid making the final winning move.
     */
    static boolean wouldSolve(int[][] B, int r, int c) {
        int[][] T = copy(B);
        press(T, r, c);
        return isSolved(T);
    }
    
    // ======================================================================
    // GAME STATE MANAGEMENT - UNDO FUNCTIONALITY
    // ======================================================================
    
    /**
     * Represents a complete game state for undo functionality.
     * Stores board configuration, move counts, and turn information.
     */
    private static class GameState {
        int[][] board;
        int userMoves;
        int computerMoves;
        boolean userTurn;
        
        GameState(int[][] board, int userMoves, int computerMoves, boolean userTurn) {
            this.board = copy(board);
            this.userMoves = userMoves;
            this.computerMoves = computerMoves;
            this.userTurn = userTurn;
        }
    }
    
    // ======================================================================
    // UI COMPONENTS AND STATE VARIABLES
    // ======================================================================
    
    private int[][] board;                    // Current game board
    private Button[][] tiles;                 // UI tile buttons
    private Label statusLabel;                // Game status display
    private Label moveLabel;                  // Move counter display
    private GridPane gameGrid;                // Main game grid container
    private boolean userTurn = true;          // Whose turn it is
    private boolean gameActive = false;       // Whether game is in progress
    private int userMoves = 0;                // User move count
    private int computerMoves = 0;            // Computer move count
    private StackPane root;                   // Root container
    private Stack<GameState> undoStack = new Stack<>(); // Undo history
    
    // ======================================================================
    // UI COLOR SCHEME - MODERN, ELEGANT DESIGN
    // ======================================================================
    
    private Color BACKGROUND = Color.rgb(248, 249, 250);
    private Color CARD_BACKGROUND = Color.WHITE;
    private Color PRIMARY_COLOR = Color.rgb(33, 37, 41);
    private Color SECONDARY_COLOR = Color.rgb(108, 117, 125);
    private Color ACCENT_COLOR = Color.rgb(13, 110, 253);
    private Color SUCCESS_COLOR = Color.rgb(25, 135, 84);
    private Color WARNING_COLOR = Color.rgb(255, 193, 7);
    private Color DANGER_COLOR = Color.rgb(220, 53, 69);
    private Color BORDER_COLOR = Color.rgb(222, 226, 230);
    private Color TEXT_COLOR = Color.rgb(33, 37, 41);

    @Override
    public void start(Stage primaryStage) {
        showMainMenu(primaryStage);
    }

    // ======================================================================
    // MAIN MENU SCREEN - GAME INTRODUCTION AND DIFFICULTY SELECTION
    // ======================================================================
    
    /**
     * Displays the main menu with game rules and difficulty selection.
     * Provides entry point to start new games at different difficulty levels.
     */
    private void showMainMenu(Stage stage) {
        VBox menu = new VBox(30);
        menu.setAlignment(Pos.CENTER);
        menu.setBackground(new Background(new BackgroundFill(
            BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        menu.setPadding(new Insets(40));

        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);
        
        Label title = new Label("FLIP");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 72));
        title.setTextFill(PRIMARY_COLOR);
        
        Label subtitle = new Label("A Logic Puzzle Game");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        subtitle.setTextFill(SECONDARY_COLOR);
        
        titleBox.getChildren().addAll(title, subtitle);

        VBox rulesCard = new VBox(20);
        rulesCard.setAlignment(Pos.CENTER);
        rulesCard.setPadding(new Insets(30));
        rulesCard.setMaxWidth(500);
        rulesCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                         "-fx-background-radius: 12;" +
                         "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                         "-fx-border-width: 1;" +
                         "-fx-border-radius: 12;");
        
        Label rulesTitle = new Label("How to Play");
        rulesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        rulesTitle.setTextFill(PRIMARY_COLOR);
        
        VBox rulesList = new VBox(8);
        rulesList.setAlignment(Pos.CENTER_LEFT);
        
        String[] rules = {
            "• Click any tile to flip it and its neighbors",
            "• You and the computer take turns",
            "• Turn all tiles to the same state to win",
            "• The computer will never make the final winning move",
            "• Use the hint button if you're stuck",
            "• Undo button lets you take back moves"
        };
        
        for (String rule : rules) {
            Label ruleLabel = new Label(rule);
            ruleLabel.setFont(Font.font("Arial", 16));
            ruleLabel.setTextFill(TEXT_COLOR);
            rulesList.getChildren().add(ruleLabel);
        }
        
        rulesCard.getChildren().addAll(rulesTitle, rulesList);

        Label selectLabel = new Label("Select Difficulty");
        selectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        selectLabel.setTextFill(PRIMARY_COLOR);

        HBox difficultyBox = new HBox(20);
        difficultyBox.setAlignment(Pos.CENTER);
        
        Button easyBtn = createDifficultyButton("Easy", "3×3");
        Button mediumBtn = createDifficultyButton("Medium", "5×5");
        Button hardBtn = createDifficultyButton("Hard", "9×9");

        easyBtn.setOnAction(e -> startGame(stage, 3));
        mediumBtn.setOnAction(e -> startGame(stage, 5));
        hardBtn.setOnAction(e -> startGame(stage, 9));

        difficultyBox.getChildren().addAll(easyBtn, mediumBtn, hardBtn);

        menu.getChildren().addAll(titleBox, rulesCard, selectLabel, difficultyBox);

        Scene scene = new Scene(menu, 700, 700);
        stage.setScene(scene);
        stage.setTitle("Flip Game");
        stage.show();
    }

    /**
     * Creates a styled difficulty selection button with main text and subtext.
     */
    private Button createDifficultyButton(String text, String size) {
        VBox buttonContent = new VBox(5);
        buttonContent.setAlignment(Pos.CENTER);
        
        Label mainText = new Label(text);
        mainText.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        mainText.setTextFill(ACCENT_COLOR);
        
        Label sizeText = new Label(size);
        sizeText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        sizeText.setTextFill(SECONDARY_COLOR);
        
        buttonContent.getChildren().addAll(mainText, sizeText);
        
        Button btn = new Button();
        btn.setGraphic(buttonContent);
        btn.setPrefSize(180, 80);
        btn.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 10;" +
                    "-fx-cursor: hand;");
        
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: " + toHex(BACKGROUND) + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + toHex(ACCENT_COLOR) + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;");
        });
        
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 10;" +
                        "-fx-cursor: hand;");
        });
        
        return btn;
    }
    
    /**
     * Converts JavaFX Color to hex string for CSS styling.
     */
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    // ======================================================================
    // GAME INITIALIZATION - BOARD SETUP AND UI CONFIGURATION
    // ======================================================================
    
    /**
     * Initializes a new game with specified grid size.
     * Sets up the game board, UI components, and game state.
     */
    private void startGame(Stage stage, int gridSize) {
        N = gridSize;
        buildAdjList();
        
        board = new int[N][N];
        Random rand = new Random();
        
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                board[r][c] = 1;
        
        for (int i = 0; i < N * N / 2 + rand.nextInt(N); i++) {
            press(board, rand.nextInt(N), rand.nextInt(N));
        }
        
        userMoves = 0;
        computerMoves = 0;
        userTurn = true;
        gameActive = true;
        undoStack.clear();
        
        saveGameState();
        
        root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(
            BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        
        VBox gameContainer = new VBox(20);
        gameContainer.setAlignment(Pos.CENTER);
        gameContainer.setPadding(new Insets(20));
        
        VBox headerCard = new VBox(10);
        headerCard.setAlignment(Pos.CENTER);
        headerCard.setPadding(new Insets(15, 30, 15, 30));
        headerCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                           "-fx-background-radius: 10;" +
                           "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                           "-fx-border-width: 1;" +
                           "-fx-border-radius: 10;");
        
        Label title = new Label("Flip Game - " + 
            (N == 3 ? "Easy" : N == 5 ? "Medium" : "Hard"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(PRIMARY_COLOR);
        
        HBox infoBox = new HBox(30);
        infoBox.setAlignment(Pos.CENTER);
        
        moveLabel = new Label("Your turn • Moves: 0 • Computer: 0");
        moveLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        moveLabel.setTextFill(SECONDARY_COLOR);
        
        statusLabel = new Label("Click any tile to begin");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        statusLabel.setTextFill(SECONDARY_COLOR);
        
        infoBox.getChildren().addAll(moveLabel, statusLabel);
        headerCard.getChildren().addAll(title, infoBox);
        
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(5);
        gameGrid.setVgap(5);
        gameGrid.setPadding(new Insets(20));
        
        int tileSize = Math.max(40, 450 / N);
        createGameBoard(tileSize);
        
        HBox controlsCard = new HBox(15);
        controlsCard.setAlignment(Pos.CENTER);
        controlsCard.setPadding(new Insets(15));
        controlsCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                             "-fx-background-radius: 10;" +
                             "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                             "-fx-border-width: 1;" +
                             "-fx-border-radius: 10;");
        
        Button undoBtn = createControlButton("Undo", PRIMARY_COLOR);
        Button hintBtn = createControlButton("Hint", SUCCESS_COLOR);
        Button solveBtn = createControlButton("Solve", ACCENT_COLOR);
        Button newBtn = createControlButton("New Game", WARNING_COLOR);
        Button menuBtn = createControlButton("Menu", DANGER_COLOR);
        
        undoBtn.setOnAction(e -> undoMove());
        hintBtn.setOnAction(e -> showHint());
        solveBtn.setOnAction(e -> autoSolve());
        newBtn.setOnAction(e -> startGame(stage, N));
        menuBtn.setOnAction(e -> showMainMenu(stage));
        
        controlsCard.getChildren().addAll(undoBtn, hintBtn, solveBtn, newBtn, menuBtn);
        
        gameContainer.getChildren().addAll(headerCard, gameGrid, controlsCard);
        root.getChildren().add(gameContainer);
        
        Scene scene = new Scene(root, Math.max(700, N * tileSize + 200), 
                                    Math.max(700, N * tileSize + 300));
        stage.setScene(scene);
    }
    
    /**
     * Creates a styled control button with hover effects.
     */
    private Button createControlButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.MEDIUM, 13));
        btn.setPrefSize(110, 35);
        btn.setStyle("-fx-background-color: " + toHex(color) + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 6;" +
                    "-fx-cursor: hand;");
        
        btn.setOnMouseEntered(e -> {
            btn.setOpacity(0.9);
        });
        
        btn.setOnMouseExited(e -> {
            btn.setOpacity(1.0);
        });
        
        return btn;
    }

    // ======================================================================
    // GAME BOARD UI CREATION - TILE MANAGEMENT
    // ======================================================================
    
    /**
     * Creates the game board UI with interactive tiles.
     * Each tile represents a cell on the game board with visual feedback.
     */
    private void createGameBoard(int tileSize) {
        tiles = new Button[N][N];
        
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                final int row = r;
                final int col = c;
                
                Button tile = new Button();
                tile.setPrefSize(tileSize, tileSize);
                tile.setFont(Font.font("Arial", FontWeight.BOLD, 
                    Math.max(14, tileSize / 3)));
                updateTileStyle(tile, board[r][c], false);
                
                tile.setOnAction(e -> {
                    if (gameActive && userTurn) {
                        handleUserMove(row, col);
                    }
                });
                
                tile.setOnMouseEntered(e -> {
                    if (gameActive && userTurn) {
                        updateTileStyle(tile, board[row][col], true);
                    }
                });
                
                tile.setOnMouseExited(e -> {
                    updateTileStyle(tile, board[row][col], false);
                });
                
                tiles[r][c] = tile;
                gameGrid.add(tile, c, r);
            }
        }
    }
    
    /**
     * Updates the visual style of a tile based on its state.
     * ON state (1): White with dark symbol
     * OFF state (0): Dark with white symbol
     * Hover state: Changes border color for interactivity
     */
    private void updateTileStyle(Button tile, int value, boolean hover) {
        if (value == 1) {
            tile.setStyle("-fx-background-color: white;" +
                         "-fx-background-radius: 6;" +
                         "-fx-border-color: " + (hover ? toHex(ACCENT_COLOR) : "#dee2e6") + ";" +
                         "-fx-border-width: 2;" +
                         "-fx-border-radius: 6;");
            tile.setText("●");
            tile.setTextFill(PRIMARY_COLOR);
        } else {
            tile.setStyle("-fx-background-color: #212529;" +
                         "-fx-background-radius: 6;" +
                         "-fx-border-color: " + (hover ? toHex(ACCENT_COLOR) : "#495057") + ";" +
                         "-fx-border-width: 2;" +
                         "-fx-border-radius: 6;");
            tile.setText("○");
            tile.setTextFill(Color.WHITE);
        }
    }

    // ======================================================================
    // GAME LOGIC - MOVE HANDLING AND TURN MANAGEMENT
    // ======================================================================
    
    /**
     * Saves current game state to undo stack.
     * Called before any move to enable undo functionality.
     */
    private void saveGameState() {
        undoStack.push(new GameState(board, userMoves, computerMoves, userTurn));
    }
    
    /**
     * Reverts to previous game state from undo stack.
     * Restores board configuration, move counts, and turn.
     */
    private void undoMove() {
        if (undoStack.size() <= 1 || !gameActive) {
            statusLabel.setText("Cannot undo");
            statusLabel.setTextFill(DANGER_COLOR);
            return;
        }
        
        undoStack.pop();
        
        GameState prevState = undoStack.peek();
        
        board = copy(prevState.board);
        userMoves = prevState.userMoves;
        computerMoves = prevState.computerMoves;
        userTurn = prevState.userTurn;
        gameActive = true;
        
        updateBoard();
        updateStatus();
        
        statusLabel.setText("Move undone");
        statusLabel.setTextFill(ACCENT_COLOR);
    }
    
    /**
     * Handles user move input on a specific tile.
     * Applies the move, updates game state, and triggers computer response.
     */
    private void handleUserMove(int row, int col) {
        if (!gameActive || !userTurn) return;
        
        saveGameState();
        
        userTurn = false;
        userMoves++;
        updateStatus();
        
        highlightMove(row, col, ACCENT_COLOR);
        press(board, row, col);
        updateBoard();
        
        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
        pause.setOnFinished(e -> {
            if (isSolved(board)) {
                showVictory(true);
            } else {
                computerMove();
            }
        });
        pause.play();
    }
    
    /**
     * Executes computer's move using the mathematical solver.
     * Finds optimal solution, avoids winning move, and applies best move.
     */
    private void computerMove() {
        statusLabel.setText("Computer thinking...");
        statusLabel.setTextFill(SECONDARY_COLOR);
        
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            List<Solution> sols = findSolutions(board);
            
            if (sols.isEmpty()) {
                statusLabel.setText("No solution found");
                statusLabel.setTextFill(DANGER_COLOR);
                return;
            }
            
            mergeSort(sols);
            Solution best = sols.get(0);
            List<int[]> moves = extractMoves(best.PM);
            
            if (moves.isEmpty()) {
                showVictory(true);
                return;
            }
            
            int[] mv = moves.get(0);
            
            if (wouldSolve(board, mv[0], mv[1])) {
                statusLabel.setText("Final move is yours!");
                statusLabel.setTextFill(WARNING_COLOR);
                highlightMove(mv[0], mv[1], WARNING_COLOR);
                userTurn = true;
                updateStatus();
                return;
            }
            
            saveGameState();
            
            computerMoves++;
            updateStatus();
            highlightMove(mv[0], mv[1], PRIMARY_COLOR);
            press(board, mv[0], mv[1]);
            updateBoard();
            
            PauseTransition pause2 = new PauseTransition(Duration.seconds(0.5));
            pause2.setOnFinished(ev -> {
                if (isSolved(board)) {
                    showVictory(false);
                } else {
                    userTurn = true;
                    updateStatus();
                }
            });
            pause2.play();
        });
        pause.play();
    }
    
    /**
     * Highlights a move by animating the affected tiles.
     * Shows visual feedback for which tiles were flipped.
     */
    private void highlightMove(int row, int col, Color color) {
        int idx = row * N + col;
        for (int k : adj[idx]) {
            int rr = k / N;
            int cc = k % N;
            Button tile = tiles[rr][cc];
            
            String currentStyle = tile.getStyle();
            String borderColor = toHex(color);
            String newStyle = currentStyle.replaceFirst(
                "-fx-border-color: #[0-9a-fA-F]{6};", 
                "-fx-border-color: " + borderColor + ";");
            tile.setStyle(newStyle);
            
            ScaleTransition st = new ScaleTransition(Duration.millis(150), tile);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        }
        
        PauseTransition pause = new PauseTransition(Duration.seconds(0.3));
        pause.setOnFinished(e -> {
            for (int k : adj[idx]) {
                int rr = k / N;
                int cc = k % N;
                updateTileStyle(tiles[rr][cc], board[rr][cc], false);
            }
        });
        pause.play();
    }
    
    /**
     * Updates all tiles to reflect current board state.
     */
    private void updateBoard() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                updateTileStyle(tiles[r][c], board[r][c], false);
            }
        }
    }
    
    /**
     * Updates status labels with current game information.
     */
    private void updateStatus() {
        String turn = userTurn ? "Your turn" : "Computer's turn";
        moveLabel.setText(turn + " • Moves: " + userMoves + " • Computer: " + computerMoves);
        statusLabel.setText(userTurn ? "Make your move" : "Computer thinking...");
        statusLabel.setTextFill(userTurn ? ACCENT_COLOR : SECONDARY_COLOR);
    }

    // ======================================================================
    // GAME ASSISTANCE FEATURES - HINTS AND AUTO-SOLVE
    // ======================================================================
    
    /**
     * Shows a hint by highlighting the optimal next move.
     * Uses the mathematical solver to determine best move.
     */
    private void showHint() {
        if (!gameActive) return;
        
        List<Solution> sols = findSolutions(board);
        if (sols.isEmpty()) return;
        
        mergeSort(sols);
        Solution best = sols.get(0);
        List<int[]> moves = extractMoves(best.PM);
        
        if (!moves.isEmpty()) {
            int[] hint = moves.get(0);
            statusLabel.setText("Hint: Try tile (" + hint[0] + ", " + hint[1] + ")");
            statusLabel.setTextFill(SUCCESS_COLOR);
            
            flashTile(hint[0], hint[1]);
        }
    }
    
    /**
     * Flashes a tile to draw attention to it.
     * Used for hint display and move highlighting.
     */
    private void flashTile(int r, int c) {
        Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                tiles[r][c].setStyle(tiles[r][c].getStyle().replaceFirst(
                    "-fx-border-color: #[0-9a-fA-F]{6};", 
                    "-fx-border-color: " + toHex(WARNING_COLOR) + ";"));
            }),
            new KeyFrame(Duration.millis(300), e -> updateTileStyle(tiles[r][c], board[r][c], false)),
            new KeyFrame(Duration.millis(600), e -> {
                tiles[r][c].setStyle(tiles[r][c].getStyle().replaceFirst(
                    "-fx-border-color: #[0-9a-fA-F]{6};", 
                    "-fx-border-color: " + toHex(WARNING_COLOR) + ";"));
            }),
            new KeyFrame(Duration.millis(900), e -> updateTileStyle(tiles[r][c], board[r][c], false))
        );
        flash.play();
    }
    
    /**
     * Automatically solves the puzzle by animating all optimal moves.
     * Shows the complete solution sequence visually.
     */
    private void autoSolve() {
        if (!gameActive) return;
        
        gameActive = false;
        statusLabel.setText("Solving...");
        statusLabel.setTextFill(SUCCESS_COLOR);
        
        List<Solution> sols = findSolutions(board);
        if (sols.isEmpty()) return;
        
        mergeSort(sols);
        Solution best = sols.get(0);
        List<int[]> moves = extractMoves(best.PM);
        
        if (!moves.isEmpty()) {
            animateSolution(moves);
        }
    }
    
    /**
     * Animates the solution sequence step by step.
     * Each move is highlighted and applied with a delay.
     */
    private void animateSolution(List<int[]> moves) {
        SequentialTransition sequence = new SequentialTransition();
        
        for (int i = 0; i < moves.size(); i++) {
            final int idx = i;
            PauseTransition pause = new PauseTransition(Duration.millis(400));
            pause.setOnFinished(e -> {
                if (idx < moves.size()) {
                    int[] move = moves.get(idx);
                    press(board, move[0], move[1]);
                    updateBoard();
                    highlightMove(move[0], move[1], SUCCESS_COLOR);
                }
            });
            sequence.getChildren().add(pause);
        }
        
        sequence.setOnFinished(e -> {
            showVictory(true);
        });
        
        sequence.play();
    }

    // ======================================================================
    // VICTORY/END SCREEN - GAME COMPLETION DISPLAY
    // ======================================================================
    
    /**
     * Displays victory or game over screen with statistics.
     * Shows move counts and provides options to replay or return to menu.
     */
    private void showVictory(boolean userWon) {
        gameActive = false;
        
        StackPane victoryOverlay = new StackPane();
        victoryOverlay.setBackground(new Background(new BackgroundFill(
            Color.rgb(0, 0, 0, 0.5), CornerRadii.EMPTY, Insets.EMPTY)));
        
        VBox victoryCard = new VBox(25);
        victoryCard.setAlignment(Pos.CENTER);
        victoryCard.setPadding(new Insets(40));
        victoryCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                           "-fx-background-radius: 15;" +
                           "-fx-border-color: " + (userWon ? toHex(SUCCESS_COLOR) : toHex(DANGER_COLOR)) + ";" +
                           "-fx-border-width: 2;" +
                           "-fx-border-radius: 15;");
        
        Label victoryLabel = new Label(userWon ? "Victory" : "Game Over");
        victoryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        victoryLabel.setTextFill(userWon ? SUCCESS_COLOR : DANGER_COLOR);
        
        Label messageLabel = new Label(userWon ? 
            "You solved the puzzle successfully." : 
            "The computer solved the puzzle.");
        messageLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        messageLabel.setTextFill(TEXT_COLOR);
        
        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER);
        
        Label userLabel = new Label("Your moves: " + userMoves);
        userLabel.setFont(Font.font("Arial", FontWeight.MEDIUM, 16));
        userLabel.setTextFill(ACCENT_COLOR);
        
        Label computerLabel = new Label("Computer moves: " + computerMoves);
        computerLabel.setFont(Font.font("Arial", FontWeight.MEDIUM, 16));
        computerLabel.setTextFill(PRIMARY_COLOR);
        
        Label totalLabel = new Label("Total moves: " + (userMoves + computerMoves));
        totalLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        totalLabel.setTextFill(TEXT_COLOR);
        
        statsBox.getChildren().addAll(userLabel, computerLabel, totalLabel);
        
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button playAgainBtn = new Button("Play Again");
        playAgainBtn.setFont(Font.font("Arial", FontWeight.MEDIUM, 14));
        playAgainBtn.setPrefSize(140, 40);
        playAgainBtn.setStyle("-fx-background-color: " + toHex(ACCENT_COLOR) + ";" +
                             "-fx-text-fill: white;" +
                             "-fx-background-radius: 6;" +
                             "-fx-cursor: hand;");
        
        Button menuBtn = new Button("Main Menu");
        menuBtn.setFont(Font.font("Arial", FontWeight.MEDIUM, 14));
        menuBtn.setPrefSize(140, 40);
        menuBtn.setStyle("-fx-background-color: " + toHex(SECONDARY_COLOR) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;");
        
        Stage stage = (Stage) root.getScene().getWindow();
        playAgainBtn.setOnAction(e -> startGame(stage, N));
        menuBtn.setOnAction(e -> showMainMenu(stage));
        
        buttonBox.getChildren().addAll(playAgainBtn, menuBtn);
        
        victoryCard.getChildren().addAll(victoryLabel, messageLabel, statsBox, buttonBox);
        victoryOverlay.getChildren().add(victoryCard);
        
        root.getChildren().add(victoryOverlay);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), victoryCard);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
