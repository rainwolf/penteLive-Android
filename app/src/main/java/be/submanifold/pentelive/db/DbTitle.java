package be.submanifold.pentelive.db;

/** Toolbar title of the DB screen: the current game's name. */
public final class DbTitle {
    private DbTitle() {
    }

    /** As {@link #of(int, String)}, but {@code screenDefault} when that would be empty. */
    public static String of(int gameId, String fallbackLabel, String screenDefault) {
        String t = of(gameId, fallbackLabel);
        return t.isEmpty() ? screenDefault : t;
    }

    /** "Speed Pente", "Keryo-Pente", ...; the label (or "") for an unsupported game. */
    public static String of(int gameId, String fallbackLabel) {
        if (gameId == 0 || !DbGames.isSupported(gameId)) {
            return fallbackLabel == null ? "" : fallbackLabel;
        }
        return DbGames.label(gameId);
    }

    /**
     * Board label for a DB game id: the first legacy spinner label that maps to it (unchanged for the
     * Pente family), else the catalog name (Go/Renju are not in the legacy array), else the first label.
     */
    public static String boardLabel(int gameId, String[] legacyLabels) {
        for (String t : legacyLabels) {
            Integer id = DbGames.fromGameType(t);
            if (id != null && id == gameId) return t;
        }
        if (gameId != 0 && DbGames.isSupported(gameId)) return DbGames.label(gameId);
        return legacyLabels[0];
    }
}
