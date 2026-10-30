package be.submanifold.pentelive.db.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import be.submanifold.pentelive.R;
import be.submanifold.pentelive.db.DbController;
import be.submanifold.pentelive.db.MoveLabel;
import be.submanifold.pentelive.db.MoveRowFormat;

/** Rows of the Moves sheet: dot, label, win% bar (heat colour), pct, games. A tap selects the row. */
public final class MovesAdapter extends RecyclerView.Adapter<MovesAdapter.Holder> {
    private final Consumer<Integer> onRowTap;
    private int gridSize = 19;
    private final List<DbController.MoveRow> rows = new ArrayList<>();
    private Integer selectedCell;

    public MovesAdapter(Consumer<Integer> onRowTap) {
        this.onRowTap = onRowTap;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    /** Highlights the row for cell (null = none). */
    public void setSelected(Integer cell) {
        if (java.util.Objects.equals(selectedCell, cell)) return;
        selectedCell = cell;
        notifyDataSetChanged();
    }

    /** The heat colour of cell's row, or null when the cell is not listed. */
    public Integer argbOf(int cell) {
        for (DbController.MoveRow r : rows) {
            if (r.move.move == cell) return r.argb;
        }
        return null;
    }

    public void submit(List<DbController.MoveRow> list) {
        rows.clear();
        rows.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_db_move, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        DbController.MoveRow row = rows.get(position);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(row.argb);
        h.dot.setBackground(dot);
        h.label.setText(MoveLabel.label(row.move.move, gridSize));
        float pct = (float) Math.max(0, Math.min(100, row.move.winPct));
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) h.bar.getLayoutParams();
        lp.weight = pct;
        h.bar.setLayoutParams(lp);
        // The bar wears the move's heat colour, the same one the board dot has.
        h.bar.setBackgroundColor(row.argb);
        h.pct.setText(MoveRowFormat.pct(row.move.winPct));
        h.games.setText(MoveRowFormat.games(row.move.games, Locale.getDefault()));
        boolean selected = selectedCell != null && selectedCell == row.move.move;
        h.itemView.setSelected(selected);
        h.itemView.setOnClickListener(v -> onRowTap.accept(row.move.move));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View dot;
        final TextView label;
        final View bar;
        final TextView pct;
        final TextView games;

        Holder(View v) {
            super(v);
            dot = v.findViewById(R.id.moveDot);
            label = v.findViewById(R.id.moveLabel);
            bar = v.findViewById(R.id.moveBar);
            pct = v.findViewById(R.id.movePct);
            games = v.findViewById(R.id.moveGames);
        }
    }
}
