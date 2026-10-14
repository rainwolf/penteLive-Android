package be.submanifold.pentelive.billing;

/** A message the billing flow shows the player; BillingManager maps each to a string resource. */
public enum ReplyMessage {
    /** Show nothing. */
    NONE,
    /** "Thank you for subscribing!" */
    THANK_YOU,
    /** "This purchase is linked to another pente.org account." */
    LINKED_TO_OTHER_ACCOUNT,
    /** "Vacation days can't be bought while you play in a turn-based tournament. ..." */
    VACATION_REFUNDED,
    /** "No valid purchase to restore." */
    NO_VALID_PURCHASE,
    /** "No purchases to restore." */
    NO_PURCHASES_TO_RESTORE,
    /** "Restore purchases" reached the server for none of its purchases: the generic billing error. */
    RESTORE_FAILED
}
