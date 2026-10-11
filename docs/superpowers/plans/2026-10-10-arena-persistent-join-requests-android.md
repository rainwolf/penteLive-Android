# Arena Persistent Join Requests (Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Android arena client render server-owned join-request state (owner snapshot, requester "my requests", ended notices), drop every client countdown, send the lowercase decline key, and add withdraw.

**Architecture:** Three plain-Java classes carry all the logic and are unit-tested with JUnit: `ArenaEventParser` (decodes the new S→C events from the `Map` that `LiveGameRoomActivity.jsonToMap` already produces), `ArenaRequestListModel` (owner list: freeze, minimum interactive window, in-flight action, new-request cue; time comes from an injected `Scheduler`), and `ArenaMyRequests` (requester lobby: requested marks, in-flight taps, withdraw guard). `ArenaRequestNotice` maps an ended reason to a string resource. The Android classes (`ArenaJoinRequestAdapter`, `LiveTableFragment`, `LiveGameRoomFragment`, `TableListAdapter`, `LiveGameRoomActivity`) become thin views over those models.

**Tech Stack:** Java (minSdk 26, Java 8 APIs), AndroidX RecyclerView 1.4.0 (`DiffUtil`, `ItemTouchHelper`), JUnit 4.13.2 local unit tests (no Robolectric), Gradle via the Android Studio JBR (JDK 21).

**Spec:** `/Users/waliedothman/mariposa/coding/pente.org-project/pente.org/docs/superpowers/specs/2026-10-10-arena-persistent-join-requests-design.md` (sections "Wire protocol", "Clients" C1–C5, "Testing"). Background: `docs/superpowers/specs/2026-05-28-android-arena-port-design.md` (the 6 s countdown design this plan removes).

## Global Constraints

- Repo: `/Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android`, branch `arena-persistent-join-requests` (already checked out; confirm with `git -C <repo> branch --show-current`).
- Wire keys, copied verbatim from the spec. C→S: `dsgArenaRequestJoinTableEvent {table}`, `dsgArenaAcceptTableJoinEvent {table, playerToAccept}`, `dsgArenaRejectTableJoinEvent {table, playerToReject}` (lowercase `dsg`), new `dsgArenaWithdrawJoinRequestEvent {table}`. S→C: `dsgArenaJoinRequestsEvent {table, requests: [{player, seq}]}`, `dsgArenaMyRequestsEvent {tables: [int]}`, `dsgArenaRequestEndedEvent {table, player, owner, reason}`.
- Reasons, verbatim: `DECLINED`, `TABLE_FULL`, `TABLE_CLOSED`, `DUPLICATE`, `BLOCKED`, `NOT_AVAILABLE`, `GUEST_RATED`, `BOOTED`, `NO_LONGER_AVAILABLE`. `BOOTED` means the requester was booted from that table less than 5 minutes ago (R2).
- No longer sent S→C, so no Android handler: `dsgArenaRequestJoinTableEvent`, `dsgArenaRejectTableJoinEvent`. There is no server alias for the capital-key decline.
- Freeze: 400 ms (milliseconds). Minimum interactive window after an unfreeze: 1000 ms. No other client timer may exist for requests (spec C1).
- Notice texts (English, verbatim from spec C4, `<n>` = table number): "<owner> declined your request", "Table <n> is no longer available", "Table <n> was closed", "You already sent a request to <owner>", "<owner> removed you from table <n>, try again in a few minutes", "You can't request table <n> again until its next game", "You can't join table <n> right now", "Guests can't join rated tables", "<player> is no longer available". `<owner>` is always the event's own `owner` field, never the lobby's table model. Withdraw confirm: "Withdraw your request to table <n>? You won't be able to request it again until its next game."
- Every new string gets a German translation in `res/values-de/strings.xml` (German is the only other locale). Apostrophes in string resources are escaped as `\'`.
- Local unit tests run with `returnDefaultValues = true` (`app/build.gradle:74-82`), so `org.json` is a stub that returns null/0. Parsers therefore take the already-decoded `Map<String, Object>`, never a JSON string.
- Gradle needs JDK 17+. Every gradle command in this plan is written in full as `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android <task>`. The default `JAVA_HOME` is JDK 8 and fails.
- Commits: `git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "..."`. Stage explicit paths only (never `git add -A` / `git add .`). No co-author or generated-by lines in any commit message. After each commit, read the diffstat and check the file count matches.
- Never commit `PentePlayer.development = true` (`app/src/main/java/be/submanifold/pentelive/PentePlayer.java:36`).
- Scratch files go to `/private/tmp/claude-501/arena-android/`, never inside the repo.

## Review Focus

1. A snapshot whose `requests` arrive out of `seq` order, with `seq` as `Long`, with the same player twice, with the `requests` key missing, or with non-string values → rows sorted by `seq`, one row per player (lowest `seq`), empty list when missing, no `ClassCastException`. Pinned in Task 2 (`joinRequests_*` tests).
2. The answer snapshot to the owner's own accept/decline arrives inside the 1000 ms minimum window → the list freezes at once instead of leaving the stale row tappable until the window ends. Pinned in Task 3 (`answerToAnActionInsideTheMinWindow_freezesAtOnce`).
3. The table fills mid-freeze, the dialog is dismissed, and the pending 400 ms timer fires later → no rows reappear and the listener is not called. Pinned in Task 3 (`reset_cancelsTimersAndClearsEverything`).
4. The requester leaves the withdraw confirm dialog open while the owner declines (`dsgArenaMyRequestsEvent` drops the table) and then confirms → no withdraw is sent (a withdraw of a non-pending request could block the table for them). Pinned in Task 4 (`confirmWithdraw_afterTheRequestAlreadyEnded_sendsNothing`).
5. A future server sends a reason this build does not know, or omits `reason` → no Toast, no crash. Pinned in Task 5 (`unknownOrMissingReason_givesNoNotice`).
6. The owner waits on the open dialog with an empty list and the first request arrives → the row appears only after the 400 ms freeze, so a tap aimed at "Exit table" (which the growing dialog moves) cannot accept it. Pinned in Task 3 (`firstRowsOntoTheOpenEmptyDialog_freezeBeforeTheyAppear`).

Accepted limitation (spec-literal, not a guarantee this plan claims): in-flight state clears on the *next* answer event, which carries no correlation to the action. A requester who taps table 5 then table 6 has table 6's guard cleared by table 5's `dsgArenaMyRequestsEvent`; a fast third tap on 6 before 6's own answer sends a second request and shows the DUPLICATE Toast. The owner list has the same shape: any snapshot for the table (e.g. a new request landing first) clears `inFlightPlayer`; the server tolerates the resulting double accept/decline (spec "Testing", server: "a double accept of the same requester"). Closing it needs a correlation field from the server.

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEvents.java` | Modify | Encode C→S arena events (lowercase decline, new withdraw). |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequest.java` | Create | Value: one pending request `{player, seq}`. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestEnded.java` | Create | Value: one `dsgArenaRequestEndedEvent`. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEventParser.java` | Create | Decode the three new S→C events from `Map<String, Object>`. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestListModel.java` | Create | Owner list state machine: freeze, min window, in-flight, cue. Plain Java. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRows.java` | Create | Owner list rows (player, seq, in-flight) and the player-keyed `DiffUtil.Callback`. Plain Java. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaHandlerScheduler.java` | Create | `ArenaRequestListModel.Scheduler` backed by a main-looper `Handler`. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaMyRequests.java` | Create | Requester lobby state: requested set, in-flight taps, withdraw guard. Plain Java. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestNotice.java` | Create | Reason → string resource + format args. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequestAdapter.java` | Rewrite | Render the model's rows via `ArenaRequestRows`, row spinner, accept/decline via the model. |
| `app/src/main/res/layout/arena_join_request_row.xml` | Rewrite | Name, rating, in-flight spinner (countdown bar removed). |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java` | Modify | Owner dialog: freeze overlay, touch/swipe gating, snapshot entry point. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java` | Modify | Dispatch the three new S→C events; drop the old request branch. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomFragment.java` | Modify | Lobby tap → request / withdraw confirm / ignore. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/TableListAdapter.java` | Modify | "Requested" mark on lobby rows. |
| `app/src/main/res/layout/live_table_row.xml` | Modify | `requestedText` view. |
| `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml` | Modify | Notice, withdraw and mark strings; remove `arena_waiting`. |
| `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventsTest.java` | Modify | Replace the capital-key test; add withdraw. |
| `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventParserTest.java` | Create | JSON-shape tests for the new S→C events. |
| `app/src/test/java/be/submanifold/pentelive/liveGameRoom/FakeArenaScheduler.java` | Create | Manual-clock scheduler for model tests. |
| `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestListModelTest.java` | Create | Freeze / window / in-flight / cue tests. |
| `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRowsTest.java` | Create | Row build and diff tests (the spec's "adapter's diff"). |
| `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaMyRequestsTest.java` | Create | Lobby state tests. |
| `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestNoticeTest.java` | Create | Reason mapping tests. |

All paths below are relative to `/Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android`.

---

### Task 1: Encoders — lowercase decline key and withdraw

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEvents.java:3-5` (class doc), `:32-36` (`reject`), append `withdraw`
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventsTest.java:32-37` (replace), append withdraw test

**Interfaces:**
- Consumes: nothing.
- Produces: `static String ArenaEvents.reject(String player, String playerToReject, int tableId)` (lowercase key, no `message`), `static String ArenaEvents.withdraw(String player, int tableId)`. Existing `requestJoin(String, int)` and `accept(String, String, int)` are unchanged.

- [ ] **Step 1: Write the failing tests**

In `ArenaEventsTest.java`, replace lines 32-37 (the `reject_usesCapitalDsgAndCorrectedMessageKey` test) with:

```java
    @Test
    public void reject_usesLowercaseKey() {
        // A capital "DSG" key decodes to null in the server's Gson wrapper, and the server
        // then disconnects the owner and closes their table (spec C2 "Decline key").
        assertEquals(
            "{\"dsgArenaRejectTableJoinEvent\":{\"player\":\"alice\",\"playerToReject\":\"bob\",\"table\":42}}",
            ArenaEvents.reject("alice", "bob", 42));
    }

    @Test
    public void withdraw_buildsExpectedJson() {
        assertEquals(
            "{\"dsgArenaWithdrawJoinRequestEvent\":{\"player\":\"bob\",\"table\":42,\"time\":0}}",
            ArenaEvents.withdraw("bob", 42));
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaEventsTest"`
Expected: FAIL — compilation error `cannot find symbol ... method withdraw(String,int)`.

- [ ] **Step 3: Implement**

In `ArenaEvents.java` replace lines 3-5 with:

```java
/** Builds the raw-JSON-string arena protocol events sent via LiveGameRoomActivity.sendEvent.
 *  requestJoin and withdraw add "player"; the server fills or checks it against the session.
 *  Every key starts with lowercase "dsg": the server's Gson wrapper matches keys case-sensitively. */
```

Replace lines 32-36 (`reject`) with:

```java
    public static String reject(String player, String playerToReject, int tableId) {
        return "{\"dsgArenaRejectTableJoinEvent\":{\"player\":\"" + player
                + "\",\"playerToReject\":\"" + playerToReject
                + "\",\"table\":" + tableId + "}}";
    }

    public static String withdraw(String player, int tableId) {
        return "{\"dsgArenaWithdrawJoinRequestEvent\":{\"player\":\"" + player
                + "\",\"table\":" + tableId + ",\"time\":0}}";
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaEventsTest"`
Expected: PASS, 5 tests (`createTable_buildsExpectedJson`, `requestJoin_includesPlayerKey`, `accept_buildsExpectedJson`, `reject_usesLowercaseKey`, `withdraw_buildsExpectedJson`).

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEvents.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventsTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "fix: send arena decline with the lowercase dsg key; add withdraw event"
```

---

### Task 2: Decoders for the new server events

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequest.java`
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestEnded.java`
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEventParser.java`
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventParserTest.java`

**Interfaces:**
- Consumes: the `Map<String, Object>` payload that `LiveGameRoomActivity.jsonToMap` (`LiveGameRoomActivity.java:833-893`) produces: numbers are `Integer` or `Long`, arrays are `List<Object>`, objects are `Map<String, Object>`.
- Produces:
  - `final class ArenaJoinRequest { public final String player; public final long seq; ArenaJoinRequest(String player, long seq); equals/hashCode/toString }`
  - `final class ArenaRequestEnded { public final int table; public final String player; public final String owner; public final String reason; ArenaRequestEnded(int, String, String, String) }`
  - `static int ArenaEventParser.table(Map<String, Object> data)` — -1 when `table` is missing or not a number
  - `static List<ArenaJoinRequest> ArenaEventParser.joinRequests(Map<String, Object> data)` — sorted by `seq` ascending, one entry per player
  - `static Set<Integer> ArenaEventParser.myRequestTables(Map<String, Object> data)`
  - `static ArenaRequestEnded ArenaEventParser.requestEnded(Map<String, Object> data)`

- [ ] **Step 1: Write the failing tests**

Create `ArenaEventParserTest.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Shapes follow spec "Wire protocol": jsonToMap yields Integer or Long numbers and List/Map containers. */
public class ArenaEventParserTest {

    private static Map<String, Object> entry(Object player, Object seq) {
        Map<String, Object> m = new HashMap<>();
        if (player != null) m.put("player", player);
        if (seq != null) m.put("seq", seq);
        return m;
    }

    private static Map<String, Object> snapshot(int table, Object requests) {
        Map<String, Object> m = new HashMap<>();
        m.put("table", table);
        if (requests != null) m.put("requests", requests);
        return m;
    }

    private static List<String> players(List<ArenaJoinRequest> rs) {
        List<String> out = new ArrayList<>();
        for (ArenaJoinRequest r : rs) out.add(r.player);
        return out;
    }

    @Test
    public void table_readsIntegerOrLong() {
        assertEquals(7, ArenaEventParser.table(snapshot(7, null)));
        Map<String, Object> m = new HashMap<>();
        m.put("table", 9L);
        assertEquals(9, ArenaEventParser.table(m));
    }

    @Test
    public void table_missingOrNonNumeric_isMinusOne() {
        // Never a real table id, so the event matches no table instead of throwing on the UI thread
        // (a throw there would also leave the event worker stuck in uiRunnable.wait()).
        assertEquals(-1, ArenaEventParser.table(new HashMap<>()));
        Map<String, Object> m = new HashMap<>();
        m.put("table", "7");
        assertEquals(-1, ArenaEventParser.table(m));
    }

    @Test
    public void joinRequests_sortsBySeqAscending() {
        List<Object> raw = Arrays.<Object>asList(entry("carol", 5), entry("bob", 2), entry("dave", 9));
        List<ArenaJoinRequest> rs = ArenaEventParser.joinRequests(snapshot(7, raw));
        assertEquals(Arrays.asList("bob", "carol", "dave"), players(rs));
        assertEquals(2L, rs.get(0).seq);
    }

    @Test
    public void joinRequests_acceptsLongSeq() {
        List<Object> raw = Arrays.<Object>asList(entry("bob", 3000000000L), entry("carol", 1));
        List<ArenaJoinRequest> rs = ArenaEventParser.joinRequests(snapshot(7, raw));
        assertEquals(Arrays.asList("carol", "bob"), players(rs));
        assertEquals(3000000000L, rs.get(1).seq);
    }

    @Test
    public void joinRequests_keepsOneRowPerPlayer_theLowestSeq() {
        List<Object> raw = Arrays.<Object>asList(entry("bob", 8), entry("carol", 4), entry("bob", 3));
        List<ArenaJoinRequest> rs = ArenaEventParser.joinRequests(snapshot(7, raw));
        assertEquals(Arrays.asList(new ArenaJoinRequest("bob", 3), new ArenaJoinRequest("carol", 4)), rs);
    }

    @Test
    public void joinRequests_missingOrMalformed_isEmptyOrSkipped() {
        assertTrue(ArenaEventParser.joinRequests(snapshot(7, null)).isEmpty());
        assertTrue(ArenaEventParser.joinRequests(snapshot(7, "not a list")).isEmpty());
        assertTrue(ArenaEventParser.joinRequests(snapshot(7, Collections.emptyList())).isEmpty());
        // new Object() stands in for org.json's JSONObject.NULL, which is not a String or Number.
        List<Object> raw = Arrays.<Object>asList(entry(null, 1), entry("bob", null),
                entry(new Object(), 2), entry("carol", new Object()), "junk", entry("dave", 6));
        assertEquals(Collections.singletonList("dave"), players(ArenaEventParser.joinRequests(snapshot(7, raw))));
    }

    @Test
    public void myRequestTables_readsTheSet() {
        Map<String, Object> m = new HashMap<>();
        m.put("tables", Arrays.<Object>asList(3, 5L, 3));
        assertEquals(new HashSet<>(Arrays.asList(3, 5)), ArenaEventParser.myRequestTables(m));
    }

    @Test
    public void myRequestTables_missingOrMalformed_isEmpty() {
        assertTrue(ArenaEventParser.myRequestTables(new HashMap<>()).isEmpty());
        Map<String, Object> m = new HashMap<>();
        m.put("tables", Arrays.<Object>asList("x", new Object()));
        assertTrue(ArenaEventParser.myRequestTables(m).isEmpty());
    }

    @Test
    public void requestEnded_readsAllFields() {
        Map<String, Object> m = new HashMap<>();
        m.put("table", 7);
        m.put("player", "bob");
        m.put("owner", "alice");
        m.put("reason", "DECLINED");
        ArenaRequestEnded e = ArenaEventParser.requestEnded(m);
        assertEquals(7, e.table);
        assertEquals("bob", e.player);
        assertEquals("alice", e.owner);
        assertEquals("DECLINED", e.reason);
    }

    @Test
    public void requestEnded_nonStringFieldsBecomeNull() {
        Map<String, Object> m = new HashMap<>();
        m.put("table", 7);
        m.put("owner", new Object());
        ArenaRequestEnded e = ArenaEventParser.requestEnded(m);
        assertNull(e.player);
        assertNull(e.owner);
        assertNull(e.reason);
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaEventParserTest"`
Expected: FAIL — compilation error `cannot find symbol class ArenaEventParser`.

- [ ] **Step 3: Implement**

Create `ArenaJoinRequest.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

/** One pending arena join request in a dsgArenaJoinRequestsEvent snapshot. {@code seq} is
 *  server-assigned at creation, increasing and never reused; rows render in seq order. */
public final class ArenaJoinRequest {
    public final String player;
    public final long seq;

    public ArenaJoinRequest(String player, long seq) {
        this.player = player;
        this.seq = seq;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArenaJoinRequest)) return false;
        ArenaJoinRequest other = (ArenaJoinRequest) o;
        return seq == other.seq && player.equals(other.player);
    }

    @Override
    public int hashCode() {
        return 31 * player.hashCode() + Long.hashCode(seq);
    }

    @Override
    public String toString() {
        return player + "#" + seq;
    }
}
```

Create `ArenaRequestEnded.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

/** A dsgArenaRequestEndedEvent: {@code player} is the requester, {@code owner} the table owner,
 *  {@code reason} one of the spec's reason codes (or something newer this build does not know). */
public final class ArenaRequestEnded {
    public final int table;
    public final String player;
    public final String owner;
    public final String reason;

    public ArenaRequestEnded(int table, String player, String owner, String reason) {
        this.table = table;
        this.player = player;
        this.owner = owner;
        this.reason = reason;
    }
}
```

Create `ArenaEventParser.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Decodes the arena server-to-client events from the Map that LiveGameRoomActivity.jsonToMap
 *  builds (numbers are Integer or Long, arrays List, objects Map). Plain Java so it is unit-tested
 *  without org.json, which is a stub in local unit tests. */
public final class ArenaEventParser {
    private ArenaEventParser() {}

    /** The event's table id, or -1 (matches no table) when it is missing or not a number. */
    public static int table(Map<String, Object> data) {
        Object t = data.get("table");
        return t instanceof Number ? ((Number) t).intValue() : -1;
    }

    /** dsgArenaJoinRequestsEvent: rows in seq order, one per player (the lowest seq wins). */
    public static List<ArenaJoinRequest> joinRequests(Map<String, Object> data) {
        List<ArenaJoinRequest> all = new ArrayList<>();
        Object raw = data.get("requests");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object player = m.get("player");
                Object seq = m.get("seq");
                if (player instanceof String && seq instanceof Number) {
                    all.add(new ArenaJoinRequest((String) player, ((Number) seq).longValue()));
                }
            }
        }
        all.sort(Comparator.comparingLong(r -> r.seq));
        List<ArenaJoinRequest> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ArenaJoinRequest r : all) {
            if (seen.add(r.player)) out.add(r);
        }
        return out;
    }

    /** dsgArenaMyRequestsEvent: the tables this player has pending requests at. */
    public static Set<Integer> myRequestTables(Map<String, Object> data) {
        Set<Integer> out = new TreeSet<>();
        Object raw = data.get("tables");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o instanceof Number) out.add(((Number) o).intValue());
            }
        }
        return out;
    }

    public static ArenaRequestEnded requestEnded(Map<String, Object> data) {
        return new ArenaRequestEnded(table(data), str(data.get("player")),
                str(data.get("owner")), str(data.get("reason")));
    }

    private static String str(Object o) {
        return o instanceof String ? (String) o : null;
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaEventParserTest"`
Expected: PASS, 10 tests.

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequest.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestEnded.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaEventParser.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaEventParserTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "feat: decode arena join-request snapshot, my-requests and ended events"
```

---

### Task 3: Owner list model — freeze, minimum window, in-flight action, new-request cue, row diff

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestListModel.java`
- Create: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/FakeArenaScheduler.java`
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestListModelTest.java`
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRows.java` (Steps 7-11)
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRowsTest.java` (Steps 7-11)

**Interfaces:**
- Consumes: `ArenaJoinRequest` (Task 2).
- Produces:
  - `ArenaRequestListModel.FREEZE_MS = 400L`, `ArenaRequestListModel.MIN_INTERACTIVE_MS = 1000L`
  - `interface ArenaRequestListModel.Cancellable { void cancel(); }`
  - `interface ArenaRequestListModel.Scheduler { Cancellable schedule(Runnable task, long delayMs); }`
  - `interface ArenaRequestListModel.Listener { void onChanged(); }`
  - `ArenaRequestListModel(Scheduler scheduler, Listener listener)`
  - `boolean onSnapshot(List<ArenaJoinRequest> snapshot)` — returns true when the snapshot holds a player absent from the previous received snapshot (sound cue)
  - `void onShown()` — the owner dialog is now on screen; `reset()` hides it again
  - `boolean beginAction(String player)` — true if the tap is allowed; marks `player` in flight
  - `List<ArenaJoinRequest> rows()` (unmodifiable, applied rows), `boolean isFrozen()`, `boolean isInteractive()`, `String inFlightPlayer()` (null when none), `void reset()`
- Produces (row diff, Steps 7-11):
  - `final class ArenaRequestRows` with `static final class Row { final String player; final long seq; final boolean inFlight; equals/hashCode }`
  - `static List<ArenaRequestRows.Row> ArenaRequestRows.build(List<ArenaJoinRequest> requests, String inFlightPlayer)`
  - `static DiffUtil.Callback ArenaRequestRows.diff(List<ArenaRequestRows.Row> old, List<ArenaRequestRows.Row> next)` — items match by player, contents by `Row.equals`

Rules (spec C2), as the model implements them:
- The list is "visible" while the owner dialog is showing (from `onShown()` until `reset()`), not when it has rows. The owner usually waits on the open dialog with an empty list and only "Exit table"; when the first row appears the dialog grows and "Exit table" moves, so the first rows go through the freeze too. A snapshot arriving while no dialog is showing is applied at once. An empty snapshot onto an empty, idle list changes nothing.
- A snapshot arriving while visible and idle starts a 400 ms freeze; snapshots during the freeze replace the held one; at freeze end the held snapshot is applied.
- After an unfreeze that leaves rows, a 1000 ms window keeps the list interactive; snapshots in the window are held and start the next freeze when the window ends.
- An empty applied snapshot hides the list (no window).
- `beginAction` blocks every tap until the next snapshot. That snapshot freezes the list at once, even inside the window (spec: "Then the freeze applies").

- [ ] **Step 1: Write the fake scheduler**

Create `FakeArenaScheduler.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import java.util.ArrayList;
import java.util.List;

/** Manual clock for ArenaRequestListModel tests: tasks run only inside advance(), in due order. */
final class FakeArenaScheduler implements ArenaRequestListModel.Scheduler {

    private static final class Task {
        final long due;
        final Runnable runnable;
        boolean cancelled;

        Task(long due, Runnable runnable) {
            this.due = due;
            this.runnable = runnable;
        }
    }

    private final List<Task> tasks = new ArrayList<>();
    private long now = 0;

    @Override
    public ArenaRequestListModel.Cancellable schedule(Runnable task, long delayMs) {
        final Task t = new Task(now + delayMs, task);
        tasks.add(t);
        return () -> t.cancelled = true;
    }

    /** Moves the clock forward by ms, running every task that falls due (including ones scheduled meanwhile). */
    void advance(long ms) {
        long target = now + ms;
        while (true) {
            Task next = null;
            for (Task t : tasks) {
                if (!t.cancelled && t.due <= target && (next == null || t.due < next.due)) {
                    next = t;
                }
            }
            if (next == null) break;
            tasks.remove(next);
            now = next.due;
            next.runnable.run();
        }
        now = target;
    }

    int pendingCount() {
        int n = 0;
        for (Task t : tasks) {
            if (!t.cancelled) n++;
        }
        return n;
    }
}
```

- [ ] **Step 2: Write the failing tests**

Create `ArenaRequestListModelTest.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Spec C2: freeze 400 ms, minimum interactive window 1000 ms, no optimistic removal. */
public class ArenaRequestListModelTest {

    private FakeArenaScheduler scheduler;
    private int changes;
    private ArenaRequestListModel model;

    @Before
    public void setUp() {
        scheduler = new FakeArenaScheduler();
        changes = 0;
        model = new ArenaRequestListModel(scheduler, () -> changes++);
    }

    private static ArenaJoinRequest req(String player, long seq) {
        return new ArenaJoinRequest(player, seq);
    }

    private static List<ArenaJoinRequest> snap(ArenaJoinRequest... rs) {
        return Arrays.asList(rs);
    }

    private List<String> shown() {
        List<String> out = new ArrayList<>();
        for (ArenaJoinRequest r : model.rows()) out.add(r.player);
        return out;
    }

    /** With no dialog showing a snapshot applies at once; then the dialog opens over those rows.
     *  The list is visible, idle and interactive. */
    private void showRows(ArenaJoinRequest... rs) {
        model.onSnapshot(snap(rs));
        model.onShown();
    }

    @Test
    public void snapshotWhileHidden_appliesAtOnceWithoutFreeze() {
        // No dialog is showing, so nothing is under the owner's finger.
        model.onSnapshot(snap(req("bob", 1)));
        assertEquals(Collections.singletonList("bob"), shown());
        assertFalse(model.isFrozen());
        assertTrue(model.isInteractive());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    public void snapshotWhileVisible_freezes400msThenApplies() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        assertTrue(model.isFrozen());
        assertFalse(model.isInteractive());
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(399);
        assertTrue(model.isFrozen());
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(1);
        assertFalse(model.isFrozen());
        assertTrue(model.isInteractive());
        assertEquals(Arrays.asList("bob", "carol"), shown());
    }

    @Test
    public void snapshotsDuringFreeze_onlyTheLastIsApplied() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(100);
        model.onSnapshot(snap(req("carol", 2)));
        scheduler.advance(100);
        model.onSnapshot(snap(req("carol", 2), req("dave", 3)));
        scheduler.advance(199);
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(1); // t=400: the freeze that started at t=0 ends; it is not restarted
        assertEquals(Arrays.asList("carol", "dave"), shown());
        assertFalse(model.isFrozen());
    }

    @Test
    public void snapshotDuringMinWindow_isHeldThenTriggersTheNextFreeze() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(400); // unfreeze at t=400; window runs to t=1400
        model.onSnapshot(snap(req("carol", 2)));
        assertFalse(model.isFrozen());
        assertTrue(model.isInteractive());
        assertEquals(Arrays.asList("bob", "carol"), shown());
        scheduler.advance(999); // t=1399
        assertFalse(model.isFrozen());
        assertEquals(Arrays.asList("bob", "carol"), shown());
        scheduler.advance(1); // t=1400: window ends, the held snapshot starts a freeze
        assertTrue(model.isFrozen());
        scheduler.advance(400); // t=1800
        assertEquals(Collections.singletonList("carol"), shown());
        assertFalse(model.isFrozen());
    }

    @Test
    public void windowEndingWithNothingHeld_staysInteractive_andTheNextSnapshotFreezesAtOnce() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(1400);
        assertTrue(model.isInteractive());
        assertEquals(0, scheduler.pendingCount());
        model.onSnapshot(snap(req("carol", 2)));
        assertTrue(model.isFrozen());
    }

    @Test
    public void burstOfSnapshots_leavesAtLeast1000msInteractiveAfterEveryUnfreeze() {
        showRows(req("p0", 0));
        List<Long> runs = new ArrayList<>();
        long run = -1; // -1: not inside an interactive run that began at an unfreeze
        boolean wasLocked = false;
        for (int t = 1; t <= 6000; t++) {
            if (t % 50 == 0) {
                model.onSnapshot(snap(req("p0", 0), req("p" + t, t)));
            }
            scheduler.advance(1);
            boolean locked = !model.isInteractive();
            if (wasLocked && !locked) run = 0;
            if (!locked && run >= 0) run++;
            if (locked && run >= 0) {
                runs.add(run);
                run = -1;
            }
            wasLocked = locked;
        }
        assertTrue("the burst must cause several freeze cycles, got " + runs.size(), runs.size() >= 3);
        for (long r : runs) {
            assertTrue("interactive run was only " + r + " ms", r >= ArenaRequestListModel.MIN_INTERACTIVE_MS);
        }
    }

    @Test
    public void firstRowsOntoTheOpenEmptyDialog_freezeBeforeTheyAppear() {
        // The owner just created the table: the dialog shows only "Exit table" over an empty list.
        model.onShown();
        model.onSnapshot(snap(req("bob", 1)));
        assertTrue(model.isFrozen());
        assertFalse(model.isInteractive());
        assertTrue(model.rows().isEmpty());
        assertFalse(model.beginAction("bob"));
        scheduler.advance(399);
        assertTrue(model.rows().isEmpty());
        scheduler.advance(1);
        assertEquals(Collections.singletonList("bob"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void emptySnapshotOntoTheOpenEmptyDialog_changesNothing() {
        model.onShown();
        model.onSnapshot(snap());
        assertFalse(model.isFrozen());
        assertTrue(model.rows().isEmpty());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    public void emptySnapshot_hidesTheListAfterTheFreeze_andTheNextRowsFreezeAgain() {
        showRows(req("bob", 1));
        model.onSnapshot(snap());
        assertTrue(model.isFrozen());
        assertEquals(Collections.singletonList("bob"), shown());
        scheduler.advance(400);
        assertTrue(model.rows().isEmpty());
        assertFalse(model.isFrozen());
        assertFalse(model.isInteractive());
        assertEquals(0, scheduler.pendingCount()); // no minimum window once the list is empty
        model.onSnapshot(snap(req("carol", 2))); // the dialog is still open
        assertTrue(model.isFrozen());
        assertTrue(model.rows().isEmpty());
        scheduler.advance(400);
        assertEquals(Collections.singletonList("carol"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void newRequestCue_firesOnlyForAPlayerAbsentFromThePreviousSnapshot() {
        assertFalse(model.onSnapshot(snap()));
        assertTrue(model.onSnapshot(snap(req("bob", 1))));
        assertFalse(model.onSnapshot(snap(req("bob", 1)))); // R11 echo: nothing new
        assertTrue(model.onSnapshot(snap(req("bob", 1), req("carol", 2))));
        assertFalse(model.onSnapshot(snap(req("carol", 2)))); // a removal is not a cue
        assertTrue(model.onSnapshot(snap(req("carol", 2), req("bob", 3)))); // bob is back
    }

    @Test
    public void beginAction_isRefusedWhileFrozenOrForAPlayerNotShown() {
        showRows(req("bob", 1));
        assertFalse(model.beginAction("zed"));
        assertFalse(model.beginAction(null));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2))); // freeze
        assertFalse(model.beginAction("bob"));
        assertNull(model.inFlightPlayer());
    }

    @Test
    public void beginAction_marksTheRowAndBlocksAllTapsUntilTheNextSnapshot() {
        showRows(req("bob", 1), req("carol", 2));
        int before = changes;
        assertTrue(model.beginAction("bob"));
        assertTrue(changes > before);
        assertEquals("bob", model.inFlightPlayer());
        assertFalse(model.isInteractive());
        assertFalse(model.beginAction("carol"));
        assertEquals(Arrays.asList("bob", "carol"), shown()); // no optimistic removal
        model.onSnapshot(snap(req("carol", 2))); // the R11 answer
        assertNull(model.inFlightPlayer());
        assertTrue(model.isFrozen());
        scheduler.advance(400);
        assertEquals(Collections.singletonList("carol"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void answerToAnActionInsideTheMinWindow_freezesAtOnce() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2)));
        scheduler.advance(400); // window runs to t=1400
        assertTrue(model.beginAction("bob"));
        scheduler.advance(100);
        model.onSnapshot(snap(req("carol", 2))); // the answer, at t=500
        assertTrue(model.isFrozen());
        assertFalse(model.beginAction("bob")); // the stale row is not tappable
        scheduler.advance(400); // t=900, inside what was the old window
        assertEquals(Collections.singletonList("carol"), shown());
        assertTrue(model.isInteractive());
    }

    @Test
    public void reset_cancelsTimersAndClearsEverything() {
        showRows(req("bob", 1));
        model.onSnapshot(snap(req("bob", 1), req("carol", 2))); // freeze timer pending
        model.reset();
        assertTrue(model.rows().isEmpty());
        assertFalse(model.isFrozen());
        assertNull(model.inFlightPlayer());
        assertEquals(0, scheduler.pendingCount());
        int before = changes;
        scheduler.advance(5000);
        assertEquals(before, changes);
        assertTrue(model.rows().isEmpty());
        assertTrue(model.onSnapshot(snap(req("bob", 2)))); // cue history is cleared too
        // reset() also hides the list: with no dialog showing, the snapshot applied at once.
        assertEquals(Collections.singletonList("bob"), shown());
        assertFalse(model.isFrozen());
    }

    @Test
    public void rowsAreACopyTheCallerCannotChange() {
        List<ArenaJoinRequest> in = new ArrayList<>(snap(req("bob", 1)));
        model.onSnapshot(in);
        in.clear();
        assertEquals(1, model.rows().size());
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaRequestListModelTest"`
Expected: FAIL — compilation error `cannot find symbol class ArenaRequestListModel`.

- [ ] **Step 4: Implement**

Create `ArenaRequestListModel.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owner-side arena request list (spec C2). It renders only the server's latest snapshot and never
 *  removes a row on its own. Snapshots that arrive while the owner dialog is showing freeze the list
 *  for FREEZE_MS, then apply; after each unfreeze the list stays interactive for at least
 *  MIN_INTERACTIVE_MS, holding newer snapshots until then. Plain Java: time comes from the
 *  injected Scheduler, so the rules are unit-tested without Android. */
public final class ArenaRequestListModel {

    public static final long FREEZE_MS = 400L;
    public static final long MIN_INTERACTIVE_MS = 1000L;

    public interface Cancellable { void cancel(); }

    public interface Scheduler { Cancellable schedule(Runnable task, long delayMs); }

    public interface Listener { void onChanged(); }

    private enum Phase { IDLE, FROZEN, WINDOW }

    private final Scheduler scheduler;
    private final Listener listener;

    private List<ArenaJoinRequest> rows = Collections.emptyList();
    private List<ArenaJoinRequest> held;              // newest snapshot not yet applied, or null
    private Set<String> lastReceived = new HashSet<>();
    private String inFlightPlayer;
    private boolean shown;                            // the owner dialog is on screen
    private Phase phase = Phase.IDLE;
    private Cancellable timer;

    public ArenaRequestListModel(Scheduler scheduler, Listener listener) {
        this.scheduler = scheduler;
        this.listener = listener;
    }

    /** Takes a dsgArenaJoinRequestsEvent snapshot (already in seq order). Returns true when it
     *  contains a player who was not in the previous snapshot (the new-request cue). */
    public boolean onSnapshot(List<ArenaJoinRequest> snapshot) {
        List<ArenaJoinRequest> copy = Collections.unmodifiableList(new ArrayList<>(snapshot));
        Set<String> names = new HashSet<>();
        for (ArenaJoinRequest r : copy) names.add(r.player);
        boolean newcomer = !lastReceived.containsAll(names);
        lastReceived = names;

        boolean answersAction = inFlightPlayer != null;
        inFlightPlayer = null;

        if (!shown) {
            // No dialog on screen: nothing is under the owner's finger, so take the snapshot at once.
            rows = copy;
        } else if (phase == Phase.IDLE && rows.isEmpty() && copy.isEmpty()) {
            // Empty onto empty: nothing would move, so no freeze.
            rows = copy;
        } else {
            // The dialog is showing, even with an empty list: a first row would grow the dialog
            // and move "Exit table" under the owner's finger, so it freezes like any change.
            held = copy;
            if (phase == Phase.IDLE || (phase == Phase.WINDOW && answersAction)) {
                startFreeze();
            }
            // FROZEN: the held snapshot was replaced. WINDOW: held until the window ends.
        }
        listener.onChanged();
        return newcomer;
    }

    /** The owner dialog is now on screen; from here on every snapshot freezes before it applies.
     *  Nothing shown changes, so the listener is not called. reset() hides the list again. */
    public void onShown() {
        shown = true;
    }

    /** Called for a tap (accept) or swipe (decline) on {@code player}. Returns false, changing
     *  nothing, when the list is frozen, an action already awaits the server, or the player is
     *  not shown. Otherwise the row is in flight and every tap is ignored until the next snapshot. */
    public boolean beginAction(String player) {
        if (player == null || !isInteractive()) return false;
        boolean shown = false;
        for (ArenaJoinRequest r : rows) {
            if (r.player.equals(player)) {
                shown = true;
                break;
            }
        }
        if (!shown) return false;
        inFlightPlayer = player;
        listener.onChanged();
        return true;
    }

    public List<ArenaJoinRequest> rows() {
        return rows;
    }

    public boolean isFrozen() {
        return phase == Phase.FROZEN;
    }

    public boolean isInteractive() {
        return !rows.isEmpty() && phase != Phase.FROZEN && inFlightPlayer == null;
    }

    public String inFlightPlayer() {
        return inFlightPlayer;
    }

    /** Clears everything, cancels the pending timer and marks the list hidden, e.g. when the table
     *  fills and the dialog closes. */
    public void reset() {
        cancelTimer();
        rows = Collections.emptyList();
        held = null;
        lastReceived = new HashSet<>();
        inFlightPlayer = null;
        shown = false;
        phase = Phase.IDLE;
        listener.onChanged();
    }

    private void startFreeze() {
        cancelTimer();
        phase = Phase.FROZEN;
        timer = scheduler.schedule(this::endFreeze, FREEZE_MS);
    }

    private void endFreeze() {
        timer = null;
        if (held != null) {
            rows = held;
            held = null;
        }
        if (rows.isEmpty()) {
            phase = Phase.IDLE; // empty snapshot: the list hides
        } else {
            phase = Phase.WINDOW;
            timer = scheduler.schedule(this::endWindow, MIN_INTERACTIVE_MS);
        }
        listener.onChanged();
    }

    private void endWindow() {
        timer = null;
        if (held != null) {
            startFreeze();
        } else {
            phase = Phase.IDLE;
        }
        listener.onChanged();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaRequestListModelTest"`
Expected: PASS, 15 tests.

- [ ] **Step 6: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestListModel.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/FakeArenaScheduler.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestListModelTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "feat: arena owner request-list model with freeze and minimum interactive window"
```

- [ ] **Step 7: Write the failing row-diff tests**

The spec's Android testing line asks for the adapter's diff to be unit-tested. The rows and the `DiffUtil.Callback` live in a plain-Java helper the Task 6 adapter calls. `DiffUtil` (RecyclerView 1.4.0, already an `implementation` dependency at `app/build.gradle:90`) references only `java.*` and its own package, so it runs in local JVM tests.

Create `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRowsTest.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Spec C2 rows: matched by player, so a snapshot change moves/removes/inserts rows and an
 *  in-flight mark rebinds one row in place. */
public class ArenaRequestRowsTest {

    private static ArenaJoinRequest req(String player, long seq) {
        return new ArenaJoinRequest(player, seq);
    }

    private static List<ArenaRequestRows.Row> rows(String inFlight, ArenaJoinRequest... rs) {
        return ArenaRequestRows.build(Arrays.asList(rs), inFlight);
    }

    /** Runs the diff and records each update DiffUtil dispatches to the adapter. */
    private static List<String> updates(List<ArenaRequestRows.Row> old, List<ArenaRequestRows.Row> next) {
        final List<String> out = new ArrayList<>();
        DiffUtil.calculateDiff(ArenaRequestRows.diff(old, next)).dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                out.add("insert " + position + " " + count);
            }

            @Override
            public void onRemoved(int position, int count) {
                out.add("remove " + position + " " + count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                out.add("move " + fromPosition + " " + toPosition);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                out.add("change " + position + " " + count);
            }
        });
        return out;
    }

    @Test
    public void build_keepsSnapshotOrder_andMarksOnlyTheInFlightPlayer() {
        List<ArenaRequestRows.Row> r = rows("carol", req("bob", 1), req("carol", 2));
        assertEquals("bob", r.get(0).player);
        assertEquals(1L, r.get(0).seq);
        assertFalse(r.get(0).inFlight);
        assertTrue(r.get(1).inFlight);
        for (ArenaRequestRows.Row row : rows(null, req("bob", 1), req("carol", 2))) {
            assertFalse(row.inFlight);
        }
    }

    @Test
    public void sameRows_dispatchNothing() {
        assertEquals(Collections.emptyList(),
                updates(rows(null, req("bob", 1)), rows(null, req("bob", 1))));
    }

    @Test
    public void inFlightMark_rebindsThatRowInPlace() {
        assertEquals(Collections.singletonList("change 0 1"),
                updates(rows(null, req("bob", 1), req("carol", 2)), rows("bob", req("bob", 1), req("carol", 2))));
    }

    @Test
    public void newSeqForTheSamePlayer_rebindsThatRow() {
        assertEquals(Collections.singletonList("change 0 1"),
                updates(rows(null, req("bob", 1)), rows(null, req("bob", 5))));
    }

    @Test
    public void answeredRow_isRemoved_andTheOthersAreNotRebound() {
        assertEquals(Collections.singletonList("remove 0 1"),
                updates(rows("bob", req("bob", 1), req("carol", 2)), rows(null, req("carol", 2))));
    }

    @Test
    public void newRequester_isInsertedAtTheEnd() {
        assertEquals(Collections.singletonList("insert 1 1"),
                updates(rows(null, req("bob", 1)), rows(null, req("bob", 1), req("carol", 2))));
    }

    @Test
    public void rowsAreMatchedByPlayer_notByPosition() {
        // bob leaves and dave arrives: carol keeps her view; nothing is rebound.
        List<String> u = updates(rows(null, req("bob", 1), req("carol", 2)),
                rows(null, req("carol", 2), req("dave", 3)));
        assertEquals(u.toString(), 2, u.size());
        assertTrue(u.toString(), u.contains("remove 0 1"));
        for (String s : u) {
            assertFalse(u.toString(), s.startsWith("change") || s.startsWith("move"));
        }
    }
}
```

- [ ] **Step 8: Run the tests to verify they fail**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaRequestRowsTest"`
Expected: FAIL — compilation error `cannot find symbol class ArenaRequestRows`.

- [ ] **Step 9: Implement**

Create `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRows.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import androidx.recyclerview.widget.DiffUtil;

import java.util.ArrayList;
import java.util.List;

/** The owner list's displayed rows and the diff between two renders (spec C2). Rows match by
 *  player, so a changed snapshot removes, inserts or moves rows instead of rebinding them all, and
 *  a row whose seq or in-flight mark changed is rebound in place. Plain Java (DiffUtil has no
 *  Android framework dependency), so ArenaRequestRowsTest runs it on the JVM. */
final class ArenaRequestRows {
    private ArenaRequestRows() {}

    static final class Row {
        final String player;
        final long seq;
        final boolean inFlight;

        Row(String player, long seq, boolean inFlight) {
            this.player = player;
            this.seq = seq;
            this.inFlight = inFlight;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Row)) return false;
            Row other = (Row) o;
            return seq == other.seq && inFlight == other.inFlight && player.equals(other.player);
        }

        @Override
        public int hashCode() {
            return (31 * player.hashCode() + Long.hashCode(seq)) * 2 + (inFlight ? 1 : 0);
        }
    }

    /** One row per request, in the given (seq) order; only {@code inFlightPlayer}'s row is marked. */
    static List<Row> build(List<ArenaJoinRequest> requests, String inFlightPlayer) {
        List<Row> out = new ArrayList<>();
        for (ArenaJoinRequest r : requests) {
            out.add(new Row(r.player, r.seq, r.player.equals(inFlightPlayer)));
        }
        return out;
    }

    static DiffUtil.Callback diff(final List<Row> old, final List<Row> next) {
        return new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return old.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return old.get(oldPosition).player.equals(next.get(newPosition).player);
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return old.get(oldPosition).equals(next.get(newPosition));
            }
        };
    }
}
```

- [ ] **Step 10: Run the tests to verify they pass**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaRequestRowsTest"`
Expected: PASS, 7 tests.

- [ ] **Step 11: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRows.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestRowsTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "feat: arena owner list rows and player-keyed diff"
```

---

### Task 4: Requester lobby model — requested marks, in-flight taps, withdraw guard

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaMyRequests.java`
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaMyRequestsTest.java`

**Interfaces:**
- Consumes: nothing (the `Set<Integer>` comes from `ArenaEventParser.myRequestTables`, Task 2).
- Produces:
  - `enum ArenaMyRequests.Tap { REQUEST, CONFIRM_WITHDRAW, IGNORE }`
  - `ArenaMyRequests.Tap onTap(int table)` — `REQUEST` also marks the table in flight
  - `boolean confirmWithdraw(int table)` — true means "send the withdraw now"; marks the table in flight
  - `void onMyRequests(Set<Integer> tables)` — replaces the requested set and clears all in-flight tables (spec C3 literal "next `dsgArenaMyRequestsEvent`"; the cross-table gap this leaves is the accepted limitation under Review Focus)
  - `boolean isRequested(int table)`

- [ ] **Step 1: Write the failing tests**

Create `ArenaMyRequestsTest.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Spec C3: marks follow the latest dsgArenaMyRequestsEvent; taps wait for it (R11). */
public class ArenaMyRequestsTest {

    private final ArenaMyRequests my = new ArenaMyRequests();

    private static Set<Integer> tables(Integer... t) {
        return new HashSet<>(Arrays.asList(t));
    }

    @Test
    public void tapOnAnUnmarkedTable_requestsOnce_thenIgnoresTapsUntilTheAnswer() {
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(5));
        assertEquals(ArenaMyRequests.Tap.IGNORE, my.onTap(5)); // a double-tap never sends twice
        my.onMyRequests(tables(5));
        assertTrue(my.isRequested(5));
        assertEquals(ArenaMyRequests.Tap.CONFIRM_WITHDRAW, my.onTap(5));
    }

    @Test
    public void anAnswerThatLeavesTheSetUnchanged_stillClearsInFlight() {
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(5));
        my.onMyRequests(tables()); // refused, e.g. BLOCKED
        assertFalse(my.isRequested(5));
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(5));
    }

    @Test
    public void otherTablesStayTappableWhileOneIsInFlight() {
        my.onTap(5);
        assertEquals(ArenaMyRequests.Tap.REQUEST, my.onTap(6));
    }

    @Test
    public void confirmWithdraw_sendsOnce_thenWaitsForTheAnswer() {
        my.onMyRequests(tables(5));
        assertTrue(my.confirmWithdraw(5));
        assertFalse(my.confirmWithdraw(5));
        assertEquals(ArenaMyRequests.Tap.IGNORE, my.onTap(5));
        my.onMyRequests(tables());
        assertFalse(my.isRequested(5));
    }

    @Test
    public void confirmWithdraw_afterTheRequestAlreadyEnded_sendsNothing() {
        my.onMyRequests(tables(5));
        // The confirm dialog is open; meanwhile the owner declines and the set drops table 5.
        my.onMyRequests(tables());
        assertFalse(my.confirmWithdraw(5));
    }

    @Test
    public void myRequestsReplacesTheWholeSet() {
        my.onMyRequests(tables(1, 2));
        my.onMyRequests(tables(2));
        assertFalse(my.isRequested(1));
        assertTrue(my.isRequested(2));
    }

    @Test
    public void laterChangesToTheCallersSetDoNotLeakIn() {
        Set<Integer> in = tables(3);
        my.onMyRequests(in);
        in.clear();
        assertTrue(my.isRequested(3));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaMyRequestsTest"`
Expected: FAIL — compilation error `cannot find symbol class ArenaMyRequests`.

- [ ] **Step 3: Implement**

Create `ArenaMyRequests.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import java.util.HashSet;
import java.util.Set;

/** Requester-side arena lobby state (spec C3). The requested set is the latest
 *  dsgArenaMyRequestsEvent. A tap sends at most one action per table until the next
 *  dsgArenaMyRequestsEvent arrives, which the server sends for every request and withdraw (R11). */
public final class ArenaMyRequests {

    public enum Tap { REQUEST, CONFIRM_WITHDRAW, IGNORE }

    private Set<Integer> requested = new HashSet<>();
    private final Set<Integer> inFlight = new HashSet<>();

    /** REQUEST: send the request now (the table is now in flight). CONFIRM_WITHDRAW: ask first.
     *  IGNORE: an action for this table still awaits the server. */
    public Tap onTap(int table) {
        if (inFlight.contains(table)) return Tap.IGNORE;
        if (requested.contains(table)) return Tap.CONFIRM_WITHDRAW;
        inFlight.add(table);
        return Tap.REQUEST;
    }

    /** The withdraw dialog was confirmed. True means send the withdraw. False when the request
     *  has already ended (or an action is in flight): a withdraw then would only block the table. */
    public boolean confirmWithdraw(int table) {
        if (inFlight.contains(table) || !requested.contains(table)) return false;
        inFlight.add(table);
        return true;
    }

    public void onMyRequests(Set<Integer> tables) {
        requested = new HashSet<>(tables);
        inFlight.clear();
    }

    public boolean isRequested(int table) {
        return requested.contains(table);
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaMyRequestsTest"`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaMyRequests.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaMyRequestsTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "feat: arena requester lobby state for requested marks and withdraw"
```

---

### Task 5: Ended-request notices (Toast) and their dispatch

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestNotice.java`
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestNoticeTest.java`
- Modify: `app/src/main/res/values/strings.xml:544` (insert after `arena_exit_table`)
- Modify: `app/src/main/res/values-de/strings.xml:306` (insert after `arena_exit_table`)
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java:303-304` (new branch before the `dsgJoinTableEvent` branch)

**Interfaces:**
- Consumes: `ArenaRequestEnded`, `ArenaEventParser.requestEnded(Map<String, Object>)` (Task 2).
- Produces:
  - `enum ArenaRequestNotice.Kind { DECLINED, TABLE_FULL, TABLE_CLOSED, DUPLICATE, BOOTED, BLOCKED, NOT_AVAILABLE, GUEST_RATED, NO_LONGER_AVAILABLE }`, each with `public final int resId`
  - `public final ArenaRequestNotice.Kind kind; public final Object[] args;`
  - `static ArenaRequestNotice ArenaRequestNotice.of(ArenaRequestEnded ended)` — null for an unknown or missing reason
  - String resources `arena_notice_declined`, `arena_notice_table_full`, `arena_notice_table_closed`, `arena_notice_duplicate`, `arena_notice_booted`, `arena_notice_blocked`, `arena_notice_not_available`, `arena_notice_guest_rated`, `arena_notice_no_longer_available`

- [ ] **Step 1: Write the failing tests**

Create `ArenaRequestNoticeTest.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Spec C4: one short notice per dsgArenaRequestEndedEvent reason. */
public class ArenaRequestNoticeTest {

    private static ArenaRequestEnded ended(String reason) {
        return new ArenaRequestEnded(7, "bob", "alice", reason);
    }

    @Test
    public void declined_namesTheOwner() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("DECLINED"));
        assertEquals(ArenaRequestNotice.Kind.DECLINED, n.kind);
        assertArrayEquals(new Object[]{"alice"}, n.args);
    }

    @Test
    public void duplicate_namesTheOwnerFromTheEvent() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("DUPLICATE"));
        assertEquals(ArenaRequestNotice.Kind.DUPLICATE, n.kind);
        assertArrayEquals(new Object[]{"alice"}, n.args);
    }

    @Test
    public void booted_namesTheOwnerAndTheTable() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("BOOTED"));
        assertEquals(ArenaRequestNotice.Kind.BOOTED, n.kind);
        assertArrayEquals(new Object[]{"alice", 7}, n.args);
    }

    @Test
    public void tableReasons_nameTheTable() {
        String[] reasons = {"TABLE_FULL", "TABLE_CLOSED", "BLOCKED", "NOT_AVAILABLE"};
        ArenaRequestNotice.Kind[] kinds = {
                ArenaRequestNotice.Kind.TABLE_FULL, ArenaRequestNotice.Kind.TABLE_CLOSED,
                ArenaRequestNotice.Kind.BLOCKED, ArenaRequestNotice.Kind.NOT_AVAILABLE};
        for (int i = 0; i < reasons.length; i++) {
            ArenaRequestNotice n = ArenaRequestNotice.of(ended(reasons[i]));
            assertEquals(reasons[i], kinds[i], n.kind);
            assertArrayEquals(reasons[i], new Object[]{7}, n.args);
        }
    }

    @Test
    public void guestRated_hasNoArguments() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("GUEST_RATED"));
        assertEquals(ArenaRequestNotice.Kind.GUEST_RATED, n.kind);
        assertEquals(0, n.args.length);
    }

    @Test
    public void noLongerAvailable_namesTheRequester() {
        ArenaRequestNotice n = ArenaRequestNotice.of(ended("NO_LONGER_AVAILABLE"));
        assertEquals(ArenaRequestNotice.Kind.NO_LONGER_AVAILABLE, n.kind);
        assertArrayEquals(new Object[]{"bob"}, n.args);
    }

    @Test
    public void unknownOrMissingReason_givesNoNotice() {
        assertNull(ArenaRequestNotice.of(ended("SOMETHING_NEWER")));
        assertNull(ArenaRequestNotice.of(ended(null)));
        assertNull(ArenaRequestNotice.of(null));
    }

    @Test
    public void everyKindHasAString() {
        for (ArenaRequestNotice.Kind k : ArenaRequestNotice.Kind.values()) {
            assertNotEquals(k.name(), 0, k.resId);
        }
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaRequestNoticeTest"`
Expected: FAIL — compilation error `cannot find symbol class ArenaRequestNotice`.

- [ ] **Step 3: Add the strings**

In `app/src/main/res/values/strings.xml`, insert after line 544 (`<string name="arena_exit_table">Exit table</string>`):

```xml
    <string name="arena_notice_declined">%1$s declined your request</string>
    <string name="arena_notice_table_full">Table %1$d is no longer available</string>
    <string name="arena_notice_table_closed">Table %1$d was closed</string>
    <string name="arena_notice_duplicate">You already sent a request to %1$s</string>
    <string name="arena_notice_booted">%1$s removed you from table %2$d, try again in a few minutes</string>
    <string name="arena_notice_blocked">You can\'t request table %1$d again until its next game</string>
    <string name="arena_notice_not_available">You can\'t join table %1$d right now</string>
    <string name="arena_notice_guest_rated">Guests can\'t join rated tables</string>
    <string name="arena_notice_no_longer_available">%1$s is no longer available</string>
```

In `app/src/main/res/values-de/strings.xml`, insert after line 306 (`<string name="arena_exit_table">Tisch verlassen</string>`):

```xml
    <string name="arena_notice_declined">%1$s hat deine Anfrage abgelehnt</string>
    <string name="arena_notice_table_full">Tisch %1$d ist nicht mehr verfügbar</string>
    <string name="arena_notice_table_closed">Tisch %1$d wurde geschlossen</string>
    <string name="arena_notice_duplicate">Du hast %1$s bereits eine Anfrage geschickt</string>
    <string name="arena_notice_booted">%1$s hat dich von Tisch %2$d entfernt, versuche es in ein paar Minuten noch einmal</string>
    <string name="arena_notice_blocked">Du kannst Tisch %1$d erst nach seinem nächsten Spiel wieder anfragen</string>
    <string name="arena_notice_not_available">Du kannst Tisch %1$d gerade nicht beitreten</string>
    <string name="arena_notice_guest_rated">Gäste können gewerteten Tischen nicht beitreten</string>
    <string name="arena_notice_no_longer_available">%1$s ist nicht mehr verfügbar</string>
```

- [ ] **Step 4: Implement the mapping**

Create `ArenaRequestNotice.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import androidx.annotation.StringRes;

import be.submanifold.pentelive.R;

/** The short notice shown for a dsgArenaRequestEndedEvent (spec C4). A reason this build does
 *  not know gives no notice, so a newer server cannot crash or confuse an older client. */
public final class ArenaRequestNotice {

    public enum Kind {
        DECLINED(R.string.arena_notice_declined),
        TABLE_FULL(R.string.arena_notice_table_full),
        TABLE_CLOSED(R.string.arena_notice_table_closed),
        DUPLICATE(R.string.arena_notice_duplicate),
        BOOTED(R.string.arena_notice_booted),
        BLOCKED(R.string.arena_notice_blocked),
        NOT_AVAILABLE(R.string.arena_notice_not_available),
        GUEST_RATED(R.string.arena_notice_guest_rated),
        NO_LONGER_AVAILABLE(R.string.arena_notice_no_longer_available);

        @StringRes
        public final int resId;

        Kind(@StringRes int resId) {
            this.resId = resId;
        }
    }

    public final Kind kind;
    /** Format arguments for {@code getString(kind.resId, args)}. */
    public final Object[] args;

    private ArenaRequestNotice(Kind kind, Object... args) {
        this.kind = kind;
        this.args = args;
    }

    public static ArenaRequestNotice of(ArenaRequestEnded ended) {
        if (ended == null || ended.reason == null) return null;
        switch (ended.reason) {
            case "DECLINED":
                return new ArenaRequestNotice(Kind.DECLINED, ended.owner);
            case "TABLE_FULL":
                return new ArenaRequestNotice(Kind.TABLE_FULL, ended.table);
            case "TABLE_CLOSED":
                return new ArenaRequestNotice(Kind.TABLE_CLOSED, ended.table);
            case "DUPLICATE":
                // owner comes from the event, never from the lobby's table model
                return new ArenaRequestNotice(Kind.DUPLICATE, ended.owner);
            case "BOOTED":
                return new ArenaRequestNotice(Kind.BOOTED, ended.owner, ended.table);
            case "BLOCKED":
                return new ArenaRequestNotice(Kind.BLOCKED, ended.table);
            case "NOT_AVAILABLE":
                return new ArenaRequestNotice(Kind.NOT_AVAILABLE, ended.table);
            case "GUEST_RATED":
                return new ArenaRequestNotice(Kind.GUEST_RATED);
            case "NO_LONGER_AVAILABLE":
                return new ArenaRequestNotice(Kind.NO_LONGER_AVAILABLE, ended.player);
            default:
                return null;
        }
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.ArenaRequestNoticeTest"`
Expected: PASS, 8 tests.

- [ ] **Step 6: Dispatch the event**

In `LiveGameRoomActivity.java`, the old arena branch ends at line 303 and line 304 is `} else if (jsonEvent.get("dsgJoinTableEvent") != null) {`. Replace that line 304 text

```java
                                } else if (jsonEvent.get("dsgJoinTableEvent") != null) {
```

with

```java
                                } else if (jsonEvent.get("dsgArenaRequestEndedEvent") != null) {
                                    // The server addresses this only to the player who needs it (spec C4).
                                    ArenaRequestNotice notice = ArenaRequestNotice.of(ArenaEventParser.requestEnded(
                                            (Map<String, Object>) jsonEvent.get("dsgArenaRequestEndedEvent")));
                                    if (notice != null) {
                                        Toast.makeText(LiveGameRoomActivity.this,
                                                getString(notice.kind.resId, notice.args), Toast.LENGTH_LONG).show();
                                    }
                                } else if (jsonEvent.get("dsgJoinTableEvent") != null) {
```

- [ ] **Step 7: Build**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestNotice.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/ArenaRequestNoticeTest.java app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "feat: show a Toast for each arena request-ended reason"
```

---

### Task 6: Owner dialog renders the snapshot model (countdown removed)

No JVM test is possible for this glue (no Robolectric; `RecyclerView`/`Handler` are stubs in local unit tests). All behavior it relies on is pinned by Task 3; this task is gated by the build, the full unit suite, and the Task 8 E2E.

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaHandlerScheduler.java`
- Rewrite: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequestAdapter.java:1-155`
- Rewrite: `app/src/main/res/layout/arena_join_request_row.xml`
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java` imports (`:19`, `:39`), fields and `setArena` (`:104-110`), `onViewCreated` arena block (`:236-245`), `showArenaJoinRequest` / `dismissArenaJoinRequest` / `arenaTableRequestJoinEvent` (`:887-955`), `onDestroyView` (`:1580-1584`)
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java:291-303` (replace the `dsgArenaRequestJoinTableEvent` branch)

**Interfaces:**
- Consumes: `ArenaRequestListModel` (incl. `onShown()`) and its `Scheduler`/`Cancellable`/`Listener`, `ArenaRequestRows.build`/`diff` (Task 3), `ArenaJoinRequest`, `ArenaEventParser.table`, `ArenaEventParser.joinRequests` (Task 2), `ArenaEvents.accept`, `ArenaEvents.reject` (Task 1).
- Produces:
  - `final class ArenaHandlerScheduler implements ArenaRequestListModel.Scheduler` (no-arg constructor)
  - `ArenaJoinRequestAdapter(LayoutInflater, TablesAndPlayers, ArenaRequestListModel, ActionSender, String me, int tableId, int gameId)`; `void render()`; `String playerAt(int)`; `boolean accept(int)`; `boolean reject(int)`; `void restoreSwipedRow(int)`
  - `boolean LiveTableFragment.arenaJoinRequestsEvent(List<ArenaJoinRequest> requests)` (replaces `arenaTableRequestJoinEvent(String)`, which is deleted)

- [ ] **Step 1: Add the Handler-backed scheduler**

Create `ArenaHandlerScheduler.java`:

```java
package be.submanifold.pentelive.liveGameRoom;

import android.os.Handler;
import android.os.Looper;

/** Runs ArenaRequestListModel timers on the main thread, where the model and its views live. */
final class ArenaHandlerScheduler implements ArenaRequestListModel.Scheduler {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public ArenaRequestListModel.Cancellable schedule(Runnable task, long delayMs) {
        // A fresh wrapper per call, so cancel() removes exactly this posting.
        final Runnable posted = task::run;
        handler.postDelayed(posted, delayMs);
        return () -> handler.removeCallbacks(posted);
    }
}
```

- [ ] **Step 2: Rewrite the row layout**

Replace the whole of `app/src/main/res/layout/arena_join_request_row.xml` with (the 6 s countdown `ProgressBar` `requestProgress` is gone; `requestSpinner` shows while an accept/decline awaits the server):

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingHorizontal="16dp"
    android:paddingVertical="12dp">

    <TextView
        android:id="@+id/requestPlayerName"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:textColor="@color/colorPrimary"
        android:textSize="16sp" />

    <TextView
        android:id="@+id/requestPlayerRating"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:textColor="@color/colorPrimary" />

    <ProgressBar
        android:id="@+id/requestSpinner"
        style="?android:attr/progressBarStyleSmall"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:indeterminate="true"
        android:visibility="gone" />
</LinearLayout>
```

- [ ] **Step 3: Rewrite the adapter**

Replace the whole of `ArenaJoinRequestAdapter.java` with:

```java
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
```

- [ ] **Step 4: Fragment imports**

In `LiveTableFragment.java`, after line 19 (`import androidx.appcompat.widget.Toolbar;`) add:

```java
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
```

After line 39 (`import android.widget.LinearLayout;`) add:

```java
import android.widget.FrameLayout;
import android.widget.ProgressBar;
```

(`Gravity`, `MotionEvent`, `View`, `ViewGroup`, `List` are already imported at `:24`, `:28-30`, `:46`.)

- [ ] **Step 5: Fragment fields and `setArena`**

Replace lines 104-110:

```java
    public boolean isArenaTable = false;
    private ArenaJoinRequestAdapter arenaAdapter;
    private AlertDialog arenaRequestDialog;

    public void setArena(boolean isArena) {
        this.isArenaTable = isArena;
    }
```

with:

```java
    public boolean isArenaTable = false;
    private ArenaRequestListModel arenaModel;
    private ArenaJoinRequestAdapter arenaAdapter;
    private AlertDialog arenaRequestDialog;
    private RecyclerView arenaList;
    private ProgressBar arenaFreezeSpinner;

    public void setArena(boolean isArena) {
        this.isArenaTable = isArena;
        if (isArena && arenaModel == null) {
            // Created before the view: a snapshot that lands before onViewCreated is kept and shown then.
            arenaModel = new ArenaRequestListModel(new ArenaHandlerScheduler(), this::onArenaRequestsChanged);
        }
    }
```

- [ ] **Step 6: Fragment `onViewCreated` arena block**

Replace lines 236-245:

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

with:

```java
        if (isArenaTable) {
            arenaAdapter = new ArenaJoinRequestAdapter(
                    activity.getLayoutInflater(),
                    activity.tablesAndPlayers,
                    arenaModel,
                    activity::sendEvent,
                    me, table.getId(), table.getGame());
            arenaAdapter.render();
            if (table.getPlayers().size() <= 1) {
                showArenaJoinRequest();
            }
        }
```

- [ ] **Step 7: Fragment dialog, dismiss and snapshot entry point**

Replace lines 887-955 (`showArenaJoinRequest`, `dismissArenaJoinRequest`, `arenaTableRequestJoinEvent`) with:

```java
    public void showArenaJoinRequest() {
        if (!isArenaTable || arenaAdapter == null) return;
        if (arenaRequestDialog != null && arenaRequestDialog.isShowing()) return;

        arenaList = new RecyclerView(activity);
        arenaList.setLayoutManager(new LinearLayoutManager(activity));
        arenaList.setAdapter(arenaAdapter);
        arenaFreezeSpinner = new ProgressBar(activity);
        arenaFreezeSpinner.setIndeterminate(true);
        FrameLayout container = new FrameLayout(activity);
        container.addView(arenaList, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.addView(arenaFreezeSpinner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // tap = accept (single tap only, so scroll/fling end does not accept)
        final android.view.GestureDetector tapDetector =
                new android.view.GestureDetector(activity, new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return true;
                    }
                });
        arenaList.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (!arenaModel.isInteractive()) {
                    // Frozen, or an accept/decline awaits the server: swallow the gesture.
                    return true;
                }
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && tapDetector.onTouchEvent(e)) {
                    arenaAdapter.accept(rv.getChildAdapterPosition(child));
                    return true;
                }
                return false;
            }
        });

        // swipe = decline
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh) {
                return arenaModel.isInteractive() ? super.getSwipeDirs(rv, vh) : 0;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                // No local removal. On success the model marks the row in flight and the adapter
                // rebinds it with a spinner, which brings the swiped view back. If nothing was sent
                // (stale position, or the list froze mid-swipe), put the view back here.
                int position = vh.getBindingAdapterPosition();
                if (!arenaAdapter.reject(position)) {
                    arenaAdapter.restoreSwipedRow(position);
                }
            }
        }).attachToRecyclerView(arenaList);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.arena_tap_to_accept));
        builder.setView(container);
        builder.setNegativeButton(getString(R.string.arena_exit_table), (dialog, which) ->
                activity.sendEvent("{\"dsgExitTableEvent\":{\"forced\":false,\"table\":" + table.getId() + ",\"booted\":false,\"time\":0}}"));
        arenaRequestDialog = builder.create();
        arenaRequestDialog.setCancelable(false);
        arenaRequestDialog.setCanceledOnTouchOutside(false);
        arenaRequestDialog.show();
        // From now on every snapshot freezes first, including the first row onto the empty list
        // (it grows the dialog and moves "Exit table"). dismissArenaJoinRequest's reset() undoes this.
        arenaModel.onShown();
        renderArenaFreeze();
    }

    public void dismissArenaJoinRequest() {
        if (arenaModel != null) arenaModel.reset();
        if (arenaRequestDialog != null) {
            arenaRequestDialog.dismiss();
            arenaRequestDialog = null;
        }
        arenaList = null;
        arenaFreezeSpinner = null;
    }

    /** dsgArenaJoinRequestsEvent for this table. Returns true when the snapshot holds a player who
     *  was not in the previous one, so the caller plays the new-request sound. */
    public boolean arenaJoinRequestsEvent(List<ArenaJoinRequest> requests) {
        return arenaModel != null && arenaModel.onSnapshot(requests);
    }

    private void onArenaRequestsChanged() {
        if (arenaAdapter == null) return; // no view yet: onViewCreated renders the model
        arenaAdapter.render();
        if (!arenaModel.rows().isEmpty() && table.getPlayers().size() <= 1) {
            showArenaJoinRequest(); // no-op while it is already showing
        }
        renderArenaFreeze();
    }

    private void renderArenaFreeze() {
        if (arenaList == null || arenaFreezeSpinner == null) return;
        boolean frozen = arenaModel.isFrozen();
        // Empty snapshot: the list hides; the dialog stays as the owner's waiting screen with "Exit table".
        // While the first rows are held the list is still hidden and only the spinner shows.
        arenaList.setVisibility(arenaModel.rows().isEmpty() ? View.GONE : View.VISIBLE);
        arenaList.setAlpha(frozen ? 0.4f : 1f);
        arenaFreezeSpinner.setVisibility(frozen ? View.VISIBLE : View.GONE);
    }
```

- [ ] **Step 8: Fragment `onDestroyView`**

Replace lines 1580-1584 (line numbers shift after Steps 4-7; match the text):

```java
    @Override
    public void onDestroyView() {
        dismissArenaJoinRequest();
        super.onDestroyView();
    }
```

with:

```java
    @Override
    public void onDestroyView() {
        dismissArenaJoinRequest();
        arenaAdapter = null; // the model may still get snapshots; nothing renders without a view
        super.onDestroyView();
    }
```

- [ ] **Step 9: Activity dispatch for the snapshot**

In `LiveGameRoomActivity.java` replace lines 291-303 (the whole `dsgArenaRequestJoinTableEvent` branch, which the server no longer sends):

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
                                        // Play the new-player sound when you receive a join request for your arena table.
                                        playSound(NEW_PLAYER_SOUND);
                                        fragment.arenaTableRequestJoinEvent(player);
                                    }
```

with:

```java
                                } else if (jsonEvent.get("dsgArenaJoinRequestsEvent") != null) {
                                    Map<String, Object> data =
                                            (Map<String, Object>) jsonEvent.get("dsgArenaJoinRequestsEvent");
                                    int tableId = ArenaEventParser.table(data);
                                    LiveTableFragment fragment = (LiveTableFragment)
                                            getSupportFragmentManager().findFragmentByTag("liveTable");
                                    if (fragment != null && fragment.table != null
                                            && fragment.table.getId() == tableId
                                            && fragment.arenaJoinRequestsEvent(ArenaEventParser.joinRequests(data))) {
                                        // New-request cue: the snapshot holds a player who was not in the previous one.
                                        playSound(NEW_PLAYER_SOUND);
                                    }
```

- [ ] **Step 10: Check nothing references the removed pieces**

Run: `grep -rnw "TIMEOUT_MS\|TICK_MS\|requestProgress\|arenaTableRequestJoinEvent\|dsgArenaRequestJoinTableEvent\") != null" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/src/main/java/be/submanifold/pentelive/liveGameRoom/Arena*.java /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/src/main/res/layout/arena_join_request_row.xml; grep -rn "arenaAdapter.addPlayer" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/src/main/java`
Expected: no output. Before this task the same command lists exactly the code this task removes: `ArenaJoinRequestAdapter.java:27,28,49,57,80,135,152`, `arena_join_request_row.xml:31`, `LiveTableFragment.java:949,954`, `LiveGameRoomActivity.java:291,302`. (The `") != null` suffix spares the C→S request name `ArenaEvents.java:22` still sends; `-w` keeps `SOCKET_CONNECT_TIMEOUT_MS`/`LIVE_READ_TIMEOUT_MS` in `LiveGameRoomActivity.java:52,55` from matching; unrelated `addPlayer(` callers such as `LiveGameRoom.java:34` and `LobbyActivity.java:228` are out of scope.)

- [ ] **Step 11: Build and run the unit suite**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 12: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaHandlerScheduler.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/ArenaJoinRequestAdapter.java app/src/main/res/layout/arena_join_request_row.xml app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "feat: arena owner list renders server snapshots with freeze, drop the 6s countdown"
```

---

### Task 7: Requester lobby — "Requested" mark, request gating, withdraw

No JVM test is possible for this glue; the rules it calls are pinned by Task 4.

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java:60` (field), and the dispatch chain (new branch before the `dsgArenaRequestEndedEvent` branch added in Task 5)
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomFragment.java:118-128` (child click), add `onArenaTableTapped`
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/TableListAdapter.java:132-157` (`getChildView`)
- Modify: `app/src/main/res/layout/live_table_row.xml:40-46` (add `requestedText` after `watchingText`)
- Modify: `app/src/main/res/values/strings.xml:543` (remove `arena_waiting`; add mark/withdraw strings), `app/src/main/res/values-de/strings.xml:305` (same)

**Interfaces:**
- Consumes: `ArenaMyRequests` (Task 4), `ArenaEventParser.myRequestTables` (Task 2), `ArenaEvents.requestJoin`, `ArenaEvents.withdraw` (Task 1).
- Produces: `public final ArenaMyRequests LiveGameRoomActivity.arenaMyRequests`; string resources `arena_requested`, `arena_withdraw`, `arena_withdraw_confirm`.

- [ ] **Step 1: Strings**

In `app/src/main/res/values/strings.xml` replace line 543:

```xml
    <string name="arena_waiting">Waiting for host to accept…</string>
```

with:

```xml
    <string name="arena_requested">Requested</string>
    <string name="arena_withdraw">Withdraw</string>
    <string name="arena_withdraw_confirm">Withdraw your request to table %1$d? You won\'t be able to request it again until its next game.</string>
```

In `app/src/main/res/values-de/strings.xml` replace line 305:

```xml
    <string name="arena_waiting">Warte auf Bestätigung des Gastgebers…</string>
```

with:

```xml
    <string name="arena_requested">Angefragt</string>
    <string name="arena_withdraw">Zurückziehen</string>
    <string name="arena_withdraw_confirm">Deine Anfrage an Tisch %1$d zurückziehen? Du kannst ihn erst nach seinem nächsten Spiel wieder anfragen.</string>
```

- [ ] **Step 2: Row layout**

In `app/src/main/res/layout/live_table_row.xml`, after the `watchingText` `TextView` (ends at line 46, before `</RelativeLayout>`), add:

```xml
    <TextView android:id="@+id/requestedText"
        android:textSize="15sp"
        android:textStyle="bold"
        android:gravity="center_vertical"
        android:layout_below="@id/watchingText"
        android:layout_marginStart="10dp"
        android:text="@string/arena_requested"
        android:visibility="gone"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
```

- [ ] **Step 3: Mark in `TableListAdapter.getChildView`**

In `TableListAdapter.java`, replace lines 155-157:

```java
        settingsTextView.setText(table.getSettingsText());
        watchingTextView.setText(table.getWatchingText(watchingTextView.getLineHeight()));
        return convertView;
```

with:

```java
        settingsTextView.setText(table.getSettingsText());
        watchingTextView.setText(table.getWatchingText(watchingTextView.getLineHeight()));
        TextView requestedTextView = convertView.findViewById(R.id.requestedText);
        requestedTextView.setTextColor(primaryColor);
        // Tables in the latest dsgArenaMyRequestsEvent show the "Requested" mark (spec C3).
        requestedTextView.setVisibility(isArena && activity.arenaMyRequests.isRequested(table.getId())
                ? View.VISIBLE : View.GONE);
        return convertView;
```

- [ ] **Step 4: Activity field and dispatch**

In `LiveGameRoomActivity.java`, after line 60 (`public TablesAndPlayers tablesAndPlayers = new TablesAndPlayers();`) add:

```java
    public final ArenaMyRequests arenaMyRequests = new ArenaMyRequests();
```

Replace the line

```java
                                } else if (jsonEvent.get("dsgArenaRequestEndedEvent") != null) {
```

with:

```java
                                } else if (jsonEvent.get("dsgArenaMyRequestsEvent") != null) {
                                    arenaMyRequests.onMyRequests(ArenaEventParser.myRequestTables(
                                            (Map<String, Object>) jsonEvent.get("dsgArenaMyRequestsEvent")));
                                    updateMainRoom();
                                } else if (jsonEvent.get("dsgArenaRequestEndedEvent") != null) {
```

- [ ] **Step 5: Lobby tap**

In `LiveGameRoomFragment.java` replace lines 118-128:

```java
        expandableList.setOnChildClickListener((expandableListView, view12, i, i1, l) -> {
            int tableId = tableListAdapter.getTablesArray().get(i1).getId();
            if (roomName != null && roomName.toLowerCase(java.util.Locale.ROOT).contains("arena")) {
                activity.sendEvent(ArenaEvents.requestJoin(activity.getMe(), tableId));
                android.widget.Toast.makeText(activity, getString(R.string.arena_waiting),
                        android.widget.Toast.LENGTH_SHORT).show();
            } else {
                activity.sendEvent("{\"dsgJoinTableEvent\":{\"table\":" + tableId + ",\"time\":0}}");
            }
            return true;
        });
```

with:

```java
        expandableList.setOnChildClickListener((expandableListView, view12, i, i1, l) -> {
            int tableId = tableListAdapter.getTablesArray().get(i1).getId();
            if (isArena) {
                onArenaTableTapped(tableId);
            } else {
                activity.sendEvent("{\"dsgJoinTableEvent\":{\"table\":" + tableId + ",\"time\":0}}");
            }
            return true;
        });
```

(`isArena` is the effectively-final local declared at line 99.)

Then add this method after `updateMainRoom()` (after line 167):

```java
    /** Spec C3: an unmarked table sends one request; a marked table asks before withdrawing; a
     *  table whose action still awaits dsgArenaMyRequestsEvent ignores the tap. */
    private void onArenaTableTapped(final int tableId) {
        switch (activity.arenaMyRequests.onTap(tableId)) {
            case REQUEST:
                activity.sendEvent(ArenaEvents.requestJoin(activity.getMe(), tableId));
                break;
            case CONFIRM_WITHDRAW:
                new AlertDialog.Builder(activity)
                        .setMessage(getString(R.string.arena_withdraw_confirm, tableId))
                        .setPositiveButton(getString(R.string.arena_withdraw), (dialog, which) -> {
                            if (activity.arenaMyRequests.confirmWithdraw(tableId)) {
                                activity.sendEvent(ArenaEvents.withdraw(activity.getMe(), tableId));
                            }
                        })
                        // android.R.string.cancel, as ArenaTableSetupDialog.java:71 does: the app's
                        // R.string.cancel is "Löschen" (Delete) in values-de/strings.xml:107.
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
            case IGNORE:
            default:
                break;
        }
    }
```

- [ ] **Step 6: Check `arena_waiting` is gone everywhere**

Run: `grep -rn "arena_waiting" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/src`
Expected: no output.

- [ ] **Step 7: Build and run the unit suite**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomFragment.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/TableListAdapter.java app/src/main/res/layout/live_table_row.xml app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android commit -S -m "feat: arena lobby shows requested tables and lets the requester withdraw"
```

---

### Task 8: Verification — full suite and E2E on the local stack

**Files:** none changed (except the temporary, never-committed `PentePlayer.development` flip in Step 3).

**Interfaces:**
- Consumes: everything above; the server side of the spec deployed on the local pente.org stack (`pente.org` branch for this feature, deployed with `./justCompile`, which compiles and restarts the container); two React instances from the `react_live_game_room` plan.
- Produces: evidence lines for the final report.

- [ ] **Step 1: Full unit suite and build**

Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android clean :app:testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`. Then count results:
Run: `grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/build/test-results/testDebugUnitTest/TEST-be.submanifold.pentelive.liveGameRoom.Arena*.xml`
Expected: six lines (ArenaEventsTest 5, ArenaEventParserTest 10, ArenaRequestListModelTest 15, ArenaRequestRowsTest 7, ArenaMyRequestsTest 7, ArenaRequestNoticeTest 8), all with `failures="0" errors="0"`.

- [ ] **Step 2: Confirm no countdown remains**

Run: `grep -rn "6000\|TIMEOUT_MS\|postDelayed" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/app/src/main/java/be/submanifold/pentelive/liveGameRoom/Arena*.java`
Expected: only `ArenaHandlerScheduler.java` (`handler.postDelayed(posted, delayMs)`).

- [ ] **Step 3: Point the app at the local backend (do not commit)**

Set `public static Boolean development = true;` at `app/src/main/java/be/submanifold/pentelive/PentePlayer.java:36` (the emulator then connects to `10.0.2.2`, `LiveGameRoomActivity.java:127`). Install on a running emulator:
Run: `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android installDebug`
Expected: `BUILD SUCCESSFUL`, "Installed on 1 device".

Setup for the scenarios: the local stack runs the server side of this feature; React instance A and B run with `PUBLIC_LOCAL_BACKEND=1` in `react_live_game_room/.env.local` and different identities (the test accounts are the `TEST_USER_A` / `TEST_USER_B` keys in `pente-react-native/.env.local`; never print or commit them; second instance via per-process `REACT_APP_USERNAME`/`REACT_APP_PASSWORD` and `npx rsbuild dev --port 3002`). The Android app logs in as a third player (a guest works for unrated tables). Everyone enters the Arena room. Create unrated tables for every scenario unless it says otherwise.

- [ ] **Step 4: E2E scenarios (spec "Testing", the ones Android takes part in)**

Record a pass/fail line for each.

1. **Decline no longer disconnects (Android owner).** Android creates a table (the dialog opens with only "Exit table"). React A requests it. The centred spinner shows for ~400 ms, then the Android dialog shows A's row (sound plays once). Swipe A's row: the row comes back with a spinner, then after ~400 ms the list hides; Android stays connected and its table stays open. React A sees "<android> declined your request". React A requests again → A sees the BLOCKED notice; Android's list does not change.
2. **Request → decline → re-request refused (Android requester).** React A creates a table. Android taps it once: the "Requested" mark appears; a quick second tap sends nothing (no DUPLICATE Toast). React A declines → Android Toast "<A> declined your request", mark disappears. Android taps again → Toast "You can't request table <n> again until its next game".
3. **Withdraw → re-request refused.** React A creates a table; Android requests it (mark shown). Tap the marked row → confirm dialog with the spec text → Withdraw → mark clears; React A's list drops Android. Tap the table again → BLOCKED Toast.
4. **Freeze and burst (Android owner).** Android creates a table. React A and B request it within one second, then A withdraws: every list change, including A's first row onto the empty dialog, is preceded by the centred spinner (and dims any rows already shown); changes arriving while the list is tappable are held and shown together at the next freeze; taps during the spinner do nothing; after each unfreeze the list stays tappable for at least about a second. Repeat while tapping "Exit table" the instant A requests: the tap exits the table and never accepts A.
5. **Accept → others cancelled.** Android creates a table; A and B request it. Tap A: A's row shows a spinner and all taps are ignored; A joins and the dialog closes. B gets "Table <n> is no longer available" and B's request mark/state clears. Variant with Android as a requester at two tables: when one owner accepts Android, Android's other "Requested" mark disappears with no Toast.
6. **Game ends, table open → previously blocked player can request.** After scenario 1 (A blocked at Android's table), Android and B play to the end, B leaves: Android's table is open again (fresh empty snapshot, no rows); A requests → Android sees A's row. (Scenarios 6, 8 and 9 check that the table reopens and takes requests. They cannot fail on R6b's memory wipe alone: R6 already wiped A's block when the table reached 2 players, and a not-open table blocks nobody. R6b's wipe is covered by the server unit tests.)
7. **Table close (Android requester).** Android requests React A's table; A exits the table → Android Toast "Table <n> was closed", mark disappears.
8. **Owner leaves a finished 2-player table.** React A owns, Android joins via accept, game ends, A leaves → Android becomes owner, the table is open, a player A had blocked can request and Android sees the row.
9. **Waiting set cancelled or force-resigned.** Android at a 2-player game with A; A disconnects mid-game; after the return timeout Android cancels (or force-resigns) the set → the table reopens; a previously blocked player can request and Android sees the row.
10. **Requester disconnect (Android owner).** A requests Android's table, then closes the React tab → after the freeze A's row is gone, with no Toast on Android.
11. **Owner exits right after accept (Android requester).** Android requests A's table; A accepts and immediately exits → Android gets "Table <n> was closed" (or joins and then sees A leave, depending on server timing) and is never left with a stuck mark.

- [ ] **Step 5: Restore and tidy**

Set `development` back to `false` at `PentePlayer.java:36`.
Run: `git -C /Users/waliedothman/mariposa/coding/pente.org-project/pentelive-android status --porcelain`
Expected: no output (the flag is reverted, nothing untracked; this plan file itself is already committed on the branch). Remove any scratch under `/private/tmp/claude-501/arena-android/`.

- [ ] **Step 6: Report**

Report the Step 1 test counts, the Step 4 pass/fail lines, and any scenario that could not be run (with the reason, e.g. server side not yet deployed). No commit in this task.
