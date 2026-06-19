# Android Turn-Based Renju — Inventory & Reuse Map (for Live Play)

Source: commits `f098b70` ("feat(renju): Taraguchi-10 turn-based (Android)") and
`54dbc0c` ("2.11.0 TB Renju"). This catalogues every renju-touching file, classifies each
piece as **transport-agnostic / reusable** vs **turn-based-only (HTTP/JSP coupled)**, and
records the opening spec + constants. Prefer CODE behaviour over docs where they differ
(noted inline).

---

## 1. Files added / changed

`54dbc0c` changed only `app/src/main/AndroidManifest.xml` (version bump 2.11.0 / versionCode).

`f098b70` (24 files, +1526/-53):

| File | Type | Renju content |
|---|---|---|
| `rules/.../RenjuSymmetry.java` | **NEW** | Pure D4 symmetry dedup helper |
| `rules/.../Variant.java` | MOD | `RENJU(31,15,NONE,1)` enum + `isRenju()` |
| `rules/.../Variants.java` | MOD | `fromGameId(81)→RENJU`, `fromGameType("Renju")` |
| `rules/.../golden/renju_blackfirst.txt` | NEW | golden board for replay test |
| `rules/.../RenjuSymmetryTest.java`, `VariantsTest.java` | NEW/MOD | unit tests |
| `app/.../Game.java` | MOD | phase fields, replay, URL builder, `gridSizeForGameType` |
| `app/.../BoardView.java` (+377) | MOD | board render + opening interaction state-machine |
| `app/.../BoardActivity.java` (+95) | MOD | opening button wiring + submit dispatch |
| `app/.../JsonModels.java` | MOD | `GameResponse.renjuPhase/renjuOffers/renjuSwaps` |
| `app/.../net/OkHttpPenteApi.java` | MOD | `submitMove(...,renjuAction)` overload |
| `app/.../InvitationActivity.java` | MOD | add Renju (id 81) to TB game spinner |
| `app/.../KingOfTheHill.java`, `KingOfTheHillActivity.java` | MOD | id≥31→"Renju" label; toolbar inset |
| `app/.../PentePlayer.java` | MOD | `writeCreds()` helper (not renju-specific) |
| layouts `activity_board.xml`, `activity_king_of_the_hill.xml` | MOD | add `@+id/playAsLabel`; fitsSystemWindows |
| `values/strings.xml`, `values-de/strings.xml` | MOD | renju_swap / renju_dont_swap / renju_place_ten etc. |
| `app/src/test/...GameRenjuUnitTest.java`, `RenjuReplayTest.java`, `JsonTest.java`, `OkHttpPenteApiTest.java` | NEW/MOD | tests |
| `docs/renju-handoff.md` | NEW | 559-line spec (§10 live + §12 TB) |

**Key structural fact:** the only *new* renju class is `RenjuSymmetry`. Everything else is
edits to existing files. There are **no new dialog/modal/fragment classes and no new
layout files** — the opening UI reuses existing button rows.

---

## 2. PURE / REUSABLE logic (transport-agnostic — use as-is for live)

### `rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java`  ★ fully reusable
Pure Java, no Android imports. D4 (dihedral-8) symmetry for Branch-B offer dedup. 15×15, center (7,7).
- `static int[] d4Images(int move)` — up-to-8 in-bounds D4 images of a move index.
- `static boolean isSymmetricDup(int move, int[] accepted)` — does `move` (any image) collide with an accepted offer?
- `static boolean isValidOfferSet(int[] offers)` — true iff no two offers are D4-symmetric.
- Verdict: **REUSE verbatim** for the live Branch-B 10-offer picker.

### `rules/.../Variant.java` / `Variants.java`  ★ reusable
- `Variant.RENJU(canonicalGameId=31, gridSize=15, CaptureRule.NONE, stonesPerTurn=1)`, `isRenju()`.
- `Variants.fromGameId(int)`: **`if (gameId==81) return RENJU;`** else even→canonical=id-1; so 31, 32, 81 all map to RENJU.
- `Variants.fromGameType(String)`: `contains("Renju")→RENJU` (checked last, after Go).
- `Variants.gridSize/captureRule/stonesPerTurn`.
- Verdict: **REUSE** — shared classification used by both transports. Plan explicitly says Milestone A is shared with live; do not duplicate.

### `Game.replayRenjuGame(int until)` (Game.java)  ★ logic reusable
```java
byte color = (byte) (2 - (i % 2));          // black-first: i=0 -> 2(black)
int moveI = move / gridSize, moveJ = move % gridSize;
abstractBoard[moveI][moveJ] = color;        // no captures
```
Pure board decode (black-first, gridSize-based, no captures). Verdict: **REUSE the algorithm** for live replay (the live path drives a different board object but the decode rule is identical).

### `Game.gridSizeForGameType(String)` (static, no Android deps)  ★ reusable
`"Renju"→15, "(9x9)"→9, "(13x13)"→13, else 19`. Verdict: **REUSE**.

### `Game.isRenju()` — `Variants.fromGameType(getGameType()).isRenju()`. Reusable pattern.

---

## 3. MIXED — Game.java data/HTTP (logic reusable, transport NOT)

- Fields `String renjuPhase`, `int[] renjuOffers`, `Integer renjuSwaps` on `Game`.
  **TB-specific source:** populated in `parseGame()` from `mGameJson.renjuPhase` (server-shipped).
  For LIVE these same fields are needed but must be **DERIVED client-side** (live server sends no `renjuPhase`). Reuse the *field shape*, not the source.
- `parseGame()` renju block: sets `gridSize=15`, parses comma-separated `renjuOffers`, and
  **resets all BoardView renju UI state** (`renjuChosen/renjuPicks/renjuCandidates/renjuBoxRadius/renjuOfferMode/renjuSelection`) — important reset discipline to copy for live.
  Also `if ("BRANCH".equals(renjuPhase)) boardView.renjuOfferMode = true;`.
- `buildSubmitMoveUrl(hideStr, gid, moves, message, renjuAction)` + `submitMove(move,msg,renjuAction)` overload + `SubmitMoveTask(...,renjuAction)` — **TB-ONLY**: builds the `gameServer/tb/game?command=move&...&renjuAction=` HTTP GET. Live uses sockets, not this.
- Replay/colour dispatch arms (`getGameType().contains("Renju")` → `setBackgroundColor(renjuColor)` + `replayRenjuGame`). The dispatch *shape* is reusable; the live screen has its own render path.

---

## 4. BoardView.java — opening interaction state-machine + rendering (UI-coupled)

This is the heart of the TB opening UX (+377 lines). It is an Android `View` (Canvas/Paint/Button),
so it is **NOT directly reusable on the live screen** (live uses `liveGameRoom/` + `LiveTableFragment`),
but the **algorithms and field model are the reference to port**.

New public fields (the renju UI state model — port the *concept* to live):
- `int renjuBoxRadius` — 0 = no constraint; >0 limits placement to central (2r+1)² box.
- `List<Integer> renjuCandidates` — translucent preview stones (drawn as board value `4`).
- `List<Integer> renjuPicks` — in-progress OFFERS multi-select (Branch B, up to 10).
- `boolean renjuOfferMode` — collecting 1 (Branch A) or up to 10 (Branch B) → one `move`.
- `List<Integer> renjuSelection` — SELECTION 2-tap pair `[black 5th, white 6th]`.
- `boolean renjuChosen` — SWAP prompt already answered (don't re-show).
- `int renjuColor = #D98880` (dusty rose) background.

Methods (private to BoardView; algorithms reusable, Canvas binding not):
- `applyRenjuSelectionMask()` / `clearRenjuSelectionMask()` — mask every empty non-offer cell to `-1` so only the 10 offers are tappable for the black 5th; clear after the pick so the white 6th can go anywhere. **Reusable masking idiom** (note: writes the render snapshot `game.getState().board`, not the engine board).
- `styleRenjuSubmit()` — greyed-until-valid submit button; enables at n==1 in-box (Branch A, non-SWAP), n==10 valid offer set (`RenjuSymmetry.isValidOfferSet`), or SELECTION pair complete. Encodes the **validity rules** — reusable logic, Android Button binding not.
- `renjuCoord(int)` — move→"H8" label (skips letter 'I'); `setSubmitEnabled(btn,enabled)`.

`onDraw` renju gating (logic to mirror in live):
- `SWAP` + `!renjuChosen` → lock board (no zoom/tracking stone), repurpose button row (see §6).
- `MOVE` phase → `renjuBoxRadius = 4` (9×9).
- `renjuOfferMode` → `renjuCandidates = renjuPicks`, show submit row.
- `SELECTION` → mask offers, render offers as candidates until 5th picked.
- Renju hides the captures subtitle (no captures).

`onTouchEvent` renju gating (the core input FSM — port to live touch handling):
- SWAP+!renjuChosen → consume all board touches (decide via buttons only).
- `renjuOfferMode` (ACTION_UP): toggle pick; reject occupied, reject `RenjuSymmetry.isSymmetricDup`, cap at 10; tracking stone on DOWN/MOVE.
- `SELECTION`: 1st tap = an offered cell (black 5th, board←2, unmask); 2nd tap = empty distinct cell (white 6th, board←1); re-tapping a complete pair lifts the 6th to re-place.
- **Box-constraint enforcement (legality):**
  `if (renjuBoxRadius>0 && (Math.abs(stoneI-7)>renjuBoxRadius || Math.abs(stoneJ-7)>renjuBoxRadius)) playedMove=-1;`
  Centre is (7,7). **REUSE this legality check verbatim** for live.

`drawBoard` renju additions: 5 star points at `{3,7,11}²` (center 7); translucent candidate stones (value `4`); box highlight is intentionally NOT drawn (plan §Task13 proposed a green box, code dropped it). Coordinate letters `A–P` over 15 cols (array already skips 'I').

myColor override (black-first): `myColor = (byte)(2 - movesList.size()%2)`; SELECTION tracking colour = black(2) for 5th, white(1) for 6th. **Reusable colour rule.**

---

## 5. TURN-BASED-ONLY (HTTP/JSP coupled — do NOT reuse for live)

- `Game.buildSubmitMoveUrl` / `SubmitMoveTask` / `submitMove(...,renjuAction)` — `tb/game?command=move&renjuAction=` GET.
- `OkHttpPenteApi.submitMove(gid,moves,message,renjuAction)` — okhttp TB write.
- `JsonModels.GameResponse.renjuPhase/renjuOffers/renjuSwaps` — server-shipped phase read (live has no equivalent JSON; live must derive).
- `parseGame()` reading `renjuPhase` from JSON — TB read path.
- `PentePlayer.writeCreds()` — TB credential query string.
- `InvitationActivity` spinner pos 15→"81", `updatePlayAsVisibility()` (rated color choice) — TB invitation flow.
- `KingOfTheHill(Activity)` id→label mapping (TB KotH).

The **3-action `renjuAction` contract** is TB wire protocol (live uses socket frames instead):
`swap` (take-over, no stone, `moves="1"`) · `move` (1 stone Branch A in 9×9, **or** 10 offers Branch B, **or** windows-1–3 decline single placement; server infers branch by count) · `select` (atomic 2-stone `moves="5th,6th"`, move5+move6). renjuAction is `null` for a plain `MOVE`-phase stone.

---

## 6. Opening-choice UI — how TB does it (and reuse verdict)

**There are NO renju dialog/modal classes.** The opening uses the existing bottom button row
(`R.id.dPenteLayout`, historically the D-Pente/Swap2 control) plus the submit row
(`R.id.submitLayout` / `R.id.submitButton`). Buttons are *repurposed by text*:

| Existing id | Renju role | String |
|---|---|---|
| `R.id.playAsWhiteButton` | **Swap** (take over) | `renju_swap_take_over` = "Swap" |
| `R.id.playAsBlackButton` | **No swap** (decline → Branch A / windows 1–3 place) | `renju_dont_swap` = "No swap" |
| `R.id.swap2PassButton` | **Place 10** (Branch B, shown only at move-4) | `renju_place_ten` = "Place 10" |
| `R.id.playAsLabel` | hidden (GONE) during decision | — |
| `R.id.submitButton` | greyed-until-valid surface; text shows coords / "n/10" | `submit` |

Wiring lives in `BoardActivity.onCreate` click listeners + `setRegularSubmitListener`
submit dispatch (the big `if (game.isRenju() ...)` chain choosing `moves`/`renjuAction`),
and the visibility/state logic in `BoardView.onDraw`.

**Reuse verdict for live:** the live screen (`liveGameRoom/LiveTableFragment` +
`LiveGameRoomActivity`) uses a **modal dialog** pattern for dPente/swap2 (per
`docs/renju-handoff.md` §10: `LiveTableFragment.addMove:452-510` shows a modal; system
messages route via `dsgSystemMessageTableEvent`). So the **BoardView button-row UI is NOT
directly portable** — live has a different presentation. What IS portable: the *decision
model* (Swap / No-swap / Place-10 / box-placement / 10-offer multi-select / 2-tap select),
the validity rules in `styleRenjuSubmit`, the touch FSM in `onTouchEvent`, and the
`RenjuSymmetry` + box-radius legality checks.

---

## 7. Opening spec (Taraguchi-10) — code-confirmed, doc-cross-checked

From plan `docs/superpowers/plans/2026-06-17-renju-turn-based.md` + `docs/renju-handoff.md`
§10.2a, reconciled with code. Black plays first (board value 2). Center (7,7) = index 112.

Phases (`renjuPhase ∈ {SWAP, BRANCH, OFFERS, SELECTION, MOVE, COMPLETE}`):
- Moves 1–3: each placement constrained to a growing central box (see constraints below). After each, a **SWAP** window: opponent may take over (swap) or decline+place next stone.
- **Move 4 = SWAP window (the fork).** Three actions:
  - **Swap / take-over** → no stone (`moves="1"`, renjuAction `swap`) → opponent now to move; leads to a standalone **BRANCH** state.
  - **No swap → Branch A**: place your single **5th stone in the central 9×9 box** (`move`, single index).
  - **Place 10 → Branch B**: offer **ten** non-symmetric 5th-move candidates (`move`, 10 CSV indices, deduped via `RenjuSymmetry`).
- **BRANCH** (post-take-over): same place-1-or-10 affordance (`renjuOfferMode=true`); server infers Branch A (n=1) vs B (n=10) by count.
- **SELECTION** (Branch B, white to act): white taps **1 of the 10** offered cells as the black 5th, then places its **white 6th on any empty cell** → submitted atomically (`select`, `moves="5th,6th"`).
- **Branch A** has an additional swap-5 window then move 6 anywhere; **Branch B** has no swap-5 window (move 6 anywhere after selection).
- `MOVE`/`NORMAL` = ordinary single-stone play (renjuAction null).

> Doc-vs-code note: the plan's early Milestone-G design had windows-1–3 decline send
> `renjuAction="swap"` with `moves="0,<m>"`. **The final code instead sends
> `renjuAction="move"` with `moves="<m>"`** for all decline+place placements (windows 1–3
> and move-4 Branch A); only the stoneless take-over uses `swap`. Trust the code.
> Also: live-derived phase `NORMAL` == server `MOVE`; server `OFFERS` is folded into the
> live `BRANCH` state (handoff §10.2a note).

D4 symmetry dedup (offers): `x=m%15, y=m/15, dx=x-7, dy=y-7`; 8 images
`(dx,dy),(-dy,dx),(-dx,-dy),(dy,-dx),(-dx,dy),(dx,-dy),(dy,dx),(-dy,-dx)` mapped back
`m'=(tx+7)+(ty+7)·15`, in-bounds only. Offer set valid iff no offer's image hits an earlier offer.

---

## 8. Constants

| Constant | Value | Where |
|---|---|---|
| Renju game ids | **31** Renju / **32** Speed Renju / **81** TB Renju | `Variant.RENJU=31`; `Variants.fromGameId(81)`; InvitationActivity pos15→"81" |
| Board size | **15×15** | `Variant.RENJU` gridSize; `Game.gridSizeForGameType` |
| Centre cell | (7,7) → index **112** = 7 + 7·15 | RenjuSymmetry `C=7`; box checks `stoneI-7`,`stoneJ-7` |
| Move encoding | `index = col + row·15` (col=`m%15`, row=`m/15`) | replay, symmetry, coord |
| Black-first colour | move `i` → `2 - i%2` (black=2, white=1) | replayRenjuGame, myColor |
| Renju board colour | `#D98880` (`renjuColor`) | BoardView |
| Star points | `{3,7,11}²` (5 points) | drawBoard |
| Candidate stone value | `4` (translucent black preview) | renjuCandidates / drawStone |
| Mask value | `-1` (forbidden/empty-masked cell) | applyRenjuSelectionMask |
| **Box constraints (radius = central (2r+1)²):** | | BoardActivity sets `renjuBoxRadius=window`; BoardView enforces |
| move 2 (after move 1, window 1) | r=1 → **3×3** | `renjuBoxRadius = window` (=1) |
| move 3 (window 2) | r=2 → **5×5** | window=2 |
| move 4 (window 3) | r=3 → **7×7** | window=3 |
| move 5 (Branch A / MOVE phase) | r=4 → **9×9** | `renjuBoxRadius=4` |

Enforcement: `Math.abs(stoneI-7) > renjuBoxRadius || Math.abs(stoneJ-7) > renjuBoxRadius` → reject (in `onTouchEvent`).

---

## 9. Reuse verdict summary

**REUSE AS-IS (pure logic, no transport coupling):**
- `RenjuSymmetry` (d4Images / isSymmetricDup / isValidOfferSet)
- `Variant.RENJU` + `Variants` (fromGameId/fromGameType/gridSize/captureRule/stonesPerTurn)
- `Game.replayRenjuGame` decode rule, `Game.gridSizeForGameType`, `Game.isRenju`
- Box-radius legality check + radius schedule (1/2/3/4 ↔ 3×3/5×5/7×7/9×9)
- Black-first colour rule `2 - i%2`; constants (ids, 15, 112, #D98880, mask -1, candidate 4)

**PORT THE ALGORITHM, NOT THE BINDING (Android View-coupled):**
- BoardView opening FSM (`onTouchEvent` pick/select/box logic), `styleRenjuSubmit` validity
  rules, offer-mask idiom, candidate/star rendering — re-express against the live screen
  (`liveGameRoom/` Table + LiveTableFragment), which uses dialogs not the bottom button row.
- The renju UI **state-field model** (renjuBoxRadius/renjuPicks/renjuOfferMode/renjuSelection/renjuChosen).

**DO NOT REUSE (turn-based / HTTP only):**
- `buildSubmitMoveUrl` / `SubmitMoveTask` / `OkHttpPenteApi.submitMove(renjuAction)` and the
  `renjuAction` GET contract (`swap`/`move`/`select`) — live uses socket frames
  (`dsgRenjuTaraguchiSwapTableEvent`, `dsgRenjuTaraguchiOffer10TableEvent`,
  `dsgRenjuTaraguchi10Select1TableEvent` per handoff §10).
- `JsonModels.GameResponse.renju*` read fields + `parseGame` phase read — **live has no
  server-shipped phase; it must DERIVE the phase from echo events** (handoff §10.2a table).
- InvitationActivity / KingOfTheHill TB plumbing, `PentePlayer.writeCreds`.

**Biggest live-specific gap (from handoff §10, already scoped):** the live path needs a
client-side phase derivation (a `RenjuState`/`renjuState` analogous to existing
`dPenteState`/`swap2State`/`goState` in `liveGameRoom/GameState.java`) plus black-first
colour and 15×15 sizing arms in `Table.java` (`currentColor`/`currentPlayer`) — none of
which exist in the TB code, because TB simply reads `renjuPhase`.
