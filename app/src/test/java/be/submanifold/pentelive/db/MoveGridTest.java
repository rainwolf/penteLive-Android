package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MoveGridTest {
    private static final int PENTE = DbGames.baseId("Pente");
    private static final int CONNECT6 = DbGames.baseId("Connect6");

    private static List<Integer> seq(int n) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = 180 + i;
        }
        return Arrays.asList(a);
    }

    @Test
    public void currentIsCursorMinusOne() {
        List<MoveGrid.Cell> cells = MoveGrid.cells(PENTE, Arrays.asList(180, 181, 182), 2);
        assertEquals(MoveGrid.State.PAST, cells.get(0).state);
        assertEquals(MoveGrid.State.CURRENT, cells.get(1).state);
        assertEquals(MoveGrid.State.FUTURE, cells.get(2).state);
    }

    @Test
    public void labelsUseMoveLabel() {
        assertEquals("K10", MoveGrid.cells(PENTE, Arrays.asList(180), 1).get(0).label);
    }

    @Test
    public void colours() {
        assertEquals(0xFF1F5FD6, MoveGrid.argb(MoveGrid.State.CURRENT));
        assertEquals(0xFF8A8F99, MoveGrid.argb(MoveGrid.State.FUTURE));
        assertEquals(0xFF16181D, MoveGrid.argb(MoveGrid.State.PAST));
    }

    @Test
    public void pairNumbering() {
        List<MoveGrid.Cell> c = MoveGrid.cells(PENTE, seq(5), 5);
        String[] want = {"1.", "", "2.", "", "3."};
        for (int i = 0; i < 5; i++) {
            assertEquals(want[i], c.get(i).number);
        }
    }

    @Test
    public void connect6Numbering() {
        List<MoveGrid.Cell> c = MoveGrid.cells(CONNECT6, seq(8), 8);
        String[] want = {"1.", "", "", "2.", "", "", "", "3."};
        for (int i = 0; i < 8; i++) {
            assertEquals(want[i], c.get(i).number);
        }
    }

    @Test
    public void goNineByNineLabelsAndPasses() {
        List<MoveGrid.Cell> c = MoveGrid.cells(21, Arrays.asList(40, 81, 0), 3);
        assertEquals("E5", c.get(0).label);
        assertEquals("PASS", c.get(1).label);
        assertEquals("A9", c.get(2).label);
        assertEquals("1.", c.get(0).number);
        assertEquals("", c.get(1).number);
        assertEquals("2.", c.get(2).number);
    }

    @Test
    public void renjuLabelsOnFifteen() {
        assertEquals("H8", MoveGrid.cells(31, Arrays.asList(112), 1).get(0).label);
    }
}
