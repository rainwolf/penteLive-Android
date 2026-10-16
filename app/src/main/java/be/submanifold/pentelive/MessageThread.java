package be.submanifold.pentelive;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java helpers for the messageJson endpoint: parse and validate the response and order
 * the thread entries. No Android dependencies, so it is unit-testable on the JVM.
 */
public class MessageThread {

    private static final Gson GSON = new Gson();

    private MessageThread() {
    }

    /**
     * Parses a messageJson body. Returns null on anything that is not a successful reply
     * (invalid JSON such as LoginFilter's HTML login page, success:false, or no message).
     */
    public static JsonModels.MessageThreadResponse parse(String body) {
        if (body == null) {
            return null;
        }
        try {
            JsonModels.MessageThreadResponse response = GSON.fromJson(body, JsonModels.MessageThreadResponse.class);
            if (response == null || !response.success || response.message == null) {
                return null;
            }
            return response;
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    /**
     * Newest-first order: the received message first, then the viewer's earlier message
     * it replied to (if any).
     */
    public static List<JsonModels.MessageThreadEntry> entries(JsonModels.MessageThreadResponse response) {
        List<JsonModels.MessageThreadEntry> entries = new ArrayList<>();
        entries.add(response.message);
        if (response.replyTo != null) {
            entries.add(response.replyTo);
        }
        return entries;
    }
}
