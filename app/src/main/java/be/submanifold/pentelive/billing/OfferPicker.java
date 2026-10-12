package be.submanifold.pentelive.billing;

import java.util.List;

/** Picks the subscription offer to launch, from Play's eligible offers (spec 3.2). */
public final class OfferPicker {

    /** One {@code ProductDetails.SubscriptionOfferDetails}, without Play types. */
    public static final class OfferRef {
        public final String basePlanId;
        /** Null for the plain base plan; set for a developer offer on top of it. */
        public final String offerId;
        public final String offerToken;
        /** Formatted price of the offer's last (recurring) pricing phase. */
        public final String formattedPrice;

        public OfferRef(String basePlanId, String offerId, String offerToken, String formattedPrice) {
            this.basePlanId = basePlanId;
            this.offerId = offerId;
            this.offerToken = offerToken;
            this.formattedPrice = formattedPrice;
        }
    }

    private OfferPicker() {
    }

    /**
     * The plain {@code basePlanId} offer (no offerId) if Play returned one, otherwise the
     * first offer on that base plan, otherwise null. Offers without a token are skipped.
     */
    public static OfferRef pick(List<OfferRef> offers, String basePlanId) {
        if (offers == null) {
            return null;
        }
        OfferRef firstOnPlan = null;
        for (OfferRef offer : offers) {
            if (offer == null || !basePlanId.equals(offer.basePlanId)
                    || offer.offerToken == null || offer.offerToken.isEmpty()) {
                continue;
            }
            if (offer.offerId == null) {
                return offer;
            }
            if (firstOnPlan == null) {
                firstOnPlan = offer;
            }
        }
        return firstOnPlan;
    }
}
