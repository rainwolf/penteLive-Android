package be.submanifold.pentelive.kids;

import be.submanifold.pentelive.Game;

/** Builds {@link Game} objects for the read-only views of a kid's active games. */
public final class KidGames {
    private KidGames() { }

    /**
     * A read-only, inactive Game carrying only the gid and game name. The opponent name and
     * privacy flag are deliberately null: {@code Game.getBoardString()} builds its info line once,
     * as soon as both are non-null, and never rebuilds it, and {@code BoardView.onDraw} calls it
     * before {@code RetrieveGame} returns. Non-null values here would freeze a line reading
     * "rating null" / "Remaining time: null". With both null, the line is first built after
     * {@code parseGame} fills them from the JSON. This matches the spectator path in
     * {@code WebViewActivity} (all nulls apart from the gid, then {@code setActive(false)}).
     */
    public static Game readOnlyGame(KidAccounts.ActiveGame g) {
        Game game = new Game(String.valueOf(g.gid), null, g.gameName, null, null, null, null, null,
                null, null, null);
        game.setActive(false);
        return game;
    }
}
