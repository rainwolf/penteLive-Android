package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import be.submanifold.pentelive.BackNavigation.Action;

public class BackNavigationTest {
    private static Action plain(int playedMove) {
        return BackNavigation.decide(false, -1, false, false, playedMove, false, false);
    }

    @Test
    public void noStoneStepsBack() {
        assertEquals(Action.STEP_BACK, plain(-1));
    }

    @Test
    public void placedStoneIsClearedWithoutStepping() {
        assertEquals(Action.CLEAR_PLACED_STONE, plain(42));
    }

    @Test
    public void placedStoneThenBackTwiceClearsThenSteps() {
        int playedMove = 42;
        Action first = plain(playedMove);
        assertEquals(Action.CLEAR_PLACED_STONE, first);
        playedMove = -1; // what the activity does for CLEAR_PLACED_STONE
        assertEquals(Action.STEP_BACK, plain(playedMove));
    }

    @Test
    public void connect6FirstOrSecondStoneClearsPendingPair() {
        assertEquals(Action.CLEAR_CONNECT6,
                BackNavigation.decide(true, 10, false, false, 10, false, false));
        assertEquals(Action.CLEAR_CONNECT6,
                BackNavigation.decide(true, 10, false, false, 20, false, false));
    }

    @Test
    public void connect6WithoutPendingBehavesLikePlain() {
        assertEquals(Action.STEP_BACK, BackNavigation.decide(true, -1, false, false, -1, false, false));
        assertEquals(Action.CLEAR_PLACED_STONE, BackNavigation.decide(true, -1, false, false, 5, false, false));
    }

    @Test
    public void dPenteOpeningNeverStepsBack() {
        assertEquals(Action.CLEAR_DPENTE_PICK, BackNavigation.decide(false, -1, true, true, 7, false, false));
        assertEquals(Action.CLEAR_DPENTE_PICK, BackNavigation.decide(false, -1, true, true, -1, false, false));
    }

    @Test
    public void dPenteAfterOpeningIsPlain() {
        assertEquals(Action.CLEAR_PLACED_STONE, BackNavigation.decide(false, -1, true, false, 7, false, false));
        assertEquals(Action.STEP_BACK, BackNavigation.decide(false, -1, true, false, -1, false, false));
    }

    @Test
    public void swap2PendingKeepsLegacyBehaviour() {
        assertEquals(Action.STEP_BACK, BackNavigation.decide(false, -1, false, false, 7, true, false));
    }

    @Test
    public void renjuOfferAndSelectionKeepLegacyBehaviour() {
        assertEquals(Action.STEP_BACK, BackNavigation.decide(false, -1, false, false, 7, false, true));
    }
}
