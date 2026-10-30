package be.submanifold.pentelive.db;

/** Per-endpoint stale guard: only the most recently issued id is latest (spec 4.0). Main thread only. */
public final class LatestRequest {
    private int latest;

    public int next() {
        return ++latest;
    }

    public boolean isLatest(int id) {
        return id == latest;
    }
}
