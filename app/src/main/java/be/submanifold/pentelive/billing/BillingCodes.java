package be.submanifold.pentelive.billing;

/**
 * What a Play Billing response code means for the UI. The int values mirror
 * {@code BillingClient.BillingResponseCode} (BillingCodesTest pins them against the real
 * library), so this class stays free of Play types.
 */
public final class BillingCodes {

    public static final int SERVICE_DISCONNECTED = -1;
    public static final int OK = 0;
    public static final int USER_CANCELED = 1;
    public static final int BILLING_UNAVAILABLE = 3;
    public static final int ITEM_UNAVAILABLE = 4;
    public static final int ITEM_ALREADY_OWNED = 7;

    /** UI handling of a response code. */
    public enum Outcome {
        /** OK: purchases (if any) go to the server. */
        PROCEED,
        /** The user closed the Play sheet: say nothing. */
        SILENT,
        /** This Google account already owns the item, probably for another pente.org account. */
        ALREADY_OWNED,
        /** No Play Store or Play Billing on this device. */
        BILLING_UNAVAILABLE,
        /** Any other error: show a short message. */
        ERROR
    }

    private BillingCodes() {
    }

    public static Outcome classify(int responseCode) {
        switch (responseCode) {
            case OK:
                return Outcome.PROCEED;
            case USER_CANCELED:
                return Outcome.SILENT;
            case ITEM_ALREADY_OWNED:
                return Outcome.ALREADY_OWNED;
            case BILLING_UNAVAILABLE:
                return Outcome.BILLING_UNAVAILABLE;
            default:
                return Outcome.ERROR;
        }
    }
}
