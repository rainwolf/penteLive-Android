package be.submanifold.pentelive.blocked;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.submanifold.pentelive.BackgroundTask;
import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.R;
import be.submanifold.pentelive.blocked.BlockedPlayers.Filter;
import be.submanifold.pentelive.blocked.BlockedPlayers.IgnoreEntry;
import be.submanifold.pentelive.blocked.BlockedPlayers.Kind;

/** Lists the players the user has blocked; swipe a row to unblock. */
public class BlockedPlayersActivity extends AppCompatActivity {

    private static final Filter[] FILTERS = {Filter.MESSAGES, Filter.INVITATIONS, Filter.BOTH};

    /** Outcome of a background call: the new list, or the failure. */
    private static final class Outcome {
        List<IgnoreEntry> entries;
        IOException error;
    }

    private final List<IgnoreEntry> allEntries = new ArrayList<>();
    private final List<IgnoreEntry> shown = new ArrayList<>();
    private AppCompatSpinner spinner;
    private TextView emptyText;
    private RowAdapter adapter;
    private boolean loaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_players);
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        toolbar.setTitle(getString(R.string.blocked_players));
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        spinner = findViewById(R.id.filterSpinner);
        emptyText = findViewById(R.id.emptyText);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[] {
                        getString(R.string.blocked_filter_messages),
                        getString(R.string.blocked_filter_invitations),
                        getString(R.string.blocked_filter_both)});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(2, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        adapter = new RowAdapter();
        RecyclerView list = findViewById(R.id.blockedList);
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
                confirmUnblock(vh.getBindingAdapterPosition());
            }
        }).attachToRecyclerView(list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
        new BackgroundTask<Void, Outcome>() {
            @Override
            protected Outcome doInBackground(Void... params) {
                Outcome o = new Outcome();
                try {
                    o.entries = BlockedPlayersClient.load();
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
                if (o.entries != null) {
                    loaded = true;
                    setEntries(o.entries);
                    return;
                }
                // A failed load keeps whatever is already shown and never claims "empty".
                String message = (o.error instanceof BlockedPlayersClient.Failure)
                        ? o.error.getMessage() : null;
                new AlertDialog.Builder(BlockedPlayersActivity.this)
                        .setTitle(getString(R.string.blocked_players))
                        .setMessage(message != null ? message : getString(R.string.try_again))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    private Filter currentFilter() {
        return FILTERS[Math.max(0, spinner.getSelectedItemPosition())];
    }

    private void setEntries(List<IgnoreEntry> entries) {
        allEntries.clear();
        allEntries.addAll(entries);
        refilter();
    }

    private void refilter() {
        shown.clear();
        shown.addAll(BlockedPlayers.filter(allEntries, currentFilter()));
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(loaded && shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String kindText(Kind kind) {
        switch (kind) {
            case MESSAGES:
                return getString(R.string.blocked_kind_messages);
            case INVITATIONS:
                return getString(R.string.blocked_kind_invitations);
            default:
                return getString(R.string.blocked_kind_both);
        }
    }

    /** Text of the flags being removed by unblocking from the given filter. */
    private String removedKindText(IgnoreEntry e, Filter filter) {
        switch (filter) {
            case MESSAGES:
                return getString(R.string.blocked_kind_messages);
            case INVITATIONS:
                return getString(R.string.blocked_kind_invitations);
            default:
                return kindText(BlockedPlayers.kind(e.ignoreInvite, e.ignoreChat));
        }
    }

    private void confirmUnblock(int position) {
        if (position < 0 || position >= shown.size()) {
            return;
        }
        final IgnoreEntry entry = shown.get(position);
        final Filter filter = currentFilter();
        final boolean[] confirmed = {false};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.unblock_dialog_title, entry.name))
                .setMessage(getString(R.string.unblock_dialog_text, entry.name,
                        removedKindText(entry, filter)))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.unblock, (d, w) -> {
                    confirmed[0] = true;
                    unblock(entry, filter, position);
                })
                .setOnDismissListener(d -> {
                    if (!confirmed[0]) {
                        adapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    private void unblock(IgnoreEntry entry, Filter filter, int position) {
        final boolean[] remaining = BlockedPlayers.remainingFlags(entry, filter);
        new BackgroundTask<Void, Outcome>() {
            @Override
            protected Outcome doInBackground(Void... params) {
                Outcome o = new Outcome();
                try {
                    o.entries = BlockedPlayersClient.update(entry.ignorePid, remaining[0], remaining[1]);
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
                if (o.entries != null) {
                    setEntries(o.entries);
                    return;
                }
                adapter.notifyItemChanged(position);
                String message = (o.error instanceof BlockedPlayersClient.Failure)
                        ? o.error.getMessage() : null;
                new AlertDialog.Builder(BlockedPlayersActivity.this)
                        .setTitle(getString(R.string.unblock_error_title, entry.name))
                        .setMessage(message != null ? message : getString(R.string.try_again))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }.execute();
    }

    private final class RowAdapter extends RecyclerView.Adapter<RowAdapter.Holder> {
        final class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView kind;

            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.nameText);
                kind = v.findViewById(R.id.kindText);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.blocked_player_row, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            IgnoreEntry e = shown.get(position);
            h.name.setText(e.name);
            if (currentFilter() == Filter.BOTH) {
                h.kind.setText(kindText(BlockedPlayers.kind(e.ignoreInvite, e.ignoreChat)));
                h.kind.setVisibility(View.VISIBLE);
            } else {
                h.kind.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return shown.size();
        }
    }
}
