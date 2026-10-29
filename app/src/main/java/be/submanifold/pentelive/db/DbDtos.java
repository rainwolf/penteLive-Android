package be.submanifold.pentelive.db;

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
}
