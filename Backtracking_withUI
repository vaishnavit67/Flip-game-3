package pck;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class FlipGame_BacktrackingGUI extends Application {
    
    // ======================================================================
    // NODE CLASS FOR TREE VISUALIZATION
    // ======================================================================
    
    static class TreeNode {
        int id;
        TreeNode parent;
        List<TreeNode> children = new ArrayList<>();
        int index;          // cell index (0-8)
        int r, c;           // row and column
        int flips;          // number of flips so far
        String result;      // "internal", "dead", "pruned", "solution"
        String edge;        // "S" for skip, "F" for flip
        int depth;
        int[][] board;      // board state at this node
        List<Integer> path; // sequence of 0/1 decisions
        
        // Layout coordinates
        double x, y;
        
        TreeNode(int id, TreeNode parent, int index, int flips, int[][] board, 
                 List<Integer> path, int depth, String edge) {
            this.id = id;
            this.parent = parent;
            this.index = index;
            if (index < 9) {
                this.r = index / 3;
                this.c = index % 3;
            } else {
                this.r = -1;
                this.c = -1;
            }
            this.flips = flips;
            this.board = copyBoard(board);
            this.path = new ArrayList<>(path);
            this.depth = depth;
            this.edge = edge;
            this.result = "internal";
        }
        
        int[][] copyBoard(int[][] b) {
            int[][] copy = new int[3][3];
            for (int i = 0; i < 3; i++)
                copy[i] = b[i].clone();
            return copy;
        }
        
        boolean isLeaf() {
            return index >= 9 || !result.equals("internal");
        }
    }
    
    // ======================================================================
    // BACKTRACKING SOLVER WITH TREE BUILDING
    // ======================================================================
    
    static class BacktrackingSolver {
        int[][] initialBoard;
        List<TreeNode> nodes = new ArrayList<>();
        TreeNode root;
        int bestCost = Integer.MAX_VALUE;
        int nodeCounter = 0;
        
        // Directions for flipping
        static int[][] dirs = {{0,0}, {1,0}, {-1,0}, {0,1}, {0,-1}};
        
        BacktrackingSolver(int[][] initialBoard) {
            this.initialBoard = copyBoard(initialBoard);
        }
        
        int[][] copyBoard(int[][] b) {
            int[][] copy = new int[3][3];
            for (int i = 0; i < 3; i++)
                copy[i] = b[i].clone();
            return copy;
        }
        
        void flip(int[][] b, int r, int c) {
            for (int[] d : dirs) {
                int nr = r + d[0];
                int nc = c + d[1];
                if (nr >= 0 && nr < 3 && nc >= 0 && nc < 3)
                    b[nr][nc] ^= 1;
            }
        }
        
        // MODIFIED: Goal state is all 1's (white)
        boolean isSolved(int[][] b) {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    if (b[i][j] == 0) return false; // If any cell is 0 (black), not solved
            return true; // All cells are 1 (white)
        }
        
        void buildTree() {
            nodes.clear();
            nodeCounter = 0;
            bestCost = Integer.MAX_VALUE;
            
            int[][] initialBoardCopy = copyBoard(initialBoard);
            List<Integer> initialPath = new ArrayList<>();
            
            root = new TreeNode(nodeCounter++, null, 0, 0, 
                                initialBoardCopy, initialPath, 0, "");
            nodes.add(root);
            
            dfs(root, initialBoardCopy, 0, 0, initialPath);
            
            // Layout the tree
            layoutTree();
        }
        
        void dfs(TreeNode node, int[][] currentBoard, int index, int flips, 
                 List<Integer> path) {
            
            // Prune if flips already >= best
            if (flips >= bestCost) {
                node.result = "pruned";
                return;
            }
            
            // Base case: reached end of board
            if (index == 9) {
                if (isSolved(currentBoard)) {
                    node.result = "solution";
                    bestCost = Math.min(bestCost, flips);
                } else {
                    node.result = "dead";
                }
                return;
            }
            
            int r = index / 3;
            int c = index % 3;
            
            // OPTION 1: SKIP (left child)
            List<Integer> skipPath = new ArrayList<>(path);
            skipPath.add(0);
            
            TreeNode skipNode = new TreeNode(nodeCounter++, node, index + 1, flips,
                                            currentBoard, skipPath, node.depth + 1, "S");
            nodes.add(skipNode);
            node.children.add(skipNode);
            
            dfs(skipNode, currentBoard, index + 1, flips, skipPath);
            
            // OPTION 2: FLIP (right child)
            List<Integer> flipPath = new ArrayList<>(path);
            flipPath.add(1);
            
            int[][] flippedBoard = copyBoard(currentBoard);
            flip(flippedBoard, r, c);
            
            TreeNode flipNode = new TreeNode(nodeCounter++, node, index + 1, flips + 1,
                                            flippedBoard, flipPath, node.depth + 1, "F");
            nodes.add(flipNode);
            node.children.add(flipNode);
            
            dfs(flipNode, flippedBoard, index + 1, flips + 1, flipPath);
        }
        
        void layoutTree() {
            if (nodes.isEmpty()) return;
            
            // Group leaves by their position
            List<TreeNode> leaves = new ArrayList<>();
            Map<Integer, List<TreeNode>> depthMap = new HashMap<>();
            
            for (TreeNode node : nodes) {
                if (node.isLeaf()) {
                    leaves.add(node);
                }
                depthMap.computeIfAbsent(node.depth, k -> new ArrayList<>()).add(node);
            }
            
            // Calculate leaf positions - use wider canvas
            double canvasWidth = 2000; // Increased width for better spacing
            double canvasHeight = 800;  // Increased height
            double leftMargin = 150;
            double rightMargin = 150;
            double topMargin = 80;
            double bottomMargin = 100;
            
            int leafCount = leaves.size();
            if (leafCount > 1) {
                for (int i = 0; i < leafCount; i++) {
                    TreeNode leaf = leaves.get(i);
                    leaf.x = leftMargin + (i * (canvasWidth - leftMargin - rightMargin) / (leafCount - 1));
                    leaf.y = canvasHeight - bottomMargin;
                }
            } else if (leafCount == 1) {
                leaves.get(0).x = canvasWidth / 2;
                leaves.get(0).y = canvasHeight - bottomMargin;
            }
            
            // Position internal nodes based on their leaf range
            for (int depth = 8; depth >= 0; depth--) {
                List<TreeNode> nodesAtDepth = depthMap.getOrDefault(depth, new ArrayList<>());
                for (TreeNode node : nodesAtDepth) {
                    if (!node.isLeaf() && !node.children.isEmpty()) {
                        double minX = Double.MAX_VALUE;
                        double maxX = -Double.MAX_VALUE;
                        
                        for (TreeNode child : node.children) {
                            minX = Math.min(minX, child.x);
                            maxX = Math.max(maxX, child.x);
                        }
                        
                        node.x = (minX + maxX) / 2;
                        node.y = topMargin + depth * 70; // Increased vertical spacing
                    }
                }
            }
            
            // Position root if not set
            if (root != null && root.x == 0) {
                root.x = canvasWidth / 2;
                root.y = topMargin;
            }
        }
        
        TreeNode getNodeById(int id) {
            for (TreeNode node : nodes) {
                if (node.id == id) return node;
            }
            return null;
        }
    }
    
    // ======================================================================
    // GUI COMPONENTS
    // ======================================================================
    
    private BacktrackingSolver solver;
    private Button[][] tiles;
    private Label statusLabel;
    private Label stepLabel;
    private Label statsLabel;
    private Label typeLabel;
    private Label pathLabel;
    private GridPane gameGrid;
    private Pane treePane;
    private ScrollPane treeScrollPane;
    private Timeline autoPlayTimeline;
    private boolean autoPlaying = false;
    private double autoPlaySpeed = 1.5; // seconds per step
    private VBox controlPanel;
    private VBox infoPanel;
    private BorderPane mainLayout;
    
    private int currentNodeIndex = -1;
    private Set<Integer> visitedNodes = new HashSet<>();
    
    // Color scheme
    private Color BACKGROUND = Color.rgb(248, 249, 250);
    private Color PRIMARY_COLOR = Color.rgb(33, 37, 41);
    private Color ACCENT_COLOR = Color.rgb(13, 110, 253);
    private Color SUCCESS_COLOR = Color.rgb(25, 135, 84);
    private Color WARNING_COLOR = Color.rgb(255, 193, 7);
    private Color DANGER_COLOR = Color.rgb(220, 53, 69);
    private Color PURPLE_COLOR = Color.rgb(111, 66, 193);
    private Color ORANGE_COLOR = Color.rgb(253, 126, 20);
    
    // MODIFIED: Original 3x3 board - goal is to make all white (1's)
    // Currently has some white (1) and some black (0)
    private int[][] originalBoard = {
        {0, 0, 1},  // black, black, white
        {0, 0, 0},  // all black
        {1, 1, 0}   // white, white, black
    };
    
    @Override
    public void start(Stage primaryStage) {
        showMainMenu(primaryStage);
    }
    
    private void showMainMenu(Stage stage) {
        VBox menu = new VBox(30);
        menu.setAlignment(Pos.CENTER);
        menu.setBackground(new Background(new BackgroundFill(
            BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        menu.setPadding(new Insets(40));
        
        Label title = new Label("FLIP GAME");
        title.setFont(Font.font("Georgia", FontWeight.BOLD, 72));
        title.setTextFill(PRIMARY_COLOR);
        
        Label subtitle = new Label("State Space Tree Visualization");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        subtitle.setTextFill(Color.GRAY);
        
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(600);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                     "-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 12;");
        
        Label description = new Label("3×3 Board - Make All White (●)");
        description.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        description.setTextFill(PRIMARY_COLOR);
        
        // Mini preview of the board
        GridPane preview = new GridPane();
        preview.setAlignment(Pos.CENTER);
        preview.setHgap(2);
        preview.setVgap(2);
        
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                Button cell = new Button();
                cell.setPrefSize(40, 40);
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
        
        // Stats preview
        HBox statsPreview = new HBox(20);
        statsPreview.setAlignment(Pos.CENTER);
        
        VBox stat1 = new VBox(5);
        stat1.setAlignment(Pos.CENTER);
        Label stat1Val = new Label("205");
        stat1Val.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        stat1Val.setTextFill(PURPLE_COLOR);
        Label stat1Label = new Label("Total Nodes");
        stat1Label.setFont(Font.font("Arial", 12));
        stat1.getChildren().addAll(stat1Val, stat1Label);
        
        VBox stat2 = new VBox(5);
        stat2.setAlignment(Pos.CENTER);
        Label stat2Val = new Label("2");
        stat2Val.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        stat2Val.setTextFill(SUCCESS_COLOR);
        Label stat2Label = new Label("Min Flips");
        stat2Label.setFont(Font.font("Arial", 12));
        stat2.getChildren().addAll(stat2Val, stat2Label);
        
        statsPreview.getChildren().addAll(stat1, stat2);
        
        Button startBtn = new Button("Start Visualization");
        startBtn.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        startBtn.setPrefSize(250, 60);
        startBtn.setStyle("-fx-background-color: " + toHex(ACCENT_COLOR) + ";" +
                         "-fx-text-fill: white;" +
                         "-fx-background-radius: 30;");
        startBtn.setOnAction(e -> startVisualization(stage));
        
        card.getChildren().addAll(description, preview, statsPreview, startBtn);
        
        menu.getChildren().addAll(title, subtitle, card);
        
        Scene scene = new Scene(menu, 700, 750);
        stage.setScene(scene);
        stage.setTitle("Flip Game - State Space Tree Visualization");
        stage.show();
    }
    
    private void startVisualization(Stage stage) {
        solver = new BacktrackingSolver(originalBoard);
        
        // Show loading
        statusLabel = new Label("Building tree... Please wait");
        
        mainLayout = new BorderPane();
        mainLayout.setBackground(new Background(new BackgroundFill(
            BACKGROUND, CornerRadii.EMPTY, Insets.EMPTY)));
        
        // Top Panel
        VBox topPanel = createTopPanel();
        mainLayout.setTop(topPanel);
        
        // Center - Split view with board and tree
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.35);
        
        // Left side - Game board
        VBox leftPanel = new VBox(15);
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 0 1 0 0;");
        
        Label boardTitle = new Label("Current Board State");
        boardTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // MODIFIED: Add goal indicator
        Label goalLabel = new Label("Goal: All White (●)");
        goalLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        goalLabel.setTextFill(SUCCESS_COLOR);
        goalLabel.setPadding(new Insets(0, 0, 10, 0));
        
        gameGrid = new GridPane();
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setHgap(10);
        gameGrid.setVgap(10);
        
        createGameBoard(80);
        
        // Info panel below board
        infoPanel = new VBox(10);
        infoPanel.setPadding(new Insets(15));
        infoPanel.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        
        Label currentNodeLabel = new Label("Current Node Info:");
        currentNodeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        typeLabel = new Label("");
        typeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        pathLabel = new Label("");
        pathLabel.setFont(Font.font("Monospaced", 12));
        
        infoPanel.getChildren().addAll(currentNodeLabel, typeLabel, pathLabel);
        
        leftPanel.getChildren().addAll(boardTitle, goalLabel, gameGrid, infoPanel);
        
        // Right side - Tree visualization with improved scrolling
        VBox rightPanel = new VBox(15);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPadding(new Insets(20));
        VBox.setVgrow(rightPanel, Priority.ALWAYS);
        
        Label treeTitle = new Label("State Space Tree");
        treeTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // Create tree pane with larger preferred size
        treePane = new Pane();
        treePane.setPrefSize(2000, 1000); // Larger canvas
        
        // Create scroll pane with both horizontal and vertical scroll bars
        treeScrollPane = new ScrollPane(treePane);
        treeScrollPane.setFitToWidth(false); // Don't fit to width - allow horizontal scroll
        treeScrollPane.setFitToHeight(false); // Don't fit to height - allow vertical scroll
        treeScrollPane.setPrefHeight(600);
        treeScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS); // Always show horizontal scrollbar
        treeScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS); // Always show vertical scrollbar
        treeScrollPane.setPannable(true); // Allow panning with mouse drag
        
        // Make the scroll pane expand to fill available space
        VBox.setVgrow(treeScrollPane, Priority.ALWAYS);
        
        // Legend
        HBox legendBox = new HBox(15);
        legendBox.setAlignment(Pos.CENTER);
        legendBox.setPadding(new Insets(10));
        legendBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        
        legendBox.getChildren().addAll(
            createLegendItem("Current", PURPLE_COLOR),
            createLegendItem("Solution", SUCCESS_COLOR),
            createLegendItem("Dead End", DANGER_COLOR),
            createLegendItem("Pruned", WARNING_COLOR),
            createLegendItem("Visited", Color.GRAY),
            createLegendItem("S = Skip", ACCENT_COLOR),
            createLegendItem("F = Flip", ORANGE_COLOR)
        );
        
        rightPanel.getChildren().addAll(treeTitle, treeScrollPane, legendBox);
        
        splitPane.getItems().addAll(leftPanel, rightPanel);
        mainLayout.setCenter(splitPane);
        
        // Bottom - Control Panel
        controlPanel = createControlPanel();
        mainLayout.setBottom(controlPanel);
        
        Scene scene = new Scene(mainLayout, 1400, 900);
        stage.setScene(scene);
        stage.setTitle("Backtracking Tree Visualization - 3×3 Board - Make All White");
        
        // Build tree in background
        new Thread(() -> {
            solver.buildTree();
            javafx.application.Platform.runLater(() -> {
                drawTree();
                updateDisplay();
                statusLabel.setText("Ready! " + solver.nodes.size() + " nodes in tree");
                
                // Auto-scroll to show the tree better
                treeScrollPane.setHvalue(0.5); // Center horizontally
                treeScrollPane.setVvalue(0.2); // Show top portion
            });
        }).start();
    }
    
    private HBox createLegendItem(String text, Color color) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER);
        
        Rectangle rect = new Rectangle(15, 15);
        rect.setFill(color);
        rect.setStroke(color.darker());
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        
        Label label = new Label(text);
        label.setFont(Font.font("Arial", 11));
        
        box.getChildren().addAll(rect, label);
        return box;
    }
    
    private VBox createTopPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");
        
        Label titleLabel = new Label("Backtracking State Space Tree");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        titleLabel.setTextFill(PRIMARY_COLOR);
        
        HBox statusBox = new HBox(20);
        statusBox.setAlignment(Pos.CENTER);
        
        statusLabel = new Label("Building tree...");
        statusLabel.setFont(Font.font("Arial", 14));
        
        stepLabel = new Label("Step: 0 / 0");
        stepLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        stepLabel.setTextFill(ACCENT_COLOR);
        
        statsLabel = new Label("");
        statsLabel.setFont(Font.font("Arial", 12));
        statsLabel.setTextFill(Color.GRAY);
        
        statusBox.getChildren().addAll(statusLabel, stepLabel, statsLabel);
        panel.getChildren().addAll(titleLabel, statusBox);
        
        return panel;
    }
    
    private void createGameBoard(int tileSize) {
        tiles = new Button[3][3];
        
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                Button tile = new Button();
                tile.setPrefSize(tileSize, tileSize);
                tile.setFont(Font.font("Arial", FontWeight.BOLD, tileSize / 3));
                updateTileStyle(tile, originalBoard[r][c], false);
                tiles[r][c] = tile;
                gameGrid.add(tile, c, r);
            }
        }
    }
    
    // MODIFIED: Update tile style - 1 = white (●), 0 = black (○)
    private void updateTileStyle(Button tile, int value, boolean highlight) {
        if (value == 1) {
            tile.setStyle("-fx-background-color: white;" +
                         "-fx-background-radius: 8;" +
                         "-fx-border-color: " + (highlight ? toHex(ACCENT_COLOR) : "#dee2e6") + ";" +
                         "-fx-border-width: " + (highlight ? "3" : "2") + ";" +
                         "-fx-border-radius: 8;");
            tile.setText("●");  // White circle for ON (goal state)
            tile.setTextFill(PRIMARY_COLOR);
        } else {
            tile.setStyle("-fx-background-color: #212529;" +
                         "-fx-background-radius: 8;" +
                         "-fx-border-color: " + (highlight ? toHex(ACCENT_COLOR) : "#495057") + ";" +
                         "-fx-border-width: " + (highlight ? "3" : "2") + ";" +
                         "-fx-border-radius: 8;");
            tile.setText("○");  // Empty circle for OFF
            tile.setTextFill(Color.WHITE);
        }
    }
    
    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");
        
        HBox navBox = new HBox(10);
        navBox.setAlignment(Pos.CENTER);
        
        Button firstBtn = createControlButton("⏮ First", PRIMARY_COLOR);
        Button prevBtn = createControlButton("⏪ Previous", PRIMARY_COLOR);
        Button nextBtn = createControlButton("Next ⏩", PRIMARY_COLOR);
        Button lastBtn = createControlButton("Last ⏭", PRIMARY_COLOR);
        
        firstBtn.setOnAction(e -> {
            stopAutoPlay();
            currentNodeIndex = -1;
            visitedNodes.clear();
            updateDisplay();
        });
        
        prevBtn.setOnAction(e -> {
            stopAutoPlay();
            if (currentNodeIndex > -1) {
                currentNodeIndex--;
                if (currentNodeIndex >= 0) {
                    visitedNodes.add(solver.nodes.get(currentNodeIndex).id);
                }
                updateDisplay();
            }
        });
        
        nextBtn.setOnAction(e -> {
            stopAutoPlay();
            if (currentNodeIndex < solver.nodes.size() - 1) {
                currentNodeIndex++;
                visitedNodes.add(solver.nodes.get(currentNodeIndex).id);
                updateDisplay();
                
                // Auto-scroll to show current node
                scrollToCurrentNode();
            }
        });
        
        lastBtn.setOnAction(e -> {
            stopAutoPlay();
            if (solver.nodes.size() > 0) {
                currentNodeIndex = solver.nodes.size() - 1;
                for (int i = 0; i <= currentNodeIndex; i++) {
                    visitedNodes.add(solver.nodes.get(i).id);
                }
                updateDisplay();
                scrollToCurrentNode();
            }
        });
        
        navBox.getChildren().addAll(firstBtn, prevBtn, nextBtn, lastBtn);
        
        HBox autoBox = new HBox(10);
        autoBox.setAlignment(Pos.CENTER);
        
        Button playBtn = createControlButton("▶ Play", SUCCESS_COLOR);
        Button pauseBtn = createControlButton("⏸ Pause", WARNING_COLOR);
        Button slowerBtn = createControlButton("Slower", ACCENT_COLOR);
        Button fasterBtn = createControlButton("Faster", ACCENT_COLOR);
        Button resetBtn = createControlButton("↺ Reset", DANGER_COLOR);
        Button centerBtn = createControlButton("⟲ Center", PURPLE_COLOR);
        
        playBtn.setOnAction(e -> startAutoPlay());
        pauseBtn.setOnAction(e -> stopAutoPlay());
        slowerBtn.setOnAction(e -> adjustSpeed(1.3));
        fasterBtn.setOnAction(e -> adjustSpeed(0.7));
        resetBtn.setOnAction(e -> resetVisualization());
        centerBtn.setOnAction(e -> centerTreeView());
        
        autoBox.getChildren().addAll(playBtn, pauseBtn, slowerBtn, fasterBtn, resetBtn, centerBtn);
        
        // Speed indicator
        HBox speedBox = new HBox(10);
        speedBox.setAlignment(Pos.CENTER);
        
        Label speedLabel = new Label(String.format("Speed: %.1fs/step", autoPlaySpeed));
        speedLabel.setFont(Font.font("Arial", 12));
        speedLabel.setId("speedLabel");
        
        // Zoom controls
        Button zoomInBtn = createControlButton("➕ Zoom In", ACCENT_COLOR);
        Button zoomOutBtn = createControlButton("➖ Zoom Out", ACCENT_COLOR);
        zoomInBtn.setPrefSize(120, 35);
        zoomOutBtn.setPrefSize(120, 35);
        
        zoomInBtn.setOnAction(e -> zoomTree(1.2));
        zoomOutBtn.setOnAction(e -> zoomTree(0.8));
        
        HBox zoomBox = new HBox(10);
        zoomBox.setAlignment(Pos.CENTER);
        zoomBox.getChildren().addAll(zoomInBtn, zoomOutBtn);
        
        speedBox.getChildren().addAll(speedLabel, zoomBox);
        
        panel.getChildren().addAll(navBox, autoBox, speedBox);
        
        return panel;
    }
    
    private void scrollToCurrentNode() {
        if (solver == null || currentNodeIndex < 0 || currentNodeIndex >= solver.nodes.size()) return;
        
        TreeNode node = solver.nodes.get(currentNodeIndex);
        
        // Calculate scroll position to center the node
        double viewportWidth = treeScrollPane.getViewportBounds().getWidth();
        double viewportHeight = treeScrollPane.getViewportBounds().getHeight();
        double contentWidth = treePane.getWidth();
        double contentHeight = treePane.getHeight();
        
        if (contentWidth > 0 && contentHeight > 0) {
            double hValue = (node.x - viewportWidth / 2) / (contentWidth - viewportWidth);
            double vValue = (node.y - viewportHeight / 2) / (contentHeight - viewportHeight);
            
            // Clamp values between 0 and 1
            hValue = Math.max(0, Math.min(1, hValue));
            vValue = Math.max(0, Math.min(1, vValue));
            
            treeScrollPane.setHvalue(hValue);
            treeScrollPane.setVvalue(vValue);
        }
    }
    
    private void centerTreeView() {
        treeScrollPane.setHvalue(0.5);
        treeScrollPane.setVvalue(0.3);
    }
    
    private void zoomTree(double factor) {
        double currentWidth = treePane.getPrefWidth();
        double currentHeight = treePane.getPrefHeight();
        
        treePane.setPrefWidth(currentWidth * factor);
        treePane.setPrefHeight(currentHeight * factor);
        
        // Trigger re-layout
        treePane.requestLayout();
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
    
    private void drawTree() {
        if (treePane == null || solver == null || solver.nodes.isEmpty()) return;
        
        treePane.getChildren().clear();
        
        // Draw edges first
        for (TreeNode node : solver.nodes) {
            if (node.parent != null) {
                Line line = new Line(node.parent.x, node.parent.y + 15, node.x, node.y - 15);
                line.setStroke(Color.rgb(200, 200, 200));
                line.setStrokeWidth(1.5);
                
                // Different style for different edge types
                if (visitedNodes.contains(node.id)) {
                    if ("F".equals(node.edge)) {
                        line.setStroke(ORANGE_COLOR);
                        line.setStrokeWidth(2);
                    } else {
                        line.setStroke(ACCENT_COLOR);
                        line.setStrokeWidth(2);
                    }
                } else {
                    line.getStrokeDashArray().addAll(5.0);
                }
                
                treePane.getChildren().add(line);
                
                // Edge label (S or F)
                if (node.edge != null && !node.edge.isEmpty() && visitedNodes.contains(node.id)) {
                    Text edgeText = new Text((node.parent.x + node.x) / 2 - 5, 
                                            (node.parent.y + node.y) / 2 - 8, node.edge);
                    edgeText.setFill(node.edge.equals("F") ? ORANGE_COLOR : ACCENT_COLOR);
                    edgeText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    edgeText.setStroke(Color.WHITE);
                    edgeText.setStrokeWidth(0.5);
                    
                    treePane.getChildren().add(edgeText);
                }
            }
        }
        
        // Draw nodes
        for (TreeNode node : solver.nodes) {
            // Node rectangle
            double width = node.depth < 4 ? 45 : 38;
            double height = node.depth < 4 ? 28 : 24;
            
            Rectangle rect = new Rectangle(node.x - width/2, node.y - height/2, width, height);
            rect.setArcWidth(8);
            rect.setArcHeight(8);
            
            // Set color based on node type and visited status
            if (currentNodeIndex >= 0 && solver.nodes.get(currentNodeIndex).id == node.id) {
                rect.setFill(PURPLE_COLOR);
                rect.setStroke(PURPLE_COLOR.darker());
                rect.setStrokeWidth(3);
                
                // Add glow effect for current node
                rect.setEffect(new javafx.scene.effect.DropShadow(10, PURPLE_COLOR));
            } else if (visitedNodes.contains(node.id)) {
                if ("solution".equals(node.result)) {
                    rect.setFill(SUCCESS_COLOR);
                    rect.setStroke(SUCCESS_COLOR.darker());
                } else if ("dead".equals(node.result)) {
                    rect.setFill(DANGER_COLOR);
                    rect.setStroke(DANGER_COLOR.darker());
                } else if ("pruned".equals(node.result)) {
                    rect.setFill(WARNING_COLOR);
                    rect.setStroke(WARNING_COLOR.darker());
                } else {
                    rect.setFill(Color.LIGHTGRAY);
                    rect.setStroke(Color.GRAY);
                }
                rect.setStrokeWidth(2);
            } else {
                rect.setFill(Color.WHITE);
                rect.setStroke(Color.LIGHTGRAY);
                rect.setStrokeWidth(1.5);
            }
            
            rect.setCursor(javafx.scene.Cursor.HAND);
            
            // Add click handler
            int nodeId = node.id;
            rect.setOnMouseClicked(e -> {
                jumpToNode(nodeId);
                scrollToCurrentNode();
            });
            
            treePane.getChildren().add(rect);
            
            // Node label
            String label;
            if (node.index < 9) {
                label = "(" + node.r + "," + node.c + ")";
            } else if ("solution".equals(node.result)) {
                label = "✓SOL";
            } else if ("dead".equals(node.result)) {
                label = "✗END";
            } else if ("pruned".equals(node.result)) {
                label = "✂PRN";
            } else {
                label = "•";
            }
            
            Text text = new Text(node.x, node.y + 1, label);
            text.setFill(visitedNodes.contains(node.id) || 
                        (currentNodeIndex >= 0 && solver.nodes.get(currentNodeIndex).id == node.id) 
                        ? Color.WHITE : Color.BLACK);
            text.setFont(Font.font("Arial", FontWeight.BOLD, node.depth < 4 ? 12 : 10));
            text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            treePane.getChildren().add(text);
            
            // Add flips count for internal nodes
            if (node.depth > 0 && node.flips > 0 && visitedNodes.contains(node.id)) {
                Text flipsText = new Text(node.x, node.y - height/2 - 3, "f=" + node.flips);
                flipsText.setFill(Color.GRAY);
                flipsText.setFont(Font.font("Arial", 8));
                flipsText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                treePane.getChildren().add(flipsText);
            }
        }
    }
    
    private void jumpToNode(int nodeId) {
        for (int i = 0; i < solver.nodes.size(); i++) {
            if (solver.nodes.get(i).id == nodeId) {
                currentNodeIndex = i;
                // Mark all nodes up to this as visited
                visitedNodes.clear();
                for (int j = 0; j <= i; j++) {
                    visitedNodes.add(solver.nodes.get(j).id);
                }
                updateDisplay();
                break;
            }
        }
    }
    
    private void updateDisplay() {
        if (solver == null || solver.nodes.isEmpty() || tiles == null) return;
        
        if (currentNodeIndex >= 0 && currentNodeIndex < solver.nodes.size()) {
            TreeNode node = solver.nodes.get(currentNodeIndex);
            
            // Update board
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    updateTileStyle(tiles[r][c], node.board[r][c], false);
                }
            }
            
            // Update info
            String resultText = "";
            Color resultColor = PRIMARY_COLOR;
            
            switch (node.result) {
                case "solution":
                    resultText = "✓ SOLUTION FOUND! (All White)";
                    resultColor = SUCCESS_COLOR;
                    break;
                case "dead":
                    resultText = "✗ DEAD END - Not All White";
                    resultColor = DANGER_COLOR;
                    break;
                case "pruned":
                    resultText = "✂ PRUNED (flips >= best)";
                    resultColor = WARNING_COLOR;
                    break;
                default:
                    if (node.index < 9) {
                        resultText = "Cell (" + node.r + "," + node.c + ") - " + 
                                   (node.edge.isEmpty() ? "ROOT" : "Edge: " + node.edge);
                    } else {
                        resultText = "Leaf Node";
                    }
            }
            
            typeLabel.setText(resultText);
            typeLabel.setTextFill(resultColor);
            
            String pathStr = node.path.toString();
            pathLabel.setText("Path: " + pathStr.substring(1, pathStr.length() - 1));
            
            statusLabel.setText("Viewing node " + (currentNodeIndex + 1) + " of " + solver.nodes.size());
            stepLabel.setText("Step: " + (currentNodeIndex + 1) + " / " + solver.nodes.size());
            
            int solutionCount = (int) solver.nodes.stream().filter(n -> "solution".equals(n.result)).count();
            int deadCount = (int) solver.nodes.stream().filter(n -> "dead".equals(n.result)).count();
            int pruneCount = (int) solver.nodes.stream().filter(n -> "pruned".equals(n.result)).count();
            
            statsLabel.setText(String.format("Solutions: %d | Dead: %d | Pruned: %d | Best: %d flips", 
                solutionCount, deadCount, pruneCount, solver.bestCost));
        } else {
            // Show initial board
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    updateTileStyle(tiles[r][c], originalBoard[r][c], false);
                }
            }
            typeLabel.setText("Ready to start - Goal: All White (●)");
            pathLabel.setText("Path: ");
            statusLabel.setText("Press Next to begin traversal");
            stepLabel.setText("Step: 0 / " + (solver.nodes.size() > 0 ? solver.nodes.size() : 0));
        }
        
        drawTree();
    }
    
    private void startAutoPlay() {
        if (autoPlaying || solver == null || solver.nodes.isEmpty()) return;
        autoPlaying = true;
        
        autoPlayTimeline = new Timeline(
            new KeyFrame(Duration.seconds(autoPlaySpeed), e -> {
                if (currentNodeIndex < solver.nodes.size() - 1) {
                    currentNodeIndex++;
                    visitedNodes.add(solver.nodes.get(currentNodeIndex).id);
                    updateDisplay();
                    scrollToCurrentNode();
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
        autoPlaySpeed = Math.max(0.5, Math.min(4.0, autoPlaySpeed));
        
        // Find speed label in control panel
        for (javafx.scene.Node node : controlPanel.getChildren()) {
            if (node instanceof HBox) {
                HBox hbox = (HBox) node;
                for (javafx.scene.Node child : hbox.getChildren()) {
                    if (child instanceof Label && "speedLabel".equals(((Label) child).getId())) {
                        ((Label) child).setText(String.format("Speed: %.1fs/step", autoPlaySpeed));
                        break;
                    }
                }
            }
        }
        
        if (autoPlaying) {
            stopAutoPlay();
            startAutoPlay();
        }
    }
    
    private void resetVisualization() {
        stopAutoPlay();
        currentNodeIndex = -1;
        visitedNodes.clear();
        updateDisplay();
        centerTreeView();
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
