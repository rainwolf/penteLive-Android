package be.submanifold.pentelive.db;

/** Board coordinate label for a cell (column letters skip I; row 1 at the bottom). */
public final class MoveLabel {
    private static final String COLUMNS = "ABCDEFGHJKLMNOPQRST";

    private MoveLabel() {}

    public static String label(int cell, int gridSize) {
        return COLUMNS.charAt(cell % gridSize) + String.valueOf(gridSize - cell / gridSize);
    }
}
