package be.submanifold.pentelive.liveGameRoom;

/** One pending arena join request in a dsgArenaJoinRequestsEvent snapshot. {@code seq} is
 *  server-assigned at creation, increasing and never reused; rows render in seq order. */
public final class ArenaJoinRequest {
    public final String player;
    public final long seq;

    public ArenaJoinRequest(String player, long seq) {
        this.player = player;
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArenaJoinRequest)) return false;
        ArenaJoinRequest other = (ArenaJoinRequest) o;
        return seq == other.seq && player.equals(other.player);
    }

    @Override
    public int hashCode() {
        return 31 * player.hashCode() + Long.hashCode(seq);
    }

    @Override
    public String toString() {
        return player + "#" + seq;
    }
}
