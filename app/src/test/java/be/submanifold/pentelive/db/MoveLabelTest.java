package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MoveLabelTest {
    @Test
    public void labels() {
        assertEquals("K10", MoveLabel.label(180, 19));
        assertEquals("A19", MoveLabel.label(0, 19));
        assertEquals("T1", MoveLabel.label(360, 19));
    }

    @Test
    public void smallBoards() {
        assertEquals("A9", MoveLabel.label(0, 9));
        assertEquals("E5", MoveLabel.label(40, 9));
        assertEquals("J1", MoveLabel.label(80, 9));
        assertEquals("G7", MoveLabel.label(84, 13));
        assertEquals("H8", MoveLabel.label(112, 15));
        assertEquals("P1", MoveLabel.label(224, 15));
    }

    @Test
    public void passSentinelReadsPass() {
        assertEquals("PASS", MoveLabel.label(81, 9));
        assertEquals("PASS", MoveLabel.label(169, 13));
        assertEquals("PASS", MoveLabel.label(361, 19));
        assertEquals("PASS", MoveLabel.label(225, 15));
    }

    @Test
    public void junkReadsQuestionMark() {
        assertEquals("?", MoveLabel.label(-1, 9));
        assertEquals("?", MoveLabel.label(82, 9));
        assertEquals("?", MoveLabel.label(400, 19));
    }
}
