package be.submanifold.pentelive.liveGameRoom;

/** Builds the raw-JSON-string arena protocol events sent via LiveGameRoomActivity.sendEvent.
 *  NOTE: diverges from iOS PR #4 on purpose — requestJoin adds "player"; reject uses
 *  "message" (iOS had "mesage"). The "DSG" capitalization on reject matches the backend. */
public final class ArenaEvents {
    private ArenaEvents() {}

    public static String createTable(boolean timed, int initialMinutes, int incrementalSeconds,
                                     boolean rated, int game, int playAs, String player) {
        return "{\"dsgArenaCreateTableEvent\":{\"timed\":" + timed
                + ",\"initialMinutes\":" + initialMinutes
                + ",\"incrementalSeconds\":" + incrementalSeconds
                + ",\"rated\":" + rated
                + ",\"game\":" + game
                + ",\"playAs\":" + playAs
                + ",\"player\":\"" + player + "\""
                + ",\"table\":-1,\"time\":0}}";
    }

    public static String requestJoin(String player, int tableId) {
        return "{\"dsgArenaRequestJoinTableEvent\":{\"player\":\"" + player
                + "\",\"table\":" + tableId + ",\"time\":0}}";
    }

    public static String accept(String player, String playerToAccept, int tableId) {
        return "{\"dsgArenaAcceptTableJoinEvent\":{\"player\":\"" + player
                + "\",\"playerToAccept\":\"" + playerToAccept
                + "\",\"table\":" + tableId + "}}";
    }

    public static String reject(String player, String playerToReject, int tableId) {
        return "{\"DSGArenaRejectTableJoinEvent\":{\"player\":\"" + player
                + "\",\"playerToReject\":\"" + playerToReject
                + "\",\"table\":" + tableId + ",\"message\":null}}";
    }
}
