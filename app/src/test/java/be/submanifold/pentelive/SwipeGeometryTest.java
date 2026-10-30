package be.submanifold.pentelive;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SwipeGeometryTest {
    @Test
    public void bandIsClampedTo200DpAndCentred() {
        // 200 dp * 2.75 = 550 px; (1080 - 550) / 2 = 265
        assertArrayEquals(new int[] {0, 265, 1080, 815}, SwipeGeometry.exclusionBand(1080, 1080, 2.75f));
    }

    @Test
    public void smallBoardIsExcludedWhole() {
        assertArrayEquals(new int[] {0, 0, 300, 300}, SwipeGeometry.exclusionBand(300, 300, 2f));
    }

    @Test
    public void zeroSizeGivesEmptyBand() {
        assertArrayEquals(new int[] {0, 0, 0, 0}, SwipeGeometry.exclusionBand(0, 0, 3f));
    }
}
