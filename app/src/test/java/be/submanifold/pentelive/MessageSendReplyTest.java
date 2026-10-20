package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import be.submanifold.pentelive.MessageSendReply.Outcome;

import org.junit.Test;

public class MessageSendReplyTest {

    @Test
    public void notFound() {
        assertEquals(Outcome.NO_SUCH_USER, MessageSendReply.classify("<p>Error: Player bob not found.</p>", "bob"));
    }

    @Test
    public void kidNotApproved() {
        assertEquals(Outcome.NOT_APPROVED, MessageSendReply.classify("<p>You can only send messages to players your parent approved.</p>", "bob"));
    }

    @Test
    public void ignored() {
        assertEquals(Outcome.IGNORED, MessageSendReply.classify("<p>Player is ignoring your messages.</p>", "bob"));
    }

    @Test
    public void ordinaryPage_isSent() {
        assertEquals(Outcome.SENT, MessageSendReply.classify("<html>messages</html>", "bob"));
    }

    @Test
    public void nullBody_isNotSent() {
        assertNotEquals(Outcome.SENT, MessageSendReply.classify(null, "bob"));
    }
}
