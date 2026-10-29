package be.submanifold.pentelive.db;

/** Server-capability flags; flip when the matching backend change deploys. */
public final class DbCapabilities {
    public static final boolean CONNECT6_TURN_STEPS = false;
    public static final boolean SERVER_FILTERS_B3_B4 = false;

    private DbCapabilities() {
    }
}
