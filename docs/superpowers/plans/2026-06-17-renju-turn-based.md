# Renju (Taraguchi-10) — Turn-Based Implementation Plan (Phase 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire the Renju (Taraguchi-10) opening into the turn-based / correspondence (HTTP) path of the `pentelive-android` app — the offline `BoardActivity`/`Game`/`BoardView` screen.

**Architecture:** TB is **read-only on phase** — the server ships the derived phase in `GameResponse.renjuPhase`; the client reads it (NO client-side derivation, unlike the live path). Work splits into: (1) shared `rules/` variant registry, (2) `GameResponse` read fields, (3) board sizing + black-first 15×15 replay/decode, (4) `renjuAction` submission contract, (5) a pure D4-symmetry dedup helper, (6) the on-board opening UI driven by the read phase. Logic is TDD'd via the existing reflection-based characterization harness + pure JUnit; Canvas/Activity UI is build + manual-QA verified.

**Tech Stack:** Java 17, Android Gradle, JUnit 4.13.2, okhttp3 MockWebServer, Gson. Source spec: `docs/renju-handoff.md` §12 (+ §10.5 for the D4 dedup algorithm). Game ids: **31 Renju / 32 Speed Renju / 81 TB Renju**; board **15×15**; center **112** (`7+7·15`); move encoding `x + y·15`; black plays first (board value **2**).

**Scope note:** This is Phase 1 (turn-based). Phase 2 (live, §10) is a separate plan. Milestone A (`rules/`) is shared with live — do **not** duplicate it in Phase 2. Within Phase 1 the UI is staged: read-side + `renjuAction` + swap windows first (Milestones A–F, G), then the branch/offer/selection multi-select (Milestone H).

---

## File Structure

| File | Responsibility | Milestone |
|---|---|---|
| `rules/src/main/java/be/submanifold/pente/rules/Variant.java` | add `RENJU` enum entry + `isRenju()` | A |
| `rules/src/main/java/be/submanifold/pente/rules/Variants.java` | map ids 31/32/81 + names "Renju"/"Speed Renju" → `RENJU` | A |
| `rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java` | **new** — pure D4 (8-image) symmetry dedup for offer validation | E |
| `app/src/main/java/be/submanifold/pentelive/JsonModels.java` | add `renjuPhase`/`renjuOffers`/`renjuSwaps` to `GameResponse` | B |
| `app/src/main/java/be/submanifold/pentelive/Game.java` | `isRenju()`, store read phase, `gridSizeForGameType` helper, `replayRenjuGame`, dispatch arms, `buildSubmitMoveUrl` + `renjuAction` overload | C, D |
| `app/src/main/java/be/submanifold/pentelive/net/OkHttpPenteApi.java` | `submitMove(..., renjuAction)` overload | D |
| `app/src/main/java/be/submanifold/pentelive/BoardView.java` | `renjuColor`, 15×15 star points, 15-letter coords, central-box highlight, translucent candidate hooks | F, H |
| `app/src/main/java/be/submanifold/pentelive/BoardActivity.java` | read `renjuPhase` → show control → submit `renjuAction` | G, H |
| `rules/src/test/.../VariantsTest.java` | extend with Renju cases | A |
| `rules/src/test/.../RenjuSymmetryTest.java` | **new** | E |
| `app/src/test/.../net/JsonTest.java` | extend with `GameResponse` Renju roundtrip | B |
| `app/src/test/.../RenjuReplayTest.java` | **new** — characterization for `replayRenjuGame` | C |
| `app/src/test/.../GameRenjuUnitTest.java` | **new** — `isRenju`, `gridSizeForGameType`, `buildSubmitMoveUrl` | C, D |
| `app/src/test/.../net/OkHttpPenteApiTest.java` | extend with `renjuAction` param | D |
| `rules/src/test/resources/golden/renju_blackfirst.txt` | **new** golden | C |

---

## Verified anchors (grounded 2026-06-17; grep the symbol — lines drift)

- `Variant.java:13-63` enum body; predicates `isSwap2():52`, `isDPente():56`, `isGo():60`. No RENJU.
- `Variants.java:10-15` `BY_CANONICAL_ID` static init; `fromGameType:33-80` (substring, longest-first); `fromGameId:87-90` (`canonical = even? id-1 : id`).
- `JsonModels.java` `GameResponse:121-154`; `dPenteState:142`, `swap2pass:143`.
- `Game.java`: `parseGame:950`; Go-only sizing `if` `:997-1010` (sets `gridSize` + `boardView.gridSize`); dPente read `:1021-1026`; `isDPente:945-948`; `isSwap2:921-926`; replay+colour dispatch `:1323-1362`; incremental dispatch `:1470-1504`; `replayGomokuGame(int):1528-1535` (`color=1+(i%2)` `:1531`; `move/19,move%19` `:1532`); `/19,%19` sites `:1381,1532,1541,1571,1619,1639,1661,1680,1704,1737,1757,2316`; Go replay already uses `/gridSize` `:2518`; `submitMove:915-918`; `SubmitMoveTask` URL `:533-539`; `abstractBoard` 19×19 literal `:97-115`; default `gridSize=19` `:2477`; `mMyColor:53`.
- `OkHttpPenteApi.java` `submitMove:163-176`.
- `BoardView.java`: colours `:37-43` (no renjuColor); `gridSize:52`; star points `drawBoard:463-488` (non-Go else uses `6*step`); `coordinateLetters:71` (19 letters A–T skip I); touch encode `:259-261,306`; `drawStone:611-646` (`==2`black,`==1`white,`==4`translucent-black `setAlpha(180)`,`==3`translucent-white).
- `BoardActivity.java`: opening-UI wiring `onCreate:65-220` (`playAsWhite/playAsBlackButton/dPenteLayout/swap2PassButton`); `setRegularSubmitListener:239-337` (branches `isConnect6/isDPente/isSwap2/swap2Choice/dPenteChoice` then `game.submitMove(moves, msg):~326`).
- **Test harness:** `GameCharacterizationTest` builds `Game` via the 11-arg string ctor (`makeGame`), injects `mMovesList` by reflection (`setMoves`), invokes private `replay*Game(int)` by reflection (`callReplay`), serialises `abstractBoard`+capture counts vs `rules/src/test/resources/golden/*.txt`. **Reuse this harness for `replayRenjuGame`.**

**Decode strategy (low-risk):** Keep `abstractBoard` 19×19 (a 15×15 board occupies the top-left; draw loop is `gridSize`-bounded). Do **NOT** rewrite the other 12 `/19` sites — they belong to other variants and changing them is out of scope. Add a dedicated `replayRenjuGame` that decodes with `gridSize`. The `gridSize` field reaches the replay worker via the field (set to 15 for Renju in `parseGame`; in unit tests set by reflection).

**Build/test commands:** `./gradlew :rules:test` (pure), `./gradlew :app:testDebugUnitTest` (app unit), `./gradlew assembleDebug` (compile/UI). Run the narrowest that covers the task.

---

## Milestone A — Shared rules registry (both transports)

### Task 1: Add `RENJU` variant + `isRenju()`

**Files:**
- Modify: `rules/src/main/java/be/submanifold/pente/rules/Variant.java:13-63`
- Test: `rules/src/test/java/be/submanifold/pente/rules/VariantsTest.java`

- [ ] **Step 1: Write the failing test** — add to `VariantsTest`:

```java
@Test
public void renjuVariantIsRegistered() {
    assertEquals(15, Variants.gridSize(Variant.RENJU));
    assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.RENJU));
    assertEquals(1, Variants.stonesPerTurn(Variant.RENJU));
    assertTrue(Variant.RENJU.isRenju());
    assertFalse(Variant.PENTE.isRenju());
    assertFalse(Variant.GOMOKU.isRenju());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :rules:test --tests '*VariantsTest'`
Expected: FAIL — `cannot find symbol: variable RENJU` / `method isRenju()`.

- [ ] **Step 3: Add the enum entry + predicate**

In `Variant.java`, add after the `GO_19(...)` entry (keep the `;` terminator after the last entry):

```java
    GO_19(19, 19, CaptureRule.NONE, 1),
    RENJU(31, 15, CaptureRule.NONE, 1);
```

Add the predicate alongside `isGo()`:

```java
    public boolean isRenju() {
        return this == RENJU;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :rules:test --tests '*VariantsTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add rules/src/main/java/be/submanifold/pente/rules/Variant.java rules/src/test/java/be/submanifold/pente/rules/VariantsTest.java
git commit -m "feat(rules): add RENJU variant (id 31, 15x15, no capture) + isRenju()"
```

### Task 2: Map ids 31/32/81 and names to `RENJU`

**Files:**
- Modify: `rules/src/main/java/be/submanifold/pente/rules/Variants.java:33-90`
- Test: `rules/src/test/java/be/submanifold/pente/rules/VariantsTest.java`

Note: `fromGameId` derives `canonical = (id even) ? id-1 : id`. So `32→31` (canonical) resolves once `31→RENJU` is in `BY_CANONICAL_ID` (automatic, the static loop registers `canonicalGameId=31`). But `81` is odd → `canonical=81`, which has no enum entry → needs an explicit mapping. The server ships `gameName="Renju"` for **both** 31 and 81 and `"Speed Renju"` for 32 (no "TB Renju" string).

- [ ] **Step 1: Write the failing test** — add to `VariantsTest`:

```java
@Test
public void renjuResolvesByIdAndName() {
    assertEquals(Variant.RENJU, Variants.fromGameId(31)); // Renju
    assertEquals(Variant.RENJU, Variants.fromGameId(32)); // Speed Renju (canonical 31)
    assertEquals(Variant.RENJU, Variants.fromGameId(81)); // TB Renju
    assertEquals(Variant.RENJU, Variants.fromGameType("Renju"));
    assertEquals(Variant.RENJU, Variants.fromGameType("Speed Renju"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :rules:test --tests '*VariantsTest'`
Expected: FAIL — `fromGameId(81)` returns null; `fromGameType("Renju")` returns null.

- [ ] **Step 3: Implement the mappings**

In `Variants.fromGameId`, special-case 81 before the canonical lookup:

```java
public static Variant fromGameId(int gameId) {
    if (gameId == 81) return Variant.RENJU; // TB Renju: no canonical enum id
    int canonical = (gameId % 2 == 0) ? gameId - 1 : gameId;
    return BY_CANONICAL_ID.get(canonical);
}
```

In `Variants.fromGameType`, add a `Renju` arm **before** the generic returns (substring match; "Speed Renju" contains "Renju", so a single `contains("Renju")` arm covers both):

```java
        if (gameType.contains("Renju")) {
            return Variant.RENJU;
        }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :rules:test`
Expected: PASS (all VariantsTest, no regressions).

- [ ] **Step 5: Commit**

```bash
git add rules/src/main/java/be/submanifold/pente/rules/Variants.java rules/src/test/java/be/submanifold/pente/rules/VariantsTest.java
git commit -m "feat(rules): resolve Renju by id (31/32/81) and gameName"
```

---

## Milestone B — TB read-side JSON

### Task 3: Add Renju fields to `GameResponse`

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/JsonModels.java` (`GameResponse:121-154`)
- Test: `app/src/test/java/be/submanifold/pentelive/net/JsonTest.java`

Backend types (confirmed): `renjuPhase` String, `renjuOffers` String (comma-separated indices), `renjuSwaps` Integer. Gson tolerates missing fields → backward-safe.

- [ ] **Step 1: Write the failing test** — add to `JsonTest`:

```java
@Test
public void parsesRenjuFieldsFromGameResponse() {
    String json = "{\"gid\":\"12345\",\"gameName\":\"Renju\",\"moves\":\"112,113,114,115\","
        + "\"renjuPhase\":\"SELECTION\",\"renjuOffers\":\"113,114,115,116,128,129,130,131,144,145\","
        + "\"renjuSwaps\":5}";
    JsonModels.GameResponse r = Json.GSON.fromJson(json, JsonModels.GameResponse.class);
    assertEquals("SELECTION", r.renjuPhase);
    assertEquals("113,114,115,116,128,129,130,131,144,145", r.renjuOffers);
    assertEquals(Integer.valueOf(5), r.renjuSwaps);
}

@Test
public void renjuFieldsDefaultNullWhenAbsent() {
    JsonModels.GameResponse r =
        Json.GSON.fromJson("{\"gameName\":\"Pente\"}", JsonModels.GameResponse.class);
    assertNull(r.renjuPhase);
    assertNull(r.renjuOffers);
    assertNull(r.renjuSwaps);
}
```

Add imports `static org.junit.Assert.assertNull;` if not present.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*JsonTest'`
Expected: FAIL — `cannot find symbol: renjuPhase`.

- [ ] **Step 3: Add the fields** — in `GameResponse`, after `public Boolean swap2pass;`:

```java
        public String renjuPhase;
        public String renjuOffers;
        public Integer renjuSwaps;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*JsonTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/JsonModels.java app/src/test/java/be/submanifold/pentelive/net/JsonTest.java
git commit -m "feat(tb): add renjuPhase/renjuOffers/renjuSwaps to GameResponse"
```

---

## Milestone C — Board sizing + black-first 15×15 replay

### Task 4: `Game.isRenju()` + `gridSizeForGameType` helper

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/Game.java` (near `isDPente:945`)
- Test (new): `app/src/test/java/be/submanifold/pentelive/GameRenjuUnitTest.java`

`gridSizeForGameType` is a **pure static** helper so board sizing is testable without `BoardView`/Android (`parseGame` needs a `BoardView`).

- [ ] **Step 1: Write the failing test** — new file:

```java
package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameRenjuUnitTest {

    private static Game makeGame(String gameType) {
        return new Game("id", "sid", gameType, "opponent", "1500",
                "white", "5", "Not Rated", "false", "1", "0");
    }

    @Test
    public void isRenjuTrueForRenjuTypes() {
        assertTrue(makeGame("Renju").isRenju());
        assertTrue(makeGame("Speed Renju").isRenju());
        assertFalse(makeGame("Gomoku").isRenju());
        assertFalse(makeGame("Pente").isRenju());
    }

    @Test
    public void gridSizeForGameTypeIsFifteenForRenju() {
        assertEquals(15, Game.gridSizeForGameType("Renju"));
        assertEquals(15, Game.gridSizeForGameType("Speed Renju"));
        assertEquals(9, Game.gridSizeForGameType("Go (9x9)"));
        assertEquals(13, Game.gridSizeForGameType("Go (13x13)"));
        assertEquals(19, Game.gridSizeForGameType("Pente"));
        assertEquals(19, Game.gridSizeForGameType("Go"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*GameRenjuUnitTest'`
Expected: FAIL — `method isRenju()` / `gridSizeForGameType` not found.

- [ ] **Step 3: Implement** — in `Game.java` add near `isDPente()`:

```java
    public boolean isRenju() {
        Variant v = Variants.fromGameType(getGameType());
        return v != null && v.isRenju();
    }

    /** Pure board-size resolver by server gameName (no Android deps). */
    public static int gridSizeForGameType(String gameType) {
        if (gameType == null) return 19;
        if (gameType.contains("Renju")) return 15;
        if (gameType.contains("(9x9)")) return 9;
        if (gameType.contains("(13x13)")) return 13;
        return 19;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*GameRenjuUnitTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/Game.java app/src/test/java/be/submanifold/pentelive/GameRenjuUnitTest.java
git commit -m "feat(tb): Game.isRenju() + pure gridSizeForGameType helper"
```

### Task 5: `replayRenjuGame` — black-first, 15×15 decode (+ golden)

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/Game.java` (add worker near `replayGomokuGame:1528`)
- Test (new): `app/src/test/java/be/submanifold/pentelive/RenjuReplayTest.java`
- Golden (new): `rules/src/test/resources/golden/renju_blackfirst.txt`

Renju has no captures (like Gomoku). Differences from `replayGomokuGame`: black-first colour `2 - (i%2)` (i=0 → value 2 = black) and `gridSize` decode (not `/19`). The test sets `gridSize=15` by reflection (parseGame is Android-bound), then drives the worker exactly like `GameCharacterizationTest`.

Move list `[112, 113, 127]` on 15×15: i=0 center (7,7) BLACK(2); i=1 (8,7) WHITE(1) [113=7+7·15+1 → col8,row7]; i=2 (12,8) BLACK(2) [127=12+...; 127%15=7? compute: 127=8·15+7 → col7,row8]. Use exact decode `col=m%15, row=m/15`: 112→(c7,r7); 113→(c8,r7); 127→(c7,r8).

- [ ] **Step 1: Write the failing test** — new file `RenjuReplayTest.java`:

```java
package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

/** Characterization for replayRenjuGame: black-first, 15x15 decode. */
public class RenjuReplayTest {

    private static Game makeGame(String t) {
        return new Game("id","sid",t,"opponent","1500","white","5","Not Rated","false","1","0");
    }
    private static void setMoves(Game g, Integer... m) throws Exception {
        Field f = Game.class.getDeclaredField("mMovesList");
        f.setAccessible(true); f.set(g, new ArrayList<>(Arrays.asList(m)));
    }
    private static void setGridSize(Game g, int gs) throws Exception {
        Field f = Game.class.getDeclaredField("gridSize");
        f.setAccessible(true); f.setInt(g, gs);
    }
    private static byte[][] board(Game g) throws Exception {
        Field f = Game.class.getDeclaredField("abstractBoard");
        f.setAccessible(true); return (byte[][]) f.get(g);
    }

    @Test
    public void centerMoveIsBlackAndDecodedAt7x7() throws Exception {
        Game g = makeGame("Renju");
        setGridSize(g, 15);
        setMoves(g, 112, 113, 127);
        Method m = Game.class.getDeclaredMethod("replayRenjuGame", int.class);
        m.setAccessible(true); m.invoke(g, 3);
        byte[][] b = board(g);
        assertEquals("center 112 -> (row7,col7) BLACK", 2, b[7][7]); // i=0 black
        assertEquals("113 -> (row7,col8) WHITE", 1, b[7][8]);        // i=1 white
        assertEquals("127 -> (row8,col7) BLACK", 2, b[8][7]);        // i=2 black
    }

    @Test
    public void matchesGolden() throws Exception {
        Game g = makeGame("Renju");
        setGridSize(g, 15);
        setMoves(g, 112, 113, 127);
        Method m = Game.class.getDeclaredMethod("replayRenjuGame", int.class);
        m.setAccessible(true); m.invoke(g, 3);
        StringBuilder sb = new StringBuilder();
        byte[][] b = board(g);
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) { if (j>0) sb.append(' '); sb.append(b[i][j]); }
            sb.append('\n');
        }
        Path dir = null;
        for (Path p = Paths.get("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("rules/src/test/resources/golden");
            if (Files.isDirectory(c)) { dir = c; break; }
        }
        if (dir == null) fail("golden dir not found");
        String expected = new String(Files.readAllBytes(dir.resolve("renju_blackfirst.txt")));
        assertEquals(expected, sb.toString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*RenjuReplayTest'`
Expected: FAIL — `replayRenjuGame` not found.

- [ ] **Step 3: Implement the worker** — in `Game.java` after `replayGomokuGame`:

```java
    private void replayRenjuGame(int until) {
        resetAbstractBoard();
        for (int i = 0; i < Math.min(mMovesList.size(), until); i++) {
            byte color = (byte) (2 - (i % 2)); // black-first: i=0 -> 2 (black)
            int move = mMovesList.get(i), moveI = move / gridSize, moveJ = move % gridSize;
            abstractBoard[moveI][moveJ] = color;
        }
    }
```

- [ ] **Step 4: Create the golden file** — `rules/src/test/resources/golden/renju_blackfirst.txt`, 15 rows × 15 cols, all `0` except row7 col7=`2`, row7 col8=`1`, row8 col7=`2`. (Row 0 first.) Content (space-separated, exactly 15 values per line, 15 lines):

```
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 2 1 0 0 0 0 0 0
0 0 0 0 0 0 0 2 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*RenjuReplayTest'`
Expected: PASS (both tests). If the golden mismatches, the assertion prints expected vs actual — fix the file to match actual only after confirming the 3 cell asserts in `centerMoveIsBlackAndDecodedAt7x7` pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/Game.java app/src/test/java/be/submanifold/pentelive/RenjuReplayTest.java rules/src/test/resources/golden/renju_blackfirst.txt
git commit -m "feat(tb): replayRenjuGame black-first 15x15 decode + golden"
```

### Task 6: Wire Renju into parseGame sizing + dispatch chains

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/Game.java` (`parseGame:997-1010`, dispatch `:1323-1362` and `:1470-1504`, read-fields after Gson parse near `:1021`)

This task touches Android-bound `parseGame` (needs `BoardView`) → verified by `assembleDebug` + manual QA, not unit tests (the pure pieces are already covered by Tasks 4–5).

- [ ] **Step 1: Add the Renju sizing branch** — in `parseGame`, **outside** the Go-only `if` block (`:997-1010`), add a sibling:

```java
        if (isRenju()) {
            this.gridSize = 15;
            boardView.gridSize = 15;
        }
```

- [ ] **Step 2: Store the read phase** — after the Gson parse where `dPenteState` is consumed (`:1021-1026`), capture the Renju fields into instance fields. First add fields near `swap2Choice:81`:

```java
    public String renjuPhase = null;
    public int[] renjuOffers = null;
    public Integer renjuSwaps = null;
```

Then in `parseGame`:

```java
        if (mGameJson.renjuPhase != null) {
            this.renjuPhase = mGameJson.renjuPhase;
            this.renjuSwaps = mGameJson.renjuSwaps;
            if (mGameJson.renjuOffers != null && !mGameJson.renjuOffers.isEmpty()) {
                String[] parts = mGameJson.renjuOffers.split(",");
                this.renjuOffers = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    this.renjuOffers[i] = Integer.parseInt(parts[i].trim());
                }
            }
        }
```

- [ ] **Step 3: Add Renju arms to both dispatch chains** — in `replayGameUntilMove` (`:1323-1362`), mirror the Gomoku arm:

```java
        } else if (getGameType().contains("Renju")) {
            boardView.setBackgroundColor(boardView.renjuColor);
            if (!delegable) replayRenjuGame(untilMove);
```

In the incremental dispatch (`:1470-1504`), add a Renju arm that replays from the move list (Renju has no incremental capture state — re-run the until-worker for the full list):

```java
        } else if (getGameType().contains("Renju")) {
            boardView.setBackgroundColor(boardView.renjuColor);
            replayRenjuGame(mMovesList.size());
```

(`boardView.renjuColor` is added in Task 7 — sequence Task 7 before compiling this, or add the colour constant first.)

- [ ] **Step 4: Verify compile**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (after Task 7's `renjuColor` exists). If run before Task 7, expect `cannot find symbol renjuColor` — do Task 7 first or together.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/Game.java
git commit -m "feat(tb): Renju board sizing, phase read, replay/colour dispatch"
```

---

## Milestone D — TB submission (`renjuAction`)

### Task 7: `BoardView.renjuColor` (needed by Task 6 dispatch)

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/BoardView.java:37-43`

- [ ] **Step 1: Add the colour constant** — append to the colour list:

```java
    public int renjuColor = Color.parseColor("#D98880");
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/BoardView.java
git commit -m "feat(tb): add renjuColor #D98880 to BoardView"
```

### Task 8: `buildSubmitMoveUrl` helper + `renjuAction` on the TB submit path

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/Game.java` (`submitMove:915`, `SubmitMoveTask:533-539`)
- Test: `app/src/test/java/be/submanifold/pentelive/GameRenjuUnitTest.java`

Extract URL building into a **pure static** method so the `renjuAction` contract is TDD-able (the AsyncTask itself is not). `BoardActivity` calls `game.submitMove(...)` → `SubmitMoveTask`, so this is the canonical path.

- [ ] **Step 1: Write the failing test** — add to `GameRenjuUnitTest`:

```java
@Test
public void buildSubmitMoveUrlOmitsRenjuActionWhenNull() {
    String url = Game.buildSubmitMoveUrl("", "999", "130", "hi", null);
    assertTrue(url.contains("command=move"));
    assertTrue(url.contains("gid=999"));
    assertTrue(url.contains("moves=130"));
    assertFalse(url.contains("renjuAction"));
}

@Test
public void buildSubmitMoveUrlAppendsRenjuAction() {
    String url = Game.buildSubmitMoveUrl("", "999", "0,113", "", "swap");
    assertTrue(url.contains("moves=0,113"));
    assertTrue(url.contains("renjuAction=swap"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*GameRenjuUnitTest'`
Expected: FAIL — `buildSubmitMoveUrl` not found.

- [ ] **Step 3: Implement** — in `Game.java`:

```java
    /** Pure builder for the TB move URL. renjuAction omitted when null/empty. */
    public static String buildSubmitMoveUrl(String hideStr, String gid, String moves,
                                            String message, String renjuAction) {
        String url = "https://www.pente.org/gameServer/tb/game?command=move" + hideStr
                + "&mobile=&gid=" + gid + "&moves=" + moves + "&message=" + message
                + "&name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
        if (renjuAction != null && !renjuAction.isEmpty()) {
            url += "&renjuAction=" + renjuAction;
        }
        return url;
    }
```

Refactor `SubmitMoveTask.doInBackground` (`:533-539`) to use it, threading a `renjuAction` field on the task:

```java
        URL url = new URL(buildSubmitMoveUrl(hideStr, mGameID, move, message, renjuAction));
```

Add an overload + field so the existing 2-arg `submitMove` keeps working:

```java
    public void submitMove(String move, String message) {
        submitMove(move, message, null);
    }
    public void submitMove(String move, String message, String renjuAction) {
        SubmitMoveTask submitTask = new SubmitMoveTask(move, message, renjuAction);
        submitTask.execute((Void) null);
    }
```

Update `SubmitMoveTask`'s constructor to accept and store `renjuAction` (keep the old 2-arg ctor delegating with `null` if other callers use it — grep `new SubmitMoveTask`).

- [ ] **Step 4: Run test + compile**

Run: `./gradlew :app:testDebugUnitTest --tests '*GameRenjuUnitTest'` then `./gradlew assembleDebug`
Expected: PASS + BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/Game.java app/src/test/java/be/submanifold/pentelive/GameRenjuUnitTest.java
git commit -m "feat(tb): buildSubmitMoveUrl + renjuAction on TB submit path"
```

### Task 9: `OkHttpPenteApi.submitMove(..., renjuAction)` overload

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/net/OkHttpPenteApi.java:163-176` (+ interface if one exists — grep `submitMove` in `net/`)
- Test: `app/src/test/java/be/submanifold/pentelive/net/OkHttpPenteApiTest.java`

Keep this overload for parity even though `BoardActivity` uses `Game.submitMove`; do not break the existing 3-arg signature.

- [ ] **Step 1: Write the failing test** — add to `OkHttpPenteApiTest`:

```java
@Test
public void submitMoveAppendsRenjuAction() throws Exception {
    server.setDispatcher(new Dispatcher() {
        @Override public MockResponse dispatch(RecordedRequest req) {
            return new MockResponse().setResponseCode(200).setBody("ok");
        }
    });
    api.submitMove("999", "0,113", "", "swap");
    RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
    HttpUrl u = req.getRequestUrl();
    assertEquals("move", u.queryParameter("command"));
    assertEquals("0,113", u.queryParameter("moves"));
    assertEquals("swap", u.queryParameter("renjuAction"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*OkHttpPenteApiTest'`
Expected: FAIL — 4-arg `submitMove` not found.

- [ ] **Step 3: Implement** — add the overload; have the existing 3-arg delegate:

```java
    @Override
    public Result<Void> submitMove(String gid, String moves, String message) {
        return submitMove(gid, moves, message, null);
    }

    public Result<Void> submitMove(String gid, String moves, String message, String renjuAction) {
        HttpUrl.Builder b = base()
                .addPathSegments("gameServer/tb/game")
                .addQueryParameter("command", "move")
                .addQueryParameter("mobile", "")
                .addQueryParameter("gid", gid)
                .addQueryParameter("moves", moves)
                .addQueryParameter("message", message)
                .addQueryParameter("name2", session.name())
                .addQueryParameter("password2", session.password());
        if (renjuAction != null && !renjuAction.isEmpty()) {
            b.addQueryParameter("renjuAction", renjuAction);
        }
        Request request = get(b.build());
        return withReauth(() -> attemptVoid(request));
    }
```

If `submitMove` is declared in an interface, add the 4-arg as a `default` method there or only on the concrete class (the test calls the concrete `api`). Grep for the interface first.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*OkHttpPenteApiTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/net/OkHttpPenteApi.java app/src/test/java/be/submanifold/pentelive/net/OkHttpPenteApiTest.java
git commit -m "feat(tb): OkHttpPenteApi.submitMove renjuAction overload"
```

---

## Milestone E — D4 symmetry dedup (offer validation, UX)

### Task 10: `RenjuSymmetry` pure helper

**Files:**
- Create: `rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java`
- Test (new): `rules/src/test/java/be/submanifold/pente/rules/RenjuSymmetryTest.java`

Algorithm (§10.5): for move `m` on 15×15, `x=m%15, y=m/15, dx=x-7, dy=y-7`; the 8 D4 images of `(dx,dy)` are `(dx,dy),(-dy,dx),(-dx,-dy),(dy,-dx),(-dx,dy),(dx,-dy),(dy,dx),(-dy,-dx)`, mapped back `m'=(tx+7)+(ty+7)·15`. An offer set is valid iff no offer's image set hits an already-accepted offer.

- [ ] **Step 1: Write the failing test** — new file:

```java
package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class RenjuSymmetryTest {

    @Test
    public void eightImagesOfOffsetOneZero() {
        // (dx,dy)=(1,0) about center 112 → images at the 4 axis/diagonal neighbours' orbit.
        int center = 112; // (7,7)
        int east = center + 1;      // (8,7) dx=1,dy=0
        int[] imgs = RenjuSymmetry.d4Images(east);
        // contains west(6,7), north(7,6), south(7,8) as part of the 4-fold orbit
        assertTrue(contains(imgs, center - 1));   // (6,7)
        assertTrue(contains(imgs, center - 15));  // (7,6)
        assertTrue(contains(imgs, center + 15));  // (7,8)
    }

    @Test
    public void detectsSymmetricDuplicateAcross() {
        // (8,7) and (6,7) are D4-symmetric → adding both is a duplicate.
        int east = 113, west = 111;
        assertTrue(RenjuSymmetry.isSymmetricDup(west, new int[]{east}));
        assertFalse(RenjuSymmetry.isSymmetricDup(113 + 30, new int[]{east})); // far point ok
    }

    @Test
    public void validOfferSetOfTenHasNoSymmetricDup() {
        int[] offers = {113,114,115,116,128,129,130,131,144,145};
        assertTrue(RenjuSymmetry.isValidOfferSet(offers));
    }

    @Test
    public void offerSetWithSymmetricPairIsInvalid() {
        int[] offers = {113, 111}; // east + west = symmetric
        assertFalse(RenjuSymmetry.isValidOfferSet(offers));
    }

    private static boolean contains(int[] a, int v) {
        return Arrays.stream(a).anyMatch(x -> x == v);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :rules:test --tests '*RenjuSymmetryTest'`
Expected: FAIL — `RenjuSymmetry` not found.

- [ ] **Step 3: Implement** — new file:

```java
package be.submanifold.pente.rules;

import java.util.LinkedHashSet;
import java.util.Set;

/** D4 (dihedral-8) symmetry helpers for Renju Branch-B offer dedup. 15x15, center (7,7). */
public final class RenjuSymmetry {

    private static final int N = 15;
    private static final int C = 7;

    private RenjuSymmetry() {}

    /** The up-to-8 D4 images of a move index (in-bounds only, deduped). */
    public static int[] d4Images(int move) {
        int x = move % N, y = move / N;
        int dx = x - C, dy = y - C;
        int[][] orbit = {
                {dx, dy}, {-dy, dx}, {-dx, -dy}, {dy, -dx},
                {-dx, dy}, {dx, -dy}, {dy, dx}, {-dy, -dx}
        };
        Set<Integer> out = new LinkedHashSet<>();
        for (int[] o : orbit) {
            int tx = o[0] + C, ty = o[1] + C;
            if (tx >= 0 && tx < N && ty >= 0 && ty < N) {
                out.add(tx + ty * N);
            }
        }
        int[] arr = new int[out.size()];
        int i = 0;
        for (int v : out) arr[i++] = v;
        return arr;
    }

    /** True if `move` (any of its 8 images) collides with an already-accepted offer. */
    public static boolean isSymmetricDup(int move, int[] accepted) {
        for (int img : d4Images(move)) {
            for (int a : accepted) {
                if (img == a) return true;
            }
        }
        return false;
    }

    /** True if no two offers in the set are D4-symmetric duplicates. */
    public static boolean isValidOfferSet(int[] offers) {
        for (int i = 0; i < offers.length; i++) {
            int[] prior = new int[i];
            System.arraycopy(offers, 0, prior, 0, i);
            if (isSymmetricDup(offers[i], prior)) return false;
        }
        return true;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :rules:test --tests '*RenjuSymmetryTest'`
Expected: PASS. (If `eightImagesOfOffsetOneZero` fails, verify the orbit math by hand against the 3 asserted neighbours before changing the test.)

- [ ] **Step 5: Commit**

```bash
git add rules/src/main/java/be/submanifold/pente/rules/RenjuSymmetry.java rules/src/test/java/be/submanifold/pente/rules/RenjuSymmetryTest.java
git commit -m "feat(rules): RenjuSymmetry D4 offer dedup helper"
```

---

## Milestone F — BoardView rendering (15×15)

### Task 11: Renju star points + 15-letter coordinates

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/BoardView.java` (`drawBoard:463-488`, `coordinateLetters:71`, label build `:364-406`)

Android Canvas — verified by `assembleDebug` + manual QA (no unit harness for `View`).

- [ ] **Step 1: Star points for 15×15** — in `drawBoard`, the non-Go `else` branch hardcodes `6*step`. Add a Renju branch using distance **3** (`{3,7,11}`, center 7). Before the `else`:

```java
        } else if (game != null && game.isRenju()) {
            linePaint.setStyle(Paint.Style.FILL);
            float r = margin / 2;
            canvas.drawCircle(margin + 3 * step, margin + 3 * step, r, linePaint);
            canvas.drawCircle(size - (margin + 3 * step), margin + 3 * step, r, linePaint);
            canvas.drawCircle(margin + 3 * step, size - (margin + 3 * step), r, linePaint);
            canvas.drawCircle(size - (margin + 3 * step), size - (margin + 3 * step), r, linePaint);
            canvas.drawCircle(size / 2, size / 2, r, linePaint);
```

- [ ] **Step 2: Coordinate labels use `% gridSize` + first 15 letters** — the label build at `:364-406` uses `coordinateLetters[m % 19]` and `19 - (m / 19)`. `coordinateLetters` already lists A–T skipping I; the first 15 are A–P skipping I — exactly what Renju needs. Replace the hardcoded `19` with `gridSize` in the label arithmetic. Change `coordinateLetters[m % 19]` → `coordinateLetters[m % gridSize]` and `19 - (m / 19)` → `gridSize - (m / gridSize)`.

- [ ] **Step 3: Verify compile + manual render**

Run: `./gradlew assembleDebug`
Then manual QA (see Manual Verification section): open a TB Renju game, confirm 15×15 grid, dusty-rose background, 5 star points at {3,7,11}², center stone (move 1) renders **black**, coordinate labels read A–P / 1–15.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/BoardView.java
git commit -m "feat(tb): Renju 15x15 star points + coordinate labels"
```

---

## Milestone G — Opening UI: swap windows (the existing-pattern part)

### Task 12: `BoardActivity` SWAP-window control (read `renjuPhase`)

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/BoardActivity.java` (`onCreate:65-220`, `setRegularSubmitListener:239-337`)

Mirror the dPente/swap2 read-server-field → show-control → `submitMove` shape. SWAP phase: "Swap (take over)" → `submitMove("1", msg, "swap")`; "Don't swap" → in windows 1–3 tap the highlighted central square then submit `submitMove("0," + playedMove, msg, "swap")`; at move 4 a decline carries no stone → `submitMove("0", msg, "swap")`. Window number derives from `game.getMovesList().size()` (1→after move1 … 4→after move4).

This is Android UI — verified by build + manual QA. Reuse `R.id.dPenteLayout`/the bottom-gravity buttons as the styling precedent; add a Renju gating block that shows the swap control when `"SWAP".equals(game.renjuPhase)`.

- [ ] **Step 1: Gate the swap control on `renjuPhase`** — in the opening-UI wiring (where dPente/swap2 controls are made visible), add:

```java
        if (game.isRenju() && "SWAP".equals(game.renjuPhase)) {
            // reuse the two-button opening layout (R.id.dPenteLayout) for the swap prompt
            showRenjuSwapPrompt(); // helper added below
        }
```

- [ ] **Step 2: Implement `showRenjuSwapPrompt` + submit wiring** — add a helper that wires the two opening buttons:

```java
    private void showRenjuSwapPrompt() {
        View layout = findViewById(R.id.dPenteLayout);
        if (layout != null) layout.setVisibility(View.VISIBLE);
        int window = game.getMovesList().size(); // 1..4
        Button takeOver = findViewById(R.id.playAsBlackButton); // relabel at runtime
        Button decline = findViewById(R.id.playAsWhite);
        if (takeOver != null) {
            takeOver.setText(R.string.renju_swap_take_over);
            takeOver.setOnClickListener(v ->
                game.submitMove("1", messageText(), "swap"));
        }
        if (decline != null) {
            decline.setText(R.string.renju_dont_swap);
            decline.setOnClickListener(v -> {
                if (window >= 4) {
                    game.submitMove("0", messageText(), "swap"); // move-4 decline: no stone
                } else if (board.playedMove > -1) {
                    game.submitMove("0," + board.playedMove, messageText(), "swap");
                } else {
                    Toast.makeText(this, getString(R.string.no_momve_played_yet),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private String messageText() {
        return ((EditText) messageView.findViewById(R.id.messageInput)).getText().toString();
    }
```

Add string resources to `app/src/main/res/values/strings.xml` (and `values-de/` German per project convention):

```xml
    <string name="renju_swap_take_over">Swap (take over)</string>
    <string name="renju_dont_swap">Don\'t swap</string>
```

- [ ] **Step 3: Constrain the decline stone to the central box** — for windows 1–3 the bundled stone must fall in the N×N square (move 2/3/4 → 3×3/5×5/7×7). Gate the board tap: in `BoardView.onTouchEvent`, when a Renju placement-box is active, reject taps outside it (set a field `renjuBoxRadius` from `BoardActivity` = window: 1→1,2→2,3→3). Minimal: store `board.renjuBoxRadius` and in `onTouchEvent` after computing `stoneI/stoneJ` reject if `Math.abs(stoneJ-7) > renjuBoxRadius || Math.abs(stoneI-7) > renjuBoxRadius` (when radius > 0). The highlight draw is Task 13.

```java
    // BoardView field
    public int renjuBoxRadius = 0; // 0 = no constraint
```

```java
    // in onTouchEvent, after stoneI/stoneJ computed, before accepting:
    if (renjuBoxRadius > 0 &&
        (Math.abs(stoneJ - 7) > renjuBoxRadius || Math.abs(stoneI - 7) > renjuBoxRadius)) {
        return true; // tap outside the legal central square: ignore
    }
```

Set `board.renjuBoxRadius` in `showRenjuSwapPrompt` for windows 1–3 (`= window`), and clear to 0 after submit.

- [ ] **Step 4: Verify compile + manual QA**

Run: `./gradlew assembleDebug`
Manual QA: a TB Renju game in `renjuPhase=SWAP` shows the two-button prompt; take-over sends `&moves=1&renjuAction=swap`; decline in window 1 with a tapped in-box stone sends `&moves=0,<m>&renjuAction=swap`; an out-of-box tap is ignored.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/BoardActivity.java app/src/main/java/be/submanifold/pentelive/BoardView.java app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(tb): Renju SWAP-window opening control"
```

---

## Milestone H — Opening UI: branch / offer / selection (the new part)

### Task 13: Central-box highlight + translucent candidate rendering

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/BoardView.java` (`drawBoard`)

- [ ] **Step 1: Draw the central-box highlight** — in `drawBoard`, after stones, when `renjuBoxRadius > 0`, shade the legal N×N cells (a translucent overlay rectangle from `(7-r)` to `(7+r)` in grid coords). Use a dedicated `Paint` with low alpha:

```java
        if (game != null && game.isRenju() && renjuBoxRadius > 0) {
            Paint boxPaint = new Paint();
            boxPaint.setColor(Color.parseColor("#3300FF00"));
            int lo = 7 - renjuBoxRadius, hi = 7 + renjuBoxRadius;
            float left = margin + (lo - 0.5f) * step, top = margin + (lo - 0.5f) * step;
            float right = margin + (hi + 0.5f) * step, bottom = margin + (hi + 0.5f) * step;
            canvas.drawRect(left, top, right, bottom, boxPaint);
        }
```

- [ ] **Step 2: Render translucent candidates** — add a `public java.util.List<Integer> renjuCandidates` field; in the draw loop, after the normal stones, draw each candidate as translucent black (value 4 path — `drawStone` already does `setAlpha(180)`):

```java
        if (game != null && game.isRenju() && renjuCandidates != null) {
            for (int m : renjuCandidates) {
                int ci = m / gridSize, cj = m % gridSize;
                float cx = margin + cj * step, cy = margin + ci * step;
                drawStone(canvas, cx, cy, (byte) 4); // translucent black
            }
        }
```

- [ ] **Step 3: Verify compile + manual render**

Run: `./gradlew assembleDebug`
Manual QA: setting `renjuBoxRadius=4` shades the 9×9; adding candidate indices renders translucent black stones.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/BoardView.java
git commit -m "feat(tb): central-box highlight + translucent Renju candidates"
```

### Task 14: BRANCH / OFFERS / SELECTION controls in `BoardActivity`

**Files:**
- Modify: `app/src/main/java/be/submanifold/pentelive/BoardActivity.java`

Phase → control → submission (uses `RenjuSymmetry` from Task 10 for instant offer feedback):
- `BRANCH`: two buttons — "Continue (place 5th)" → `submitMove("1", msg, "branch")`; "Offer 10" → `submitMove("2", msg, "branch")`.
- `MOVE`: Branch-A move 5 — a **plain** move with NO renjuAction. Constrain to 9×9 (`renjuBoxRadius=4`). Submit `submitMove("" + playedMove, msg)` (2-arg → null action).
- `OFFERS`: 10-pick multi-select (whole board, minus occupied + D4 dup), `n/10`, submit `submitMove(join(picks), msg, "offer")`.
- `SELECTION`: render `game.renjuOffers` as candidates; tap one → `submitMove("" + picked, msg, "select")`.

- [ ] **Step 1: BRANCH control** — gate on `"BRANCH".equals(game.renjuPhase)`:

```java
        if (game.isRenju() && "BRANCH".equals(game.renjuPhase)) {
            View layout = findViewById(R.id.dPenteLayout);
            if (layout != null) layout.setVisibility(View.VISIBLE);
            Button a = findViewById(R.id.playAsBlackButton);
            Button b = findViewById(R.id.playAsWhite);
            if (a != null) { a.setText(R.string.renju_branch_continue);
                a.setOnClickListener(v -> game.submitMove("1", messageText(), "branch")); }
            if (b != null) { b.setText(R.string.renju_branch_offer);
                b.setOnClickListener(v -> game.submitMove("2", messageText(), "branch")); }
        }
```

- [ ] **Step 2: MOVE (Branch-A move 5)** — gate on `"MOVE".equals(game.renjuPhase)`; set `board.renjuBoxRadius = 4` (9×9) and let the normal submit button send a plain move. In `setRegularSubmitListener`, add an early arm:

```java
            } else if (game.isRenju() && "MOVE".equals(game.renjuPhase)) {
                if (board.playedMove == -1) {
                    Toast.makeText(BoardActivity.this, getString(R.string.no_momve_played_yet),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                moves = "" + board.playedMove; // plain move, no renjuAction
```

- [ ] **Step 3: OFFERS multi-select** — gate on `"OFFERS".equals(game.renjuPhase)`; set `board.renjuBoxRadius = 0` (whole board). Maintain a pick list on `BoardView` (`renjuPicks`), tap toggles add/remove via `RenjuSymmetry.isSymmetricDup` check, show `n/10`. Submit button validates exactly 10 + `RenjuSymmetry.isValidOfferSet`, then:

```java
            } else if (game.isRenju() && "OFFERS".equals(game.renjuPhase)) {
                if (board.renjuPicks.size() != 10 ||
                        !RenjuSymmetry.isValidOfferSet(toIntArray(board.renjuPicks))) {
                    Toast.makeText(BoardActivity.this, getString(R.string.renju_need_10_offers),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                moves = join(board.renjuPicks);
                game.submitMove(moves, messageText(), "offer");
                return;
```

Add `BoardView.renjuPicks` (a `List<Integer>`) and toggle logic in `onTouchEvent` when a flag `renjuMultiSelect` is set; render picks as translucent candidates (Task 13 path). Add private `toIntArray`/`join` helpers in `BoardActivity`.

- [ ] **Step 4: SELECTION control** — gate on `"SELECTION".equals(game.renjuPhase)`; set `board.renjuCandidates = listOf(game.renjuOffers)` so they render translucent; a tap on a candidate sets `playedMove`; submit:

```java
            } else if (game.isRenju() && "SELECTION".equals(game.renjuPhase)) {
                if (board.playedMove == -1 || !contains(game.renjuOffers, board.playedMove)) {
                    Toast.makeText(BoardActivity.this, getString(R.string.renju_pick_one),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                game.submitMove("" + board.playedMove, messageText(), "select");
                return;
```

- [ ] **Step 5: Add string resources** (en + de):

```xml
    <string name="renju_branch_continue">Continue (place 5th move)</string>
    <string name="renju_branch_offer">Offer 10</string>
    <string name="renju_need_10_offers">Pick exactly 10 non-symmetric offers</string>
    <string name="renju_pick_one">Tap one of the offered moves</string>
```

- [ ] **Step 6: Verify compile + manual QA (full opening walkthrough)**

Run: `./gradlew assembleDebug`
Manual QA against a live backend: play a full TB Renju opening — swap windows 1–4, Branch A (continue → 9×9 move 5), Branch B (offer 10 → opponent selects). Confirm each submission's query tail matches §12.4 (`renjuAction` + `moves`), and the server accepts (no "Invalid move").

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/be/submanifold/pentelive/BoardActivity.java app/src/main/java/be/submanifold/pentelive/BoardView.java app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(tb): Renju BRANCH/OFFERS/SELECTION opening controls"
```

---

## Final Verification

- [ ] **Full test suite green**

Run: `./gradlew :rules:test :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all new + existing tests pass (no golden regressions in the other variants).

- [ ] **Release-quality compile**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Manual QA matrix** (real backend, TB Renju game id 81; and a historic 31/32 game shipping `renjuPhase=COMPLETE`):
  - Board renders 15×15, dusty-rose `#D98880`, 5 star points {3,7,11}², coords A–P / 1–15.
  - Move 1 (center 112) renders **black**.
  - `SWAP`: take-over and decline+place each round-trip correctly; out-of-box taps rejected.
  - `BRANCH` → A (9×9 move 5 plain) and B (offer 10) both accepted.
  - `OFFERS`: 10-pick with D4 dedup; <10 or symmetric → blocked client-side; valid 10 accepted.
  - `SELECTION`: tap one offered candidate → accepted.
  - `COMPLETE` / historic game: renders, normal play continues, no opening controls shown.

---

## Self-Review (completed by plan author)

**Spec coverage (§12 of `docs/renju-handoff.md`):**
- §12.3.1 rules registry → Tasks 1–2. §12.3.2 GameResponse fields → Task 3. §12.3.3 sizing/phase-read/replay/decode/submit → Tasks 4–8. §12.3.4 OkHttp overload → Task 9. §12.3.5 BoardView (colour/stars/coords/translucent) → Tasks 7, 11, 13. §12.3.6 BoardActivity opening UI → Tasks 12, 14. §10.5 D4 dedup → Task 10. §12.5 box highlight + translucent candidates → Tasks 12–13.
- **Deliberately deferred to Phase 2 (live, §10):** all `dsg*RenjuTaraguchi*` echo handling, `RenjuState`/`GameState` tracking, `LiveGameRoomActivity`/`LiveTableFragment`/`LiveBoardView`, `Table.java` live decode/colour/currentColor. Not in this plan.
- **`renjuSwaps` decode:** intentionally not decoded (server ships resolved `renjuPhase`); stored only, per §12.2.

**Open verify items carried to Manual QA:** exact `gameName` string (resolved: "Renju"/"Speed Renju"); which submit path fires (`Game.submitMove` → `SubmitMoveTask`, wired in Task 8; OkHttp overload in Task 9 for parity); `MOVE` is a plain move with no `renjuAction` (resolved per §12 "Could NOT confirm → RESOLVED"); coordinate-label set (first 15 of existing array); star-point layout {3,7,11}.

**Placeholder scan:** no TBD/TODO; every code step shows code; golden file contents given in full.

**Type consistency:** `gridSizeForGameType`/`buildSubmitMoveUrl` static signatures match their tests; `RenjuSymmetry.d4Images/isSymmetricDup/isValidOfferSet` names match the test; `renjuPhase`(String)/`renjuOffers`(int[])/`renjuSwaps`(Integer) consistent across Game + GameResponse(String offers); `BoardView.renjuBoxRadius/renjuCandidates/renjuPicks/renjuColor` consistent across Tasks 7/12/13/14.
