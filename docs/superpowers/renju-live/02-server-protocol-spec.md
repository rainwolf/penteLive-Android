# Renju (Taraguchi-10) LIVE-Play Server Wire Protocol — Authoritative Spec

Extracted from server PR (`/tmp/server_pr8.diff`) + the merged server source at
`…/pente.org-project/pente.org/dsg_src/java`. Covers **LIVE socket play only**
(the "TableEvent" path). Turn-based (JSP/HTTP, `mobileGame.jsp`, `RenjuTbContract`,
`GameResponse.renjuPhase/renjuOffers/renjuSwaps`) is out of scope except where the
shared engine `RenjuState` is referenced.

---

## 0. Game-id constants (confirmed)

`GridStateFactory.java`:

| Constant | Value | Meaning |
|---|---|---|
| `RENJU` | **31** | Live Renju (real-time socket) |
| `SPEED_RENJU` | **32** (`RENJU + 1`) | Live **speed** Renju |
| `TB_START` | 50 | turn-based offset |
| `TB_RENJU` | **81** (`TB_START + RENJU` = 50+31) | Correspondence / turn-based Renju |

`createGridState(31|32|81)` all `return new RenjuState(15, 15)` — Renju is **always
played on the canonical 15×15 board**, ignoring the requested size (like GO9/GO13).
`isLiveGame(...)` covers `PENTE..SPEED_RENJU`, so 31 and 32 are live; 81 is TB.
No separate "variant flag" — the game-id alone selects Renju and its `RenjuState`.

**Board geometry:** 15×15 = 225 points. A move is a single packed int index.
Centre = **112** = (col 7, row 7) since `centerX()=centerY()=15/2=7`.
Out-of-range/sentinel index = `-1` (the "no stone" / batch-reject value).

---

## 1. The three LIVE-play event classes

All three extend `AbstractDSGTableEvent` (which extends `AbstractDSGEvent`), so every
one inherits two base fields from `AbstractDSGTableEvent`:

```java
private String player;   // actor login name (null when server→client broadcast/echo)
private int table;       // table number
```

They are **plain data carriers** — no custom binary `write`/`parse`. The wire format is
produced entirely by `DSGEventWrapper` + Gson (see §1.4). The `toString()` overrides are
**logging only**, NOT the wire format.

### 1.1 `DSGRenjuTaraguchiSwapTableEvent` — cmd `renjuSwap`

File: `dsg_src/java/org/pente/gameServer/event/DSGRenjuTaraguchiSwapTableEvent.java`

Fields (declaration order): `boolean swap`, `int move` (+ inherited `player`, `table`).

```java
public DSGRenjuTaraguchiSwapTableEvent(String player, int table, boolean swap, int move)
public boolean isSwap();  public void setSwap(boolean);
public int     getMove(); public void setMove(int);
public String toString(){ return "Renju Taraguchi swap="+swap+" move="+move+" "+super.toString(); }
```

Semantics (a Taraguchi-10 swap window, after moves 1–4 and the move-5 window):
- `swap=true`  → take the other side; **no stone placed** (`move` ignored — send `-1`).
- `swap=false` → decline; the bundled `move` is the next opening stone:
  moves 2–4 in their central box, or **move 5 in the 9×9 = Branch A**. At the move-5
  window a decline carries **no** stone (`move=-1`; move 5 already on board).

### 1.2 `DSGRenjuTaraguchiOffer10TableEvent` — cmd `renjuOffer10`

File: `…/event/DSGRenjuTaraguchiOffer10TableEvent.java`

Fields: `int[] moves` (+ inherited `player`, `table`).

```java
public DSGRenjuTaraguchiOffer10TableEvent(String player, int table, int[] moves)
public int[] getMoves(); public void setMoves(int[]);
public String toString(){ return "Renju Taraguchi offer10 "+(moves==null?0:moves.length)+" moves "+super.toString(); }
```

Semantics — **Branch B**: black's ten 5th-move candidates. Implies "declined the
move-4 swap + chose Branch B". Exactly **10** entries required. Also re-sent with
`player==null` to a client that (re)joins while selection is still pending.

### 1.3 `DSGRenjuTaraguchi10Select1TableEvent` — cmd `renjuSelect1`

File: `…/event/DSGRenjuTaraguchi10Select1TableEvent.java`

Fields: `int move` (+ inherited `player`, `table`).

```java
public DSGRenjuTaraguchi10Select1TableEvent(String player, int table, int move)
public int getMove(); public void setMove(int);
public String toString(){ return "Renju Taraguchi select1 move="+move+" "+super.toString(); }
```

Semantics: the *other* player (white) picks one of the ten offered candidates as move 5.
`move` must be one of the ten offered indices.

### 1.4 Wire serialization (the actual bytes on the socket)

There is **no per-class encoder**. Both transports wrap the event in a single fat
`DSGEventWrapper` (`…/event/DSGEventWrapper.java`) that declares **one private field per
event type**, named by the camelCase class name. The three Renju fields were registered:

```java
private DSGRenjuTaraguchiSwapTableEvent     dsgRenjuTaraguchiSwapTableEvent;
private DSGRenjuTaraguchiOffer10TableEvent  dsgRenjuTaraguchiOffer10TableEvent;
private DSGRenjuTaraguchi10Select1TableEvent dsgRenjuTaraguchi10Select1TableEvent;
```

Plus matching getter/setter pairs. Encoding/decoding is pure reflection + Gson:
- **Encode:** `new DSGEventWrapper(event)` sets the one matching field (by exact class
  name); `getJSON()` → `gson.toJson(this)`. Gson default **omits null fields**, so the
  JSON has exactly **one top-level key** = the wrapper field name, whose value is the
  event object serialized by its own fields. **The field name is the "type key"; it is
  the only routing token on the wire.**
- **Decode:** `gson.fromJson(jsonStr, DSGEventWrapper.class)`; `getEncodedEvent()` returns
  the first non-null field.

This single registration serves **BOTH** front-ends (shared codec):
- **TCP socket** (`SocketDSGEventHandler`): writes `getJSON()` UTF-8 bytes followed by a
  single delimiter byte **`255` (0xFF)**; the reader accumulates bytes until `0xFF`, then
  `fromJson`. Frame = `<utf8 json bytes><0xFF>`.
- **WebSocket** (`WebSocketDSGEventHandler`): `session.getBasicRemote().sendText(getJSON())`
  — one text frame per event.

**Concrete on-the-wire JSON examples** (keys = field names; order irrelevant since it is a
JSON object; Gson emits the subclass fields plus inherited `player`/`table`):

```jsonc
// renjuSwap — decline + bundled move 5 (Branch A), from client "alice" at table 7
{"dsgRenjuTaraguchiSwapTableEvent":{"swap":false,"move":112,"player":"alice","table":7}}

// renjuSwap — take-over (no stone)
{"dsgRenjuTaraguchiSwapTableEvent":{"swap":true,"move":-1,"player":"alice","table":7}}

// renjuOffer10 — Branch B, ten candidate indices
{"dsgRenjuTaraguchiOffer10TableEvent":{"moves":[98,99,100,113,114,115,128,129,130,131],"player":"alice","table":7}}

// renjuSelect1 — white picks one of the ten as move 5
{"dsgRenjuTaraguchi10Select1TableEvent":{"move":114,"player":"bob","table":7}}
```

**Server→client echo/rejoin frames are identical** but with `"player":null` (Gson with
default settings omits the null `player` key entirely; `table` and the payload remain).

### 1.5 Dispatch registration

`SynchronizedServerTable.callServerTable(...)` has three `case` arms (Java 21 pattern
switch, alongside the existing `DSGSwap2PassTableEvent` arm) routing to three
`ServerTable` handlers:

```java
case DSGRenjuTaraguchiSwapTableEvent e     -> serverTable.handleRenjuSwap(e);
case DSGRenjuTaraguchiOffer10TableEvent e  -> serverTable.handleRenjuOffer10(e);
case DSGRenjuTaraguchi10Select1TableEvent e-> serverTable.handleRenjuSelect1(e);
```

All table events are **serialized** by `SynchronizedServerTable`, so each handler runs
atomically with no interleaving — the basis for the atomic branch+offer commit.

---

## 2. `RenjuState` — shared opening engine

File: `dsg_src/java/org/pente/game/RenjuState.java`
`extends GridStateDecorator implements GomokuState, HashCalculator`. Wraps a
`SimpleGomokuState(15,15)` with `allowOverlines(true)` + `setDoHashes(false)`, plus a
`RenjuForbiddenPointFinder(15)` for black forbidden-point detection.

### 2.1 Internal opening state

```java
private boolean openingComplete;            // latched true at numMoves==6
private boolean awaitingSwap;               // a swap window is open
private boolean branchChosen, tenOffer;     // post-move-4 branch flags
private final boolean[] swapDecision = new boolean[6]; // [k] = did-swap after k stones
private final boolean[] swapResolved = new boolean[6]; // [k] = window k decided?
private final List<Integer> offeredFifth;   // Branch-B candidates
private Integer selectedFifth;              // Branch-B chosen move 5
```

`addMove(move)` drives the gating: after stones 1–4 → `awaitingSwap=true`; after stone 5
**iff `!tenOffer`** (Branch A) → `awaitingSwap=true` (the move-5 swap window); at stone 6
→ `openingComplete=true`.

### 2.2 `getOpeningPhase() : RenjuOpeningPhase`

Enum `RenjuOpeningPhase = { SWAP, BRANCH, SELECTION, MOVE, COMPLETE }`. Priority order:

```
openingComplete            -> COMPLETE
isAwaitingSwapDecision()   -> SWAP        (awaitingSwap)
isAwaitingBranchChoice()   -> BRANCH      (!complete && !awaitingSwap && numMoves==4 && !branchChosen)
isAwaitingFifthSelection() -> SELECTION   (branchChosen && tenOffer && numMoves==4 && offers==10 && selected==null)
otherwise                  -> MOVE
```
(`isAwaitingFifthOffers()` — branchChosen && tenOffer && numMoves==4 && offers<10 — is an
internal transient; never observed across the wire, see §3.4.)

### 2.3 `getCurrentPlayer() : int` (1=black, 2=white)

```
openingComplete              -> super.getCurrentPlayer()        (normal alternation)
awaitingSwap                 -> opponent of last mover = 3 - ((n-1)%2 + 1)
branchChosen&&tenOffer&&n==4 -> offers<10 ? 1 (black offering) : 2 (white selecting)
n==4 && !branchChosen        -> 1   (black chooses branch / would play move 5)
otherwise                    -> n%2 + 1
```
While a swap window is open the **opponent of the last mover** is to move (they may swap or
decline). Board moves are blocked while `awaitingSwap` (`isValidMove` returns false).

### 2.4 Opening hooks (the state-machine mutators)

| Method | Guard | Effect |
|---|---|---|
| `renjuSwapDecisionMade(boolean swap)` | `awaitingSwap` | record `swapDecision[n]=swap`, `swapResolved[n]=true`, clear `awaitingSwap` |
| `chooseBranch(boolean tenOffer)` | `isAwaitingBranchChoice()` | set `tenOffer`, `branchChosen=true` (false=Branch A, true=Branch B) |
| `offerFifthMove(int)` | `isAwaitingFifthOffers()` | validate (empty, in-bounds, not symmetric-dup) and append one offer |
| `offerFifthMoves(int[10])` | `isAwaitingFifthOffers()` | **atomic**: exactly 10; validate-all-or-rollback (restores prior list on any reject) |
| `wouldAcceptFifthOffers(int[10])` | — | **pure** pre-check of the same rules, no mutation |
| `selectFifthMove(int)` | `isAwaitingFifthSelection()` | must be one of the ten; sets `selectedFifth` then `addMove(move)` (commits as move 5, discards other nine) |
| `wouldAcceptDeclinedOpeningMove(int)` | swap or branch pending | pure check that the bundled stone would be a legal Branch-A continuation |

`getRenjuSwapsPacked()` encodes resolved windows into the `RenjuOpeningState` base-3 word
(pending windows = 0), used for rejoin/archival.

### 2.5 Move legality (`isValidMove`, `withinOpeningSquare`)

Central-square restriction by stones-already-placed `n` (Manhattan box around centre 7,7):

| Move # | n | Allowed region |
|---|---|---|
| 1 | 0 | centre only (`dx==0 && dy==0`) = index **112** |
| 2 | 1 | 3×3 (`|d|<=1`) |
| 3 | 2 | 5×5 (`|d|<=2`) |
| 4 | 3 | 7×7 (`|d|<=3`) |
| 5 (Branch A) | 4 | 9×9 (`|d|<=4`) |
| 6+ | ≥5 | anywhere |

`isValidMove` also rejects: out-of-bounds, not-your-turn, occupied point, any move while
`awaitingSwap`, a move at n==4 before the branch is chosen, and direct moves at n==4 when
Branch B (offers/selection use dedicated hooks). Post-opening: any empty in-bounds point is
legal — a black forbidden point is **allowed** but is an immediate loss (`isGameOver()`/
`getWinner()` return white). White wins on overline (6+), black loses on forbidden
(double-three / double-four / overline).

### 2.6 Symmetry / legality of the ten offers

- `positionStabilizer()` → the subset of the 8 D4 rotation/reflection indices (0..7) whose
  operation leaves the **current placed stones** invariant.
- `rotateMove(move, rot)` (inherited from the grid) maps a point under symmetry `rot`.
- `isSymmetricDuplicate(move)` → true if some stabilizer symmetry maps the candidate onto an
  already-offered point. Offers must be distinct **up to the board's current symmetry**, so
  symmetric mirror images count as duplicates and are rejected.

### 2.7 `reconstruct(MoveData moves, int renjuSwapsPacked, int[] offers)`

Static rebuild for persisted games: replays moves **and** re-applies swap/branch/offer/
select decisions in protocol order (decoding `RenjuOpeningState`), stopping at whatever is
still PENDING, so the rehydrated state reports the correct pending decision + current
player. (Plain `getInstance(moveData)` only replays moves — loses opening decisions.)

---

## 3. Server validation, echo, and rejoin (`ServerTable`)

`ServerTable.java`. Stone placement **always** rides the single existing
`DSGMoveTableEvent` broadcast (via `handleMove`, or the reproduced `broadcastRenjuFifthMove`
tail). The Renju events themselves are **decision-only echoes**. `broadcastMainRoom(...)`
routes to every player in the main room **including the sender** (the echo is authoritative
and reflexive); errors go to the sender only.

### 3.1 `handleRenjuSwap(DSGRenjuTaraguchiSwapTableEvent)`

Validates in order (each failure → `DSGMoveTableErrorEvent(actor, table, move, error)` to
sender, **no mutation, no broadcast**):
`NOT_IN_TABLE` → not RenjuState (`UNKNOWN`) → `NOT_SITTING` → `NO_GAME_IN_PROGRESS` →
not at a swap/branch window (`INVALID_MOVE`) → `gridState.getCurrentPlayer()!=seat`
(`NOT_TURN`) → `swap=true` while not at an open swap window (`INVALID_MOVE`).

On success:
- **`swap=true`** (open window only): swap both seat arrays (`playingPlayers[1]↔[2]`,
  `sittingPlayers[1]↔[2]`) exactly like `handleSwap`, adjust timers, call
  `renjuSwapDecisionMade(true)`, then broadcast a **non-silent** `DSGSwapSeatsTableEvent
  (actor, table, true, false)` so every client applies the visual seat swap via its existing
  swap-seats handler. (The renju swap event itself is reserved for declines + Branch-A move5.)
- **`swap=false` at an open window, n≤4** (bundled-stone): pre-validate the stone with the
  pure `wouldAcceptDeclinedOpeningMove(move)`; on fail `INVALID_MOVE` (clean reject); on pass
  `renjuSwapDecisionMade(false)`, and if `n==4` also `chooseBranch(false)` (move-4 decline =
  **Branch A**); then `broadcastMainRoom(swapEvent)` (decision echo) and `handleMove(actor,
  move)` (the stone arrives as the normal `DSGMoveTableEvent`).
- **`swap=false` at the move-5 window (n==5)**: white declines swap5 and keeps playing move 6
  itself — no bundled stone, no turn handoff. `renjuSwapDecisionMade(false)`,
  `broadcastMainRoom(swapEvent)`, white's clock reset/continued. Move 6 arrives later as a
  normal `DSGMoveTableEvent`.
- **`swap=false` in the post-take-over branch-choice state (n==4, window already accepted)**:
  black declines Branch B → Branch A by playing move 5 in the 9×9. Do **not** call
  `renjuSwapDecisionMade`, only `chooseBranch(false)` (after the pure pre-check); echo + place
  via `handleMove`.

### 3.2 `handleRenjuOffer10(DSGRenjuTaraguchiOffer10TableEvent)`

`atMove4 = !openingComplete && numMoves==4 && (awaitingSwapDecision || awaitingBranchChoice ||
awaitingFifthOffers)`. Validates: `NOT_IN_TABLE`/`UNKNOWN`/`NOT_SITTING`/`NO_GAME_IN_PROGRESS`
→ `!atMove4` (`INVALID_MOVE`) → `NOT_TURN` → `moves==null || length!=10` (`INVALID_MOVE`).

Success path (atomic): pure `wouldAcceptFifthOffers(moves)` first (reject before mutating, so
a bad batch never strands the player in Branch B with zero offers); then inside one handler:
`renjuSwapDecisionMade(false)` (if a swap window was open) → `chooseBranch(true)` (Branch B) →
`offerFifthMoves(moves)` (atomic; any throw → `INVALID_MOVE`). On success pass turn+timer to
the selector (white), `broadcastMainRoom(offerEvent)` (echo the ten to everyone), and route a
`DSGSystemMessageTableEvent` prompt to white only. **Batch rejects report `move = -1`** in the
error event (no single offending move).

### 3.3 `handleRenjuSelect1(DSGRenjuTaraguchi10Select1TableEvent)`

Validates: `…` → `!isAwaitingFifthSelection()` (`INVALID_MOVE`) → `NOT_TURN` →
`!offeredFifth.contains(move)` (`INVALID_MOVE`). Success: capture `oldCurrentPlayer`,
`rs.selectFifthMove(move)` (engine `addMove`s the chosen candidate as move 5; opening
completes when move 6 lands), `broadcastMainRoom(selectEvent)` (decision echo — clears the
other nine on clients), then `broadcastRenjuFifthMove(actor, move, oldCurrentPlayer)` — the
reproduced `handleMove` tail: timer/`moveTimes` accounting (Branch B is the *same-player*
case: white selects move 5 **and** plays move 6), undo/cancel reset, the single
`DSGMoveTableEvent` placement broadcast, and game-over check.

### 3.4 (Re)join — what a reconnecting client receives

The join block (`ServerTable` ~538-580) sends `sendPlayingPlayers` (authoritative seats),
then **exactly one** Renju phase signal derived by `RenjuRejoin.encode(rs)`, then
`sendMoves` (full `DSGMoveTableEvent` board snapshot) and timers. The client reconstructs the
opening phase from `(numMoves, signal)` via `RenjuRejoin.decode`.

`RenjuRejoin` (`dsg_src/java/org/pente/game/RenjuRejoin.java`) — pure encode/decode, unit-
testable without networking. Signal kinds (`RejoinKind`):

| Phase (server) | Rejoin signal sent | Wire event to the joiner |
|---|---|---|
| `SWAP` / `COMPLETE` | `NONE` | nothing (window-open vs normal play told apart by numMoves on decode) |
| `BRANCH` | `SILENT_SWAP` (swapValue = window-4 decision) | `DSGSwapSeatsTableEvent(null, table, swapValue, **silent=true**)` |
| `MOVE` (a swap window resolved) | `SILENT_SWAP` | same silent swap-seats event |
| `MOVE`, n==5, Branch B (move 5 chosen) | `SELECT1` (carries the point) | `DSGRenjuTaraguchi10Select1TableEvent(null, table, move)` |
| `SELECTION` (Branch B, offers in, white to pick) | `OFFERS` | `DSGRenjuTaraguchiOffer10TableEvent(null, table, int[10])` (re-sent via `sendRenjuBranchBOffers`) |

`decode(numMoves, signal)`:
```
NONE        -> numMoves<=5 ? SWAP : COMPLETE
SILENT_SWAP -> numMoves==4 ? BRANCH : MOVE
OFFERS      -> SELECTION
SELECT1     -> MOVE
```
`(numMoves, signal)` is **injective** over every rejoin-reachable state, so
`decode(numMoves, encode(state)) == state.getOpeningPhase()`. The silent swap-seats event is
suppressed when no window has resolved (`isSwapResolvedAt(numMoves)` false, e.g. numMoves==0).

**Important:** the `SILENT_SWAP` swap bit is a per-window **phase marker only**, NOT net seat
orientation — the client must read who-owns-black from `sendPlayingPlayers`, never derive it
from this bit. Two intra-handler transient n==4 states (Branch chosen but move 5 / offers not
yet committed) report `MOVE` yet encode to `SILENT_SWAP→BRANCH`; they are intentionally
**out of contract** and never network-observable because `handleRenjuSwap` and
`handleRenjuOffer10` commit the branch + its follow-up atomically in one serialized handler.

### 3.5 Move 1 (auto-centre)

There is **no server-side auto-placement** in `startGame()`; move 1 = centre (**112**) is a
**client convention** (the client auto-sends it). The server enforces it via
`withinOpeningSquare(n==0)` (centre only). Hence `numMoves==0` is never a live rejoin state
(noted in the rejoin block comment).

---

## 4. `RenjuOpeningState` packed word (rejoin/archival codec)

File: `dsg_src/java/org/pente/game/RenjuOpeningState.java`. Six base-3 digits in one int
(0..728, fits unsigned smallint): `encode = swap1 + swap2*3 + swap3*9 + swap4*27 +
branch*81 + swap5*243`. Each digit: `0=PENDING, 1=NO (swap declined / Branch A), 2=YES
(swap taken / Branch B)`. The ten Branch-B offers pack as one unsigned byte each
(`encodeOffers`/`decodeOffers`, position 0..224). This is the `renjuSwaps` value surfaced to
TB/historic JSON (`GameResponse.renjuSwaps`); live clients do **not** decode it (they use the
per-phase rejoin signal above).

---

## 5. Full Taraguchi-10 LIVE move sequence (summary)

```
move 1  black @ centre (112)            [client auto, server enforces centre]
  -> SWAP window 1  (white may take-over or decline)
move 2  white in 3×3
  -> SWAP window 2  (black)
move 3  black in 5×5
  -> SWAP window 3  (white)
move 4  white in 7×7
  -> SWAP window 4  (black) ── resolves into BRANCH choice:
       • decline + play move 5 in 9×9  => Branch A   (renjuSwap swap=false + move)
       • take-over (swap=true) then later move 5     => Branch A after seat swap
       • offer ten 5th moves           => Branch B   (renjuOffer10, 10 moves)
Branch A:
  move 5  black in 9×9
    -> SWAP window 5 (white): take-over OR decline (renjuSwap swap=false, move=-1)
  move 6  white anywhere  => opening COMPLETE, normal play
Branch B:
  white SELECTs one of the ten (renjuSelect1) => that becomes move 5 (black),
  the other nine are discarded; white then plays move 6 => COMPLETE
```

- **Branch A** = the 9×9 continuation: black plays move 5 itself in the 9×9 box, then a
  final move-5 swap window, then white plays move 6.
- **Branch B** = the ten-offer continuation: black offers 10 legal, symmetry-distinct 5th
  moves; white picks one (it becomes black's move 5), then white plays move 6. No move-5 swap
  window exists in Branch B.

---

## 6. Source file index

| File | Role |
|---|---|
| `…/gameServer/event/DSGRenjuTaraguchiSwapTableEvent.java` | `renjuSwap` carrier (`boolean swap`,`int move`) |
| `…/gameServer/event/DSGRenjuTaraguchiOffer10TableEvent.java` | `renjuOffer10` carrier (`int[] moves`) |
| `…/gameServer/event/DSGRenjuTaraguchi10Select1TableEvent.java` | `renjuSelect1` carrier (`int move`) |
| `…/gameServer/event/DSGEventWrapper.java` | 3 fields + getters/setters; Gson codec (wire format) |
| `…/gameServer/event/SocketDSGEventHandler.java` | TCP transport: JSON + 0xFF delimiter |
| `…/gameServer/event/WebSocketDSGEventHandler.java` | WS transport: sendText(JSON) |
| `…/gameServer/server/SynchronizedServerTable.java` | 3 dispatch case arms (serialized) |
| `…/gameServer/server/ServerTable.java` | `handleRenjuSwap/Offer10/Select1`, rejoin block (~538), `broadcastRenjuFifthMove`, `sendRenjuBranchBOffers` |
| `…/game/RenjuState.java` | opening engine (phases, hooks, symmetry, reconstruct) |
| `…/game/RenjuOpeningPhase.java` | enum SWAP/BRANCH/SELECTION/MOVE/COMPLETE |
| `…/game/RenjuOpeningState.java` | base-3 packed-word + offer-byte codec |
| `…/game/RenjuRejoin.java` | rejoin encode/decode contract |
| `…/game/GridStateFactory.java` | game ids RENJU=31, SPEED_RENJU=32, TB_RENJU=81 |
| `…/game/RenjuForbiddenPointFinder.java` | black forbidden-point / five detection |
