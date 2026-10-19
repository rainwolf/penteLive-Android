package be.submanifold.pentelive.blocked;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;

import be.submanifold.pentelive.BackgroundTask;
import be.submanifold.pentelive.R;

/** Confirm-then-block flow shared by the screens that offer a block action. */
public final class BlockFlow {
    private BlockFlow() {
    }

    public static void confirmAndBlock(final AppCompatActivity activity, final String name,
                                       @StringRes int messageRes, final boolean invite,
                                       final boolean chat, final Runnable onSuccess) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.block_dialog_title, name))
                .setMessage(activity.getString(messageRes, name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.block, (d, w) -> block(activity, name, invite, chat, onSuccess))
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(activity, android.R.color.holo_red_dark));
    }

    private static void block(final AppCompatActivity activity, final String name,
                              final boolean invite, final boolean chat, final Runnable onSuccess) {
        new BackgroundTask<Void, IOException>() {
            @Override
            protected IOException doInBackground(Void... params) {
                try {
                    BlockedPlayersClient.block(name, invite, chat);
                    return null;
                } catch (IOException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(IOException error) {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                if (error == null) {
                    onSuccess.run();
                    return;
                }
                String text = (error instanceof BlockedPlayersClient.Failure)
                        ? error.getMessage() : null;
                new AlertDialog.Builder(activity)
                        .setTitle(activity.getString(R.string.block_error_title, name))
                        .setMessage(text != null ? text : activity.getString(R.string.try_again))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }.execute();
    }
}
