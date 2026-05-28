package be.submanifold.pentelive.liveGameRoom;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.submanifold.pentelive.R;

/** Host-side list of arena join requesters. Each request lives 6s (auto-removed).
 *  A single 100ms ticker drives the countdown bars (avoids per-view timer leaks).
 *  Tap a row = accept; swipe (wired via ItemTouchHelper in the fragment) = reject. */
public class ArenaJoinRequestAdapter extends RecyclerView.Adapter<ArenaJoinRequestAdapter.VH> {

    private static final long TIMEOUT_MS = 6000L;
    private static final long TICK_MS = 100L;

    public interface ActionSender { void send(String event); }

    private final LayoutInflater inflater;
    private final TablesAndPlayers tablesAndPlayers;
    private final ActionSender sender;
    private final String me;
    private final int tableId;
    private final int gameId;

    private final List<String> data = new ArrayList<>();
    private final Map<String, Long> joinedAt = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ticking = false;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            boolean removed = false;
            for (int i = data.size() - 1; i >= 0; i--) {
                Long start = joinedAt.get(data.get(i));
                if (start == null || now - start >= TIMEOUT_MS) {
                    joinedAt.remove(data.get(i));
                    data.remove(i);
                    removed = true;
                }
            }
            if (removed) {
                notifyDataSetChanged();
            } else {
                notifyItemRangeChanged(0, data.size(), "progress");
            }
            if (!data.isEmpty()) {
                handler.postDelayed(this, TICK_MS);
            } else {
                ticking = false;
            }
        }
    };

    public ArenaJoinRequestAdapter(LayoutInflater inflater, TablesAndPlayers tablesAndPlayers,
                                   ActionSender sender, String me, int tableId, int gameId) {
        this.inflater = inflater;
        this.tablesAndPlayers = tablesAndPlayers;
        this.sender = sender;
        this.me = me;
        this.tableId = tableId;
        this.gameId = gameId;
    }

    public void addPlayer(String name) {
        if (!data.contains(name)) {
            data.add(name);
        }
        joinedAt.put(name, System.currentTimeMillis());
        notifyDataSetChanged();
        if (!ticking) {
            ticking = true;
            handler.postDelayed(ticker, TICK_MS);
        }
    }

    private void removeAt(int position) {
        if (position < 0 || position >= data.size()) return;
        joinedAt.remove(data.get(position));
        data.remove(position);
        notifyDataSetChanged();
    }

    public void accept(int position) {
        if (position < 0 || position >= data.size()) return;
        sender.send(ArenaEvents.accept(me, data.get(position), tableId));
        removeAt(position);
    }

    public void reject(int position) {
        if (position < 0 || position >= data.size()) return;
        sender.send(ArenaEvents.reject(me, data.get(position), tableId));
        removeAt(position);
    }

    /** Cancel everything — call from fragment onDestroyView and on dismiss. */
    public void reset() {
        handler.removeCallbacks(ticker);
        ticking = false;
        data.clear();
        joinedAt.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(inflater.inflate(R.layout.arena_join_request_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String name = data.get(position);
        LivePlayer player = tablesAndPlayers.players.get(name);
        if (player != null) {
            holder.name.setText(player.coloredNameString(holder.name.getLineHeight()));
            holder.rating.setText(player.coloredRatingSquare(player.getRating(gameId)));
        } else {
            holder.name.setText(name);
            holder.rating.setText("");
        }
        Long start = joinedAt.get(name);
        long remaining = start == null ? 0 : Math.max(0, TIMEOUT_MS - (System.currentTimeMillis() - start));
        holder.progress.setProgress((int) remaining);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView rating;
        final ProgressBar progress;
        VH(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.requestPlayerName);
            rating = itemView.findViewById(R.id.requestPlayerRating);
            progress = itemView.findViewById(R.id.requestProgress);
        }
    }
}
