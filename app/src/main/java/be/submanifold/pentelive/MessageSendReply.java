package be.submanifold.pentelive;

/** Classifies the HTML the server returns after a private message send. Pure Java. */
public final class MessageSendReply {

    public enum Outcome { SENT, NO_SUCH_USER, NOT_APPROVED, IGNORED }

    private MessageSendReply() {
    }

    public static Outcome classify(String body, String recipient) {
        if (body == null) {
            return Outcome.NO_SUCH_USER;
        }
        if (body.contains("You can only send messages to players your parent approved.")) {
            return Outcome.NOT_APPROVED;
        }
        if (body.contains("Player is ignoring your messages.")) {
            return Outcome.IGNORED;
        }
        if (body.contains("Error: Player " + recipient + " not found.")) {
            return Outcome.NO_SUCH_USER;
        }
        return Outcome.SENT;
    }
}
