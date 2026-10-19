package be.submanifold.pentelive.kids;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.submanifold.pentelive.BackgroundTask;
import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.NameStyle;
import be.submanifold.pentelive.R;

/** Parent view of one kid: copy toggles, approved players, password and release. */
public class KidDetailActivity extends AppCompatActivity {

    public static final String EXTRA_KID_PID = "kidPid";

    /** Outcome of a background call: the new reply, or a failure. */
    private static final class Outcome {
        KidAccounts.Reply reply;
        IOException error;
    }

    /** A blocking call into KidAccountsClient. */
    private interface Call {
        KidAccounts.Reply run() throws IOException;
    }

    private final List<KidAccounts.Player> approved = new ArrayList<>();
    private long kidPid;
    private String kidName = "";
    private boolean loaded;
    private Toolbar toolbar;
    private SwitchCompat copyMessages;
    private SwitchCompat copyLiveChat;
    private TextView emptyText;
    private RowAdapter adapter;

    private CompoundButton.OnCheckedChangeListener messagesListener;
    private CompoundButton.OnCheckedChangeListener liveChatListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kidPid = getIntent().getLongExtra(EXTRA_KID_PID, 0L);
        setContentView(R.layout.activity_kid_detail);
        toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        copyMessages = findViewById(R.id.copyMessagesSwitch);
        copyLiveChat = findViewById(R.id.copyLiveChatSwitch);
        emptyText = findViewById(R.id.emptyText);
        messagesListener = (b, flag) -> toggle(copyMessages, messagesListener, flag, true);
        liveChatListener = (b, flag) -> toggle(copyLiveChat, liveChatListener, flag, false);
        copyMessages.setOnCheckedChangeListener(messagesListener);
        copyLiveChat.setOnCheckedChangeListener(liveChatListener);

        findViewById(R.id.addApprovedButton).setOnClickListener(v -> showAddDialog());
        findViewById(R.id.setPasswordButton).setOnClickListener(v -> showPasswordDialog());
        findViewById(R.id.releaseButton).setOnClickListener(v -> confirmRelease());

        adapter = new RowAdapter();
        RecyclerView list = findViewById(R.id.approvedList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                confirmRemove(vh.getBindingAdapterPosition());
            }
        }).attachToRecyclerView(list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        run(KidAccountsClient::load, false, null, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    /**
     * Runs a call in the background. Success re-renders from the returned reply; a failure keeps
     * what is shown, tells the user why and runs {@code onFailure}.
     */
    private void run(Call call, boolean finishOnSuccess, Runnable onFailure, int toastRes) {
        new BackgroundTask<Void, Outcome>() {
            @Override
            protected Outcome doInBackground(Void... params) {
                Outcome o = new Outcome();
                try {
                    o.reply = call.run();
                } catch (IOException e) {
                    o.error = e;
                }
                return o;
            }

            @Override
            protected void onPostExecute(Outcome o) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (o.reply != null) {
                    if (finishOnSuccess) {
                        finish();
                        return;
                    }
                    loaded = true;
                    show(o.reply);
                    if (toastRes != 0) {
                        Toast.makeText(KidDetailActivity.this, toastRes, Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                if (onFailure != null) {
                    onFailure.run();
                }
                String message = (o.error instanceof KidAccountsClient.Failure)
                        ? o.error.getMessage() : null;
                new AlertDialog.Builder(KidDetailActivity.this)
                        .setTitle(kidName.isEmpty() ? getString(R.string.kid_accounts) : kidName)
                        .setMessage(message != null ? message : getString(R.string.try_again))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }.execute();
    }

    private void show(KidAccounts.Reply reply) {
        KidAccounts.Kid kid = KidAccounts.findKid(reply, kidPid);
        if (kid == null) {
            // Released elsewhere.
            finish();
            return;
        }
        kidName = kid.name == null ? "" : kid.name;
        toolbar.setTitle(kidName);
        setChecked(copyMessages, messagesListener, kid.copyMessages);
        setChecked(copyLiveChat, liveChatListener, kid.copyLiveChat);
        approved.clear();
        if (kid.whitelist != null) {
            approved.addAll(kid.whitelist);
        }
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(loaded && approved.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Sets a switch without firing its listener. */
    private static void setChecked(SwitchCompat s, CompoundButton.OnCheckedChangeListener l,
                                   boolean checked) {
        s.setOnCheckedChangeListener(null);
        s.setChecked(checked);
        s.setOnCheckedChangeListener(l);
    }

    private void toggle(SwitchCompat s, CompoundButton.OnCheckedChangeListener l, boolean flag,
                        boolean messages) {
        run(() -> messages
                        ? KidAccountsClient.settings(kidPid, flag, null)
                        : KidAccountsClient.settings(kidPid, null, flag),
                false, () -> setChecked(s, l, !flag), 0);
    }

    private EditText field(int hintRes, boolean password) {
        EditText e = new EditText(this);
        e.setHint(hintRes);
        e.setSingleLine(true);
        e.setInputType(password
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT);
        return e;
    }

    private LinearLayout dialogBody(EditText field) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad / 2, pad, 0);
        body.addView(field);
        return body;
    }

    private void showAddDialog() {
        EditText name = field(R.string.kid_name, false);
        new AlertDialog.Builder(this)
                .setTitle(R.string.kid_add_approved)
                .setView(dialogBody(name))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String n = name.getText().toString().trim();
                    run(() -> KidAccountsClient.whitelistAdd(kidPid, n), false, null, 0);
                })
                .show();
    }

    private void showPasswordDialog() {
        EditText password = field(R.string.kid_password, true);
        new AlertDialog.Builder(this)
                .setTitle(R.string.kid_set_password)
                .setView(dialogBody(password))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String p = password.getText().toString();
                    run(() -> KidAccountsClient.setPassword(kidPid, p), false, null,
                            R.string.kid_password_set);
                })
                .show();
    }

    private void confirmRelease() {
        new AlertDialog.Builder(this)
                .setTitle(kidName)
                .setMessage(getString(R.string.kid_release_text, kidName))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.kid_release,
                        (d, w) -> run(() -> KidAccountsClient.release(kidPid), true, null, 0))
                .show();
    }

    private void confirmRemove(int position) {
        if (position < 0 || position >= approved.size()) {
            return;
        }
        final KidAccounts.Player player = approved.get(position);
        final String playerName = player.name == null ? "" : player.name;
        final boolean[] confirmed = {false};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.kid_remove_approved_title, playerName))
                .setMessage(getString(R.string.kid_remove_approved_text, playerName, kidName))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.kid_remove, (d, w) -> {
                    confirmed[0] = true;
                    run(() -> KidAccountsClient.whitelistRemove(kidPid, player.pid), false,
                            () -> adapter.notifyItemChanged(position), 0);
                })
                .setOnDismissListener(d -> {
                    if (!confirmed[0]) {
                        adapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    private final class RowAdapter extends RecyclerView.Adapter<RowAdapter.Holder> {
        final class Holder extends RecyclerView.ViewHolder {
            final TextView name;

            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.nameText);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.kid_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            KidAccounts.Player p = approved.get(position);
            SpannableStringBuilder styled = NameStyle.colored(p.name == null ? "" : p.name, p.nameColor);
            NameStyle.appendCrown(KidDetailActivity.this, styled, p.tourneyWinner, h.name);
            h.name.setText(styled);
        }

        @Override
        public int getItemCount() {
            return approved.size();
        }
    }
}
