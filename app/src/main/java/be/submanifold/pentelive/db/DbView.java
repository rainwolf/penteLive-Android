package be.submanifold.pentelive.db;

import java.util.List;
import java.util.Map;

/** What {@link DbController} renders into. Called on the main thread only, and only while attached. */
public interface DbView {
    void showPosition(List<Integer> moves, CaptureIcons.Counts caps);

    void showDots(Map<Integer, Integer> cellArgb);

    void showMoves(List<DbController.MoveRow> rows);

    void clearResults();

    void showBlocked();

    /** Go, Renju or an unknown game. */
    void showUnsupported();

    void showLoginPrompt();

    void showSubscribeGate();

    void showError(DbController.Endpoint e);

    /** All games loaded so far for the position, the server total, and whether more pages remain. */
    void showGames(List<DbDtos.GameHeader> games, int total, boolean hasMore);
}
