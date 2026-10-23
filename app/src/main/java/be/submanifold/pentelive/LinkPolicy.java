package be.submanifold.pentelive;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Pure decision logic for every link the app opens. Android-free so it is unit-testable.
 * Callers act on the result via {@link LinkOpener}.
 */
public final class LinkPolicy {

    public enum Action {
        /** pente.org (or the dev backend): may load in the in-app web view. */
        IN_APP,
        /** Hand to the system handler (browser, mail, dialer). */
        EXTERNAL,
        /** Would be external, but kid accounts may not leave pente.org. */
        BLOCKED_KID,
        /** Never navigated: javascript:, file:, intent:, data:, unknown or malformed. */
        BLOCKED
    }

    private LinkPolicy() {
    }

    /** True for http(s) URLs whose host is pente.org, *.pente.org, or {@code devHost} (may be null). */
    public static boolean isPenteUrl(String url, String devHost) {
        URI uri = parse(url);
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.equals("pente.org") || host.endsWith(".pente.org")) {
            return true;
        }
        return devHost != null && host.equals(devHost.toLowerCase(Locale.ROOT));
    }

    /** Kid unless a logged-in account is positively known to be a non-kid. */
    public static boolean shouldTreatAsKid(boolean loggedIn, boolean kidStatusKnown, boolean kidAccount) {
        if (!loggedIn || !kidStatusKnown) {
            return true;
        }
        return kidAccount;
    }

    public static Action decide(String url, boolean kid, String devHost) {
        URI uri = parse(url);
        if (uri == null || uri.getScheme() == null) {
            return Action.BLOCKED;
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (scheme.equals("http") || scheme.equals("https")) {
            if (uri.getHost() == null) {
                return Action.BLOCKED;
            }
            if (isPenteUrl(url, devHost)) {
                return Action.IN_APP;
            }
            return kid ? Action.BLOCKED_KID : Action.EXTERNAL;
        }
        if (scheme.equals("mailto") || scheme.equals("tel")) {
            return kid ? Action.BLOCKED_KID : Action.EXTERNAL;
        }
        return Action.BLOCKED;
    }

    private static URI parse(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
