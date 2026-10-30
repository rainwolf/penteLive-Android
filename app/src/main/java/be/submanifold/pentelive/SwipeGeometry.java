package be.submanifold.pentelive;

/**
 * Pure geometry for the board swipe gestures. Android's system Back gesture claims horizontal
 * swipes that start near a screen edge, and the boards span the full screen width, so each board
 * excludes a band of itself from it. The system honours at most 200 dp of height per edge.
 */
public final class SwipeGeometry {
    static final int MAX_EXCLUSION_DP = 200;

    private SwipeGeometry() {}

    /** Returns {left, top, right, bottom}: full width, at most 200 dp tall, vertically centred. */
    public static int[] exclusionBand(int width, int height, float density) {
        if (width <= 0 || height <= 0) return new int[] {0, 0, 0, 0};
        int band = Math.min(height, Math.round(MAX_EXCLUSION_DP * density));
        int top = (height - band) / 2;
        return new int[] {0, top, width, top + band};
    }
}
