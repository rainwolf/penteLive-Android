# Android Arena Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the iOS PR #4 "arena" feature to the Android app at full functional parity — an arena room where a host creates a table, players send join *requests*, and the host accepts/rejects them from a live list with a 6-second per-request countdown.

**Architecture:** Mirror the iOS implementation inline (Approach A). Add `isArena`/`isArenaTable` flags to the existing room/table classes, two new focused UI units (a join-request `RecyclerView` adapter and an arena create-table dialog), a small testable `ArenaEvents` JSON-builder helper, and one new branch in the central event-dispatch chain. No new Activity.

**Tech Stack:** Java, Android SDK (minSdk 26), AndroidX AppCompat/Material/RecyclerView, raw-JSON-string socket protocol via `LiveGameRoomActivity.sendEvent(String)`, JUnit 4 for the helper unit tests, Gradle (`./gradlew assembleDebug`).

**Spec:** `docs/superpowers/specs/2026-05-28-android-arena-port-design.md`

---

## Verification approach (read first)

This codebase has no UI/socket test harness; the app is socket + JNI + Views. Per the agreed bar, verification is:
- **Real unit tests** only for the pure JSON builders (`ArenaEvents`) — Task 1.
- **Compile gate** after every code task: `./gradlew assembleDebug` must succeed.
- **Emulator smoke test** at the end (Task 10) with `PentePlayer.development = true` (connects to the local backend).

`RecyclerView` is not yet a dependency. Task 4 adds it.

---

## File structure

**New files**
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEvents.java` — static builders for the 4 arena JSON events (single source of the protocol, incl. the two divergences from iOS).
- `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventsTest.java` — JUnit tests for `ArenaEvents`.
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequestAdapter.java` — host's join-request list (RecyclerView.Adapter) with a single 100 ms ticker driving per-row countdown bars; tap = accept, swipe = reject.
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaTableSetupDialog.java` — builds + shows the arena create-table dialog, emits `dsgArenaCreateTableEvent`.
- `app/src/main/res/layout/arena_join_request_row.xml` — one request row: name + rating + horizontal progress bar.
- `app/src/main/res/layout/arena_table_settings.xml` — arena create-table form.

**Modified files**
- `app/build.gradle` — add `recyclerview` dependency.
- `app/src/main/res/values/strings.xml` and `app/src/main/res/values-de/strings.xml` — new arena strings.
- `app/src/main/java/.../liveGameRoom/TableListAdapter.java` — arena ≤1-player filter + route the "+" button to the arena create dialog.
- `app/src/main/java/.../liveGameRoom/LiveGameRoomFragment.java` — arena tables-only UI; table tap sends request-join.
- `app/src/main/java/.../liveGameRoom/LiveTableFragment.java` — own the join-request adapter; show while host alone, dismiss on opponent join; `arenaTableRequestJoinEvent`; lifecycle cleanup.
- `app/src/main/java/.../liveGameRoom/LiveGameRoomActivity.java` — `isArena` field; set `isArenaTable` on new table fragments; new dispatch branch; dismiss-on-join.

**Key facts (verified in code)**
- `LiveGameRoomActivity`: `getMe()` (L66), `room = getIntent().getParcelableExtra("room")` (L99), room fragment built with `room.getName()` (L107), `sendEvent(String)` (L410), `public TablesAndPlayers tablesAndPlayers`, join branch (L265-280), fragment lookup `getSupportFragmentManager().findFragmentByTag("liveTable")`.
- `TablesAndPlayers`: `public Map<String, LivePlayer> players`.
- `LivePlayer`: `getName()`, `getRating(int game)`, `coloredNameString(int height)`, `coloredRatingSquare(int rating)`.
- `Table`: `getId()`, `getGame()`, `isTimed()`, `isRated()`, `getTimer().get("initialMinutes")`, `getPlayers()` (Map). Game spinner index = `(game-1)/2`; game int = `position*2+1`.
- `LiveTableFragment`: package-private `Table table`, `me = activity.getMe()` (L211), `setTable(Table)` (L419), settings dialog `initializeSettingsView()` (L662) using `R.array.game_types_array`.
- `TableListAdapter`: `tablesArray = new ArrayList<>(tables.values())` in ctor (L39) and `updateList()` (L148); `newTableButton` click sends join -1 (L93); has `roomName` + `activity`.

---

## Task 1: `ArenaEvents` JSON builders (TDD)

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEvents.java`
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventsTest.java`

- [ ] **Step 1: Write the failing test**

```java
package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ArenaEventsTest {

    @Test
    public void createTable_buildsExpectedJson() {
        String json = ArenaEvents.createTable(true, 5, 3, false, 1, 2, "alice");
        assertEquals(
            "{\"dsgArenaCreateTableEvent\":{\"timed\":true,\"initialMinutes\":5,"
            + "\"incrementalSeconds\":3,\"rated\":false,\"game\":1,\"playAs\":2,"
            + "\"player\":\"alice\",\"table\":-1,\"time\":0}}",
            json);
    }

    @Test
    public void requestJoin_includesPlayerKey() {
        assertEquals(
            "{\"dsgArenaRequestJoinTableEvent\":{\"player\":\"bob\",\"table\":42,\"time\":0}}",
            ArenaEvents.requestJoin("bob", 42));
    }

    @Test
    public void accept_buildsExpectedJson() {
        assertEquals(
            "{\"dsgArenaAcceptTableJoinEvent\":{\"player\":\"alice\",\"playerToAccept\":\"bob\",\"table\":42}}",
            ArenaEvents.accept("alice", "bob", 42));
    }

    @Test
    public void reject_usesCapitalDsgAndCorrectedMessageKey() {
        assertEquals(
            "{\"DSGArenaRejectTableJoinEvent\":{\"player\":\"alice\",\"playerToReject\":\"bob\",\"table\":42,\"message\":null}}",
            ArenaEvents.reject("alice", "bob", 42));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaEventsTest"`
Expected: FAIL — `ArenaEvents` does not exist / cannot resolve symbol.

- [ ] **Step 3: Write minimal implementation**

```java
package be.submanifold.pentelive.liveGameRoom;

/** Builds the raw-JSON-string arena protocol events sent via LiveGameRoomActivity.sendEvent.
 *  NOTE: diverges from iOS PR #4 on purpose — requestJoin adds "player"; reject uses
 *  "message" (iOS had "mesage"). The "DSG" capitalization on reject matches the backend. */
public final class ArenaEvents {
    private ArenaEvents() {}

    public static String createTable(boolean timed, int initialMinutes, int incrementalSeconds,
                                     boolean rated, int game, int playAs, String player) {
        return "{\"dsgArenaCreateTableEvent\":{\"timed\":" + timed
                + ",\"initialMinutes\":" + initialMinutes
                + ",\"incrementalSeconds\":" + incrementalSeconds
                + ",\"rated\":" + rated
                + ",\"game\":" + game
                + ",\"playAs\":" + playAs
                + ",\"player\":\"" + player + "\""
                + ",\"table\":-1,\"time\":0}}";
    }

    public static String requestJoin(String player, int tableId) {
        return "{\"dsgArenaRequestJoinTableEvent\":{\"player\":\"" + player
                + "\",\"table\":" + tableId + ",\"time\":0}}";
    }

    public static String accept(String player, String playerToAccept, int tableId) {
        return "{\"dsgArenaAcceptTableJoinEvent\":{\"player\":\"" + player
                + "\",\"playerToAccept\":\"" + playerToAccept
                + "\",\"table\":" + tableId + "}}";
    }

    public static String reject(String player, String playerToReject, int tableId) {
        return "{\"DSGArenaRejectTableJoinEvent\":{\"player\":\"" + player
                + "\",\"playerToReject\":\"" + playerToReject
                + "\",\"table\":" + tableId + ",\"message\":null}}";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaEventsTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEvents.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventsTest.java
git commit -m "feat(arena): add ArenaEvents JSON builders with tests"
```

---

## Task 2: String resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Add English strings**

Add inside the `<resources>` element of `app/src/main/res/values/strings.xml`:

```xml
<string name="arena_create_table">Create arena table</string>
<string name="arena_create_button">Create table</string>
<string name="arena_reject">Reject</string>
<string name="arena_tap_to_accept">Tap player to accept</string>
<string name="arena_play_as">Play as:</string>
<string name="arena_white">white</string>
<string name="arena_black">black</string>
<string name="arena_game">Game:</string>
<string name="arena_rated">Rated:</string>
<string name="arena_timed">Timed:</string>
<string name="arena_initial_minutes">Initial minutes:</string>
<string name="arena_incremental_seconds">Incremental seconds:</string>
<string name="arena_waiting">Waiting for host to accept…</string>
```

- [ ] **Step 2: Add German strings**

Add inside the `<resources>` element of `app/src/main/res/values-de/strings.xml`:

```xml
<string name="arena_create_table">Arena-Tisch erstellen</string>
<string name="arena_create_button">Tisch erstellen</string>
<string name="arena_reject">Ablehnen</string>
<string name="arena_tap_to_accept">Tippe auf Spieler zum Akzeptieren</string>
<string name="arena_play_as">Spielen als:</string>
<string name="arena_white">weiß</string>
<string name="arena_black">schwarz</string>
<string name="arena_game">Spiel:</string>
<string name="arena_rated">Gewertet:</string>
<string name="arena_timed">Mit Zeit:</string>
<string name="arena_initial_minutes">Startminuten:</string>
<string name="arena_incremental_seconds">Inkrementsekunden:</string>
<string name="arena_waiting">Warte auf Bestätigung des Gastgebers…</string>
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (resources compile).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(arena): add arena string resources (en, de)"
```

---

## Task 3: Join-request row layout

**Files:**
- Create: `app/src/main/res/layout/arena_join_request_row.xml`

- [ ] **Step 1: Create the layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:paddingHorizontal="16dp"
    android:paddingVertical="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/requestPlayerName"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textSize="16sp" />

        <TextView
            android:id="@+id/requestPlayerRating"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp" />
    </LinearLayout>

    <ProgressBar
        android:id="@+id/requestProgress"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="4dp"
        android:layout_marginTop="6dp"
        android:max="6000"
        android:progress="6000" />
</LinearLayout>
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/arena_join_request_row.xml
git commit -m "feat(arena): add join-request row layout"
```

---

## Task 4: `ArenaJoinRequestAdapter`

**Files:**
- Modify: `app/build.gradle` (add RecyclerView)
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequestAdapter.java`

- [ ] **Step 1: Add the RecyclerView dependency**

In `app/build.gradle`, inside the `dependencies { ... }` block, add after the `swiperefreshlayout` line (L72):

```groovy
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

- [ ] **Step 2: Create the adapter**

```java
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
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (RecyclerView resolves; adapter compiles).

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequestAdapter.java
git commit -m "feat(arena): add join-request adapter with countdown"
```

---

## Task 5: Arena create-table form + dialog

**Files:**
- Create: `app/src/main/res/layout/arena_table_settings.xml`
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaTableSetupDialog.java`

- [ ] **Step 1: Create the settings layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
        android:text="@string/arena_game" />
    <Spinner android:id="@+id/arenaGameSpinner"
        android:layout_width="match_parent" android:layout_height="wrap_content" />

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_marginTop="12dp">
        <TextView android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:text="@string/arena_rated" />
        <TextView android:id="@+id/arenaRatedChoice"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/no" />
    </LinearLayout>

    <LinearLayout android:id="@+id/arenaPlayAsRow"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_marginTop="12dp">
        <TextView android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:text="@string/arena_play_as" />
        <TextView android:id="@+id/arenaPlayAsChoice"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/arena_white" />
    </LinearLayout>

    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="horizontal" android:layout_marginTop="12dp">
        <TextView android:layout_width="0dp" android:layout_height="wrap_content"
            android:layout_weight="1" android:text="@string/arena_timed" />
        <TextView android:id="@+id/arenaTimedChoice"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/no" />
    </LinearLayout>

    <LinearLayout android:id="@+id/arenaTimedFields"
        android:layout_width="match_parent" android:layout_height="wrap_content"
        android:orientation="vertical" android:visibility="gone">
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/arena_initial_minutes" android:layout_marginTop="12dp" />
        <EditText android:id="@+id/arenaInitialMinutes"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:inputType="number" android:text="5" />
        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="@string/arena_incremental_seconds" android:layout_marginTop="12dp" />
        <EditText android:id="@+id/arenaIncrementalSeconds"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:inputType="number" android:text="0" />
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 2: Create the dialog class**

```java
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
        if (me.startsWith("guest")) {
            return; // guests cannot create arena tables
        }
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
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/arena_table_settings.xml app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaTableSetupDialog.java
git commit -m "feat(arena): add create-table form and dialog"
```

---

## Task 6: `TableListAdapter` — arena filter + "+" routing

**Files:**
- Modify: `app/src/main/java/.../liveGameRoom/TableListAdapter.java`

- [ ] **Step 1: Add an arena flag and a filtered rebuild**

Replace the constructor (L37-43) so it computes `isArena` from `roomName`:

```java
    private final boolean isArena;

    public TableListAdapter(Map<Integer, Table> tables, String roomName, LiveGameRoomActivity activity) {
        this.tables = tables;
        this.roomName = roomName;
        this.activity = activity;
        ctx = MyApplication.getContext();
        this.isArena = roomName != null && roomName.toLowerCase().contains("arena");
        this.tablesArray = buildTablesArray();
    }

    private List<Table> buildTablesArray() {
        List<Table> all = new ArrayList<>(tables.values());
        if (!isArena) {
            return all;
        }
        List<Table> open = new ArrayList<>();
        for (Table t : all) {
            if (t.getPlayers().size() <= 1) {   // arena shows only open tables
                open.add(t);
            }
        }
        return open;
    }
```

- [ ] **Step 2: Use the filtered list in `updateList()`**

Replace the body of `updateList()` (L147-150):

```java
    public void updateList() {
        this.tablesArray = buildTablesArray();
        notifyDataSetChanged();
    }
```

- [ ] **Step 3: Route the "+" button to the arena dialog**

In `getGroupView`, replace the `newTableButton` click handler (L93):

```java
        convertView.findViewById(R.id.newTableButton).setOnClickListener(view -> {
            if (isArena) {
                ArenaTableSetupDialog.show(activity, activity.getMe());
            } else {
                activity.sendEvent("{\"dsgJoinTableEvent\":{\"table\":-1,\"time\":0}}");
            }
        });
```

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/TableListAdapter.java
git commit -m "feat(arena): filter to open tables and route + to arena dialog"
```

---

## Task 7: `LiveGameRoomFragment` — arena tables-only + request-join

**Files:**
- Modify: `app/src/main/java/.../liveGameRoom/LiveGameRoomFragment.java`

- [ ] **Step 1: Add an arena flag derived from the room name**

In `onViewCreated`, immediately after `activity = (LiveGameRoomActivity) getActivity();` (L98), add:

```java
        boolean isArena = roomName != null && roomName.toLowerCase().contains("arena");
        if (isArena) {
            getView().findViewById(R.id.segments).setVisibility(View.GONE);
            getView().findViewById(R.id.playersList).setVisibility(View.GONE);
            getView().findViewById(R.id.tablesList).setVisibility(View.VISIBLE);
        }
```

- [ ] **Step 2: Send request-join instead of join on table tap**

Replace the table-list child click handler (L112-116) so arena rooms request to join:

```java
        expandableList.setOnChildClickListener((expandableListView, view12, i, i1, l) -> {
            int tableId = tableListAdapter.getTablesArray().get(i1).getId();
            if (roomName != null && roomName.toLowerCase().contains("arena")) {
                activity.sendEvent(ArenaEvents.requestJoin(activity.getMe(), tableId));
                android.widget.Toast.makeText(activity, getString(R.string.arena_waiting),
                        android.widget.Toast.LENGTH_SHORT).show();
            } else {
                activity.sendEvent("{\"dsgJoinTableEvent\":{\"table\":" + tableId + ",\"time\":0}}");
            }
            return true;
        });
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomFragment.java
git commit -m "feat(arena): tables-only room UI and request-join on tap"
```

---

## Task 8: `LiveTableFragment` — own the request list

**Files:**
- Modify: `app/src/main/java/.../liveGameRoom/LiveTableFragment.java`

- [ ] **Step 1: Add arena fields**

After the field declarations block (after L91, near `AlertDialog tableSettingsWindow;`), add:

```java
    public boolean isArenaTable = false;
    private ArenaJoinRequestAdapter arenaAdapter;
    private AlertDialog arenaRequestDialog;

    public void setArena(boolean isArena) {
        this.isArenaTable = isArena;
    }
```

- [ ] **Step 2: Build the adapter once `me`/`table` are known**

In `onViewCreated`, immediately after `me = activity.getMe();` (L211), add:

```java
        if (isArenaTable) {
            arenaAdapter = new ArenaJoinRequestAdapter(
                    activity.getLayoutInflater(),
                    activity.tablesAndPlayers,
                    activity::sendEvent,
                    me, table.getId(), table.getGame());
            if (table.getPlayers().size() <= 1) {
                showArenaJoinRequest();
            }
        }
```

- [ ] **Step 3: Add show/dismiss + the incoming-request entry point**

Add these methods to the class (anywhere among the other methods, e.g. after `initializeSettingsView()` at ~L736):

```java
    public void showArenaJoinRequest() {
        if (!isArenaTable || arenaAdapter == null) return;
        if (arenaRequestDialog != null && arenaRequestDialog.isShowing()) return;

        androidx.recyclerview.widget.RecyclerView list =
                new androidx.recyclerview.widget.RecyclerView(activity);
        list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(activity));
        list.setAdapter(arenaAdapter);

        // tap = accept
        list.addOnItemTouchListener(new androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(androidx.recyclerview.widget.RecyclerView rv, android.view.MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && e.getAction() == android.view.MotionEvent.ACTION_UP) {
                    arenaAdapter.accept(rv.getChildAdapterPosition(child));
                }
                return false;
            }
        });

        // swipe = reject
        new androidx.recyclerview.widget.ItemTouchHelper(
            new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0,
                androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(androidx.recyclerview.widget.RecyclerView rv,
                                      androidx.recyclerview.widget.RecyclerView.ViewHolder vh,
                                      androidx.recyclerview.widget.RecyclerView.ViewHolder t) { return false; }
                @Override
                public void onSwiped(androidx.recyclerview.widget.RecyclerView.ViewHolder vh, int dir) {
                    arenaAdapter.reject(vh.getBindingAdapterPosition());
                }
            }).attachToRecyclerView(list);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.arena_tap_to_accept));
        builder.setView(list);
        builder.setNegativeButton(android.R.string.cancel, null);
        arenaRequestDialog = builder.create();
        arenaRequestDialog.show();
    }

    public void dismissArenaJoinRequest() {
        if (arenaAdapter != null) arenaAdapter.reset();
        if (arenaRequestDialog != null) {
            arenaRequestDialog.dismiss();
            arenaRequestDialog = null;
        }
    }

    public void arenaTableRequestJoinEvent(String playerName) {
        if (arenaAdapter == null) return;
        if (arenaRequestDialog == null || !arenaRequestDialog.isShowing()) {
            showArenaJoinRequest();
        }
        arenaAdapter.addPlayer(playerName);
    }
```

- [ ] **Step 4: Clean up on destroy**

Add an `onDestroyView` override to the class:

```java
    @Override
    public void onDestroyView() {
        dismissArenaJoinRequest();
        super.onDestroyView();
    }
```

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java
git commit -m "feat(arena): host join-request list with accept/reject"
```

---

## Task 9: `LiveGameRoomActivity` — wire arena into dispatch

**Files:**
- Modify: `app/src/main/java/.../liveGameRoom/LiveGameRoomActivity.java`

- [ ] **Step 1: Add an `isArena` field set from the room**

Immediately after `room = getIntent().getParcelableExtra("room");` (L99), add:

```java
        isArena = room != null && room.getName() != null
                && room.getName().toLowerCase().contains("arena");
```

And declare the field near the other fields (e.g. just after the `me` field at L64):

```java
    private boolean isArena = false;
```

- [ ] **Step 2: Mark new table fragments as arena**

In the `dsgJoinTableEvent` branch, after `tableFragment.setTable(table);` (L273), add:

```java
                                        tableFragment.setArena(isArena);
```

- [ ] **Step 3: Dismiss the request list when an opponent joins**

In the same `dsgJoinTableEvent` branch, after `updateMainRoom();` (L270), add:

```java
                                    LiveTableFragment arenaTable = (LiveTableFragment)
                                            getSupportFragmentManager().findFragmentByTag("liveTable");
                                    if (arenaTable != null && arenaTable.isArenaTable
                                            && arenaTable.table != null
                                            && arenaTable.table.getId() == tableId
                                            && arenaTable.table.getPlayers().size() >= 2) {
                                        arenaTable.dismissArenaJoinRequest();
                                    }
```

- [ ] **Step 4: Add the incoming arena-request dispatch branch**

In the dispatch chain, add a new branch immediately after the `dsgChangeStateTableEvent` branch (after L264, before the `dsgJoinTableEvent` branch at L265):

```java
                                } else if (jsonEvent.get("dsgArenaRequestJoinTableEvent") != null) {
                                    Map<String, Object> data =
                                            (Map<String, Object>) jsonEvent.get("dsgArenaRequestJoinTableEvent");
                                    String player = (String) data.get("player");
                                    int tableId = (Integer) data.get("table");
                                    LiveTableFragment fragment = (LiveTableFragment)
                                            getSupportFragmentManager().findFragmentByTag("liveTable");
                                    if (fragment != null && fragment.table != null
                                            && fragment.table.getId() == tableId) {
                                        fragment.arenaTableRequestJoinEvent(player);
                                    }
```

(If the incoming payload's requester key turns out to differ from `"player"`, adjust this single `data.get(...)` — confirm against the backend during the smoke test.)

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java
git commit -m "feat(arena): dispatch arena join requests and dismiss on join"
```

---

## Task 10: Integration + emulator smoke test

**Files:**
- Modify (temporarily): `app/src/main/java/be/submanifold/pentelive/PentePlayer.java:33`

- [ ] **Step 1: Point the app at the local backend**

In `PentePlayer.java:33`, change:

```java
    public static Boolean development = true;
```

(Revert to `false` before any release build — do NOT commit this flip; see Step 5.)

- [ ] **Step 2: Full build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install + launch on a running emulator**

Run: `./gradlew installDebug`
Then launch the app and log in (non-guest account) against the local backend.

- [ ] **Step 4: Smoke-test checklist (manual)**

- [ ] Lobby lists the "Arena" room; entering it shows a tables-only view with a "+".
- [ ] "+" opens the arena create form; toggling Rated hides/shows Play-as; toggling Timed shows minute/second fields.
- [ ] Submitting creates a table and opens the table view with the join-request list ("Tap player to accept").
- [ ] The arena table list hides tables that already have 2 players.
- [ ] (With a 2nd client) a join request appears with a shrinking 6s bar; it auto-removes at 0; tap accepts; swipe rejects; the list dismisses when an opponent actually joins.
- [ ] Rapidly enter/exit the arena table — no crash (timer cleanup OK).

**Note:** the full request→accept/reject round-trip needs a second client; that part is verified on-device by the user.

- [ ] **Step 5: Confirm the dev flag is not committed**

Run: `git status --short`
Expected: `PentePlayer.java` is either unmodified or intentionally reverted to `false`. Do not commit the `development = true` flip.

---

## Self-review notes

- **Spec coverage:** room detection (T6/T7/T9), create form + `dsgArenaCreateTableEvent` (T5), request-join with `player` key (T1/T7), host list + countdown + accept/reject incl. `message` fix (T1/T4/T8), incoming dispatch + dismiss-on-join + ≤1-player filter (T6/T8/T9), guest guard + play-as-when-unrated (T5), timer lifecycle cleanup (T4/T8), testing (T10) — all mapped.
- **Type consistency:** `ArenaEvents.{createTable,requestJoin,accept,reject}` signatures are identical across T1, T4 (`accept`/`reject`), T5 (`createTable`), T7 (`requestJoin`). `setArena`/`isArenaTable`/`arenaTableRequestJoinEvent`/`dismissArenaJoinRequest` defined in T8 and consumed in T9. `ArenaJoinRequestAdapter.ActionSender` satisfied by `activity::sendEvent`.
- **Known anchor caveat:** line numbers reflect the current files; if they drift, the surrounding code snippets identify the insertion points.
