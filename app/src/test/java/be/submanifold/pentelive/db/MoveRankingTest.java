package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import be.submanifold.pentelive.db.DbDtos.NextMove;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MoveRankingTest {
    private static NextMove m(int move, int games, int wins) {
        return new NextMove(move, games, wins, games == 0 ? 0 : 100.0 * wins / games);
    }

    @Test
    public void wilsonValue() {
        assertEquals(0.5020, MoveRanking.wilsonLowerBound(60, 100), 1e-4);
        assertEquals(0.0, MoveRanking.wilsonLowerBound(0, 0), 0.0);
    }

    @Test
    public void lowSampleLast() {
        List<NextMove> s = MoveRanking.sorted(Arrays.asList(m(1, 4, 4), m(2, 100, 60)));
        assertEquals(2, s.get(0).move);
        assertEquals(1, s.get(1).move);
    }

    @Test
    public void wilsonDescWithinGroup() {
        List<NextMove> s = MoveRanking.sorted(Arrays.asList(m(1, 10, 6), m(2, 100, 60)));
        assertEquals(2, s.get(0).move);
    }

    @Test
    public void tieBrokenByGames() {
        List<NextMove> s = MoveRanking.sorted(Arrays.asList(m(1, 5, 0), m(2, 8, 0)));
        assertEquals(2, s.get(0).move);
    }

    @Test
    public void topN() {
        List<NextMove> rows = new ArrayList<>();
        for (int i = 0; i < 25; i++) rows.add(m(i, 10 + i, 5));
        List<NextMove> s = MoveRanking.sorted(rows);
        assertEquals(20, MoveRanking.top(s, 20).size());
        assertEquals(25, MoveRanking.top(s, 0).size());
    }
}
