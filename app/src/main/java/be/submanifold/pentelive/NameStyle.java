package be.submanifold.pentelive;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * How a player's name is drawn: donor color in bold, then a tournament (1-3) or
 * King of the Hill (above 3) crown. Shared by the dashboard message senders and the
 * Blocked players list so both look the same.
 */
public final class NameStyle {
    private NameStyle() {
    }

    /** The name, bold and colored when color is not 0 (0 means no donor color). */
    public static SpannableStringBuilder colored(String name, int color) {
        SpannableStringBuilder sb = new SpannableStringBuilder(name);
        if (color != 0) {
            sb.setSpan(new ForegroundColorSpan(color), 0, name.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return sb;
    }

    /** Appends the crown image for the given crown value, sized to the text line; nothing for 0. */
    public static void appendCrown(Context context, SpannableStringBuilder sb, int crown, TextView target) {
        Drawable crownIcon = null;
        switch (crown) {
            case 1:
                crownIcon = ContextCompat.getDrawable(context, R.drawable.crown);
                break;
            case 2:
                crownIcon = ContextCompat.getDrawable(context, R.drawable.scrown);
                break;
            case 3:
                crownIcon = ContextCompat.getDrawable(context, R.drawable.bcrown);
                break;
            default:
                if (crown > 3) {
                    crownIcon = ContextCompat.getDrawable(context, KothCrownDrawables.IDS[crown - 4]);
                }
                break;
        }
        if (crownIcon != null && crown > 0) {
            crownIcon.setBounds(0, 0, target.getLineHeight() * 2 / 3, target.getLineHeight() * 2 / 3);
            sb.append("   ").setSpan(new ImageSpan(crownIcon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
        }
    }
}
