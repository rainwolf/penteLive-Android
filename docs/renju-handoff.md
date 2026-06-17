# Renju (Taraguchi-10) — Android implementation handoff

Extracted from `pente.org/docs/renju-integration-guide.md` (§10 live + §12 turn-based) on branch `feat/renju`. TWO parts: the **live** opening UI and the **turn-based (correspondence)** handoff — both must be wired (the app plays both transports).

- Cross-refs like `§2.4`/`§2.6`/`§7`/`§8` point to that integration guide; `§8` is the React reference implementation.
- Reference client: `rainwolf/react_live_game_room#5`. Live+TB server: `rainwolf/pente.org#8`.
- Canonical Renju board colour: **#D98880** (distinct from gomoku #A3FDEB).
- Anchors were grounded but **line numbers drift — grep the symbol** before editing. Resolve "(verify)" items during planning.
- LIVE path **derives** the phase from echoes; TURN-BASED **reads** the server-shipped `renjuPhase` and submits via the `renjuAction` contract (§2.4 — three actions: `swap` (take-over, no stone) · `move` (1 or 10 stones; branch inferred by count; windows-1–3 decline is a `move`) · `select` (atomic 2-stone: move 5 + move 6)).
- Suggested flow: treat each part as a spec → `writing-plans` → `subagent-driven-development`; build + test natively.

---

## 10. Sub-project 5 — Android (`pentelive-android`) Taraguchi-10 handoff

Zero-context handoff for a fresh agent wiring the Renju (Taraguchi-10) opening into the
`pentelive-android` app. **iOS and Android have no Renju support today**; this section (Android)
and §9 (iOS) are their from-scratch handoffs. Every anchor below was grep-verified against the
submodule on this branch; line numbers are as-of-now and may drift, so grep the symbol.

**TRANSPORT VERDICT — Android plays BOTH.** Unlike the React reference (live-only WebSocket),
Android has two live-game *and* turn-based code paths:
- **LIVE** — raw **SSL TCP** socket (NOT WebSocket) speaking the `dsg*TableEvent` JSON protocol,
  byte-`255` frame delimited (`SocketDSGEventHandler.run:44-84`), dispatched by an if-else chain
  in `LiveGameRoomActivity.eventOccurred:231-420`. The live server sends **NO `renjuPhase`**
  (§2.6 / §4) — the live client must **DERIVE** the opening phase from tracked echo state, exactly
  like React §8.2 (see §10.2a).
- **JSON / turn-based** — HTTP `GET gameServer/mobile/json/game.jsp` → Gson `GameResponse`
  (`Game.java:432`), moves sent as an HTTP **GET** with query params (`OkHttpPenteApi.submitMove`
  uses `get(url)` at `:174`). Here the server **ships the derived phase** in `renjuPhase` (§2.6) —
  read it directly (see §10.2b).

So Android needs **both shapes**. **Recommended order: do LIVE first** (it mirrors the React
reference port, and all of Android's existing opening UI lives in the live path). The turn-based
path has **no opening UI at all today** (the offline `Game.java`/`BoardView.java` screen only
places ordinary moves), so TB Renju opening is a larger from-scratch build and can be deferred —
but still add the read-side fields (`GameResponse`) and the `renjuAction` param so historic
viewers and TB reads don't break.

**Where Android genuinely differs from React (do not force the React shape):**
- **Language/UI:** Java + Android `Canvas` drawing + `AlertDialog`, not JSX/MUI.
- **No Protocol abstraction.** React has a `protocol/` module (decode + `MESSAGES` registry +
  `Commands` facade). Android has **none of that**: inbound is a raw `if (jsonEvent.get("dsgXEvent")!=null)`
  chain over a `Map<String,Object>` (from `jsonToMap`), and **outbound events are built by raw
  JSON string concatenation** (`LiveBoardView:156`, `LiveTableFragment.sendSwap2Choice`). There is
  **no Gson serialization on the live send path** and no `Commands.<cmd>(...)` — you add three
  `else if` arms + three handler methods + three string builders.
- **Two board views, two screens.** Offline/TB = `Game.java` + `BoardView.java`; live =
  `LiveGameRoomActivity` + `LiveTableFragment` + `LiveBoardView.java`. Both board views hardcode
  19×19 star points; fix **both**.
- **The `rules/` module is a lightweight variant registry**
  (`Variant`/`Variants`/`BoardState`), **NOT** a copy of the server `org.pente.game` engine. So
  like React, **derive the phase from echoes — do not run a client-side engine** (there isn't one).
- **Stone-color convention is already black-capable.** `BoardState.java:6` encodes `0=empty,
  1=white, 2=black, -1=forbidden`. The `currentColor()` Go (PLAY) arm (`Table.java:282`,
  `2 - moves.size()%2`, inside `if (isGo())` → `goState==PLAY`) is already black-first — Renju
  reuses that formula. Do **not** copy the Connect6 else-branch formula at `:288-293`
  (`2 - (((moves.size()-1)/2)%2)`, 2-stones-per-turn) — it is wrong for single-stone Renju.

### 10.0 Board basics (restated for this client)
- Board **15×15**, game ids **31 (Renju) / 32 (Speed Renju) / 81 (TB Renju)**. Move encoding
  `x + y·15`; **center = 112** (`7 + 7·15`); the **server auto-places** it as move 1 — the live
  client receives it as an ordinary `dsgMoveTableEvent`, it must **not** place the center itself.
- **Board background colour = `#D98880` (dusty rose)** — the canonical Renju board colour, **distinct
  from gomoku's `#A3FDEB`**. Matches the web (`renjuColor`, `gameServer/tb/gameScript.js:14`) and
  react_live_game_room (`.renju` / `VARIANT_COLORS['renju']`). Android selects the board colour by id
  in `Table.getGameColor:904-932`; set ids 31/32 to this value (see §10.3 step 3).
- **Black plays first.** Android board values: `1 = white`, `2 = black` (`BoardState.java:6`;
  confirmed in `LiveBoardView.drawStone` — `stoneColor==2` renders black). So "black first" ⇒ the
  first stone must carry board **value 2**. Today the default `currentColor()` arm
  (`Table.java:287`) returns `1 + (moves.size()%2)` → first stone = value 1 = **WHITE** (inverted
  for Renju). The fix is a Renju arm = `2 - moves.size()%2` (black-first), identical to the
  existing Go (PLAY) arm at line 282 (inside `if (isGo())` → `goState==PLAY`). Do **not** copy the
  Connect6 else-branch formula at `:288-293` (`2 - (((moves.size()-1)/2)%2)`, 2-stones-per-turn) — it
  is wrong for single-stone Renju.
- Move encoding parity: `LiveBoardView:147` already computes `playedMove = gridSize*stoneI + stoneJ`
  = `x + y·gridSize` (correct once `gridSize=15`). The bug is **decode**: `Table.addMove:199-200`
  hardcodes `/19` and `%19`.
- Phase source depends on transport (see §10.2): **LIVE derives**, **TB reads `renjuPhase`**.

### 10.1 Confirmed anchors (file : symbol)
All grep-verified in the submodule. **(WRONG today)** = breaks for Renju as-is; **(OK)** = already
correct / reusable as precedent; **(verify)** = could not fully confirm from code.

| Area | File | Symbol / fact |
|---|---|---|
| variant enum | `rules/.../pente/rules/Variant.java` | `enum Variant:13-29` — entries `(canonicalGameId, gridSize, CaptureRule, stonesPerTurn)`; live ids 1–29 (+ even speed doubles); `GO_13(23,13,NONE,1)`, `GO_19(19,19,NONE,1)`; predicates `isDPente`/`isSwap2`/`isGo` (no `isRenju`). **No 31/32/81.** **(WRONG today)** |
| variant by id | `rules/.../Variants.java` | `fromGameId:87-89` → `BY_CANONICAL_ID.get(canonical)` (odd canonical, even = id-1). `31/32/81` → **null** → NPE / silent default. **(WRONG today)** |
| variant by name | `rules/.../Variants.java` | `fromGameType:33` — substring match on `gameName`; `"Renju"` not handled → null (the TB/offline lookup path). **(WRONG today)** |
| live game names | `app/.../liveGameRoom/Table.java` | static `gameNames` map `:54-86` — ids 1→30 only (ends `30→"Speed Swap2-Keryo"`); `getGameName()` returns null for 31/32. **(WRONG today)** |
| live grid size | `Table.java` | `setGame:1090-1102` sets `gridSize` by id (`==21/22→9`, `==23/24→13`, **else 19**); no 31/32 → 19. Default `gridSize=19` (`:102`); `setGridSize:98-99` exists. **(WRONG today)** |
| live move decode | `Table.java` | `addMove:199-200` — `move_i = move/19; move_j = move%19` (hardcoded 19). For Renju, `112` decodes to `board[5][17]` (off-center). NOTE: the capture helpers (`:537+`) already use `gridSize`. **(WRONG today)** |
| live move encode | `app/.../liveGameRoom/LiveBoardView.java` | `onTouchEvent:142-156` — `stoneI = gridSize*stoneY/size`, `playedMove = gridSize*stoneI + stoneJ` = `x + y·gridSize` (matches contract once `gridSize=15`); emits move JSON at `:156`. **(OK)** |
| live render decode | `LiveBoardView.java` | `drawBoard:216-222` — `movei = move/gridSize; movej = move%gridSize`. **(OK)** |
| stone-color encoding | `rules/.../BoardState.java` | `:6` — `0 empty / 1 white / 2 black / -1 forbidden` (authoritative). **(OK)** |
| first-stone color | `Table.java` | `currentColor:279-294` — default arm `:287` `1 + (moves.size()%2)` → move 0 = value 1 = **WHITE** (inverted for Renju). The black-first pattern Renju needs is the Go (PLAY) arm `:282` `2 - moves.size()%2` (inside `if (isGo())` → `goState==PLAY`) — **NOT** the Connect6 else-branch `:288-293` (`2 - (((moves.size()-1)/2)%2)`, 2-stones-per-turn, wrong for single-stone Renju). **(WRONG today)** |
| opening-player FSM | `Table.java` | `currentPlayer:300-342` — derives to-move seat for dPente/swap2 from move count + state enums (arms `1 + moves.size()%2` at `:309,:335`); no Renju branch. **(needs Renju arm)** |
| swap-state enums | `app/.../liveGameRoom/DPenteState.java`, `Swap2State.java` | `DPenteState:4 = {NOCHOICE, SWAPPED, NOTSWAPPED}`; `Swap2State:4 = {NOCHOICE, SWAP2PASS, SWAPPED, NOTSWAPPED}`. Pattern for a new `RenjuState` enum. **(OK precedent)** |
| opening-phase tracking | `app/.../liveGameRoom/GameState.java` | `:8-12` — fields `state`, `dPenteState`, `swap2State`, `goState`; **no `renjuState`**. **(WRONG today / missing)** |
| live event dispatch | `app/.../liveGameRoom/LiveGameRoomActivity.java` | `eventOccurred:231-420` — if-else chain on `jsonEvent.get("dsg…Event")`; has `dsgMoveTableEvent:345→updateTableMove:491`, `dsgSwapSeatsTableEvent:363→swapSeats:571`, `dsgSwap2PassTableEvent:366→swap2Pass:591`, `dsgSystemMessageTableEvent:409→addTableMessage:481`. **No `dsgRenju*`.** This is the dispatch seam. **(WRONG today / missing)** |
| live transport frame | `app/.../org/pente/gameServer/event/SocketDSGEventHandler.java` | `run:44-84` — raw SSL TCP; reads bytes until `255` (`:56`), UTF-8 string (`:59`) → `notifyListeners(String)` (`:69`); outbound writes terminator `255` (`:110`). **NOT WebSocket.** **(OK)** |
| outbound build (live) | `LiveBoardView.java:156` / `LiveTableFragment.sendSwap2Choice` | raw JSON **string concatenation** via `fragment.getListener().sendEvent("{…}")`; no Gson/Commands facade on send. **(OK pattern)** |
| swap dialog precedent | `app/.../liveGameRoom/LiveTableFragment.java` | `showDPenteChoice:1017` / `showSwap2Choice:1049` — `AlertDialog.Builder` + `setItems(options, cb)`, `Gravity.BOTTOM` (`:565+,:973`); send `dsgSwapSeatsTableEvent`. **Yes/no only, no board interaction.** **(OK pattern)** |
| dialog trigger | `LiveTableFragment.java` | `addMove:452-510` — after `table.addMove(move)` checks `isDPente()&&moves==4&&NOCHOICE` etc. → shows the modal. Renju gates here off the derived phase. **(OK pattern)** |
| translucent stones | `LiveBoardView.java` | `drawStone:243-278` — Go dead stones (`stoneColor==3/4`) use `stonePaint.setAlpha(180)` (`:264,:268`). Reusable for translucent candidates. **(OK)** |
| board-tap legality | `LiveBoardView.java` | `onTouchEvent:142-153` — only checks the cell is empty (`abstractBoard[i][j]!=0`); no central-square / forbidden checks (server-side). **(OK)** |
| system-message handler | `LiveGameRoomActivity.java` | `dsgSystemMessageTableEvent:409-413` — `data.get("message")` → `addTableMessage(tableId, "* "+msg)`. The Branch-B selector prompt (server→white) routes here; gating the picker on it is new UI. **(OK, repurpose)** |
| TB JSON model | `app/.../JsonModels.java` | `GameResponse:121-154` — fields `gameName:125`, `moves`, `state:136`, `goState:137`, `dPenteState:142`, `swap2pass:143`. **No `renjuPhase`/`renjuOffers`/`renjuSwaps`.** **(WRONG today / missing)** |
| TB move submit | `app/.../net/OkHttpPenteApi.java` | `submitMove:163-176` — query params `command=move`, `gid`, `moves` (`, message`). **No `renjuAction`.** Cannot speak the §2.4 opening contract. **(WRONG today / missing)** |
| TB game load | `app/.../Game.java` | `RetrieveGame.doInBackground:432` — HTTP GET `game.jsp?gid=` → Gson `GameResponse`. **(OK)** |
| offline board size | `Game.java` | `parseGame:998-1007` — `gameType.contains("(9x9)")→9`, `("(13x13)")→13`, **else 19**; no `"Renju"` → 19. **(WRONG today)** |
| offline star points | `app/.../BoardView.java` | `drawBoard:483-487` — hardcoded index `6` (`margin+6*step`), 4 corners + center; correct for 19×19, wrong for 15×15. Live twin: `LiveBoardView.drawBoard:200-204` same. **(WRONG today)** |
| offline coord labels | `BoardView.java` | `onTouchEvent:364-406` — modulo 19 (`coordinateLetters[m%19]`, `19-(m/19)`); `coordinateLetters` (`:71`) = 19 letters A–T skip I. Renju needs first 15 (A–P skip I) and `% gridSize`. **(WRONG today)** |
| server wrapper keys | backend `…/event/DSGEventWrapper.java` (reference) | The three live frames use exact top-level keys **`dsgRenjuTaraguchiSwapTableEvent`**, **`dsgRenjuTaraguchiOffer10TableEvent`**, **`dsgRenjuTaraguchi10Select1TableEvent`** (one top-level key per frame). Android's `eventOccurred` keys + outbound strings **must** match these byte-for-byte. **(server contract)** |
| server event fields | backend `…/event/DSGRenjuTaraguchi*.java` (reference) | `…Swap`: `boolean swap`, `int move`. `…Offer10`: `int[] moves`. `…Select1`: `int move`. All extend `AbstractDSGTableEvent` → inherited `String player`, `int table`; `AbstractDSGEvent` → `long time`. Each inner object carries `player`/`table`/`time` plus its own fields. **(server contract)** |

**Resolve before coding (table-specific verify):** the survey's *protocol* pass claimed
`currentColor()` returns black (value 2) on the first move; that is **wrong** — `BoardState.java:6`
(`1=white, 2=black`) plus `currentColor:287` (`1 + moves%2` → first = value 1 = white) confirm the
first stone renders **white** today. The black-first fix (a `2 - moves%2` Renju arm) is therefore
**required**, not optional. Confirm by rendering a live Renju move 1 after the fix.

### 10.2 Two phase sources (LIVE derives · TB reads `renjuPhase`)

Heading adapted because Android is **BOTH**. The live path mirrors React §8.2; the TB path mirrors
the §2.6/§4 read.

#### 10.2a LIVE phase derivation (mirror React §8.2)
The live server sends **no `renjuPhase`**; the engine is server-side only. The Android live client
**accumulates** a tracked opening record **from the echo events** (§10.4) and derives the phase
from it + the move count — exactly the React approach. Add a new `RenjuState` enum (mirror
`DPenteState`/`Swap2State`) **plus** a tracked object on `GameState`:

```
renjuState = {
  swapWindowOpen,   // bool — is the current swap window still undecided?
  branch,           // null | 'A' | 'B' — set by the move-4 decision echoes
  offers,           // int[] | null — the 10 Branch-B candidates (offer10 echo)
  selection,        // int | null — white's pick (select1 echo)
}
// NO net-swap/orientation field. Who-owns-black comes from table.seats (the visual seat
// swap that rides dsgSwapSeatsTableEvent on a take-over, and sendPlayingPlayers on rejoin) —
// NEVER from the silent rejoin swap event (its swap bit is the current window's decision,
// not net orientation).
```

`movesLength` = stones on board (incl. the auto-center = move 1). Add a pure
`renjuPhase(movesLength, renjuState)` helper (Android has no `openingPhase.js` analogue — the
dPente/swap2 logic lives inline in `Table.currentPlayer`/`currentColor` + `LiveTableFragment.addMove`,
so put the helper on `Table`/a small `RenjuPhase` class):

| movesLength | tracked state | phase | to-move acts |
|---|---|---|---|
| 1 | swapWindowOpen | `SWAP` (window 1) | Swap, **or** decline + place move 2 ∈ 3×3 |
| 2 | swapWindowOpen | `SWAP` (window 2) | Swap, **or** decline + place move 3 ∈ 5×5 |
| 3 | swapWindowOpen | `SWAP` (window 3) | Swap, **or** decline + place move 4 ∈ 7×7 |
| **4** | **swapWindowOpen** | **`SWAP`** (window 4) | THREE actions: `swap=true` take-over → `BRANCH` (no stone) · `swap=false` **bundled with move 5 ∈ 9×9** → Branch A · `Offer10` → Branch B |
| **4** | swap decided, `branch===null` | **`BRANCH`** | Branch A: place move 5 ∈ 9×9 · Branch B: offer 10 |
| **4** | `branch==='B'`, offers present | **`SELECTION`** | white picks 1 of the 10 → becomes move 5 |
| **5** | `branch==='A'`, swap-5 undecided | **`SWAP`** (window 5) | Swap, **or** decline → then move 6 |
| **5** | `branch==='A'`, swap-5 decided | `NORMAL` (move 6 anywhere) | place move 6 |
| **5** | `branch==='B'` (selection done) | `NORMAL` (move 6 anywhere) | place move 6 — **no swap-5 window in Branch B** |
| ≥6 | — | `COMPLETE` / `NORMAL` | plain alternation; black forbidden points **server-enforced** |

> **Naming reconciliation (docs only, no code impact).** This live-derived table uses `NORMAL` and
> folds the Branch-B offer step into `BRANCH`; §10.2b and the backend enum use `MOVE` and a distinct
> `OFFERS`. Map them: live-derived `NORMAL` == server `MOVE`; the server `OFFERS` phase is represented
> inside the live `BRANCH` state (movesLength 4, `branch == null`).

**Move-4 model (live).** Three wire actions at the move-4 window: (a) `swap=true` take-over →
standalone `BRANCH` (no stone); (b) `swap=false` **bundled with move 5 in the 9×9** = Branch A
(advances to 5 moves — there is **no** stoneless move-4 decline); (c) `Offer10` = Branch B. The
standalone `BRANCH` state arises **only** after a take-over. Branch-A move 5 always arrives as a
**swap event** (`swap=false`, with the move), not a branch event.

**Rejoin / spectate (§7 current-decision-point signal).** On (re)join the server sends the
authoritative seats (`sendPlayingPlayers`) **plus exactly one** signal keyed by `numMoves`:
*nothing* (window open / opening complete), a **silent** `dsgSwapSeatsTableEvent` (window resolved
→ `MOVE`/`BRANCH`), an **offer10** frame (Branch-B selection pending), or a replayed **select1**
frame (Branch-B move 5 chosen). Android's `swapSeats:571` handler must learn the silent branch for
Renju: **advance the tracked phase for the current window only; do NOT re-swap seats** (seats are
already current from `sendPlayingPlayers`; its `swap` bit is the current window's decision, not net
orientation) — the same contract Android already honours for the dPente silent swap.

**Do not** port `RenjuState`'s server-side Taraguchi-10 engine into Android — it drags the
forbidden-point finder with it. Track the four decision variables the echoes carry; that is enough.

#### 10.2b TB phase (read `renjuPhase` from `GameResponse`, §2.6 / §4)
For turn-based Renju (`TB_RENJU=81`) the server **already ships** the derived phase. Add the three
fields to `GameResponse` and **read** them — no derivation:
`renjuPhase ∈ {SWAP, BRANCH, OFFERS, SELECTION, MOVE, COMPLETE}` plus `renjuOffers` (the persisted
Branch-B candidates) and `renjuSwaps` (packed decisions). **Caveat:** the offline/TB screen
(`Game.java`/`BoardView.java`) has **no opening UI today** — all opening dialogs/pickers live in
the live `LiveTableFragment`. So the TB opening flow (swap windows, branch, 10-pick, selection) is
a from-scratch build on the offline screen and is **recommended deferred**; add the fields +
`renjuAction` param now so the *read* side and historic viewers work, then build the TB opening UI
as a follow-up (or reuse the live pickers if the screens are unified).

### 10.3 File-by-file map (the real work)
Live-first ordering. **(L)** = live path, **(TB)** = turn-based/offline, **(both)** = shared rules.

1. **`rules/.../pente/rules/Variant.java`** *(both)* — add `RENJU(31, 15, CaptureRule.NONE, 1)`
   (captures NONE — Renju has no captures; forbidden points are server-enforced). `SPEED_RENJU`
   reuses this entry via canonical id 31 (the enum lists only odd canonicals). Add an
   `isRenju()` predicate (`this == RENJU`) alongside `isDPente()/isSwap2()/isGo()`. **`TB_RENJU=81`
   has no canonical entry** (existing TB games aren't in this enum either) — resolve via the
   `fromGameType` string path below, or add an explicit `81→RENJU` mapping **(verify which path the
   TB code hits)**.
2. **`rules/.../Variants.java`** *(both)* — `fromGameId:87-89`: ensure `31→RENJU`, `32→`(canonical
   31)`→RENJU`; add `81→RENJU` if the TB path calls `fromGameId(81)`. `fromGameType:33`: add a
   `"Renju"` / `"Speed Renju"` / `"TB Renju"` substring arm → `RENJU` (the string the server puts
   in `GameResponse.gameName` — **verify the exact value**).
3. **`app/.../liveGameRoom/Table.java`** *(L)* —
   - `gameNames:54-86`: `put(31, "Renju"); put(32, "Speed Renju");` (81 is TB, not a live table id
     — confirm live never sees 81).
   - `setGame:1090-1102`: add `else if (game==31 || game==32) gridSize = 15;` **before** the
     `else gridSize=19`.
   - `addMove:199-200`: replace `move/19`,`move%19` with `move/gridSize`,`move%gridSize` (also fixes
     Go 9/13 live). This is the **game-breaking** decode bug.
   - `currentColor:279-294`: add a Renju arm returning `2 - moves.size()%2` (black-first), mirroring
     the Go (PLAY) arm at `:282` (inside `if (isGo())` → `goState==PLAY`) — **not** the Connect6
     else-branch at `:288-293` (`2 - (((moves.size()-1)/2)%2)`, 2-stones-per-turn, wrong for Renju).
     Without it the first stone renders white.
   - `currentPlayer:300-342`: add a Renju opening branch (mirror the `isDPente`/`isSwap2` arms) that
     calls a new `renjuOpeningPlayer(moves.size(), renjuState)` so `isMyTurn` is right during the
     opening **(verify exact need)**.
   - `getGameColor:904-932`: add `31/32` → a new `renjuColor = 0xFFD98880` constant (dusty rose, the
     canonical Renju board colour, §10.0; today ids 31/32 fall through to `swap2KeryoColor` = wrong).
   - add `isRenju()` (mirror `isDPente:269` / `isSwap2:274`).
4. **`app/.../liveGameRoom/LiveBoardView.java`** *(L)* — `drawBoard:200-204`: add a Renju
   star-point branch. Match §4's `{3,7,11}` (center 7). The existing renderer draws a **5-point**
   set (4 corners + center) — for Renju that is indices **`[48, 56, 168, 176, 112]`**
   (`(3,3)/(11,3)/(3,11)/(11,11)/(7,7)`, index `= col + row·15`); or switch to the full Go-style
   9-dot set like React §8.3 (`[48,52,56,108,112,116,168,172,176]`). Encode/decode already
   `gridSize`-correct; `setGridSize:44-46` flows from `LiveTableFragment.updateTable:395`
   (`board.setGridSize(table.getGridSize())`). Reuse `drawStone` `setAlpha(180)` (`:264,:268`) for
   translucent candidates (§10.6).
5. **`app/.../liveGameRoom/GameState.java`** *(L)* — add a `RenjuState renjuState` field (new enum)
   **and/or** the tracked `renjuState` object of §10.2a. Initialize it where `dPenteState`/`swap2State`
   are reset.
6. **`app/.../liveGameRoom/LiveGameRoomActivity.java`** *(L)* — in `eventOccurred:231-420` add three
   `else if` arms (keys **must** equal the wrapper keys in §10.1):
   ```java
   } else if (jsonEvent.get("dsgRenjuTaraguchiSwapTableEvent") != null) {
       handleRenjuSwap((Map<String,Object>) jsonEvent.get("dsgRenjuTaraguchiSwapTableEvent"));
   } else if (jsonEvent.get("dsgRenjuTaraguchiOffer10TableEvent") != null) {
       handleRenjuOffer10((Map<String,Object>) jsonEvent.get("dsgRenjuTaraguchiOffer10TableEvent"));
   } else if (jsonEvent.get("dsgRenjuTaraguchi10Select1TableEvent") != null) {
       handleRenjuSelect1((Map<String,Object>) jsonEvent.get("dsgRenjuTaraguchi10Select1TableEvent"));
   }
   ```
   Add the three handler methods (mirror `updateTableMove:491` / `swapSeats:571` / `swap2Pass:591`).
   They **update opening-tracking state ONLY and place NO stones** — stones ride
   `dsgMoveTableEvent → updateTableMove`:
   - `handleRenjuSwap`: mark the current window decided; at `movesLength==4`, **any** `swap=false`
     echo carrying a valid stone ⇒ `branch='A'`. The take-over visual seat swap rides a separate
     `dsgSwapSeatsTableEvent` (already handled by `swapSeats`).
   - `handleRenjuOffer10`: `branch='B'`, `offers = (List) data.get("moves")`.
   - `handleRenjuSelect1`: `selection = data.get("move")`.
   Also extend `swapSeats:571`: in the **silent** branch for Renju, advance the tracked phase for
   the current window (do not re-swap). Repurpose `dsgSystemMessageTableEvent:409` to gate the
   Branch-B selection UI (the server→white prompt arrives here).
   Note: `jsonToMap` yields JSON numbers as `Double`/`Long` and arrays as `List` — cast `move`/`moves`
   exactly as `updateTableMove:491-507` already does.
7. **`app/.../liveGameRoom/LiveTableFragment.java`** *(L)* — the opening UI. After `table.addMove`
   (the existing `addMove:452-510` dispatch point) read the derived phase and show the right control,
   reusing the `showSwap2Choice:1049`/`showDPenteChoice:1017` pattern (`AlertDialog.Builder.setItems`,
   `Gravity.BOTTOM`) for the yes/no cases and `sendSwap2Choice:1092` (raw JSON string →
   `mListener.sendEvent(...)`) for sending:
   - **SWAP windows 1–3:** "Swap (take over)" / "Don't swap" — decline **bundles** the next opening
     stone (constrained to the move's central square).
   - **move-4 SWAP window:** three choices — take-over, Branch A (decline + place move 5 ∈ 9×9),
     Branch B (offer 10).
   - **BRANCH** (after take-over): place move 5 ∈ 9×9, or offer 10.
   - **SELECTION:** white picks one of the 10.
   The board interaction (central-box highlight, 10-pick multi-select, translucent candidates,
   selection screen) is **new** — see §10.6.
8. **`app/.../JsonModels.java`** *(TB)* — add to `GameResponse:121-154`:
   `public String renjuPhase; public String renjuOffers; public Integer renjuSwaps;`
   These match the backend `GameResponse.java:45-47` exactly (confirmed): `renjuPhase` (`String`,
   one of `SWAP|BRANCH|OFFERS|SELECTION|MOVE|COMPLETE`, else null), `renjuOffers` (`String`,
   comma-separated offered moves, else null), `renjuSwaps` (`Integer`, packed opening word, else
   null). The String/String/Integer POJO is correct. Gson tolerates missing fields, so this is
   backward-safe.
9. **`app/.../net/OkHttpPenteApi.java`** *(TB)* — `submitMove:163-176`: add a `renjuAction` query
   param (overload `submitMove(gid, moves, message, renjuAction)` →
   `.addQueryParameter("renjuAction", …)`) to speak the §2.4 contract — the three actions
   `swap` (take-over, no `moves`), `move` (1 stone = decline+place / Branch A; 10 stones = Branch B;
   branch inferred by count), and `select` (atomic 2-stone move 5 + move 6) (§2.4 / §12).
10. **`app/.../Game.java` + `app/.../BoardView.java`** *(TB, deferrable)* —
    `Game.parseGame:998-1007`: add `"Renju"` → `gridSize=15`. `BoardView.drawBoard:483-487`: Renju
    star points (same set as step 4). `BoardView.onTouchEvent:364-406` + `coordinateLetters:71`: use
    `% gridSize` and the first-15 label set **A–P skipping I** (instead of `%19` / 19 letters). The
    TB opening UI itself (no precedent on this screen) is the deferred follow-up.

### 10.4 Wire examples (verified keys + fields)
**Live (raw JSON strings).** Android builds outbound frames by **string concatenation** (no
`Commands` facade) — e.g. existing moves: `sendEvent("{\"dsgMoveTableEvent\":{\"move\":" + m +
",\"moves\":[" + m + "],\"player\":\"" + me + "\",\"table\":" + table + ",\"time\":0}}")`
(`LiveBoardView:156`). Inbound arrives as a `Map<String,Object>` via `jsonToMap` with a
**server-stamped non-zero `time`**. One literal per event (table 5, center 112, 15×15):

**Swap event** — take-over, decline+place, or Branch-A move 5 (all share this event):
```json
// outbound: decline window-1 swap + place move 2 at col8,row7 (=113, in 3×3)
{ "dsgRenjuTaraguchiSwapTableEvent": { "swap": false, "move": 113, "player": "alice", "table": 5, "time": 0 } }
// outbound: take over the side (no stone). move = -1 no-move sentinel — server ignores `move` on
// swap=true; -1 (not 0, a legal corner cell) is unambiguous (cf. ServerTable:1529)
{ "dsgRenjuTaraguchiSwapTableEvent": { "swap": true,  "move": -1,  "player": "bob",   "table": 5, "time": 0 } }
// inbound echo (server time stamped); the stone, if any, arrives separately as dsgMoveTableEvent
{ "dsgRenjuTaraguchiSwapTableEvent": { "swap": false, "move": 113, "player": "alice", "table": 5, "time": 1718400000123 } }
```
**Offer 10** (Branch B — black offers ten 5th-move candidates, no two D4-symmetric — offsets
`(1,0)(2,0)(3,0)(4,0)(1,1)(2,1)(3,1)(4,1)(2,2)(3,2)` about centre 112 → **10 distinct {|dx|,|dy|} orbits**):
```json
{ "dsgRenjuTaraguchiOffer10TableEvent": { "moves": [113,114,115,116,128,129,130,131,144,145], "player": "alice", "table": 5, "time": 0 } }
```
**Select 1** (white picks one of the ten → becomes move 5; the stone follows as a `dsgMoveTableEvent`):
```json
{ "dsgRenjuTaraguchi10Select1TableEvent": { "move": 130, "player": "bob", "table": 5, "time": 0 } }
```
**Stone (always separate):** `{ "dsgMoveTableEvent": { "move": 113, "moves": [113], "player": "alice", "table": 5, "time": 0 } }` — the §10.1 `LiveBoardView:156` format.

**Turn-based (HTTP query params).** `OkHttpPenteApi.submitMove` builds
`gameServer/tb/game?command=move&gid=<gid>&moves=<payload>&renjuAction=<action>`, resolved by
`RenjuTbContract.resolve`. **Full phase-driven contract: §2.4 and §12** — three actions, branch
inferred from the `move` stone count:

| `renjuAction` | phase | `moves` payload | server behavior |
|---|---|---|---|
| `swap` | SWAP | none (`moves` ignored) | Take over the opponent's side at the open swap window — seats swap, **no stone** placed. The next decision (branch / next stone) arrives as a subsequent `move`. |
| `move` | SWAP / BRANCH / MOVE | `<m>` (1 stone) | Auto-declines a pending swap first, then places one stone — windows 1–3 → the next opening stone; **at the branch point** (move 4, branch unchosen; fresh-decline *or* post-take-over) → Branch A move 5 (restricted to the 9×9 centre); MOVE phase → a plain opening stone. |
| `move` | SWAP@4 / BRANCH | `<s1>,…,<s10>` (10 stones) | Auto-declines a pending swap, then Branch B: take the ten-offer branch and validate + persist the ten 5th-move offers **atomically**. Only valid at the branch point. |
| `select` | SELECTION | `<m5>,<m6>` (2 stones) | **Atomic**: commit one of the ten offered moves as **move 5 (black)** *and* place **move 6 (white)** → opening complete. Stores neither unless both are legal. |

Branch A vs B is inferred from the `move` stone count alone (1 = A, 10 = B) — no separate
branch/offer request. Declining a swap is implicit in sending a `move`; **windows-1–3 decline is a
`move`, not `swap "0,m"`**. MOVE/COMPLETE stones go as a plain `command=move` with **no `renjuAction`**.

Concrete (one action per step): decline window-1 + place 113 → `…&moves=113&renjuAction=move`; take
over → `…&renjuAction=swap` (no `moves`); Branch A move 5 @130 → `…&moves=130&renjuAction=move`;
Branch B offers → `…&moves=113,114,115,116,128,129,130,131,144,145&renjuAction=move`; select move 5
@130 + move 6 @131 → `…&moves=130,131&renjuAction=select`.

**Contract reminders (§7):** never place stones from the three echoes — stones ride
`dsgMoveTableEvent`. On (re)join, take seats from `sendPlayingPlayers`; the rejoin signal is
exactly one of {*nothing* / silent `dsgSwapSeatsTableEvent` / `offer10` / `select1`} — its silent
swap `swap` bit is the **current window's** decision, **not** net orientation. **Recovery:** if a
declined-swap's bundled stone is rejected, the decline is already committed → recover by re-sending
the stone as a plain `dsgMoveTableEvent`; if the ten offers are rejected, the move-4 decline +
Branch B are already committed → recover by re-sending a corrected ten.

### 10.5 Offer symmetry dedup (client-side, UX nicety)
The ten Branch-B offers must contain no two D4-symmetric duplicates. The server already rejects
violations (`RenjuState.offerFifthMoves` → `offerFifthMove`), so client-side checking is a **UX
nicety** (instant feedback vs a round-trip error) — recommended, not required. **The ten offers are
NOT box-constrained** — any in-bounds, empty, non-D4-symmetric point is legal (corners included);
only the **Branch-A** move 5 is restricted to the 9×9. So the 10-pick picker must allow the **whole
board** (minus occupied + symmetric-duplicate cells).

Port the algorithm to Java (15×15, center `(7,7)`):
- For move `m`: `x = m % 15`, `y = m / 15`, `dx = x - 7`, `dy = y - 7`.
- The **8 D4 images** of `(dx,dy)`: rotations `(dx,dy)`, `(-dy,dx)`, `(-dx,-dy)`, `(dy,-dx)` and
  reflections `(-dx,dy)`, `(dx,-dy)`, `(dy,dx)`, `(-dy,-dx)`. Map each back:
  `m' = (tx + 7) + (ty + 7)·15`.
- Reject an offer if **any** of its 8 images equals an already-accepted offer. Maintain a running
  set of all images of accepted offers and test membership (`n/10` counter, §10.6).

Mirror the proven reference so the client agrees with the server exactly: the JSP port
`renjuRotate` / `renjuStabilizer` / `renjuIsSymmetricDup` in `gameServer/tb/mobileGame.jsp` (itself
a JS port of `SimpleGridState.rotateMove` + the position stabilizer, §3).

### 10.6 New UI primitives (no Android precedent)
The swap2/dPente dialogs are plain yes/no (`AlertDialog` + `setItems`, §10.1) — the Renju opening
needs board-level interaction with **no analogue** in this client. Single-tap placement
(`LiveBoardView.onTouchEvent:142-153`) is the only existing board interaction; there is **no**
multi-select, **no** zone highlight (`drawBoard:171-241` draws only lines + star points).
- **Central-box highlight** — a new `Canvas` draw layer in `LiveBoardView.drawBoard` highlighting
  the legal cells of the N×N square about center 112 for the current opening move:
  **moves 2/3/4/5 → 3×3 / 5×5 / 7×7 / 9×9** (radius 1/2/3/4). Applies during the placement phase and
  the **decline-and-place** action of a SWAP window. **Only single-stone placements (moves 2–5,
  incl. Branch-A move 5) are box-constrained** — do **not** draw a box for the Branch-B offer-10
  picker (§10.5).
- **Translucent "dead-stone" candidates** — render the 10 Branch-B offers (and, during SELECTION,
  the non-picked nine) as translucent black. **Reuse the existing primitive:** `drawStone:243-278`
  already applies `stonePaint.setAlpha(180)` for Go dead stones (`:264,:268`) — draw candidates with
  the same alpha (value 2 + alpha) rather than adding a new path.
- **10-pick multi-select + submit** — tap to add a candidate, tap again to remove, `n/10` counter,
  submit button (a new dialog/overlay; the `Gravity.BOTTOM` dialog chrome from
  `showSwap2Choice:973+` is the styling precedent). **Validation before send:** exactly **1** stone
  (and inside the 9×9) for Branch A, or exactly **10** distinct, non-D4-duplicate (§10.5) stones
  **anywhere on the board** for Branch B; alert otherwise. Branch is inferred from the count
  (1 = continue / 10 = offer), matching the `ServerTable`/`MoveServlet` contract.
- **White selection screen** — gate on the `dsgSystemMessageTableEvent` prompt (`:409`); show the
  ten offered candidates and let white tap one → send `dsgRenjuTaraguchi10Select1TableEvent`. The
  picked candidate renders solid (value 2), the rest translucent.

Visual reference (different framework — do not copy code): `gameServer/tb/mobileGame.jsp` and its
board JS — `drawDeadStone`, the central-square hinting by move number, the multi-pick picker.

### Could NOT confirm (carry into QA / verify before relying on)
- **Stone-color contradiction (resolved, confirm visually):** the *protocol* survey pass said
  `currentColor` is already black-first; the *board* pass + verified `BoardState.java:6` +
  `currentColor:287` say the first stone is **white** today. The §10.0/§10.3 black-first fix
  (`2 - moves%2`) is required — confirm by rendering a live Renju move 1.
- **`swap=true` take-over `move` sentinel — RESOLVED: send `-1`.** `handleRenjuSwap` ignores `move`
  on `swap=true` (and on the bare window-5 decline); use `-1` (not `0`, a legal corner cell) — the
  house no-move convention (`ServerTable:1529`).
- **`renjuOpeningPlayer` need** — whether a Renju arm in `Table.currentPlayer` is strictly required
  for correct `isMyTurn` during the opening (mirror `swap2OpeningPlayer`; the safe move). **(verify)**
- **`TB_RENJU=81` resolution** — does the TB/offline path call `Variants.fromGameId(81)` (needs a
  canonical `81→31` mapping) or `Variants.fromGameType(gameName)` (string)? And **what string** does
  the server put in `GameResponse.gameName` / the live `gameNames` for Renju? **(verify)**
- **`GameResponse` JSON types (CONFIRMED — was a verify item):** matched against backend
  `GameResponse.java:45-47` — `renjuPhase` (`String`), `renjuOffers` (`String`, comma-separated), and
  `renjuSwaps` (`Integer`). The String/String/Integer POJO in §10.3 step 8 is correct; no further verify.
- **Server auto-center on the live socket** — the contract says the server auto-places move 1 (112);
  confirm Android receives it as an ordinary `dsgMoveTableEvent` (and `movesLength` includes it), so
  the client never places the center. **(verify)**
- **`dsgSystemMessageTableEvent` for the Branch-B selector** — confirm the server actually emits it
  to the Android selector and that it is sufficient to gate the picker (vs needing a dedicated signal). **(verify)**
- **Index parity end-to-end** — contract is `x + y·15` (x = low component, `move%15`). Android
  encodes `gridSize*row + col` = `x + y·gridSize` (`LiveBoardView:147`), consistent; confirm after
  the `addMove:199-200` `/gridSize` fix. (This closes the survey's open "col+row·15 vs row·15+col"
  question → it is `x + y·15`.) **(verify)**
- **`gameHasCaptures` (`Table.java:~900`)** — current `game != 5,6,13,14` would let Renju **detect**
  captures; confirm Renju (ids 31/32) is excluded (it has none). **(verify)**
- **Other hardcoded-19 assumptions** — undo, resignation, message formatting on either board view. **(verify)**
- **Arena mode** — `isArenaTable` Renju opening behavior is unspecified. **(verify)**
- **`Canvas` layer / z-order** — for the central-box highlight + translucent candidates relative to
  stones; drawing order not specified. **(verify)**
- **TB opening UI scope** — there is **no** turn-based opening UI on the offline `Game`/`BoardView`
  screen today (all opening UI is live-only). Confirm whether TB Renju opening is required now or
  deferred (this handoff recommends live-first; wire only the TB **read** side + `renjuAction`
  initially). **(verify)**

---

## 12. Sub-project 7 — Android TURN-BASED (correspondence) Taraguchi-10 handoff

Zero-context handoff for a fresh agent wiring the Renju (Taraguchi-10) opening into the **turn-based / correspondence** (days-per-move, over HTTP) path of the `pentelive-android` app — the complement to the **live** handoff (§10, raw-SSL-TCP socket). **Correcting the record:** Android plays **both** transports (§10 established this; the live path is §10.2a). §10.2b already *sketched* the TB read-side (add three `GameResponse` fields + the `renjuAction` param); **this section is the full TB build** — JSON parsing, board sizing, the `renjuAction` submission, and the on-board opening UI. The same record-correction applies to iOS: the original §9 verdict ("LIVE ONLY"; `BoardViewController` a "read-only" viewer) is **WRONG** — `BoardViewController` is iOS's *interactive* turn-based board (`BoardViewController.h` declares `boardTap:` (a `UILongPressGestureRecognizer`) + `submitMove:`; `BoardViewController.m` has `submitMove:` ~:1190, `submitMoveToServer` ~:1216, and builds `game?command=move…&gid=…&moves=…&message=` ~:1275-1295). So **both** mobile apps play **both** live and turn-based; the originals documented only the live opening path. **THE KEY TB DIFFERENCE FROM LIVE:** in turn-based the **server ships the derived phase** — the client **reads** `renjuPhase` from the `game.jsp` `GameResponse` and does **NO** client-side phase derivation (unlike §10.2a/§8.2, which derive it from echo events). Moves go via the `MoveServlet` **§2.4 contract** (`command=move&…&renjuAction=…`).

### 12.0 Board basics (TB-specific)
- Board **15×15**, game ids **31 (Renju) / 32 (Speed Renju) / 81 (turn-based Renju)**. The server ships `gameName="Renju"` for **both** ids 31 and 81 and `"Speed Renju"` for 32 (`GridStateFactory.java:135-137`); the TB-vs-live distinction is by **endpoint/screen, not `gameName`** (there is no `"TB Renju"` string). Move encoding `x + y·15`; **center = 112** (`7 + 7·15`); the **server auto-places** it as move 1 (it already sits in `GameResponse.moves`, so the client only renders it — never places the center itself).
- The TB/correspondence screen is **`BoardActivity` + `Game.java` + `BoardView.java`** (the *offline* board; distinct from the live `LiveGameRoomActivity`/`LiveTableFragment`/`LiveBoardView`). Transport is **HTTP**, not the socket: load = `GET gameServer/mobile/json/game.jsp`, submit = `GET gameServer/tb/game?command=move…`.
- Correspondence Renju games carry id **81**; the **same** `game.jsp` endpoint also serves *completed* live games (31/32) historically — those ship `renjuPhase=COMPLETE` plus the archived `renjuOffers`/`renjuSwaps` (§2.7/§7 archival persistence), so the read-side must tolerate all three ids.
- **Board background colour = `#D98880` (dusty rose)** — the canonical Renju colour, **distinct from gomoku's `#A3FDEB`** (`BoardView.java:38`). `BoardView` has no `renjuColor` constant today (`:37-43`).
- **Black plays first.** Android board values: `1 = white`, `2 = black`, `3 = translucent white`, `4 = translucent black` (both translucent paths `setAlpha(180)`; confirmed in `BoardView.drawStone` **:611-646** — `==2`→black `:623`, `==1`→white `:626`, `==4`→translucent **black** `:629-632`, `==3`→translucent **white** `:633-636`). So "black first" ⇒ the first stone must carry board **value 2**. Today the offline replay (`replayGomokuGame`/`replayPenteGame`) computes `color = 1 + (i % 2)` at `Game.java:1531` (Gomoku) / `:1540` (Pente) → move 0 = value 1 = **WHITE** (inverted for Renju). The fix is a black-first Renju replay `color = 2 - (i % 2)` (a `2 - size%2` form already appears at `Game.java:1290`).
- **Win:** black on **exactly five**, white on **five+** (display only; the server is authority). **Black forbidden points are server-enforced** — never port the finder; if marking is ever wanted, fetch `getForbiddenPoints`.

### 12.1 Confirmed anchors (file : symbol)
All grep-verified in the submodule on this branch (line numbers as-of-now; grep the symbol). **(WRONG today)** = breaks for / ignores Renju as-is; **(OK)** = correct / reusable; **(precedent)** = existing TB mechanism to mirror; **(verify)** = not fully confirmable from code; **(server contract)** = backend-side fact. `renju` appears **zero** times anywhere in the app (confirmed) — no Renju support today.

| Area | File | Symbol / fact |
|---|---|---|
| TB JSON model | `app/.../JsonModels.java` | `GameResponse` **:121-154** — 22 fields: `gid, privateGame, rated, gameName:125, moves:126, player1/2 (PlayerRef), messages, messageNums, sid (Long), currentPlayer:132, seqNums, dates, players, state:136, goState:137, undoRequested (Boolean), canHide, canUnHide, cancel (CancelInfo), dPenteState:142 (String), swap2pass:143 (Boolean)`. **No `renjuPhase`/`renjuOffers`/`renjuSwaps`.** **(WRONG today / missing)** |
| TB game load | `app/.../Game.java` | `RetrieveGame.doInBackground` **:418** — `GET game.jsp?gid=` (URL `:432`, dev `:435`) → `new Gson().fromJson(…, GameResponse.class)` **:464**; result stored in `mGameJson` (`:59`, setter `:204`). Does not read the renju fields (they don't exist). **(WRONG today)** |
| TB move submit (URL) | `Game.java` | `SubmitMoveTask.doInBackground` **:530-538** — builds `https://www.pente.org/gameServer/tb/game?command=move + hideStr + &mobile=&gid=…&moves=…&message=…&name2=…&password2=…`. **No `renjuAction` param** → server rejects opening actions. **(WRONG today / missing)** |
| TB move submit (entry) | `Game.java` | `submitMove(String move, String message)` **:915-918** → `new SubmitMoveTask(move,message).execute()`. The single TB submission entry point; overload it to carry `renjuAction`. **(OK pattern)** |
| TB move submit (alt) | `app/.../net/OkHttpPenteApi.java` | `submitMove(String gid, String moves, String message)` **:163-176** — `addQueryParameter("command","move")`, `"mobile"`, `"gid"`, `"moves"`, `"message"`, `"name2"`, `"password2"`; `.get()` request (`:101`). **No `renjuAction`.** (Two submit paths coexist — confirm which BoardActivity uses; see verify.) **(WRONG today / missing)** |
| board sizing | `Game.java` | `parseGame` **:950**; the `(9x9)→9 / (13x13)→13 / else 19` block (**:1000-1006**, then `boardView.gridSize = gridSize` at **:1007**) sits **INSIDE an outer Go-only `if`** (`:997-1010`, gated on `mGameType.equals("Go")/"Speed Go"/"Go (9x9)"/…`) → **unreachable for Renju**. No `"Renju"` case anywhere → Renju falls through to the default field `private int gridSize = 19` **:2477**. The fix must be a **separate sibling branch OUTSIDE the Go `if`** that sets **both `this.gridSize = 15` AND `boardView.gridSize = 15`** for Renju game types. **(WRONG today)** |
| move-index DECODE | `Game.java` | replay methods hardcode `move / 19`, `move % 19` at **13+ sites** (`:1380, 1532, 1541, 1571, 1593, 1619, 1639, 1661, 1680, 1695, 1704, 1737, 2316`). For Renju `112` decodes to `board[5][17]` (off-center). Encode is fine (see below); **decode is the game-breaking bug.** **(WRONG today)** |
| abstractBoard dims | `Game.java` | `abstractBoard` **:97+** — a literal **19×19** `byte[][]` initializer (19 zeros/row). The draw loop reads it `gridSize`-bounded, but replay populates it via the `/19` decode. **(WRONG today)** |
| replay + colour dispatch | `Game.java` | two `getGameType().equals(...)` chains: **:1320-1362** (`replayGameUntilMove`; `boardView.setBackgroundColor(<variantColor>)` + `replayXGame(untilMove)`) and **:1480-1503** (incremental single-move; `replayXGame(moveI,moveJ,…)`). Exact-string match per variant (`"Gomoku"`,`"Pente"`,`"D-Pente"`,…); also `Variants.fromGameType(getGameType())` + `ALLOWLIST` delegable path (`:1321-1322`). **No `"Renju"` arm in either.** **(WRONG today)** |
| board colours | `app/.../BoardView.java` | **:37-43** (full list) — `penteColor=#FDDEA3, keryoPenteColor=#BAFDA3, gomokuColor=#A3FDEB (:38), dPenteColor=#A3CDFD, gPenteColor=#AEA3FD, poofPenteColor=#EDA3FD, connect6Color=#EDA3FD, boatPenteColor=#25BAFF, dkeryoColor=#FFA500, goColor=#FAC832, oPenteColor=#52be80, swap2PenteColor=#E5AA70, swap2KeryoColor=#50C878` (plus `blackColor`/`whiteColor`). **No `renjuColor`.** **(WRONG today / missing)** |
| board grid size | `BoardView.java` | `public int gridSize = 19` **:52**; set from `Game.parseGame` (`:1007`). **(WRONG today until 15)** |
| move ENCODE (touch) | `BoardView.java` | `onTouchEvent` **:259-306** — `stoneJ = gridSize*stoneX/size` (`:259`), `stoneI = gridSize*stoneY/size` (`:261`), bounds-check `>= gridSize` (`:262`), `playedMove = gridSize*stoneI + stoneJ` (`:306`) = `x + y·gridSize`. **Correct once `gridSize=15`.** **(OK)** |
| board draw loop | `BoardView.java` | `drawBoard` loop **:490-492** — `for i<gridSize, j<gridSize: drawStone(board[i][j])`; `board = game.getState().board` (`:489`). `gridSize`-bounded. **(OK)** |
| star points | `BoardView.java` | `drawBoard` **:482-488** — non-Go `else` branch draws 4 corners at `margin + 6*step` + center (`margin/2` radius). Distance **6** is 19×19-specific; Renju 15×15 needs distance **3** → cols/rows `{3,7,11}`. (A Go branch at `:478-480` uses `3*step` but only 3 circles.) **(WRONG today)** |
| coordinate labels | `BoardView.java` | `onTouchEvent` label build **:364-406** — `coordinateLetters[m % 19]` + `19 - (m / 19)`; `coordinateLetters` (`:71`) = 19 letters `A–T` skipping I. Renju needs `% gridSize` and the **first 15: A–P skipping I**. **(WRONG today)** |
| stone colours + translucent | `BoardView.java` | `drawStone` **:611-646** — `stoneColor==2`→black (`:623`), `==1`→white (`:626`), `==4`→translucent **black** `setAlpha(180)` (`:629-632`), `==3`→translucent **white** `setAlpha(180)` (`:633-636`). **Value 2=black, 1=white, 3=translucent white, 4=translucent black** confirmed; **move 5 is black, so its offer/candidate previews use value 4** (translucent black), solidified to value 2 once chosen. The translucent paths are the **reusable preview primitive**. **(OK / reusable)** |
| **existing TB opening precedent** | `Game.java` | `dPenteChoice` field **:61**, `swap2Choice` **:81**; `parseGame` **:1021-1026** reads `mGameJson.dPenteState == "2"` → sets `dPenteChoice=true` (+ `swap2Choice` if `isSwap2()`); `parseGame` **:1178-1182** makes `R.id.swap2PassButton` `VISIBLE` when `mActive && isSwap2() && movesList.size()==3 && swap2Choice`. **This is the read-server-field → show-choice precedent to mirror for `renjuPhase`.** **(precedent — confirmed now)** |
| **existing TB opening UI wiring** | `app/.../BoardActivity.java` | `onCreate` **:65-127** wires `R.id.playAsWhite`/`playAsBlackButton` + `R.id.dPenteLayout` — `game.submitMove("0", …)` fires at **:96** (playAsWhite / swap2 branch) and **:116** (playAsBlack / non-swap2 branch); `:99/:110/:127` are `setVisibility(INVISIBLE)` layout-hide calls (**not** submits) — and `R.id.swap2PassButton` (`:121-127`); `setRegularSubmitListener` **:239** branches at **:282** (`isSwap2() && swap2Choice`) and **:300** (`isDPente() && dPenteChoice`) before `game.submitMove(moves, …)` **:326**. **Corrects §10.2b's "no opening UI" claim** — a yes/no opening-answer skeleton exists on TB; Renju reuses its shape (read phase → show control → `submitMove`). **(precedent — confirmed now)** |
| load entry | `BoardActivity.java` | `onCreate` **:79** calls `game.parseGame(board)` immediately → triggers `RetrieveGame`. **(OK)** |
| game list → Game | `app/.../PentePlayer.java` | `populateFromJson` **:174** — only `IndexResponse.activeGamesMyTurn` (`:257-269`) builds `mActiveGames` (`this.mActiveGames = newActive` **:269**); `activeGamesOpponentTurn` (`:273-285`) feeds a **separate** non-active list `mNonActiveGames` (`:285`), **not** `mActiveGames`. Both call `new Game(String.valueOf(entry.gid), null, entry.gameName, …)`. **No game-type filter** → Renju games already list. The `entry.gameName` string flows into `Game.mGameType`. **(OK)** |
| player colour hint | `Game.java` | `mMyColor` field **:53** (ctor `:124`, getter `:163`); used `:1281` `mMyColor.contains("white") ? 1 : 2` and `:1290` `2 - mMovesList.size()%2`. Server tells the client which side it plays. **(OK / context)** |
| renjuAction wire | backend `RenjuTbContract.java` + `MoveServlet.java` (reference, §2.4) | TB opening actions = a `renjuAction` query param alongside `command=move`, resolved by `RenjuTbContract.resolve`: **`swap` / `move` / `select`** (THREE total). `swap` = take-over (no `moves`, no stone); `move` carries **1 stone** (auto-declines a pending swap, then places it — windows 1–3 next stone / at the branch point Branch A move 5) or **10 stones** (Branch B — branch inferred by count); `select` carries **2 stones** (move 5 + move 6, atomic). MOVE/COMPLETE stones go as a plain `command=move` with **no `renjuAction`**. Server validates and throws `InvalidMoveException` → page shows **"Invalid move"**. **(server contract)** |
| GameResponse renju fields | backend `GameResponse.java:45-47` (reference) | `renjuPhase` (`String`, one of `SWAP|BRANCH|OFFERS|SELECTION|MOVE|COMPLETE`, else null), `renjuOffers` (`String`, comma-separated offered move indices, else null), `renjuSwaps` (`Integer`, packed opening word, else null). The Android POJO types must be `String/String/Integer`. **(server contract)** |

### 12.2 TB phase + transport (the server SHIPS `renjuPhase` — read it, do NOT derive)
Unlike the live path (§10.2a, which **derives** the phase from `dsgRenjuTaraguchi*` echoes because the socket carries **no** `renjuPhase`), the turn-based path is **read-only on phase**: `TBGame.getRenjuPhase()` reconstructs server-side (§2.6) and ships the result in `GameResponse.renjuPhase`. The Android TB client adds the three fields to `GameResponse` and **reads `renjuPhase` directly** — no `renjuState` tracking object, no `renjuPhase(...)` classifier, none of the §10.2a echo accounting. This is strictly simpler than live; the only "logic" is mapping the read phase to a control and the chosen action to a `renjuAction` payload.

`renjuPhase` → control → §2.4 submission (the client shows the control the phase names, then submits the matching `renjuAction` via `submitMove`):

| `renjuPhase` (read) | meaning | client control | `renjuAction` + `moves` (§2.4) |
|---|---|---|---|
| `SWAP` | swap window open (after moves 1–4) | "Swap (take over)" / "Don't swap" — declining places a stone (windows 1–3: next central-square stone; at move 4: the branch move itself) | take-over → `swap` (no `moves`, no stone) · decline+place → `move` / `<m>` (1 stone) · Branch B at move 4 → `move` / `<s1>,…,<s10>` (10 stones) |
| `MOVE` | place the next central-square opening stone (incl. Branch-A move 5), no decision pending (radius by move #) | constrained placement | **plain move** — `command=move&moves=<move>`, **no `renjuAction`** |
| `BRANCH` | move-4 swap was **taken**; brancher chooses A vs B (branch inferred by stone count) | Branch A (place move 5 ∈ 9×9) **or** Branch B (offer 10) | Branch A → `move` / `<m>` (1 stone) · Branch B → `move` / `<s1>,…,<s10>` (10 stones) |
| `SELECTION` | white commits move 5 (black) **and** plays move 6 (white) | tap one of `renjuOffers`, then place move 6 | `select` / `<m5>,<m6>` (2 stones, atomic) |
| `COMPLETE` | opening done | normal play | **plain move** (no `renjuAction`); black forbidden points server-enforced |

The client maps the read phase to exactly one of the **three actions** — `swap` (take-over, no stone), `move` (1 stone = decline+place / Branch A; 10 stones = Branch B; **branch inferred by count**), `select` (atomic move 5 + move 6) — or, in `MOVE`/`COMPLETE`, a plain `command=move` with **no `renjuAction`**. Declining a swap is implicit in sending a `move`; there is **no** separate decline, branch, or offer request. **Branch B and its ten offers are a single `move` (10 stones)** — so a TB client **never acts on a standalone `OFFERS` phase** even though the read enum still ships it (the server resolves OFFERS into the same `move`). `renjuOffers` (comma-separated indices) is the `SELECTION` render source. `renjuSwaps` (the §2.1 base-3 `RenjuOpeningState` word) the client does **not** decode — the server already shipped the resolved `renjuPhase`; `renjuSwaps` is only needed for archival/opening-replay rendering (deferred).

### 12.3 File-by-file map (the real work)
**(TB)** = turn-based/offline screen; **(both)** = shared `rules/` registry (already covered for live by §10.3 steps 1–2 — reuse, don't duplicate).

1. **`rules/.../pente/rules/Variant.java` + `Variants.java`** *(both)* — already specified in §10.3 steps 1–2: add `RENJU(31, 15, CaptureRule.NONE, 1)` + `isRenju()`; `fromGameId` `31/32/81→RENJU`; `fromGameType` `"Renju"`/`"Speed Renju"` → `RENJU` (**no "TB Renju" arm** — the server ships `gameName="Renju"` for **both** ids 31 and 81, `GridStateFactory.java:135-137`). The TB path leans on **`fromGameType(gameName)`** (the string in `GameResponse.gameName`), exercised at `Game.java:1321`.
2. **`app/.../JsonModels.java`** *(TB)* — add to `GameResponse` (`:121-154`): `public String renjuPhase; public String renjuOffers; public Integer renjuSwaps;` (matches backend `GameResponse.java:45-47`). Gson tolerates absent fields → backward-safe.
3. **`app/.../Game.java`** *(TB)* — the bulk of the work:
   - `parseGame`: add a **sibling Renju branch OUTSIDE the Go-only `if` (`:997-1010`)** — when `mGameType` is a Renju type, set **both `this.gridSize = 15` AND `boardView.gridSize = 15`**. (The existing `(9x9)/(13x13)/else 19` block at `:1000-1007` is unreachable for Renju, being gated behind the `Go`/`Speed Go` check — do **not** add the Renju case inside it.)
   - **Store the read phase:** after the Gson parse, capture `mGameJson.renjuPhase`/`renjuOffers`/`renjuSwaps` (parse `renjuOffers` to `int[]`) into `Game` instance fields, mirroring how `dPenteState` is consumed at `:1021-1026`. Add `isRenju()` mirroring `isDPente():945` (`Variants.fromGameType(getGameType())…isRenju()`).
   - **Replay + colour dispatch** `:1320-1362` **and** `:1480-1503`: add a `"Renju"`/`"Speed Renju"` arm in **both** → `boardView.setBackgroundColor(boardView.renjuColor)` + a new **black-first** `replayRenjuGame(…)`.
   - **`replayRenjuGame`** — a black-first clone of `replayGomokuGame`: `color = 2 - (i % 2)` (i=0 → value 2 = black) and `move / gridSize`, `move % gridSize` (**not** `/19`,`%19`).
   - **Fix the decode bug:** replace hardcoded `/19`,`%19` with `/gridSize`,`%gridSize` at the 13+ sites (`:1380,1532,1541,1571,1593,1619,1639,1661,1680,1695,1704,1737,2316`); allocate `abstractBoard` (`:97+`) from `gridSize` instead of the literal 19×19.
   - **`submitMove` + `SubmitMoveTask`** (`:915`, `:530-538`): overload to carry `renjuAction` and append `&renjuAction=<swap|move|select>` to the `tb/game` URL (omit it entirely for plain `MOVE`/`COMPLETE` moves).
4. **`app/.../net/OkHttpPenteApi.java`** *(TB)* — `submitMove:163-176`: overload `submitMove(gid, moves, message, renjuAction)` → `.addQueryParameter("renjuAction", action)`. **Resolve first** which submit path `BoardActivity` actually uses (`Game.SubmitMoveTask` vs `OkHttpPenteApi`) and wire `renjuAction` into that one (verify).
5. **`app/.../BoardView.java`** *(TB)* —
   - `:37-43`: add `renjuColor = Color.parseColor("#D98880")`.
   - `:482-488`: add a Renju star-point branch — distance-**3** corners + center (`{3,7,11}`, center `(7,7)`), not the hardcoded `6*step`.
   - `:364-406` + `coordinateLetters:71`: use `% gridSize` and the **first 15 letters A–P (skipping I)** instead of `% 19` / 19 letters.
   - Reuse `drawStone`'s translucent-black `stoneColor==4` `setAlpha(180)` path (`:629-632`) for the move-5 offer/selection candidates (move 5 is black); `stoneColor==3` (`:633-636`) is the translucent-**white** variant.
6. **`app/.../BoardActivity.java`** *(TB)* — the opening UI, mirroring the **existing dPente/swap2 skeleton** (`:65-127`, `setRegularSubmitListener:239-326`): read `Game.renjuPhase`, gate the matching control (§12.5), and submit via the new `renjuAction`-carrying `submitMove`. The yes/no swap windows reuse the `dPenteLayout`/`swap2PassButton` pattern almost verbatim; the branch/offer/selection board-interaction is new (§12.5).

### 12.4 Wire / URL examples (concrete `game.jsp` JSON + `renjuAction` requests)
**Read — `GET gameServer/mobile/json/game.jsp?gid=<id>&name2=<user>&password2=<pass>`** → Gson `GameResponse`. A Branch-B game awaiting white's selection (15×15, center 112) ships:
```json
{
  "gid": "12345", "gameName": "Renju", "moves": "112,113,114,115",
  "currentPlayer": "bob", "state": "...", "player1": {"name":"alice","rating":1500},
  "renjuPhase": "SELECTION",
  "renjuOffers": "113,114,115,116,128,129,130,131,144,145",
  "renjuSwaps": 5
}
```
A normal mid-opening read (white's swap window after black's move 2): `"renjuPhase": "SWAP"`, `"renjuOffers": null`, `"renjuSwaps": <packed>`. A finished/historic game: `"renjuPhase": "COMPLETE"` (with archived offers/swaps for replay).

**Submit — `GET gameServer/tb/game?command=move&mobile=&gid=<id>&moves=<payload>&renjuAction=<action>&message=…&name2=…&password2=…`** (the §12.3 step 3/4 builders add `&renjuAction=`):

| intent | `moves` | `renjuAction` | resulting query tail |
|---|---|---|---|
| take over opponent's side (seats swap, no stone) | _(none)_ | `swap` | `…&command=move&renjuAction=swap` |
| decline window 1–3 + place next opening stone (e.g. move 2 @113) | `113` | `move` | `…&moves=113&renjuAction=move` |
| Branch A at the branch point: place move 5 @130 (1 stone ⇒ A) | `130` | `move` | `…&moves=130&renjuAction=move` |
| Branch B at the branch point: the ten 5th-move offers (10 stones ⇒ B) | `113,114,115,116,128,129,130,131,144,145` | `move` | `…&moves=113,…,145&renjuAction=move` |
| white commits move 5 @130 + plays move 6 @131 (atomic) | `130,131` | `select` | `…&moves=130,131&renjuAction=select` |
| plain opening/normal stone in `MOVE`/`COMPLETE` (e.g. move 6 @131) | `131` | _(none)_ | `…&command=move&moves=131` |

The server validates authoritatively (central squares, forbidden points, offer distinctness/D4-symmetry) and throws `InvalidMoveException` → the page shows **"Invalid move"**; client-side checks are UX only. The ten Branch-B offers are **NOT** box-constrained (any in-bounds, empty, non-D4-symmetric point — corners legal); only the **Branch-A move 5** is restricted to the 9×9 (§10.5 has the client-side D4 dedup algorithm if you want instant feedback).

### 12.5 Opening UI on the TB board (driven by the read `renjuPhase`)
**There IS a precedent** (correcting §10.2b): the TB screen already reads an opening-state field (`dPenteState`, `Game.java:1021`) and shows yes/no opening controls (`R.id.dPenteLayout` two-button choice + `R.id.swap2PassButton`, `BoardActivity:96-127`), submitting via `game.submitMove(...)`. Renju reuses that exact shape — read `renjuPhase` → show control → `submitMove` with `renjuAction`. The **swap windows are essentially the existing pattern**; only the branch/offer/selection board-interaction is genuinely new (no multi-select / zone-highlight precedent on this screen).

- **Central-box placement highlight** (new `Canvas` layer in `BoardView.drawBoard`) — highlight only the legal cells of the N×N square about center 112 for the current opening move: **moves 2/3/4/5 → 3×3 / 5×5 / 7×7 / 9×9** (radius 1/2/3/4). Applies during `MOVE` placement **and** the decline-and-place action of a `SWAP` window (the bundled stone is constrained to the same square). **Only single-stone placements (moves 2–5, incl. Branch-A move 5) are box-constrained** — do **not** box the Branch-B offer picker.
- **Swap prompt** (`SWAP` phase) — mirror `dPenteLayout`/`swap2PassButton` (`AlertDialog`/bottom-gravity buttons): "Swap (take over)" → submit `swap` with **no `moves`** (seats swap, no stone); "Don't swap" → in windows 1–3, tap the highlighted square to place the bundled stone → `move`/`<m>` (1 stone; the decline is implicit). At the move-4 branch point a decline **is** the branch move itself — Branch A places move 5 → `move`/`<m>` (1 stone), Branch B offers ten → `move`/`<s1>,…,<s10>` (10 stones).
- **Branch choice** (`BRANCH` phase, after a take-over) — a two-option control; branch is **inferred from the stone count**, so each option is a single `move`: "Continue (place 5th move)" → place move 5 in the 9×9 → submit `move`/`<m>` (1 stone ⇒ Branch A); "Offer 10" → enter the offer picker, then submit `move`/`<s1>,…,<s10>` (10 stones ⇒ Branch B). No separate `branch` request, and Branch B + its offers are one `move` (the client never acts on a standalone `OFFERS` phase).
- **10-pick multi-select** (Branch-B offer picker, reached from the "Offer 10" choice above) — tap to add a candidate (move 5 is **black**, so render it translucent via `drawStone` **value 4** / `setAlpha(180)`), tap again to remove, `n/10` counter, submit button (exactly 10). Whole board allowed (minus occupied + D4-duplicate). Submit `move`/`<s1>,…,<s10>` (10 stones ⇒ Branch B).
- **White selection** (`SELECTION` phase) — render the `renjuOffers` indices as translucent-black candidates (**value 4**); white taps one to commit as **move 5** (solidify to value **2**, black), then places **move 6** (white) → submit `select`/`<m5>,<m6>` (atomic 2-stone: the server stores neither unless both are legal). The non-picked candidates stay translucent.

### Could NOT confirm (carry into QA / verify before relying on)
- **Exact `gameName` string for Renju ids 31/32/81** — **RESOLVED:** the server ships `gameName="Renju"` for **both** 31 and 81 and `"Speed Renju"` for 32 (`GridStateFactory.java:135-137`). `parseGame` board-sizing and the replay/colour dispatch (`:1320-1362`, `:1480-1503`) match those exact `mGameType` strings; there is **no "TB Renju"** string. **(confirmed)**
- **Which submit path is canonical** — both `Game.SubmitMoveTask` (`:530-538`) and `OkHttpPenteApi.submitMove` (`:163-176`) build a `tb/game?command=move` request; `BoardActivity:326` calls `game.submitMove(...)` (→ `SubmitMoveTask`). Confirm which actually fires in production and wire `renjuAction` there (and whether the other is dead). **(verify)**
- **`MOVE`-phase submission** — **RESOLVED:** a `renjuPhase=MOVE` (or `COMPLETE`) opening stone is a **plain `command=move&moves=<move>` with NO `renjuAction`** — the `matchesPending` guard (`MoveServlet.java:430-435`) only accepts a `renjuAction` while a swap/branch/offer/selection is pending, so an unwrapped `MOVE` is the correct form. Branch-A move 5 is likewise a plain `MOVE`. **(confirmed)**
- **`renjuSwaps` packed format** — it is the §2.1 base-3 `RenjuOpeningState` word (`Integer`); the TB client need not decode it (the server ships `renjuPhase`). Confirm it is only needed for archival opening-replay rendering (deferred). **(verify / informational)**
- **Black-first colour fix** — confirm the offline replay is white-first today (`Game.java:1531` (Gomoku) / `:1540` (Pente) `1 + (i%2)`; `:1532` is the `move/19` **decode** line, not the colour) and that the new `replayRenjuGame` (`2 - (i%2)`) renders move 1 (center 112) as **black** after the fix. **(verify visually)**
- **Coordinate-label set for 15×15** — first 15 of the existing `coordinateLetters` (`A–P` skipping I) vs a Renju-specific array; current code is 19 letters `% 19` (`:364-406`). **(verify)**
- **Star-point layout** — `{3,7,11}` distance-3 corners + center for 15×15 (vs the hardcoded `6*step` at `:483-487`). **(verify)**
- **TB opening-UI scope on the offline screen** — the dPente/swap2 precedent (`:65-127`) is yes/no buttons only; the Renju branch/offer/selection board-interaction is from-scratch. Confirm whether the full TB opening UI is in scope now or staged after the read-side + `renjuAction` (this handoff recommends: ship parsing + `renjuAction` + the swap windows first, then the multi-select/selection). **(verify scope)**
- **iOS TB complement** — the corrected §9 verdict (`BoardViewController` is iOS's interactive TB board: `boardTap:`/`submitMove:` in `.h`; `submitMove:`~:1190, `submitMoveToServer`~:1216, `game?command=move…&moves=…`~:1275-1295) means iOS needs the same TB build (read `renjuPhase`, send `renjuAction`); not grep-re-verified here. **(verify against the iOS submodule)**
