package be.submanifold.pentelive.db.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import be.submanifold.pentelive.R;
import be.submanifold.pentelive.StoreActivity;

/** The subscribers-only popup shown on a 403 {@code subscription_required}, shared by the DB screens. */
public final class SubscribeGate {
    private SubscribeGate() {
    }

    /**
     * Shows the gate over {@code anchor}, dimming it until dismissed. The subscribe button opens the store.
     * {@code onDismiss} (may be null) runs after the anchor is restored.
     */
    @SuppressLint("InflateParams")
    public static PopupWindow show(Activity activity, View anchor, Runnable onDismiss) {
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        View policyView = ((LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.db_subscribers_only, null, false);
        PopupWindow popup = new PopupWindow(policyView, width * 9 / 10, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        policyView.findViewById(R.id.subscribeButton).setOnClickListener(view -> {
            popup.dismiss();
            activity.startActivity(new Intent(activity, StoreActivity.class));
        });
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        ((TextView) policyView.findViewById(R.id.informationView)).setText(activity.getString(R.string.level_up_your_game));
        popup.setOnDismissListener(() -> {
            anchor.setAlpha(1.0f);
            if (onDismiss != null) onDismiss.run();
        });
        anchor.setAlpha(0.05f);
        popup.showAtLocation(anchor, Gravity.TOP, 0, 260);
        return popup;
    }
}
