package pck;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class FlipGameDPGUI extends Application {
    
    // ======================================================================
    // DP GAME LOGIC CORE
    // ======================================================================
    
    private int rows = 3;
    private int cols = 3;
    private int N;
    private int[] flipMask;
    private Map<Integer, Integer> dp = new HashMap<>();
    private Map<Integer, Integer> parent = new HashMap<>();
    private Map<Integer, Integer> moveUsed = new HashMap<>();
    private int overlapCount = 0;
    private int totalChecks = 0;
    private int goalState;  // Will be set to ((1 << N) - 1) = all bits 1 = all white
    private int startState;
    private boolean solutionFound = false;
    private long solveTime = 0;
    private List<int[]> solutionMoves = new ArrayList<>();
    
    // ======================================================================
    // UI COMPONENTS
    // ======================================================================
    
    private int[][] board;
    private Button[][] tiles;
    private Label statusLabel;
    private Label moveLabel;
    private Label statsLabel;
    private GridPane gameGrid;
    private StackPane root;
    private int currentMoveIndex = 0;
    private SequentialTransition solutionAnimation;
    private int totalMoves = 0;
    private Stage primaryStage;
    
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

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showMainMenu();
    }

    // ======================================================================
    // MAIN MENU
    // ======================================================================
    
    private void showMainMenu() {
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
        
        Label subtitle = new Label("DP Solver Edition");
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
        
        Label rulesTitle = new Label("Goal: Make all tiles WHITE");
        rulesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        rulesTitle.setTextFill(PRIMARY_COLOR);
        
        VBox rulesList = new VBox(8);
        rulesList.setAlignment(Pos.CENTER_LEFT);
        
        String[] rules = {
            "• WHITE ● = ON  (1) — DARK ○ = OFF (0)",
            "• Click 'New Game' to generate random solvable board",
            "• Click 'Solve with DP' to find optimal solution",
            "• Watch each move being applied step-by-step",
            "• Each move flips the tile and its neighbors",
            "• Final popup shows detailed statistics"
        };
        
        for (String rule : rules) {
            Label ruleLabel = new Label(rule);
            ruleLabel.setFont(Font.font("Arial", 14));
            ruleLabel.setTextFill(PRIMARY_COLOR);
            rulesList.getChildren().add(ruleLabel);
        }
        
        rulesCard.getChildren().addAll(rulesTitle, rulesList);

        Label selectLabel = new Label("Select Board Size");
        selectLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        selectLabel.setTextFill(PRIMARY_COLOR);

        HBox difficultyBox = new HBox(20);
        difficultyBox.setAlignment(Pos.CENTER);
        
        Button easyBtn = createDifficultyButton("Small", "3×3");
        Button mediumBtn = createDifficultyButton("Medium", "4×4");
        Button hardBtn = createDifficultyButton("Large", "5×5");

        easyBtn.setOnAction(e -> startGame(3));
        mediumBtn.setOnAction(e -> startGame(4));
        hardBtn.setOnAction(e -> startGame(5));

        difficultyBox.getChildren().addAll(easyBtn, mediumBtn, hardBtn);

        menu.getChildren().addAll(titleBox, rulesCard, selectLabel, difficultyBox);

        Scene scene = new Scene(menu, 700, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Flip Game - DP Solver");
        primaryStage.show();
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

    // ======================================================================
    // GAME INITIALIZATION
    // ======================================================================
    
    private void startGame(int gridSize) {
        this.rows = gridSize;
        this.cols = gridSize;
        this.N = rows * cols;
        this.goalState = (1 << N) - 1;  // All bits 1 = all tiles white
        
        // Initialize
        flipMask = new int[N];
        precomputeFlipMasks();
        initializeNewBoard();
        
        root = new StackPane();
        root.setBackground(new Background(new BackgroundFill(
            BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        
        VBox gameContainer = new VBox(20);
        gameContainer.setAlignment(Pos.CENTER);
        gameContainer.setPadding(new Insets(20));
        
        // Header Card
        VBox headerCard = new VBox(10);
        headerCard.setAlignment(Pos.CENTER);
        headerCard.setPadding(new Insets(15, 30, 15, 30));
        headerCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                           "-fx-background-radius: 10;" +
                           "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                           "-fx-border-width: 1;" +
                           "-fx-border-radius: 10;");
        
        Label title = new Label("Flip Game - DP Solver (" + rows + "×" + cols + ")");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(PRIMARY_COLOR);
        
        HBox infoBox = new HBox(30);
        infoBox.setAlignment(Pos.CENTER);
        
        moveLabel = new Label("Moves: 0");
        moveLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        moveLabel.setTextFill(SECONDARY_COLOR);
        
        statusLabel = new Label("Click 'New Game' to start");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        statusLabel.setTextFill(SECONDARY_COLOR);
        
        infoBox.getChildren().addAll(moveLabel, statusLabel);
        headerCard.getChildren().addAll(title, infoBox);
        
        // Game Board
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(5);
        gameGrid.setVgap(5);
        gameGrid.setPadding(new Insets(20));
        
        int tileSize = Math.max(40, 450 / rows);
        createGameBoard(tileSize);
        
        // Stats Label
        statsLabel = new Label("DP Stats: States: 0 | Overlaps: 0 | Checks: 0");
        statsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        statsLabel.setTextFill(SECONDARY_COLOR);
        
        // Controls Card
        HBox controlsCard = new HBox(15);
        controlsCard.setAlignment(Pos.CENTER);
        controlsCard.setPadding(new Insets(15));
        controlsCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                             "-fx-background-radius: 10;" +
                             "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                             "-fx-border-width: 1;" +
                             "-fx-border-radius: 10;");
        
        Button newBtn = createControlButton("New Game", SUCCESS_COLOR);
        Button solveBtn = createControlButton("Solve with DP", ACCENT_COLOR);
        Button menuBtn = createControlButton("Main Menu", DANGER_COLOR);
        
        newBtn.setOnAction(e -> {
            if (solutionAnimation != null) {
                solutionAnimation.stop();
            }
            initializeNewBoard();
            updateBoard();
            moveLabel.setText("Moves: 0");
            statusLabel.setText("New board generated");
            statsLabel.setText("DP Stats: States: 0 | Overlaps: 0 | Checks: 0");
        });
        
        solveBtn.setOnAction(e -> solveWithDP());
        menuBtn.setOnAction(e -> showMainMenu());
        
        controlsCard.getChildren().addAll(newBtn, solveBtn, menuBtn);
        
        gameContainer.getChildren().addAll(headerCard, gameGrid, statsLabel, moveLabel, 
                                          controlsCard);
        root.getChildren().add(gameContainer);
        
        Scene scene = new Scene(root, 700, 700);
        primaryStage.setScene(scene);
    }
    
    private Button createControlButton(String text, Color color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.MEDIUM, 13));
        btn.setPrefSize(130, 35);
        btn.setStyle("-fx-background-color: " + toHex(color) + ";" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 6;" +
                    "-fx-cursor: hand;");
        
        btn.setOnMouseEntered(e -> btn.setOpacity(0.9));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        
        return btn;
    }

    // ======================================================================
    // BOARD UI
    // ======================================================================
    
    private void createGameBoard(int tileSize) {
        tiles = new Button[rows][cols];
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Button tile = new Button();
                tile.setPrefSize(tileSize, tileSize);
                tile.setFont(Font.font("Arial", FontWeight.BOLD, tileSize / 3));
                updateTileStyle(tile, board[r][c], false);
                
                tiles[r][c] = tile;
                gameGrid.add(tile, c, r);
            }
        }
    }
    
    private void updateTileStyle(Button tile, int value, boolean highlight) {
        if (value == 1) {  // ON = WHITE
            tile.setStyle("-fx-background-color: white;" +
                         "-fx-background-radius: 6;" +
                         "-fx-border-color: " + (highlight ? toHex(ACCENT_COLOR) : "#dee2e6") + ";" +
                         "-fx-border-width: " + (highlight ? "3" : "2") + ";" +
                         "-fx-border-radius: 6;");
            tile.setText("●");
            tile.setTextFill(PRIMARY_COLOR);
        } else {  // OFF = DARK
            tile.setStyle("-fx-background-color: #212529;" +
                         "-fx-background-radius: 6;" +
                         "-fx-border-color: " + (highlight ? toHex(ACCENT_COLOR) : "#495057") + ";" +
                         "-fx-border-width: " + (highlight ? "3" : "2") + ";" +
                         "-fx-border-radius: 6;");
            tile.setText("○");
            tile.setTextFill(Color.WHITE);
        }
    }
    
    private void updateBoard() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                updateTileStyle(tiles[r][c], board[r][c], false);
            }
        }
    }
    
    private void highlightMove(int row, int col) {
        // First reset all highlights
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                updateTileStyle(tiles[r][c], board[r][c], false);
            }
        }
        
        // Highlight the clicked tile and its neighbors
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (Math.abs(dr) + Math.abs(dc) <= 1) {
                    int nr = row + dr;
                    int nc = col + dc;
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        updateTileStyle(tiles[nr][nc], board[nr][nc], true);
                    }
                }
            }
        }
    }

    // ======================================================================
    // BOARD LOGIC
    // ======================================================================
    
    private void initializeNewBoard() {
        board = new int[rows][cols];
        Random rand = new Random();
        
        // Start with all zeros (all dark)
        for (int r = 0; r < rows; r++) {
            Arrays.fill(board[r], 0);
        }
        
        // Generate random board by applying random flips
        int numFlips = rand.nextInt(rows * cols / 2) + rows;
        for (int i = 0; i < numFlips; i++) {
            int r = rand.nextInt(rows);
            int c = rand.nextInt(cols);
            press(r, c);
        }
        
        // Save this as the start state for future solves
        startState = boardToState();
    }
    
    private void press(int row, int col) {
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (Math.abs(dr) + Math.abs(dc) <= 1) {
                    int nr = row + dr;
                    int nc = col + dc;
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        board[nr][nc] ^= 1;
                    }
                }
            }
        }
    }
    
    private boolean isSolved() {
        // Check if all tiles are ON (1 = white)
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == 0) return false;  // Found a dark tile
            }
        }
        return true;  // All tiles are white
    }

    // ======================================================================
    // DP LOGIC
    // ======================================================================
    
    private void precomputeFlipMasks() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int index = i * cols + j;
                int mask = 0;
                
                int[][] dirs = {{0,0}, {-1,0}, {1,0}, {0,-1}, {0,1}};
                
                for (int[] d : dirs) {
                    int ni = i + d[0];
                    int nj = j + d[1];
                    
                    if (ni >= 0 && ni < rows && nj >= 0 && nj < cols) {
                        int pos = ni * cols + nj;
                        mask ^= (1 << pos);
                    }
                }
                flipMask[index] = mask;
            }
        }
    }
    
    private int boardToState() {
        int state = 0;
        int bit = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == 1)
                    state |= (1 << bit);
                bit++;
            }
        }
        return state;
    }
    
    private void stateToBoard(int state) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int bit = i * cols + j;
                board[i][j] = (state >> bit) & 1;
            }
        }
    }
    
    private void solveWithDP() {
        // Stop any ongoing animation
        if (solutionAnimation != null) {
            solutionAnimation.stop();
        }
        
        // Reset to the saved start state
        stateToBoard(startState);
        updateBoard();
        
        // Reset DP stats
        dp.clear();
        parent.clear();
        moveUsed.clear();
        solutionMoves.clear();
        overlapCount = 0;
        totalChecks = 0;
        solutionFound = false;
        
        statusLabel.setText("Solving with DP...");
        
        // Start timing
        long startTime = System.nanoTime();
        
        // BFS - looking for goalState (all bits 1 = all white)
        Queue<Integer> q = new LinkedList<>();
        q.add(startState);
        dp.put(startState, 0);
        parent.put(startState, -1);
        
        while (!q.isEmpty()) {
            int current = q.poll();
            int depth = dp.get(current);
            
            if (current == goalState) {
                solutionFound = true;
                break;
            }
            
            for (int move = 0; move < N; move++) {
                totalChecks++;
                int nextState = current ^ flipMask[move];
                
                if (dp.containsKey(nextState)) {
                    overlapCount++;
                } else {
                    dp.put(nextState, depth + 1);
                    parent.put(nextState, current);
                    moveUsed.put(nextState, move);
                    q.add(nextState);
                }
            }
        }
        
        // End timing
        long endTime = System.nanoTime();
        solveTime = (endTime - startTime) / 1_000_000;
        
        // Update stats
        statsLabel.setText(String.format("DP Stats: States: %d | Overlaps: %d | Checks: %d", 
            dp.size(), overlapCount, totalChecks));
        
        if (solutionFound) {
            extractSolutionMoves();
            totalMoves = solutionMoves.size();
            moveLabel.setText("Moves: 0/" + totalMoves);
            statusLabel.setText("Solution found! Animating " + totalMoves + " moves...");
            animateSolution();
        } else {
            statusLabel.setText("No solution exists for this board!");
            // Use Platform.runLater to show popup
            Platform.runLater(this::showSummaryPopup);
        }
    }
    
    private void extractSolutionMoves() {
        solutionMoves.clear();
        int current = goalState;
        while (parent.containsKey(current) && parent.get(current) != -1) {
            int move = moveUsed.get(current);
            int row = move / cols;
            int col = move % cols;
            solutionMoves.add(0, new int[]{row, col});
            current = parent.get(current);
        }
    }
    
    private void animateSolution() {
        if (solutionMoves.isEmpty()) return;
        
        // Reset to start state
        stateToBoard(startState);
        updateBoard();
        
        solutionAnimation = new SequentialTransition();
        
        for (int i = 0; i < solutionMoves.size(); i++) {
            final int step = i;
            int[] move = solutionMoves.get(i);
            int row = move[0];
            int col = move[1];
            
            // Step 1: Highlight (0ms)
            PauseTransition highlightStep = new PauseTransition(Duration.ZERO);
            highlightStep.setOnFinished(e -> {
                highlightMove(row, col);
                statusLabel.setText(String.format("Step %d/%d: Flip (%d,%d)", 
                    step + 1, totalMoves, row, col));
                moveLabel.setText(String.format("Moves: %d/%d", step + 1, totalMoves));
            });
            
            // Step 2: Show highlight (700ms)
            PauseTransition showHighlight = new PauseTransition(Duration.millis(700));
            
            // Step 3: Apply flip and remove highlight (0ms)
            PauseTransition applyStep = new PauseTransition(Duration.ZERO);
            applyStep.setOnFinished(e -> {
                press(row, col);
                updateBoard();  // This removes highlights automatically
            });
            
            // Step 4: Pause before next move (500ms)
            PauseTransition pauseBeforeNext = new PauseTransition(Duration.millis(500));
            
            solutionAnimation.getChildren().addAll(highlightStep, showHighlight, 
                                                   applyStep, pauseBeforeNext);
        }
        
        solutionAnimation.setOnFinished(e -> {
            if (isSolved()) {
                statusLabel.setText("✓ SOLUTION COMPLETE! All tiles are white!");
            } else {
                statusLabel.setText("⚠ Animation complete but board not solved?");
            }
            moveLabel.setText("Moves: " + totalMoves + "/" + totalMoves);
            // Use Platform.runLater to show popup after animation
            Platform.runLater(this::showSummaryPopup);
        });
        
        solutionAnimation.play();
    }
    
    private void showSummaryPopup() {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Solution Summary");
        popupStage.setResizable(false);
        
        VBox popupLayout = new VBox(20);
        popupLayout.setAlignment(Pos.TOP_CENTER);
        popupLayout.setPadding(new Insets(30));
        popupLayout.setBackground(new Background(new BackgroundFill(BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        
        // Top Section
        VBox topSection = new VBox(8);
        topSection.setAlignment(Pos.CENTER);
        
        Label resultLabel;
        if (solutionFound) {
            resultLabel = new Label("✓ SOLVED");
            resultLabel.setTextFill(SUCCESS_COLOR);
        } else {
            resultLabel = new Label("✗ NO SOLUTION");
            resultLabel.setTextFill(DANGER_COLOR);
        }
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        
        Label subtitle = new Label("BFS + Dynamic Programming Result");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        subtitle.setTextFill(SECONDARY_COLOR);
        
        topSection.getChildren().addAll(resultLabel, subtitle);
        
        // Stats Card
        VBox statsCard = new VBox(10);
        statsCard.setStyle("-fx-background-color: " + toHex(CARD_BACKGROUND) + ";" +
                          "-fx-background-radius: 10;" +
                          "-fx-border-color: " + toHex(BORDER_COLOR) + ";" +
                          "-fx-border-width: 1;" +
                          "-fx-border-radius: 10;");
        statsCard.setPadding(new Insets(20));
        
        Label summaryTitle = new Label("Summary");
        summaryTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        summaryTitle.setTextFill(ACCENT_COLOR);
        VBox.setMargin(summaryTitle, new Insets(0, 0, 10, 0));
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        
        int row = 0;
        
        // Row 0: Board Size
        Label sizeLabel = new Label("Board Size:");
        sizeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        sizeLabel.setTextFill(PRIMARY_COLOR);
        Label sizeValue = new Label(rows + "×" + cols);
        sizeValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        sizeValue.setTextFill(SECONDARY_COLOR);
        statsGrid.add(sizeLabel, 0, row);
        statsGrid.add(sizeValue, 1, row);
        row++;
        
        // Row 1: Solution
        Label solutionLabel = new Label("Solution:");
        solutionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        solutionLabel.setTextFill(PRIMARY_COLOR);
        Label solutionValue = new Label(solutionFound ? "YES ✓" : "NO ✗");
        solutionValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        solutionValue.setTextFill(solutionFound ? SUCCESS_COLOR : DANGER_COLOR);
        statsGrid.add(solutionLabel, 0, row);
        statsGrid.add(solutionValue, 1, row);
        row++;
        
        // Row 2: Minimum Moves (only if solved)
        if (solutionFound) {
            Label movesLabel = new Label("Minimum Moves:");
            movesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            movesLabel.setTextFill(PRIMARY_COLOR);
            Label movesValue = new Label(String.valueOf(totalMoves));
            movesValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
            movesValue.setTextFill(SECONDARY_COLOR);
            statsGrid.add(movesLabel, 0, row);
            statsGrid.add(movesValue, 1, row);
            row++;
        }
        
        // Row 3: States Explored
        Label statesLabel = new Label("States Explored:");
        statesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        statesLabel.setTextFill(PRIMARY_COLOR);
        Label statesValue = new Label(String.valueOf(dp.size()));
        statesValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        statesValue.setTextFill(SECONDARY_COLOR);
        statsGrid.add(statesLabel, 0, row);
        statsGrid.add(statesValue, 1, row);
        row++;
        
        // Row 4: Total Checks
        Label checksLabel = new Label("Total Checks:");
        checksLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        checksLabel.setTextFill(PRIMARY_COLOR);
        Label checksValue = new Label(String.valueOf(totalChecks));
        checksValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        checksValue.setTextFill(SECONDARY_COLOR);
        statsGrid.add(checksLabel, 0, row);
        statsGrid.add(checksValue, 1, row);
        row++;
        
        // Row 5: Overlaps Detected
        Label overlapsLabel = new Label("Overlaps Detected:");
        overlapsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        overlapsLabel.setTextFill(PRIMARY_COLOR);
        Label overlapsValue = new Label(String.valueOf(overlapCount));
        overlapsValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        overlapsValue.setTextFill(SECONDARY_COLOR);
        statsGrid.add(overlapsLabel, 0, row);
        statsGrid.add(overlapsValue, 1, row);
        row++;
        
        // Row 6: Overlap Ratio
        Label ratioLabel = new Label("Overlap Ratio:");
        ratioLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        ratioLabel.setTextFill(PRIMARY_COLOR);
        double ratio = totalChecks > 0 ? (overlapCount * 100.0 / totalChecks) : 0;
        Label ratioValue = new Label(String.format("%.2f%%", ratio));
        ratioValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        ratioValue.setTextFill(SECONDARY_COLOR);
        statsGrid.add(ratioLabel, 0, row);
        statsGrid.add(ratioValue, 1, row);
        row++;
        
        // Row 7: Time Taken
        Label timeLabel = new Label("Time Taken:");
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        timeLabel.setTextFill(PRIMARY_COLOR);
        Label timeValue = new Label(solveTime + " ms");
        timeValue.setFont(Font.font("Arial", FontWeight.NORMAL, 13));
        timeValue.setTextFill(SECONDARY_COLOR);
        statsGrid.add(timeLabel, 0, row);
        statsGrid.add(timeValue, 1, row);
        
        statsCard.getChildren().addAll(summaryTitle, statsGrid);
        
        // Move Sequence (if solved)
        VBox moveSequence = new VBox(5);
        if (solutionFound && !solutionMoves.isEmpty()) {
            Label movesTitle = new Label("Move Sequence:");
            movesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            movesTitle.setTextFill(PRIMARY_COLOR);
            
            VBox movesList = new VBox(3);
            for (int i = 0; i < solutionMoves.size(); i++) {
                int[] move = solutionMoves.get(i);
                Label moveItem = new Label(String.format("  Step %d: Flip (%d, %d)", 
                    i + 1, move[0], move[1]));
                moveItem.setFont(Font.font("Monospace", 12));
                moveItem.setTextFill(SECONDARY_COLOR);
                movesList.getChildren().add(moveItem);
            }
            
            ScrollPane scrollPane = new ScrollPane(movesList);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(120);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            
            moveSequence.getChildren().addAll(movesTitle, scrollPane);
        }
        
        // Close Button
        Button closeBtn = new Button("Close");
        closeBtn.setFont(Font.font("Arial", FontWeight.MEDIUM, 14));
        closeBtn.setPrefSize(120, 38);
        closeBtn.setStyle("-fx-background-color: " + toHex(ACCENT_COLOR) + ";" +
                         "-fx-text-fill: white;" +
                         "-fx-background-radius: 6;" +
                         "-fx-cursor: hand;");
        closeBtn.setOnAction(e -> popupStage.close());
        
        popupLayout.getChildren().addAll(topSection, statsCard, moveSequence, closeBtn);
        
        // Adjust height based on whether solution exists
        double height = solutionFound ? 620 : 500;
        Scene popupScene = new Scene(popupLayout, 500, height);
        popupStage.setScene(popupScene);
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }
    
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    public static void main(String[] args) {
        launch(args);
    }
}