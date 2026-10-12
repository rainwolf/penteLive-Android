package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StoreCardsTest {

    private static final long NOW = 1_760_000_000_000L;

    @Test
    public void notEntitled() {
        StoreCards cards = StoreCards.of(new StoreInfo("1.a", true, null, false), NOW);
        assertFalse(cards.showSubscribedUntil);
        assertFalse(cards.showYearlyOverlapWarning);
        assertTrue(cards.showMonthPass);
        assertFalse(cards.showMonthPassStacksNote);
        assertTrue(cards.showVacation);
        assertFalse(cards.yearlyOwned);
    }

    @Test
    public void entitledThroughAnotherChannelWarnsAndStacks() {
        StoreCards cards = StoreCards.of(new StoreInfo("1.a", true, NOW + 1, false), NOW);
        assertTrue(cards.showSubscribedUntil);
        assertTrue(cards.showYearlyOverlapWarning);
        assertTrue(cards.showMonthPass);
        assertTrue(cards.showMonthPassStacksNote);
        assertFalse(cards.yearlyOwned);
    }

    @Test
    public void autoRenewingHidesMonthPassAndWarning() {
        StoreCards cards = StoreCards.of(new StoreInfo("1.a", true, NOW + 1, true), NOW);
        assertTrue(cards.showSubscribedUntil);
        assertFalse(cards.showYearlyOverlapWarning);
        assertFalse(cards.showMonthPass);
        assertFalse(cards.showMonthPassStacksNote);
    }

    @Test
    public void autoRenewingYearlyCannotBeBoughtAgain() {
        // Play would answer ITEM_ALREADY_OWNED: the yearly button is disabled with a "Manage subscription" hint.
        StoreCards cards = StoreCards.of(new StoreInfo("1.a", true, NOW + 1, true), NOW);
        assertTrue(cards.yearlyOwned);
        assertFalse(cards.showYearlyOverlapWarning);
        // The server's autoRenewing flag decides, even without a future expiry.
        assertTrue(StoreCards.of(new StoreInfo("1.a", true, null, true), NOW).yearlyOwned);
        assertFalse(StoreCards.of(new StoreInfo("1.a", true, null, true), NOW).showYearlyOverlapWarning);
    }

    @Test
    public void pastOrNowExpiryIsNotEntitled() {
        StoreCards cards = StoreCards.of(new StoreInfo("1.a", true, NOW, false), NOW);
        assertFalse(cards.showSubscribedUntil);
        assertFalse(cards.showYearlyOverlapWarning);
        assertFalse(cards.showMonthPassStacksNote);
    }

    @Test
    public void vacationOnlyWhenAllowed() {
        assertFalse(StoreCards.of(new StoreInfo("1.a", false, null, false), NOW).showVacation);
    }

    @Test
    public void buyNeedsEverythingLoaded() {
        assertTrue(StoreCards.buyEnabled(true, true, true, true));
        assertFalse(StoreCards.buyEnabled(false, true, true, true));
        assertFalse(StoreCards.buyEnabled(true, false, true, true));
        assertFalse(StoreCards.buyEnabled(true, true, false, true));
        assertFalse(StoreCards.buyEnabled(true, true, true, false));
    }
}
