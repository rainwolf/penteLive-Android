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

    /** Returns {invite, chat} for the chosen dialog kind and button. */
    static boolean[] flags(boolean forInvitation, boolean both) {
        if (both) {
            return new boolean[]{true, true};
        }
        return forInvitation ? new boolean[]{true, false} : new boolean[]{false, true};
    }

    public static void confirmAndBlock(final AppCompatActivity activity, final String name,
                                       final boolean forInvitation, final Runnable onSuccess) {
        @StringRes int messageRes = forInvitation
                ? R.string.block_invitation_dialog_text : R.string.block_message_dialog_text;
        @StringRes int onlyRes = forInvitation ? R.string.block_invitations : R.string.block_messages;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.block_dialog_title, name))
                .setMessage(activity.getString(messageRes, name))
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(onlyRes, (d, w) -> block(activity, name, flags(forInvitation, false), onSuccess))
                .setPositiveButton(R.string.block_both, (d, w) -> block(activity, name, flags(forInvitation, true), onSuccess))
                .create();
        dialog.show();
        int red = ContextCompat.getColor(activity, android.R.color.holo_red_dark);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(red);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(red);
    }

    private static void block(final AppCompatActivity activity, final String name,
                              final boolean[] flags, final Runnable onSuccess) {
        new BackgroundTask<Void, IOException>() {
            @Override
            protected IOException doInBackground(Void... params) {
                try {
                    BlockedPlayersClient.block(name, flags[0], flags[1]);
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
