# Renju (Taraguchi-10) Live Play — Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real-time (live-socket) Renju / Taraguchi-10 opening support (game ids 31 Renju, 32 Speed Renju) to the Android `liveGameRoom` so a full Taraguchi opening (both branches) can be played against the pente.org server.

**Architecture:** Live Android talks to the server over a raw SSL socket exchanging single-key JSON frames terminated by `0xFF` (no Gson/wrapper on the client — JSON is hand-built outbound and parsed with `org.json` inbound). Unlike turn-based renju (which reads a server-shipped `renjuPhase`), the live client gets **no phase field** — it must **derive** the opening phase client-side from the stone moves plus three new decision-echo events. The heart of this work is a pure, JVM-unit-tested state machine (`RenjuLiveState`) that mirrors the server `RenjuState` / React reducer exactly. Everything else is: 3 new socket events (in/out), Table/GameState integration (black-first colour, 15×15, turn logic), a swap2-style `AlertDialog` for the discrete choice, and board taps/overlays for box-placement, the 10-offer pick, and the selection.

**Tech Stack:** Java 17 (app, AndroidX), pure Java (`rules/` Gradle module, JUnit 4 JVM tests), `org.json` (inbound parse), Android `View`/Canvas (`LiveBoardView`), `AlertDialog` (choice UI). Build: Gradle (`./gradlew`).

## Reference docs (read before starting)

The behavioural spec was reverse-engineered from the reference PRs into four notes — **read them**, they contain the exact rules this plan implements:
- `docs/superpowers/renju-live/01-react-frontend-spec.md` — the client state machine (React reducer) the live phase derivation mirrors. **The §3 reducer rules and §18 opening sequences are normative.**
- `docs/superpowers/renju-live/02-server-protocol-spec.md` — the authoritative wire protocol: the 3 events, `RenjuState`, validation/echo, and the rejoin contract.
- `docs/superpowers/renju-live/03-android-livegameroom-map.md` — exactly where in the Android live code each change goes (file:line).
- `docs/superpowers/renju-live/04-android-tb-renju-reuse.md` — what the existing turn-based renju code already gives us to reuse.

## Global Constraints

- **Game ids:** `31` = Renju (live), `32` = Speed Renju (live), `81` = TB Renju (out of scope here). All three already map to `Variant.RENJU` in `rules/` — do **not** re-add them; only the live wiring is missing.
- **Board:** always 15×15 (225 points). Move index = `col + row*15`. Centre = index **112** = (col 7, row 7).
- **Colours:** Renju is **black-first**. Board stone value `2 = black`, `1 = white`. Colour of the stone for move `i` (0-based) = `2 - (i % 2)`. Seat/player numbering: player **1 = black (seat 1)**, player **2 = white (seat 2)**.
- **No captures** in renju; do not run capture logic on a renju board.
- **Box constraints** (Manhattan box around centre 7,7; `r = stones already placed`): move 2 → 3×3 (`|d|≤1`), move 3 → 5×5 (`|d|≤2`), move 4 → 7×7 (`|d|≤3`), move 5 (Branch A) → 9×9 (`|d|≤4`), move 1 = centre only, move 6+ = anywhere. The live client enforces this only as input-gating (server is authoritative).
- **The server is authoritative.** Every client send is optimistic-then-confirmed: send the event, then mark the local UI "pending" and change board/seat/phase **only when the server echoes** (the echo is reflexive — it comes back to the sender too). A `dsgMoveTableErrorEvent` to the sender means rejected — unlock and let the user retry.
- **Renju board colour:** `#D98880` (dusty rose). Candidate/preview stone board value: `4` (translucent). Selection-mask value: `-1`.
- **Transport:** outbound events are hand-built JSON strings sent via `LiveGameRoomActivity.sendEvent(String)`; every table event carries `"player"`, `"table"`, and `"time":0`. Inbound frames are `{"<eventKey>":{...}}` single-key objects parsed by `jsonToMap`.
- **Symmetry dedup is position-aware** (stabilizer of the placed position), not full-D4 — see Task A1. The server accepts rotated offers when the position is asymmetric; the live pre-check must match so it never blocks a server-legal offer.

## File Structure

**New files:**
- `rules/src/main/java/be/submanifold/pente/rules/RenjuLiveState.java` — pure phase/turn/tracking state machine (the heart). No Android deps.
- `rules/src/test/java/be/submanifold/pente/rules/RenjuLiveStateTest.java` — JVM unit tests (ported from React `renjuTracking.test.js` + `openingPhase.test.js`).
- `rules/src/test/java/be/submanifold/pente/rules/RenjuSymmetryStabilizerTest.java` — tests for the new position-aware dedup.

**Modified files:**
- `rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java` — add position-aware (stabilizer) dedup helpers.
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/GameState.java` — add `RenjuLiveState renjuState`.
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/Table.java` — `isRenju()`, `gameNames` 31/32, black-first `currentColor()`, renju arm in `currentPlayerName()`/`isMyTurn()`, renju advance hook in `addMove/addMoves`, 15×15 grid, choice predicates.
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java` — 3 inbound dispatch branches + renju handling of `swapSeats`/rejoin ordering; reads grid size.
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java` — `showRenjuChoice()` dialog + `sendRenjuSwap/Offer10/Select1`; raise it reactively from `addMove/addMoves`; offer/selection arming.
- `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveBoardView.java` — 15×15, box-gating, candidate overlays, offer multi-tap FSM, selection tap, phase/mode gating.
- `app/src/main/res/values/strings.xml` + `values-de/strings.xml` — renju live strings (reuse existing `renju_*` where present; add the few new ones).

---

## Milestone A — Shared symmetry (rules module, JVM-testable)

### Task A1: Position-aware (stabilizer) symmetry dedup

**Why:** The existing `RenjuSymmetry.isSymmetricDup`/`isValidOfferSet` dedup against the **full D4 group** unconditionally. The server (`RenjuState.isSymmetricDuplicate` via `positionStabilizer()`) and the React reference dedup only against the symmetries that fix the **current placed stones**. For a typical asymmetric opening the stabilizer is just identity → only an *exact* duplicate is a dup; rotations are legal. Matching this prevents the live client from blocking offers the server would accept. Keep the existing methods (used by TB) and **add** position-aware variants.

**Files:**
- Modify: `rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java`
- Test: `rules/src/test/java/be/submanifold/pente/rules/RenjuSymmetryStabilizerTest.java`

**Interfaces:**
- Produces:
  - `static int rotate(int move, int rot)` — image of `move` under D4 op `rot` (0..7) about centre 7. Ops match the server `ROTX/ROTY/ROTF`: `ROTX={1,1,1,1,-1,-1,-1,-1}`, `ROTY={1,1,-1,-1,-1,-1,1,1}`, `ROTF={0,1,0,1,0,1,0,1}`.
  - `static int[] stabilizer(byte[] board)` — the ops (0..7) that map the coloured 225-cell `board` (0=empty) onto itself.
  - `static boolean isOfferDup(int move, int[] offers, int[] stab)` — true if `move` maps onto an offered point under any op in `stab`.
  - `static boolean isSymmetricDup(int move, int[] accepted, byte[] board)` — convenience: `isOfferDup(move, accepted, stabilizer(board))`.

- [ ] **Step 1: Write the failing test**

```java
package be.submanifold.pente.rules;

import static org.junit.Assert.*;
import org.junit.Test;

public class RenjuSymmetryStabilizerTest {
    private static byte[] empty() { return new byte[225]; }
    private static byte[] withBlack(int... idx) { byte[] b = empty(); for (int i : idx) b[i] = 2; return b; }

    // Centre is fixed by 180-degree rotation (op 4): 40 <-> 184, 112 fixed.
    @Test public void rotate_180_about_centre() {
        assertEquals(112, RenjuSymmetry.rotate(112, 4));   // centre fixed
        assertEquals(184, RenjuSymmetry.rotate(40, 4));    // (10,2)->(4,12): 10+2*15=40 -> 4+12*15=184
        assertEquals(40, RenjuSymmetry.rotate(184, 4));
    }

    // Lone centre stone is fully symmetric: stabilizer is all 8 ops -> a rotated offer collides.
    @Test public void symmetric_position_rejects_rotations() {
        byte[] board = withBlack(112);
        int[] stab = RenjuSymmetry.stabilizer(board);
        assertEquals(8, stab.length);
        assertTrue(RenjuSymmetry.isSymmetricDup(RenjuSymmetry.rotate(98, 4), new int[]{98}, board));
    }

    // Asymmetric placed position: stabilizer is identity only -> only EXACT dup rejected; rotations legal.
    @Test public void asymmetric_position_allows_rotations() {
        // centre + a stone off any axis breaks all symmetry
        byte[] board = withBlack(112, 99); // 99 = (9,6)
        int[] stab = RenjuSymmetry.stabilizer(board);
        assertEquals(1, stab.length);                       // identity only
        assertTrue(RenjuSymmetry.isSymmetricDup(57, new int[]{57}, board));        // exact dup
        assertFalse(RenjuSymmetry.isSymmetricDup(RenjuSymmetry.rotate(57, 4), new int[]{57}, board)); // rotation OK
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :rules:test --tests "*RenjuSymmetryStabilizerTest"`
Expected: FAIL — compile error / methods `rotate`, `stabilizer`, `isSymmetricDup(int,int[],byte[])` not found.

- [ ] **Step 3: Add the implementation to `RenjuSymmetry.java`**

Append inside the class (keep existing methods unchanged):

```java
    private static final int[] ROTX = {1, 1, 1, 1, -1, -1, -1, -1};
    private static final int[] ROTY = {1, 1, -1, -1, -1, -1, 1, 1};
    private static final int[] ROTF = {0, 1, 0, 1, 0, 1, 0, 1};

    /** Image of `move` under D4 op `rot` (0..7) about centre (7,7). */
    public static int rotate(int move, int rot) {
        int x = (move % N) - C, y = (move / N) - C;
        int x1 = x * ROTX[rot], y1 = y * ROTY[rot];
        if (ROTF[rot] != 0) { int t = x1; x1 = y1; y1 = t; }
        return (x1 + C) + (y1 + C) * N;
    }

    /** The ops (0..7) that map the coloured board (0=empty) onto itself — the position's stabilizer. */
    public static int[] stabilizer(byte[] board) {
        Set<Integer> stab = new LinkedHashSet<>();
        for (int r = 0; r < 8; r++) {
            boolean invariant = true;
            for (int m = 0; m < N * N && invariant; m++) {
                byte v = board[m];
                if (v > 0 && board[rotate(m, r)] != v) invariant = false;
            }
            if (invariant) stab.add(r);
        }
        int[] arr = new int[stab.size()];
        int i = 0;
        for (int v : stab) arr[i++] = v;
        return arr;
    }

    /** True if `move` maps onto an already-offered point under any op in the precomputed stabilizer. */
    public static boolean isOfferDup(int move, int[] offers, int[] stab) {
        for (int r : stab) {
            int img = rotate(move, r);
            for (int o : offers) if (img == o) return true;
        }
        return false;
    }

    /** Convenience: dedup `move` against `accepted` using the stabilizer of `board`. */
    public static boolean isSymmetricDup(int move, int[] accepted, byte[] board) {
        return isOfferDup(move, accepted, stabilizer(board));
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :rules:test --tests "*RenjuSymmetryStabilizerTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java \
        rules/src/test/java/be/submanifold/pente/rules/RenjuSymmetryStabilizerTest.java
git commit -m "feat(renju): position-aware (stabilizer) symmetry dedup for live offers"
```

---

## Milestone B — Pure live phase/turn state machine (the heart)

### Task B1: `RenjuLiveState` — tracking record + phase/turn derivation

**Why:** The live client has no server-shipped phase. `RenjuLiveState` is the exact Java port of the React tracking record (`freshRenjuTracking`) + `openingPhase.js` + the four reducer mutators in `01-react-frontend-spec.md` §3. Pure logic, no Android — unit-tested in the `rules` module. `Table`/`LiveGameRoomActivity` own one instance per live game and call its mutators on echoes.

**Files:**
- Create: `rules/src/main/java/be/submanifold/pente/rules/RenjuLiveState.java`
- Test: `rules/src/test/java/be/submanifold/pente/rules/RenjuLiveStateTest.java`

**Interfaces:**
- Produces:
  - `enum Phase { SWAP, BRANCH, SELECTION, MOVE, COMPLETE }`
  - mutable fields mirroring React: `boolean complete, awaitingSwap, branchChosen, tenOffer, swapTaken; int[] offered; Integer selected;` (offered defaults `new int[0]`, selected `null`).
  - `void reset()` — back to fresh values.
  - `void advanceAfterMove(int numMoves, boolean isRejoin)` — §3.1.
  - `void applySwap(boolean swap, int numMoves)` — §3.3.
  - `void applyOffer10(int[] moves)` — §3.4.
  - `void applySelect1(int move)` — §3.5.
  - `void applySwapSeats()` — §3.6 (the renju part: `awaitingSwap=false; swapTaken=true`).
  - `Phase phase(int numMoves)` — §2 `renjuPhase`.
  - `int openingPlayer(int numMoves)` — `renjuOpeningPlayer`; returns 1/2, or `0` when complete (caller falls back to parity). (Use 0-as-null since Java int; document it.)
  - `int boxRadius(int numMoves)` — `(numMoves>=1 && numMoves<=4) ? numMoves : 0`.
  - `boolean isSwapChoice(int n)`, `boolean isBranchChoice(int n)`, `boolean isSelection(int n)`.
  - `boolean showSwap(int n)`, `boolean showDeclinePlace(int n)`, `boolean showOffer10(int n)` — `renjuModalButtons`.
  - `void applyRejoinSignal(RejoinKind kind, int numMoves)` + `enum RejoinKind { NONE, SILENT_SWAP, OFFERS, SELECT1 }` — applies the server rejoin contract (`02-server-protocol-spec.md` §3.4) BEFORE the bulk move replay, so `advanceAfterMove(.., isRejoin=true)` respects it. For `OFFERS`/`SELECT1` the actual `moves`/`move` arrive on their own echo just like live; `applyRejoinSignal` only needs to set `swapTaken`/branch flags for `SILENT_SWAP`.

- [ ] **Step 1: Write the failing tests** (port of `renjuTracking.test.js` §18 sequences)

```java
package be.submanifold.pente.rules;

import static org.junit.Assert.*;
import org.junit.Test;

public class RenjuLiveStateTest {

    // ---- phase classifier (openingPhase.test.js) ----
    @Test public void fresh_state_is_move_then_swap_after_first_stone() {
        RenjuLiveState r = new RenjuLiveState();
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(0));
        r.advanceAfterMove(1, false);
        assertEquals(RenjuLiveState.Phase.SWAP, r.phase(1));
        assertEquals(2, r.openingPlayer(1)); // white decides window 1
    }

    @Test public void branch_phase_after_move4_window_resolved_without_branch() {
        RenjuLiveState r = new RenjuLiveState();
        r.awaitingSwap = false; // move-4 window resolved (e.g. take-over)
        r.swapTaken = true;
        assertEquals(RenjuLiveState.Phase.BRANCH, r.phase(4));
        assertEquals(1, r.openingPlayer(4)); // black chooses branch
    }

    @Test public void selection_phase_after_offer10() {
        RenjuLiveState r = new RenjuLiveState();
        r.applyOffer10(new int[]{98,99,100,113,114,115,128,129,130,131});
        assertEquals(RenjuLiveState.Phase.SELECTION, r.phase(4));
        assertEquals(2, r.openingPlayer(4)); // white selecting
    }

    // ---- Branch A full sequence (01 spec §18) ----
    @Test public void branch_A_full_sequence() {
        RenjuLiveState r = new RenjuLiveState();
        // M1 black centre
        r.advanceAfterMove(1, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(1));
        // win1 white decline+place 113
        r.applySwap(false, 1);        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(1));
        r.advanceAfterMove(2, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(2));
        // win2 black decline+place
        r.applySwap(false, 2);        r.advanceAfterMove(3, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(3));
        // win3 white decline+place
        r.applySwap(false, 3);        r.advanceAfterMove(4, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4));
        // win4 black "Place 5th move" => Branch A
        r.applySwap(false, 4);
        assertTrue(r.branchChosen); assertFalse(r.tenOffer);
        // M5 black lands => window 5 opens
        r.advanceAfterMove(5, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(5));
        assertEquals(1, r.openingPlayer(5)); // window 5: opponent of move-5 (black) = ... see note
        // win5 white bare decline
        r.applySwap(false, 5);        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(5));
        // M6 white anywhere => complete
        r.advanceAfterMove(6, false); assertEquals(RenjuLiveState.Phase.COMPLETE, r.phase(6));
        assertTrue(r.complete);
    }

    // ---- Branch B full sequence ----
    @Test public void branch_B_full_sequence() {
        RenjuLiveState r = new RenjuLiveState();
        r.advanceAfterMove(1, false);
        r.applySwap(false, 1); r.advanceAfterMove(2, false);
        r.applySwap(false, 2); r.advanceAfterMove(3, false);
        r.applySwap(false, 3); r.advanceAfterMove(4, false);
        assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4));
        // win4 black "Offer 10" => Branch B, SELECTION
        r.applyOffer10(new int[]{98,99,100,113,114,115,128,129,130,131});
        assertEquals(RenjuLiveState.Phase.SELECTION, r.phase(4));
        // white selects one
        r.applySelect1(114);
        assertEquals(Integer.valueOf(114), r.selected);
        assertEquals(RenjuLiveState.Phase.MOVE, r.phase(4)); // gap: board inert
        // server places move 5 (tenOffer => no window 5) => complete
        r.advanceAfterMove(5, false);
        assertEquals(RenjuLiveState.Phase.COMPLETE, r.phase(5));
        assertTrue(r.complete);
    }

    // ---- take-over at window 4 -> BRANCH (no swap button) ----
    @Test public void takeover_at_window4_yields_branch() {
        RenjuLiveState r = new RenjuLiveState();
        for (int n = 1; n <= 3; n++) { r.advanceAfterMove(n, false); r.applySwap(false, n); }
        r.advanceAfterMove(4, false); assertEquals(RenjuLiveState.Phase.SWAP, r.phase(4));
        r.applySwapSeats(); // take-over
        assertFalse(r.awaitingSwap); assertTrue(r.swapTaken); assertFalse(r.branchChosen);
        assertEquals(RenjuLiveState.Phase.BRANCH, r.phase(4));
        assertFalse(r.showSwap(4));         // no swap button post take-over
        assertTrue(r.showDeclinePlace(4));
        assertTrue(r.showOffer10(4));
    }

    // ---- rejoin: bulk replay must not reopen a resolved window ----
    @Test public void rejoin_does_not_reopen_resolved_window() {
        RenjuLiveState r = new RenjuLiveState();
        r.applyRejoinSignal(RenjuLiveState.RejoinKind.SILENT_SWAP, 4); // a window resolved before join
        r.advanceAfterMove(4, true); // bulk replay of 4 moves, isRejoin
        assertNotEquals(RenjuLiveState.Phase.SWAP, r.phase(4)); // no spurious swap modal
    }

    // ---- modal buttons by window (renjuModalButtons) ----
    @Test public void modal_buttons_open_move4_window_shows_all_three() {
        RenjuLiveState r = new RenjuLiveState();
        r.awaitingSwap = true;
        assertTrue(r.showSwap(4)); assertTrue(r.showDeclinePlace(4)); assertTrue(r.showOffer10(4));
    }
    @Test public void modal_buttons_window1_no_offer10() {
        RenjuLiveState r = new RenjuLiveState();
        r.awaitingSwap = true;
        assertTrue(r.showSwap(1)); assertTrue(r.showDeclinePlace(1)); assertFalse(r.showOffer10(1));
    }
}
```

> NOTE on `openingPlayer(5)` in the Branch-A test: per `renjuOpeningPlayer`, window 5 has `awaitingSwap=true`, n=5 → `lastColor = ((5-1)%2)+1 = 1`, return `3-1 = 2` (white decides window 5). **Fix the assertion to `assertEquals(2, r.openingPlayer(5));`** when writing — the comment above is a reminder to use the formula, not the wrong literal. (Black placed move 5; white is the opponent who decides window 5.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :rules:test --tests "*RenjuLiveStateTest"`
Expected: FAIL — `RenjuLiveState` does not exist.

- [ ] **Step 3: Write `RenjuLiveState.java`** (exact port of `01-react-frontend-spec.md` §2–§3)

```java
package be.submanifold.pente.rules;

import java.util.Arrays;

/**
 * Client-side Renju (Taraguchi-10) opening state machine for LIVE play.
 * Pure mirror of the server RenjuState.getOpeningPhase/getCurrentPlayer and the React reducer
 * (docs/superpowers/renju-live/01-react-frontend-spec.md). No Android deps — JVM unit-tested.
 * The live client derives the phase from stone moves + 3 decision-echo events (no server phase field).
 */
public final class RenjuLiveState {

    public enum Phase { SWAP, BRANCH, SELECTION, MOVE, COMPLETE }
    public enum RejoinKind { NONE, SILENT_SWAP, OFFERS, SELECT1 }

    public boolean complete;
    public boolean awaitingSwap;
    public boolean branchChosen;
    public boolean tenOffer;
    public boolean swapTaken;
    public int[] offered = new int[0];
    public Integer selected = null;

    public void reset() {
        complete = false; awaitingSwap = false; branchChosen = false; tenOffer = false;
        swapTaken = false; offered = new int[0]; selected = null;
    }

    /** §3.1 advanceRenjuTrackingAfterMove — runs after every stone lands. */
    public void advanceAfterMove(int numMoves, boolean isRejoin) {
        if (!isRejoin) swapTaken = false; // a fresh incremental move opens a new window
        boolean windowResolved = swapTaken
                || (numMoves == 4 && (branchChosen || tenOffer || selected != null));
        boolean windowOpens = !windowResolved && (numMoves <= 4 || (numMoves == 5 && !tenOffer));
        awaitingSwap = windowOpens;
        complete = !windowOpens && numMoves >= 5;
    }

    /** §3.3 renjuSwap echo. */
    public void applySwap(boolean swap, int numMoves) {
        awaitingSwap = false;                 // ALWAYS clears the window
        if (!swap && numMoves == 4) {         // move-4 decline = Branch A
            branchChosen = true;
            tenOffer = false;
        }
    }

    /** §3.4 renjuOffer10 echo — Branch B. */
    public void applyOffer10(int[] moves) {
        branchChosen = true;
        tenOffer = true;
        offered = Arrays.copyOf(moves, moves.length);
        awaitingSwap = false;
    }

    /** §3.5 renjuSelect1 echo. */
    public void applySelect1(int move) {
        selected = move;
    }

    /** §3.6 swapSeats (renju part): a take-over or silent rejoin marker resolved the window. */
    public void applySwapSeats() {
        awaitingSwap = false;
        swapTaken = true;
    }

    /** §3.4 rejoin: apply the single phase signal BEFORE the bulk move replay. */
    public void applyRejoinSignal(RejoinKind kind, int numMoves) {
        switch (kind) {
            case SILENT_SWAP: applySwapSeats(); break;     // BRANCH (n==4) or MOVE (n!=4)
            case OFFERS:                                    // offered[] arrives on the re-sent offer10 echo
            case SELECT1:                                   // selected arrives on the re-sent select1 echo
            case NONE:
            default: break;
        }
    }

    /** §2 renjuPhase. */
    public Phase phase(int numMoves) {
        if (complete) return Phase.COMPLETE;
        if (awaitingSwap) return Phase.SWAP;
        if (numMoves == 4 && !branchChosen) return Phase.BRANCH;
        if (numMoves == 4 && branchChosen && tenOffer && offered.length == 10 && selected == null) {
            return Phase.SELECTION;
        }
        return Phase.MOVE;
    }

    /** renjuOpeningPlayer — returns 1 (black) or 2 (white) during the opening, else 0 (caller: parity). */
    public int openingPlayer(int numMoves) {
        if (complete) return 0;
        int n = numMoves;
        if (awaitingSwap) {
            int lastColor = ((n - 1) % 2) + 1;
            return 3 - lastColor;
        }
        if (branchChosen && tenOffer && n == 4) {
            if (offered.length < 10) return 1; // black offering
            if (selected == null) return 2;    // white selecting
        }
        if (n == 4 && !branchChosen) return 1; // black chooses branch / plays move 5
        return (n % 2) + 1;
    }

    public int boxRadius(int numMoves) {
        return (numMoves >= 1 && numMoves <= 4) ? numMoves : 0;
    }

    public boolean isSwapChoice(int n)   { return phase(n) == Phase.SWAP; }
    public boolean isBranchChoice(int n) { return phase(n) == Phase.BRANCH; }
    public boolean isSelection(int n)    { return phase(n) == Phase.SELECTION; }

    // renjuModalButtons
    public boolean showSwap(int n)         { return isSwapChoice(n); }
    public boolean showDeclinePlace(int n) { return isSwapChoice(n) || isBranchChoice(n); }
    public boolean showOffer10(int n)      { return isBranchChoice(n) || (isSwapChoice(n) && n == 4); }
}
```

- [ ] **Step 4: Run tests to verify they pass** (after correcting the `openingPlayer(5)` assertion to `2` per the NOTE)

Run: `./gradlew :rules:test --tests "*RenjuLiveStateTest"`
Expected: PASS (all).

- [ ] **Step 5: Commit**

```bash
git add rules/src/main/java/be/submanifold/pente/rules/RenjuLiveState.java \
        rules/src/test/java/be/submanifold/pente/rules/RenjuLiveStateTest.java
git commit -m "feat(renju): pure live opening state machine (RenjuLiveState) + tests"
```

---

## Milestone C — Table / GameState integration

### Task C1: Add `RenjuLiveState` to `GameState` + renju predicates/colour/turn to `Table`

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/GameState.java`
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/Table.java`

**Interfaces:**
- Consumes: `RenjuLiveState` (Task B1), `RenjuSymmetry` (Task A1), `Variants.fromGameId` (existing).
- Produces (on `Table`):
  - `boolean isRenju()` → `Variant v = Variants.fromGameId(game); return v != null && v.isRenju();`
  - `int renjuNumMoves()` → `getMoves().size()`
  - `void advanceRenjuAfterMove(boolean isRejoin)` → `if (isRenju()) gameState.renjuState.advanceAfterMove(getMoves().size(), isRejoin);`
  - `boolean renjuChoiceNow()` → `isRenju() && (gameState.renjuState.isSwapChoice(getMoves().size()) || gameState.renjuState.isBranchChoice(getMoves().size()))`
  - black-first `currentColor()` and renju turn arm in `currentPlayerName()`/`isMyTurn()`.

**Read first:** `Table.java` `currentColor()` (~279), `currentPlayerName()` (~344), `isMyTurn()` (~296), the `isGo/isDPente/isSwap2` delegators (~187/269/274), `gameNames` map (~54-85). Match their exact style.

- [ ] **Step 1: GameState — add the field**

In `GameState.java`, add an import and field:
```java
import be.submanifold.pente.rules.RenjuLiveState;
// ...
public RenjuLiveState renjuState = new RenjuLiveState();
```

- [ ] **Step 2: Table — `isRenju()` + grid size + `gameNames`**

Add `isRenju()` next to `isSwap2()` (mirror the delegator form). Add to the static `gameNames` map (after id 30):
```java
gameNames.put(31, "Renju");
gameNames.put(32, "Speed Renju");
```
Ensure the live grid size resolves to 15 for renju (the code path that sizes `LiveBoardView` / `gridSize`): wherever the table computes its board dimension (Go-special branch today), add `else if (isRenju()) gridSize = 15;` (or `Variants.gridSize(Variants.fromGameId(game))`). Verify against the actual current sizing code in `Table.java`/`LiveTableFragment`/`LiveBoardView` before editing.

- [ ] **Step 3: Table — black-first colour + renju turn arm**

In `currentColor()`, add a renju branch (black-first): `if (isRenju()) return (byte)(2 - (getMoves().size() % 2));` (move 0 → 2 black). In `currentPlayerName()`/`isMyTurn()`, add a renju arm that consults the state machine:
```java
if (isRenju()) {
    int op = gameState.renjuState.openingPlayer(getMoves().size());
    if (op != 0) {
        // op==1 -> black seat, op==2 -> white seat; map to the player name in that seat
        return playerNameForSeat(op); // use the existing seat->name lookup used by swap2/dpente
    }
    // op==0 (opening complete): fall through to normal parity
}
```
Match the existing seat→name mechanism (the same `playingPlayers`/seat map swap2 uses). Verify the exact accessor name in `Table.java`.

- [ ] **Step 4: Table — advance hook + reset**

In `addMove(int)` and `addMoves(List)` (the methods that append to `moves`), after the stone(s) are recorded, call `advanceRenjuAfterMove(false)` for incremental `addMove` and `advanceRenjuAfterMove(true)` for the bulk `addMoves` (rejoin). On new-game / table reset (wherever `gameState` is re-initialised), `gameState.renjuState.reset()` is implicit via `new GameState()`. Confirm `addMoves` is the bulk/rejoin path (per `03` map it is `LiveTableFragment.addMoves` → `table.addMoves`).

- [ ] **Step 5: Build + run rules/app unit tests; commit**

Run: `./gradlew :rules:test :app:compileDebugJavaWithJavac`
Expected: compiles; rules tests green.
```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/GameState.java \
        app/src/main/java/be/submanifold/pentelive/liveGameRoom/Table.java
git commit -m "feat(renju): live Table/GameState integration (state, colour, turn, grid)"
```

---

## Milestone D — Transport (inbound dispatch + outbound builders)

### Task D1: Inbound — 3 renju event branches + swapSeats/rejoin handling

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java`

**Read first:** `eventOccurred(String)` dispatch chain (~231-418), `updateTableMove` (~491), `swapSeats` (~571), `jsonToMap`/`toList` (~687-737), `sendEvent` (~443).

**Interfaces:**
- Consumes: `table.gameState.renjuState`, `Table.advanceRenjuAfterMove`, the fragment's UI hooks (Task E1: `fragment.onRenjuSwap/onRenjuOffer10/onRenjuSelect1` or direct dialog refresh).
- Produces: inbound handling that mutates `renjuState` then refreshes board + (re)raises/hides the choice dialog.

- [ ] **Step 1: Add three dispatch branches** in `eventOccurred` (mirror the `dsgSwapSeatsTableEvent` branch). **The wire keys are the FULL wrapper field names** — `dsgRenjuTaraguchiSwapTableEvent`, `dsgRenjuTaraguchiOffer10TableEvent`, `dsgRenjuTaraguchi10Select1TableEvent` — exactly like the existing `dsgMoveTableEvent`/`dsgSwapSeatsTableEvent` keys (NOT the short React `cmd` labels `renjuSwap`/etc., which are JS-side only — confirmed by doc 02 §1.4 and doc 01 §16). Each reads `table` (int), then:
```java
else if (jsonEvent.get("dsgRenjuTaraguchiSwapTableEvent") != null) {
    Map<String,Object> p = (Map<String,Object>) jsonEvent.get("dsgRenjuTaraguchiSwapTableEvent");
    int tbl = ((Number) p.get("table")).intValue();
    boolean swap = Boolean.TRUE.equals(p.get("swap"));
    Table t = tableFor(tbl);
    if (t != null) {
        int n = t.getMoves().size();
        t.gameState.renjuState.applySwap(swap, n);
        runOnUiThread(() -> fragment.onRenjuDecisionEcho(tbl)); // refresh board+dialog
    }
}
else if (jsonEvent.get("dsgRenjuTaraguchiOffer10TableEvent") != null) {
    Map<String,Object> p = (Map<String,Object>) jsonEvent.get("dsgRenjuTaraguchiOffer10TableEvent");
    int tbl = ((Number) p.get("table")).intValue();
    int[] moves = toIntArray((List<Object>) p.get("moves"));
    Table t = tableFor(tbl);
    if (t != null) {
        t.gameState.renjuState.applyOffer10(moves);
        runOnUiThread(() -> fragment.onRenjuDecisionEcho(tbl));
    }
}
else if (jsonEvent.get("dsgRenjuTaraguchi10Select1TableEvent") != null) {
    Map<String,Object> p = (Map<String,Object>) jsonEvent.get("dsgRenjuTaraguchi10Select1TableEvent");
    int tbl = ((Number) p.get("table")).intValue();
    int move = ((Number) p.get("move")).intValue();
    Table t = tableFor(tbl);
    if (t != null) {
        t.gameState.renjuState.applySelect1(move);
        runOnUiThread(() -> fragment.onRenjuDecisionEcho(tbl));
    }
}
```
Use the existing table-lookup accessor (`tableFor`/`tables.get(...)` — match the name used by `swapSeats`). Add a small `int[] toIntArray(List<Object>)` helper near `toList` if none exists.

- [ ] **Step 2: Renju arm in `swapSeats`** — when the swapped table is renju, also resolve the renju window: after the existing `table.swapSeats(swap, silent)` call, add `if (t.isRenju()) t.gameState.renjuState.applySwapSeats();` then refresh board+dialog. (Covers both live take-over and the silent rejoin marker — both set `awaitingSwap=false, swapTaken=true`.)

- [ ] **Step 3: Rejoin ordering** — `updateTableMove`’s bulk path (`fragment.addMoves(moves)`) already routes to `Table.advanceRenjuAfterMove(true)` (Task C1 step 4). Because the server sends the rejoin signal (silent swapSeats / re-sent offer10 / select1) BEFORE the bulk move list, the `renjuState` is already in the right pre-state when the bulk replay runs with `isRejoin=true`. No extra code beyond Steps 1–2 + C1; add a comment documenting the ordering dependency.

- [ ] **Step 4: Build; commit**

Run: `./gradlew :app:compileDebugJavaWithJavac`
```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveGameRoomActivity.java
git commit -m "feat(renju): inbound live event dispatch (swap/offer10/select1) + swapSeats arm"
```

### Task D2: Outbound — `sendRenjuSwap/Offer10/Select1` builders

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java`

**Read first:** `sendSwap2Choice` (~1092), `sendSwap2Pass` (~1097), `sendDPenteChoice` (~1044), and how `me`/table id are obtained — match exactly (hand-built JSON, `getListener().sendEvent(...)`).

**Interfaces:**
- Produces (on `LiveTableFragment`):
  Wire keys are the FULL wrapper field names (match the inbound keys / existing `dsgMoveTableEvent`):
  - `void sendRenjuSwap(boolean swap, int move)` →
    `sendEvent("{\"dsgRenjuTaraguchiSwapTableEvent\":{\"swap\":"+swap+",\"move\":"+move+",\"player\":\""+me+"\",\"table\":"+id+",\"time\":0}}")`
  - `void sendRenjuOffer10(int[] moves)` →
    `sendEvent("{\"dsgRenjuTaraguchiOffer10TableEvent\":{\"moves\":["+csv(moves)+"],\"player\":\""+me+"\",\"table\":"+id+",\"time\":0}}")`
  - `void sendRenjuSelect1(int move)` →
    `sendEvent("{\"dsgRenjuTaraguchi10Select1TableEvent\":{\"move\":"+move+",\"player\":\""+me+"\",\"table\":"+id+",\"time\":0}}")`
  - private `String csv(int[] a)` — comma-joined.
- Note: ordinary stones (decline+place move, Branch-A move 5, Branch-B white move 6) are sent on the **existing** `dsgMoveTableEvent` path (the board's normal send) — NOT here. Take-over uses `sendRenjuSwap(true, -1)`. Window-5 bare decline uses `sendRenjuSwap(false, -1)`. Decline+place at windows 1–4: send `sendRenjuSwap(false, <m>)` (the server bundles the stone and re-broadcasts it as a normal move).

- [ ] **Step 1: Add the three builders + `csv` helper** exactly as above (match `sendSwap2Choice` style/access to `me` and table id).

- [ ] **Step 2: Build; commit**

Run: `./gradlew :app:compileDebugJavaWithJavac`
```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java
git commit -m "feat(renju): outbound live event builders (swap/offer10/select1)"
```

---

## Milestone E — Choice dialog (swap2 methodology)

### Task E1: `showRenjuChoice()` + reactive raise + echo refresh

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java`
- Strings: `app/src/main/res/values/strings.xml`, `values-de/strings.xml`

**Read first:** `showSwap2Choice` (~1049) and `showDPenteChoice` (~1017) — replicate the builder (bottom gravity, `setCanceledOnTouchOutside(false)`, `clearFlags(FLAG_DIM_BEHIND)`, `setItems`), and where `addMove`/`addMoves` (~452/491) raise them.

**Interfaces:**
- Consumes: `table.gameState.renjuState`, `sendRenjuSwap/Offer10/Select1` (D2), board arming hooks (Task F1: `board.beginRenjuPlace()`, `board.beginRenjuOffer()`).
- Produces: `void showRenjuChoice()`, `void onRenjuDecisionEcho(int table)` (dismiss any open renju dialog, refresh board, re-raise if a new decision now falls to me), and the raise-conditions in `addMove/addMoves`.

- [ ] **Step 1: Add the dialog.** Build items dynamically from `renjuState.showSwap/showDeclinePlace/showOffer10(n)`:
```java
void showRenjuChoice() {
    RenjuLiveState rs = table.gameState.renjuState;
    int n = table.getMoves().size();
    List<String> labels = new ArrayList<>();
    List<Runnable> actions = new ArrayList<>();
    if (rs.showSwap(n)) {
        labels.add(getString(R.string.renju_swap_take_over)); // "Swap"
        actions.add(() -> { markRenjuPending(); sendRenjuSwap(true, -1); });
    }
    if (rs.showDeclinePlace(n)) {
        boolean bare = (n == 5); // window-5 bare decline (no stone)
        labels.add(getString(n == 4 ? R.string.renju_place_fifth
                          : bare ? R.string.renju_decline_swap
                                 : R.string.renju_dont_swap));
        actions.add(() -> {
            if (bare) { markRenjuPending(); sendRenjuSwap(false, -1); }
            else board.beginRenjuPlace(); // arm board; tap sends sendRenjuSwap(false, m)
        });
    }
    if (rs.showOffer10(n)) {
        labels.add(getString(R.string.renju_place_ten)); // "Place 10"
        actions.add(() -> board.beginRenjuOffer());      // arm board 10-pick
    }
    AlertDialog d = new AlertDialog.Builder(getContext())
        .setTitle(R.string.renju_choice_title)
        .setItems(labels.toArray(new String[0]), (dlg, which) -> actions.get(which).run())
        .create();
    // bottom gravity + not-cancelable + clear dim — copy verbatim from showSwap2Choice
    configureBottomDialog(d);
    renjuDialog = d;
    d.show();
}
```
(Use the existing helper or inline the window-attributes block from `showSwap2Choice`.) `markRenjuPending()` sets a fragment/board flag suppressing the dialog + board until the next echo (mirrors React `markPending`).

- [ ] **Step 2: Raise reactively.** In `addMove`/`addMoves`, after `table.addMove(...)` + `advanceRenjuAfterMove(...)`, add (gated to *my* decision):
```java
if (table.isRenju() && table.renjuChoiceNow() && table.isMyTurn(me) && !renjuPending) {
    showRenjuChoice();
}
```
SELECTION (white picking one of the ten) is handled on the board, not the dialog — see Task F1.

- [ ] **Step 3: `onRenjuDecisionEcho(int table)`** (called from D1): dismiss `renjuDialog` if showing, clear `renjuPending`, `board.clearRenjuArming()`, `board.invalidate()`, then if `renjuChoiceNow() && isMyTurn` re-raise `showRenjuChoice()` (e.g. a take-over handing the branch choice to the new black). For SELECTION, call `board.beginRenjuSelection(renjuState.offered)`.

- [ ] **Step 4: Strings.** Reuse existing `renju_swap_take_over`="Swap", `renju_dont_swap`="No swap", `renju_place_ten`="Place 10". Add new:
```xml
<string name="renju_choice_title">Taraguchi opening — your choice</string>
<string name="renju_place_fifth">Place 5th move</string>
<string name="renju_decline_swap">Decline swap</string>
<string name="renju_offer_progress">Offer fifth moves: %1$d/10</string>
<string name="renju_select_prompt">Pick one of the offered points</string>
```
Mirror all in `values-de/strings.xml` (German): e.g. `renju_choice_title`="Taraguchi-Eröffnung — deine Wahl", `renju_place_fifth`="5. Stein setzen", `renju_decline_swap`="Tausch ablehnen", `renju_offer_progress`="Fünfte Züge anbieten: %1$d/10", `renju_select_prompt`="Wähle einen der angebotenen Punkte".

- [ ] **Step 5: Build; commit**

Run: `./gradlew :app:compileDebugJavaWithJavac`
```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveTableFragment.java \
        app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(renju): live opening-choice dialog (swap2 methodology) + strings"
```

---

## Milestone F — Board interaction & rendering

### Task F1: `LiveBoardView` — 15×15, box-gating, candidates, offer/selection FSM

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveBoardView.java`

**Read first:** `onTouchEvent` (~101-160), `onDraw` (~80) + `drawBoard` (~170+), `setGridSize` (~44), the Go overlay path `setGoTerritoryByPlayer` (~54) + `clearGoStructures` (~162). Also read the turn-based `BoardView.java` renju touch/render code (per `04` reuse doc §4) to port the algorithm.

**Interfaces:**
- Consumes: `table.gameState.renjuState`, `RenjuSymmetry.isSymmetricDup(move, accepted, board)` (Task A1), `fragment.sendRenjuSwap/Offer10/Select1` (D2), the box-radius rule.
- Produces (on `LiveBoardView`):
  - `void beginRenjuPlace()` — arm single box-constrained decline/Branch-A placement.
  - `void beginRenjuOffer()` — arm 10-pick offer collection.
  - `void beginRenjuSelection(int[] offers)` — render the ten offers, accept one tap.
  - `void clearRenjuArming()` — back to idle.
  - renju fields: `int renjuMode` (IDLE/PLACE/OFFER/SELECTION), `List<Integer> renjuPicks`, `int[] renjuOffers`.

- [ ] **Step 1: Grid size.** Ensure renju tables use grid 15: in the setup path (`setTable`/`onActivityCreated`), `if (table.isRenju()) setGridSize(15);`. The `redDot`/cell math already keys off `gridSize`.

- [ ] **Step 2: Touch FSM.** Extend `onTouchEvent` so that, when `table.isRenju()`, behaviour is by `renjuMode` and `renjuState.phase(n)` (port from `BoardView` §4; mirror Board.js click tree in `01` §11):
  - `PLACE` mode: on UP, if cell empty and within `boxRadius(n)` of centre 7,7 → `fragment.sendRenjuSwap(false, move)` + markPending + clear arming. Reject out-of-box (`|i-7|>r || |j-7|>r`).
  - `OFFER` mode: on UP, if cell already a pick → remove it; else if empty and `!RenjuSymmetry.isSymmetricDup(move, picksArray, boardSnapshot)` → if `picks.size()>=9` send `sendRenjuOffer10([...picks, move])` + markPending + clear (10th auto-sends), else add to picks + invalidate.
  - SELECTION (white): if `renjuOffers.contains(move)` → `fragment.sendRenjuSelect1(move)` + markPending + clear.
  - Opening windows where the dialog owns the decision (phase SWAP/BRANCH and not in PLACE/OFFER mode) → consume touch (board inert).
  - Normal renju move (phase MOVE/COMPLETE, no arming): keep the existing `dsgMoveTableEvent` send, but **add box-gating** when `boxRadius(n)>0` (opening moves 2–5). Reject otherwise.
  - Guard the whole renju branch by the same turn gate already present (`currentPlayerName().equals(me) && state==STARTED`).

- [ ] **Step 3: Rendering.** In `drawBoard`, when `table.isRenju()`:
  - Draw the 9 star points at indices `{48,52,56,108,112,116,168,172,176}` (cols/rows {3,7,11}).
  - Draw translucent black candidates (board value `4`, 0.6 alpha — reuse the Go translucent-cell draw) for `renjuPicks` (OFFER mode) and `renjuOffers` (SELECTION mode).
  - Renju background `#D98880` (match TB `renjuColor`).
  - No capture subtitle.

- [ ] **Step 4: Offer progress + cancel.** While `renjuMode==OFFER`, show the `renju_offer_progress` count (toast/inline TextView, or reuse the fragment status line) and allow cancel (a Cancel affordance → `clearRenjuArming()` + re-raise dialog). While `renjuMode==PLACE`, allow cancel back to the dialog. (Mirror React `RenjuOfferPanel`.)

- [ ] **Step 5: Build; commit**

Run: `./gradlew :app:compileDebugJavaWithJavac`
```bash
git add app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveBoardView.java
git commit -m "feat(renju): live board interaction (box, offer, selection) + rendering"
```

---

## Milestone G — End-to-end verification

### Task G1: Full build + unit tests + emulator play-through

**Files:** none (verification).

- [ ] **Step 1: Unit tests green**

Run: `./gradlew :rules:test`
Expected: all renju tests pass (`RenjuLiveStateTest`, `RenjuSymmetryStabilizerTest`, existing `RenjuSymmetryTest`, `VariantsTest`).

- [ ] **Step 2: Full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL; APK produced.

- [ ] **Step 3: Emulator + localhost backend**

- Boot an emulator; confirm `adb devices` lists it.
- Confirm the live socket host: `LiveGameRoomActivity.connectSocket` uses `10.0.2.2` for the emulator → the localhost backend. Verify the port + TLS the activity expects matches the running backend.
- `./gradlew installDebug`.

- [ ] **Step 4: Play Branch A end-to-end**

Log in as `iostest` (one device) and `graviton` (second emulator/instance) — pw per the task. Start a Renju (id 31) live game. Drive: M1 centre → win1 decline+place → … → win4 "Place 5th move" → M5 in 9×9 → win5 "Decline swap" → M6 → confirm both clients show 6 stones, correct colours (black=2 first), correct turn, and normal play resumes. Confirm take-over at a window swaps seats and continues.

- [ ] **Step 5: Play Branch B end-to-end**

Repeat to win4; choose "Place 10" → tap 10 valid (symmetry-distinct) points (10th auto-sends) → other client (white) taps one of the ten → confirm it becomes black's move 5, the other nine clear, white plays move 6, opening completes (5 stones then 6th). Confirm a symmetric-duplicate pick is blocked client-side and a server-rejected offer surfaces cleanly (move error → unlock).

- [ ] **Step 6: Rejoin test**

Mid-opening (e.g. at a SELECTION or open window), kill and relaunch one client; confirm it reconstructs the correct phase/seats/board from the rejoin signal + bulk moves (no spurious swap dialog, correct pending decision).

- [ ] **Step 7: Speed Renju (id 32)** — quick smoke: start a Speed Renju game, confirm the same opening flow works (only the timer differs).

- [ ] **Step 8: Final commit / version bump (if desired)**

```bash
git add -A && git commit -m "feat(renju): live Taraguchi-10 play — verified vs localhost (both branches + rejoin)"
```

---

## Self-Review notes (coverage map)

- **3 events** (renjuSwap/offer10/select1) — inbound D1, outbound D2, wire format per `02` §1.4 (single-key JSON + 0xFF; client hand-builds/parses).
- **Phase derivation** (no server phase) — B1 `RenjuLiveState` ports `01` §2–§3 exactly; C1 wires it into Table turn/colour; D1 drives it from echoes; rejoin ordering D1 step 3.
- **Branch A / Branch B / take-over / window-5 / rejoin** — all covered by B1 tests + G1 play-through.
- **Symmetry** — A1 position-aware dedup matches server acceptance; used in F1 offer gating.
- **Box constraints / black-first / 15×15 / #D98880 / star points** — Global Constraints; enforced C1 (colour/grid) + F1 (box/render).
- **UI = swap2 methodology** — E1 `showRenjuChoice` mirrors `showSwap2Choice`; board owns offer/selection per the user's chosen approach.
- **Out of scope:** turn-based (id 81) — untouched; the TB renju code is reused only via `rules/` (`RenjuSymmetry`, `Variant`).

**Known risk / verify-in-code during execution:** exact accessor names on `Table`/`LiveTableFragment`/`LiveGameRoomActivity` (`tableFor`, seat→name lookup, `me`, table id, bottom-dialog config, the grid-sizing path). Each task says "read first" — confirm the real signatures before editing; the snippets above are faithful to the documented patterns but must be matched to the live code.
