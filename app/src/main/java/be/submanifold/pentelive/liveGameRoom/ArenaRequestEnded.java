package be.submanifold.pentelive.liveGameRoom;

/** A dsgArenaRequestEndedEvent: {@code player} is the requester, {@code owner} the table owner,
 *  {@code reason} one of the spec's reason codes (or something newer this build does not know). */
public final class ArenaRequestEnded {
    public final int table;
    public final String player;
    public final String owner;
    public final String reason;

    public ArenaRequestEnded(int table, String player, String owner, String reason) {
        this.table = table;
        this.player = player;
        this.owner = owner;
        this.reason = reason;
    }
}
