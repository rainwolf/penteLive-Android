package be.submanifold.pentelive;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

public class Helpers {
    public static ColorStateList tintList(Context ctx) {
        return new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_enabled}, //disabled
                        new int[]{android.R.attr.state_enabled} //enabled
                },
                new int[]{
                        Color.GRAY //disabled
                        , ContextCompat.getColor(ctx, R.color.colorPrimary) //enabled
                }
        );
    }

    public static int getResourceColor(Context ctx, int colorPrimary) {
        return ContextCompat.getColor(ctx, colorPrimary);
    }

}
