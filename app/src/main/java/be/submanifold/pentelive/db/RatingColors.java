package be.submanifold.pentelive.db;

/** Rating colour bands used in the games list. */
public final class RatingColors {
    private RatingColors() {
    }

    public static int argb(int rating) {
        if (rating >= 1900) return 0xFFFF0000;
        if (rating >= 1700) return 0xFFFAF508;
        if (rating >= 1400) return 0xFF0000FF;
        if (rating >= 1000) return 0xFF1E824C;
        return 0xFF888888;
    }
}
