package be.submanifold.pentelive.db;

/** Phase 0 helper for the legacy mobileController/search params. Deleted in Task 16. */
public final class LegacyDbQuery {
    private LegacyDbQuery() {}

    public static String liveOrTbParam(String selected, String liveOnlyLabel, String turnBasedOnlyLabel) {
        if (liveOnlyLabel.equals(selected)) {
            return "&only_live=yes";
        }
        if (turnBasedOnlyLabel.equals(selected)) {
            return "&only_turn_based=yes";
        }
        return "";
    }
}
