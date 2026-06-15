# PR #7 Review — Extract PenteRules engine + PenteApi transport layer

**Date:** 2026-06-13
**Reviewer:** Multi-agent local review (adversarially verified)
**Diff range:** `a74035d..701a8a9`
**Status:** Merged to `main`, app already deployed. Findings below are fast-follow fixes.

---

## 1. Verdict

This is a well-scoped, genuinely behavior-preserving refactor and it is **safe as deployed**. The rules-engine extraction is conservatively gated: only six proven-equivalent, non-rated variants (PENTE, KERYO_PENTE, POOF_PENTE, O_PENTE, GOMOKU, CONNECT6) delegate to the new `:rules` engine, while every rated game, G_PENTE, Swap2/Speed-Swap2, D-Pente, and Go retain the legacy `replay*`/`detect*` path. The new transport seam (OkHttp + single-flight re-auth) is wired to exactly one low-risk endpoint (whos-online) and its concurrency design holds up under tracing — the executor is single-threaded, `authGeneration` is volatile, and the documented "200 + null body = expired" re-auth contract is intentional. The differential test corpus and the deliberately legacy-matching transport semantics survived adversarial scrutiny. **Exactly one substantive issue** survived verification: a **medium-severity visual regression in the Go dead-stone scoring phase** caused by `BoardView` now reading a cached `BoardState` snapshot that `processDeadStone()` never refreshes. It does not affect scores, rated outcomes, or any non-Go game, and it self-corrects on the next server reparse — so deployment risk is low and a fast-follow patch is sufficient. No critical or high-severity defects were confirmed.

---

## 2. Confirmed findings (by adjusted severity)

| # | Severity | Title | File:line | Category | One-line fix |
|---|----------|-------|-----------|----------|--------------|
| 1 | medium | Go dead-stone marking renders a stale board snapshot | `Game.java:3068-3086`; `BoardView.java:444,489` | correctness / wiring-regression | Reassign `this.state` from `abstractBoard` at the end of `processDeadStone()` |

---

## 3. Detailed findings

### Finding 1 — Go dead-stone marking renders a stale board snapshot

- **Severity:** medium
- **Category:** correctness / wiring-regression
- **Files:** `app/src/main/java/be/submanifold/pentelive/Game.java:3068-3086`; `app/src/main/java/be/submanifold/pentelive/BoardView.java:443-448, 489-492`
- **Affects:** Live (subscriber) Go games played through to the dead-stone scoring phase.

**Description**

Before this PR, `BoardView` aliased the *live* board array (`abstractBoard = game.abstractBoard` in `setGame`), so any in-place mutation of the board appeared on the next `invalidate()`. The refactor removed that alias and changed `BoardView.drawBoard` to read `game.getState().board`. `getState()` returns a **cached** `BoardState` whose `board` is a **deep copy**, and the cache is only rebuilt when `state == null` or reassigned by `finishBoardState()` / `replayGame(byte...)`.

`Game.processDeadStone(int)` — the Go scoring "mark dead stones" handler — mutates `abstractBoard` directly (sets a stone to `0` or restores it) and recomputes territories, but it **never reassigns `state`**. `BoardView.onTouchEvent` calls `game.processDeadStone(playedMove)` then `invalidate()`, with no replay/reparse, so `drawBoard` re-reads the stale snapshot. During dead-stone marking, tapping a stone produces no visual change: the opaque live stone from the stale snapshot is still drawn, and the alpha-180 "dead" marker is composited on top, so the translucency cue that signals a dead stone is lost. Users get no feedback when marking, may double-tap, and inadvertently un-mark.

Scope is bounded: the **score is unaffected** — `getTerritories()` / `getMovesForValue()` read the live `abstractBoard` via `getPosition()`, so rated outcomes are correct — and the display self-corrects on the next server reparse. The *restore* tap actually renders correctly (the snapshot retains the original stone); only the *mark-dead* visual fails.

**Evidence**

```java
// Game.java:3070-3085  (processDeadStone) — mutates abstractBoard, no state refresh
int pos = abstractBoard[i][j];
...
abstractBoard[i][j] = 0;   // or restore
...
getTerritories();          // recomputed from live board; state never reassigned
```

```java
// BoardView.java:443-448  (onTouchEvent) — mutate then invalidate, no reparse
} else if (game.isGoMarkStones() && playedMove > -1) {
    game.processDeadStone(playedMove);
}
...
invalidate();
```

```java
// BoardView.java:489-492  (drawBoard) — reads the CACHED deep-copied snapshot
byte[][] board = game.getState().board;
...
drawStone(canvas, i, j, board[i][j]);
```

```java
// Game.java:292-298  (getState) — rebuilds only when state == null
state = new BoardState(abstractBoard, ...);   // BoardState ctor: this.board = deepCopy(board)
```

**Concrete fix**

Refresh the snapshot after the in-place mutation. Preferred: at the end of `processDeadStone()`, reassign `state` from the live board, e.g.

```java
this.state = new BoardState(
    this.abstractBoard, whiteCaptures, blackCaptures, gridSize,
    this.state != null ? this.state.lastMove : -1,
    null, false, false, koMove);
```

Alternatives: have `getState()` rebuild from `abstractBoard` on the Go path, or have `BoardView` re-fetch via a method that re-snapshots. Add a regression test that toggles a dead stone and asserts `getState().cell(...)` reflects the mutation.

---

## 4. Refuted / out-of-scope (checked and dismissed)

The following candidates were investigated and **refuted** after verification. They are listed so reviewers can trust coverage breadth. In most cases the code was read correctly but the conclusion did not hold (dead code, intentional/documented design, pre-existing legacy behavior, or behavior that moves toward correctness).

**Rules engine correctness**
- *`isWin`/`fiveInARow` adds a center-cell guard absent from legacy `detectPente`.* Mechanically real, but `isWin` has **zero production callers** (grep: only the interface, `DefaultPenteRules`, and `WinDetectionTest`); the delegated path still uses legacy `detectPente`. The divergence is also *corrective*, never producing a wrong win. Not a defect.
- *`isWin` capture-win uses exact `== 10`, which Keryo trio captures could overshoot.* Dead code in production (legacy inline `==10` check decides wins on every executed path); no divergence from legacy. Latent at most.
- *Differential test corpus uses only distinct cells and never invokes `isWin`.* Test-coverage observation about a method with no production caller; re-occupation onto an emptied cell is byte-identical to placing on empty in both engines, so no divergent path exists. Not a new defect.
- *Delegation predicate (`Variants.fromGameType` `contains`) is broader than legacy `equals` dispatch.* For all six shipping labels the predicates agree; only a hypothetical future `*-Pente` label would diverge, and adding such a label requires editing the same dispatch chain. Defensive-hardening preference, not an active defect.

**Transport concurrency & re-auth**
- *Re-auth holds `authLock` across the blocking network login.* This is the **explicitly documented single-flight design** (one re-login under lock; others observe the generation bump and skip to retry). `authGeneration` is volatile so the common path never locks; the executor is single-threaded so the lock is never even contended. No deadlock, bounded by OkHttp timeouts.
- *whos-online null/empty body triggers re-auth + error toast (twice reported).* The "200 + null/sentinel body = expired" signal is intentional, documented design. An empty room list returns a non-null empty `List`, rendering identically to legacy; a genuinely null body corresponds to logged-out, where transparent re-auth is strictly better. Only a pathological persistent-null-with-valid-session case is worse, which is speculative.
- *A new `OkHttpClient` is built per Activity and never shut down.* No thread-pool duplication (all calls are synchronous `execute()`, never `enqueue()`); idle connections self-evict on the shared TaskRunner; portrait-lock prevents rotation recreation. At most a few idle sockets for minutes — minor code-quality nit, not a leak.
- *`Cancelable.cancel()` only suppresses the callback, never cancels the in-flight `Call`.* Documented, intentional contract; retention is bounded by request timeout and improves on the legacy AsyncTask it replaces. No leak.
- *`enqueue()` after `shutdown()` throws `RejectedExecutionException`.* Unreachable: both `enqueue` (menu click) and `shutdown` (`onDestroy`) run on the serialized main Looper and cannot race. Defensive-guard suggestion only.
- *Wrong-credentials re-auth has no negative caching (re-issues a doomed login).* Exact legacy parity (`PentePlayer` did the same); retry capped at one, user-rate bound, rare precondition, error surfaced. Behavior-preserving.
- *`InMemoryCookieJar` is process-local/empty and isolated from the legacy `CookieManager`.* Legacy whos-online forwarded only `name2`/`password2` cookies (redundant with the query string), never a session token; auth is stateless query-param based. No drift, no failure on the migrated endpoints.

**Transport correctness & API parity**
- *whos-online failure now surfaced as a Toast (legacy was silent).* Intentional, beneficial UX via the new typed-`Result`/`showError` seam; dispatched on the UI thread, cancelled in `onDestroy`. Strictly more information than legacy. Self-classified as an improvement.
- *Credentials now percent-encoded via `addQueryParameter` vs legacy raw concatenation.* Strictly more correct (server `getParameter()` decoding recovers identical values; fixes plus-addressed emails). No input yields a worse server-observed credential.
- *`PenteApi.submitMove` documented as POST but issues a GET.* GET matches legacy exactly; method is unwired; the HTTP verb is encapsulated so no caller can be misled. Cosmetic one-word Javadoc nit.
- *OkHttp `InMemoryCookieJar` not seeded from WebView `CookieManager`.* Same root cause as the cookie-jar item above; legacy never forwarded a session cookie for these endpoints. No functional impact.

**Android integration, lifecycle & leaks**
- *`PopupWindow` never dismissed in `onPause`/`onDestroy` → WindowLeaked.* Pre-existing legacy pattern not introduced by this PR (no `onDestroy` existed before); the worse failure (BadTokenException) is prevented by the new `cancel()` guard; popups are focusable so normal navigation dismisses them. Not a new defect.
- *`shutdown()`/`cancel()` do not interrupt the in-flight blocking call → Activity transiently retained.* Bounded by 15s/30s timeouts, no static root, identical to the legacy AsyncTask. Transient retention, not a leak.
- *`showWhosOnlinePopup` reassigns the shared `popupWindow` without dismissing the previous one.* Byte-for-byte pre-existing legacy code; the PR actually *reduced* the double-popup risk by adding single-flight cancellation. Practically unreachable.
- *Dark-mode: group header `Color.GRAY` and border stroke `@android:color/black` not theme-adaptive.* Not touched by this PR (diff confirms only `online_row_background` was added and `border.xml` was unmodified); renders acceptably in both themes. Cosmetic, pre-existing.

**Security**
- *Plaintext password in GET query strings across the new transport.* Pre-existing, pervasive backend API contract (~20+ legacy call sites); the new code faithfully replicates the wire format with no new logging/leak path (grep found no `Log`/`printStackTrace`/`toString` of requests). Remediation requires coordinated server changes; the refactor centralizes credential handling, easing any future fix.
- *`SharedPrefsSession` persists credentials in plaintext SharedPreferences, exposed via `allowBackup`.* Pre-existing and unchanged (`LoginActivity`/`RegisterActivity` already stored the password; manifest `allowBackup` untouched). The new `updateCredentials` write path is dead code. Pre-existing hardening note, not a defect introduced here.

**Documented, intentional quirks (out of scope by brief)**
- edge-0 win miss (faithful legacy port), G_PENTE move-2 cross-restriction (excluded from delegation by design), `isSwap2` missing "Speed Swap2" (left unchanged), keryo-poof AIOOBE guard fix (proven behavior-preserving).

---

## 5. Not covered / suggested follow-up

- **Server-side behavior unverified.** The "200 + null body = expired" re-auth contract and the stateless `name2`/`password2` query-param auth model were inferred from client code and the legacy URL shape, not confirmed against the pente.org JSP. A quick server-side confirmation would fully close the cookie-jar and re-auth refutations.
- **No differential coverage of win detection on the engine path.** Acceptable today because production win detection still runs legacy `detectPente`. **Before migrating any production caller onto `DefaultPenteRules.isWin`**, add a differential test comparing engine `isWin` against legacy across the existing corpus, and extend the corpus to include re-occupation of captured cells.
- **Security hardening (backend-coordinated, not blocking).** Migrating credential transport off URL query strings (cookie/header or POST body) and adding `fullBackupContent`/`dataExtractionRules` to exclude credential prefs are worthwhile platform-level follow-ups, but require server cooperation and are out of scope for this refactor.
- **Regression test for Finding 1.** Add a Go dead-stone toggle test asserting `getState()` reflects the mutation, to lock in the fix and guard against the snapshot-aliasing class of bug recurring as more rendering moves onto `BoardState`.
- **Consider a single app-wide `OkHttpClient`.** Not a bug, but as more endpoints are strangled onto the new transport, sharing one client (and one connection pool/dispatcher) across Activities is the standard pattern and avoids per-Activity client churn.
