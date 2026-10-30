package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import be.submanifold.pentelive.db.DbDtos.NextMove;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class HeatScaleTest {
    private static NextMove m(int move, double pct) {
        return new NextMove(move, 10, 5, pct);
    }

    @Test
    public void threePoint() {
        Map<Integer, Integer> c = HeatScale.colours(
                Arrays.asList(m(1, 40), m(2, 50), m(3, 60)), Collections.<Integer>emptySet());
        assertEquals(0xFFFF0000, (int) c.get(1));
        assertEquals(0xFFFFFF00, (int) c.get(2));
        assertEquals(0xFF00FF00, (int) c.get(3));
    }

    @Test
    public void single55() {
        Map<Integer, Integer> c = HeatScale.colours(
                Arrays.asList(m(1, 55)), Collections.<Integer>emptySet());
        assertEquals(0xFFE6FF00, (int) c.get(1));
    }

    @Test
    public void occupiedExcluded() {
        Map<Integer, Integer> c = HeatScale.colours(
                Arrays.asList(m(7, 0), m(1, 40), m(2, 60)), new HashSet<>(Arrays.asList(7)));
        assertFalse(c.containsKey(7));
        assertEquals(0xFFFF0000, (int) c.get(1));
        assertEquals(0xFF00FF00, (int) c.get(2));
    }

    @Test
    public void emptyAndAllOccupiedGiveNoColours() {
        List<NextMove> none = Collections.emptyList();
        assertTrue(HeatScale.colours(none, Collections.<Integer>emptySet()).isEmpty());
        assertTrue(HeatScale.colours(Arrays.asList(m(1, 50)), new HashSet<>(Arrays.asList(1))).isEmpty());
    }
}
