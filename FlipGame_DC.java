package pck;

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

public class FlipGame_DC extends Application {
    
    // ======================================================================
    // GAME CONFIGURATION
    // ======================================================================
    
    private static int N;                          // Board size (must be even)
    private int[][] board;                          // Current game board
    private Button[][] tiles;                        // UI tile buttons
    private Label statusLabel;                       // Game status display
    private Label moveLabel;                          // Move counter display
    private Label phaseLabel;                         // Current solving phase display
    private GridPane gameGrid;                        // Main game grid container
    private boolean userTurn = true;                   // Whose turn it is
    private boolean gameActive = false;                 // Whether game is in progress
    private int userMoves = 0;                          // User move count
    private int computerMoves = 0;                       // Computer move count
    private StackPane root;                              // Root container
    private Stack<GameState> undoStack = new Stack<>();   // Undo history
    private Button undoButton;                            // Reference to undo button (to disable during computer turn)
    
    // ======================================================================
    // STATE FOR COMPUTER'S PROGRESS THROUGH REGIONS
    // ======================================================================
    
    private enum Phase { SQUARES, HALVES, FULL }
    private Phase currentPhase;
    private int currentRegionIndex; // for squares: 0=TL,1=TR,2=BL,3=BR; for halves: 0=top,1=bottom
    
    // ======================================================================
    // GAME STATE FOR UNDO
    // ======================================================================
    
    private static class GameState {
        int[][] board;
        int userMoves;
        int computerMoves;
        boolean userTurn;
        Phase currentPhase;
        int currentRegionIndex;
        
        GameState(int[][] board, int userMoves, int computerMoves, boolean userTurn,
                  Phase currentPhase, int currentRegionIndex) {
            this.board = copy(board);
            this.userMoves = userMoves;
            this.computerMoves = computerMoves;
            this.userTurn = userTurn;
            this.currentPhase = currentPhase;
            this.currentRegionIndex = currentRegionIndex;
        }
    }
    
    // ======================================================================
    // UI COLOR SCHEME
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
    private Color PHASE_1_COLOR = Color.rgb(255, 99, 132);   // Pink for squares
    private Color PHASE_2_COLOR = Color.rgb(54, 162, 235);   // Blue for halves
    private Color PHASE_3_COLOR = Color.rgb(255, 206, 86);   // Yellow for full

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

        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER);
        
        Label title = new Label("FLIP");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 72));
        title.setTextFill(PRIMARY_COLOR);
        
        Label subtitle = new Label("A Logic Puzzle Game - Even Boards Only");
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
            "• You and the computer take turns (one move each)",
            "• Turn all tiles to WHITE (1) to win",
            "• Computer solves in phases: squares → halves → full",
            "• Once the computer starts a region, it sticks to it until solved",
            "• Board size must be even (4×4, 6×6, or 10×10)",
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
        
        Button easyBtn = createDifficultyButton("Easy", "4×4");
        Button mediumBtn = createDifficultyButton("Medium", "6×6");
        Button hardBtn = createDifficultyButton("Hard", "10×10");

        easyBtn.setOnAction(e -> startGame(stage, 4));
        mediumBtn.setOnAction(e -> startGame(stage, 6));
        hardBtn.setOnAction(e -> startGame(stage, 10));

        difficultyBox.getChildren().addAll(easyBtn, mediumBtn, hardBtn);

        menu.getChildren().addAll(titleBox, rulesCard, selectLabel, difficultyBox);

        Scene scene = new Scene(menu, 700, 700);
        stage.setScene(scene);
        stage.setTitle("Flip Game - Even Boards Only");
        stage.show();
    }

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
    
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    // ======================================================================
    // GAME INITIALIZATION
    // ======================================================================
    
    private void startGame(Stage stage, int gridSize) {
        N = gridSize;  // N is always even
        
        // Initialize board with random state
        board = new int[N][N];
        Random rand = new Random();
        
        // Start with all white
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                board[r][c] = 1;
        
        // Add random flips to create interesting puzzle
        int flips = N * 2 + rand.nextInt(N);
        for (int i = 0; i < flips; i++) {
            flip(board, rand.nextInt(N), rand.nextInt(N));
        }
        
        userMoves = 0;
        computerMoves = 0;
        userTurn = true;
        gameActive = true;
        undoStack.clear();               // Clear old stack
        
        // Initialize computer's region progress
        currentPhase = Phase.SQUARES;
        currentRegionIndex = 0;
        
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
            (N == 4 ? "Easy (4×4)" : N == 6 ? "Medium (6×6)" : "Hard (10×10)"));
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(PRIMARY_COLOR);
        
        HBox infoBox = new HBox(30);
        infoBox.setAlignment(Pos.CENTER);
        
        VBox leftInfo = new VBox(5);
        leftInfo.setAlignment(Pos.CENTER_LEFT);
        
        moveLabel = new Label("Your turn • Moves: 0 • Computer: 0");
        moveLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        moveLabel.setTextFill(SECONDARY_COLOR);
        
        phaseLabel = new Label("Current phase: Starting");
        phaseLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        phaseLabel.setTextFill(SECONDARY_COLOR);
        
        leftInfo.getChildren().addAll(moveLabel, phaseLabel);
        
        statusLabel = new Label("Click any tile to begin");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        statusLabel.setTextFill(SECONDARY_COLOR);
        
        infoBox.getChildren().addAll(leftInfo, statusLabel);
        headerCard.getChildren().addAll(title, infoBox);
        
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(5);
        gameGrid.setVgap(5);
        gameGrid.setPadding(new Insets(20));
        
        int tileSize = Math.max(40, 500 / N);
        createGameBoard(tileSize);
        
        HBox controlsCard = new HBox(15);
        controlsCard.setAlignment(Pos.CENTER);
        controlsCard.setPadding(new Insets(15));
        controlsCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                             "-fx-background-radius: 10;" +
                             "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                             "-fx-border-width: 1;" +
                             "-fx-border-radius: 10;");
        
        undoButton = createControlButton("Undo", PRIMARY_COLOR);
        Button hintBtn = createControlButton("Hint", SUCCESS_COLOR);
        Button newBtn = createControlButton("New Game", WARNING_COLOR);
        Button menuBtn = createControlButton("Menu", DANGER_COLOR);
        
        undoButton.setOnAction(e -> undoMove());
        hintBtn.setOnAction(e -> showHint());
        newBtn.setOnAction(e -> startGame(stage, N));
        menuBtn.setOnAction(e -> showMainMenu(stage));
        
        controlsCard.getChildren().addAll(undoButton, hintBtn, newBtn, menuBtn);
        
        gameContainer.getChildren().addAll(headerCard, gameGrid, controlsCard);
        root.getChildren().add(gameContainer);
        
        Scene scene = new Scene(root, Math.max(800, N * tileSize + 200), 
                                    Math.max(700, N * tileSize + 250));
        stage.setScene(scene);
        
        // Update phase label
        updateStatus();
    }
    
    private Button createControlButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.MEDIUM, 13));
        btn.setPrefSize(110, 35);
        btn.setStyle("-fx-background-color: " + toHex(color) + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 6;" +
                    "-fx-cursor: hand;");
        
        btn.setOnMouseEntered(e -> btn.setOpacity(0.9));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        
        return btn;
    }

    // ======================================================================
    // GAME BOARD UI
    // ======================================================================
    
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
    
    private void updateTileStyle(Button tile, int value, boolean hover) {
        if (value == 1) { // White (ON)
            tile.setStyle("-fx-background-color: white;" +
                         "-fx-background-radius: 6;" +
                         "-fx-border-color: " + (hover ? toHex(ACCENT_COLOR) : "#dee2e6") + ";" +
                         "-fx-border-width: 2;" +
                         "-fx-border-radius: 6;");
            tile.setText("●");
            tile.setTextFill(PRIMARY_COLOR);
        } else { // Black (OFF)
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
    // GAME LOGIC - FLIP OPERATION
    // ======================================================================
    
    static void flip(int[][] board, int r, int c) {
        int[] dr = {0, 1, -1, 0, 0};
        int[] dc = {0, 0, 0, 1, -1};

        for (int i = 0; i < 5; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (nr >= 0 && nr < N && nc >= 0 && nc < N)
                board[nr][nc] ^= 1;
        }
    }
    
    static boolean isAllWhite(int[][] board) {
        for (int[] row : board)
            for (int val : row)
                if (val == 0)
                    return false;
        return true;
    }
    
    static boolean isAllWhite(int[][] board, int r1, int r2, int c1, int c2) {
        for (int i = r1; i <= r2; i++)
            for (int j = c1; j <= c2; j++)
                if (board[i][j] == 0)
                    return false;
        return true;
    }
    
    static int[][] copy(int[][] board) {
        int[][] newBoard = new int[N][N];
        for (int i = 0; i < N; i++)
            newBoard[i] = board[i].clone();
        return newBoard;
    }

    // ======================================================================
    // STRUCTURED PLAN ALGORITHM
    // ======================================================================
    
    /**
     * Gets the next move for the computer based on the current board state
     * following the structured algorithm: squares → halves → full.
     * The method now respects the current region state (currentPhase and currentRegionIndex)
     * so that the computer continues working on the same region until it is solved,
     * then moves forward.
     */
    private int[] getNextComputerMove(int[][] board) {
        int half = N / 2;
        
        while (true) {
            if (currentPhase == Phase.SQUARES) {
                // Determine boundaries of the current square
                int r1, r2, c1, c2;
                switch (currentRegionIndex) {
                    case 0: r1 = 0; r2 = half - 1; c1 = 0; c2 = half - 1; break;
                    case 1: r1 = 0; r2 = half - 1; c1 = half; c2 = N - 1; break;
                    case 2: r1 = half; r2 = N - 1; c1 = 0; c2 = half - 1; break;
                    case 3: r1 = half; r2 = N - 1; c1 = half; c2 = N - 1; break;
                    default: r1 = 0; r2 = half - 1; c1 = 0; c2 = half - 1; // fallback
                }
                
                // Check if this square is already solved
                boolean solved = true;
                for (int r = r1; r <= r2 && solved; r++) {
                    for (int c = c1; c <= c2; c++) {
                        if (board[r][c] == 0) {
                            solved = false;
                            break;
                        }
                    }
                }
                
                if (solved) {
                    // Move to the next square
                    currentRegionIndex++;
                    if (currentRegionIndex > 3) {
                        currentPhase = Phase.HALVES;
                        currentRegionIndex = 0;
                    }
                    continue; // check the new region
                }
                
                // Try to solve this square
                int[] move = trySolveRegion(board, r1, r2, c1, c2);
                if (move != null) {
                    return move;
                } else {
                    // Should not happen, but fallback: advance region
                    currentRegionIndex++;
                    if (currentRegionIndex > 3) {
                        currentPhase = Phase.HALVES;
                        currentRegionIndex = 0;
                    }
                    continue;
                }
                
            } else if (currentPhase == Phase.HALVES) {
                // Determine boundaries of the current half
                int r1, r2, c1 = 0, c2 = N - 1;
                if (currentRegionIndex == 0) {
                    r1 = 0; r2 = half - 1;
                } else {
                    r1 = half; r2 = N - 1;
                }
                
                // Check if this half is solved
                boolean solved = true;
                for (int r = r1; r <= r2 && solved; r++) {
                    for (int c = c1; c <= c2; c++) {
                        if (board[r][c] == 0) {
                            solved = false;
                            break;
                        }
                    }
                }
                
                if (solved) {
                    // Move to next half or to full board
                    currentRegionIndex++;
                    if (currentRegionIndex > 1) {
                        currentPhase = Phase.FULL;
                    }
                    continue;
                }
                
                int[] move = trySolveRegion(board, r1, r2, c1, c2);
                if (move != null) {
                    return move;
                } else {
                    // Fallback: advance
                    currentRegionIndex++;
                    if (currentRegionIndex > 1) {
                        currentPhase = Phase.FULL;
                    }
                    continue;
                }
                
            } else { // FULL
                // Check if full board is solved – if so game would have ended, but just in case
                int[] move = trySolveRegion(board, 0, N - 1, 0, N - 1);
                return move; // may be null
            }
        }
    }
    
    /**
     * Tries to find a move that helps solve a specific region
     * Returns the best move for that region or null if region is already solved
     */
    private int[] trySolveRegion(int[][] board, int r1, int r2, int c1, int c2) {
        // First check if this region is already all white
        boolean regionAllWhite = true;
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                if (board[r][c] == 0) {
                    regionAllWhite = false;
                    break;
                }
            }
            if (!regionAllWhite) break;
        }
        
        if (regionAllWhite) {
            return null; // Region already solved
        }
        
        int height = r2 - r1 + 1;
        int width = c2 - c1 + 1;
        
        // Try all possible first row patterns
        for (int mask = 0; mask < (1 << width); mask++) {
            int[][] temp = copy(board);
            List<int[]> moves = new ArrayList<>();
            
            // Apply first row pattern
            for (int col = 0; col < width; col++) {
                if ((mask & (1 << col)) != 0) {
                    int[] move = new int[]{r1, c1 + col};
                    flip(temp, move[0], move[1]);
                    moves.add(move);
                }
            }
            
            // Chase down
            for (int r = r1 + 1; r <= r2; r++) {
                for (int c = c1; c <= c2; c++) {
                    if (temp[r - 1][c] == 0) {
                        int[] move = new int[]{r, c};
                        flip(temp, move[0], move[1]);
                        moves.add(move);
                    }
                }
            }
            
            // Check if region is solved
            boolean solved = true;
            for (int r = r1; r <= r2; r++) {
                for (int c = c1; c <= c2; c++) {
                    if (temp[r][c] == 0) {
                        solved = false;
                        break;
                    }
                }
                if (!solved) break;
            }
            
            if (solved && !moves.isEmpty()) {
                // Return the first move from this solution
                return moves.get(0);
            }
        }
        
        return null; // No solution found for this region
    }
    
    // Check if a move would immediately solve the board
    boolean wouldSolve(int[][] board, int r, int c) {
        int[][] temp = copy(board);
        flip(temp, r, c);
        return isAllWhite(temp);
    }
    
    // ======================================================================
    // DETECT CURRENT SOLVING PHASE (for display)
    // ======================================================================
    
    private String detectPhase() {
        if (currentPhase == Phase.SQUARES) {
            String[] squareNames = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right"};
            return "Solving " + squareNames[currentRegionIndex] + " square";
        } else if (currentPhase == Phase.HALVES) {
            return "Solving " + (currentRegionIndex == 0 ? "Top" : "Bottom") + " half";
        } else {
            return "Solving full board";
        }
    }
    
    /**
     * After a move, check if the current region has become solved.
     * If so, advance to the next region (so the phase label stays accurate).
     */
    private void advanceRegionIfSolved() {
        int half = N / 2;
        if (currentPhase == Phase.SQUARES) {
            int r1, r2, c1, c2;
            switch (currentRegionIndex) {
                case 0: r1 = 0; r2 = half - 1; c1 = 0; c2 = half - 1; break;
                case 1: r1 = 0; r2 = half - 1; c1 = half; c2 = N - 1; break;
                case 2: r1 = half; r2 = N - 1; c1 = 0; c2 = half - 1; break;
                case 3: r1 = half; r2 = N - 1; c1 = half; c2 = N - 1; break;
                default: return;
            }
            boolean solved = true;
            for (int r = r1; r <= r2 && solved; r++)
                for (int c = c1; c <= c2; c++)
                    if (board[r][c] == 0) { solved = false; break; }
            if (solved) {
                currentRegionIndex++;
                if (currentRegionIndex > 3) {
                    currentPhase = Phase.HALVES;
                    currentRegionIndex = 0;
                }
            }
        } else if (currentPhase == Phase.HALVES) {
            int r1, r2, c1 = 0, c2 = N - 1;
            if (currentRegionIndex == 0) {
                r1 = 0; r2 = half - 1;
            } else {
                r1 = half; r2 = N - 1;
            }
            boolean solved = true;
            for (int r = r1; r <= r2 && solved; r++)
                for (int c = c1; c <= c2; c++)
                    if (board[r][c] == 0) { solved = false; break; }
            if (solved) {
                currentRegionIndex++;
                if (currentRegionIndex > 1) {
                    currentPhase = Phase.FULL;
                }
            }
        }
        // For FULL, no further advancement needed
    }

    // ======================================================================
    // MOVE HANDLING - PROPER ALGORITHM IMPLEMENTATION
    // ======================================================================
    
    private void saveGameState() {
        undoStack.push(new GameState(board, userMoves, computerMoves, userTurn,
                                     currentPhase, currentRegionIndex));
    }
    
    private void undoMove() {
        if (undoStack.isEmpty() || !gameActive) {
            statusLabel.setText("Cannot undo");
            statusLabel.setTextFill(DANGER_COLOR);
            return;
        }
        
        // Pop the state before the last move and restore it
        GameState prevState = undoStack.pop();
        board = copy(prevState.board);
        userMoves = prevState.userMoves;
        computerMoves = prevState.computerMoves;
        userTurn = prevState.userTurn;
        currentPhase = prevState.currentPhase;
        currentRegionIndex = prevState.currentRegionIndex;
        gameActive = true;
        
        updateBoard();
        updateStatus();
        
        statusLabel.setText("Move undone");
        statusLabel.setTextFill(ACCENT_COLOR);
    }
    
    private void handleUserMove(int row, int col) {
        if (!gameActive || !userTurn) return;
        
        saveGameState();  // Save state before user's move
        
        // User makes ONE move
        userTurn = false;
        userMoves++;
        updateStatus();
        
        highlightMove(row, col, ACCENT_COLOR);
        flip(board, row, col);
        updateBoard();
        
        // Check if user won
        if (isAllWhite(board)) {
            showVictory(true);
            return;
        }
        
        // Computer makes ONE move using the algorithm
        computerMove();
    }
    
    private void computerMove() {
        undoButton.setDisable(true);
        statusLabel.setText("Computer thinking...");
        statusLabel.setTextFill(SECONDARY_COLOR);

        PauseTransition thinkingPause = new PauseTransition(Duration.seconds(0.5));
        thinkingPause.setOnFinished(e -> {
            int[] move = getNextComputerMove(board);
            if (move == null) {
                statusLabel.setText("Computer has no move");
                statusLabel.setTextFill(DANGER_COLOR);
                userTurn = true;
                updateStatus();
                undoButton.setDisable(false);
                updateBoard(); // clear any lingering region highlight
                return;
            }

            if (wouldSolve(board, move[0], move[1])) {
                statusLabel.setText("Final move is yours!");
                statusLabel.setTextFill(WARNING_COLOR);
                highlightMove(move[0], move[1], WARNING_COLOR);
                userTurn = true;
                updateStatus();
                undoButton.setDisable(false);
                return;
            }

            // Highlight the current subproblem region (blue border) – no auto‑reset
            applyRegionHighlight();

            // Short pause to let the region highlight be visible
            PauseTransition regionPause = new PauseTransition(Duration.seconds(0.8));
            regionPause.setOnFinished(ev -> {
                saveGameState();
                computerMoves++;
                updateStatus();

                // Perform the move – this will also clear all highlights after animation
                highlightMove(move[0], move[1], PRIMARY_COLOR);
                flip(board, move[0], move[1]);
                updateBoard(); // immediate visual update (though highlightMove will also update after 0.3s)

                advanceRegionIfSolved();
                phaseLabel.setText("Current phase: " + detectPhase());

                if (isAllWhite(board)) {
                    showVictory(false);
                    undoButton.setDisable(false);
                    return;
                }

                userTurn = true;
                updateStatus();
                undoButton.setDisable(false);
            });
            regionPause.play();
        });
        thinkingPause.play();
    }

    /**
     * Highlights the current subproblem region (based on currentPhase and currentRegionIndex)
     * with a blue border. No automatic reset – the border will be cleared by the next updateBoard.
     */
    private void applyRegionHighlight() {
        int half = N / 2;
        int r1, r2, c1, c2;

        if (currentPhase == Phase.SQUARES) {
            switch (currentRegionIndex) {
                case 0: r1 = 0; r2 = half - 1; c1 = 0; c2 = half - 1; break;
                case 1: r1 = 0; r2 = half - 1; c1 = half; c2 = N - 1; break;
                case 2: r1 = half; r2 = N - 1; c1 = 0; c2 = half - 1; break;
                case 3: r1 = half; r2 = N - 1; c1 = half; c2 = N - 1; break;
                default: return;
            }
        } else if (currentPhase == Phase.HALVES) {
            if (currentRegionIndex == 0) {
                r1 = 0; r2 = half - 1; c1 = 0; c2 = N - 1;
            } else {
                r1 = half; r2 = N - 1; c1 = 0; c2 = N - 1;
            }
        } else { // FULL
            r1 = 0; r2 = N - 1; c1 = 0; c2 = N - 1;
        }

        Color regionColor = PHASE_2_COLOR; // consistent blue

        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                Button tile = tiles[r][c];
                String newStyle = tile.getStyle().replaceFirst(
                    "-fx-border-color: #[0-9a-fA-F]{6};",
                    "-fx-border-color: " + toHex(regionColor) + ";") +
                    "-fx-border-width: 3;";
                tile.setStyle(newStyle);
            }
        }
    }
    
    private void highlightMove(int row, int col, Color color) {
        int[] dr = {0, 1, -1, 0, 0};
        int[] dc = {0, 0, 0, 1, -1};
        
        for (int i = 0; i < 5; i++) {
            int nr = row + dr[i];
            int nc = col + dc[i];
            
            if (nr >= 0 && nr < N && nc >= 0 && nc < N) {
                Button tile = tiles[nr][nc];
                
                String currentStyle = tile.getStyle();
                String borderColor = toHex(color);
                String newStyle = currentStyle.replaceFirst(
                    "-fx-border-color: #[0-9a-fA-F]{6};", 
                    "-fx-border-color: " + borderColor + ";");
                tile.setStyle(newStyle);
                
                ScaleTransition st = new ScaleTransition(Duration.millis(150), tile);
                st.setToX(1.1);
                st.setToY(1.1);
                st.setAutoReverse(true);
                st.setCycleCount(2);
                st.play();
            }
        }
        
        // Reset styles after animation
        PauseTransition pause = new PauseTransition(Duration.seconds(0.3));
        pause.setOnFinished(e -> updateBoard());
        pause.play();
    }
    
    private void updateBoard() {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                updateTileStyle(tiles[r][c], board[r][c], false);
            }
        }
    }
    
    private void updateStatus() {
        String turn = userTurn ? "Your turn" : "Computer's turn";
        moveLabel.setText(turn + " • Moves: " + userMoves + " • Computer: " + computerMoves);
        statusLabel.setText(userTurn ? "Make your move" : "Computer thinking...");
        statusLabel.setTextFill(userTurn ? ACCENT_COLOR : SECONDARY_COLOR);
        phaseLabel.setText("Current phase: " + detectPhase());
    }
    
    // ======================================================================
    // HINT SYSTEM - FOLLOWS THE SAME ALGORITHM
    // ======================================================================
    
    private void showHint() {
        if (!gameActive) return;
        
        // Get the next move using the same stateful algorithm
        int[] hint = getNextComputerMove(board);
        
        if (hint == null) {
            statusLabel.setText("No hint available");
            statusLabel.setTextFill(DANGER_COLOR);
            return;
        }
        
        String phase = detectPhase();
        Color phaseColor = PHASE_1_COLOR;
        
        if (phase.contains("square")) {
            phaseColor = PHASE_1_COLOR;
        } else if (phase.contains("half")) {
            phaseColor = PHASE_2_COLOR;
        } else {
            phaseColor = PHASE_3_COLOR;
        }
        
        statusLabel.setText("Hint: Try tile (" + hint[0] + ", " + hint[1] + ") - " + phase);
        statusLabel.setTextFill(phaseColor);
        
        // Highlight the suggested tile
        flashTile(hint[0], hint[1]);
        
        // Show which region is being solved
        highlightHintRegion(hint[0], hint[1], phase);
    }
    
    private void highlightHintRegion(int row, int col, String phase) {
        int half = N / 2;
        int r1, r2, c1, c2;
        
        // Determine region bounds based on current region index (same as before)
        if (phase.contains("square")) {
            switch (currentRegionIndex) {
                case 0: r1 = 0; r2 = half - 1; c1 = 0; c2 = half - 1; break;
                case 1: r1 = 0; r2 = half - 1; c1 = half; c2 = N - 1; break;
                case 2: r1 = half; r2 = N - 1; c1 = 0; c2 = half - 1; break;
                case 3: r1 = half; r2 = N - 1; c1 = half; c2 = N - 1; break;
                default: r1 = 0; r2 = half - 1; c1 = 0; c2 = half - 1;
            }
        } else if (phase.contains("half")) {
            if (currentRegionIndex == 0) {
                r1 = 0; r2 = half - 1; c1 = 0; c2 = N - 1;
            } else {
                r1 = half; r2 = N - 1; c1 = 0; c2 = N - 1;
            }
        } else {
            r1 = 0; r2 = N - 1; c1 = 0; c2 = N - 1;
        }
        
        // Always use the same blue color for region highlighting
        Color regionColor = PHASE_2_COLOR;  // Consistent blue
        
        // Store original styles
        Map<Button, String> originalStyles = new HashMap<>();
        
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                Button tile = tiles[r][c];
                originalStyles.put(tile, tile.getStyle());
                
                String newStyle = tile.getStyle().replaceFirst(
                    "-fx-border-color: #[0-9a-fA-F]{6};", 
                    "-fx-border-color: " + toHex(regionColor) + ";") +
                    "-fx-border-width: 3;";
                tile.setStyle(newStyle);
            }
        }
        
        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
        pause.setOnFinished(e -> {
            for (Map.Entry<Button, String> entry : originalStyles.entrySet()) {
                entry.getKey().setStyle(entry.getValue());
            }
        });
        pause.play();
    }
    
    private void flashTile(int r, int c) {
        Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                tiles[r][c].setStyle(tiles[r][c].getStyle().replaceFirst(
                    "-fx-border-color: #[0-9a-fA-F]{6};", 
                    "-fx-border-color: " + toHex(WARNING_COLOR) + ";") +
                    "-fx-border-width: 3;");
            }),
            new KeyFrame(Duration.millis(300), e -> updateTileStyle(tiles[r][c], board[r][c], false)),
            new KeyFrame(Duration.millis(600), e -> {
                tiles[r][c].setStyle(tiles[r][c].getStyle().replaceFirst(
                    "-fx-border-color: #[0-9a-fA-F]{6};", 
                    "-fx-border-color: " + toHex(WARNING_COLOR) + ";") +
                    "-fx-border-width: 3;");
            }),
            new KeyFrame(Duration.millis(900), e -> updateTileStyle(tiles[r][c], board[r][c], false))
        );
        flash.play();
    }

    // ======================================================================
    // VICTORY SCREEN
    // ======================================================================
    
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
        
        Label victoryLabel = new Label(userWon ? "Victory!" : "Game Over");
        victoryLabel.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        victoryLabel.setTextFill(userWon ? SUCCESS_COLOR : DANGER_COLOR);
        
        Label messageLabel = new Label(userWon ? 
            "You solved the puzzle together with the computer!" : 
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
