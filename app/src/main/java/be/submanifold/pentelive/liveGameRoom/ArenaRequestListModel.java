package be.submanifold.pentelive.liveGameRoom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owner-side arena request list (spec C2). It renders only the server's latest snapshot and never
 *  removes a row on its own. Snapshots that arrive while the owner dialog is showing freeze the list
 *  for FREEZE_MS, then apply; after each unfreeze the list stays interactive for at least
 *  MIN_INTERACTIVE_MS, holding newer snapshots until then. Plain Java: time comes from the
 *  injected Scheduler, so the rules are unit-tested without Android. */
public final class ArenaRequestListModel {

    public static final long FREEZE_MS = 400L;
    public static final long MIN_INTERACTIVE_MS = 1000L;

    public interface Cancellable { void cancel(); }

    public interface Scheduler { Cancellable schedule(Runnable task, long delayMs); }

    public interface Listener { void onChanged(); }

    private enum Phase { IDLE, FROZEN, WINDOW }

    private final Scheduler scheduler;
    private final Listener listener;

    private List<ArenaJoinRequest> rows = Collections.emptyList();
    private List<ArenaJoinRequest> held;              // newest snapshot not yet applied, or null
    private Set<String> lastReceived = new HashSet<>();
    private String inFlightPlayer;
    private boolean shown;                            // the owner dialog is on screen
    private Phase phase = Phase.IDLE;
    private Cancellable timer;

    public ArenaRequestListModel(Scheduler scheduler, Listener listener) {
        this.scheduler = scheduler;
        this.listener = listener;
    }

    /** Takes a dsgArenaJoinRequestsEvent snapshot (already in seq order). Returns true when it
     *  contains a player who was not in the previous snapshot (the new-request cue). */
    public boolean onSnapshot(List<ArenaJoinRequest> snapshot) {
        List<ArenaJoinRequest> copy = Collections.unmodifiableList(new ArrayList<>(snapshot));
        Set<String> names = new HashSet<>();
        for (ArenaJoinRequest r : copy) names.add(r.player);
        boolean newcomer = !lastReceived.containsAll(names);
        lastReceived = names;

        boolean answersAction = inFlightPlayer != null;
        inFlightPlayer = null;

        if (!shown) {
            // No dialog on screen: nothing is under the owner's finger, so take the snapshot at once.
            rows = copy;
        } else if (phase == Phase.IDLE && rows.isEmpty() && copy.isEmpty()) {
            // Empty onto empty: nothing would move, so no freeze.
            rows = copy;
        } else {
            // The dialog is showing, even with an empty list: a first row would grow the dialog
            // and move "Exit table" under the owner's finger, so it freezes like any change.
            held = copy;
            if (phase == Phase.IDLE || (phase == Phase.WINDOW && answersAction)) {
                startFreeze();
            }
            // FROZEN: the held snapshot was replaced. WINDOW: held until the window ends.
        }
        listener.onChanged();
        return newcomer;
    }

    /** The owner dialog is now on screen; from here on every snapshot freezes before it applies.
     *  Nothing shown changes, so the listener is not called. reset() hides the list again. */
    public void onShown() {
        shown = true;
    }

    /** Called for a tap (accept) or swipe (decline) on {@code player}. Returns false, changing
     *  nothing, when the list is frozen, an action already awaits the server, or the player is
     *  not shown. Otherwise the row is in flight and every tap is ignored until the next snapshot. */
    public boolean beginAction(String player) {
        if (player == null || !isInteractive()) return false;
        boolean shown = false;
        for (ArenaJoinRequest r : rows) {
            if (r.player.equals(player)) {
                shown = true;
                break;
            }
        }
        if (!shown) return false;
        inFlightPlayer = player;
        listener.onChanged();
        return true;
    }

    public List<ArenaJoinRequest> rows() {
        return rows;
    }

    public boolean isFrozen() {
        return phase == Phase.FROZEN;
    }

    public boolean isInteractive() {
        return !rows.isEmpty() && phase != Phase.FROZEN && inFlightPlayer == null;
    }

    public String inFlightPlayer() {
        return inFlightPlayer;
    }

    /** Clears everything, cancels the pending timer and marks the list hidden, e.g. when the table
     *  fills and the dialog closes. */
    public void reset() {
        cancelTimer();
        rows = Collections.emptyList();
        held = null;
        lastReceived = new HashSet<>();
        inFlightPlayer = null;
        shown = false;
        phase = Phase.IDLE;
        listener.onChanged();
    }

    private void startFreeze() {
        cancelTimer();
        phase = Phase.FROZEN;
        timer = scheduler.schedule(this::endFreeze, FREEZE_MS);
    }

    private void endFreeze() {
        timer = null;
        if (held != null) {
            rows = held;
            held = null;
        }
        if (rows.isEmpty()) {
            phase = Phase.IDLE; // empty snapshot: the list hides
        } else {
            phase = Phase.WINDOW;
            timer = scheduler.schedule(this::endWindow, MIN_INTERACTIVE_MS);
        }
        listener.onChanged();
    }

    private void endWindow() {
        timer = null;
        if (held != null) {
            startFreeze();
        } else {
            phase = Phase.IDLE;
        }
        listener.onChanged();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
