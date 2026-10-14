package be.submanifold.pentelive.billing;

/** The parts of a Play {@code Purchase} the client acts on, without Play types. */
public final class PurchaseRecord {

    public final String productId;
    public final String purchaseToken;
    /** Purchase state PURCHASED; false for PENDING or unspecified. */
    public final boolean purchased;
    public final boolean acknowledged;

    public PurchaseRecord(String productId, String purchaseToken, boolean purchased, boolean acknowledged) {
        this.productId = productId;
        this.purchaseToken = purchaseToken;
        this.purchased = purchased;
        this.acknowledged = acknowledged;
    }
}
