package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ResyncSelectorTest {

    private static final PurchaseRecord SUB_ACKED = new PurchaseRecord(Products.YEARLY, "t-sub-acked", true, true);
    private static final PurchaseRecord SUB_UNACKED = new PurchaseRecord(Products.YEARLY, "t-sub-new", true, false);
    private static final PurchaseRecord PASS = new PurchaseRecord(Products.MONTH_PASS, "t-pass", true, false);
    private static final PurchaseRecord VACATION_ACKED = new PurchaseRecord(Products.VACATION_30, "t-vac", true, true);
    private static final PurchaseRecord PENDING = new PurchaseRecord(Products.VACATION_10, "t-pending", false, false);
    private static final PurchaseRecord UNKNOWN = new PurchaseRecord("coins_100", "t-coins", true, false);

    private static String tokens(List<PurchaseRecord> records) {
        StringBuilder s = new StringBuilder();
        for (PurchaseRecord r : records) {
            s.append(r.purchaseToken).append(' ');
        }
        return s.toString().trim();
    }

    @Test
    public void automaticResyncSendsOnlyPurchasesNeedingServerAction() {
        List<PurchaseRecord> all = Arrays.asList(SUB_ACKED, SUB_UNACKED, PASS, VACATION_ACKED, PENDING, UNKNOWN);
        // An INAPP purchase still returned by Play is not consumed, even if acknowledged.
        assertEquals("t-sub-new t-pass t-vac", tokens(ResyncSelector.forResync(all, false)));
    }

    @Test
    public void restoreSendsEveryPurchasedKnownPurchase() {
        List<PurchaseRecord> all = Arrays.asList(SUB_ACKED, SUB_UNACKED, PASS, VACATION_ACKED, PENDING, UNKNOWN);
        assertEquals("t-sub-acked t-sub-new t-pass t-vac", tokens(ResyncSelector.forResync(all, true)));
    }

    @Test
    public void updatesSendPurchasedButNeverPending() {
        assertEquals("t-sub-acked t-pass", tokens(ResyncSelector.forUpdate(Arrays.asList(SUB_ACKED, PENDING, PASS))));
    }

    @Test
    public void duplicateTokensAndBadRecordsAreDropped() {
        PurchaseRecord again = new PurchaseRecord(Products.MONTH_PASS, "t-pass", true, false);
        PurchaseRecord noToken = new PurchaseRecord(Products.MONTH_PASS, "", true, false);
        assertEquals("t-pass", tokens(ResyncSelector.forUpdate(Arrays.asList(PASS, again, noToken, null))));
    }

    @Test
    public void automaticResyncSkipsTokensTheServerRejected() {
        List<PurchaseRecord> all = Arrays.asList(SUB_ACKED, SUB_UNACKED, PASS, VACATION_ACKED, PENDING, UNKNOWN);
        Set<String> rejected = new HashSet<>(Arrays.asList("t-pass", "t-sub-new"));
        assertEquals("t-vac", tokens(ResyncSelector.forResync(all, false, rejected)));
        // "Restore purchases" still sends them, so the player sees why they were rejected.
        assertEquals("t-sub-acked t-sub-new t-pass t-vac", tokens(ResyncSelector.forResync(all, true, rejected)));
        assertEquals("t-sub-new t-pass t-vac",
                tokens(ResyncSelector.forResync(all, false, Collections.<String>emptySet())));
    }

    @Test
    public void nullListIsEmpty() {
        assertTrue(ResyncSelector.forResync(null, true).isEmpty());
        assertTrue(ResyncSelector.forUpdate(null).isEmpty());
    }
}
