package be.submanifold.pentelive.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Viewer state for one game opened from the games list: the full move list plus a cursor.
 * Only a result whose header gid matches the requested id is accepted (spec 4.0). Pure Java.
 */
public final class GameViewerModel {
    private final long requestedId;
    private DbDtos.GameHeader header;
    private int[] moves = new int[0];
    private int cursor;
    private boolean turnSteps;

    public GameViewerModel(long requestedId) {
        this.requestedId = requestedId;
    }

    public long requestedId() {
        return requestedId;
    }

    /** False when the result is not OK, is blocked, has no header, or is for another gid. */
    public boolean accept(DbResult<DbDtos.GameDetail> r) {
        if (r == null || r.kind != DbResult.Kind.OK || r.value == null) return false;
        if (r.value.blocked || r.value.header == null || r.value.header.gid != requestedId) return false;
        header = r.value.header;
        moves = r.value.moves == null ? new int[0] : r.value.moves.clone();
        cursor = moves.length;
        turnSteps = DbGames.isSupported(header.game) && DbGames.stepsByTurn(header.game);
        return true;
    }

    /** Why a result was not accepted; drives the viewer's state. */
    public enum Rejection { BLOCKED, UNAUTHORIZED, SUBSCRIPTION_REQUIRED, FAILED }

    /** For a result {@link #accept} turned down: blocked, login, subscribe gate, or retry (anything else). */
    public static Rejection rejection(DbResult<DbDtos.GameDetail> r) {
        if (isBlocked(r)) return Rejection.BLOCKED;
        if (r != null && r.kind == DbResult.Kind.UNAUTHORIZED) return Rejection.UNAUTHORIZED;
        if (r != null && r.kind == DbResult.Kind.SUBSCRIPTION_REQUIRED) return Rejection.SUBSCRIPTION_REQUIRED;
        return Rejection.FAILED;
    }

    /** A 403 {@code blocked} or an OK reply with {@code blocked: true}. */
    public static boolean isBlocked(DbResult<DbDtos.GameDetail> r) {
        if (r == null) return false;
        if (r.kind == DbResult.Kind.BLOCKED) return true;
        return r.kind == DbResult.Kind.OK && r.value != null && r.value.blocked;
    }

    /** Every move of the loaded game, regardless of the cursor. */
    public List<Integer> allMoves() {
        List<Integer> out = new ArrayList<>(moves.length);
        for (int m : moves) out.add(m);
        return Collections.unmodifiableList(out);
    }

    public List<Integer> movesUpTo() {
        List<Integer> out = new ArrayList<>(cursor);
        for (int i = 0; i < cursor; i++) out.add(moves[i]);
        return Collections.unmodifiableList(out);
    }

    /** One stone back; in Connect6 turn-step mode to the previous turn boundary (sizes 1, 3, 5, ...). */
    public boolean back() {
        if (cursor <= 0) return false;
        int target = cursor - 1;
        if (turnSteps) target = (cursor % 2 == 1) ? cursor - 2 : cursor - 1;
        cursor = Math.max(target, 0);
        return true;
    }

    /** One stone forward; in Connect6 turn-step mode to the next turn boundary. */
    public boolean forward() {
        if (cursor >= moves.length) return false;
        int count = (turnSteps && cursor % 2 == 1) ? 2 : 1;
        cursor = Math.min(cursor + count, moves.length);
        return true;
    }

    /** Jumps to the empty board. */
    public boolean first() {
        if (cursor <= 0) return false;
        cursor = 0;
        return true;
    }

    /** Jumps to the final position. */
    public boolean last() {
        if (cursor >= moves.length) return false;
        cursor = moves.length;
        return true;
    }

    public void jumpTo(int index) {
        cursor = Math.max(0, Math.min(index, moves.length));
    }

    public int cursor() {
        return cursor;
    }

    public int moveCount() {
        return moves.length;
    }

    /** Null until a result is accepted. */
    public DbDtos.GameHeader header() {
        return header;
    }
}
