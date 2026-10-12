package be.submanifold.pentelive.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PurchaseReplyTest {

    @Test
    public void classifiesEveryServerReply() {
        assertEquals(PurchaseReply.NEW_PURCHASE, PurchaseReply.fromServer("success"));
        assertEquals(PurchaseReply.NEW_PURCHASE, PurchaseReply.fromServer("success\n"));
        assertEquals(PurchaseReply.RENEWAL, PurchaseReply.fromServer("success:renewal"));
        assertEquals(PurchaseReply.KNOWN, PurchaseReply.fromServer("success:known"));
        assertEquals(PurchaseReply.SHARED, PurchaseReply.fromServer("success:shared"));
        assertEquals(PurchaseReply.REFUNDED, PurchaseReply.fromServer("success:refunded"));
        assertEquals(PurchaseReply.INVALID, PurchaseReply.fromServer("invalid receipt"));
    }

    @Test
    public void unknownSuccessVariantIsKnownNeverThankYou() {
        assertEquals(PurchaseReply.KNOWN, PurchaseReply.fromServer("success:somethingnew"));
    }

    @Test
    public void noReplyOrGarbageIsFailed() {
        assertEquals(PurchaseReply.FAILED, PurchaseReply.fromServer(null));
        assertEquals(PurchaseReply.FAILED, PurchaseReply.fromServer(""));
        assertEquals(PurchaseReply.FAILED, PurchaseReply.fromServer("<html>login</html>"));
        assertEquals(PurchaseReply.FAILED, PurchaseReply.fromServer("error"));
    }

    @Test
    public void messagesFollowSpecTable() {
        assertEquals(ReplyMessage.THANK_YOU, PurchaseReply.NEW_PURCHASE.message(false));
        assertEquals(ReplyMessage.THANK_YOU, PurchaseReply.NEW_PURCHASE.message(true));
        assertEquals(ReplyMessage.NONE, PurchaseReply.RENEWAL.message(true));
        assertEquals(ReplyMessage.NONE, PurchaseReply.KNOWN.message(true));
        assertEquals(ReplyMessage.NONE, PurchaseReply.SHARED.message(false));
        assertEquals(ReplyMessage.LINKED_TO_OTHER_ACCOUNT, PurchaseReply.SHARED.message(true));
        assertEquals(ReplyMessage.VACATION_REFUNDED, PurchaseReply.REFUNDED.message(false));
        assertEquals(ReplyMessage.VACATION_REFUNDED, PurchaseReply.REFUNDED.message(true));
        assertEquals(ReplyMessage.NONE, PurchaseReply.INVALID.message(false));
        assertEquals(ReplyMessage.NO_VALID_PURCHASE, PurchaseReply.INVALID.message(true));
        assertEquals(ReplyMessage.NONE, PurchaseReply.FAILED.message(true));
    }

    @Test
    public void onlyGrantingRepliesReloadThePlayer() {
        assertTrue(PurchaseReply.NEW_PURCHASE.reloadsPlayer());
        assertTrue(PurchaseReply.RENEWAL.reloadsPlayer());
        assertTrue(PurchaseReply.KNOWN.reloadsPlayer());
        assertFalse(PurchaseReply.SHARED.reloadsPlayer());
        assertFalse(PurchaseReply.REFUNDED.reloadsPlayer());
        assertFalse(PurchaseReply.INVALID.reloadsPlayer());
        assertFalse(PurchaseReply.FAILED.reloadsPlayer());
    }
}
