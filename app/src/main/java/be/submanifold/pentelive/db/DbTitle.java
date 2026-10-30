package be.submanifold.pentelive.db;

/** Toolbar title of the DB screen: the current game's name. */
public final class DbTitle {
    private DbTitle() {
    }

    /** "Speed Pente", "Keryo-Pente", ...; the label (or "") for an unsupported game. */
    public static String of(int gameId, String fallbackLabel) {
        if (gameId == 0 || !DbGames.isSupported(gameId)) {
            return fallbackLabel == null ? "" : fallbackLabel;
        }
        return (DbGames.isSpeed(gameId) ? "Speed " : "") + DbGames.baseName(gameId);
    }
}
