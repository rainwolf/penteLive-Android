package be.submanifold.pentelive.db;

import java.util.List;

/**
 * How the DB screen builds its {@link DbController}: from the launch extras on a fresh start, or from the
 * game saved in the instance state after process death (the extras are then ignored, so the saved moves
 * are never replayed on a different game). Pure Java.
 */
public final class DbStart {
    /** {@link DbController} gameOverride: null = stored filters' game, 0 = unsupported. */
    public final Integer gameOverride;
    /** Extras moves for a fresh start; null on restore (DbController.restore puts the saved ones back). */
    public final List<Integer> initialMoves;
    /** Game label for the board and toolbar; null = derive it from the controller's game. */
    public final String label;

    private DbStart(Integer gameOverride, List<Integer> initialMoves, String label) {
        this.gameOverride = gameOverride;
        this.initialMoves = initialMoves;
        this.label = label;
    }

    /**
     * @param savedGameId {@code controller.gameId()} from the saved instance state, or null on a fresh start
     * @param savedLabel  the game label saved with it
     */
    public static DbStart resolve(String extraLabel, List<Integer> extraMoves, Integer savedGameId, String savedLabel) {
        if (savedGameId != null) {
            return new DbStart(savedGameId, null, savedLabel);
        }
        if (extraLabel == null) {
            return new DbStart(null, null, null);
        }
        Integer id = DbGames.fromGameType(extraLabel);
        return new DbStart(id == null ? 0 : id, extraMoves, extraLabel);
    }
}
