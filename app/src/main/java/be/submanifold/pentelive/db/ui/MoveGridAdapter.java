package be.submanifold.pentelive.db.ui;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import be.submanifold.pentelive.R;
import be.submanifold.pentelive.db.MoveGrid;

/** Viewer move grid: one cell per stone, coloured by past / current / future. */
public final class MoveGridAdapter extends RecyclerView.Adapter<MoveGridAdapter.Holder> {
    private final IntConsumer onCellTap;
    private final List<MoveGrid.Cell> cells = new ArrayList<>();

    /** @param onCellTap called with the stone index of the tapped cell */
    public MoveGridAdapter(IntConsumer onCellTap) {
        this.onCellTap = onCellTap;
    }

    public void submit(List<MoveGrid.Cell> list) {
        cells.clear();
        cells.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_db_move_cell, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        MoveGrid.Cell c = cells.get(position);
        h.number.setText(c.number);
        h.label.setText(c.label);
        h.label.setTextColor(MoveGrid.argb(c.state));
        h.label.setTypeface(null, c.state == MoveGrid.State.CURRENT ? Typeface.BOLD : Typeface.NORMAL);
        h.itemView.setOnClickListener(v -> onCellTap.accept(c.index));
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView label;

        Holder(View v) {
            super(v);
            number = v.findViewById(R.id.cellNumber);
            label = v.findViewById(R.id.cellLabel);
        }
    }
}
