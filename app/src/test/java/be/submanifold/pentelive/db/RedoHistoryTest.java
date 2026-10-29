package be.submanifold.pentelive.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class RedoHistoryTest {
    @Test
    public void spec39Vector() {
        RedoHistory h = new RedoHistory(new ArrayList<Integer>(), 0, false);
        h.play(10);
        h.play(20);
        assertTrue(h.back());
        assertTrue(h.back());
        assertTrue(h.forward());
        assertEquals(Arrays.asList(10), h.moves());
        assertEquals(Arrays.asList(20), h.redo());
        h.play(30);
        assertEquals(Arrays.asList(10, 30), h.moves());
        assertTrue(h.redo().isEmpty());
    }

    @Test
    public void backAtFloorIsNoop() {
        RedoHistory h = new RedoHistory(Collections.singletonList(180), 1, false);
        assertFalse(h.back());
        assertEquals(Arrays.asList(180), h.moves());
    }

    @Test
    public void forwardAtEndIsNoop() {
        RedoHistory h = new RedoHistory(new ArrayList<Integer>(), 0, false);
        assertFalse(h.forward());
    }

    @Test
    public void turnStepsConnect6() {
        RedoHistory h = new RedoHistory(Arrays.asList(180, 1, 2, 3, 4), 1, true);
        assertTrue(h.back());
        assertEquals(Arrays.asList(180, 1, 2), h.moves());
        assertTrue(h.back());
        assertEquals(Arrays.asList(180), h.moves());
        assertFalse(h.back());
        assertTrue(h.forward());
        assertEquals(Arrays.asList(180, 1, 2), h.moves());
        assertEquals(Arrays.asList(3, 4), h.redo());
    }

    @Test
    public void immutableInitialAndOutputs() {
        RedoHistory h = new RedoHistory(Collections.singletonList(180), 1, false);
        h.play(5);
        assertEquals(Arrays.asList(180, 5), h.moves());
        try {
            h.moves().add(1);
            throw new AssertionError("mutable");
        } catch (UnsupportedOperationException expected) {
        }
        try {
            h.redo().add(1);
            throw new AssertionError("mutable");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void resetClearsRedo() {
        RedoHistory h = new RedoHistory(new ArrayList<Integer>(), 0, false);
        h.play(1);
        h.back();
        h.reset(Arrays.asList(7, 8));
        assertEquals(Arrays.asList(7, 8), h.moves());
        assertTrue(h.redo().isEmpty());
    }
}
