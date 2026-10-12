package be.submanifold.pentelive.billing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Recognises links to pente.org's PayPal subscription page, which the app replaces with
 * StoreActivity, and links to PayPal itself, which the app never opens.
 */
public final class SubscriptionLinks {

    private static final String SUBSCRIPTIONS_PATH = "/gameServer/subscriptions";
    private static final String SUBSCRIBE_JSP_PATH = "/gameServer/subscribe.jsp";

    private SubscriptionLinks() {
    }

    /**
     * Whether {@code url} is an http(s) link on pente.org, www.pente.org or {@code extraHost}
     * (the development server's host, or null) whose path is /gameServer/subscriptions or
     * /gameServer/subscribe.jsp, with or without a trailing slash, ;jsessionid, query or fragment.
     * Host matching ignores case; path matching does not (Tomcat paths are case-sensitive).
     */
    public static boolean isSubscriptionPage(String url, String extraHost) {
        if (url == null) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();
        if (scheme == null || host == null || path == null) {
            return false;
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        boolean penteHost = host.equals("pente.org") || host.equals("www.pente.org")
                || (extraHost != null && host.equals(extraHost.toLowerCase(Locale.ROOT)));
        if (!penteHost) {
            return false;
        }
        int matrix = path.indexOf(';'); // Tomcat URL rewriting: /path;jsessionid=...
        if (matrix >= 0) {
            path = path.substring(0, matrix);
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path.equals(SUBSCRIPTIONS_PATH) || path.equals(SUBSCRIBE_JSP_PATH);
    }

    /** Whether {@code url}'s host is paypal.com or a subdomain of it (case ignored). */
    public static boolean isPayPal(String url) {
        if (url == null) {
            return false;
        }
        String host;
        try {
            host = new URI(url.trim()).getHost();
        } catch (URISyntaxException e) {
            return false;
        }
        if (host == null) {
            return false;
        }
        host = host.toLowerCase(Locale.ROOT);
        return host.equals("paypal.com") || host.endsWith(".paypal.com");
    }
}
