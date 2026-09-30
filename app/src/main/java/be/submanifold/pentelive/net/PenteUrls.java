package be.submanifold.pentelive.net;

import be.submanifold.pentelive.PentePlayer;

/** Absolute URLs on the current server (production, or 10.0.2.2 in development builds). */
public final class PenteUrls {

    private PenteUrls() {
    }

    /** {@code web("/gameServer/stairs.jsp?game=1")} is that path on the current base URL. */
    public static String web(String pathAndQuery) {
        return new StaticBaseUrlProvider(PentePlayer.development).baseUrl() + pathAndQuery;
    }
}
