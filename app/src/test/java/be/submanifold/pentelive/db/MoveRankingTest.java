package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertEquals(0.0, MoveRanking.wilsonLowerBound(0, 5), 0.0);
        assertEquals(0.0, MoveRanking.wilsonLowerBound(0, 11), 0.0);
        List<NextMove> t = MoveRanking.sorted(Arrays.asList(m(10, 10, 0), m(11, 11, 0), m(12, 12, 0)));
        assertEquals(12, t.get(0).move);
        assertEquals(11, t.get(1).move);
        assertEquals(10, t.get(2).move);
    }

    @Test
    public void topN() {
        List<NextMove> rows = new ArrayList<>();
        for (int i = 0; i < 25; i++) rows.add(m(i, 10 + i, 5));
        List<NextMove> s = MoveRanking.sorted(rows);
        assertEquals(20, MoveRanking.top(s, 20).size());
        assertEquals(25, MoveRanking.top(s, 0).size());
    }

    @Test
    public void gamesSortIsMostPlayedFirstWithWilsonTieBreak() {
        // 1: 4/4 (4 games), 2: 100 games 60 wins, 3: 100 games 80 wins, 4: 10 games 9 wins
        List<NextMove> s = MoveRanking.sorted(
                Arrays.asList(m(1, 4, 4), m(2, 100, 60), m(3, 100, 80), m(4, 10, 9)), MoveSort.GAMES);
        assertEquals(Arrays.asList(3, 2, 4, 1), moves(s));
    }

    @Test
    public void winrateSortIsTheWilsonRanking() {
        List<NextMove> in = Arrays.asList(m(1, 4, 4), m(2, 100, 60), m(3, 100, 80), m(4, 10, 9));
        assertEquals(moves(MoveRanking.sorted(in)), moves(MoveRanking.sorted(in, MoveSort.WINRATE)));
    }

    @Test
    public void winrateSortPutsSmallSampleHighPercentageBelowLargeSample() {
        // 7/10 = 70% has Wilson lower bound ~0.40; 120/200 = 60% has ~0.53
        List<NextMove> s = MoveRanking.sorted(Arrays.asList(m(1, 10, 7), m(2, 200, 120)), MoveSort.WINRATE);
        assertEquals(Arrays.asList(2, 1), moves(s));
    }

    @Test
    public void gamesSortOfEqualCountsKeepsWilsonOrder() {
        List<NextMove> s = MoveRanking.sorted(Arrays.asList(m(1, 20, 5), m(2, 20, 15)), MoveSort.GAMES);
        assertEquals(Arrays.asList(2, 1), moves(s));
    }

    @Test
    public void sortKeysRoundTripAndDefaultToWinrate() {
        assertEquals(MoveSort.WINRATE, MoveSort.fromKey(null));
        assertEquals(MoveSort.WINRATE, MoveSort.fromKey("bogus"));
        assertEquals(MoveSort.GAMES, MoveSort.fromKey(MoveSort.GAMES.key));
        assertEquals(MoveSort.WINRATE, MoveSort.fromKey(MoveSort.WINRATE.key));
    }

    private static List<Integer> moves(List<NextMove> l) {
        List<Integer> out = new ArrayList<>();
        for (NextMove n : l) out.add(n.move);
        return out;
    }

    @Test
    public void lowSampleThresholdIsSharedWithTheRanking() {
        java.util.List<DbDtos.NextMove> in = java.util.Arrays.asList(
                new DbDtos.NextMove(1, MoveRanking.LOW_SAMPLE_GAMES - 1, MoveRanking.LOW_SAMPLE_GAMES - 1, 100),
                new DbDtos.NextMove(2, MoveRanking.LOW_SAMPLE_GAMES, 0, 0));
        assertEquals(2, MoveRanking.sorted(in).get(0).move);
    }

    @Test
    public void infoDialogTextTakesTheThresholdAsAFormatArgument() throws Exception {
        String xml = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("src/main/res/values/strings.xml")), java.nio.charset.StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "<string name=\"db_sort_info_message\">(.*?)</string>", java.util.regex.Pattern.DOTALL).matcher(xml);
        org.junit.Assert.assertTrue(m.find());
        assertTrue(m.group(1).contains("%1$d"));
        assertFalse(m.group(1).contains("fewer than 5"));
    }
}
