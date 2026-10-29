package be.submanifold.pentelive.db;

import be.submanifold.pente.rules.Variant;
import be.submanifold.pente.rules.Variants;
import java.util.ArrayList;
import java.util.List;

/** Viewer move-grid cell model: number, label and past/current/future state per move. */
public final class MoveGrid {
    public enum State { PAST, CURRENT, FUTURE }

    public static final class Cell {
        public final int index;
        public final String number;
        public final String label;
        public final State state;

        Cell(int index, String number, String label, State state) {
            this.index = index;
            this.number = number;
            this.label = label;
            this.state = state;
        }
    }

    private MoveGrid() {
    }

    public static List<Cell> cells(int gameId, List<Integer> moves, int cursor) {
        int gridSize = DbGames.gridSize(gameId);
        Variant v = Variants.fromGameId(gameId > 50 ? gameId - 50 : gameId);
        boolean connect6 = v != null && v.isConnect6();
        List<Cell> out = new ArrayList<>(moves.size());
        for (int i = 0; i < moves.size(); i++) {
            String number = "";
            if (connect6) {
                if (i == 0) {
                    number = "1.";
                } else if (i >= 3 && (i & 3) == 3) {
                    number = ((i >> 2) + 2) + ".";
                }
            } else if (i % 2 == 0) {
                number = (i / 2 + 1) + ".";
            }
            State state = i == cursor - 1 ? State.CURRENT : i < cursor - 1 ? State.PAST : State.FUTURE;
            out.add(new Cell(i, number, MoveLabel.label(moves.get(i), gridSize), state));
        }
        return out;
    }

    public static int argb(State s) {
        switch (s) {
            case CURRENT:
                return 0xFF1F5FD6;
            case FUTURE:
                return 0xFF8A8F99;
            default:
                return 0xFF16181D;
        }
    }
}
