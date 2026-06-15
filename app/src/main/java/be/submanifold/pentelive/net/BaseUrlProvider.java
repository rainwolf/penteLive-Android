package be.submanifold.pentelive.net;

/** Supplies the resolved server base URL (scheme + host, no trailing slash). */
public interface BaseUrlProvider {
    String baseUrl();
}
