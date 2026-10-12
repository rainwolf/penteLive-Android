package be.submanifold.pentelive.billing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** The Play product IDs pente.org sells (spec section 2). Play-free so it can be unit tested. */
public final class Products {

    public static final String YEARLY = "yearly_subscription";
    /** Base plan of {@link #YEARLY} whose offer token is launched. */
    public static final String YEARLY_BASE_PLAN = "yearly";
    public static final String MONTH_PASS = "month_pass";
    public static final String VACATION_10 = "vacation_10";
    public static final String VACATION_30 = "vacation_30";
    public static final String VACATION_60 = "vacation_60";

    /** Products of Play type SUBS. */
    public static final List<String> SUBS = Collections.singletonList(YEARLY);
    /** Products of Play type INAPP (one-time, consumable). */
    public static final List<String> INAPP = Collections.unmodifiableList(
            Arrays.asList(MONTH_PASS, VACATION_10, VACATION_30, VACATION_60));

    private Products() {
    }

    /** Whether {@code productId} is one of the products above. */
    public static boolean isKnown(String productId) {
        return SUBS.contains(productId) || INAPP.contains(productId);
    }

    /** Whether {@code productId} is an auto-renewing subscription. */
    public static boolean isSubscription(String productId) {
        return SUBS.contains(productId);
    }
}
