package be.submanifold.pentelive.liveGameRoom;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import be.submanifold.pentelive.R;

/** Host-side list of arena join requesters. It shows ArenaRequestListModel's applied rows (the
 *  server's snapshot, in seq order, keyed by player) and never removes a row itself.
 *  Tap a row = accept; swipe (wired via ItemTouchHelper in the fragment) = decline. The acted-on
 *  row shows a spinner until the server's next snapshot. */
public class ArenaJoinRequestAdapter extends RecyclerView.Adapter<ArenaJoinRequestAdapter.VH> {

    public interface ActionSender { void send(String event); }

    private final LayoutInflater inflater;
    private final TablesAndPlayers tablesAndPlayers;
    private final ArenaRequestListModel model;
    private final ActionSender sender;
    private final String me;
    private final int tableId;
    private final int gameId;

    private List<ArenaRequestRows.Row> shown = new ArrayList<>();

    public ArenaJoinRequestAdapter(LayoutInflater inflater, TablesAndPlayers tablesAndPlayers,
                                   ArenaRequestListModel model, ActionSender sender,
                                   String me, int tableId, int gameId) {
        this.inflater = inflater;
        this.tablesAndPlayers = tablesAndPlayers;
        this.model = model;
        this.sender = sender;
        this.me = me;
        this.tableId = tableId;
        this.gameId = gameId;
    }

    /** Re-reads the model and dispatches row-level changes, matched by player. */
    public void render() {
        List<ArenaRequestRows.Row> next = ArenaRequestRows.build(model.rows(), model.inFlightPlayer());
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(ArenaRequestRows.diff(shown, next));
        shown = next;
        diff.dispatchUpdatesTo(this);
    }

    public String playerAt(int position) {
        return position >= 0 && position < shown.size() ? shown.get(position).player : null;
    }

    /** Sends an accept for the row at position. False (nothing sent) if the model refuses the tap. */
    public boolean accept(int position) {
        String player = playerAt(position);
        if (player == null || !model.beginAction(player)) return false;
        sender.send(ArenaEvents.accept(me, player, tableId));
        return true;
    }

    /** Sends a decline for the row at position. False (nothing sent) if the model refuses it. */
    public boolean reject(int position) {
        String player = playerAt(position);
        if (player == null || !model.beginAction(player)) return false;
        sender.send(ArenaEvents.reject(me, player, tableId));
        return true;
    }

    /** Brings back a row that was swiped away but not declined. */
    @SuppressLint("NotifyDataSetChanged")
    public void restoreSwipedRow(int position) {
        if (position == RecyclerView.NO_POSITION) {
            // The swiped holder no longer maps to a row: rebind everything so no gap is left.
            notifyDataSetChanged();
        } else {
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(inflater.inflate(R.layout.arena_join_request_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ArenaRequestRows.Row row = shown.get(position);
        LivePlayer player = tablesAndPlayers.players.get(row.player);
        if (player != null) {
            int rating = player.getRating(gameId);
            holder.name.setText(player.coloredNameString(holder.name.getLineHeight()));
            android.text.SpannableStringBuilder ratingText = player.coloredRatingSquare(rating);
            ratingText.append(" ").append(String.valueOf(rating));
            holder.rating.setText(ratingText);
        } else {
            holder.name.setText(row.player);
            holder.rating.setText("");
        }
        holder.spinner.setVisibility(row.inFlight ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return shown.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView rating;
        final ProgressBar spinner;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.requestPlayerName);
            rating = itemView.findViewById(R.id.requestPlayerRating);
            spinner = itemView.findViewById(R.id.requestSpinner);
        }
    }
}
