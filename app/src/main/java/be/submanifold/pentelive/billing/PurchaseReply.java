package be.submanifold.pentelive.billing;

/**
 * What pente.org's /gameServer/googlePlayPurchase reply means. Mirrors the iOS ReceiptReply
 * (penteLive-iOS test1/ReceiptReply.swift) and adds "success:refunded" for a vacation
 * purchase the server refunded.
 */
public enum PurchaseReply {
    /** "success": a new purchase; the server thanked the player. */
    NEW_PURCHASE,
    /** "success:renewal": a renewal or extension was recorded. */
    RENEWAL,
    /** "success:known", or any other "success:" variant: already recorded for this player. */
    KNOWN,
    /** "success:shared": the purchase belongs to another pente.org account. */
    SHARED,
    /** "success:refunded": vacation days bought during a turn-based tourney; Play refunded them. */
    REFUNDED,
    /** "invalid receipt": no valid purchase behind the token. */
    INVALID,
    /** No reply, a network error, or an unrecognised reply: a later resync retries. */
    FAILED;

    /** Classifies the server's reply body; {@code null} means no reply. */
    public static PurchaseReply fromServer(String reply) {
        if (reply == null) {
            return FAILED;
        }
        if (reply.contains("success")) {
            if (reply.contains("success:renewal")) {
                return RENEWAL;
            }
            if (reply.contains("success:shared")) {
                return SHARED;
            }
            if (reply.contains("success:refunded")) {
                return REFUNDED;
            }
            // "success:known", and any variant a newer server adds, is already recorded:
            // never thank the player again.
            if (reply.contains("success:")) {
                return KNOWN;
            }
            return NEW_PURCHASE;
        }
        if (reply.contains("invalid receipt")) {
            return INVALID;
        }
        return FAILED;
    }

    /**
     * The message to show for this reply (spec 3.3). {@code explicit} is true for
     * "Restore purchases", false for purchase updates and the automatic resync.
     */
    public ReplyMessage message(boolean explicit) {
        switch (this) {
            case NEW_PURCHASE:
                return ReplyMessage.THANK_YOU;
            case SHARED:
                return explicit ? ReplyMessage.LINKED_TO_OTHER_ACCOUNT : ReplyMessage.NONE;
            case REFUNDED:
                return ReplyMessage.VACATION_REFUNDED;
            case INVALID:
                return explicit ? ReplyMessage.NO_VALID_PURCHASE : ReplyMessage.NONE;
            default:
                return ReplyMessage.NONE;
        }
    }

    /** Whether the server granted or holds an entitlement, so the player's flags must reload. */
    public boolean reloadsPlayer() {
        return this == NEW_PURCHASE || this == RENEWAL || this == KNOWN;
    }
}
