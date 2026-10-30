package be.submanifold.pentelive.db;

/**
 * Board geometry for DBBoardView on an n x n grid (9, 13, 15 or 19). Cells are row-major
 * ({@code row * n + col}); index {@code i} is a column or row. Pure Java.
 */
public final class DbBoardGeometry {
    private static final int[] PENTE_CIRCLES = {120, 126, 180, 234, 240};
    private static final int[] GO_19 = {60, 66, 72, 174, 180, 186, 288, 294, 300};
    private static final int[] GO_13 = {42, 45, 48, 81, 84, 87, 120, 123, 126};
    private static final int[] GO_9 = {20, 24, 40, 56, 60};
    private static final int[] RENJU = {48, 52, 56, 108, 112, 116, 168, 172, 176};

    private DbBoardGeometry() {
    }

    /** The cell under (x, y) on a board {@code size} wide, or -1 outside it. */
    public static int cellAt(float x, float y, float size, int n) {
        if (size <= 0 || x < 0 || y < 0 || x > size || y > size) return -1;
        int col = Math.min(n - 1, (int) (n * x / size));
        int row = Math.min(n - 1, (int) (n * y / size));
        return row * n + col;
    }

    /** Pixel centre of column or row {@code i}. */
    public static float centre(int i, float size, int n) {
        return size * i / n + size / (2f * n);
    }

    public static float stoneRadius(float size, int n) {
        return size / (2f * n + 1);
    }

    /** The aiming stone while zoomed; size/30 on 19x19, scaled for smaller grids. */
    public static float zoomedStoneRadius(float size, int n) {
        return size * 19f / (30f * n);
    }

    /** Marked points (web boardSpecialPoints): Pente circles, Go/Renju star points. */
    public static int[] starPoints(int gameId) {
        if (!DbGames.blackMovesFirst(gameId)) return PENTE_CIRCLES.clone();
        if (!DbGames.isGo(gameId)) return RENJU.clone();
        switch (DbGames.gridSize(gameId)) {
            case 9:
                return GO_9.clone();
            case 13:
                return GO_13.clone();
            default:
                return GO_19.clone();
        }
    }

    /** Go and Renju draw filled star dots; the Pente family keeps its stroked circles. */
    public static boolean filledStarPoints(int gameId) {
        return DbGames.blackMovesFirst(gameId);
    }
}
