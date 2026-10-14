package be.submanifold.pentelive.billing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Which Play purchases the client sends to the server (spec 3.2). */
public final class ResyncSelector {

    private ResyncSelector() {
    }

    /**
     * Purchases from onPurchasesUpdated: every PURCHASED purchase of a known product.
     * PENDING purchases are never sent; they arrive later through the update listener,
     * resync or RTDN.
     */
    public static List<PurchaseRecord> forUpdate(List<PurchaseRecord> purchases) {
        return select(purchases, true, Collections.<String>emptySet());
    }

    /** {@link #forResync(List, boolean, Set)} with no tokens to skip. */
    public static List<PurchaseRecord> forResync(List<PurchaseRecord> purchases, boolean explicit) {
        return forResync(purchases, explicit, Collections.<String>emptySet());
    }

    /**
     * Purchases from queryPurchasesAsync (SUBS and INAPP together).
     *
     * @param explicit       true for "Restore purchases": every PURCHASED purchase. False for
     *                       the automatic resync: only purchases that still need server action,
     *                       i.e. subscriptions not yet acknowledged and one-time products still
     *                       returned by Play (not yet consumed).
     * @param skipTokens     tokens the automatic resync skips: the server already rejected them
     *                       ("success:shared", "invalid receipt") or dealt with them in this
     *                       process. "Restore purchases" sends them anyway.
     */
    public static List<PurchaseRecord> forResync(List<PurchaseRecord> purchases, boolean explicit,
                                                 Set<String> skipTokens) {
        return select(purchases, explicit, explicit ? Collections.<String>emptySet() : skipTokens);
    }

    private static List<PurchaseRecord> select(List<PurchaseRecord> purchases, boolean includeAcknowledgedSubs,
                                               Set<String> skipTokens) {
        List<PurchaseRecord> selected = new ArrayList<>();
        if (purchases == null) {
            return selected;
        }
        Set<String> seenTokens = new HashSet<>();
        for (PurchaseRecord p : purchases) {
            if (p == null || !p.purchased || p.purchaseToken == null || p.purchaseToken.isEmpty()
                    || !Products.isKnown(p.productId)) {
                continue;
            }
            if (!includeAcknowledgedSubs && Products.isSubscription(p.productId) && p.acknowledged) {
                continue;
            }
            if (skipTokens != null && skipTokens.contains(p.purchaseToken)) {
                continue;
            }
            if (seenTokens.add(p.purchaseToken)) {
                selected.add(p);
            }
        }
        return selected;
    }
}
