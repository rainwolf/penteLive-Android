package be.submanifold.pentelive.liveGameRoom;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;

/** The owner list's displayed rows and the diff between two renders (spec C2). Rows match by
 *  player, so a changed snapshot removes, inserts or moves rows instead of rebinding them all, and
 *  a row whose seq or in-flight mark changed is rebound in place. Plain Java (DiffUtil has no
 *  Android framework dependency), so ArenaRequestRowsTest runs it on the JVM. */
final class ArenaRequestRows {
    private ArenaRequestRows() {}

    static final class Row {
        final String player;
        final long seq;
        final boolean inFlight;

        Row(String player, long seq, boolean inFlight) {
            this.player = player;
            this.seq = seq;
            this.inFlight = inFlight;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Row)) return false;
            Row other = (Row) o;
            return seq == other.seq && inFlight == other.inFlight && player.equals(other.player);
        }

        @Override
        public int hashCode() {
            return (31 * player.hashCode() + Long.hashCode(seq)) * 2 + (inFlight ? 1 : 0);
        }
    }

    /** One row per request, in the given (seq) order; only {@code inFlightPlayer}'s row is marked. */
    static List<Row> build(List<ArenaJoinRequest> requests, String inFlightPlayer) {
        List<Row> out = new ArrayList<>();
        for (ArenaJoinRequest r : requests) {
            out.add(new Row(r.player, r.seq, r.player.equals(inFlightPlayer)));
        }
        return out;
    }

    static DiffUtil.Callback diff(final List<Row> old, final List<Row> next) {
        return new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return old.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return old.get(oldPosition).player.equals(next.get(newPosition).player);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return old.get(oldPosition).equals(next.get(newPosition));
            }
        };
    }
}
