package be.submanifold.pentelive.billing;

/** Which parts of the store screen show, from the server's {@link StoreInfo} (spec 3.4). */
public final class StoreCards {

    /** Header "Subscribed until <date>": the entitlement ends in the future. */
    public final boolean showSubscribedUntil;
    /** Yearly card warning: entitled through another channel, so a new period overlaps it. */
    public final boolean showYearlyOverlapWarning;
    /**
     * The player already has a Play yearly subscription that auto-renews: its buy button is
     * disabled (Play would answer ITEM_ALREADY_OWNED) and a "Manage subscription" hint shows.
     */
    public final boolean yearlyOwned;
    /** Month pass card: hidden while a Play yearly subscription auto-renews. */
    public final boolean showMonthPass;
    /** Month pass note: the pass starts after the current entitlement ends. */
    public final boolean showMonthPassStacksNote;
    /** Vacation cards: only when the server allows buying vacation days now. */
    public final boolean showVacation;

    private StoreCards(boolean showSubscribedUntil, boolean showYearlyOverlapWarning, boolean yearlyOwned,
                       boolean showMonthPass, boolean showMonthPassStacksNote, boolean showVacation) {
        this.showSubscribedUntil = showSubscribedUntil;
        this.showYearlyOverlapWarning = showYearlyOverlapWarning;
        this.yearlyOwned = yearlyOwned;
        this.showMonthPass = showMonthPass;
        this.showMonthPassStacksNote = showMonthPassStacksNote;
        this.showVacation = showVacation;
    }

    public static StoreCards of(StoreInfo info, long nowMillis) {
        boolean entitled = info.subscribedUntil != null && info.subscribedUntil > nowMillis;
        boolean showMonthPass = !info.autoRenewing;
        return new StoreCards(
                entitled,
                entitled && !info.autoRenewing,
                info.autoRenewing,
                showMonthPass,
                showMonthPass && entitled,
                info.vacationAllowed);
    }

    /**
     * Whether a buy button is enabled: the server info and the Play products have both
     * loaded, Play Billing is available, and Play returned this product.
     */
    public static boolean buyEnabled(boolean infoLoaded, boolean productsLoaded, boolean billingAvailable,
                                     boolean productFound) {
        return infoLoaded && productsLoaded && billingAvailable && productFound;
    }
}
