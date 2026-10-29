package be.submanifold.pentelive.db;

import java.util.ArrayList;
import java.util.List;

/** Gson POJOs for /api/db responses. */
public final class DbDtos {
    private DbDtos() {}

    public static class NextMove {
        public int move;
        public int games;
        public int wins;
        public double winPct;

        public NextMove() {}

        public NextMove(int move, int games, int wins, double winPct) {
            this.move = move;
            this.games = games;
            this.wins = wins;
            this.winPct = winPct;
        }
    }

    public static class PositionStats {
        public int totalGames;
        public double totalWinPct;
        public List<NextMove> nextMoves = new ArrayList<>();
        public boolean blocked;
    }

    public static class Player {
        public String name;
        public int rating;
        public int nameColor;
        public int crown;
    }

    public static class GameHeader {
        public long gid;
        public String source;
        public int game;
        public Player player1;
        public Player player2;
        public int winner;
        public String site;
        public String event;
        public String round;
        public String section;
        public String playDate;
        public int moveCount;

        public boolean isMine() {
            return "mine".equals(source);
        }
    }

    public static class GamePage {
        public int total;
        public List<GameHeader> games = new ArrayList<>();
        public boolean blocked;
    }

    public static class GameDetail {
        public GameHeader header;
        public int[] moves = new int[0];
        public boolean blocked;
    }

    /** {@code GET /ping}; a missing {@code features} field means none. */
    public static class Ping {
        public boolean ok;
        public String auth;
        public boolean dbAccess;
        public List<String> features = new ArrayList<>();
    }

    public static class PlayersReply {
        public List<String> players = new ArrayList<>();
    }
}
