package be.submanifold.pentelive.net;

/**
 * Pure (Android-free) presenter for the whos-online endpoint: maps a
 * {@link Result}&lt;{@link WhosOnline}&gt; to a {@link WhosOnlineView} call.
 * Ok renders the rooms; a failure shows the typed error. Replaces the
 * success/failure branching that lived in {@code LoadWhosOnlineTask.onPostExecute}.
 */
public final class WhosOnlinePresenter {

    private final WhosOnlineView view;

    public WhosOnlinePresenter(WhosOnlineView view) {
        this.view = view;
    }

    public void onResult(Result<WhosOnline> r) {
        if (r.isOk()) {
            view.renderWhosOnline(r.value);
        } else {
            view.showError(r.failure.reason);
        }
    }
}
