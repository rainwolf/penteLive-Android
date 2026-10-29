package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.Test;

public class BoardReplayTest {
    private static int at(BoardReplay.Result r, int cell) {
        return r.boardXY[cell % 19][cell / 19];
    }

    @Test
    public void connect6ColoursFollowTurns() {
        BoardReplay.Result r = BoardReplay.replay(13, Arrays.asList(180, 0, 1, 2, 3));
        assertEquals(1, at(r, 180));
        assertEquals(2, at(r, 0));
        assertEquals(2, at(r, 1));
        assertEquals(1, at(r, 2));
        assertEquals(1, at(r, 3));
    }

    @Test
    public void connect6HasNoCaptures() {
        // Connect6 colours: moves 180..183 -> 1,2,2,1, a real X-O-O-X line on row 9
        BoardReplay.Result r = BoardReplay.replay(13, Arrays.asList(180, 181, 182, 183));
        assertEquals(0, r.whiteCaptures);
        assertEquals(0, r.blackCaptures);
        assertEquals(1, at(r, 180));
        assertEquals(2, at(r, 181));
        assertEquals(2, at(r, 182));
        assertEquals(1, at(r, 183));
    }

    @Test
    public void xyLayout() {
        BoardReplay.Result r = BoardReplay.replay(1, Arrays.asList(1));
        assertEquals(1, r.boardXY[1][0]);
        assertEquals(0, r.boardXY[0][1]);
    }
}
