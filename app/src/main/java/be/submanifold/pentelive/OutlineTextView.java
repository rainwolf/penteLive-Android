package be.submanifold.pentelive;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.TextPaint;
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

    @Override
    protected void onDraw(Canvas canvas) {
        Layout layout = getLayout();
        if (!isNightMode() || layout == null) {
            super.onDraw(canvas);
            return;
        }

        TextPaint paint = getPaint();
        Paint.Style originalStyle = paint.getStyle();
        int fillColor = getCurrentTextColor();

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
