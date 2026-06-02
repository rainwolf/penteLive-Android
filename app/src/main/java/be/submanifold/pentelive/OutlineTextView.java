package be.submanifold.pentelive;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * In dark mode, draws a crisp black stroke around the (white) text so labels stay
 * legible on any background. Light mode renders normally. Installed app-wide via the
 * theme's viewInflaterClass (see OutlineViewInflater), so layouts need no changes.
 *
 * Drawn by stroking then filling the text Layout directly (no setTextColor in onDraw),
 * which avoids the continuous-invalidate loop the naive double-draw approach causes.
 */
public class OutlineTextView extends AppCompatTextView {

    private final float strokeWidth;

    public OutlineTextView(Context context) {
        this(context, null);
    }

    public OutlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        strokeWidth = 3f * getResources().getDisplayMetrics().density;
    }

    private boolean isNightMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    /** The black outline only helps (and only looks right) on white/near-white text. */
    private static boolean isNearWhite(int color) {
        return Color.red(color) >= 0xCC
                && Color.green(color) >= 0xCC
                && Color.blue(color) >= 0xCC;
    }

    /**
     * A ForegroundColorSpan (e.g. a user's chosen username color) overrides the base
     * text color, so the base color alone isn't enough to decide. If any span recolors
     * the text to something non-white, skip the outline — it looks bad on colored text.
     */
    private boolean hasNonWhiteColorSpan() {
        CharSequence text = getText();
        if (!(text instanceof Spanned)) {
            return false;
        }
        Spanned spanned = (Spanned) text;
        ForegroundColorSpan[] spans =
                spanned.getSpans(0, spanned.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            if (!isNearWhite(span.getForegroundColor())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        int fillColor = getCurrentTextColor();
        if (!isNightMode() || layout == null
                || !isNearWhite(fillColor) || hasNonWhiteColorSpan()) {
            super.onDraw(canvas);
            return;
        }

        TextPaint paint = getPaint();
        Paint.Style originalStyle = paint.getStyle();

        canvas.save();
        canvas.translate(getTotalPaddingLeft(), getTotalPaddingTop());

        // Black outline pass.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.BLACK);
        layout.draw(canvas);

        // Fill pass with the real text color (spans keep their own colors).
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        layout.draw(canvas);

        canvas.restore();
        paint.setStyle(originalStyle);
    }
}
