package be.submanifold.pentelive.net;

/**
 * Resolves the base URL exactly once at construction from a single dev/prod
 * flag, replacing the scattered per-call {@code if (development)} branches in
 * PentePlayer.java:650-651 and Game.java:398-401.
 */
public final class StaticBaseUrlProvider implements BaseUrlProvider {

    public static final String PROD_BASE_URL = "https://www.pente.org";
    public static final String DEV_BASE_URL  = "https://10.0.2.2";

    private final String baseUrl;

    public StaticBaseUrlProvider(boolean development) {
        this.baseUrl = development ? DEV_BASE_URL : PROD_BASE_URL;
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }
}
