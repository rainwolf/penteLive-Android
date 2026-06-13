package be.submanifold.pentelive.net;

import com.google.gson.Gson;

/**
 * The single shared {@link Gson} instance for the whole app, replacing the
 * six inline {@code new Gson()} call sites (LobbyActivity, MainActivity,
 * KingOfTheHillActivity, PentePlayer, Game x2).
 */
public final class Json {

    /** Shared, thread-safe Gson instance. Reuse this everywhere. */
    public static final Gson GSON = new Gson();

    private Json() {
        // No instances.
    }
}
