package be.submanifold.pentelive.net;

/**
 * Render seam for the whos-online dashboard popup. Lets the Android render logic
 * in {@code MainActivity} be driven by a plain, unit-testable presenter
 * ({@link WhosOnlinePresenter}) instead of an AsyncTask.
 */
public interface WhosOnlineView {
    void renderWhosOnline(WhosOnline data);

    void showError(Result.Reason reason);
}
