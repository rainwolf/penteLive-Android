package be.submanifold.pentelive.db;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DbBoardGeometryTest {
    private static final float EPS = 1e-3f;

    @Test
    public void cellAtCorners() {
        assertEquals(0, DbBoardGeometry.cellAt(0, 0, 900, 9));
        assertEquals(80, DbBoardGeometry.cellAt(899, 899, 900, 9));
        assertEquals(8, DbBoardGeometry.cellAt(899, 0, 900, 9));
        assertEquals(72, DbBoardGeometry.cellAt(0, 899, 900, 9));
        assertEquals(168, DbBoardGeometry.cellAt(899, 899, 900, 13));
        assertEquals(224, DbBoardGeometry.cellAt(899, 899, 900, 15));
        assertEquals(360, DbBoardGeometry.cellAt(899, 899, 900, 19));
    }

    @Test
    public void cellAtCentreIsRowMajor() {
        assertEquals(180, DbBoardGeometry.cellAt(450, 450, 900, 19));
        assertEquals(40, DbBoardGeometry.cellAt(450, 450, 900, 9));
        assertEquals(112, DbBoardGeometry.cellAt(450, 450, 900, 15));
        // column 1 (x), row 2 (y) on 9x9: cell 2*9 + 1
        assertEquals(19, DbBoardGeometry.cellAt(150, 250, 900, 9));
    }

    @Test
    public void cellAtRightEdgeIsClamped() {
        assertEquals(8, DbBoardGeometry.cellAt(900, 0, 900, 9));
        assertEquals(360, DbBoardGeometry.cellAt(900, 900, 900, 19));
    }

    @Test
    public void cellAtOutsideIsMinusOne() {
        assertEquals(-1, DbBoardGeometry.cellAt(-1, 10, 900, 9));
        assertEquals(-1, DbBoardGeometry.cellAt(10, -1, 900, 9));
        assertEquals(-1, DbBoardGeometry.cellAt(901, 10, 900, 9));
        assertEquals(-1, DbBoardGeometry.cellAt(10, 901, 900, 9));
        assertEquals(-1, DbBoardGeometry.cellAt(10, 10, 0, 9));
    }

    @Test
    public void centreOfIndex() {
        assertEquals(450f, DbBoardGeometry.centre(4, 900, 9), EPS);
        assertEquals(50f, DbBoardGeometry.centre(0, 900, 9), EPS);
        // 19x19 matches the old size*i/19 + size/38
        assertEquals(390f * 9 / 19 + 390f / 38, DbBoardGeometry.centre(9, 390, 19), EPS);
    }

    @Test
    public void stoneRadiusScalesWithGrid() {
        assertEquals(10f, DbBoardGeometry.stoneRadius(390, 19), EPS); // the old size/39
        assertEquals(900f / 19, DbBoardGeometry.stoneRadius(900, 9), EPS);
        assertEquals(390f / 30, DbBoardGeometry.zoomedStoneRadius(390, 19), EPS); // the old size/30
        assertEquals(900f * 19 / 30 / 9, DbBoardGeometry.zoomedStoneRadius(900, 9), EPS);
    }

    @Test
    public void starPointsPerGame() {
        assertArrayEquals(new int[] {120, 126, 180, 234, 240}, DbBoardGeometry.starPoints(1));
        assertArrayEquals(new int[] {120, 126, 180, 234, 240}, DbBoardGeometry.starPoints(13));
        assertArrayEquals(new int[] {60, 66, 72, 174, 180, 186, 288, 294, 300}, DbBoardGeometry.starPoints(19));
        assertArrayEquals(new int[] {20, 24, 40, 56, 60}, DbBoardGeometry.starPoints(22));
        assertArrayEquals(new int[] {42, 45, 48, 81, 84, 87, 120, 123, 126}, DbBoardGeometry.starPoints(23));
        assertArrayEquals(new int[] {48, 52, 56, 108, 112, 116, 168, 172, 176}, DbBoardGeometry.starPoints(31));
        assertArrayEquals(new int[] {48, 52, 56, 108, 112, 116, 168, 172, 176}, DbBoardGeometry.starPoints(81));
    }

    @Test
    public void filledStarPointsOnlyForGoAndRenju() {
        assertEquals(false, DbBoardGeometry.filledStarPoints(1));
        assertEquals(true, DbBoardGeometry.filledStarPoints(21));
        assertEquals(true, DbBoardGeometry.filledStarPoints(31));
    }
}
