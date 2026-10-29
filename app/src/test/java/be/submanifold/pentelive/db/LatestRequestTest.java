package be.submanifold.pentelive.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LatestRequestTest {
    @Test
    public void olderIdNotLatest() {
        LatestRequest g = new LatestRequest();
        int a = g.next();
        int b = g.next();
        assertFalse(g.isLatest(a));
        assertTrue(g.isLatest(b));
    }
}
