package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class ProductsTest {

    @Test
    public void productIdsMatchSpec() {
        assertEquals(Arrays.asList("yearly_subscription"), Products.SUBS);
        assertEquals(Arrays.asList("month_pass", "vacation_10", "vacation_30", "vacation_60"), Products.INAPP);
        assertEquals("yearly", Products.YEARLY_BASE_PLAN);
    }

    @Test
    public void classifiesProducts() {
        assertTrue(Products.isKnown("vacation_30"));
        assertTrue(Products.isSubscription("yearly_subscription"));
        assertFalse(Products.isSubscription("month_pass"));
        assertFalse(Products.isKnown("coins_100"));
        assertFalse(Products.isKnown(null));
    }
}
