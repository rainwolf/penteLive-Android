# Android Live-Game-Room Map (for Renju / Taraguchi-10 live support)

Scope: how live multiplayer events are (de)serialized & dispatched on Android, the
swap2/dpente "special opening" template, board tap/overlay rendering, variant/game-id
detection, and an inventory of the renju **turn-based** code already in the tree.

All paths are absolute. Source root:
`/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/`

---

## 1. Event transport, serialization & dispatch

### 1a. Raw-socket framing — `org/pente/gameServer/event/`

The transport classes the server imports DO exist on Android (not in a jar):
`app/src/main/java/org/pente/gameServer/event/` — `SocketDSGEventHandler.java`,
`ClientSocketDSGEventHandler.java`, `DSGEventListener.java`, `SynchronizedQueue.java`.

**There is NO `DSGEventWrapper` class on Android.** The server's
`DSGEventWrapper` (a single-key Gson container) lives only server-side
(`pente.org/dsg_src/java/org/pente/gameServer/event/DSGEventWrapper.java`). On Android the
handler deals in **raw JSON strings**, never typed event objects.

Framing = `<UTF-8 JSON bytes><0xFF (255) terminator>`.

`DSGEventListener.java` (whole file):
```java
public interface DSGEventListener {
    void eventOccurred(String dsgEvent);   // payload is a raw JSON string
}
```

`SocketDSGEventHandler.java`:
- **Reader** (`ObjectReader.run`, lines 44-84): reads bytes into a `ByteArrayOutputStream`
  until it sees `255`, then `baos.toString(StandardCharsets.UTF_8)` and
  `notifyListeners(jsonStr)`:
  ```java
  while ((b = inStream.read()) > -1) {
      if (b != 255) { baos.write(b); }
      else { jsonStr = baos.toString(StandardCharsets.UTF_8); baos.reset(); break; }
  }
  if (jsonStr != null) { notifyListeners(jsonStr); }
  ```
- **Writer** (`ObjectWriter.run`, lines 86-123): pulls a `String` off `outputQueue`, writes
  UTF-8 bytes then the `255` terminator + flush:
  ```java
  String jsonStr = (String) o;
  byte[] bytes = jsonStr.getBytes(StandardCharsets.UTF_8);
  outStream.write(bytes, 0, bytes.length);
  outStream.write(255);
  outStream.flush();
  ```
- `eventOccurred(String)` (line 188) just enqueues onto `outputQueue` (the outgoing send API).
- `notifyListeners` (line 181) fans the incoming JSON string to all registered
  `DSGEventListener`s.

`ClientSocketDSGEventHandler.java` (lines 24-48): subclass; ctor wraps the socket streams in
`BufferedInputStream`/`BufferedOutputStream` and calls `super.go()` to start the two threads.

### 1b. Dispatch hub — `liveGameRoom/LiveGameRoomActivity.java` (791 lines)

`LiveGameRoomActivity implements DSGEventListener` (line 57). It holds the
`ClientSocketDSGEventHandler eventHandler` (line 59), connects the SSL socket in
`connectSocket(int)` (line 134; emulator `10.0.2.2`, prod `pente.org`), then
`new ClientSocketDSGEventHandler(socket)` and `eventHandler.addListener(this)`.

**Incoming routing — `eventOccurred(String dsgEvent)` (line 231):**
- Parses with **org.json (NOT Gson)** via `jsonToMap(dsgEvent)` (line 687) →
  `Map<String,Object>` (helpers `toMap`/`toList` recurse, lines 704-737).
- `dsgPingEvent` is echoed straight back on the socket thread (line 234).
- Everything else is posted to the UI thread and dispatched by a **long
  `if/else if` chain on single-key presence** (`jsonEvent.get("dsgXxxEvent") != null`),
  lines 246-418. The JSON wire shape is exactly the server's wrapper:
  `{"dsgMoveTableEvent":{...}}` — one key = event type, value = payload map.

Table/game events handled (lines 345-418):
| Wire key | Handler | Notes |
|---|---|---|
| `dsgMoveTableEvent` | `updateTableMove` (491) | stone placement / move list |
| `dsgGameStateTableEvent` | `updateTableGameState` (509) | `state` int + `changeText` |
| `dsgTimerChangeTableEvent` | `updateTableTimer` (528) | |
| `dsgSwapSeatsTableEvent` | `swapSeats` (571) | **swap2/dpente colour choice** |
| `dsgSwap2PassTableEvent` | `swap2Pass` (591) | swap2 pass |
| `dsgUndoRequestTableEvent` / `dsgUndoReplyTableEvent` | (353/360) | |
| `dsgRejectGoStateEvent` | (417) | Go |
| join/sit/stand/text/exit/owner/invite/boot/changeState | (246-414) | room + table mgmt |

`updateTableMove` (491-507): reads `table` (int), `moves` (`List<Integer>`), `move` (int).
If `move != 0` → `fragment.addMove(move)` + new-move sound; else `fragment.addMoves(moves)`
(bulk catch-up after join). **This is the move event renju stones still ride on.**

**Outgoing send — `sendEvent(String event)` (line 443):** the listener interface the
fragments call; it forwards to `eventHandler.eventOccurred(event)` (enqueue→write). Events are
built as **hand-concatenated JSON strings**, not via Gson. Examples in this file:
```java
sendEvent("{\"dsgJoinTableEvent\":{\"table\":" + tableId + ",\"time\":0}}");       // 660
sendEvent("{\"dsgExitTableEvent\":{...,\"table\":" + id + ",...}}");                 // 656
```

> **Renju implication:** the three new events are added the same way — register
> `else if (jsonEvent.get("renjuSwap"/"renjuOffer10"/"renjuSelect1") != null)` branches in
> `eventOccurred`, and emit them as hand-built JSON strings via `sendEvent(...)`. No Gson, no
> wrapper class — match the existing one-key-per-event JSON shape and include `player` + `table`
> (and `time:0`) like every other table event.

---

## 2. Template special-opening variant: swap2 / dpente (end to end)

This is the closest existing analogue to renju's colour/branch choice and is the template to
mirror.

### 2a. State model — `liveGameRoom/GameState.java` + enums
`Table` owns a `GameState gameState` (Table.java:58). `GameState` (whole class):
```java
public State state = State.NOTSTARTED;            // State enum: NOTSTARTED, STARTED, PAUSED, HALFSET
public DPenteState dPenteState = DPenteState.NOCHOICE;   // NOCHOICE, SWAPPED, NOTSWAPPED
public Swap2State swap2State = Swap2State.NOCHOICE;      // NOCHOICE, SWAP2PASS, SWAPPED, NOTSWAPPED
public GoState goState = GoState.PLAY;                   // PLAY, MARKSTONES, EVALUATESTONES
public Map<Integer, Map<String,Long>> timers;
```
A new `renjuState` enum/field would live here (e.g. `RenjuState renjuState = NOCHOICE`).

### 2b. Opening-phase + turn tracking — `liveGameRoom/Table.java` (1748 lines)
- Variant predicates **delegate to the shared rules module** (see §4):
  `isGo()` (187), `isDPente()` (269), `isSwap2()` (274) each do
  `Variant v = Variants.fromGameId(game); return v != null && v.isXxx();`.
  **There is NO `isRenju()` on Table yet — must be added** (`v.isRenju()` exists on `Variant`).
- Whose-turn / colour logic keys off the opening sub-state:
  `currentColor()` (279), `currentPlayerName()` (344), `isMyTurn(String)` (296) branch on
  `isDPente()`/`isSwap2()` + `moves.size()` + the `*State` enum (e.g. lines 313-331).
- Choice-window predicates the UI polls after every move:
  ```java
  public boolean swap2ChoiceWithPass()    // 375: isSwap2 && swap2State==NOCHOICE && moves.size()==3
  public boolean swap2ChoiceWithoutPass() // 378: isSwap2 && (SWAP2PASS||NOCHOICE) && moves.size()==5
  // dpente uses: isDPente() && moves.size()==4 && dPenteState==NOCHOICE  (checked in fragment)
  ```
- Apply-choice mutators (called when the inbound swap event arrives):
  `swapSeats(boolean swap, boolean silent)` (353) sets both `dPenteState`/`swap2State` to
  `SWAPPED`/`NOTSWAPPED`; `swap2Pass(boolean)` (372) sets `swap2State = SWAP2PASS`.
- `getGame()`/`setGame(int)` (1086/1090) hold the numeric game id; static `gameNames` map
  (lines 54-85) translates id→display name **but stops at id 30 — 31/32 (renju) are absent**
  (see §4).

### 2c. UI choice flow — `liveGameRoom/LiveTableFragment.java` (1286 lines)
The opening choice is a **bottom-gravity `AlertDialog.Builder.setItems(...)` list**, raised
*reactively* from the move handler — NOT a dedicated event:

- `addMove(int)` (452) / `addMoves(List)` (491): after `table.addMove(...)`, they check
  ```java
  if (table.isDPente() && table.getMoves().size()==4 && dPenteState==NOCHOICE) showDPenteChoice();   // 456
  if (table.swap2ChoiceWithPass() || table.swap2ChoiceWithoutPass()) showSwap2Choice();              // 462
  ```
  (only shown when it's *this* client's decision — gated on `isMyTurn`/seat).
- `showDPenteChoice()` (1017) and `showSwap2Choice()` (1049): build a 2- or 3-item dialog
  (`p1white`, `p2black`, optional `swap2pass`), `setCanceledOnTouchOutside(false)`, bottom
  gravity, `clearFlags(FLAG_DIM_BEHIND)`.
- Chosen action → **hand-built JSON strings on the same `dsgSwapSeatsTableEvent` /
  `dsgSwap2PassTableEvent` wire keys**:
  ```java
  sendSwap2Choice(boolean swap)  // 1092
    -> sendEvent("{\"dsgSwapSeatsTableEvent\":{\"swap\":"+swap+",\"silent\":false,\"player\":\""+me+"\",\"table\":"+id+",\"time\":0}}");
  sendSwap2Pass()                // 1097 -> "{\"dsgSwap2PassTableEvent\":{\"silent\":false,\"player\":..,\"table\":..,\"time\":0}}"
  sendDPenteChoice(boolean white)// 1044 -> also emits dsgSwapSeatsTableEvent (swap = white)
  ```
- Inbound apply: `LiveGameRoomActivity.swapSeats` (571) reads `swap`/`silent`, calls
  `table.swapSeats(...)`, posts a "* seats swapped" table message; `swap2Pass` (591) →
  `table.swap2Pass(silent)`.
- `updateGameState(int)` (584) applies `dsgGameStateTableEvent` (start/pause).

> **Renju template:** the renju 5th-move choice is the analogue. New events `renjuSwap`
> {swap, move}, `renjuOffer10` {moves[]}, `renjuSelect1` {move} replace `dsgSwapSeatsTableEvent`/
> `dsgSwap2PassTableEvent`. The reactive `addMove/addMoves` hook + a `showRenjuChoice()` dialog
> (or the richer board-overlay flow from the TB code, §5) is where to wire it.

---

## 3. Live board rendering & tap input — `liveGameRoom/LiveBoardView.java` (409 lines)

Custom `View`. Set up in `LiveTableFragment.onActivityCreated`:
`board = getView().findViewById(R.id.boardView); board.setTable(table, me); board.setFragment(this);`
(LiveTableFragment.java:234-236).

- `onDraw(Canvas)` (80): scales/translates, computes `redDot` = last move
  (`table.getMoves().get(size-1)`), `drawBoard(canvas)`, and a zoomed preview stone while
  dragging. `drawBoard` (>line 170) renders stones from `table.abstractBoard[i][j]` and Go
  overlays.
- **Tap → move — `onTouchEvent` (101):**
  - Guards: `if (!table.currentPlayerName().equals(me) || gameState.state != STARTED) return false;`
    (turn + game-active gate).
  - DOWN/MOVE = magnifier zoom (`zoomedScale = 3`) with a tracking stone; UP commits.
  - Cell from pixels: `stoneJ = gridSize*stoneX/size; stoneI = gridSize*stoneY/size;`
    `playedMove = gridSize*stoneI + stoneJ;` (only if `abstractBoard[i][j]==0`).
  - On `ACTION_UP` with a valid `playedMove`, it sends the move directly:
    ```java
    fragment.getListener().sendEvent(
      "{\"dsgMoveTableEvent\":{\"move\":"+playedMove+",\"moves\":["+playedMove+"],\"player\":\""+me+"\",\"table\":"+table.getId()+",\"time\":0}}");  // 156
    ```
  - `gridSize` is a field defaulting to 19 (`setGridSize`, line 44) — **renju needs 15**
    (rules module reports `gridSize(RENJU)==15`).
- **Overlay model for highlighting candidate points:** Go already overlays per-player cell
  lists: `setGoTerritoryByPlayer(Map<Integer,List<Integer>>)` (54),
  `setGoDeadStonesByPlayer` (49), drawn in `onDraw`, cleared by `clearGoStructures()` (162).
  This translucent-cell mechanism is the natural template for **rendering the 10 candidate
  fifth-move points** (cf. the TB `renjuCandidates` translucent-stone rendering in §5).

> Note: the richer turn-based board (`BoardView.java`, §5) already implements the full renju
> multi-tap input + candidate overlays. `LiveBoardView` is a separate, simpler view; the live
> work either ports those renju draw/touch paths into `LiveBoardView` or factors them shared.

---

## 4. Variant / game-id detection — shared `rules` module

A standalone Gradle module `rules/` holds the single source of truth (also unit-tested):
`rules/src/main/java/be/submanifold/pente/rules/Variant.java` + `Variants.java`.

`Variant.java` enum (line 13) — **RENJU already registered**:
```java
RENJU(31, 15, CaptureRule.NONE, 1);   // line 30: canonicalGameId=31, gridSize=15, no capture, 1 stone/turn
public boolean isRenju() { return this == RENJU; }   // line 65
```
Also `isSwap2()`, `isDPente()`, `isGo()`, `isGomoku()`, `isConnect6()`.

`Variants.java`:
- `fromGameType(String)` (33): substring match; `if (gameType.contains("Renju")) return RENJU;`
  (line 79) — handles both "Renju" and "Speed Renju".
- `fromGameId(int)` (90): `if (gameId == 81) return RENJU; // TB Renju`; else canonical =
  even→id-1, lookup `BY_CANONICAL_ID`. **So 31→RENJU and 32→(canonical 31)→RENJU already work.**
- Helpers `gridSize(v)`, `captureRule(v)`, `stonesPerTurn(v)`.

`VariantsTest.java` already asserts `fromGameId(31)==RENJU`, `fromGameId(32)==RENJU`,
`fromGameId(81)==RENJU`, gridSize 15, isRenju (lines 127-142).

**Gaps for live play:**
1. `Table.java` static `gameNames` map (lines 54-85) has **no 31/32 entries** → add
   `gameNames.put(31,"Renju"); put(32,"Speed Renju");` so the lobby/table shows the name and
   `getGameName()` resolves.
2. `Table.java` has no `isRenju()` wrapper (only isGo/isDPente/isSwap2 delegate to `Variants`)
   → add `isRenju()` mirroring the others.
3. `LiveBoardView.gridSize` defaults 19; renju tables must call `setGridSize(15)` (the live
   table sizing path currently keys off Go specials only).

The HTTP/turn-based side (`Game.java`) already detects renju via the rules module:
`isRenju()` (Game.java:979) = `Variants.fromGameType(getGameType()).isRenju()`, and
`gridSizeForGameType` returns 15 for `contains("Renju")` (Game.java:987). `JsonModels.java`
already carries `renjuPhase`, `renjuOffers`, `renjuSwaps` (lines 144-146).

---

## 5. Existing renju **turn-based** code (inventory only — reuse detailed elsewhere)

Recent commit `f098b70 feat(renju): Taraguchi-10 turn-based (Android)` added a complete
**HTTP/REST** renju opening (NOT socket). It is the behavioural reference for the live port.

| File | Renju content |
|---|---|
| `rules/src/main/java/be/submanifold/pente/rules/Variant.java` | `RENJU(31,15,NONE,1)`, `isRenju()` |
| `rules/src/main/java/be/submanifold/pente/rules/Variants.java` | `fromGameType`/`fromGameId` renju (incl. TB id 81) |
| `rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java` | D4 symmetry helpers: `int[] d4Images(int move)` (15), `boolean isSymmetricDup(int, int[])` (36), `boolean isValidOfferSet(int[])` (46). N=15, center C=7. Used to reject symmetric duplicate offers. |
| `Game.java` | Fields `renjuPhase`, `renjuOffers (int[])`, `renjuSwaps (Integer)` (83-85); `isRenju()` (979); `SubmitMoveTask`/`buildSubmitMoveUrl`/`submitMove(move,msg,renjuAction)` carry a `renjuAction` query param (519-549, 928-945); `parseGame` populates renjuPhase/offers/swaps and toggles `boardView.renjuOfferMode` for the BRANCH phase (1074-1099). |
| `BoardView.java` (turn-based board, **not** LiveBoardView) | Full renju render + multi-tap input. Fields (54-75): `renjuBoxRadius`, `renjuCandidates` (translucent), `renjuPicks` (in-progress offer multi-select), `renjuOfferMode`, `renjuSelection` (2-tap [black5th, white6th]), `renjuChosen`. `onDraw` drives SWAP decision buttons, offer mode, SELECTION mask (147-262). `onTouchEvent` collects up to 10 offers w/ `RenjuSymmetry.isSymmetricDup` rejection (419-446) and the SELECTION 2-tap pick (447-485). `styleRenjuSubmit`/`applyRenjuSelectionMask`/`renjuCoord` helpers. |
| `BoardActivity.java` | Renju decision-button wiring (HTTP). `playAsWhiteButton`→`submitMove("1",msg,"swap")` (95-98); `playAsBlackButton` decline (114-145); `swap2PassButton` reused as "Place 10" (150-160, sets `renjuOfferMode`); submit handler computes `renjuAction` ∈ {`"swap"`,`"move"`,`"select"`, or null plain move} per phase and calls `game.submitMove(moves,msg,renjuAction)` (281-413). |
| `net/OkHttpPenteApi.java` | `submitMove(gid,moves,msg,renjuAction)` adds `renjuAction` query param (167-178). |
| `JsonModels.java` | `renjuPhase`, `renjuOffers`, `renjuSwaps` (144-146). |
| `res/values/strings.xml` | `renju_swap_take_over`="Swap", `renju_dont_swap`="No swap", `renju_place_ten`="Place 10", `renju_place_in_box`, `renju_need_10_offers`, `renju_place_1_or_10`, `renju_select_two` (lines 11-17). Mirrored in `values-de/`. |
| `rules/src/test/.../RenjuSymmetryTest.java`, `VariantsTest.java` | unit tests for the above. |

### TB phases & actions (the semantics the live events must reproduce)
- `renjuPhase` values: `"SWAP"`, `"BRANCH"` (post-take-over place-1-or-10),
  `"MOVE"` (constrained single placement, `renjuBoxRadius=4`), `"SELECTION"` (offerer picks
  black 5th + white 6th from the 10 offers).
- TB `renjuAction` HTTP values: `"swap"` (take over, move="1"), `"move"` (place / offer set),
  `"select"` (pick pair), or null (plain stone). Decline = move="0" / "1" with no action.

### Mapping TB → the 3 new live socket events
- `renjuSwap {boolean swap, int move}` ⇐ TB `"swap"` take-over vs decline (carry the
  continuing stone in `move` when applicable). Replaces the `dsgSwapSeatsTableEvent`-style
  colour decision.
- `renjuOffer10 {int[] moves}` ⇐ TB offer-mode (`renjuPicks`, validated by
  `RenjuSymmetry.isValidOfferSet`) — the 10 candidate fifth points.
- `renjuSelect1 {int move}` ⇐ TB `"select"` — the offerer's chosen black 5th (the white 6th
  rides the ordinary `dsgMoveTableEvent`).
- Ordinary stones (BRANCH place-1, MOVE, the white 6th) **stay on `dsgMoveTableEvent`**.

---

## Key takeaways / gaps to close for live renju
1. Transport is **raw JSON strings + 0xFF framing**; incoming parsed with **org.json**
   `jsonToMap`, dispatched by a single-key if/else in `LiveGameRoomActivity.eventOccurred`;
   outgoing **hand-built JSON** via `sendEvent`. Add 3 inbound branches + 3 outbound builders.
   No `DSGEventWrapper` / no Gson on Android.
2. Opening template = swap2/dpente: sub-state enum in `GameState`, predicates in `Table`,
   reactive `AlertDialog` raised from `LiveTableFragment.addMove/addMoves`, choice sent as a
   table event. Add `RenjuState`, `Table.isRenju()`, renju choice UI.
3. Board taps live in `LiveBoardView.onTouchEvent` (gated on `currentPlayerName==me` &&
   `state==STARTED`), candidate overlays via the Go `setGoTerritoryByPlayer` translucent-cell
   pattern. Port the renju multi-tap/offer/candidate logic from `BoardView.java`.
4. Game-id mapping already correct in the `rules` module (31/32/81 → RENJU, grid 15), but
   `Table.gameNames` lacks 31/32 and `LiveBoardView` defaults grid 19 — both need a renju entry.
5. A full **turn-based** renju implementation already exists (`Game.java`, `BoardView.java`,
   `BoardActivity.java`, `RenjuSymmetry.java`, strings) — reuse its phase model, symmetry
   helpers, and UI affordances; only the transport (HTTP `renjuAction` → socket events) differs.
