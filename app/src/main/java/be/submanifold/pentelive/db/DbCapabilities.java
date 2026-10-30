package be.submanifold.pentelive.db;

/** Server-capability flags; flip when the matching backend change deploys. */
public final class DbCapabilities {
    public static final boolean CONNECT6_TURN_STEPS = false;
    /** Until {@code /ping} answers: B3/B4 filters are off. The live value is {@link DbController#serverFilters()}. */
    public static final boolean SERVER_FILTERS_DEFAULT = false;

    private DbCapabilities() {
    }
}
