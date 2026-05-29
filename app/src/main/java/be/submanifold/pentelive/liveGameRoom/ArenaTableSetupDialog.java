package be.submanifold.pentelive.liveGameRoom;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import be.submanifold.pentelive.R;

/** Arena create-table form. Mirrors iOS ArenaTableSetupView; emits dsgArenaCreateTableEvent. */
public class ArenaTableSetupDialog {

    public static void show(final LiveGameRoomActivity activity, final String me) {
        View view = activity.getLayoutInflater().inflate(R.layout.arena_table_settings, null);

        final Spinner gameSpinner = view.findViewById(R.id.arenaGameSpinner);
        ArrayAdapter<CharSequence> gameAdapter = ArrayAdapter.createFromResource(activity,
                R.array.game_types_array, android.R.layout.simple_spinner_item);
        gameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameSpinner.setAdapter(gameAdapter);

        final TextView ratedChoice = view.findViewById(R.id.arenaRatedChoice);
        final TextView playAsChoice = view.findViewById(R.id.arenaPlayAsChoice);
        final TextView timedChoice = view.findViewById(R.id.arenaTimedChoice);
        final LinearLayout playAsRow = view.findViewById(R.id.arenaPlayAsRow);
        final LinearLayout timedFields = view.findViewById(R.id.arenaTimedFields);
        final EditText initialMinutes = view.findViewById(R.id.arenaInitialMinutes);
        final EditText incrementalSeconds = view.findViewById(R.id.arenaIncrementalSeconds);

        ratedChoice.setOnClickListener(v -> {
            if (me.startsWith("guest")) {
                return; // guests can only create unrated tables
            }
            boolean nowRated = ratedChoice.getText().toString().equals(activity.getString(R.string.no));
            ratedChoice.setText(activity.getString(nowRated ? R.string.yes : R.string.no));
            // play-as only matters for unrated games
            playAsRow.setVisibility(nowRated ? View.GONE : View.VISIBLE);
        });

        playAsChoice.setOnClickListener(v -> {
            boolean white = playAsChoice.getText().toString().equals(activity.getString(R.string.arena_white));
            playAsChoice.setText(activity.getString(white ? R.string.arena_black : R.string.arena_white));
        });

        timedChoice.setOnClickListener(v -> {
            boolean nowTimed = timedChoice.getText().toString().equals(activity.getString(R.string.no));
            timedChoice.setText(activity.getString(nowTimed ? R.string.yes : R.string.no));
            timedFields.setVisibility(nowTimed ? View.VISIBLE : View.GONE);
        });

        // Default to a timed table.
        timedChoice.setText(activity.getString(R.string.yes));
        timedFields.setVisibility(View.VISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.arena_create_table));
        builder.setView(view);
        builder.setPositiveButton(activity.getString(R.string.arena_create_button), (dialog, which) -> {
            boolean rated = ratedChoice.getText().toString().equals(activity.getString(R.string.yes));
            boolean timed = timedChoice.getText().toString().equals(activity.getString(R.string.yes));
            int game = gameSpinner.getSelectedItemPosition() * 2 + 1;
            int playAs = playAsChoice.getText().toString().equals(activity.getString(R.string.arena_black)) ? 2 : 1;
            int minutes = parseOrZero(initialMinutes.getText().toString());
            int seconds = parseOrZero(incrementalSeconds.getText().toString());
            activity.sendEvent(ArenaEvents.createTable(timed, minutes, seconds, rated, game, playAs, me));
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }

    private static int parseOrZero(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
