package be.submanifold.pentelive.kids;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.submanifold.pentelive.BackgroundTask;
import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.NameStyle;
import be.submanifold.pentelive.R;

/**
 * Parent view: lists the user's kid accounts and creates or converts one.
 * Kid view: read-only list of the players the parent approved.
 */
public class KidAccountsActivity extends AppCompatActivity {

    /** Outcome of a background call: the new reply, or a failure. */
    private static final class Outcome {
        KidAccounts.Reply reply;
        IOException error;
    }

    /** A blocking call into KidAccountsClient. */
    private interface Call {
        KidAccounts.Reply run() throws IOException;
    }

    /** One row: a kid (parent view) or an approved player (kid view). */
    private static final class Row {
        String name;
        int nameColor;
        int tourneyWinner;
        long kidPid;
    }

    private final List<Row> rows = new ArrayList<>();
    private TextView introText;
    private TextView verifyText;
    private TextView approvedHeader;
    private TextView emptyText;
    private Button createButton;
    private Button convertButton;
    private RowAdapter adapter;
    private boolean loaded;
    private boolean kidView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kid_accounts);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle(getString(R.string.kid_accounts));
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        introText = findViewById(R.id.introText);
        verifyText = findViewById(R.id.verifyText);
        approvedHeader = findViewById(R.id.approvedHeader);
        emptyText = findViewById(R.id.emptyText);
        createButton = findViewById(R.id.createButton);
        convertButton = findViewById(R.id.convertButton);
        createButton.setOnClickListener(v -> showCreateDialog());
        convertButton.setOnClickListener(v -> showConvertDialog());

        adapter = new RowAdapter();
        RecyclerView list = findViewById(R.id.kidList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        run(KidAccountsClient::load, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    /**
     * Runs a call in the background. Success replaces everything shown with the returned reply
     * (and shows the reply's message in a Toast when {@code toastMessage} says so); a failure
     * keeps what is shown and tells the user why.
     */
    private void run(Call call, Boolean toastMessage) {
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
                    loaded = true;
                    show(o.reply);
                    if (toastMessage != null && o.reply.message != null) {
                        Toast.makeText(KidAccountsActivity.this, o.reply.message,
                                Toast.LENGTH_LONG).show();
                    }
                    return;
                }
                // A failed load or write keeps whatever is already shown and never claims "empty".
                String message = (o.error instanceof KidAccountsClient.Failure)
                        ? o.error.getMessage() : null;
                new AlertDialog.Builder(KidAccountsActivity.this)
                        .setTitle(getString(R.string.kid_accounts))
                        .setMessage(message != null ? message : getString(R.string.try_again))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }.execute();
    }

    private void show(KidAccounts.Reply reply) {
        rows.clear();
        kidView = reply.isKid;
        if (reply.isKid) {
            if (reply.parentName != null) {
                introText.setText(getString(R.string.kid_managed_by, reply.parentName));
                introText.setVisibility(View.VISIBLE);
            } else {
                introText.setVisibility(View.GONE);
            }
            verifyText.setVisibility(View.GONE);
            createButton.setVisibility(View.GONE);
            convertButton.setVisibility(View.GONE);
            approvedHeader.setVisibility(View.VISIBLE);
            emptyText.setText(R.string.kid_no_approved);
            if (reply.whitelist != null) {
                for (KidAccounts.Player p : reply.whitelist) {
                    Row r = new Row();
                    r.name = p.name;
                    r.nameColor = p.nameColor;
                    r.tourneyWinner = p.tourneyWinner;
                    rows.add(r);
                }
            }
        } else {
            introText.setText(R.string.kid_accounts_intro);
            introText.setVisibility(View.VISIBLE);
            verifyText.setVisibility(reply.emailVerified ? View.GONE : View.VISIBLE);
            createButton.setVisibility(View.VISIBLE);
            convertButton.setVisibility(View.VISIBLE);
            createButton.setEnabled(reply.emailVerified);
            convertButton.setEnabled(reply.emailVerified);
            approvedHeader.setVisibility(View.GONE);
            emptyText.setText(R.string.kid_none);
            if (reply.kids != null) {
                for (KidAccounts.Kid k : reply.kids) {
                    Row r = new Row();
                    r.name = k.name;
                    r.kidPid = k.kidPid;
                    rows.add(r);
                }
            }
        }
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(loaded && rows.isEmpty() ? View.VISIBLE : View.GONE);
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

    private LinearLayout dialogBody() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad / 2, pad, 0);
        return body;
    }

    private void showCreateDialog() {
        LinearLayout body = dialogBody();
        EditText name = field(R.string.kid_name, false);
        EditText password = field(R.string.kid_password, true);
        body.addView(name);
        body.addView(password);
        new AlertDialog.Builder(this)
                .setTitle(R.string.kid_create)
                .setView(body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String n = name.getText().toString().trim();
                    String p = password.getText().toString();
                    run(() -> KidAccountsClient.create(n, p), null);
                })
                .show();
    }

    private void showConvertDialog() {
        LinearLayout body = dialogBody();
        EditText name = field(R.string.kid_name, false);
        body.addView(name);
        new AlertDialog.Builder(this)
                .setTitle(R.string.kid_convert)
                .setMessage(R.string.kid_convert_hint)
                .setView(body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String n = name.getText().toString().trim();
                    // The reply is identical whether or not anything happened: show it as-is.
                    run(() -> KidAccountsClient.convert(n), Boolean.TRUE);
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
            Row r = rows.get(position);
            SpannableStringBuilder styled = NameStyle.colored(r.name == null ? "" : r.name, r.nameColor);
            NameStyle.appendCrown(KidAccountsActivity.this, styled, r.tourneyWinner, h.name);
            h.name.setText(styled);
            if (kidView) {
                h.itemView.setOnClickListener(null);
                h.itemView.setClickable(false);
            } else {
                h.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(KidAccountsActivity.this, KidDetailActivity.class);
                    intent.putExtra(KidDetailActivity.EXTRA_KID_PID, r.kidPid);
                    startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }
}
