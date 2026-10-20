# Kids Accounts (Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a parent manage kid accounts from native Android screens, and give a kid login the kid-mode UI (composer suggestions limited to parent + approved players, chat hints, no invite text, no broadcast, refusal strings shown). Kids keep the blocked-players screen and every block button (blocking only restricts further).

**Architecture:** The kid flag comes from the index JSON into `PentePlayer` (static `parentPid`, carried through the Parcel), and from the live-room login echo into `TablesAndPlayers`. All parsing and decisions live in plain-Java classes that are unit-tested with JUnit: `KidAccounts` (the `/gameServer/kids` model, parser and form builders), `MessageSendReply` (classifies the `/gameServer/mymessages` reply), and `KidLiveRoom` (login-echo flag and quote rendering). The Android classes (`KidAccountsActivity`, `KidDetailActivity`, `SendMessageActivity`, `ReplyMessageActivity`, `LobbyActivity`, `SettingsActivity`, `BoardActivity` and the live-room classes) are thin views over them. The server enforces everything; the client UI is courtesy only.

**Tech Stack:** Java (minSdk 26), AndroidX AppCompat + RecyclerView, Gson (`net.Json.GSON`), JUnit 4 local unit tests (no Robolectric), Gradle via the Android Studio JBR.

**Spec:** `/Users/waliedothman/mariposa/coding/pente.org-project/pente.org/docs/superpowers/specs/2026-10-19-kids-accounts-design.md` (section "Clients", "Android"). Wire contract: **only** the section "Server API contract" of `/Users/waliedothman/mariposa/coding/pente.org-project/pente.org/docs/superpowers/plans/2026-10-19-kids-accounts-server.md`. Executors read both.

## Global Constraints

- Repo: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android`, branch `kids-accounts` (already created; confirm with `git -C <repo> branch --show-current`). All paths below are relative to this repo.
- Kid flag: JSON field `parentPid` (a number). Present = kid, absent = not a kid. A server without the feature never sends it, so the app must behave exactly as today when it is missing.
- Kid flag sources, verbatim from the contract: index JSON top-level `"player": {"parentPid": ...}` (`/gameServer/mobile/json/index.jsp`), and live login echo `{"dsgLoginEvent": {"me": {"parentPid": ...}}}`. Never read it from `dsgJoinMainRoomEvent`/`dsgUpdatePlayerDataEvent` player data.
- Endpoint `/gameServer/kids`: `GET` returns the caller's view; `POST` form-encoded with `action=` one of `create` (`kidName`, `kidPassword`), `convert` (`kidName`), `password` (`kidPid`, `kidPassword`), `whitelistAdd` (`kidPid`, `playerName`), `whitelistRemove` (`kidPid`, `playerPid`), `settings` (`kidPid`, optional `copyMessages`, optional `copyLiveChat`; send `Y`/`N`; a missing flag keeps its value), `release` (`kidPid`). Every success returns the GET body (plus `message` for `convert`). Reply fields: `success`, `error`, `message`, `isKid`, `emailVerified`, `parentName`, `whitelist[] {pid, name, nameColor, tourneyWinner}`, `kids[] {kidPid, name, copyMessages, copyLiveChat, whitelist[]}`.
- `convert` replies are deliberately neutral: a target that is already a kid, has kids, or is an admin gets the **same** success body (with `message`) and nothing happens. The app shows the reply as-is and never infers anything about the target from it.
- Errors come as `{"success": false, "error": "<string>"}` with a **non-200** status (400/401/403/404/409/500; no credentials → 401 `Login required`). `HttpURLConnection.getInputStream()` throws on 4xx/5xx, so the kids client must read these through the error-stream variant of `AuthedHttp` added in Task 2 (`getAllowingErrors` / `postFormAllowingErrors`). Wrong credentials come back as the HTML login page with status 200.
- A non-JSON body (expired session HTML login page, status 200) or `"success": false` is a failure. On failure show the server's `error` string verbatim when present, else `R.string.try_again`.
- Kid composer (contract "Composer" rule): suggestions/autocomplete are `parentName` plus the `whitelist` names only; the client **never** blocks a send — whatever the kid types is sent and the server's refusal string is shown.
- Open TB invitations: the server omits a non-allowed inviter's text for a kid viewer on `/gameServer/tb/replyInvitation?mobile=` and `mobile/json/game.jsp`. The app needs no change (it renders whatever text arrives); Task 7 checks it.
- Refusal strings the app matches, verbatim: `You can only send messages to players your parent approved.` and `Player is ignoring your messages.` (`/gameServer/mymessages` `command=create`); `Kid accounts can't broadcast.` (`/gameServer/broadcast`).
- Live room: no new events and no refusal events for kid filtering. `dsgInviteTableEvent.inviteText` and `dsgInviteResponseTableEvent.responseText` can arrive as `""`; render an empty text with no quote bubble (for every user, not only kids).
- Every new string gets a German translation in `app/src/main/res/values-de/strings.xml` (the only other locale). Apostrophes in string resources are escaped as `\'`.
- Local unit tests run with `returnDefaultValues = true` (`app/build.gradle:74-82`), so `org.json` and `android.*` are stubs. Pure classes take `String` bodies (parsed with Gson) or already-decoded `Map<String, ?>`; they never touch `android.*`.
- Gradle needs JDK 17+. Every gradle command is written in full as `env JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/gradlew -p /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android <task>`. The default `JAVA_HOME` is JDK 8 and fails. Below this is abbreviated as `GRADLE <task>`; always run the full form.
- Commits: `git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "..."`. Stage explicit paths only (never `git add -A` / `git add .`). No co-author or generated-by lines. After each commit, read the diffstat and check the file count.
- Never commit `PentePlayer.development = true` (`app/src/main/java/be/submanifold/pentelive/PentePlayer.java:36`).
- Scratch files go to `/private/tmp/claude-501/kids-plans/`, never inside the repo.

## Review Focus

1. A server without the feature: index JSON has no `parentPid`, `/gameServer/kids` answers 404 HTML → app is not in kid mode anywhere, and the Kid accounts screen shows the `try_again` dialog instead of crashing or claiming "no kids". Pinned in Task 1 (`indexJsonWithoutParentPid_isNotKid`) and Task 2 (`parse_htmlOrNull_isNull`).
2. Kid status leaking between logins on one device: a kid logs out and a parent logs in (or the reverse) → kid mode follows the new login, not the old one. `PentePlayer(String, String)` resets `parentPid` to null; Task 1 step pins it (`newPlayer_resetsKidFlag`, with an `@After` reset so the static never leaks into other suites).
3. Server error strings on non-200 replies (403 `Verify your email address…`, 409 `Requested name … already taken`, 404 `Player not found`) actually reach the dialog, instead of an `IOException` from `getInputStream()` turning every failure into `try_again`. Pinned in Task 2 (`AuthedHttpTest` error-stream tests) and Task 7 step 2.
4. The kid composer never refuses a send on the client (admins, system and computer accounts are allowed by the server but unknown to the client); it only narrows suggestions, starting from an empty list so unfiltered recent players never show. Server refusals are shown via `MessageSendReply`. Pinned in Task 3 (`allowedRecipients_includesParentFirst`) and Task 7 step 2.
5. A `responseText` / `inviteText` that is `""` or missing (`null`) → no `: ""` and no `null` in the table log or invite dialog. Pinned in Task 6 (`quotedSuffix_emptyOrNull_isEmpty`).
6. A parent whose email is not verified (`emailVerified: false`) → sees `kid_verify_email` and the create/convert buttons are disabled, rather than discovering it only through a failed write (Task 4a).
7. Kids keep the blocked-players button and every `BlockFlow` entry point; nothing in this plan hides them.

---

## File Structure

| File | Status | Responsibility |
|---|---|---|
| `app/src/main/java/be/submanifold/pentelive/JsonModels.java` | Modify | `PlayerInfo.parentPid` (`Long`). |
| `app/src/main/java/be/submanifold/pentelive/PentePlayer.java` | Modify | Static `parentPid`, `isKid()`, reset, populate, Parcel. |
| `app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java` | Create | Pure model, parser, form builders, recipient rule for `/gameServer/kids`. |
| `app/src/main/java/be/submanifold/pentelive/kids/KidAccountsClient.java` | Create | Blocking HTTP calls (run inside `BackgroundTask`). |
| `app/src/main/java/be/submanifold/pentelive/net/AuthedHttp.java` | Modify | Add `getAllowingErrors` / `postFormAllowingErrors` that read the error stream on 4xx/5xx; existing `read()` unchanged. |
| `app/src/main/java/be/submanifold/pentelive/kids/KidAccountsActivity.java` | Create | Parent: list kids, create, convert. Kid: read-only "managed by" + approved list. |
| `app/src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java` | Create | One kid: approved list (swipe remove, add), two copy toggles, set password, release. |
| `app/src/main/res/layout/activity_kid_accounts.xml`, `activity_kid_detail.xml`, `kid_row.xml` | Create | Layouts. |
| `app/src/main/java/be/submanifold/pentelive/MessageSendReply.java` | Create | Pure classifier of the `mymessages` create reply. |
| `app/src/main/java/be/submanifold/pentelive/SendMessageActivity.java`, `ReplyMessageActivity.java` | Modify | Use the classifier; kid composer limits suggestions only. |
| `app/src/main/java/be/submanifold/pentelive/SettingsActivity.java`, `res/layout/activity_settings.xml`, `AndroidManifest.xml` | Modify | Kid accounts button (blocked players stays for everyone); register activities. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LobbyActivity.java` | Modify | Hide broadcast for kids; match the kid refusal string. |
| `app/src/main/java/be/submanifold/pentelive/BoardActivity.java` | Modify | Kid hint on the TB message input. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/KidLiveRoom.java` | Create | Pure: kid flag from login echo, quote suffix. |
| `app/src/main/java/be/submanifold/pentelive/liveGameRoom/TablesAndPlayers.java`, `LiveGameRoomActivity.java`, `LiveGameRoomFragment.java`, `LiveTableFragment.java` | Modify | Live-room kid mode. |
| `app/src/main/res/values/strings.xml`, `values-de/strings.xml` | Modify | New strings (each task adds its own). |
| `app/src/test/java/be/submanifold/pentelive/KidFlagTest.java`, `MessageSendReplyTest.java`, `kids/KidAccountsTest.java`, `liveGameRoom/KidLiveRoomTest.java` | Create | Unit tests. |
| `app/src/test/java/be/submanifold/pentelive/net/AuthedHttpTest.java` | Modify | Error-stream variant tests (MockWebServer). |

---

### Task 1: Kid flag in `PentePlayer`

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/JsonModels.java:32-42` (`PlayerInfo`)
- Modify: `app/src/main/java/be/submanifold/pentelive/PentePlayer.java` — statics near `:42`, constructor `PentePlayer(String, String)` (`:85-108`), `populateFromJson` (`:178-194`), Parcel read (`:494-498`), Parcel write (`:601-605`)
- Test: `app/src/test/java/be/submanifold/pentelive/KidFlagTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `public Long JsonModels.IndexResponse.PlayerInfo.parentPid` (null when absent); `public static Long PentePlayer.parentPid`; `public static boolean PentePlayer.isKid()` (true iff `parentPid != null`). Every later task reads kid status only through `PentePlayer.isKid()` (outside the live room).

- [ ] **Step 1: Write the failing tests** in `KidFlagTest.java`:

```java
@After public void resetKidFlag() { PentePlayer.parentPid = null; } // the static must not leak into other suites in the same JVM
@Test public void indexJsonWithParentPid_isKid() {
    JsonModels.IndexResponse r = new Gson().fromJson(
        "{\"player\":{\"name\":\"kid_one\",\"parentPid\":23000000012345}}", JsonModels.IndexResponse.class);
    assertEquals(Long.valueOf(23000000012345L), r.player.parentPid);
}
@Test public void indexJsonWithoutParentPid_isNotKid() {
    JsonModels.IndexResponse r = new Gson().fromJson("{\"player\":{\"name\":\"iostest\"}}", JsonModels.IndexResponse.class);
    assertNull(r.player.parentPid);
}
@Test public void isKid_followsParentPid() {
    PentePlayer.parentPid = 23000000012345L; assertTrue(PentePlayer.isKid());
    PentePlayer.parentPid = null; assertFalse(PentePlayer.isKid());
}
@Test public void newPlayer_resetsKidFlag() {
    PentePlayer.parentPid = 23000000012345L;
    new PentePlayer("iostest", "pw");
    assertFalse(PentePlayer.isKid());
}
```

If `new PentePlayer(...)` cannot run on the JVM stubs (it throws from an `android.*` call), drop only `newPlayer_resetsKidFlag`, keep the reset in the code, and say so in the task report.

- [ ] **Step 2: Run to verify they fail**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.KidFlagTest"`
Expected: FAIL, compilation error `cannot find symbol parentPid`.

- [ ] **Step 3: Implement**
  - `PlayerInfo`: add `public Long parentPid;` (boxed so Gson leaves it null when absent).
  - `PentePlayer`: add `public static Long parentPid = null;` beside `mSubscriber` and `public static boolean isKid() { return parentPid != null; }`. Constructor `PentePlayer(String, String)`: `parentPid = null;`. `populateFromJson`: `parentPid = json.player.parentPid;`. Do not touch `reloadSubscriberFlags`.
  - Parcel: append after `tbHills` in both places, so earlier fields keep their order: write `dest.writeLong(parentPid == null ? 0L : parentPid);`, read `long p = in.readLong(); parentPid = p == 0L ? null : p;` (pid 0 is never a real player).

- [ ] **Step 4: Run to verify they pass**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.KidFlagTest"`
Expected: PASS (4 tests, or 3 if Step 1's fallback applied).

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/JsonModels.java app/src/main/java/be/submanifold/pentelive/PentePlayer.java app/src/test/java/be/submanifold/pentelive/KidFlagTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): read the parentPid kid flag from the index JSON into PentePlayer"
```

---

### Task 2: `/gameServer/kids` model, parser and client

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java`
- Create: `app/src/main/java/be/submanifold/pentelive/kids/KidAccountsClient.java`
- Modify: `app/src/main/java/be/submanifold/pentelive/net/AuthedHttp.java:88-105` (public methods), `:208-247` (`exchangeGet`, `exchangePost`, `read`)
- Test: `app/src/test/java/be/submanifold/pentelive/kids/KidAccountsTest.java`, `app/src/test/java/be/submanifold/pentelive/net/AuthedHttpTest.java` (append)

**Interfaces:**
- Consumes: `be.submanifold.pentelive.net.Json.GSON`, `net.Forms.encode(String...)`, `net.AuthedHttp.Reply {int code; String finalPath; String body}`, `be.submanifold.pentelive.RedactingLog.e(String, String, Throwable)`. Pattern to copy: `blocked/BlockedPlayers.java` and `blocked/BlockedPlayersClient.java` — but **not** its use of `get`/`postForm`: those go through `AuthedHttp.read()`, which calls `getInputStream()` and throws on every 4xx/5xx (`AuthedHttp.java:236-246`), so the server's `error` body would never be seen.
- Produces in `be.submanifold.pentelive.net.AuthedHttp`:
  - `public Reply getAllowingErrors(String pathAndQuery) throws IOException` and `public Reply postFormAllowingErrors(String path, String form) throws IOException` — same `send(...)` path (login cookies, re-login retry) as `get`/`postForm`, but the exchange reads with a new `private static Reply readAllowingErrors(HttpURLConnection)`: when `getResponseCode() >= 400` it reads `getErrorStream()` (null stream → `""` body), else `getInputStream()`. Implement by passing a reader flag/function into `exchangeGet`/`exchangePost`; the existing `read()` and every existing caller keep today's behaviour.
- Produces (all in package `be.submanifold.pentelive.kids`):
  - `public final class KidAccounts` with nested `public static final class Player { public long pid; public String name; public int nameColor; public int tourneyWinner; }`, `public static final class Kid { public long kidPid; public String name; public boolean copyMessages; public boolean copyLiveChat; public List<Player> whitelist; }`, `public static final class Reply { public boolean success; public String error; public String message; public boolean isKid; public boolean emailVerified; public String parentName; public List<Player> whitelist; public List<Kid> kids; }`.
  - `public static Reply parse(String body)` — null for null, non-JSON or `success:false`; null `whitelist`/`kids` (top-level and per kid) become empty lists.
  - `public static String parseError(String body)` — the `error` string, or null.
  - `public static Kid findKid(Reply r, long kidPid)` — null when absent (e.g. after `release`).
  - `public static String createForm(String kidName, String kidPassword)`, `convertForm(String kidName)`, `passwordForm(long kidPid, String kidPassword)`, `whitelistAddForm(long kidPid, String playerName)`, `whitelistRemoveForm(long kidPid, long playerPid)`, `settingsForm(long kidPid, Boolean copyMessages, Boolean copyLiveChat)` (a null flag is omitted; true/false → `Y`/`N`), `releaseForm(long kidPid)`. Each starts with `action=<name>`.
  - `public final class KidAccountsClient` with `public static final class Failure extends IOException` (message = server `error` or null) and blocking methods that each return the parsed `KidAccounts.Reply` or throw `Failure`: `load()`, `create(String, String)`, `convert(String)`, `setPassword(long, String)`, `whitelistAdd(long, String)`, `whitelistRemove(long, long)`, `settings(long, Boolean, Boolean)`, `release(long)`. Every call uses `AuthedHttp.shared().getAllowingErrors` / `.postFormAllowingErrors`. A reply counts only when `code == 200` and `parse` is non-null; any other reply (any status) throws `new Failure(KidAccounts.parseError(body))`, so a 401/403/404/409 JSON body's `error` reaches the UI. A transport `IOException` propagates unchanged (UI shows `try_again`).

- [ ] **Step 1: Write the failing tests** in `KidAccountsTest.java` (use the contract's sample bodies verbatim):

```java
@Test public void parse_parentView() {
    Reply r = KidAccounts.parse("{\"success\":true,\"isKid\":false,\"emailVerified\":true,\"kids\":[{\"kidPid\":23000000012345,\"name\":\"kid_one\",\"copyMessages\":false,\"copyLiveChat\":true,\"whitelist\":[{\"pid\":23000000000777,\"name\":\"graviton\",\"nameColor\":0,\"tourneyWinner\":0}]}]}");
    assertFalse(r.isKid); assertTrue(r.emailVerified);
    assertEquals(23000000012345L, r.kids.get(0).kidPid);
    assertTrue(r.kids.get(0).copyLiveChat);
    assertEquals("graviton", r.kids.get(0).whitelist.get(0).name);
    assertTrue(r.whitelist.isEmpty());
}
@Test public void parse_kidView() {
    Reply r = KidAccounts.parse("{\"success\":true,\"isKid\":true,\"emailVerified\":true,\"parentName\":\"iostest\",\"whitelist\":[{\"pid\":23000000000777,\"name\":\"graviton\",\"nameColor\":0,\"tourneyWinner\":0}],\"kids\":[]}");
    assertTrue(r.isKid); assertEquals("iostest", r.parentName); assertEquals(1, r.whitelist.size());
}
@Test public void parse_missingListsBecomeEmpty() {
    Reply r = KidAccounts.parse("{\"success\":true,\"kids\":[{\"kidPid\":1,\"name\":\"k\"}]}");
    assertTrue(r.whitelist.isEmpty()); assertTrue(r.kids.get(0).whitelist.isEmpty());
}
@Test public void parse_htmlOrNull_isNull() {
    assertNull(KidAccounts.parse("<html>login</html>")); assertNull(KidAccounts.parse(null));
}
@Test public void parse_failure_isNullAndErrorIsReadable() {
    String body = "{\"success\":false,\"error\":\"Kid not found\"}";
    assertNull(KidAccounts.parse(body)); assertEquals("Kid not found", KidAccounts.parseError(body));
}
@Test public void findKid_absentIsNull() { /* parent view above: findKid(r, 23000000012345L) non-null, findKid(r, 5L) null */ }
@Test public void forms() throws Exception {
    assertEquals("action=create&kidName=kid_one&kidPassword=pw_1", KidAccounts.createForm("kid_one", "pw_1"));
    assertEquals("action=convert&kidName=kid_one", KidAccounts.convertForm("kid_one"));
    assertEquals("action=password&kidPid=5&kidPassword=abcde", KidAccounts.passwordForm(5L, "abcde"));
    assertEquals("action=whitelistAdd&kidPid=5&playerName=graviton", KidAccounts.whitelistAddForm(5L, "graviton"));
    assertEquals("action=whitelistRemove&kidPid=5&playerPid=777", KidAccounts.whitelistRemoveForm(5L, 777L));
    assertEquals("action=settings&kidPid=5&copyMessages=Y", KidAccounts.settingsForm(5L, true, null));
    assertEquals("action=settings&kidPid=5&copyLiveChat=N", KidAccounts.settingsForm(5L, null, false));
    assertEquals("action=release&kidPid=5", KidAccounts.releaseForm(5L));
}
```

Write `findKid_absentIsNull` with real assertions as described in its comment. Also add to `KidAccountsTest`:

```java
@Test public void parseError_loginRequired401Body() {
    assertEquals("Login required", KidAccounts.parseError("{\"success\":false,\"error\":\"Login required\"}"));
    assertNull(KidAccounts.parseError("<html>login</html>"));
}
```

Append to `AuthedHttpTest.java` (reuse its existing MockWebServer setup and the helper it uses to serve a login response plus a data response; log in successfully first as the existing tests do):

```java
@Test public void getAllowingErrors_returnsErrorBodyAndCode() throws Exception {
    // data handler: 404 with body {"success":false,"error":"Kid not found"}
    AuthedHttp.Reply r = http.getAllowingErrors("/gameServer/kids");
    assertEquals(404, r.code);
    assertEquals("{\"success\":false,\"error\":\"Kid not found\"}", r.body);
}
@Test public void postFormAllowingErrors_returnsErrorBodyAndCode() throws Exception {
    // data handler: 409 with body {"success":false,"error":"Requested name kid_one is already taken, please choose another."}
    AuthedHttp.Reply r = http.postFormAllowingErrors("/gameServer/kids", "action=create&kidName=kid_one&kidPassword=pw_1");
    assertEquals(409, r.code);
    assertTrue(r.body.contains("already taken"));
}
@Test public void get_stillThrowsOnErrorStatus() throws Exception {
    // data handler: 404 → http.get("/gameServer/kids") throws IOException (existing behaviour kept for other callers)
}
```

Write the handler setup and the `get_stillThrowsOnErrorStatus` assertion (`assertThrows(IOException.class, ...)` or try/fail) for real, following the existing tests' style.

- [ ] **Step 2: Run to verify they fail**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.kids.KidAccountsTest" --tests "be.submanifold.pentelive.net.AuthedHttpTest"`
Expected: FAIL, compilation errors (package `kids` does not exist; `getAllowingErrors` not found).

- [ ] **Step 3: Implement the `AuthedHttp` variants, then `KidAccounts` and `KidAccountsClient`** as in Interfaces. Path constant `"/gameServer/kids"`. Form builders wrap `Forms.encode` and rethrow `UnsupportedEncodingException` as `IllegalStateException`. Client logs failures with `RedactingLog.e(TAG, "kids request failed, response code " + code, null)` (never the body).

- [ ] **Step 4: Run to verify they pass**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.kids.KidAccountsTest" --tests "be.submanifold.pentelive.net.AuthedHttpTest"`
Expected: PASS — `KidAccountsTest` 8 tests; `AuthedHttpTest` all existing tests plus the 3 new ones.

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java app/src/main/java/be/submanifold/pentelive/kids/KidAccountsClient.java app/src/main/java/be/submanifold/pentelive/net/AuthedHttp.java app/src/test/java/be/submanifold/pentelive/kids/KidAccountsTest.java app/src/test/java/be/submanifold/pentelive/net/AuthedHttpTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): model, parser and client for /gameServer/kids; AuthedHttp variants that return error bodies"
```

---

### Task 3: Private messages — refusal strings and the kid composer

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/MessageSendReply.java`
- Modify: `app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java` (add the suggestion list)
- Modify: `app/src/main/java/be/submanifold/pentelive/SendMessageActivity.java:40-42` (adapter; the send click at `:48-62` is not changed), `:120-131` (`doInBackground`), `:148-156` (`onPostExecute`)
- Modify: `app/src/main/java/be/submanifold/pentelive/ReplyMessageActivity.java:296-304`, `:322-330`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`
- Test: `app/src/test/java/be/submanifold/pentelive/MessageSendReplyTest.java`, `app/src/test/java/be/submanifold/pentelive/kids/KidAccountsTest.java` (append)

**Interfaces:**
- Consumes: `KidAccounts.Reply`, `KidAccountsClient.load()` (Task 2); `PentePlayer.isKid()` (Task 1).
- Produces:
  - `public final class MessageSendReply { public enum Outcome { SENT, NO_SUCH_USER, NOT_APPROVED, IGNORED } public static Outcome classify(String body, String recipient); }` — `NO_SUCH_USER` when the body contains `"Error: Player " + recipient + " not found."` (the existing check, unchanged), `NOT_APPROVED` when it contains `You can only send messages to players your parent approved.`, `IGNORED` when it contains `Player is ignoring your messages.`, `SENT` otherwise. A null body returns `NO_SUCH_USER` (the failure the screens already show today).
  - `public static List<String> KidAccounts.allowedRecipients(Reply r)` — autocomplete suggestions only: for a kid reply, `parentName` first (when non-null), then whitelist names in order; empty for a non-kid reply or null `r`. It is never used to refuse a send (the server also allows admins, system and computer accounts, which the client cannot know).
  - Strings: `kid_not_approved_recipient` = `You can only send messages to players your parent approved.` / de `Du kannst nur Spielern schreiben, die deine Eltern freigegeben haben.`; `player_ignoring_messages` = `Player is ignoring your messages.` / de `Dieser Spieler ignoriert deine Nachrichten.`

- [ ] **Step 1: Write the failing tests**

`MessageSendReplyTest.java`:
```java
@Test public void notFound() { assertEquals(Outcome.NO_SUCH_USER, MessageSendReply.classify("<p>Error: Player bob not found.</p>", "bob")); }
@Test public void kidNotApproved() { assertEquals(Outcome.NOT_APPROVED, MessageSendReply.classify("<p>You can only send messages to players your parent approved.</p>", "bob")); }
@Test public void ignored() { assertEquals(Outcome.IGNORED, MessageSendReply.classify("<p>Player is ignoring your messages.</p>", "bob")); }
@Test public void ordinaryPage_isSent() { assertEquals(Outcome.SENT, MessageSendReply.classify("<html>messages</html>", "bob")); }
@Test public void nullBody_isNotSent() { assertNotEquals(Outcome.SENT, MessageSendReply.classify(null, "bob")); }
```

Append to `KidAccountsTest.java`:
```java
@Test public void allowedRecipients_includesParentFirst() {
    Reply r = KidAccounts.parse("{\"success\":true,\"isKid\":true,\"parentName\":\"iostest\",\"whitelist\":[{\"pid\":7,\"name\":\"graviton\"}]}");
    assertEquals(Arrays.asList("iostest", "graviton"), KidAccounts.allowedRecipients(r));
}
@Test public void allowedRecipients_parentView_isEmpty() {
    assertTrue(KidAccounts.allowedRecipients(KidAccounts.parse("{\"success\":true,\"isKid\":false}")).isEmpty());
}
@Test public void allowedRecipients_null_isEmpty() {
    assertTrue(KidAccounts.allowedRecipients(null).isEmpty());
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.MessageSendReplyTest" --tests "be.submanifold.pentelive.kids.KidAccountsTest"`
Expected: FAIL, compilation errors (`MessageSendReply`, `allowedRecipients` not found).

- [ ] **Step 3: Implement**
  - `MessageSendReply.classify`: null → `NO_SUCH_USER`; then the three `contains` checks in the order NOT_APPROVED, IGNORED, NO_SUCH_USER; else `SENT`.
  - `SendMessageActivity` and `ReplyMessageActivity`: `doInBackground` returns the `Outcome` (IOException → `NO_SUCH_USER`, as today's `false`); change the task type to `BackgroundTask<Void, MessageSendReply.Outcome>`. `onPostExecute`: `SENT` keeps today's success path; `NO_SUCH_USER` keeps `setError(no_such_user)`; `NOT_APPROVED` → `setError(kid_not_approved_recipient)`; `IGNORED` → `setError(player_ignoring_messages)`. `ReplyMessageActivity` has a `recipient` view too (`:328`), so use the same calls.
  - Kid composer, `SendMessageActivity.onCreate` only, when `PentePlayer.isKid()`: build the autocomplete adapter with an **empty** list instead of `PrefUtils.getPlayers` (`:41`), so unfiltered recent players never show. Start a `BackgroundTask` that calls `KidAccountsClient.load()`; on success (guard `isFinishing() || isDestroyed()`) swap the items with `adapter.clear(); adapter.addAll(KidAccounts.allowedRecipients(reply)); adapter.notifyDataSetChanged();`. On failure keep the empty list, no dialog. The send click is **not** changed: no client-side recipient check; a refusal comes back from the server and `MessageSendReply` maps it to `kid_not_approved_recipient`. Non-kids: no change at all.
  - Add the two strings to both `strings.xml` files.

- [ ] **Step 4: Run to verify they pass, and that the app compiles**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.MessageSendReplyTest" --tests "be.submanifold.pentelive.kids.KidAccountsTest"`
Expected: PASS (5 + 11 tests).
Run: `GRADLE :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.
Run: `git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android diff app/src/main/java/be/submanifold/pentelive/SendMessageActivity.java`
Expected: no change inside the send click listener (no recipient check or `return` added for kids).

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/MessageSendReply.java app/src/main/java/be/submanifold/pentelive/kids/KidAccounts.java app/src/main/java/be/submanifold/pentelive/SendMessageActivity.java app/src/main/java/be/submanifold/pentelive/ReplyMessageActivity.java app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml app/src/test/java/be/submanifold/pentelive/MessageSendReplyTest.java app/src/test/java/be/submanifold/pentelive/kids/KidAccountsTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): show message refusals and limit a kid's composer suggestions to approved players"
```

---

### Task 4a: Settings entry, strings and `KidAccountsActivity`

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/kids/KidAccountsActivity.java`
- Create: `app/src/main/res/layout/activity_kid_accounts.xml`, `app/src/main/res/layout/kid_row.xml`
- Modify: `app/src/main/java/be/submanifold/pentelive/SettingsActivity.java:105-106` (click listeners), `app/src/main/res/layout/activity_settings.xml:193-217`, `app/src/main/AndroidManifest.xml:81-83`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml` (all strings of the table below, including those 4b uses)

**Interfaces:**
- Consumes: `KidAccounts`, `KidAccountsClient` (Task 2), `be.submanifold.pentelive.BackgroundTask`, `MyApplication.activityResumed/activityPaused`, `NameStyle.colored(String, int)` + `NameStyle.appendCrown(Context, SpannableStringBuilder, int, TextView)`. Mirror `blocked/BlockedPlayersActivity.java` and `res/layout/activity_blocked_players.xml`: Toolbar `my_toolbar` with `britishracinggreen` background, RecyclerView + `emptyText`, the `Outcome {reply; error}` holder and the `onResume` load at `:103-140` (`isFinishing() || isDestroyed()` guard; a failed load keeps what is shown and shows the `Failure` message or `try_again`).
- Produces: `KidAccountsActivity` (no extras); Settings button id `kidAccountsButton`; every string in the table below (4b only consumes them).

Behaviour (no unit tests: the logic is tested in Task 2; verified by build + Task 7):
- **Settings.** New `Button` `@+id/kidAccountsButton`, text `@string/kid_accounts`, `layout_below="@id/blockedPlayersButton"`. Re-point `infoLabel3` and `avatarView` from `blockedPlayersButton` to `kidAccountsButton`. `blockedPlayersButton` stays visible for everyone, kids included (no visibility code). Click opens `KidAccountsActivity`. Register `KidAccountsActivity` and `KidDetailActivity` in the manifest after `.blocked.BlockedPlayersActivity`, `android:screenOrientation="portrait"` (4b creates the second class; add a minimal stub here so the manifest compiles, 4b replaces it).
- **`KidAccountsActivity`**, loads with `KidAccountsClient.load()` in `onResume`.
  - Kid reply (`isKid`): show `kid_managed_by` (with `parentName`) and the read-only approved list (`whitelist`); hide the create/convert buttons. No write actions.
  - Parent reply: intro text `kid_accounts_intro`; a list of `kids` (row `kid_row.xml` = styled kid name; tap opens `KidDetailActivity` with `KidDetailActivity.EXTRA_KID_PID`); `emptyText` = `kid_none` once loaded and empty; buttons `kid_create` and `kid_convert`.
  - When `!reply.emailVerified` (parent view): show `kid_verify_email` above the buttons and `setEnabled(false)` on `kid_create` and `kid_convert`; re-enable when a later load says verified.
  - `kid_create` dialog: two `EditText`s (name, password with `TYPE_TEXT_VARIATION_PASSWORD`) → `create`. `kid_convert` dialog: one name field plus `kid_convert_hint` → `convert`; on success show the reply's `message` in a Toast as-is (the server's reply is identical whether or not anything happened; never infer the target's status).
  - Every write replaces the shown list with the returned reply. Any failure: dialog with the `Failure` message (server `error`, e.g. `Verify your email address before adding kids`, `Requested name … already taken`) or `try_again`. A failed load keeps what is shown and never shows `kid_none`.

Strings (English / German):

| name | English | German |
|---|---|---|
| `kid_accounts` | Kid accounts | Kinderkonten |
| `kid_accounts_intro` | Kids can play anyone, but they can only exchange messages with players you approve. | Kinder können gegen alle spielen, aber nur mit Spielern schreiben, die du freigibst. |
| `kid_verify_email` | Verify your email address before adding kids. | Bestätige deine E-Mail-Adresse, bevor du Kinderkonten hinzufügst. |
| `kid_none` | No kid accounts yet. | Noch keine Kinderkonten. |
| `kid_create` | Create kid account | Kinderkonto erstellen |
| `kid_convert` | Make an existing account a kid account | Bestehendes Konto zum Kinderkonto machen |
| `kid_convert_hint` | We will email that account to confirm. | Wir schicken dem Konto eine Bestätigungs-E-Mail. |
| `kid_name` | Username | Benutzername |
| `kid_password` | Password | Passwort |
| `kid_managed_by` | Your account is managed by %1$s. | Dein Konto wird von %1$s verwaltet. |
| `kid_approved_players` | Approved players | Freigegebene Spieler |
| `kid_no_approved` | No approved players yet. | Noch keine freigegebenen Spieler. |
| `kid_add_approved` | Approve a player | Spieler freigeben |
| `kid_remove_approved_title` | Remove %1$s? | %1$s entfernen? |
| `kid_remove_approved_text` | %1$s will no longer be able to exchange messages with %2$s. | %1$s kann dann keine Nachrichten mehr mit %2$s austauschen. |
| `kid_remove` | Remove | Entfernen |
| `kid_copy_messages` | Email me copies of private messages | Kopien privater Nachrichten per E-Mail |
| `kid_copy_live_chat` | Email me live-room chat when my kid leaves | Live-Chat per E-Mail, wenn mein Kind den Raum verlässt |
| `kid_copy_live_chat_note` | Live-chat copies are skipped if the server can\'t store the chat. | Live-Chat-Kopien entfallen, wenn der Server den Chat nicht speichern kann. |
| `kid_set_password` | Set password | Passwort setzen |
| `kid_password_set` | Password changed. | Passwort geändert. |
| `kid_release` | Release kid account | Kinderkonto freigeben |
| `kid_release_text` | %1$s becomes a normal account. Approved players and copy settings are deleted. | %1$s wird ein normales Konto. Freigegebene Spieler und Kopie-Einstellungen werden gelöscht. |

- [ ] **Step 1: Add the strings, layouts, manifest entries and the `KidDetailActivity` stub; build to verify resources compile**

Run: `GRADLE :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Implement `KidAccountsActivity` and the Settings button as described above**

- [ ] **Step 3: Build and run the whole unit suite**

Run: `GRADLE :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, no test failures.
Run: `grep -n "blockedPlayersButton" /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/SettingsActivity.java`
Expected: only the existing click-listener line; no `setVisibility` on it.

- [ ] **Step 4: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/kids/KidAccountsActivity.java app/src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java app/src/main/res/layout/activity_kid_accounts.xml app/src/main/res/layout/kid_row.xml app/src/main/java/be/submanifold/pentelive/SettingsActivity.java app/src/main/res/layout/activity_settings.xml app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): Kid accounts screen and Settings entry"
```

---

### Task 4b: `KidDetailActivity`

**Files:**
- Modify (replace the 4a stub): `app/src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java`
- Create: `app/src/main/res/layout/activity_kid_detail.xml`

**Interfaces:**
- Consumes: `KidAccounts.findKid`, `KidAccountsClient` (Task 2); strings from 4a. Mirror from `blocked/BlockedPlayersActivity.java`: the `ItemTouchHelper.SimpleCallback(0, LEFT | RIGHT)` swipe at `:87-101`, `confirmUnblock` at `:186` (confirm dialog that restores the swiped row on cancel) and `unblock` at `:210`; the `onResume` load at `:103-140`; row binding with `NameStyle` as in its adapter.
- Produces: `public static final String EXTRA_KID_PID = "kidPid"` (long).

Behaviour:
- Loads with `load()` in `onResume` and shows `KidAccounts.findKid(reply, kidPid)`; if null (released elsewhere), `finish()`.
- Title = kid name. Two `SwitchCompat`s: `kid_copy_messages`, `kid_copy_live_chat`, plus the note `kid_copy_live_chat_note` under the second. Set their checked state without firing the listener (set listener to null, `setChecked`, re-attach). Toggling calls `settings(kidPid, flag, null)` or `settings(kidPid, null, flag)`; on failure revert the switch (same null-listener pattern) and show the error dialog.
- Approved list (`kid_approved_players`): rows as in blocked players; swipe left/right → confirm `kid_remove_approved_title`/`kid_remove_approved_text` (positive `kid_remove`; cancel restores the row) → `whitelistRemove(kidPid, pid)`. Empty text `kid_no_approved`. Button `kid_add_approved` → name dialog → `whitelistAdd`.
- Button `kid_set_password` → password dialog (`TYPE_TEXT_VARIATION_PASSWORD`) → `setPassword`; on success Toast `kid_password_set`.
- Button `kid_release` → confirm `kid_release_text` → `release`; on success `finish()`.
- Every success re-renders from the returned reply. Every failure: dialog with the `Failure` message or `try_again`.

- [ ] **Step 1: Layout, then implement as described above**

- [ ] **Step 2: Build and run the whole unit suite**

Run: `GRADLE :app:assembleDebug :app:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, no test failures.

- [ ] **Step 3: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/kids/KidDetailActivity.java app/src/main/res/layout/activity_kid_detail.xml
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): kid detail screen (approved players, copy toggles, password, release)"
```

---

### Task 5: Broadcast and turn-based message input for kids

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LobbyActivity.java:90-93` (`onCreateOptionsMenu`), `:277-290` (`BroadcastTask.onPostExecute`)
- Modify: `app/src/main/java/be/submanifold/pentelive/BoardActivity.java:76` (after `messageView` is inflated)
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`

**Interfaces:**
- Consumes: `PentePlayer.isKid()` (Task 1).
- Produces: strings `kid_broadcast_refused` = `Kid accounts can\'t broadcast.` / de `Kinderkonten können nicht rundsenden.`; `kid_chat_hint` = `Only players your parent approved will see your messages.` / de `Nur von deinen Eltern freigegebene Spieler sehen deine Nachrichten.` (Task 6 reuses `kid_chat_hint`).

- [ ] **Step 1: Implement**
  - `LobbyActivity.onCreateOptionsMenu`: after inflating, `menu.findItem(R.id.broadcast).setVisible(!PentePlayer.isKid())`.
  - `BroadcastTask.onPostExecute`: new first branch `dashboardString.contains("Kid accounts can't broadcast.")` → Toast `kid_broadcast_refused` (covers a stale menu or a server-side kid flag the app has not seen yet).
  - `BoardActivity`: when `PentePlayer.isKid()`, `((EditText) messageView.findViewById(R.id.messageInput)).setHint(R.string.kid_chat_hint)`. The input stays usable (the server drops text to non-approved players silently).
  - Add both strings to both `strings.xml` files.

- [ ] **Step 2: Build**

Run: `GRADLE :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LobbyActivity.java app/src/main/java/be/submanifold/pentelive/BoardActivity.java app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): hide broadcast for kids and hint on the game message input"
```

---

### Task 6: Native live room kid mode

**Files:**
- Create: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/KidLiveRoom.java`
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/TablesAndPlayers.java:126-135` (`login`), add `isKid()`
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java:477-494` (invite response lines), `:773-822` (`receivedInvitation`)
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomFragment.java:135-147` (lobby chat dialog)
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java:322-339` (table chat dialog), `:1585-1600` (invite dialog in `showInvitePlayers`)
- Test: `app/src/test/java/be/submanifold/pentelive/liveGameRoom/KidLiveRoomTest.java`

**Interfaces:**
- Consumes: the `Map` from `LiveGameRoomActivity.jsonToMap` (nested objects are `Map<String, Object>`, numbers are `Integer` or `Long`); string `kid_chat_hint` (Task 5).
- Produces:
  - `final class KidLiveRoom { static boolean isKidLogin(Map<String, ?> loginEvent); static String quotedSuffix(String text); }` — `isKidLogin` is true iff `loginEvent.get("me")` is a `Map` whose `"parentPid"` is a `Number`; any other shape (missing `me`, guest login, null) is false. `quotedSuffix` returns `""` for null or empty, else `": \"" + text + "\""` (the same text `label_quoted` builds today).
  - `public boolean TablesAndPlayers.isKid()`, set in `login(...)` via `KidLiveRoom.isKidLogin(data)`.

- [ ] **Step 1: Write the failing tests** in `KidLiveRoomTest.java`:

```java
@Test public void kidLogin_hasMeParentPid() {
    Map<String, Object> me = new HashMap<>(); me.put("parentPid", 23000000012345L);
    Map<String, Object> login = new HashMap<>(); login.put("me", me);
    assertTrue(KidLiveRoom.isKidLogin(login));
}
@Test public void normalLogin_noParentPid() {
    Map<String, Object> login = new HashMap<>(); login.put("me", new HashMap<String, Object>());
    assertFalse(KidLiveRoom.isKidLogin(login));
}
@Test public void oldServerOrGuest_noMe() {
    assertFalse(KidLiveRoom.isKidLogin(new HashMap<String, Object>()));
    assertFalse(KidLiveRoom.isKidLogin(null));
}
@Test public void parentPidAsInteger_stillKid() { /* same as kidLogin_hasMeParentPid with Integer 5 */ }
@Test public void quotedSuffix_text() { assertEquals(": \"hi\"", KidLiveRoom.quotedSuffix("hi")); }
@Test public void quotedSuffix_emptyOrNull_isEmpty() {
    assertEquals("", KidLiveRoom.quotedSuffix("")); assertEquals("", KidLiveRoom.quotedSuffix(null));
}
```

Write `parentPidAsInteger_stillKid` with real assertions as its comment says.

- [ ] **Step 2: Run to verify they fail**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.KidLiveRoomTest"`
Expected: FAIL, compilation error `cannot find symbol KidLiveRoom`.

- [ ] **Step 3: Implement `KidLiveRoom`, then wire it**
  - `TablesAndPlayers`: `private boolean kid;` set in `login`; `isKid()` getter.
  - Lobby (`LiveGameRoomFragment`) and table (`LiveTableFragment`) chat dialogs: when `activity.tablesAndPlayers.isKid()`, `input.setHint(R.string.kid_chat_hint)`. Sending is unchanged.
  - `LiveTableFragment.showInvitePlayers` invite dialog: when kid, do not `setView(invitationText)` and send `"inviteText":""`.
  - `LiveGameRoomActivity.receivedInvitation`: hide `invitationText` (`GONE`) when `inviteText` is null or empty (all users); when kid, hide `replyText` (`GONE`) and pass `""` as the reply text for accept, decline and ignore.
  - `dsgInviteResponseTableEvent` lines (`:487`, `:489`, `:491`): replace `+ ": \"" + responseText + "\""` with `+ KidLiveRoom.quotedSuffix(responseText)` (all users).
  - No other live events change: the contract adds no refusal events, so there is nothing new to decode.

- [ ] **Step 4: Run to verify they pass, then build**

Run: `GRADLE :app:testDebugUnitTest --tests "be.submanifold.pentelive.liveGameRoom.KidLiveRoomTest"`
Expected: PASS, 6 tests.
Run: `GRADLE :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android add app/src/main/java/be/submanifold/pentelive/liveGameRoom/KidLiveRoom.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/TablesAndPlayers.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomFragment.java app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java app/src/test/java/be/submanifold/pentelive/liveGameRoom/KidLiveRoomTest.java
git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android commit -S -m "feat(kids): live room kid mode (chat hint, no invite text, empty text without quotes)"
```

---

### Task 7: Full suite and local end-to-end check

**Files:** none committed. `PentePlayer.development` is flipped to `true` only for this check and reverted before finishing.

**Interfaces:**
- Consumes: everything above; a local pente.org stack running the `kids-accounts` server branch (SQL migration applied). Test accounts `iostest` (parent, verified email) and `graviton` are in `pente-react-native/.env.local` (never commit them).

- [ ] **Step 1: Full unit suite and debug build**

Run: `GRADLE :app:testDebugUnitTest :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, 0 failures.

- [ ] **Step 2: Manual check on an emulator against the local backend** (`PentePlayer.development = true`, not committed). Record pass/fail for each:
  Steps run in this order; "second client" = `graviton` on the website or a second emulator.
  1. As `iostest`: Settings → Kid accounts → create `kid_e2e` → open it → approve `graviton` → toggle both copies → set password → all succeed and re-render.
  2. Create with a taken name → the server's `Requested name ... already taken` text (409 body) is shown, not `try_again`. Approve a nonexistent name → `Player not found` shown.
  3. Convert `kid_e2e` (already a kid) → the normal `Confirmation email sent to kid_e2e` Toast, no error (neutral reply).
  4. Optional, if a local parent with `email_valid` not `Y` is available: Kid accounts shows `kid_verify_email` and both buttons are disabled.
  5. Log in as `kid_e2e`: Settings shows both Blocked players and Kid accounts; Kid accounts shows "managed by iostest" and `graviton`; New message autocomplete is empty until loaded, then offers only `iostest`, `graviton`; lobby menu has no Broadcast; live lobby and table chat show the hint; the live invite dialog has no text field.
  6. As `iostest`: remove `graviton` from `kid_e2e`'s approved list.
  7. As `kid_e2e`: send a message to `graviton` → the request is sent and the server's refusal shows `kid_not_approved_recipient` on the recipient field.
  8. Second client as `graviton`: message `kid_e2e` → `graviton` gets `Player is ignoring your messages.` (on Android: `player_ignoring_messages`).
  9. Live room: `graviton` types in the lobby and at a shared table → `kid_e2e` sees none of those lines; `graviton` invites `kid_e2e` to a table with text → `kid_e2e` sees the invite with no message line.
  10. Second client as `graviton`: create a turn-based invitation to `kid_e2e` with a message → on Android as `kid_e2e`, the invitation shows no inviter text and can be accepted; the accepted game's screen also shows no such text (server D3 filtering).
  11. As `kid_e2e`: block `graviton` from a message reply screen → `graviton` appears in Blocked players; unblock works.
  12. Back as `iostest`: release `kid_e2e` → detail screen closes, list no longer shows it.

- [ ] **Step 3: Revert and tidy**

Run: `git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android checkout -- app/src/main/java/be/submanifold/pentelive/PentePlayer.java` only if the only change left in it is the `development` flag (check `git -C ... diff app/src/main/java/be/submanifold/pentelive/PentePlayer.java` first), then `git -C /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android status --porcelain`.
Expected: clean working tree, no untracked files.

---

## Self-review notes (writer)

- Spec coverage (Android bullets): Kid accounts entry next to `blockedPlayersButton` with native screens → Tasks 4a/4b; blocked players kept for kids (controller decision D1 reverses the spec's hiding) → Task 4a leaves it alone, Task 7 step 11 checks it; composer suggestions limited to parent + whitelist, sends never blocked client-side (D2) → Task 3; server error bodies on non-200 statuses → Task 2 `AuthedHttp` variants; unverified-email parent → Task 4a; chat input hint → Tasks 5 (TB input) and 6 (live room); broadcast hidden → Task 5; refusal strings where the app matches error strings → Tasks 3 and 5; kid flag via `PentePlayer` Parcelable from index JSON → Task 1; live room chat hint, hidden invite text, refusal handling → Task 6 (the contract defines no refusal event, so nothing to decode). Parent actions list/create/convert → Task 4a; password/whitelist add+remove/two toggles/release → Task 4b; Redis-unavailable note in the parent UI → `kid_copy_live_chat_note`.
- Settled ambiguities: (a) "chat input hint" is applied to every chat input a kid has (live lobby, live table, TB move message), since the server drops TB text silently too; (b) a kid opening Kid accounts sees a read-only view (parent name + approved list) rather than a hidden button, using the GET kid view the contract already returns; (c) the composer suggests the parent plus the whitelist only (contract) and never refuses a send itself; the server decides, which also covers admins/system/computer recipients the client cannot know; (d) `convert` replies are shown as-is since the server makes them neutral.
- Out of scope, deliberately not touched: TB invitation UI (the server omits non-allowed inviter text for kids per D3; Android renders what arrives, checked in Task 7 step 10), `SocialActivity`, `emailMeToggleButton`, `WebViewActivity`, push notifications (server-enforced; not in the spec's Android list).
