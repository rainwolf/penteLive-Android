package be.submanifold.pentelive.db.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import androidx.core.content.ContextCompat;

import be.submanifold.pentelive.R;
import be.submanifold.pentelive.db.CaptureIcons;

/** Toolbar subtitle: game name plus the two capture icons with their counts. */
public final class CaptureSpans {
    private CaptureSpans() {
    }

    public static SpannableStringBuilder build(Context context, String gameName,
                                               CaptureIcons.Counts caps, int lineHeight) {
        SpannableStringBuilder sb = new SpannableStringBuilder(gameName == null ? "" : gameName);
        if (caps == null || !caps.visible) {
            return sb;
        }
        int size = lineHeight * 4 / 5;
        if (sb.length() > 0) {
            sb.append("  ");
        }
        append(sb, context, R.drawable.white_nobg, caps.p1Fill, size);
        sb.append(" x ").append(String.valueOf(caps.p1Count)).append("  ");
        append(sb, context, R.drawable.black_nobg, caps.p2Fill, size);
        sb.append(" x ").append(String.valueOf(caps.p2Count));
        return sb;
    }

    private static void append(SpannableStringBuilder sb, Context context, int res, int tint, int size) {
        Drawable base = ContextCompat.getDrawable(context, res);
        if (base == null) {
            return;
        }
        Drawable icon = base.mutate();
        if (res == R.drawable.black_nobg) {
            icon.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP);
        }
        icon.setBounds(0, 0, size, size);
        sb.append(" ");
        sb.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
    }
}
