package be.submanifold.pentelive;

/**
 * Drawable resource ids for the King of the Hill crown icons (kothcrown1..kothcrown39),
 * indexed by (crown - 4). Replaces runtime getIdentifier("kothcrown" + (crown - 3), ...)
 * lookups used by DashboardListAdapter, KingOfTheHillListAdapter, LivePlayer and
 * WhosOnlineListAdapter, so the ids are resolved at compile time instead of by name.
 * An out-of-range crown throws ArrayIndexOutOfBoundsException, matching the previous
 * failure mode (getIdentifier returning 0, then getDrawable(0) throwing
 * Resources.NotFoundException) rather than silently hiding it.
 */
public final class KothCrownDrawables {

    private KothCrownDrawables() {
    }

    public static final int[] IDS = {
            R.drawable.kothcrown1, R.drawable.kothcrown2, R.drawable.kothcrown3, R.drawable.kothcrown4,
            R.drawable.kothcrown5, R.drawable.kothcrown6, R.drawable.kothcrown7, R.drawable.kothcrown8,
            R.drawable.kothcrown9, R.drawable.kothcrown10, R.drawable.kothcrown11, R.drawable.kothcrown12,
            R.drawable.kothcrown13, R.drawable.kothcrown14, R.drawable.kothcrown15, R.drawable.kothcrown16,
            R.drawable.kothcrown17, R.drawable.kothcrown18, R.drawable.kothcrown19, R.drawable.kothcrown20,
            R.drawable.kothcrown21, R.drawable.kothcrown22, R.drawable.kothcrown23, R.drawable.kothcrown24,
            R.drawable.kothcrown25, R.drawable.kothcrown26, R.drawable.kothcrown27, R.drawable.kothcrown28,
            R.drawable.kothcrown29, R.drawable.kothcrown30, R.drawable.kothcrown31, R.drawable.kothcrown32,
            R.drawable.kothcrown33, R.drawable.kothcrown34, R.drawable.kothcrown35, R.drawable.kothcrown36,
            R.drawable.kothcrown37, R.drawable.kothcrown38, R.drawable.kothcrown39
    };
}
