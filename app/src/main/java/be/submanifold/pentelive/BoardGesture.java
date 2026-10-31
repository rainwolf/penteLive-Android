package be.submanifold.pentelive;

/**
 * Pure-Java touch classifier shared by the Android game boards. Decides whether a touch is a
 * placement (with the zoom loupe) or a horizontal swipe, from distance and direction only; speed
 * and hold time at release are never used, so a slow or held swipe is still a swipe.
 *
 * <p>No android.* imports so it can be unit tested on the JVM.
 */
public final class BoardGesture {
    /** Movement needed before direction is decided. */
    public static final float SLOP_DP = 10f;
    /** Horizontal travel needed at release for a latched swipe to fire. */
    public static final float SWIPE_MIN_DP = 40f;
    /** |dx| must be at least this times |dy| when the touch passes the slop to latch a swipe. */
    public static final float LATCH_RATIO = 1.5f;
    /** At release |dx| must still be at least this times |dy|. */
    public static final float UP_RATIO = 1.0f;
    /** Holding still this long arms the loupe. */
    public static final long LOUPE_HOLD_MS = 250;

    public enum State { IDLE, PENDING, AIMING, SWIPING }

    public enum Action {
        NONE,
        SHOW_LOUPE,
        MOVE_LOUPE,
        PLACE,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        CANCEL
    }

    /** A swipe never ran touch-down work, so any staged stone is still valid and must survive it. */
    public static boolean swipeKeepsStagedStone(Action a) {
        return a == Action.SWIPE_LEFT || a == Action.SWIPE_RIGHT;
    }

    private final float slopPx;
    private final float swipeMinPx;
    private final boolean swipesEnabled;
    private final boolean aimEnabled;

    private State state = State.IDLE;
    private float downX, downY;
    private long downT;
    private int latchedSign;

    public BoardGesture(float density, boolean swipesEnabled) {
        this(density, swipesEnabled, true);
    }

    /** aimEnabled false (read-only viewer): never aims or places; only swipes can result. */
    public BoardGesture(float density, boolean swipesEnabled, boolean aimEnabled) {
        this.aimEnabled = aimEnabled;
        this.slopPx = SLOP_DP * density;
        this.swipeMinPx = SWIPE_MIN_DP * density;
        this.swipesEnabled = swipesEnabled;
    }

    public Action down(float x, float y, long tMs) {
        downX = x;
        downY = y;
        downT = tMs;
        latchedSign = 0;
        if (!swipesEnabled) {
            if (!aimEnabled) {
                state = State.IDLE;
                return Action.NONE;
            }
            state = State.AIMING;
            return Action.SHOW_LOUPE;
        }
        state = State.PENDING;
        return Action.NONE;
    }

    public Action move(float x, float y, long tMs) {
        switch (state) {
            case PENDING: {
                float dx = x - downX;
                float dy = y - downY;
                if (Math.hypot(dx, dy) < slopPx) {
                    if (aimEnabled && tMs - downT >= LOUPE_HOLD_MS) {
                        state = State.AIMING;
                        return Action.SHOW_LOUPE;
                    }
                    return Action.NONE;
                }
                if (Math.abs(dx) >= LATCH_RATIO * Math.abs(dy)) {
                    state = State.SWIPING;
                    latchedSign = dx < 0 ? -1 : 1;
                    return Action.NONE;
                }
                if (!aimEnabled) return Action.NONE;
                state = State.AIMING;
                return Action.SHOW_LOUPE;
            }
            case AIMING:
                return Action.MOVE_LOUPE;
            default:
                return Action.NONE;
        }
    }

    /** Driven by a postDelayed runnable: a perfectly still finger may send no move events. */
    public Action tick(long tMs) {
        if (aimEnabled && state == State.PENDING && tMs - downT >= LOUPE_HOLD_MS) {
            state = State.AIMING;
            return Action.SHOW_LOUPE;
        }
        return Action.NONE;
    }

    public Action up(float x, float y, long tMs) {
        State s = state;
        state = State.IDLE;
        switch (s) {
            case PENDING: {
                // A release that never produced a MOVE past the slop may still be a swipe.
                float dx = x - downX;
                float dy = y - downY;
                if (Math.hypot(dx, dy) >= slopPx
                        && Math.abs(dx) >= LATCH_RATIO * Math.abs(dy)) {
                    if (Math.abs(dx) >= swipeMinPx) {
                        return dx < 0 ? Action.SWIPE_LEFT : Action.SWIPE_RIGHT;
                    }
                    return Action.CANCEL;
                }
                return aimEnabled ? Action.PLACE : Action.NONE;
            }
            case AIMING:
                return Action.PLACE;
            case SWIPING: {
                float dx = x - downX;
                float dy = y - downY;
                int sign = dx < 0 ? -1 : 1;
                if (Math.abs(dx) >= swipeMinPx
                        && Math.abs(dx) >= UP_RATIO * Math.abs(dy)
                        && sign == latchedSign) {
                    return sign < 0 ? Action.SWIPE_LEFT : Action.SWIPE_RIGHT;
                }
                return Action.CANCEL;
            }
            default:
                return Action.NONE;
        }
    }

    /** ACTION_CANCEL or a second pointer. */
    public Action cancel() {
        state = State.IDLE;
        return Action.CANCEL;
    }

    public State state() {
        return state;
    }

    public boolean loupeAllowed() {
        return state == State.AIMING;
    }

    public boolean swipeLatched() {
        return state == State.SWIPING;
    }
}
