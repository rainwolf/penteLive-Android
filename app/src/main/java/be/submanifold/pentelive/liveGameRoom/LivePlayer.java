package be.submanifold.pentelive.liveGameRoom;

import be.submanifold.pentelive.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;

import java.util.HashMap;
import java.util.Map;

import be.submanifold.pentelive.MyApplication;

/**
 * Created by waliedothman on 08/01/2017.
 */

public class LivePlayer {
    private String name;
    private Map<Integer, Integer> ratings = new HashMap<>();
    private boolean subscriber = false;
    private int crown = 0;
    private int color = 0;

    private Context ctx = MyApplication.getContext();

    public LivePlayer(String name, boolean subscriber, int crown, int color) {
        this.name = name;
        this.subscriber = subscriber;
        this.crown = crown;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setCrown(int crown) {
        this.crown = crown;
    }

    public void addRating(int game, int rating) {
        ratings.put(new Integer(game), new Integer(rating));
    }

    public int getRating(int game) {
        if (ratings.get(game) != null) {
            return ratings.get(game);
        }
        return 1600;
    }

    public SpannableStringBuilder coloredNameString(int height) {
        SpannableStringBuilder sb = new SpannableStringBuilder(name);
        if (subscriber) {
            ForegroundColorSpan fcs = new ForegroundColorSpan(color);
            StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
            sb.setSpan(fcs, 0, name.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            sb.setSpan(bss, 0, name.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }
//        System.out.println(" ******************* crown = " + crown);
        if (crown > 0) {
            Drawable crownIcon = null;
            switch (crown) {
                case 1:
                    crownIcon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.crown);
                    break;
                case 2:
                    crownIcon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.scrown);
                    break;
                case 3:
                    crownIcon = ContextCompat.getDrawable(MyApplication.getContext(), R.drawable.bcrown);
                    break;
                default:
                    if (crown > 3) {
                        int resourceId = ctx.getResources().getIdentifier("kothcrown"+(crown-3),"drawable", ctx.getPackageName());
                        crownIcon = ContextCompat.getDrawable(MyApplication.getContext(), resourceId);
                    }
                    break;
            }
            if (crownIcon != null && crown > 0) {
                crownIcon.setBounds(0, 0, height * 2 / 3, height * 2 / 3);
                sb.append("  ").setSpan(new ImageSpan(crownIcon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
            }
        }
        return sb;
    }

    public SpannableStringBuilder coloredRatingSquare(int ratingInt) {
        SpannableStringBuilder sb = new SpannableStringBuilder("\u25A0");
        ForegroundColorSpan ratingColor = null;new ForegroundColorSpan(color);
        if (ratingInt >= 1900) {
            ratingColor = new ForegroundColorSpan(Color.RED);
        } else if (ratingInt >= 1700) {
            ratingColor = new ForegroundColorSpan(Color.rgb((int) (0.98*255), (int) (0.96*255) ,(int) (0.03*255)));
        } else if (ratingInt >= 1400) {
            ratingColor =  new ForegroundColorSpan(Color.BLUE);
        } else if (ratingInt >= 1000) {
            ratingColor =  new ForegroundColorSpan(Color.rgb(30,130,76));
        } else {
            ratingColor = new ForegroundColorSpan(Color.GRAY);
        }
        sb.setSpan(ratingColor, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return sb;
    }

}
