package be.submanifold.pentelive.billing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * POSTs purchases to /gameServer/googlePlayPurchase and turns the replies into what the
 * player sees (spec 3.3). Blocking: call it off the main thread. Android- and Play-free; the
 * HTTP call sits behind {@link Transport} so tests can fake it.
 */
public final class PurchaseSender {

    public static final String PATH = "/gameServer/googlePlayPurchase";

    /** Sends one purchase; returns the reply body, or null when the status was not 200. */
    public interface Transport {
        String post(String productId, String purchaseToken) throws IOException;
    }

    /** What a batch of sends asks the UI to do. */
    public static final class Outcome {
        /** Distinct messages to show, in first-seen order; never contains NONE. */
        public final List<ReplyMessage> messages;
        /** Whether any reply granted or confirmed an entitlement. */
        public final boolean reloadPlayer;
        /**
         * Tokens the server answered "success:shared" or "invalid receipt" for, in send order;
         * the automatic resync skips them for the rest of the process.
         */
        public final List<String> rejectedTokens;

        Outcome(List<ReplyMessage> messages, boolean reloadPlayer, List<String> rejectedTokens) {
            this.messages = messages;
            this.reloadPlayer = reloadPlayer;
            this.rejectedTokens = rejectedTokens;
        }
    }

    private final Transport transport;

    public PurchaseSender(Transport transport) {
        this.transport = transport;
    }

    /** Sends one purchase. Network errors and runtime failures read as {@link PurchaseReply#FAILED}. */
    public PurchaseReply send(String productId, String purchaseToken) {
        try {
            return PurchaseReply.fromServer(transport.post(productId, purchaseToken));
        } catch (IOException | RuntimeException e) {
            return PurchaseReply.FAILED;
        }
    }

    /**
     * Sends each purchase in order. With {@code explicit} ("Restore purchases") and no
     * purchases, the outcome says there is nothing to restore; when no send reached the
     * server, it says the restore failed.
     *
     * @param fromPurchaseFlow true for purchases from onPurchasesUpdated (the player just paid):
     *                         KNOWN and RENEWAL also thank, because RTDN may have granted the
     *                         purchase before this POST arrived. False for resync and restore.
     */
    public Outcome sendAll(List<PurchaseRecord> purchases, boolean explicit, boolean fromPurchaseFlow) {
        Set<ReplyMessage> messages = new LinkedHashSet<>();
        List<String> rejectedTokens = new ArrayList<>();
        boolean reload = false;
        boolean anyAnswered = false;
        if (purchases.isEmpty() && explicit) {
            messages.add(ReplyMessage.NO_PURCHASES_TO_RESTORE);
        }
        for (PurchaseRecord purchase : purchases) {
            PurchaseReply reply = send(purchase.productId, purchase.purchaseToken);
            ReplyMessage message = reply.message(explicit);
            if (fromPurchaseFlow && (reply == PurchaseReply.KNOWN || reply == PurchaseReply.RENEWAL)) {
                message = ReplyMessage.THANK_YOU;
            }
            if (message != ReplyMessage.NONE) {
                messages.add(message);
            }
            if (reply == PurchaseReply.SHARED || reply == PurchaseReply.INVALID) {
                rejectedTokens.add(purchase.purchaseToken);
            }
            anyAnswered |= reply != PurchaseReply.FAILED;
            reload |= reply.reloadsPlayer();
        }
        if (explicit && !purchases.isEmpty() && !anyAnswered) {
            messages.add(ReplyMessage.RESTORE_FAILED);
        }
        return new Outcome(new ArrayList<>(messages), reload, rejectedTokens);
    }
}
