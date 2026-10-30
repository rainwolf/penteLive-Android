package be.submanifold.pentelive.db;

import be.submanifold.pente.rules.BoardState;
import be.submanifold.pente.rules.DefaultPenteRules;
import be.submanifold.pente.rules.Variant;
import be.submanifold.pente.rules.Variants;
import java.util.List;

/** Replays a move list through :rules and converts to DBBoardView's [x][y] layout. Pure Java. */
public final class BoardReplay {
    public static final class Result {
        public final byte[][] boardXY;
        public final int whiteCaptures;
        public final int blackCaptures;
        /** Go: the cell the side to move may not retake (ko), else -1. Board-relative (y*n + x). */
        public final int koMove;

        Result(byte[][] boardXY, int whiteCaptures, int blackCaptures, int koMove) {
            this.boardXY = boardXY;
            this.whiteCaptures = whiteCaptures;
            this.blackCaptures = blackCaptures;
            this.koMove = koMove;
        }
    }

    private BoardReplay() {
    }

    public static Result replay(int gameId, List<Integer> moves) {
        Variant v = Variants.fromGameId(gameId);
        if (v == null) {
            throw new IllegalArgumentException("Unknown game id " + gameId);
        }
        BoardState s = new DefaultPenteRules().replay(moves, v, moves.size());
        int n = s.gridSize;
        byte[][] xy = new byte[n][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                xy[x][y] = s.board[y][x];
            }
        }
        return new Result(xy, s.whiteCaptures, s.blackCaptures, v.isGo() ? s.koMove : -1);
    }
}
