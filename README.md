# Flip-game-3
# 🔆 Flip Game Analysis — Four Algorithms on a 6×6 Lights Out Board

A JavaFX application that lets you play the classic **Lights Out** puzzle manually, then watch **four different algorithms** solve it simultaneously — with real-time animation and a performance comparison across 30 boards.

---

## 📌 What Is the Flip Game?

Lights Out is a classic puzzle played on an N×N grid of lights.

- Each cell is either **ON (white ●)** or **OFF (dark ○)**
- Clicking any cell **toggles it and its 4 neighbours** (up, down, left, right)
- **Goal:** Turn ALL cells WHITE (ON)

The challenge is that every move affects multiple cells, making naive solutions ineffective.

---

## 🎮 Features

- **Manual Play Mode** — Try to solve a randomly generated 6×6 board yourself
- **Give Up** — Let all four algorithms take over and solve it simultaneously
- **Side-by-side animation** — Watch each algorithm apply moves step by step in real time
- **Analysis Mode** — Bar chart and table comparing performance across 30 precomputed boards
- **Parallel execution** — All four solvers run on separate threads simultaneously

---

## 🧠 The Four Algorithms

### 1. 🔴 Graph + Greedy
**How it works:**
- Tries all `2^N` possible flip patterns for the first row (64 patterns for 6×6)
- For each pattern, forces the remaining rows greedily — if a cell in row `r` is OFF, flip the cell directly below it in row `r+1`
- Checks if the last row ends up all ON
- Picks the pattern that results in fewest total moves

**Time Complexity:** `O(2^N × N²)`
For 6×6: `64 × 36 = 2,304` operations

**Correctness:** ✅ Always finds a solution if one exists. Not always minimum moves globally but fast and reliable.

---

### 2. 🔵 Divide and Conquer (Greedy Variant)
**How it works:**
- Divides the board into overlapping regions (quadrants, halves, full board)
- Solves each region independently using the same row-by-row greedy approach
- Tries all flip patterns for the top row of each region, forces remaining rows

**Time Complexity:** `O(2^N × N²)`
For 6×6: `64 × 36 = 2,304++` operations

**Correctness:** ⚠️ Not always correct — regions overlap and flips cross boundaries, so solving one region can disrupt another. Returns null if full board is not solved.

---

### 3. 🟢 Backtracking
**How it works:**
- For every cell, makes a binary decision: **press** or **skip**
- Recursively tries both choices for all `N²` cells
- Prunes branches where current cost already exceeds best known solution
- Has a **10-second timeout** — falls back to Graph+Greedy if exceeded

**Time Complexity:** `O(2^(N²))`
For 6×6: `2^36 ≈ 68 billion` in worst case (always times out on 6×6)

**Correctness:** ✅ Optimal if it completes. For 6×6 it always times out and uses Graph+Greedy results as fallback.

---

### 4. 🟣 Dynamic Programming (BFS)
**How it works:**
- Represents the entire board as a single **64-bit integer** (bitmask)
- Flipping a cell = XOR with a precomputed flip mask (toggles cell + 4 neighbours in one operation)
- BFS explores board states **level by level** — all 1-flip states, then 2-flip, etc.
- **HashMap** stores every visited state (the DP table) — any state already seen is an **overlap** and is skipped immediately
- First time goal state is reached = minimum flips guaranteed
- Has a **5-second timeout** and **1 million state cap** — falls back to Graph+Greedy if exceeded

**Time Complexity:** `O(N² × 2^(N²))`
For 6×6: `36 × 2^36 ≈ 2.4 trillion` in worst case (always hits limits on 6×6)

**Correctness:** ✅ Optimal and guaranteed minimum moves — if it completes within limits.

---

## 📊 Algorithm Comparison

| Algorithm | Time Complexity | 6×6 Operations | Optimal? | Completes on 6×6? |
|---|---|---|---|---|
| Graph+Greedy | O(2^N × N²) | ~2,304 | Near-optimal | ✅ Always |
| Divide&Conquer | O(7 × 2^(N/2) × N²) | ~2,016 | No | ⚠️ Sometimes |
| Backtracking | O(2^(N²)) | ~68 billion | Yes | ❌ Times out (10s) |
| DP (BFS) | O(N² × 2^(N²)) | ~2.4 trillion | Yes | ❌ Hits state cap (5s) |

> **Note:** For 6×6 boards, Backtracking and DP always hit their limits and fall back to Graph+Greedy results. The analysis section uses precomputed realistic data to demonstrate expected relative performance.

---

## 🔬 Why DP Is Special — Overlapping Subproblems

The DP solver uses **genuine dynamic programming** because:

**Overlapping Subproblems exist:**
```
flip(A) then flip(B)  →  State X
flip(B) then flip(A)  →  State X  ← same state, different path
```
Since flip order does not matter, many different move sequences reach the same board configuration. Without DP each would be explored separately. The HashMap detects and eliminates all duplicates instantly.

**Optimal Substructure:**
```
min flips to solve from State S
= 1 + min flips to solve from (S XOR flipMask[i])
```

**BFS guarantees minimum moves** because it processes states level by level — the first time the goal state is reached it is provably at minimum depth.

---

## 🗂️ Project Structure

```
FlipGameAllInOne.java
│
├── Main Menu
│   ├── Play Manual          → manual 6×6 board
│   └── See Analysis         → bar chart + 30-board table
│
├── Manual Play Mode
│   ├── Click tiles to flip
│   ├── Give Up → triggers parallel solver animation
│   └── New Board / Main Menu
│
├── Algorithm Animation Screen
│   ├── 2×2 grid of boards (one per algorithm)
│   ├── All four solve simultaneously (parallel threads)
│   └── Each board animates moves step by step
│
└── Solver Implementations
    ├── solveGraphGreedy()
    ├── solveDivideConquerGreedy()
    ├── solveBacktracking()  (with BacktrackSolver inner class)
    └── solveDP()            (BFS + bitmask + HashMap)
```

---

## 🚀 How to Run

### Prerequisites
- Java 11 or higher
- JavaFX SDK (11+)

### Compile and Run

```bash
# Compile
javac --module-path /path/to/javafx/lib \
      --add-modules javafx.controls,javafx.fxml \
      -d out src/pck/FlipGameAllInOne.java

# Run
java --module-path /path/to/javafx/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out pck.FlipGameAllInOne
```

### Using Eclipse
1. Add JavaFX as a library in Project Structure
2. Add VM options: `--module-path /path/to/javafx/lib --add-modules javafx.controls`
3. Run `FlipGameAllInOne.java`

---

## 🧩 Key Implementation Details

### Bitmask Representation
```java
// Each cell = one bit in a long integer
// Flipping cell i = XOR with precomputed mask
long next = current ^ flipMasks[i];
```

### Flip Mask Precomputation
```java
// For each cell, compute which bits it toggles
// (itself + up to 4 neighbours)
mask |= (1L << pos);  // for cell and each valid neighbour
```

### BFS + DP Check
```java
if (!parent.containsKey(next)) {
    parent.put(next, current);   // record path
    queue.add(next);             // explore later
}
// Already seen = OVERLAP = skip immediately
```

### Parallel Execution
```java
ExecutorService executor = Executors.newFixedThreadPool(4);
// All four solvers run simultaneously on separate threads
// Results collected and animation triggered on JavaFX thread
```

---

## 📈 Analysis Section

The **See Analysis** screen shows precomputed performance data across 30 different 6×6 boards:

- **Bar chart** comparing average time (ms) and average moves per algorithm
- **Board list** showing each board's miniature grid and per-algorithm stats

> The analysis data uses realistic precomputed ranges because the actual Backtracking and DP solvers cannot complete within reasonable time on 6×6 boards.

---

## 📚 Concepts Demonstrated

- **Dynamic Programming** — memoisation via HashMap, overlapping subproblems, optimal substructure
- **BFS shortest path** — level-by-level exploration guaranteeing minimum moves
- **Bitmask manipulation** — entire board state as one integer, XOR for O(1) flip
- **Backtracking with pruning** — binary decision tree with cost-based branch cutting
- **Greedy algorithms** — row-by-row forced flip strategy
- **Parallel computing** — ExecutorService with four simultaneous solver threads
- **JavaFX animation** — SequentialTransition, ParallelTransition, ScaleTransition

---

## ⚠️ Known Limitations

- **Divide and Conquer** does not correctly solve all boards — flips cross region boundaries
- **Backtracking and DP** always time out on 6×6 and fall back to Graph+Greedy
- **Analysis data** is precomputed/simulated — not from actual solver runs on those boards
- Board size is hardcoded to 6×6 — changing `BOARD_SIZE` requires recompilation

---

## 📄 License

This project is for educational purposes — demonstrating algorithm design, complexity analysis, and dynamic programming concepts through an interactive puzzle game.
