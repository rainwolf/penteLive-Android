package be.submanifold.pentelive;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MessageThreadTest {

    private static final String WITH_REPLY_TO = "{\"success\":true,"
            + "\"message\":{\"mid\":20,\"replyToMid\":10,\"from\":\"bob\",\"to\":\"alice\",\"subject\":\"Re: hi\","
            + "\"body\":\"line1\\nline2 <b>x</b>\",\"date\":1700000100000,\"read\":false},"
            + "\"replyTo\":{\"mid\":10,\"replyToMid\":0,\"from\":\"alice\",\"to\":\"bob\",\"subject\":\"hi\","
            + "\"body\":\"hello\",\"date\":1700000000000}}";

    @Test
    public void successWithReplyToParsesAndOrdersChronologically() {
        JsonModels.MessageThreadResponse r = MessageThread.parse(WITH_REPLY_TO);
        assertNotNull(r);
        assertEquals(20, r.message.mid);
        assertEquals(10, r.message.replyToMid);
        assertEquals("bob", r.message.from);
        assertEquals("line1\nline2 <b>x</b>", r.message.body);
        assertEquals(1700000100000L, r.message.date);
        List<JsonModels.MessageThreadEntry> entries = MessageThread.entries(r);
        assertEquals(2, entries.size());
        assertEquals(20, entries.get(0).mid);
        assertEquals(10, entries.get(1).mid);
    }

    @Test
    public void successWithNullReplyToHasSingleEntry() {
        JsonModels.MessageThreadResponse r = MessageThread.parse("{\"success\":true,"
                + "\"message\":{\"mid\":5,\"replyToMid\":0,\"from\":\"bob\",\"to\":\"alice\",\"subject\":\"s\","
                + "\"body\":\"b\",\"date\":1,\"read\":true},\"replyTo\":null}");
        assertNotNull(r);
        assertNull(r.replyTo);
        List<JsonModels.MessageThreadEntry> entries = MessageThread.entries(r);
        assertEquals(1, entries.size());
        assertEquals(5, entries.get(0).mid);
    }

    @Test
    public void successFalseIsFailure() {
        assertNull(MessageThread.parse("{\"success\":false,\"error\":\"Message not found\"}"));
    }

    @Test
    public void htmlLoginPageIsFailure() {
        assertNull(MessageThread.parse("<html><body><form action=\"/gameServer/login\">Login</form></body></html>"));
    }

    @Test
    public void emptyOrSuccessWithoutMessageIsFailure() {
        assertNull(MessageThread.parse(""));
        assertNull(MessageThread.parse(null));
        assertNull(MessageThread.parse("{\"success\":true}"));
    }
}
