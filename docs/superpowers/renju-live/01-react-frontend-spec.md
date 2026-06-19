# Renju (Taraguchi-10) — React Live Game Room Frontend Behavioral Spec

Extracted from `/tmp/react_pr5.diff` (PR #5, `react_live_game_room`). This documents the exact
behavior of every non-test source file the diff touches/adds, plus what the test files assert.

## 0. Known constants (given)

- Renju game ids: **31, 32, 81** (31 = live, 81 = turn-based, 32 = speed variant of 31). All three.
- Board: **15×15**, move index = `x + y*15`, centre = **112** (col 7, row 7).
- **Black plays first.** Board stone values: **2 = black, 1 = white** (note: inverted vs gomoku
  which is white-first). Seat/player numbering used by `renjuOpeningPlayer`: **player 1 = black
  (seat 1), player 2 = white (seat 2)**.
- Phase machine `renjuPhase(numMoves, tracking)`:
  `complete → COMPLETE`; `awaitingSwap → SWAP`; `n===4 && !branchChosen → BRANCH`;
  `n===4 && branchChosen && tenOffer && offered.length===10 && selected==null → SELECTION`;
  else `MOVE`.

---

## 1. The tracking record (`src/game/gameState.js`)

`freshRenjuTracking()` returns a NEW object every call:

```js
{ complete:false, awaitingSwap:false, branchChosen:false, tenOffer:false,
  offered:[], selected:null, swapTaken:false }
```

Field meaning:
- `complete` — six/five-stone opening finished; normal play.
- `awaitingSwap` — a swap window is open (SWAP phase).
- `branchChosen` — black has chosen Branch A or B (set at the move-4 window decision).
- `tenOffer` — Branch B taken (10 fifth-move offers were made).
- `offered[]` — the 10 candidate fifth-move indices (Branch B).
- `selected` — the index white picked from the 10 offers (Branch B), else `null`.
- `swapTaken` — a take-over (swap-seats) resolved the current window; used so the bulk
  rejoin replay does NOT reopen an already-resolved window.

This record lives at `game.gameState.renjuState`.

---

## 2. `src/game/openingPhase.js` (pure phase/player logic — the thresholds)

### `RenjuPhase` enum
`SWAP` (window open), `BRANCH` (move-4 resolved, black picks A vs B), `SELECTION` (Branch B,
10 offers in, white picks one), `MOVE` (place a stone), `COMPLETE`.

### `renjuPhase(numMoves, rs)`
```
if complete            -> COMPLETE
if awaitingSwap        -> SWAP
if n===4 && !branchChosen -> BRANCH
if n===4 && branchChosen && tenOffer && offered.length===10 && selected==null -> SELECTION
else                   -> MOVE
```

### `renjuOpeningPlayer(numMoves, rs)` — mirrors server `getCurrentPlayer`, returns 1/2 or null
```
if complete -> null  (caller falls back to plain parity)
if awaitingSwap: lastColor = ((n-1)%2)+1 ; return 3 - lastColor
   // n=1 -> 2 (white decides), n=2 -> 1 (black), n=3 -> 2, n=4 -> 1 (black decides move-4 window)
if branchChosen && tenOffer && n===4:
   if offered.length < 10 -> 1   (black still offering)
   if selected == null    -> 2   (white selecting)
if n===4 && !branchChosen -> 1   (black chooses branch / plays move 5)
return (n%2)+1
   // n=5 -> 2 (white): branch-A window-5 AND move-6 are both white; branch-B selected gap falls here
```

### Predicates (all gate on `started`)
- `isRenjuSwapChoice` ⇔ phase SWAP
- `isRenjuBranchChoice` ⇔ phase BRANCH
- `isRenjuSelection` ⇔ phase SELECTION

### `renjuModalButtons(n, rs, started)` — which decision buttons to show
```
swapChoice   = isRenjuSwapChoice
branchChoice = isRenjuBranchChoice
{ swap:         swapChoice,
  declinePlace: swapChoice || branchChoice,
  offer10:      branchChoice || (swapChoice && n===4) }
```
Concrete:
- windows 1–3 open: `{swap:T, declinePlace:T, offer10:F}`
- open move-4 window: `{swap:T, declinePlace:T, offer10:T}` (all three)
- standalone BRANCH (post take-over, awaitingSwap=false, !branchChosen): `{swap:F, declinePlace:T, offer10:T}`
- window 5 open: `{swap:T, declinePlace:T, offer10:F}`

---

## 3. `src/redux_reducers/utils.js` — THE STATE MACHINE (reducer mutations)

All four handlers clone via `state.game.newInstance()` (which deep-copies `renjuState` incl.
`offered[]`) and reassign `state.game`. They no-op unless `data.table === state.table`.

### 3.1 `advanceRenjuTrackingAfterMove(game, isRejoin)` — runs after every stone lands
No-op unless `game.isRenjuGame()`. `r = game.gameState.renjuState`, `n = game.moves.length`.
```js
if (!isRejoin) r.swapTaken = false;   // a fresh incremental move opens a new window, consumes marker
windowResolved = r.swapTaken || (n === 4 && (r.branchChosen || r.tenOffer || r.selected != null));
windowOpens    = !windowResolved && (n <= 4 || (n === 5 && !r.tenOffer));
r.awaitingSwap = windowOpens;
r.complete     = !windowOpens && n >= 5;
```
Key consequences:
- Incremental moves at n=1..4 (no decision yet) → `awaitingSwap=true` (open window n).
- After a Branch-A decline at n=4 then the bundled move (n=5, tenOffer=false) → window 5 opens
  (`awaitingSwap=true`).
- After a Branch-B selected move 5 (n=5, tenOffer=true) → `windowOpens=false`, `complete=true`.
- After a window-5 decline + move 6 (n=6) → `complete=true`.
- `swapTaken` true (take-over already resolved this window) → window does NOT reopen.

### 3.2 `addMove(data, state)` — move echoes
Two paths, distinguished by shape:
- **Incremental append** (`data.moves.length===1 && data.move===data.moves[0]`):
  `game.addMove(data.move)` then `advanceRenjuTrackingAfterMove(game, /*isRejoin=*/false)`.
  The decision echo for this stone already arrived earlier, so this opens a FRESH window
  (and clears `swapTaken`).
- **Bulk rejoin/state-sync** (full move list): `game.resetBoard()`, replay all moves, then
  `advanceRenjuTrackingAfterMove(game, /*isRejoin=*/true)`. On rejoin the decision echoes arrive
  FIRST (ServerTable.sendMoves), so this path must NOT reopen a window those echoes already
  resolved → `isRejoin=true` keeps `swapTaken` and the n===4 resolution flags intact.
- Plays `move` sound when `data.player !== state.me`.

### 3.3 `renjuSwap(data, state)` — `dsgRenjuTaraguchiSwapTableEvent` echo
```js
r.awaitingSwap = false;                       // ALWAYS clears the window
if (data.swap === false && game.moves.length === 4) {
   r.branchChosen = true;                     // Branch A chosen (decline+place at move-4 window)
   r.tenOffer = false;
}
```
- `swap===false` at windows 1–3 or window 5 → only clears `awaitingSwap` (branch NOT chosen).
  The bundled stone (windows 1–4) arrives via the following `dsgMoveTableEvent`.
- `swap===false` at n===4 → Branch A: `branchChosen=true, tenOffer=false`.
- `swap===true` (take-over) → only clears `awaitingSwap`; branch stays unchosen. The visual seat
  swap is done by `swapSeats`/`table.swap()`, NOT here.

### 3.4 `renjuOffer10(data, state)` — `dsgRenjuTaraguchiOffer10TableEvent` echo
```js
r.branchChosen = true;
r.tenOffer = true;
r.offered = [...data.moves];   // the 10 candidate fifth-moves
r.awaitingSwap = false;
```
→ Branch B. With n===4 this yields phase SELECTION (selected still null).

### 3.5 `renjuSelect1(data, state)` — `dsgRenjuTaraguchi10Select1TableEvent` echo
```js
game.gameState.renjuState.selected = data.move;
```
Records white's pick. (n stays 4; `renjuPhase` now reads MOVE because selected!=null — Board keeps
itself inert in this gap.) The server then sends the move-5 `dsgMoveTableEvent` → `complete=true`.

### 3.6 `swapSeats(data, state)` — `dsgSwapSeatsTableEvent` (live take-over OR silent rejoin marker)
After the existing dPente/swap2 state set and (for `!silent && swap`) the visual seat swap:
```js
if (game.isRenjuGame()) {
   game.gameState.renjuState.awaitingSwap = false;  // window resolved by a take-over
   game.gameState.renjuState.swapTaken = true;      // so bulk rejoin replay won't reopen it
}
```
- Live take-over = non-silent `swap:true` → also swaps the seat array visually.
- Rejoin marker = silent (seats come from `sendPlayingPlayers`).
- EITHER way: `awaitingSwap=false`, `swapTaken=true`. Does NOT touch `branchChosen`/`tenOffer`
  (branch is chosen by offer10 or the swap=false move-4 decline, never by a take-over).
- Phase after: n<4 → MOVE; **n===4 → BRANCH** (branchChosen stays false — the new black must still
  pick a branch).

---

## 4. `src/redux_reducers/rootReducer.js` — wiring

- Imports `renjuOpeningUiReducer, INITIAL as RENJU_UI_INITIAL` and the three reducers
  `renjuOffer10, renjuSelect1, renjuSwap`.
- `initialState.renjuOpeningUi = RENJU_UI_INITIAL` (`{mode:'idle', picks:[]}`).
- `EVENT_HANDLERS` adds:
  - `dsgRenjuTaraguchiSwapTableEvent → renjuSwap(p,s)`
  - `dsgRenjuTaraguchiOffer10TableEvent → renjuOffer10(p,s)`
  - `dsgRenjuTaraguchi10Select1TableEvent → renjuSelect1(p,s)`
- After the main switch: `newState.renjuOpeningUi = renjuOpeningUiReducer(newState.renjuOpeningUi, action)`
  (runs for EVERY action, beside `modalsReducer`).

---

## 5. `src/Classes/GameClass.js`

- New variant rule: `'renju': {replay:'renju', disableRatedOnReplay:false, add:'gomoku',
  goMove:false, player:'renju', postRule:'none'}`.
- `newInstance()` now deep-copies `renjuState` (spread + `offered:[...]`) so reducers mutate a copy.
- `reset()` sets `renjuState: freshRenjuTracking()`.
- `currentPlayer()`: when `#isRenju()`, uses `renjuOpeningPlayer(moves.length, renjuState)`; if
  non-null, returns it (drives `isMyTurn`). Falls back to parity once opening complete.
- `currentColor()`: renju → `2 - (moves.length % 2)` (black-first hover/ghost). `replayMove`/
  `replayMoveBack`/`#replayRenjuGame` all color black-first (`2 - i%2`), 15×15 grid.
- New helpers:
  - `isRenjuGame()` (public) → `game === 31 || 32 || 81`; `#isRenju` delegates to it.
  - `renjuPhaseNow()` → `renjuPhase(moves.length, renjuState)`.
  - `renjuBoxRadius()` → `(n>=1 && n<=4) ? n : 0`. Box radius about centre for the NEXT stone:
    moves 2–5 are box-constrained (radius = current stone count), move 6+ = whole board (0).
  - `renjuChoice()` → started && (`isRenjuSwapChoice` || `isRenjuBranchChoice`).

## 6. `src/Classes/TableClass.js`

- `VARIANT_COLORS['renju'] = '#D98880'` (rose lobby-card colour).
- New `myRenjuChoice(game)` → `isMyTurn(game) && game.renjuChoice()`.

## 7. `src/Classes/utils.js`

- `VARIANT_NAMES['renju'] = 'Renju'`.

## 8. `src/game/boardGeometry.js`

- `gridSizeForGame`: 31/32/81 → 15.
- `variantKey`: 31/32/81 → `'renju'`.
- `STANDARD_GAME_IDS` now includes **31** (16 ids; 14 distinct variant keys). 32/81 are not picker ids.
- `boardSpecialPoints(31/32/81)` → 9 star dots (part **52**, go-style) at indices
  `[48,52,56,108,112,116,168,172,176]` (cols/rows {3,7,11}).

## 9. `src/game/renjuSymmetry.js` — client-side offer dedup (UX pre-check; server authoritative)

D4 group via `ROTX=[1,1,1,1,-1,-1,-1,-1]`, `ROTY=[1,1,-1,-1,-1,-1,1,1]`, `ROTF=[0,1,0,1,0,1,0,1]`.
- `renjuRotate(move, r, size=15)` — image of `move` under op r about centre (off=7). r=0 identity,
  r=4 is 180° (e.g. 40↔184, 112 fixed).
- `renjuStabilizer(valueAt, size=15)` — ops 0..7 that map the **coloured placed position** onto
  itself (`valueAt(m)` is the stone value, 0=empty). Returns the stabilizer subgroup.
- `isOfferDup(move, offers, stab, size)` — true if `move` maps onto an already-offered point under
  any op in `stab`. (Asymmetric position → stab=[0] → only an EXACT duplicate is a dup; rotations
  are legal. Symmetric position → rotated offers collide.)
- `isSymmetricDup(move, offers, valueAt, size)` — convenience: stabilizer + test in one call.

---

## 10. `src/ui/renjuOpeningUi.js` — transient UI slice (board arming + modal suppression)

`INITIAL = { mode:'idle', picks:[] }`. Modes: `idle | placing | offering | pending`.

Action creators / types:
- `renjuBeginPlace()` → `BEGIN_PLACE` → `{mode:'placing', picks:[]}` (arm board for a box-constrained
  decline / Branch-A stone).
- `renjuBeginOffer()` → `BEGIN_OFFER` → `{mode:'offering', picks:[]}` (arm board for the 10-pick).
- `renjuTogglePick(move)` → `TOGGLE_PICK` → only in offering mode, adds/removes `move` from `picks`.
- `renjuMarkPending()` → `MARK_PENDING` → `{mode:'pending', picks:[]}` (a decision/move was SENT;
  suppress modal + board until the server echoes).
- `renjuResetOpeningUi()` → `RESET` → back to INITIAL (returns same ref if already idle/empty).

`ADVANCING_EVENTS` (any one → reducer resets to INITIAL): `dsgMoveTableEvent`,
`dsgSwapSeatsTableEvent`, `dsgRenjuTaraguchiSwapTableEvent`, `dsgRenjuTaraguchiOffer10TableEvent`,
`dsgRenjuTaraguchi10Select1TableEvent`, `dsgGameStateTableEvent`, `dsgChangeStateTableEvent`,
`dsgMoveTableErrorEvent`, `dsgExitTableEvent`, `dsgBootTableEvent`. So the UI is strictly
server-driven: the click sends + marks pending; only the server echo returns it to idle (and a
move-error echo unlocks a rejected pending; reset/exit/boot prevent stale leak into the next game).
Unrelated actions return the SAME state reference (no-op, like the modals slice).

---

## 11. `src/Components/Board/Board.js`

Maps `state.renjuOpeningUi → renjuUi`; dispatch `togglePick`, `markPending`.

Send helpers (each SENDS then `markPending()` — board/seat/phase only change on the server echo):
- `sendMove(m)` → `Commands.move({move:m, moves:[m], player, table})`.
- `sendRenjuDecline(m)` → `Commands.renjuSwap({swap:false, move:m, player, table})` + markPending.
- `sendRenjuSelect(m)` → `Commands.renjuSelect1({move:m, player, table})` + markPending.
- `sendRenjuOffer10(moves)` → `Commands.renjuOffer10({moves, player, table})` + markPending.

Per-render renju context (computed once before the cell loop):
- `renjuPhaseNow = game.renjuPhaseNow()`; `boxRadius = game.renjuBoxRadius()`; `center = floor(size/2)`.
- `inBox(m)` — true if boxRadius 0, else `|x-center|<=r && |y-center|<=r`.
- `picks` = `renjuUi.picks` when offering, else `[]`.
- `offers` = `renjuState.offered` when phase SELECTION, else `[]`.
- `offerStab = renjuStabilizer(valueAt, size)` when offering (computed once), where
  `valueAt(q) = abstractBoard[col][row]`.

Click-handler decision tree (only when `myTurn && started`), in order:
1. `pending` mode → board inert (no handler).
2. `placing` mode → if `empty && inBox(m)` → `sendRenjuDecline(m)`.
3. `offering` mode → if `m` already in picks → `togglePick(m)` (re-tap removes); else if
   `empty && !isOfferDup(m, picks, offerStab, size)` → if `picks.length >= 9` →
   `sendRenjuOffer10([...picks, m])` (the 10th pick AUTO-SENDS; count can never exceed 10), else
   `togglePick(m)`.
4. SELECTION phase → if `offers.includes(m)` → `sendRenjuSelect(m)` (white picks one of the ten).
5. `selected != null && moves.length===4` (Branch-B gap: select echo in, move-5 not yet landed) →
   board inert (placed AFTER the SELECTION arm, which needs selected==null, so never shadows it).
6. phase SWAP or BRANCH → board inert (decision modal is up).
7. else `empty && (!isRenju || inBox(m))` → `sendMove` (normal move; renju opening moves 2–5 are
   box-constrained).

Rendering: after marking last move, renju draws **translucent black candidates** — both the
in-progress `picks` (offering) and the ten `offers` (selection) get `board[s].deadStone =
player_colors[2]` (black-stone-gradient at 0.6 opacity). NO visual placement-box highlight is drawn
(box enforced only via click gating).

## 12. `src/Components/Board/BoardSquare.js`, `SimpleStone.js`

- `BoardSquare`: the transparent hit `<rect>` gains `pointerEvents={'all'}` so taps register even on
  fully transparent cells (needed for empty-cell renju taps).
- `SimpleStone`: `key` changed to `props.id + '-' + props.opacity` so multiple translucent stones
  (the offer/pick candidates) render distinctly.

## 13. `src/Components/Table/RenjuChoiceModal.js`

Connected: `game`, `table`, `renjuUi`; dispatch `send_message`, `beginPlace`, `beginOffer`,
`markPending`. Positioned top:70% left:70%.
- `buttons = renjuModalButtons(n, renjuState, started)`.
- `open = table.myRenjuChoice(game) && renjuUi.mode === 'idle'` (suppressed while a board
  interaction or pending is armed).
- Renders title "Taraguchi opening — your choice:" and up to 3 buttons:
  - **Take over (swap sides)** [if `buttons.swap`] → `takeOver()` →
    `Commands.renjuSwap({swap:true, move:-1, player, table})` + `markPending()`. (move=-1 is the
    no-stone sentinel; server ignores `move` on swap=true.)
  - **Decline button** [if `buttons.declinePlace`], label = `n===5 ? 'Decline swap' :
    n===4 ? 'Place 5th move' : 'Decline & place'`:
    - `n===5` → BARE decline: `Commands.renjuSwap({swap:false, move:-1, player, table})` +
      `markPending()` (no bundled stone; move 6 follows as a normal move).
    - else → `beginPlace()` (arm board; the actual stone is tapped on the board → sendRenjuDecline).
  - **Offer 10 fifth-moves** [if `buttons.offer10`] → `offer()` → `beginOffer()` (arm board 10-pick).

## 14. `src/Components/Table/RenjuOfferPanel.js`

Connected: `renjuUi`; dispatch `reset` (`renjuResetOpeningUi`). Renders beneath the board.
- `mode==='placing'` → "Decline & place — tap a point to play, or cancel" + **Cancel** (reset → idle
  → re-shows the choice modal; recoverable before any stone is sent).
- `mode==='offering'` → "Offer fifth moves: {picks.length}/10 — tap the 10th to send" + **Cancel**.
  No submit button — the 10th board tap auto-sends.
- otherwise renders `null`.

## 15. `src/Pages/Table.js`

- `<RenjuOfferPanel/>` mounted directly under `<Board/>` (same board column).
- `<RenjuChoiceModal/>` mounted alongside the other choice modals (after `<Swap2ChoiceModal/>`).

## 16. `src/protocol/messages.js` (confirm)

Three `dir:'both'` events, `req: TBL`, auto `time:0` on outbound:
| event type | cmd | out fields |
|---|---|---|
| `dsgRenjuTaraguchiSwapTableEvent` | `renjuSwap` | `['swap','move','player','table']` |
| `dsgRenjuTaraguchiOffer10TableEvent` | `renjuOffer10` | `['moves','player','table']` |
| `dsgRenjuTaraguchi10Select1TableEvent` | `renjuSelect1` | `['move','player','table']` |

Outbound frame (from `commands.test.js`): `Commands.renjuSwap({swap:false, move:113, player:'alice',
table:5})` → `{ dsgRenjuTaraguchiSwapTableEvent: { swap:false, move:113, player:'alice', table:5,
time:0 } }` (the `time:0` is auto-added). Offer10/Select1 analogous.

Inbound decode (`decode.test.js`): a `REDUX_WEBSOCKET::MESSAGE` whose JSON message is
`{ dsgRenjuTaraguchiSwapTableEvent:{...} }` decodes to `{ ok:true, event:{ type:
'dsgRenjuTaraguchiSwapTableEvent', payload:{...} } }`; payload fields preserved (e.g. `move:113`).

## 17. `src/App.css`

`.renju { fill: #D98880 }` (board-style class colour).

---

## 18. FULL OPENING SEQUENCES

Black = stone value 2 / seat-player 1; White = value 1 / seat-player 2. Stones are box-constrained:
move 2 ≤ 3×3, move 3 ≤ 5×5, move 4 ≤ 7×7, move 5 ≤ 9×9 (radius = current stone count); move 6+ free.
"Window N" = swap window after move N; the OPPONENT of the player who placed move N decides.

### Branch A (decline at move-4 → place 5th; 6-stone opening)

| step | actor | action | tracking after | phase |
|---|---|---|---|---|
| M1 | Black | place 112 (centre) | n=1, awaitingSwap | SWAP (win 1) |
| win1 | White | Decline & place → renjuSwap(swap:false,move=113) | awaitingSwap=false | MOVE |
| M2 | White | move 113 lands | n=2, awaitingSwap | SWAP (win 2) |
| win2 | Black | Decline & place → renjuSwap(false,97) | awaitingSwap=false | MOVE |
| M3 | Black | move 97 lands | n=3, awaitingSwap | SWAP (win 3) |
| win3 | White | Decline & place → renjuSwap(false,98) | awaitingSwap=false | MOVE |
| M4 | White | move 98 lands | n=4, awaitingSwap | SWAP (win 4 / branch) |
| win4 | Black | "Place 5th move" → renjuSwap(false,129) | branchChosen=true, tenOffer=false, awaitingSwap=false | (transient) |
| M5 | Black | move 129 lands | n=5, awaitingSwap=true (window 5 opens, tenOffer=false) | SWAP (win 5) |
| win5 | White | "Decline swap" → renjuSwap(swap:false,move=-1) bare | awaitingSwap=false | MOVE |
| M6 | White | normal move anywhere | n=6, complete=true | COMPLETE |

Then normal play (black to move 7). White could instead **Take over** at win 5 (`swap:true`).

### Branch B (offer 10 → white selects → 5-stone opening)

| step | actor | action | tracking after | phase |
|---|---|---|---|---|
| M1–M4 | — | identical to Branch A (windows 1–3 declined+placed) | n=4, awaitingSwap | SWAP (win 4) |
| win4 | Black | "Offer 10 fifth-moves" → tap 10 board points; 10th auto-sends renjuOffer10(moves=[…10]) | branchChosen=true, tenOffer=true, offered=[10], awaitingSwap=false | SELECTION |
| sel | White | tap one offered point → renjuSelect1(move=57) | selected=57 (n still 4) | MOVE (Board inert in gap) |
| M5 | Black(srv) | server places move 5 at 57 | n=5, tenOffer=true → window does NOT open, complete=true | COMPLETE |

Then normal play (white to move 6). Branch B has **no window 5** (tenOffer suppresses it).

### Take-over branches
- **Win 1–3 take-over** (live `dsgSwapSeatsTableEvent`, non-silent swap:true): `awaitingSwap=false,
  swapTaken=true`, seats swapped visually → phase MOVE; taker plays next stone normally.
- **Win 4 take-over**: `awaitingSwap=false, swapTaken=true`, branchChosen stays false → phase
  **BRANCH**. The new black sees a modal with NO swap button (`{swap:F, declinePlace:T, offer10:T}`)
  and picks Branch A ("Place 5th move") or Branch B ("Offer 10"). Then M5 → window 5 opens (Branch A).

### Rejoin ordering (code review #2)
On rejoin the server sends the decision echo (offer10 / silent swap-seats marker) FIRST, THEN the
bulk move list. `addMove`'s bulk path passes `isRejoin=true` so `advanceRenjuTrackingAfterMove`
respects `swapTaken` and the n===4 resolution flags and does NOT reopen the window (no spurious SWAP
modal). Open-window rejoin (bulk 4 moves, no decision echo) correctly yields SWAP.

---

## 19. Event → tracking summary

| inbound event | reducer | mutation |
|---|---|---|
| `dsgMoveTableEvent` (incremental) | `addMove` | append stone; `advanceRenjuTrackingAfterMove(false)` → opens window n / completes; clears `swapTaken` |
| `dsgMoveTableEvent` (bulk rejoin) | `addMove` | replay all; `advance(true)` → respects already-resolved window |
| `dsgRenjuTaraguchiSwapTableEvent` | `renjuSwap` | `awaitingSwap=false`; if `swap===false && n===4`: `branchChosen=true, tenOffer=false` |
| `dsgRenjuTaraguchiOffer10TableEvent` | `renjuOffer10` | `branchChosen=true, tenOffer=true, offered=[…], awaitingSwap=false` |
| `dsgRenjuTaraguchi10Select1TableEvent` | `renjuSelect1` | `selected=move` |
| `dsgSwapSeatsTableEvent` (renju) | `swapSeats` | `awaitingSwap=false, swapTaken=true` (+ visual seat swap if live) |

All of these (plus game/table-reset and move-error events) also reset the `renjuOpeningUi` slice to
`idle`.
