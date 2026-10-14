package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class OfferPickerTest {

    private static final OfferPicker.OfferRef PROMO =
            new OfferPicker.OfferRef("yearly", "intro-50", "tok-promo", "€2.49");
    private static final OfferPicker.OfferRef BASE =
            new OfferPicker.OfferRef("yearly", null, "tok-base", "€4.99");
    private static final OfferPicker.OfferRef OTHER_PLAN =
            new OfferPicker.OfferRef("monthly", null, "tok-monthly", "€0.99");

    @Test
    public void prefersPlainBasePlanOffer() {
        assertEquals("tok-base", OfferPicker.pick(Arrays.asList(OTHER_PLAN, PROMO, BASE), "yearly").offerToken);
    }

    @Test
    public void fallsBackToFirstOfferOnThePlan() {
        assertEquals("tok-promo", OfferPicker.pick(Arrays.asList(OTHER_PLAN, PROMO), "yearly").offerToken);
    }

    @Test
    public void noOfferOnThePlan() {
        assertNull(OfferPicker.pick(Collections.singletonList(OTHER_PLAN), "yearly"));
        assertNull(OfferPicker.pick(Collections.emptyList(), "yearly"));
        assertNull(OfferPicker.pick(null, "yearly"));
    }

    @Test
    public void skipsOffersWithoutToken() {
        OfferPicker.OfferRef noToken = new OfferPicker.OfferRef("yearly", null, null, "€4.99");
        assertEquals("tok-promo", OfferPicker.pick(Arrays.asList(noToken, PROMO), "yearly").offerToken);
    }
}
