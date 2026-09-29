package be.submanifold.pentelive;

import android.util.Log;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Logs network failures without the credentials that ride in request URLs.
 * <p>
 * Most requests still send name2/password2 in the query string, and HttpURLConnection
 * puts the full URL in the message of the exception it throws for an HTTP error status.
 * Log.e and printStackTrace print that message (and those of causes and suppressed
 * exceptions), so the stack trace is logged from a redacted copy instead.
 */
public final class RedactingLog {

    private static final String REDACTED = "<redacted>";
    // Whole query: the password is not URL-encoded, so it may itself contain '&' or '?'.
    private static final Pattern URL_QUERY = Pattern.compile("\\?\\S*");
    private static final Pattern BARE_PASSWORD = Pattern.compile("(?i)(password2?=)\\S*");

    private RedactingLog() {
    }

    /**
     * {@link Log#e(String, String, Throwable)} with {@code tr} replaced by {@link #redacted}.
     * The copies are not UnknownHostExceptions, so Log keeps their trace (it drops a trace
     * whose cause chain has one), as printStackTrace did.
     */
    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, redacted(tr));
    }

    /**
     * A copy of {@code tr} whose messages, and those of its causes and suppressed exceptions,
     * have every URL query string and password parameter removed. The copies print as the
     * original class name and keep the original stack frames; {@code tr} is not modified.
     */
    static Throwable redacted(Throwable tr) {
        return copy(tr, new IdentityHashMap<>());
    }

    private static Throwable copy(Throwable original, Map<Throwable, Throwable> copies) {
        Throwable existing = copies.get(original);
        if (existing != null) {
            return existing;
        }
        Redacted copy = new Redacted(original);
        copies.put(original, copy);
        Throwable cause = original.getCause();
        if (cause != null) {
            copy.initCause(copy(cause, copies));
        }
        for (Throwable suppressed : original.getSuppressed()) {
            copy.addSuppressed(copy(suppressed, copies));
        }
        return copy;
    }

    private static String redact(String text) {
        if (text == null) {
            // No message: stays null, as on the original.
            return null;
        }
        String withoutQueries = URL_QUERY.matcher(text).replaceAll("?" + REDACTED);
        return BARE_PASSWORD.matcher(withoutQueries).replaceAll("$1" + REDACTED);
    }

    private static final class Redacted extends Throwable {

        private final String description;

        Redacted(Throwable original) {
            super(redact(original.getMessage()));
            // The original's toString() is "ClassName" or "ClassName: message", as the JDK prints it.
            description = redact(original.toString());
            setStackTrace(original.getStackTrace());
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
