package be.submanifold.pentelive.billing;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * The reply of GET /gameServer/googlePlayPurchase:
 * {"accountId": "...", "vacationAllowed": bool, "subscribedUntil": epoch ms or null, "autoRenewing": bool}.
 */
public final class StoreInfo {

    private static final Gson GSON = new Gson();

    /** Play's obfuscatedAccountId for this player; never blank. */
    public final String accountId;
    public final boolean vacationAllowed;
    /** End of the current entitlement in epoch ms, or null when the player has none. */
    public final Long subscribedUntil;
    /** Whether the player has an active or in-grace Play yearly subscription. */
    public final boolean autoRenewing;

    public StoreInfo(String accountId, boolean vacationAllowed, Long subscribedUntil, boolean autoRenewing) {
        this.accountId = accountId;
        this.vacationAllowed = vacationAllowed;
        this.subscribedUntil = subscribedUntil;
        this.autoRenewing = autoRenewing;
    }

    /** Field names match the server's JSON keys. */
    private static final class Wire {
        String accountId;
        Boolean vacationAllowed;
        Long subscribedUntil;
        Boolean autoRenewing;
    }

    /**
     * Parses the reply body. Returns null when the body is null, not a JSON object (for
     * example an HTML error page), or has no non-blank accountId or an accountId longer than
     * Play's 64-character limit. Missing booleans read as false.
     */
    public static StoreInfo parse(String body) {
        if (body == null) {
            return null;
        }
        Wire wire;
        try {
            wire = GSON.fromJson(body, Wire.class);
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
        if (wire == null || wire.accountId == null) {
            return null;
        }
        String accountId = wire.accountId.trim();
        if (accountId.isEmpty() || accountId.length() > 64) {
            return null;
        }
        return new StoreInfo(accountId,
                Boolean.TRUE.equals(wire.vacationAllowed),
                wire.subscribedUntil,
                Boolean.TRUE.equals(wire.autoRenewing));
    }
}
