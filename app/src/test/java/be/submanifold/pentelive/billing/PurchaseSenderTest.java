package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.Test;

public class PurchaseSenderTest {

    /** Replies by token; a token mapped to "IOException" throws, one not mapped returns null (non-200). */
    private static final class FakeTransport implements PurchaseSender.Transport {
        final Map<String, String> replies = new HashMap<>();
        final List<String> sent = new ArrayList<>();

        @Override
        public String post(String productId, String purchaseToken) throws IOException {
            sent.add(productId + ":" + purchaseToken);
            String reply = replies.get(purchaseToken);
            if ("IOException".equals(reply)) {
                throw new IOException("offline");
            }
            if ("RuntimeException".equals(reply)) {
                throw new IllegalStateException("password2 cookie missing");
            }
            return reply;
        }
    }

    private static PurchaseRecord record(String productId, String token) {
        return new PurchaseRecord(productId, token, true, false);
    }

    @Test
    public void sendsProductIdAndTokenAndClassifiesReply() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("t1", "success");
        PurchaseSender sender = new PurchaseSender(transport);
        assertEquals(PurchaseReply.NEW_PURCHASE, sender.send(Products.YEARLY, "t1"));
        assertEquals(Collections.singletonList("yearly_subscription:t1"), transport.sent);
    }

    @Test
    public void networkAndRuntimeFailuresAreFailed() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("io", "IOException");
        transport.replies.put("rt", "RuntimeException");
        PurchaseSender sender = new PurchaseSender(transport);
        assertEquals(PurchaseReply.FAILED, sender.send(Products.MONTH_PASS, "io"));
        assertEquals(PurchaseReply.FAILED, sender.send(Products.MONTH_PASS, "rt"));
        assertEquals(PurchaseReply.FAILED, sender.send(Products.MONTH_PASS, "non200"));
    }

    @Test
    public void restoreWithNothingSaysSo() {
        PurchaseSender.Outcome outcome =
                new PurchaseSender(new FakeTransport()).sendAll(Collections.emptyList(), true, false);
        assertEquals(Collections.singletonList(ReplyMessage.NO_PURCHASES_TO_RESTORE), outcome.messages);
        assertFalse(outcome.reloadPlayer);
    }

    @Test
    public void automaticResyncWithNothingIsSilent() {
        PurchaseSender.Outcome outcome =
                new PurchaseSender(new FakeTransport()).sendAll(Collections.emptyList(), false, false);
        assertTrue(outcome.messages.isEmpty());
    }

    @Test
    public void batchCollectsDistinctMessagesAndReloadsOnAnyGrant() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("a", "success:shared");
        transport.replies.put("b", "success:shared");
        transport.replies.put("c", "success:known");
        transport.replies.put("d", "IOException");
        PurchaseSender.Outcome outcome = new PurchaseSender(transport).sendAll(Arrays.asList(
                record(Products.YEARLY, "a"), record(Products.MONTH_PASS, "b"),
                record(Products.VACATION_10, "c"), record(Products.VACATION_30, "d")), true, false);
        assertEquals(Collections.singletonList(ReplyMessage.LINKED_TO_OTHER_ACCOUNT), outcome.messages);
        assertTrue(outcome.reloadPlayer);
        assertEquals(4, transport.sent.size());
    }

    @Test
    public void refundedVacationIsShownEvenWhenNotExplicit() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("v", "success:refunded");
        PurchaseSender.Outcome outcome = new PurchaseSender(transport)
                .sendAll(Collections.singletonList(record(Products.VACATION_60, "v")), false, false);
        assertEquals(Collections.singletonList(ReplyMessage.VACATION_REFUNDED), outcome.messages);
        assertFalse(outcome.reloadPlayer);
    }

    @Test
    public void purchaseFlowThanksEvenWhenTheServerAlreadyGranted() {
        // RTDN can grant before the client's POST arrives: the server then answers known or renewal.
        FakeTransport transport = new FakeTransport();
        transport.replies.put("k", "success:known");
        transport.replies.put("r", "success:renewal");
        List<PurchaseRecord> purchases = Arrays.asList(record(Products.YEARLY, "k"), record(Products.MONTH_PASS, "r"));
        PurchaseSender sender = new PurchaseSender(transport);
        PurchaseSender.Outcome flow = sender.sendAll(purchases, false, true);
        assertEquals(Collections.singletonList(ReplyMessage.THANK_YOU), flow.messages);
        assertTrue(flow.reloadPlayer);
        // The resume resync and "Restore purchases" stay silent for the same replies.
        assertTrue(sender.sendAll(purchases, false, false).messages.isEmpty());
        assertTrue(sender.sendAll(purchases, true, false).messages.isEmpty());
    }

    @Test
    public void sharedAndInvalidTokensAreReportedAsRejected() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("a", "success:shared");
        transport.replies.put("b", "invalid receipt");
        transport.replies.put("c", "success");
        transport.replies.put("d", "IOException");
        PurchaseSender.Outcome outcome = new PurchaseSender(transport).sendAll(Arrays.asList(
                record(Products.YEARLY, "a"), record(Products.MONTH_PASS, "b"),
                record(Products.VACATION_10, "c"), record(Products.VACATION_30, "d")), false, false);
        assertEquals(Arrays.asList("a", "b"), outcome.rejectedTokens);
    }

    @Test
    public void restoreWithOnlyFailedRepliesSaysSo() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("io", "IOException");
        transport.replies.put("k", "success:known");
        List<PurchaseRecord> failing = Arrays.asList(record(Products.YEARLY, "io"), record(Products.MONTH_PASS, "non200"));
        PurchaseSender sender = new PurchaseSender(transport);
        assertEquals(Collections.singletonList(ReplyMessage.RESTORE_FAILED), sender.sendAll(failing, true, false).messages);
        // The automatic resync stays silent; a restore that reached the server once says nothing extra.
        assertTrue(sender.sendAll(failing, false, false).messages.isEmpty());
        assertTrue(sender.sendAll(Arrays.asList(record(Products.YEARLY, "io"), record(Products.MONTH_PASS, "k")),
                true, false).messages.isEmpty());
    }

    @Test
    public void handledAndFailedTokensAreReported() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("new", "success");
        transport.replies.put("renewal", "success:renewal");
        transport.replies.put("known", "success:known");
        transport.replies.put("refunded", "success:refunded");
        transport.replies.put("shared", "success:shared");
        transport.replies.put("invalid", "invalid receipt");
        transport.replies.put("io", "IOException");
        transport.replies.put("garbage", "<html>maintenance</html>");
        List<PurchaseRecord> purchases = new ArrayList<>();
        for (String token : Arrays.asList("new", "renewal", "known", "refunded", "shared", "invalid", "io",
                "non200", "garbage")) {
            purchases.add(record(Products.VACATION_10, token));
        }
        PurchaseSender.Outcome outcome = new PurchaseSender(transport).sendAll(purchases, false, false);
        assertEquals(Arrays.asList("new", "renewal", "known", "refunded"), outcome.handledTokens);
        assertEquals(Arrays.asList("io", "non200", "garbage"), outcome.failedTokens);
        assertEquals(Arrays.asList("shared", "invalid"), outcome.rejectedTokens);
    }

    @Test
    public void outcomeListsAreUnmodifiable() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("a", "success");
        PurchaseSender.Outcome outcome = new PurchaseSender(transport)
                .sendAll(Collections.singletonList(record(Products.YEARLY, "a")), true, false);
        assertUnmodifiable(outcome.messages, ReplyMessage.THANK_YOU);
        assertUnmodifiable(outcome.rejectedTokens, "x");
        assertUnmodifiable(outcome.handledTokens, "x");
        assertUnmodifiable(outcome.failedTokens, "x");
    }

    private static <T> void assertUnmodifiable(List<T> list, T element) {
        try {
            list.add(element);
            fail("list is modifiable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void explicitInvalidReceiptSaysNoValidPurchase() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("i", "invalid receipt");
        PurchaseSender.Outcome outcome = new PurchaseSender(transport)
                .sendAll(Collections.singletonList(record(Products.YEARLY, "i")), true, false);
        assertEquals(Collections.singletonList(ReplyMessage.NO_VALID_PURCHASE), outcome.messages);
        assertFalse(outcome.reloadPlayer);
    }

    @Test
    public void messagesKeepFirstSeenOrder() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("i", "invalid receipt");
        transport.replies.put("s", "success:shared");
        transport.replies.put("i2", "invalid receipt");
        PurchaseSender sender = new PurchaseSender(transport);
        assertEquals(Arrays.asList(ReplyMessage.NO_VALID_PURCHASE, ReplyMessage.LINKED_TO_OTHER_ACCOUNT),
                sender.sendAll(Arrays.asList(record(Products.YEARLY, "i"), record(Products.MONTH_PASS, "s"),
                        record(Products.VACATION_10, "i2")), true, false).messages);
        assertEquals(Arrays.asList(ReplyMessage.LINKED_TO_OTHER_ACCOUNT, ReplyMessage.NO_VALID_PURCHASE),
                sender.sendAll(Arrays.asList(record(Products.MONTH_PASS, "s"), record(Products.YEARLY, "i")),
                        true, false).messages);
    }

    @Test
    public void noReloadWhenEveryReplyFailed() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("io", "IOException");
        transport.replies.put("rt", "RuntimeException");
        PurchaseSender.Outcome outcome = new PurchaseSender(transport).sendAll(Arrays.asList(
                record(Products.YEARLY, "io"), record(Products.MONTH_PASS, "rt"),
                record(Products.VACATION_10, "non200")), false, true);
        assertFalse(outcome.reloadPlayer);
        assertTrue(outcome.messages.isEmpty());
        assertTrue(outcome.handledTokens.isEmpty());
    }

    @Test
    public void explicitPurchaseFlowThanksForKnownAndExplainsShared() {
        FakeTransport transport = new FakeTransport();
        transport.replies.put("k", "success:known");
        transport.replies.put("s", "success:shared");
        PurchaseSender.Outcome outcome = new PurchaseSender(transport).sendAll(Arrays.asList(
                record(Products.YEARLY, "k"), record(Products.MONTH_PASS, "s")), true, true);
        assertEquals(Arrays.asList(ReplyMessage.THANK_YOU, ReplyMessage.LINKED_TO_OTHER_ACCOUNT), outcome.messages);
        assertTrue(outcome.reloadPlayer);
    }

    @Test
    public void runtimeFailuresAreLogged() {
        Logger logger = Logger.getLogger(PurchaseSender.class.getName());
        List<LogRecord> records = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            FakeTransport transport = new FakeTransport();
            transport.replies.put("tok-secret", "RuntimeException");
            assertEquals(PurchaseReply.FAILED, new PurchaseSender(transport).send(Products.MONTH_PASS, "tok-secret"));
        } finally {
            logger.removeHandler(handler);
        }
        assertEquals(1, records.size());
        assertTrue(records.get(0).getThrown() instanceof IllegalStateException);
        assertFalse("the token stays out of the log",
                String.valueOf(records.get(0).getMessage()).contains("tok-secret"));
    }
}
