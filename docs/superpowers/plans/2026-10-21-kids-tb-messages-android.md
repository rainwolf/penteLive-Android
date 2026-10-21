# Kids: Turn-Based Messages for Parents (Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On a kid's detail screen, a parent can switch on the "Turn-based game messages" transcript email, and can see the kid's active turn-based (TB) games. Tapping a game opens the existing board read-only, with the kid's and the opponent's messages, and gives the parent no way to write to the game.

**Architecture:** All parsing and form building live in the pure-Java `kids/KidAccounts` model (JVM unit tests, no Robolectric). The read-only `Game` factory lives in a separate small helper `kids/KidGames`, because `Game` is an Android class (`Parcelable`, `Context`, `R`) and `KidAccounts` stays free of `android.*`. `KidAccountsClient` gains one GET call. `KidDetailActivity` stays a thin view: it gets a third switch and an active-games list that opens `BoardActivity` with a `Game` built by `setActive(false)`. The switch and the whole list section are hidden on a server without the feature. Message display needs no change. The server now returns messages to a parent, and `Game.parseGame` already renders whatever `messages`/`messageNums` arrive. The audit found one client path where a non-player could be offered a write: the cancel-request Accept/Decline prompt. `Game.parseGame` gets a pure guard for it, unit-tested.

**Tech Stack:** Java (minSdk 26), AndroidX AppCompat + RecyclerView + SwitchCompat, Gson (`net.Json.GSON`), JUnit 4 local unit tests (`returnDefaultValues = true`), Gradle through the Android Studio JBR.

**Spec:** `/Users/waliedothman/mariposa/coding/pente.org-project/pente.org/docs/superpowers/specs/2026-10-21-kids-tb-messages-design.md` (approved; sections "Goals", "Active games list", "Transcript email", "Clients"). The wire contract is **only** the "Server API contract" section of `/Users/waliedothman/mariposa/coding/pente.org-project/pente.org/docs/superpowers/plans/2026-10-21-kids-tb-messages-server.md`. Executors read both. Background: `docs/superpowers/plans/2026-10-19-kids-accounts-android.md` (the shipped kids screens this plan extends).

**Base:** `main` at `ee616c8`. Every `file:line` below was checked against that commit. Paths are relative to `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/`. Earlier tasks shift lines, so match on the quoted code, not the number.

## Global Constraints

- Repo `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android`. Create branch `kids-tb-messages` from an up-to-date `main`: `git -C <repo> checkout main && git -C <repo> pull --ff-only && git -C <repo> checkout -b kids-tb-messages`. Always use `git -C <repo>`, never a git command that depends on the cwd.
- Commits: `git -C <repo> commit -S -m "..."`. Stage explicit paths only, never `git add -A` or `git add .`. No co-author or generated-by lines. After each commit, read the diffstat and check the file count.
- Gradle needs JDK 17+. `GRADLE <task>` means exactly `env TMPDIR=/private/tmp/claude-501/kids-tb JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android <task>`. Always run the full form; the default `JAVA_HOME` is JDK 8 and fails. Create `/private/tmp/claude-501/kids-tb` first.
- Scratch, E2E scripts, screenshots and logcat dumps go only under `/private/tmp/claude-501/kids-tb/android/`, never in the repo. Gradle output (`app/build`, `build/`, `.gradle`) is gitignored. Task 4 still removes it with `GRADLE clean` at the end. Before each commit, `git -C <repo> status --porcelain | grep '^??'` must list only files this plan creates.
- Never commit `PentePlayer.development = true` (`app/src/main/java/be/submanifold/pentelive/PentePlayer.java:36`).
- Endpoint additions consumed (contract, verbatim):
  - `GET /gameServer/kids`: each `kids[]` entry gains `"copyTbMessages": bool`. A server without the feature omits the field, and clients may hide the toggle.
  - `POST /gameServer/kids` `action=settings`: new optional param `copyTbMessages` (`Y` or `true` is on). A missing param keeps the stored value. The reply is the GET body.
  - `GET /gameServer/kids?action=activeGames&kidPid=<pid>` returns 200 `{"success":true,"kidPid":<long>,"games":[{"gid":<long>,"game":<int TB id>,"gameName":<string>,"opponentPid":<long>,"opponentName":<string>,"kidsTurn":<bool>,"lastMoveDate":<epoch ms, 0 if unknown>,"privateGame":<bool>}]}`. The server already sorts it (`lastMoveDate` descending, then `gid` ascending), and the client keeps that order. Errors: 401 `Login required`, 404 `Kid not found`, 500 `Database error`, each as `{"success":false,"error":"..."}`.
  - Opening a row: the app loads the game exactly as today (`/gameServer/mobile/json/game.jsp?gid=<gid>`, `Game.RetrieveGame`, `Game.java:438`) and shows it read-only.
- Toggle label (spec, verbatim): `Turn-based game messages`. List heading `Active turn-based games` and empty text `No active turn-based games.` match the website copy in the server plan.
- Read-only (spec anti-goal): a parent who is not a player must get no client path that sends a move, message, cancel request or reply, undo, resign, draw or hide. The server refuses all of these anyway (server plan Task 3). The client must not offer them either.
- Every new string gets a German translation in `app/src/main/res/values-de/strings.xml`. Escape apostrophes as `\'`.
- Pure classes never touch `android.*`. `KidAccounts` is pure (its class doc says "No Android dependencies") and must not import `be.submanifold.pentelive.Game`, which implements `android.os.Parcelable`. The one `Game`-building helper is `kids/KidGames`, which is not a pure class. `Game` may be constructed in JVM tests: `GameCharacterizationTest` already does this.

## Review Focus

1. **The opponent (player 2) has a pending cancel request when the parent opens the game.** Today `Game.parseGame` (`Game.java:849`) compares `cancel.name` against `getOpponentName()`, which for a non-player is still player 2's name at that point. The parent gets an Accept/Decline prompt, and dismissing it closes the board. Expected: no prompt for a non-player, and the board stays open. Players still get the prompt exactly as before. Pinned: `cancelPrompt_*` tests (Task 2) and E2E step 5 (Task 4).
2. **A server without the feature (production before deploy) omits `copyTbMessages` and answers `action=activeGames` with the ordinary GET body (no `games`).** Expected: the switch is hidden, not shown as "off". The list heading, empty text and list are hidden too, and `action=activeGames` is never called, so the parent is not told "No active turn-based games." falsely. As a second line of defence, an `activeGames` reply without `games` reads as an empty list, not a crash. Pinned: `parse_missingCopyTbMessages_isNull` and `parseActiveGames_missingGames_isEmpty` (Task 1), and the source test `activeGamesHiddenWithoutServerSupport` (Task 3).
3. **The active-games load fails (404 after the kid was released elsewhere, 500, no network) while the rest of the screen loaded.** Expected: the list area shows `Could not load games.`, no extra dialog, and the toggles and approved list stay usable. A later successful load replaces it. Pinned: Task 3 source test `activeGamesFailureShowsInlineError`. E2E step 7 covers only the backend-down case, where the main load fails first and the games call is never made.
4. **The parent taps a game, then comes back.** Expected: the list reloads after the `onResume` load, so a game that ended while the board was open disappears. A slower, older `activeGames` reply never overwrites a newer one (`loadSeq` guard). The `Game` handed to `BoardActivity` is never active (`isActive()` false before and after the JSON load, because `currentPlayer` is never the parent), and it carries only gid and game name, so the board info line is built from the JSON, never with `null` values. Pinned: `readOnlyGame_isInactiveWithIdsOnly` (Task 1, `KidGamesTest`), the `loadSeq` source pin (Task 3) and E2E steps 3-4.
5. **Toggling the TB switch must not touch the other two flags.** Expected: the form carries only `copyTbMessages`. A failed POST reverts only that switch. Pinned: `settingsForm_tbOnly` (Task 1) and E2E step 1 (SQL check).

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java` | Modify | `Kid.copyTbMessages`; `ActiveGame` and `ActiveGamesReply` models; `parseActiveGames`; `activeGamesPath`; 4-arg `settingsForm`; `turnName`. Stays pure. |
| `app/src/main/java/be/submanifold/pentelive/kids/KidGames.java` | Create | `readOnlyGame` (the only `Game`-building code for kids). |
| `app/src/main/java/be/submanifold/pentelive/kids/KidAccountsClient.java` | Modify | `activeGames(long)`; 4-arg `settings`. |
| `app/src/main/java/be/submanifold/pentelive/Game.java` | Modify | Pure `isCancelPromptFor(...)`, used at `:849`. |
| `app/src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java` | Modify | Third switch; active-games list; opens `BoardActivity`. |
| `app/src/main/res/layout/activity_kid_detail.xml` | Modify | Switch, heading, empty text, list. |
| `app/src/main/res/layout/kid_game_row.xml` | Create | Two-line game row. |
| `app/src/main/res/values/strings.xml`, `values-de/strings.xml` | Modify | New strings. |
| `app/src/test/java/be/submanifold/pentelive/kids/KidAccountsTest.java` | Modify | Model and parser tests. |
| `app/src/test/java/be/submanifold/pentelive/kids/KidGamesTest.java` | Create | `readOnlyGame` test. |
| `app/src/test/java/be/submanifold/pentelive/GameCancelPromptTest.java` | Create | Cancel-prompt guard tests. |
| `app/src/test/java/be/submanifold/pentelive/kids/KidDetailSourceTest.java` | Modify | Source-level wiring pins for the new controls. |

---

### Task 1: Model, parser and client for the new contract fields

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java:30-36` (`Kid`), `:140-156` (`settingsForm`), plus new members
- Modify: `app/src/main/java/be/submanifold/pentelive/kids/KidAccountsClient.java:48-51` (`settings`), plus `activeGames`
- Create: `app/src/main/java/be/submanifold/pentelive/kids/KidGames.java`
- Test: `app/src/test/java/be/submanifold/pentelive/kids/KidAccountsTest.java` (append; it has 11 tests today)
- Test: `app/src/test/java/be/submanifold/pentelive/kids/KidGamesTest.java` (create)

**Interfaces:**
- Consumes: `Json.GSON`, `Forms.encode` and the private `form(...)`/`parseRaw` pattern already in `KidAccounts`. Also `KidAccountsClient.result(AuthedHttp.Reply)` and `AuthedHttp.shared().getAllowingErrors(String pathAndQuery)` (`AuthedHttp.java:96`). `KidGames` uses the `Game` constructor `Game(String gameID, String setID, String gameType, String opponentName, String opponentRating, String myColor, String remainingTime, String ratedNot, String privateGame, String nameColor, String crown)` (`Game.java:119`) and `Game.setActive(boolean)` (`:206`).
- Why `readOnlyGame` passes only gid and game name: `Game.getBoardString()` (`Game.java:375-394`) builds `mBoardString` once, as soon as `mOpponentName != null && mPrivateGame != null`, and never rebuilds it. `BoardView.onDraw` calls it (`BoardView.java:178`) before `RetrieveGame` returns. A non-null opponent name and privacy flag would freeze an info line reading `rating null` / `Remaining time: null`. With both null, the line is first built after `parseGame` fills them from the JSON. This matches the existing spectator path, `WebViewActivity.java:91-92` (all nulls apart from the gid, then `setActive(false)`).
- Produces (package `be.submanifold.pentelive.kids`):
  - `KidAccounts.Kid` gains `public Boolean copyTbMessages;`. It is boxed so that a missing field stays `null`, which means the server has no feature and the switch is hidden.
  - `public static final class ActiveGame { public long gid; public int game; public String gameName; public long opponentPid; public String opponentName; public boolean kidsTurn; public long lastMoveDate; public boolean privateGame; }`
  - `public static final class ActiveGamesReply { public boolean success; public String error; public long kidPid; public List<ActiveGame> games; }`
  - `public static ActiveGamesReply parseActiveGames(String body)` returns null for a null body, non-JSON, or `success:false`. A null `games` becomes an empty list. It keeps the server's order.
  - `public static String activeGamesPath(long kidPid)` returns `"/gameServer/kids?action=activeGames&kidPid=" + kidPid`.
  - `public static String settingsForm(long kidPid, Boolean copyMessages, Boolean copyLiveChat, Boolean copyTbMessages)` adds `copyTbMessages=Y|N` after the other two when non-null. The existing 3-arg overload delegates with `null` and keeps its current output byte for byte.
  - `public final class KidGames` (private constructor): `public static Game readOnlyGame(KidAccounts.ActiveGame g)` returns `new Game(String.valueOf(g.gid), null, g.gameName, null, null, null, null, null, null, null, null)` with `setActive(false)`. Javadoc says why the other fields stay null (see above).
  - `public static String KidAccounts.turnName(ActiveGame g, String kidName)` returns `kidName` when `g.kidsTurn`, else `g.opponentName`.
  - `KidAccountsClient.activeGames(long kidPid) throws IOException` returns `ActiveGamesReply`. It calls `getAllowingErrors(KidAccounts.activeGamesPath(kidPid))`. When `code == 200` and `parseActiveGames` is non-null it returns that reply. Otherwise it logs `RedactingLog.e(TAG, "kids activeGames failed, response code " + code, null)` and throws `new Failure(KidAccounts.parseError(body))`.
  - `KidAccountsClient.settings(long kidPid, Boolean copyMessages, Boolean copyLiveChat, Boolean copyTbMessages)`. The 3-arg overload stays and delegates with `null`.

- [ ] **Step 1: Write the failing tests** (append to `KidAccountsTest.java`)

```java
@Test public void parse_copyTbMessages() {
    Reply r = KidAccounts.parse("{\"success\":true,\"kids\":[{\"kidPid\":5,\"name\":\"k\",\"copyMessages\":false,\"copyLiveChat\":false,\"copyTbMessages\":true}]}");
    assertEquals(Boolean.TRUE, r.kids.get(0).copyTbMessages);
}
@Test public void parse_missingCopyTbMessages_isNull() {
    Reply r = KidAccounts.parse("{\"success\":true,\"kids\":[{\"kidPid\":5,\"name\":\"k\"}]}");
    assertNull(r.kids.get(0).copyTbMessages);
}
@Test public void settingsForm_tbOnly() {
    assertEquals("action=settings&kidPid=5&copyTbMessages=Y", KidAccounts.settingsForm(5L, null, null, true));
    assertEquals("action=settings&kidPid=5&copyTbMessages=N", KidAccounts.settingsForm(5L, null, null, false));
    assertEquals("action=settings&kidPid=5&copyMessages=Y", KidAccounts.settingsForm(5L, true, null));
}
@Test public void parseActiveGames_contractSample() {
    ActiveGamesReply r = KidAccounts.parseActiveGames("{\"success\":true,\"kidPid\":23000000012345,\"games\":[{\"gid\":50000000001234,\"game\":51,\"gameName\":\"Pente\",\"opponentPid\":23000000000777,\"opponentName\":\"graviton\",\"kidsTurn\":true,\"lastMoveDate\":1760882580000,\"privateGame\":true},{\"gid\":50000000001235,\"game\":53,\"gameName\":\"Keryo-Pente\",\"opponentPid\":23000000000777,\"opponentName\":\"graviton\",\"kidsTurn\":false,\"lastMoveDate\":0,\"privateGame\":false}]}");
    assertEquals(23000000012345L, r.kidPid);
    assertEquals(2, r.games.size());
    ActiveGame g = r.games.get(0);
    assertEquals(50000000001234L, g.gid); assertEquals(51, g.game); assertEquals("Pente", g.gameName);
    assertEquals("graviton", g.opponentName); assertTrue(g.kidsTurn); assertEquals(1760882580000L, g.lastMoveDate); assertTrue(g.privateGame);
    assertEquals(50000000001235L, r.games.get(1).gid); // server order kept
}
@Test public void parseActiveGames_emptyAndFailures() {
    assertTrue(KidAccounts.parseActiveGames("{\"success\":true,\"kidPid\":5,\"games\":[]}").games.isEmpty());
    assertNull(KidAccounts.parseActiveGames("{\"success\":false,\"error\":\"Kid not found\"}"));
    assertNull(KidAccounts.parseActiveGames("<html>login</html>"));
    assertNull(KidAccounts.parseActiveGames(null));
}
@Test public void parseActiveGames_missingGames_isEmpty() {
    // an old server ignores action=activeGames and returns the plain GET body
    assertTrue(KidAccounts.parseActiveGames("{\"success\":true,\"isKid\":false,\"kids\":[]}").games.isEmpty());
}
@Test public void activeGamesPath() {
    assertEquals("/gameServer/kids?action=activeGames&kidPid=5", KidAccounts.activeGamesPath(5L));
}
@Test public void turnName() {
    ActiveGame g = new ActiveGame(); g.opponentName = "graviton";
    g.kidsTurn = true; assertEquals("kidstb1", KidAccounts.turnName(g, "kidstb1"));
    g.kidsTurn = false; assertEquals("graviton", KidAccounts.turnName(g, "kidstb1"));
}
```

Import `KidAccounts.ActiveGame` and `KidAccounts.ActiveGamesReply`. `KidAccountsTest` does not import `Game`.

Create `KidGamesTest.java` with one test:

```java
@Test public void readOnlyGame_isInactiveWithIdsOnly() {
    ActiveGame g = new ActiveGame();
    g.gid = 50000000001234L; g.gameName = "Pente"; g.opponentName = "graviton"; g.privateGame = true;
    Game game = KidGames.readOnlyGame(g);
    assertFalse(game.isActive());
    assertEquals("50000000001234", game.getGameID());
    assertEquals("Pente", game.getGameType());
    assertNull(game.getOpponentName());   // else getBoardString() freezes "rating null" before the JSON load
    assertNull(game.getPrivateGame());
}
```

If constructing `Game` throws on the JVM stubs, stop and report it. `GameCharacterizationTest` already does `new Game(...)`, so it should not throw.

- [ ] **Step 2: Run to verify they fail**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.kids.KidAccountsTest" --tests "be.submanifold.pentelive.kids.KidGamesTest"`
Expected: FAIL, compilation errors (`copyTbMessages`, `ActiveGame`, `parseActiveGames`, `KidGames` not found).

- [ ] **Step 3: Implement** the Interfaces above in `KidAccounts`, `KidAccountsClient` and `KidGames`. Keep the three existing `copyMessages`/`copyLiveChat` assertions in `forms()` passing unchanged.

- [ ] **Step 4: Run to verify they pass**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.kids.KidAccountsTest" --tests "be.submanifold.pentelive.kids.KidGamesTest"`
Expected: PASS, 19 tests in `KidAccountsTest`, 1 in `KidGamesTest`.
Run: `grep -n "import be.submanifold.pentelive.Game\|import android" /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java`
Expected: no output (`KidAccounts` stays pure).

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java app/src/main/java/be/submanifold/pentelive/kids/KidAccountsClient.java app/src/main/java/be/submanifold/pentelive/kids/KidGames.java app/src/test/java/be/submanifold/pentelive/kids/KidAccountsTest.java app/src/test/java/be/submanifold/pentelive/kids/KidGamesTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): copyTbMessages flag and activeGames model, parser and client"
```

---

### Task 2: Read-only audit of `BoardActivity` for a non-player, and the cancel-prompt fix

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/Game.java:849` (the `if (mGameJson.cancel != null && getOpponentName().equals(mGameJson.cancel.name))` line), plus a new static method
- Test: `app/src/test/java/be/submanifold/pentelive/GameCancelPromptTest.java`

**Interfaces:**
- Consumes: the local variables `p1Name`, `p2Name` (`Game.java:758-781`) and `PentePlayer.mPlayerName`.
- Produces: `static boolean Game.isCancelPromptFor(String cancelName, String p1Name, String p2Name, String me)`. It is package-private and pure. It returns true only when `cancelName` is non-null, `me` matches exactly one of `p1Name`/`p2Name` (`equalsIgnoreCase`, as at `:761`/`:775`), and `cancelName.equals(...)` the other player's name. That is exactly today's player behaviour, because for a player `getOpponentName()` at `:849` is the other player's name.

Audit result (checked at `ee616c8`; no change needed for these). A parent viewer is never `currentPlayer`, so `mActive` is false after `Game.java:769`, and the parent is not `amIPlaying` (`:765`/`:779`):

| Write path | Guard |
|---|---|
| Submit move / Go pass (`BoardActivity.setRegularSubmitListener`, `:315-320`) | `!game.isActive()` shows a `not_your_turn` toast and returns |
| Renju PASS (`:106`), DRAW? only reaches the server through a move | `!game.isActive()` returns; `updateRenjuTbButtons` shows the buttons only when `isActive()` (`:652-653`) |
| Swap / dPente / swap2 / renju choice buttons (`:121-205`) | Their layout is only made visible when `game.isActive()` (`BoardView.java:191`, `:196`, `:206`) |
| Resign, request cancel, hide/unhide menu (`:209-249`) | `!game.isActive()` returns false |
| Accept draw (`:680`, `:693`) | Only when `isActive()`; a non-player gets the neutral `draw_offer_pending` toast |
| Request/accept/decline undo (`Game.java:900-960`) | Every branch requires `amIPlaying` |
| Go dead-stone accept/reject (`Game.java:1001`) | Requires `mActive` |
| Message text input (`BoardActivity.java:578-579`) | Input `GONE` when `!isActive()`; text is only sent along with a move |
| **Cancel reply Yes/No (`Game.java:849-868`)** | **Not guarded: fixed in this task** |

- [ ] **Step 1: Write the failing tests** in `GameCancelPromptTest.java`

```java
@Test public void cancelPrompt_parentOfPlayer1_noPrompt() {
    assertFalse(Game.isCancelPromptFor("graviton", "kidstb1", "graviton", "iostest"));
    assertFalse(Game.isCancelPromptFor("kidstb1", "kidstb1", "graviton", "iostest"));
}
@Test public void cancelPrompt_playerSeesOpponentRequest() {
    assertTrue(Game.isCancelPromptFor("graviton", "kidstb1", "graviton", "kidstb1"));
    assertTrue(Game.isCancelPromptFor("kidstb1", "kidstb1", "graviton", "graviton"));
    assertTrue(Game.isCancelPromptFor("graviton", "kidstb1", "graviton", "KidsTB1")); // viewer-name match ignores case (:761/:775); cancel name matched exactly, as today
}
@Test public void cancelPrompt_ownRequest_noPrompt() {
    assertFalse(Game.isCancelPromptFor("kidstb1", "kidstb1", "graviton", "kidstb1"));
}
@Test public void cancelPrompt_noRequest_noPrompt() {
    assertFalse(Game.isCancelPromptFor(null, "kidstb1", "graviton", "kidstb1"));
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.GameCancelPromptTest"`
Expected: FAIL, `cannot find symbol isCancelPromptFor`.

- [ ] **Step 3: Implement** `isCancelPromptFor`. Replace the condition at `Game.java:849` with `mGameJson.cancel != null && isCancelPromptFor(mGameJson.cancel.name, p1Name, p2Name, PentePlayer.mPlayerName)`. The dialog body is unchanged. This also stops the same prompt for any other spectator opened from a `viewLiveGame` link, which is intended.

- [ ] **Step 4: Run to verify they pass, plus the existing Game suites**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.GameCancelPromptTest" --tests "be.submanifold.pentelive.GameCharacterizationTest"`
Expected: PASS (4 new tests, plus `GameCharacterizationTest` unchanged).
Run: `grep -n "getOpponentName().equals(mGameJson.cancel.name)" /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/Game.java`
Expected: no output.

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/Game.java app/src/test/java/be/submanifold/pentelive/GameCancelPromptTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "fix(tb): offer the cancel-request reply only to the other player, never to a spectator"
```

---

### Task 3: Kid detail screen: TB messages switch and active games list

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java` (fields `:55-61`, `onCreate` `:72-78`, `onResume` `:104-108`, `show` `:164-181`, `toggle` `:191-197`, new adapter)
- Modify: `app/src/main/res/layout/activity_kid_detail.xml` (after `copyLiveChatNote`, `:47-52`)
- Create: `app/src/main/res/layout/kid_game_row.xml`
- Modify: `app/src/main/res/values/strings.xml` (after `kid_copy_live_chat_note`, `:664`), `app/src/main/res/values-de/strings.xml` (after `:414`)
- Test: `app/src/test/java/be/submanifold/pentelive/kids/KidDetailSourceTest.java` (modify)

**Interfaces:**
- Consumes: from Task 1, `KidAccounts.Kid.copyTbMessages`, `KidAccountsClient.settings(long, Boolean, Boolean, Boolean)`, `KidAccountsClient.activeGames(long)`, `KidGames.readOnlyGame(ActiveGame)` and `KidAccounts.turnName(ActiveGame, String)`. Also `be.submanifold.pentelive.BoardActivity` (extra key `"game"`, as in `MainActivity.java:278-279`), `android.text.format.DateUtils.getRelativeTimeSpanString(long)`, and the existing `run(...)`, `setChecked(...)` and `Outcome` pattern in `KidDetailActivity`.
- Produces: layout ids `copyTbMessagesSwitch`, `activeGamesHeading`, `activeGamesList`, `activeGamesEmpty`; row ids `gameTitle`, `gameDetail`; the strings below. Private members of `KidDetailActivity`: `int loadSeq`, `boolean gamesPending`, `void loadActiveGames()`, `void setGamesVisible(boolean visible)`.

Strings (English / German):

| name | English | German |
|---|---|---|
| `kid_copy_tb_messages` | Turn-based game messages | Nachrichten aus rundenbasierten Spielen |
| `kid_tb_games` | Active turn-based games | Laufende rundenbasierte Spiele |
| `kid_tb_games_none` | No active turn-based games. | Keine laufenden rundenbasierten Spiele. |
| `kid_tb_games_error` | Could not load games. | Spiele konnten nicht geladen werden. |
| `kid_tb_game_title` | %1$s against %2$s | %1$s gegen %2$s |
| `kid_tb_turn` | %1$s\'s turn | %1$s ist am Zug |
| `kid_tb_private` | private | privat |

Behaviour:
- **Layout.** After `copyLiveChatNote`, add a third `SwitchCompat` `@+id/copyTbMessagesSwitch` with `minHeight 48dp`, text `@string/kid_copy_tb_messages` and `android:visibility="gone"`. Before the "Approved players" heading, add a bold medium heading `@+id/activeGamesHeading` with `@string/kid_tb_games` (`marginTop 16dp`), then a `TextView` `@+id/activeGamesEmpty` (same style as `emptyText`), then a `RecyclerView` `@+id/activeGamesList` (`nestedScrollingEnabled="false"`). All three start `android:visibility="gone"`. `kid_game_row.xml` copies `kid_row.xml` (padding, `selectableItemBackground`, `minHeight 56dp`) with two `TextView`s: `gameTitle` (`textAppearanceMedium`) and `gameDetail` (`textAppearanceSmall`).
- **Switch and old-server gate.** `show(reply)`: when `kid.copyTbMessages == null`, the switch is `GONE`, `setGamesVisible(false)` hides the heading, empty text and list, and `activeGames` is not called (an old server would answer with the plain GET body, read as an empty list, and the screen would falsely say "No active turn-based games."). Otherwise the switch is `VISIBLE` and set with `setChecked(copyTbMessages, tbListener, kid.copyTbMessages)`, `setGamesVisible(true)` shows the heading and list, and if `gamesPending` is true it is cleared and `loadActiveGames()` runs. Change `toggle` to `toggle(SwitchCompat s, CompoundButton.OnCheckedChangeListener l, boolean flag, Call call)`. The three listeners pass `() -> KidAccountsClient.settings(kidPid, flag, null)`, `() -> KidAccountsClient.settings(kidPid, null, flag)` and `() -> KidAccountsClient.settings(kidPid, null, null, flag)`. On failure, revert only that switch (existing `run` `onFailure`).
- **List.** `onResume` sets `gamesPending = true` before the existing `load()`, so the games load runs once per resume, after the main load has told the screen whether the server has the feature. The toggles' `settings` replies also go through `show`, but `gamesPending` is false by then, so they do not reload the list. `loadActiveGames()` does `final int seq = ++loadSeq;` and starts a `BackgroundTask` calling `KidAccountsClient.activeGames(kidPid)`. In `onPostExecute`, after the `isFinishing() || isDestroyed()` guard, it returns early when `seq != loadSeq`, so a slower, older reply never overwrites a newer one. On success, replace the adapter's list. `activeGamesEmpty` shows `kid_tb_games_none` when the list is empty and is otherwise `GONE`. On any failure, empty the list and show `activeGamesEmpty` with `kid_tb_games_error`. No dialog: the existing load already reports connection failures.
- **Row.** Title: `getString(R.string.kid_tb_game_title, g.gameName, g.opponentName)`. Detail: `getString(R.string.kid_tb_turn, KidAccounts.turnName(g, kidName))`. Then append `" · " + DateUtils.getRelativeTimeSpanString(g.lastMoveDate)` when `lastMoveDate > 0`, and `" · " + getString(R.string.kid_tb_private)` when `privateGame`. A tap calls `startActivity(new Intent(this, BoardActivity.class).putExtra("game", KidGames.readOnlyGame(g)))`. The list rows have no swipe and no long-press.

- [ ] **Step 1: Write the failing source tests** (modify `KidDetailSourceTest.java`)
  - `layoutHasTheControls`: add `copyTbMessagesSwitch`, `activeGamesHeading`, `activeGamesList` and `activeGamesEmpty` to the id array. Change the SwitchCompat count to `assertEquals(3, ...)`. Add `assertTrue(layout.contains("@string/kid_copy_tb_messages"))` and `assertTrue(layout.contains("@string/kid_tb_games"))`.
  - `activityIsWiredToTheClient`: keep every existing assertion and add:
    ```java
    assertTrue(src.contains("KidAccountsClient.settings(kidPid, null, null, flag)"));
    assertTrue(src.contains("KidAccountsClient.activeGames(kidPid)"));
    assertTrue(src.contains("KidGames.readOnlyGame("));
    assertTrue(src.contains("BoardActivity.class"));
    assertTrue(src.contains("kid.copyTbMessages == null"));
    assertTrue(src.contains("seq != loadSeq"));
    ```
  - New `activeGamesHiddenWithoutServerSupport`: let `i = src.indexOf("kid.copyTbMessages == null")` (`>= 0`) and `branch = src.substring(i, src.indexOf("} else {", i))`. Assert `branch.contains("setGamesVisible(false)")` and `!branch.contains("loadActiveGames()")`. Also assert the layout has `android:visibility="gone"` exactly 5 times (1 today, for `emptyText`, plus switch, heading, empty text and list).
  - New `activeGamesFailureShowsInlineError`: `src.contains("R.string.kid_tb_games_error")` and `src.contains("R.string.kid_tb_games_none")`.
  - New `tbToggleLabelIsVerbatim`: `values/strings.xml` contains `<string name="kid_copy_tb_messages">Turn-based game messages</string>`, and `values-de/strings.xml` contains `name="kid_copy_tb_messages"`.

- [ ] **Step 2: Run to verify they fail**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.kids.KidDetailSourceTest"`
Expected: FAIL (missing ids and strings; SwitchCompat count 2).

- [ ] **Step 3: Implement** the strings, the layouts and the activity as described above.

- [ ] **Step 4: Run to verify they pass, and build**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.kids.KidDetailSourceTest"`
Expected: PASS, 6 tests.
Run: `GRADLE :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java app/src/main/res/layout/activity_kid_detail.xml app/src/main/res/layout/kid_game_row.xml app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml app/src/test/java/be/submanifold/pentelive/kids/KidDetailSourceTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): turn-based messages toggle and active games list on the kid screen"
```

---

### Task 4: Full suite and emulator end-to-end against the local backend

**Files:** none committed. `PentePlayer.development` is set to `true` only for this check and reverted before finishing. Scratch (screenshots, logcat, helper script) goes in `/private/tmp/claude-501/kids-tb/android/`.

**Interfaces:**
- Consumes: everything above. A local pente.org stack running the server branch `kids-tb-messages`, with the SQL migration applied and the server plan's Task 9 completed. That leaves parent `iostest`, opponent `graviton`, kids `kidstb1` and `kidstb2`, the active games A (private) and B (public) between `kidstb1` and `graviton`, and the messages `kidhello1`, `gravhello1`, `kidmove1`, `gravmove1` in A. If these are missing, create them with the server plan's E2E script (`/private/tmp/claude-501/kids-tb/e2e/kids_tb_e2e.py`, scenarios 1-3) before starting. Credentials: `iostest`/`graviton` come from `/Users/waliedothman/.claudeboat/projects/-Users-waliedothman-mariposa-coding-pente-org-project/memory/local-backend-test-accounts.md`. For `kidstb1`, if its password is unknown, set it from the app as `iostest` (Kid detail → Set password) to a value kept only in the shell env `KIDS_E2E_KID_PASS`. Never write credentials to any file. HTTP checks use Python `urllib` with an unverified SSL context (`curl`/`wget` are hook-blocked). SQL uses the server plan's `docker compose ... exec -T main_db` recipe.

- [ ] **Step 1: Full unit suite and debug build**

Run: `GRADLE :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, 0 failures.

- [ ] **Step 2: Emulator run.** Set `PentePlayer.development = true`, then run `GRADLE :app:installDebug` on a running emulator. Record PASS or FAIL for each step with its evidence (a screenshot path or an SQL/JSON line):
  1. As `iostest`: Settings → Kid accounts → `kidstb1`. The switch `Turn-based game messages` shows and matches SQL `select copy_tb_messages from dsg_kid_settings where kid_pid=<kidstb1>`. Toggle it on: SQL gives `Y`, and `copy_messages`/`copy_live_chat` are unchanged. Toggle it off: `N`. Leave it as the server E2E needs it.
  2. The `Active turn-based games` list shows the same games in the same order as `GET /gameServer/kids?action=activeGames&kidPid=<kidstb1>` (urllib as `iostest`). Each row reads `<gameName> against graviton`, with whose turn and `private` on A only.
  3. Tap A: the board opens with the title `<gameName>` and the players `kidstb1 vs graviton`. Screenshot the board info line (`playerInfo`): it contains no `null` (rating, remaining time and the rated/private sentence are filled from the JSON). Stepping back and forward through the moves, the message icon spins on the message moves and shows `kidhello1`, `gravhello1`, `kidmove1` and `gravmove1`. The message dialog has no input field.
  4. On A as `iostest`: tap a point, then Submit → `It's not your turn.` toast. The cancel/resign menu icon does nothing. Back on the kid screen, the list reloads. SQL: A's move count is unchanged, there is no `tb_message` row from `iostest`, and the set's `cancel_pid` is unchanged.
  5. Cancel prompt (Review Focus 1). With urllib, as the player who is player 2 in game B (check `tb_game.player2_pid`), POST `/gameServer/tb/cancel` `sid=<B's sid>&command=request&mobile=`. As `iostest`, open B from the list: no Accept/Decline dialog appears and the board stays open. Then log in on the emulator as player 1 of B and open B from the dashboard: the `requests_cancellation` dialog appears. Choose Decline and confirm in SQL that `cancel_pid` is 0 again.
  6. `kidstb2` (if still a kid of `iostest`) shows `No active turn-based games.`
  7. Backend down: stop the backend container (`docker compose -f /Users/waliedothman/mariposa/coding/pente.org-project/pente.org/docker-compose.yml stop pente.org`), then reopen `kidstb1`. The existing load shows its `try_again` dialog, the games section stays hidden (the games call only runs after a successful main load), and nothing crashes. Start the container again and reopen `kidstb1`: the list shows. The inline `Could not load games.` path (main load OK, games call fails) is pinned by the source test `activeGamesFailureShowsInlineError` only.
  8. `adb logcat -d` saved to `/private/tmp/claude-501/kids-tb/android/logcat.txt` has no `FATAL EXCEPTION` from `be.submanifold.pentelive`.

- [ ] **Step 3: Revert, clean and tidy**

Run: `git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android diff app/src/main/java/be/submanifold/pentelive/PentePlayer.java`. If the only change is the `development` flag, run `git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android checkout -- app/src/main/java/be/submanifold/pentelive/PentePlayer.java`. Then run `GRADLE clean` and `git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android status --porcelain`.
Expected: a clean working tree with no untracked files. `git -C ... log --oneline main..kids-tb-messages` shows the three commits from Tasks 1-3.

---

## Deployment note

Release the app only after the server branch is deployed (server plan "Deployment order"). On an older server the new switch and the whole active-games section stay hidden, and `action=activeGames` is never called (Task 3, `copyTbMessages == null`). So releasing early degrades quietly, but the feature is invisible until the server ships.

## Self-review notes (writer)

- Spec coverage (Android): toggle `Turn-based game messages`, off by default, next to the two existing toggles → Tasks 1 and 3. Active games list per kid, where tapping opens `BoardActivity` with `setActive(false)` → Tasks 1 (`KidGames.readOnlyGame`, gid and game name only, like `WebViewActivity.java:91`) and 3. Message display needs no change, as verified: `json/game.jsp` fills `messages`/`messageNums` for a parent (server plan Task 2), and `Game.parseGame:869-977` renders whatever arrives regardless of who is viewing. Read-only for a parent → Task 2 audit table plus the cancel-prompt fix. E2E against the local backend → Task 4.
- Deviation from the spec, with reason: the spec says Android needs no non-player fixes. The audit found that `Game.java:849` offers a non-player the cancel Yes/No prompt when player 2 has requested a cancel, and closes the board when it is dismissed. This is the Android analog of the iOS bug (b) the spec lists. The fix is a one-line guard with a pure helper. Writes stay server-refused regardless.
- Decisions this plan makes where the spec leaves a choice: the switch and the active-games section are hidden, and the games call skipped, when the server omits the field; the games load runs after the main load and keeps only the latest reply (`loadSeq`); the client keeps the server's sort order; a list-load failure is shown inline rather than as a dialog; row copy is `kid_tb_game_title`/`kid_tb_turn`; finished games are not listed (spec non-goal).
- Out of scope, reported: a non-player viewing a Renju game with a pending draw offer gets the neutral `draw offer pending…` toast (harmless; existing behaviour for every spectator).
- Out of scope, reported: after the JSON load, `parseGame` sets `mOpponentName` to player 2's name for any non-player (`Game.java:760-779`, the p2 branch runs last), so the info line reads `opponent: <player 2>`. When the kid is player 2, the kid is labelled "opponent". This is existing behaviour for every spectator and the Android analog of iOS bug (a), but the spec lists the label bug for iOS only, so this plan does not change it.
