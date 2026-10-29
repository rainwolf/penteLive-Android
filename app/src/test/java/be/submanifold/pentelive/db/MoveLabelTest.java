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
}
