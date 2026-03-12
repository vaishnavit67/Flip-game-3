package pck;
import java.util.*;

/**
 * ============================================================
 *  FLIP GAME (Lights Out) — DP Strategy
 * ============================================================
 *
 *  STATE
 *    dp[row][mask] = minimum flips used for rows 0..row
 *                   such that row's residual equals mask
 *
 *  RESIDUAL of row i
 *    The lights still ON in row i after accounting for:
 *      - row i's own flip choices (and their left/right neighbours)
 *      - the row above's residual pressing down
 *    The row below hasn't acted yet — that's the next transition.
 *
 *  TRANSITION
 *    For each valid dp[i][prevMask], try every flipMask for row i+1.
 *    Compute nextResidual and update dp[i+1][nextResidual].
 *
 *  OVERLAP (two kinds)
 *    1. READ overlap  : dp[i][mask] is read by ALL 2^COLS flip choices
 *                       for row i+1 — 32 branches share one cached value.
 *    2. WRITE overlap : multiple (prevMask, flipMask) pairs produce the
 *                       same nextResidual → they compete to write
 *                       dp[i+1][nextResidual]; only the cheapest wins.
 *
 *  COMPLEXITY
 *    With DP    : O(ROWS × 4^COLS)  =  5 × 1024  =  5 120 ops
 *    Without DP : O(2^(ROWS×COLS))  =  2^25       ≈  33 million ops
 * ============================================================
 */
public class FlipGameDP {

    static final int ROWS = 5;
    static final int COLS = 5;
    static final int FULL = (1 << COLS) - 1;   // 0b11111 = 31
    static final int INF  = Integer.MAX_VALUE / 2;

    // Board: board[r] is a COLS-bit mask, 1 = light ON
    static int[] board = new int[ROWS];

    // dp[row][mask] = min flips for rows 0..row with residual = mask
    static int[][] dp = new int[ROWS][1 << COLS];

    // chosenFlip[row][residual] = which flipMask produced this dp entry
    static int[][] chosenFlip = new int[ROWS][1 << COLS];

    // prevResid[row][residual] = what prevMask (row-1 residual) was used
    // Needed to backtrack the solution path cleanly
    static int[][] prevResid = new int[ROWS][1 << COLS];

    // How many times each dp[row][mask] was read as a source
    static int[][] readCount = new int[ROWS][1 << COLS];

    // Counters
    static int writeOverlaps = 0;  // competing writes to same dp cell
    static int readShares    = 0;  // total extra subproblem reads (beyond first)

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    /** Pretty-print a COLS-bit mask, MSB = leftmost column */
    static String bits(int mask) {
        StringBuilder sb = new StringBuilder("[");
        for (int c = COLS - 1; c >= 0; c--)
            sb.append((mask >> c) & 1);
        sb.append("]");
        return sb.toString();
    }

    static int pop(int x) { return Integer.bitCount(x); }

    /**
     * Compute the residual of a row after applying:
     *   - flipMask   : which cells in this row are flipped
     *   - aboveResid : the row above's residual (forces flips from above)
     *
     * Cell c of this row is toggled by:
     *   flipMask[c]    (direct flip)
     *   flipMask[c-1]  (left neighbour's flip reaches c)
     *   flipMask[c+1]  (right neighbour's flip reaches c)
     *   aboveResid[c]  (above residual forces this cell from above)
     */
    static int residual(int rowState, int flipMask, int aboveResid) {
        int effect = flipMask
                   ^ ((flipMask << 1) & FULL)   // right neighbour presses left
                   ^ (flipMask >> 1);            // left  neighbour presses right
        return (rowState ^ effect ^ aboveResid) & FULL;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Solver
    // ─────────────────────────────────────────────────────────────────────────

    static void solve() {

        // Initialise
        for (int[] row : dp)         Arrays.fill(row, INF);
        for (int[] row : chosenFlip) Arrays.fill(row, -1);
        for (int[] row : prevResid)  Arrays.fill(row, -1);

        printHeader();

        // ── BASE CASE: row 0 ─────────────────────────────────────────────────
        System.out.println("─".repeat(72));
        System.out.printf("BASE CASE  row 0   board=%s   (try all %d flip masks)%n",
                bits(board[0]), 1 << COLS);
        System.out.println("─".repeat(72));

        for (int flip = 0; flip <= FULL; flip++) {
            int res  = residual(board[0], flip, 0);
            int cost = pop(flip);

            System.out.printf("  flip=%-8s cost=%d  -> residual=%-8s  ",
                    bits(flip), cost, bits(res));

            if (cost < dp[0][res]) {
                boolean isOverlap = dp[0][res] != INF;
                if (isOverlap) {
                    writeOverlaps++;
                    System.out.printf("ACCEPTED  (beats existing cost %d) [WRITE-OVERLAP #%d]%n",
                            dp[0][res], writeOverlaps);
                } else {
                    System.out.println("ACCEPTED  (new residual state)");
                }
                dp[0][res]         = cost;
                chosenFlip[0][res] = flip;
                prevResid[0][res]  = -1; // no row above
            } else {
                System.out.printf("REJECTED  (dp[0][%s]=%d <= %d)%n",
                        bits(res), dp[0][res], cost);
            }
        }

        // ── TRANSITIONS: row i -> row i+1 ────────────────────────────────────
        for (int i = 0; i < ROWS - 1; i++) {

            // Count valid source states
            int validSrc = 0;
            for (int m = 0; m <= FULL; m++) if (dp[i][m] != INF) validSrc++;

            System.out.println();
            System.out.println("─".repeat(72));
            System.out.printf("TRANSITION  row %d -> row %d   board[%d]=%s%n",
                    i, i + 1, i + 1, bits(board[i + 1]));
            System.out.printf(
                "  %d valid source states x %d flip choices = %d transitions%n",
                    validSrc, 1 << COLS, validSrc * (1 << COLS));
            System.out.printf(
                "  Each source state is READ by all %d flip choices " +
                "-> READ overlap of %dx per source state%n", 1 << COLS, 1 << COLS);
            System.out.println("─".repeat(72));

            for (int prev = 0; prev <= FULL; prev++) {
                if (dp[i][prev] == INF) continue;

                System.out.printf("%n  SOURCE  dp[%d][%s] = %d flips:%n",
                        i, bits(prev), dp[i][prev]);

                for (int flip = 0; flip <= FULL; flip++) {
                    int next    = residual(board[i + 1], flip, prev);
                    int newCost = dp[i][prev] + pop(flip);

                    readCount[i][prev]++;
                    if (readCount[i][prev] > 1) readShares++;

                    System.out.printf("    flip=%-8s +%d  -> dp[%d][%s]  ",
                            bits(flip), pop(flip), i + 1, bits(next));

                    if (newCost < dp[i + 1][next]) {
                        boolean isOverlap = dp[i + 1][next] != INF;
                        if (isOverlap) {
                            writeOverlaps++;
                            System.out.printf(
                                "ACCEPTED  (beats %d -> %d) [WRITE-OVERLAP #%d]%n",
                                dp[i + 1][next], newCost, writeOverlaps);
                        } else {
                            System.out.println("ACCEPTED  (new residual state)");
                        }
                        dp[i + 1][next]         = newCost;
                        chosenFlip[i + 1][next] = flip;
                        prevResid[i + 1][next]  = prev;
                    } else {
                        System.out.printf("REJECTED  (dp=%d <= %d)%n",
                                dp[i + 1][next], newCost);
                    }
                }
            }
        }

        printResult();
        printOverlapSummary();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result + solution reconstruction
    // ─────────────────────────────────────────────────────────────────────────

    static void printResult() {
        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("RESULT");
        System.out.println("=".repeat(72));

        int ans = dp[ROWS - 1][0];

        if (ans == INF) {
            System.out.println("  No solution exists for this board configuration.");
            return;
        }

        System.out.println("  Minimum flips required: " + ans);
        System.out.println();

        // Backtrack using prevResid
        // Last row must have residual 0 (all lights off)
        int[] flipPath  = new int[ROWS];
        int[] residPath = new int[ROWS];

        int curRes = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            flipPath[r]  = (chosenFlip[r][curRes] == -1) ? 0 : chosenFlip[r][curRes];
            residPath[r] = curRes;
            curRes       = (r > 0) ? prevResid[r][curRes] : -1;
        }

        System.out.println("  Reconstructed solution path (backtracked via prevResid):");
        System.out.println();
        System.out.printf("  %-5s  %-10s  %-10s  %-10s  %s%n",
                "Row", "Board", "FlipMask", "Residual", "Cells flipped");
        System.out.println("  " + "─".repeat(62));

        for (int r = 0; r < ROWS; r++) {
            int f = flipPath[r];
            StringBuilder cols = new StringBuilder();
            for (int c = 0; c < COLS; c++)
                if (((f >> (COLS - 1 - c)) & 1) == 1)
                    cols.append("col").append(c).append(" ");
            System.out.printf("  row %d  %-10s  %-10s  %-10s  %s%n",
                    r, bits(board[r]), bits(f), bits(residPath[r]),
                    cols.length() == 0 ? "(none)" : cols.toString().trim());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overlap summary
    // ─────────────────────────────────────────────────────────────────────────

    static void printOverlapSummary() {
        System.out.println();
        System.out.println("=".repeat(72));
        System.out.println("OVERLAP SUMMARY");
        System.out.println("=".repeat(72));
        System.out.println();
        System.out.println("  TWO KINDS OF OVERLAP IN THIS DP:");
        System.out.println();
        System.out.println("  1. READ overlap (subproblem reuse via memoisation)");
        System.out.println("     dp[i][mask] = 'min flips for rows 0..i with residual=mask'");
        System.out.println("     Computed ONCE, then READ by all 2^COLS flip choices for");
        System.out.println("     row i+1. Without DP, each branch would recompute it.");
        System.out.println("     Every read beyond the first = one saved recomputation.");
        System.out.println();
        System.out.println("  2. WRITE overlap (competing paths to the same residual)");
        System.out.println("     Multiple (prevMask, flipMask) pairs can map to the SAME");
        System.out.println("     nextResidual. They all try to write dp[i+1][nextResidual].");
        System.out.println("     Only the cheapest cost wins. Each such collision counted.");
        System.out.println();

        System.out.println("  dp[row][mask] read counts:");
        System.out.println();
        for (int r = 0; r < ROWS - 1; r++) {
            for (int m = 0; m <= FULL; m++) {
                if (readCount[r][m] > 0) {
                    System.out.printf(
                        "    dp[%d][%s] = %-3s  read %2d times  (%d extra reuses saved)%n",
                        r, bits(m),
                        dp[r][m] == INF ? "INF" : String.valueOf(dp[r][m]),
                        readCount[r][m],
                        readCount[r][m] - 1);
                }
            }
        }

        int totalValid = countValid();

        System.out.println();
        System.out.printf("  Unique subproblems (valid dp cells)          : %d%n", totalValid);
        System.out.printf("  Total subproblem reads                       : %d%n",
                readShares + totalValid);
        System.out.printf("  Extra reads saved by memoisation (READ ovlp) : %d%n", readShares);
        System.out.println();
        System.out.println("  +----------------------------------------------------------+");
        System.out.printf( "  |  Write overlaps (competing paths, cheapest kept) : %-5d  |%n",
                writeOverlaps);
        System.out.printf( "  |  Read  overlaps (subproblem reuses via memoisation): %-4d  |%n",
                readShares);
        System.out.printf( "  |  TOTAL overlaps                                   : %-5d  |%n",
                writeOverlaps + readShares);
        System.out.println("  +----------------------------------------------------------+");
        System.out.println();
        System.out.println("  COMPLEXITY COMPARISON:");
        System.out.println("  Without DP : O(2^(ROWS x COLS)) = O(2^25) ~= 33 000 000 ops");
        System.out.println("  With DP    : O(ROWS x 4^COLS)   = O(5 x 1024) = 5 120 ops");
        System.out.printf( "  Speedup    : ~%.0fx%n", 33_000_000.0 / 5120.0);
    }

    static int countValid() {
        int c = 0;
        for (int r = 0; r < ROWS; r++)
            for (int m = 0; m <= FULL; m++)
                if (dp[r][m] != INF) c++;
        return c;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────────────────

    static void printHeader() {
        System.out.println("=".repeat(72));
        System.out.println("  FLIP GAME — DP SOLVER");
        System.out.println("=".repeat(72));
        System.out.println();
        System.out.println("  Board (1 = light ON, MSB = leftmost column):");
        System.out.println();
        for (int r = 0; r < ROWS; r++)
            System.out.printf("    row %d : %s%n", r, bits(board[r]));
        System.out.println();
        System.out.println("  Goal : reach residual [00000] at the last row.");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main — change board values here to try different configurations
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        /*
         *  Board layout (1 = light ON):
         *
         *   row 0:  1 0 1 0 1   =  0b10101  = 21
         *   row 1:  0 1 1 0 0   =  0b01100  = 12
         *   row 2:  1 0 0 1 0   =  0b10010  = 18
         *   row 3:  0 0 1 0 1   =  0b00101  =  5
         *   row 4:  1 1 0 0 0   =  0b11000  = 24
         */
        board[0] = 0b10101;
        board[1] = 0b01100;
        board[2] = 0b10010;
        board[3] = 0b00101;
        board[4] = 0b11000;

        solve();
    }
}
