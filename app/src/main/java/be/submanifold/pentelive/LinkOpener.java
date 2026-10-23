package be.submanifold.pentelive;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import be.submanifold.pentelive.net.StaticBaseUrlProvider;

/** Android side of {@link LinkPolicy}: the one place links are opened from. */
public final class LinkOpener {

    private LinkOpener() {
    }

    /** Host of the local backend when the development flag is on, else null. */
    public static String devHost() {
        if (!Boolean.TRUE.equals(PentePlayer.development)) {
            return null;
        }
        return java.net.URI.create(StaticBaseUrlProvider.DEV_BASE_URL).getHost();
    }

    /** Percent-encodes characters strict URI parsing rejects (spaces, |, {, non-ASCII); idempotent. */
    static String normalize(String url) {
        return url == null ? null : Uri.encode(url.trim(), "!#$&'()*+,-./:;=?@[]_~%");
    }

    public static LinkPolicy.Action decide(String url) {
        return LinkPolicy.decide(normalize(url), PentePlayer.shouldTreatAsKid(), devHost());
    }

    /** pente.org links open in the in-app web view; everything else per policy. */
    public static void open(Context context, String url) {
        if (decide(url) == LinkPolicy.Action.IN_APP) {
            Intent intent = new Intent(context, WebViewActivity.class);
            intent.putExtra("url", url);
            context.startActivity(intent);
        } else {
            openExternal(context, url);
        }
    }

    /** Opens in the system handler (never in-app); kid accounts are blocked from non-pente.org targets. */
    public static void openExternal(Context context, String url) {
        if (!allowExternal(context, url)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(normalize(url)));
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            // no app handles it
        }
    }

    /** True if url may be handed to the system; shows the kid message when that is why it may not. */
    public static boolean allowExternal(Context context, String url) {
        switch (decide(url)) {
            case IN_APP:
            case EXTERNAL:
                return true;
            case BLOCKED_KID:
                Toast.makeText(context, R.string.links_blocked_kid, Toast.LENGTH_SHORT).show();
                return false;
            default:
                return false;
        }
    }
}
