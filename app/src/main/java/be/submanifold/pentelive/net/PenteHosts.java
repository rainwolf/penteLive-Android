package be.submanifold.pentelive.net;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The hosts whose cookies go through the shared {@link CookieStorage}: the configured base
 * URL's host (10.0.2.2 in development builds, www.pente.org in production), plus
 * www.pente.org, which some screens still hard-code, and pente.org, where the
 * Domain=pente.org login cookies live. Every other host (Firebase and other SDKs using
 * HttpURLConnection) stays out of the store.
 */
public final class PenteHosts {

    static final String WWW_HOST = URI.create(StaticBaseUrlProvider.PROD_BASE_URL).getHost();
    static final String APEX_HOST = "pente.org";

    private final Set<String> hosts;

    public PenteHosts(BaseUrlProvider urls) {
        Set<String> all = new HashSet<>(Arrays.asList(WWW_HOST, APEX_HOST));
        all.add(URI.create(urls.baseUrl()).getHost().toLowerCase(Locale.ROOT));
        this.hosts = Collections.unmodifiableSet(all);
    }

    /** Whether {@code uri}'s host is one of the pente hosts (exact match, case-insensitive). */
    public boolean contains(URI uri) {
        return hosts.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }
}
