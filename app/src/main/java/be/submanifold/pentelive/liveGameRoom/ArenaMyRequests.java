package be.submanifold.pentelive.liveGameRoom;

import java.util.HashSet;
import java.util.Set;

/** Requester-side arena lobby state (spec C3). The requested set is the latest
 *  dsgArenaMyRequestsEvent. A tap sends at most one action per table until the next
 *  dsgArenaMyRequestsEvent arrives, which the server sends for every request and withdraw (R11). */
public final class ArenaMyRequests {

    public enum Tap { REQUEST, CONFIRM_WITHDRAW, IGNORE }

    private Set<Integer> requested = new HashSet<>();
    private final Set<Integer> inFlight = new HashSet<>();

    /** REQUEST: send the request now (the table is now in flight). CONFIRM_WITHDRAW: ask first.
     *  IGNORE: an action for this table still awaits the server. */
    public Tap onTap(int table) {
        if (inFlight.contains(table)) return Tap.IGNORE;
        if (requested.contains(table)) return Tap.CONFIRM_WITHDRAW;
        inFlight.add(table);
        return Tap.REQUEST;
    }

    /** The withdraw dialog was confirmed. True means send the withdraw. False when the request
     *  has already ended (or an action is in flight): a withdraw then would only block the table. */
    public boolean confirmWithdraw(int table) {
        if (inFlight.contains(table) || !requested.contains(table)) return false;
        inFlight.add(table);
        return true;
    }

    public void onMyRequests(Set<Integer> tables) {
        requested = new HashSet<>(tables);
        inFlight.clear();
    }

    public boolean isRequested(int table) {
        return requested.contains(table);
    }
}
