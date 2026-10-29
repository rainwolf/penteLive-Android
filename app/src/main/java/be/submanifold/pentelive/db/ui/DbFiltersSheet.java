package be.submanifold.pentelive.db.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import be.submanifold.pentelive.R;
import be.submanifold.pentelive.db.AuthedDbTransport;
import be.submanifold.pentelive.db.DbApi;
import be.submanifold.pentelive.db.DbCapabilities;
import be.submanifold.pentelive.db.DbFilters;
import be.submanifold.pentelive.db.DbGames;
import be.submanifold.pentelive.db.DbResult;
import be.submanifold.pentelive.db.LatestRequest;

/**
 * The one filters sheet (spec 3.8). It edits a draft and hands it back through a FragmentResult;
 * nothing is applied or saved until Apply. Reset only resets the draft.
 */
public class DbFiltersSheet extends BottomSheetDialogFragment {
    public static final String TAG = "db_filters_sheet";
    /** FragmentResult key; the bundle holds one string, {@link DbFilters#toJson()}, under {@link #RESULT_JSON}. */
    public static final String RESULT_KEY = "db_filters";
    public static final String RESULT_JSON = "filters";

    private static final String ARG_APPLIED = "applied";
    private static final String STATE_DRAFT = "draft";
    private static final int MIN_NAME_CHARS = 2;
    private static final long DEBOUNCE_MS = 250;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private DbApi api;
    private DbViewModel model;

    private ChipGroup gameChips;
    private MaterialSwitch speed, eitherSeat, excludeTimeouts;
    private MaterialAutoCompleteTextView p1Name, p2Name, p1Rating, p2Rating;
    private MaterialButtonToggleGroup winner, live, source;
    private TextInputEditText after, before;
    private int[] ratingValues;
    private int p1RatingIndex, p2RatingIndex;
    /** True while populate() sets the name fields, so loading a draft does not fire a player search. */
    private boolean populating;

    public static DbFiltersSheet newInstance(DbFilters applied) {
        DbFiltersSheet sheet = new DbFiltersSheet();
        Bundle args = new Bundle();
        args.putString(ARG_APPLIED, applied.toJson());
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_PenteDb_FiltersSheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // Open fully: the sheet is a form, and the keyboard must not hide Apply.
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_db_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        model = new ViewModelProvider(requireActivity()).get(DbViewModel.class);
        api = new DbApi(new AuthedDbTransport());

        gameChips = v.findViewById(R.id.filterGameChips);
        speed = v.findViewById(R.id.filterSpeed);
        eitherSeat = v.findViewById(R.id.filterEitherSeat);
        excludeTimeouts = v.findViewById(R.id.filterExcludeTimeouts);
        p1Name = v.findViewById(R.id.filterP1Name);
        p2Name = v.findViewById(R.id.filterP2Name);
        p1Rating = v.findViewById(R.id.filterP1Rating);
        p2Rating = v.findViewById(R.id.filterP2Rating);
        winner = v.findViewById(R.id.filterWinner);
        live = v.findViewById(R.id.filterLive);
        source = v.findViewById(R.id.filterSource);
        after = v.findViewById(R.id.filterAfter);
        before = v.findViewById(R.id.filterBefore);

        // The timeout and live/turn-based filters are not on the server yet (B3/B4): hide the controls.
        if (!DbCapabilities.SERVER_FILTERS_B3_B4) {
            excludeTimeouts.setVisibility(View.GONE);
            v.findViewById(R.id.filterLiveLabel).setVisibility(View.GONE);
            live.setVisibility(View.GONE);
        }

        for (int i = 0; i < DbGames.BASE_NAMES.size(); i++) {
            String name = DbGames.BASE_NAMES.get(i);
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_db_filter_chip, gameChips, false);
            chip.setId(View.generateViewId());
            chip.setText(name);
            chip.setTag(DbGames.baseId(name));
            gameChips.addView(chip);
        }
        gameChips.setOnCheckedStateChangeListener((group, ids) -> updateSpeedEnabled());

        setUpRatings();
        setUpNameField(p1Name);
        setUpNameField(p2Name);
        setUpDate(after);
        setUpDate(before);

        v.findViewById(R.id.filterReset).setOnClickListener(b -> populate(DbFilters.defaults()));
        v.findViewById(R.id.filterApply).setOnClickListener(b -> apply());

        String json = savedInstanceState != null ? savedInstanceState.getString(STATE_DRAFT) : null;
        if (json == null) json = requireArguments().getString(ARG_APPLIED);
        populate(DbFilters.fromJson(json));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (gameChips != null) outState.putString(STATE_DRAFT, readDraft().toJson());
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    // Draft <-> views

    private void populate(DbFilters f) {
        populating = true;
        for (int i = 0; i < gameChips.getChildCount(); i++) {
            Chip chip = (Chip) gameChips.getChildAt(i);
            chip.setChecked(((Integer) chip.getTag()) == f.baseGameId);
        }
        speed.setChecked(f.speed && DbGames.speedAllowed(f.baseGameId));
        updateSpeedEnabled();
        setName(p1Name, f.player1Name);
        setName(p2Name, f.player2Name);
        p1RatingIndex = indexOfRating(f.player1MinRating);
        p2RatingIndex = indexOfRating(f.player2MinRating);
        setRatingText(p1Rating, p1RatingIndex);
        setRatingText(p2Rating, p2RatingIndex);
        eitherSeat.setChecked(f.eitherSeat);
        excludeTimeouts.setChecked(f.excludeTimeouts);
        winner.check(f.winner == 1 ? R.id.filterWinnerP1 : f.winner == 2 ? R.id.filterWinnerP2 : R.id.filterWinnerEither);
        live.check(DbFilters.LIVE_ONLY.equals(f.liveOrTurnBased) ? R.id.filterLiveLive
                : DbFilters.TURN_BASED_ONLY.equals(f.liveOrTurnBased) ? R.id.filterLiveTb : R.id.filterLiveAll);
        after.setText(f.afterDate);
        before.setText(f.beforeDate);
        source.check(DbFilters.SCOPE_MINE.equals(f.scope) ? R.id.filterSourceMine
                : DbFilters.SCOPE_BOTH.equals(f.scope) ? R.id.filterSourceBoth : R.id.filterSourceArchive);
        populating = false;
    }

    private DbFilters readDraft() {
        int base = 1;
        int checked = gameChips.getCheckedChipId();
        View chip = checked == View.NO_ID ? null : gameChips.findViewById(checked);
        if (chip != null) base = (Integer) chip.getTag();
        int w = winner.getCheckedButtonId();
        int l = live.getCheckedButtonId();
        int s = source.getCheckedButtonId();
        return DbFilters.defaults().toBuilder()
                .baseGameId(base)
                .speed(speed.isChecked() && DbGames.speedAllowed(base))
                .player1Name(text(p1Name))
                .player2Name(text(p2Name))
                .player1MinRating(ratingValues[p1RatingIndex])
                .player2MinRating(ratingValues[p2RatingIndex])
                .eitherSeat(eitherSeat.isChecked())
                .excludeTimeouts(DbCapabilities.SERVER_FILTERS_B3_B4 && excludeTimeouts.isChecked())
                .winner(w == R.id.filterWinnerP1 ? 1 : w == R.id.filterWinnerP2 ? 2 : 0)
                .liveOrTurnBased(!DbCapabilities.SERVER_FILTERS_B3_B4 ? DbFilters.LIVE_ALL
                        : l == R.id.filterLiveLive ? DbFilters.LIVE_ONLY
                        : l == R.id.filterLiveTb ? DbFilters.TURN_BASED_ONLY : DbFilters.LIVE_ALL)
                .afterDate(emptyToNull(text(after)))
                .beforeDate(emptyToNull(text(before)))
                .scope(s == R.id.filterSourceMine ? DbFilters.SCOPE_MINE
                        : s == R.id.filterSourceBoth ? DbFilters.SCOPE_BOTH : DbFilters.SCOPE_ARCHIVE)
                .build();
    }

    private void apply() {
        Bundle result = new Bundle();
        result.putString(RESULT_JSON, readDraft().toJson());
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    private void updateSpeedEnabled() {
        int checked = gameChips.getCheckedChipId();
        View chip = checked == View.NO_ID ? null : gameChips.findViewById(checked);
        boolean allowed = chip != null && DbGames.speedAllowed((Integer) chip.getTag());
        if (!allowed) speed.setChecked(false);
        speed.setEnabled(allowed);
    }

    // Ratings

    private void setUpRatings() {
        String[] raw = getResources().getStringArray(R.array.db_ratings_array);
        ratingValues = new int[raw.length];
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < raw.length; i++) {
            ratingValues[i] = Integer.parseInt(raw[i]);
            labels.add(ratingValues[i] <= 0 ? getString(R.string.db_filters_rating_any)
                    : getString(R.string.db_filters_rating_min, ratingValues[i]));
        }
        p1Rating.setSimpleItems(labels.toArray(new String[0]));
        p2Rating.setSimpleItems(labels.toArray(new String[0]));
        p1Rating.setOnItemClickListener((parent, view, pos, id) -> p1RatingIndex = pos);
        p2Rating.setOnItemClickListener((parent, view, pos, id) -> p2RatingIndex = pos);
    }

    private int indexOfRating(int value) {
        for (int i = 0; i < ratingValues.length; i++) {
            if (ratingValues[i] == value) return i;
        }
        return 0;
    }

    private void setRatingText(MaterialAutoCompleteTextView field, int index) {
        // filter=false keeps the full list in the dropdown after the text is set.
        field.setText(ratingValues[index] <= 0 ? getString(R.string.db_filters_rating_any)
                : getString(R.string.db_filters_rating_min, ratingValues[index]), false);
    }

    // Name autocomplete: from 2 characters, 250 ms debounce, stale replies dropped.

    private void setUpNameField(MaterialAutoCompleteTextView field) {
        final LatestRequest latest = new LatestRequest();
        final NoFilterAdapter adapter = new NoFilterAdapter(requireContext());
        field.setAdapter(adapter);
        final Runnable[] pendingHolder = new Runnable[1];
        // Picking a suggestion: drop the query its text change started and any reply in flight.
        field.setOnItemClickListener((parent, view, pos, id) -> {
            if (pendingHolder[0] != null) handler.removeCallbacks(pendingHolder[0]);
            latest.next();
        });
        field.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (pendingHolder[0] != null) handler.removeCallbacks(pendingHolder[0]);
                final String q = s.toString().trim();
                final int id = latest.next();
                if (populating || q.length() < MIN_NAME_CHARS) {
                    adapter.setItems(new ArrayList<String>());
                    return;
                }
                Runnable pending = () -> model.io().execute(() -> {
                    DbResult<List<String>> r = api.players(q);
                    model.main().execute(() -> {
                        if (!latest.isLatest(id) || !isAdded() || getView() == null) return;
                        if (r.kind != DbResult.Kind.OK || r.value == null || r.value.isEmpty()) {
                            adapter.setItems(new ArrayList<String>());
                            return;
                        }
                        adapter.setItems(r.value);
                        if (field.hasFocus()) field.showDropDown();
                    });
                });
                pendingHolder[0] = pending;
                handler.postDelayed(pending, DEBOUNCE_MS);
            }
        });
    }

    private void setName(MaterialAutoCompleteTextView field, @Nullable String name) {
        // filter=false: setting the text must not open a suggestions list.
        field.setText(name == null ? "" : name, false);
    }

    /** Shows exactly what the server returned (a normal ArrayAdapter would re-filter it by prefix). */
    private static final class NoFilterAdapter extends ArrayAdapter<String> {
        private final List<String> items = new ArrayList<>();

        NoFilterAdapter(android.content.Context ctx) {
            super(ctx, android.R.layout.simple_dropdown_item_1line);
        }

        void setItems(List<String> next) {
            items.clear();
            items.addAll(next);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public String getItem(int position) {
            return items.get(position);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults r = new FilterResults();
                    r.values = items;
                    r.count = items.size();
                    return r;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }
            };
        }
    }

    // Dates (yyyy-MM-dd, picked in UTC so the day never shifts)

    private void setUpDate(TextInputEditText field) {
        field.setOnClickListener(v -> {
            MaterialDatePicker.Builder<Long> b = MaterialDatePicker.Builder.datePicker();
            Long current = parseDate(text(field));
            if (current != null) b.setSelection(current);
            MaterialDatePicker<Long> picker = b.build();
            picker.addOnPositiveButtonClickListener(sel -> field.setText(formatDate(sel)));
            picker.show(getChildFragmentManager(), "db_filters_date_" + field.getId());
        });
    }

    private static SimpleDateFormat dateFormat() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        f.setLenient(false);
        return f;
    }

    private static String formatDate(long utcMillis) {
        return dateFormat().format(new Date(utcMillis));
    }

    @Nullable
    private static Long parseDate(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            return dateFormat().parse(s).getTime();
        } catch (ParseException e) {
            return null;
        }
    }

    private static String text(TextView v) {
        CharSequence t = v.getText();
        return t == null ? "" : t.toString().trim();
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }
}
