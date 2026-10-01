package be.submanifold.pentelive;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.core.content.ContextCompat;

public class Helpers {
    /** Toolbar icon tint; same list the menus apply via app:iconTint (res/color/toolbar_icon_tint.xml). */
    public static ColorStateList tintList(Context ctx) {
        return ContextCompat.getColorStateList(ctx, R.color.toolbar_icon_tint);
    }

    public static int getResourceColor(Context ctx, int colorPrimary) {
        return ContextCompat.getColor(ctx, colorPrimary);
    }

}
