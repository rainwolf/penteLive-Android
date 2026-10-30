package be.submanifold.pentelive.db;

/** Board coordinate label for a cell (column letters skip I; row 1 at the bottom). */
public final class MoveLabel {
    private static final String COLUMNS = "ABCDEFGHJKLMNOPQRST";

    private MoveLabel() {}

    /** "K10", "E5", ...; the pass sentinel ({@code gridSize²}) reads "PASS", anything else off-board "?". */
    public static String label(int cell, int gridSize) {
        if (cell == gridSize * gridSize) return "PASS";
        if (cell < 0 || cell > gridSize * gridSize) return "?";
        return COLUMNS.charAt(cell % gridSize) + String.valueOf(gridSize - cell / gridSize);
    }
}
