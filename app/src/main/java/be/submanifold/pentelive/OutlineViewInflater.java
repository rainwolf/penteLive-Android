package be.submanifold.pentelive;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.app.AppCompatViewInflater;
import androidx.appcompat.widget.AppCompatTextView;

/**
 * Makes AppCompat inflate every &lt;TextView&gt; as an {@link OutlineTextView}, so the
 * dark-mode outline applies app-wide without touching layouts. Wired via the AppTheme
 * {@code viewInflaterClass} item. Buttons/EditTexts use their own create* methods and
 * are unaffected.
 */
public class OutlineViewInflater extends AppCompatViewInflater {
    @Override
    protected AppCompatTextView createTextView(Context context, AttributeSet attrs) {
        return new OutlineTextView(context, attrs);
    }
}
