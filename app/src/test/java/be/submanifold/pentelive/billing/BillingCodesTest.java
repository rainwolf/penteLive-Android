package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;

import com.android.billingclient.api.BillingClient;

import org.junit.Test;

public class BillingCodesTest {

    @Test
    public void constantsMatchPlayBillingLibrary() {
        assertEquals(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, BillingCodes.SERVICE_DISCONNECTED);
        assertEquals(BillingClient.BillingResponseCode.OK, BillingCodes.OK);
        assertEquals(BillingClient.BillingResponseCode.USER_CANCELED, BillingCodes.USER_CANCELED);
        assertEquals(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE, BillingCodes.BILLING_UNAVAILABLE);
        assertEquals(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, BillingCodes.ITEM_UNAVAILABLE);
        assertEquals(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED, BillingCodes.ITEM_ALREADY_OWNED);
    }

    @Test
    public void classifiesResponseCodes() {
        assertEquals(BillingCodes.Outcome.PROCEED, BillingCodes.classify(BillingCodes.OK));
        assertEquals(BillingCodes.Outcome.SILENT, BillingCodes.classify(BillingCodes.USER_CANCELED));
        assertEquals(BillingCodes.Outcome.ALREADY_OWNED, BillingCodes.classify(BillingCodes.ITEM_ALREADY_OWNED));
        assertEquals(BillingCodes.Outcome.BILLING_UNAVAILABLE, BillingCodes.classify(BillingCodes.BILLING_UNAVAILABLE));
        assertEquals(BillingCodes.Outcome.ERROR, BillingCodes.classify(BillingCodes.SERVICE_DISCONNECTED));
        assertEquals(BillingCodes.Outcome.ERROR, BillingCodes.classify(6));
        assertEquals(BillingCodes.Outcome.ERROR, BillingCodes.classify(12));
    }
}
