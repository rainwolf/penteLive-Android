package be.submanifold.pentelive.liveGameRoom;

import androidx.annotation.StringRes;

import be.submanifold.pentelive.R;

/** The short notice shown for a dsgArenaRequestEndedEvent (spec C4). A reason this build does
 *  not know gives no notice, so a newer server cannot crash or confuse an older client. */
public final class ArenaRequestNotice {

    public enum Kind {
        DECLINED(R.string.arena_notice_declined),
        TABLE_FULL(R.string.arena_notice_table_full),
        TABLE_CLOSED(R.string.arena_notice_table_closed),
        DUPLICATE(R.string.arena_notice_duplicate),
        BOOTED(R.string.arena_notice_booted),
        BLOCKED(R.string.arena_notice_blocked),
        NOT_AVAILABLE(R.string.arena_notice_not_available),
        GUEST_RATED(R.string.arena_notice_guest_rated),
        NO_LONGER_AVAILABLE(R.string.arena_notice_no_longer_available);

        @StringRes
        public final int resId;

        Kind(@StringRes int resId) {
            this.resId = resId;
        }
    }

    public final Kind kind;
    /** Format arguments for {@code getString(kind.resId, args)}. */
    public final Object[] args;

    private ArenaRequestNotice(Kind kind, Object... args) {
        this.kind = kind;
        this.args = args;
    }

    public static ArenaRequestNotice of(ArenaRequestEnded ended) {
        if (ended == null || ended.reason == null) return null;
        switch (ended.reason) {
            case "DECLINED":
                return new ArenaRequestNotice(Kind.DECLINED, ended.owner);
            case "TABLE_FULL":
                return new ArenaRequestNotice(Kind.TABLE_FULL, ended.table);
            case "TABLE_CLOSED":
                return new ArenaRequestNotice(Kind.TABLE_CLOSED, ended.table);
            case "DUPLICATE":
                // owner comes from the event, never from the lobby's table model
                return new ArenaRequestNotice(Kind.DUPLICATE, ended.owner);
            case "BOOTED":
                return new ArenaRequestNotice(Kind.BOOTED, ended.owner, ended.table);
            case "BLOCKED":
                return new ArenaRequestNotice(Kind.BLOCKED, ended.table);
            case "NOT_AVAILABLE":
                return new ArenaRequestNotice(Kind.NOT_AVAILABLE, ended.table);
            case "GUEST_RATED":
                return new ArenaRequestNotice(Kind.GUEST_RATED);
            case "NO_LONGER_AVAILABLE":
                return new ArenaRequestNotice(Kind.NO_LONGER_AVAILABLE, ended.player);
            default:
                return null;
        }
    }
}
