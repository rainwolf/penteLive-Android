package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import be.submanifold.pentelive.BackNavigation.Action;

public class BackNavigationTest {
    private static Action plain(int playedMove) {
        return BackNavigation.decide(false, -1, false, false, playedMove, -1, -1, -1);
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
                BackNavigation.decide(true, 10, false, false, 10, -1, -1, -1));
        assertEquals(Action.CLEAR_CONNECT6,
                BackNavigation.decide(true, 10, false, false, 20, -1, -1, -1));
    }

    @Test
    public void connect6WithoutPendingBehavesLikePlain() {
        assertEquals(Action.STEP_BACK, BackNavigation.decide(true, -1, false, false, -1, -1, -1, -1));
        assertEquals(Action.CLEAR_PLACED_STONE, BackNavigation.decide(true, -1, false, false, 5, -1, -1, -1));
    }

    @Test
    public void dPenteOpeningNeverStepsBack() {
        assertEquals(Action.CLEAR_DPENTE_PICK, BackNavigation.decide(false, -1, true, true, 7, -1, -1, -1));
        assertEquals(Action.CLEAR_DPENTE_PICK, BackNavigation.decide(false, -1, true, true, -1, -1, -1, -1));
    }

    @Test
    public void dPenteAfterOpeningIsPlain() {
        assertEquals(Action.CLEAR_PLACED_STONE, BackNavigation.decide(false, -1, true, false, 7, -1, -1, -1));
        assertEquals(Action.STEP_BACK, BackNavigation.decide(false, -1, true, false, -1, -1, -1, -1));
    }

    private static Action pick(int swap2, int sel, int offers, int playedMove) {
        return BackNavigation.decide(false, -1, false, false, playedMove, swap2, sel, offers);
    }

    @Test
    public void swap2PicksAreClearedOneAtATimeThenLegacy() {
        assertEquals(Action.CLEAR_SWAP2_PICK, pick(3, -1, -1, 7));
        assertEquals(Action.CLEAR_SWAP2_PICK, pick(1, -1, -1, -1));
        assertEquals(Action.STEP_BACK, pick(0, -1, -1, 7));
    }

    @Test
    public void renjuSelectionPicksConsumedBeforeStepBack() {
        assertEquals(Action.CLEAR_RENJU_SELECTION_PICK, pick(-1, 2, -1, 7));
        assertEquals(Action.CLEAR_RENJU_SELECTION_PICK, pick(-1, 1, -1, -1));
        assertEquals(Action.STEP_BACK, pick(-1, 0, -1, 7));
    }

    @Test
    public void renjuOfferPicksConsumedBeforeStepBack() {
        assertEquals(Action.CLEAR_RENJU_OFFER_PICK, pick(-1, -1, 10, 7));
        assertEquals(Action.CLEAR_RENJU_OFFER_PICK, pick(-1, -1, 1, -1));
        assertEquals(Action.STEP_BACK, pick(-1, -1, 0, 7));
    }

    @Test
    public void renjuSelectionBeatsOffers() {
        assertEquals(Action.CLEAR_RENJU_SELECTION_PICK, pick(-1, 1, 3, 7));
    }

    @Test
    public void connect6AndDPenteStillOutrankPicks() {
        assertEquals(Action.CLEAR_CONNECT6, BackNavigation.decide(true, 10, false, false, 7, 2, 2, 2));
        assertEquals(Action.CLEAR_DPENTE_PICK, BackNavigation.decide(false, -1, true, true, 7, 2, 2, 2));
    }
}
