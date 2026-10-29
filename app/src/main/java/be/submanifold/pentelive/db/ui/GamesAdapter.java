package be.submanifold.pentelive.db.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import be.submanifold.pentelive.NameStyle;
import be.submanifold.pentelive.R;
import be.submanifold.pentelive.db.Crowns;
import be.submanifold.pentelive.db.DbDtos;
import be.submanifold.pentelive.db.GameRowFormat;
import be.submanifold.pentelive.db.RatingColors;

/** Games tab: list label, column header, game rows and a "Show more" footer. */
public final class GamesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int HEADER = 0;
    private static final int GAME = 1;
    private static final int FOOTER = 2;

    private final Consumer<DbDtos.GameHeader> onGameTap;
    private final Runnable onShowMore;
    private final List<DbDtos.GameHeader> games = new ArrayList<>();
    private boolean hasMore;
    private String listLabel = "";

    public GamesAdapter(Consumer<DbDtos.GameHeader> onGameTap, Runnable onShowMore) {
        this.onGameTap = onGameTap;
        this.onShowMore = onShowMore;
    }

    public void submit(List<DbDtos.GameHeader> list, boolean hasMore, String listLabel) {
        games.clear();
        games.addAll(list);
        this.hasMore = hasMore;
        this.listLabel = listLabel == null ? "" : listLabel;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return HEADER;
        return position <= games.size() ? GAME : FOOTER;
    }

    @Override
    public int getItemCount() {
        return 1 + games.size() + (hasMore ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater in = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case HEADER:
                return new HeaderHolder(in.inflate(R.layout.item_db_games_header, parent, false));
            case GAME:
                return new GameHolder(in.inflate(R.layout.item_db_game, parent, false));
            default:
                return new FooterHolder(in.inflate(R.layout.item_db_games_footer, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).label.setText(listLabel);
        } else if (holder instanceof GameHolder) {
            bindGame((GameHolder) holder, games.get(position - 1));
        } else {
            ((FooterHolder) holder).button.setOnClickListener(v -> onShowMore.run());
        }
    }

    private void bindGame(GameHolder h, DbDtos.GameHeader g) {
        Context c = h.itemView.getContext();
        bindPlayer(c, h.p1Name, h.p1Rating, g.player1);
        bindPlayer(c, h.p2Name, h.p2Rating, g.player2);
        h.score.setText(GameRowFormat.score(g.winner));
        h.date.setText(GameRowFormat.date(g.playDate, ZonedDateTime.now(), Locale.getDefault()));
        h.itemView.setOnClickListener(v -> onGameTap.accept(g));
    }

    private static void bindPlayer(Context c, TextView nameView, TextView ratingView, DbDtos.Player p) {
        if (p == null || p.name == null) {
            nameView.setText("?");
            ratingView.setText("");
            return;
        }
        SpannableStringBuilder name = new SpannableStringBuilder(p.name);
        name.setSpan(new ForegroundColorSpan(GameRowFormat.nameArgb(p.nameColor)), 0, name.length(),
                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        if (GameRowFormat.nameBold(p.nameColor)) {
            name.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        NameStyle.appendCrown(c, name, Crowns.safeCode(p.crown), nameView);
        nameView.setText(name);

        SpannableStringBuilder rating = new SpannableStringBuilder("■");
        rating.setSpan(new ForegroundColorSpan(RatingColors.argb(p.rating)), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        rating.append(' ').append(String.valueOf(p.rating));
        ratingView.setText(rating);
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView label;

        HeaderHolder(View v) {
            super(v);
            label = v.findViewById(R.id.gamesListLabel);
        }
    }

    static final class GameHolder extends RecyclerView.ViewHolder {
        final TextView p1Name, p1Rating, p2Name, p2Rating, score, date;

        GameHolder(View v) {
            super(v);
            p1Name = v.findViewById(R.id.gameP1Name);
            p1Rating = v.findViewById(R.id.gameP1Rating);
            p2Name = v.findViewById(R.id.gameP2Name);
            p2Rating = v.findViewById(R.id.gameP2Rating);
            score = v.findViewById(R.id.gameScore);
            date = v.findViewById(R.id.gameDate);
        }
    }

    static final class FooterHolder extends RecyclerView.ViewHolder {
        final View button;

        FooterHolder(View v) {
            super(v);
            button = v.findViewById(R.id.gamesShowMore);
        }
    }
}
