# Extract PenteRules Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Extract a pure, JVM-testable rules engine (PenteRules) out of the 2996-line Game god class, with views pulling an immutable BoardState.

**Architecture:** A new pure-Java `:rules` Gradle module (package `be.submanifold.pente.rules`) with zero Android imports — the compiler enforces the seam. A deep interface `replay(moves, variant, untilMove) -> BoardState` plus `isWin(...)` hides all capture/win detection. Full pull-model: `Game` holds the latest `BoardState` and exposes `getState()`; its public board/capture fields are deleted and views derive bg-colour/red-dot from the state. A `Variant` descriptor becomes the single classifier, replacing Game's string predicates and Table's game-id predicates. Characterization goldens are captured from the current code BEFORE extraction.

**Tech Stack:** Java, Gradle (new `:rules` module), JUnit 4.13.2.

---

## File Structure

- Create: `settings.gradle` (add `include ':rules'`), `rules/build.gradle` (pure-Java library)
- Create: `rules/src/main/java/be/submanifold/pente/rules/` — `Variant.java`, `CaptureRule.java`, `Variants.java`, `BoardState.java`, `PenteRules.java`, `DefaultPenteRules.java`
- Create: `rules/src/test/java/be/submanifold/pente/rules/` — unit tests (equivalence, capture, win, BoardState)
- Create: `app/src/test/java/be/submanifold/pentelive/GameCharacterizationTest.java` + `app/src/test/resources/golden/`
- Modify: `app/build.gradle` (depend on `:rules`)
- Modify: `app/src/main/java/be/submanifold/pentelive/Game.java` (delegate to PenteRules; add `getState()`; delete public fields; route predicates through `Variants`; delete dead detect*/replay*)
- Modify: `app/src/main/java/be/submanifold/pentelive/BoardView.java`, `BoardActivity.java`, `liveGameRoom/Table.java`, `liveGameRoom/LiveBoardView.java` (pull from `getState()`)

---

### Task 0: Create the `:rules` pure-Java Gradle module

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/build.gradle` (new file)
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/test/java/be/submanifold/pente/rules/SmokeTest.java` (new file — Test path)
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/settings.gradle` (line 1 — currently only `include ':app'`)
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/build.gradle` (dependencies block, lines 76–94)

Environment confirmed from real files: Gradle 9.4.1 (`gradle/wrapper/gradle-wrapper.properties`), AGP 9.2.1 (root `build.gradle` line 13), root project name defaults to `penteLive-Android` (no `rootProject.name` in `settings.gradle`). App module declares no `compileOptions`, so the new module pins `JavaVersion.VERSION_17` (the JDK required by Gradle 9.4.1 / AGP 9) for determinism.

- [ ] **Step 1: Write the failing smoke test (real code).** Create the test source tree and file. Run: `mkdir -p /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/test/java/be/submanifold/pente/rules /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules` then write `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/test/java/be/submanifold/pente/rules/SmokeTest.java`:
  ```java
  package be.submanifold.pente.rules;

  import static org.junit.Assert.assertTrue;

  import org.junit.Test;

  public class SmokeTest {
      @Test
      public void moduleIsWired() {
          assertTrue(true);
      }
  }
  ```

- [ ] **Step 2: Run the test — expect FAIL (module not registered).** Exact command: `./gradlew :rules:test`. Expected output: build fails with `Project 'rules' not found in root project 'penteLive-Android'.` (the `:rules` project does not yet exist in `settings.gradle`).

- [ ] **Step 3: Add the `:rules` build script (minimal real implementation).** Write `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/build.gradle`:
  ```groovy
  plugins {
      id 'java-library'
  }

  java {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
  }

  dependencies {
      testImplementation 'junit:junit:4.13.2'
  }

  tasks.named('test') {
      useJUnit()
  }
  ```

- [ ] **Step 4: Register the module in `settings.gradle`.** Edit `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/settings.gradle`. Replace the entire current content (`include ':app'`) with:
  ```groovy
  include ':app'
  include ':rules'
  ```

- [ ] **Step 5: Wire `:rules` into the app module.** Edit `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/build.gradle`. In the `dependencies {` block (line 76), insert the project dependency as the first entry. Change:
  ```groovy
  dependencies {
      implementation 'androidx.activity:activity:1.12.2'
  ```
  to:
  ```groovy
  dependencies {
      implementation project(':rules')
      implementation 'androidx.activity:activity:1.12.2'
  ```

- [ ] **Step 6: Run the test — expect PASS.** Exact command: `./gradlew :rules:test`. Expected output: `BUILD SUCCESSFUL`; `SmokeTest > moduleIsWired` passes (1 test, 0 failures). Report at `rules/build/reports/tests/test/index.html`.

- [ ] **Step 7: Verify both projects are registered and the app config resolves the new dependency.** Exact command: `./gradlew projects`. Expected output lists `+--- Project ':app'` and `+--- Project ':rules'` under root project `penteLive-Android` (`BUILD SUCCESSFUL`). This confirms the `include ':rules'` wiring without requiring the NDK/`google-services.json` of a full `:app:compileDebugJavaWithJavac`.

- [ ] **Step 8: Commit only the files this task touched.** Exact command:
  ```bash
  git add rules/build.gradle rules/src/test/java/be/submanifold/pente/rules/SmokeTest.java settings.gradle app/build.gradle && git commit -m "Add :rules pure-Java Gradle module with smoke test

  Introduce a zero-Android-dependency :rules module (package
  be.submanifold.pente.rules) pinned to Java 17, wired into the app via
  implementation project(':rules'). SmokeTest proves the module builds and
  runs under ./gradlew :rules:test.

  Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
  ```

---

### Task 1: Variant + CaptureRule + Variants classifier (descriptor table = single source of truth)

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/build.gradle`
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/CaptureRule.java`
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/Variant.java`
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/Variants.java`
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/settings.gradle` (line 1: only contains `include ':app'`)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/test/java/be/submanifold/pente/rules/VariantsTest.java`

Derived mapping (Table.java:54-83 `gameNames`, isGo Table.java:185-187, isDPente 266-268, isSwap2 270-272, capture calls 199-211; Game.java:885-918 string predicates). Even id = "Speed" sibling of canonical odd id. Capture rule composed from `detectPenteCapture`(all but 5/6/13/14), `detectKeryoPenteCapture`(3/4/17/18/25/26/29/30), `detectPoof`(11/12/25/26), `detectKeryoPoof`(25/26):

| canonical id | label | Variant | grid | capture | stones |
|---|---|---|---|---|---|
|1|Pente|PENTE|19|PENTE_PAIR|1|
|3|Keryo-Pente|KERYO_PENTE|19|KERYO_TRIO|1|
|5|Gomoku|GOMOKU|19|NONE|1|
|7|D-Pente|D_PENTE|19|PENTE_PAIR|1|
|9|G-Pente|G_PENTE|19|PENTE_PAIR|1|
|11|Poof-Pente|POOF_PENTE|19|POOF|1|
|13|Connect6|CONNECT6|19|NONE|2|
|15|Boat-Pente|BOAT_PENTE|19|PENTE_PAIR|1|
|17|DK-Pente|DK_PENTE|19|KERYO_TRIO|1|
|19|Go|GO_19|19|NONE|1|
|21|Go (9x9)|GO_9|9|NONE|1|
|23|Go (13x13)|GO_13|13|NONE|1|
|25|O-Pente|O_PENTE|19|KERYO_POOF|1|
|27|Swap2-Pente|SWAP2_PENTE|19|PENTE_PAIR|1|
|29|Swap2-Keryo|SWAP2_KERYO|19|KERYO_TRIO|1|

- [ ] **Step 1: Branch + register the `:rules` pure-Java module.** Run `git checkout -b rules-module`. Edit `settings.gradle` from `include ':app'` to:
```gradle
include ':app'
include ':rules'
```
Create `rules/build.gradle` with exactly:
```gradle
plugins {
    id 'java-library'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation 'junit:junit:4.13.2'
}

test {
    useJUnit()
}
```
(Root `build.gradle` `allprojects` already supplies `mavenCentral()`, so JUnit resolves. The `:app` dependency on `:rules` is deferred to the task that introduces `BoardState`/`Game.replayGame` wiring — no app code consumes the module yet.)

- [ ] **Step 2: Write the failing equivalence + descriptor test (REAL code).** Create `rules/src/test/java/be/submanifold/pente/rules/VariantsTest.java`:
```java
package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VariantsTest {

    // (gameId, server label, expected variant) mirroring Table.gameNames (Table.java:54-83).
    private static final Object[][] CASES = {
            {1, "Pente", Variant.PENTE},
            {2, "Speed Pente", Variant.PENTE},
            {3, "Keryo-Pente", Variant.KERYO_PENTE},
            {4, "Speed Keryo-Pente", Variant.KERYO_PENTE},
            {5, "Gomoku", Variant.GOMOKU},
            {6, "Speed Gomoku", Variant.GOMOKU},
            {7, "D-Pente", Variant.D_PENTE},
            {8, "Speed D-Pente", Variant.D_PENTE},
            {9, "G-Pente", Variant.G_PENTE},
            {10, "Speed G-Pente", Variant.G_PENTE},
            {11, "Poof-Pente", Variant.POOF_PENTE},
            {12, "Speed Poof-Pente", Variant.POOF_PENTE},
            {13, "Connect6", Variant.CONNECT6},
            {14, "Speed Connect6", Variant.CONNECT6},
            {15, "Boat-Pente", Variant.BOAT_PENTE},
            {16, "Speed Boat-Pente", Variant.BOAT_PENTE},
            {17, "DK-Pente", Variant.DK_PENTE},
            {18, "Speed DK-Pente", Variant.DK_PENTE},
            {19, "Go", Variant.GO_19},
            {20, "Speed Go", Variant.GO_19},
            {21, "Go (9x9)", Variant.GO_9},
            {22, "Speed Go (9x9)", Variant.GO_9},
            {23, "Go (13x13)", Variant.GO_13},
            {24, "Speed Go (13x13)", Variant.GO_13},
            {25, "O-Pente", Variant.O_PENTE},
            {26, "Speed O-Pente", Variant.O_PENTE},
            {27, "Swap2-Pente", Variant.SWAP2_PENTE},
            {28, "Speed Swap2-Pente", Variant.SWAP2_PENTE},
            {29, "Swap2-Keryo", Variant.SWAP2_KERYO},
            {30, "Speed Swap2-Keryo", Variant.SWAP2_KERYO},
    };

    @Test
    public void fromGameTypeEqualsFromGameIdForEveryKnownPair() {
        for (Object[] c : CASES) {
            int id = (Integer) c[0];
            String label = (String) c[1];
            Variant expected = (Variant) c[2];
            assertEquals("fromGameId(" + id + ")", expected, Variants.fromGameId(id));
            assertEquals("fromGameType(" + label + ")", expected, Variants.fromGameType(label));
            assertEquals(
                    "fromGameType(" + label + ") == fromGameId(" + id + ")",
                    Variants.fromGameId(id),
                    Variants.fromGameType(label));
        }
    }

    @Test
    public void gridSizeMatchesVariant() {
        assertEquals(19, Variants.gridSize(Variant.PENTE));
        assertEquals(19, Variants.gridSize(Variant.BOAT_PENTE));
        assertEquals(19, Variants.gridSize(Variant.KERYO_PENTE));
        assertEquals(19, Variants.gridSize(Variant.G_PENTE));
        assertEquals(19, Variants.gridSize(Variant.POOF_PENTE));
        assertEquals(19, Variants.gridSize(Variant.D_PENTE));
        assertEquals(19, Variants.gridSize(Variant.DK_PENTE));
        assertEquals(19, Variants.gridSize(Variant.O_PENTE));
        assertEquals(19, Variants.gridSize(Variant.SWAP2_PENTE));
        assertEquals(19, Variants.gridSize(Variant.SWAP2_KERYO));
        assertEquals(19, Variants.gridSize(Variant.GOMOKU));
        assertEquals(19, Variants.gridSize(Variant.CONNECT6));
        assertEquals(9, Variants.gridSize(Variant.GO_9));
        assertEquals(13, Variants.gridSize(Variant.GO_13));
        assertEquals(19, Variants.gridSize(Variant.GO_19));
    }

    @Test
    public void captureRuleMatchesVariant() {
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.BOAT_PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.D_PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.G_PENTE));
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.SWAP2_PENTE));
        assertEquals(CaptureRule.KERYO_TRIO, Variants.captureRule(Variant.KERYO_PENTE));
        assertEquals(CaptureRule.KERYO_TRIO, Variants.captureRule(Variant.DK_PENTE));
        assertEquals(CaptureRule.KERYO_TRIO, Variants.captureRule(Variant.SWAP2_KERYO));
        assertEquals(CaptureRule.POOF, Variants.captureRule(Variant.POOF_PENTE));
        assertEquals(CaptureRule.KERYO_POOF, Variants.captureRule(Variant.O_PENTE));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GOMOKU));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.CONNECT6));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GO_9));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GO_13));
        assertEquals(CaptureRule.NONE, Variants.captureRule(Variant.GO_19));
    }

    @Test
    public void stonesPerTurnMatchesVariant() {
        assertEquals(2, Variants.stonesPerTurn(Variant.CONNECT6));
        for (Variant v : Variant.values()) {
            if (v != Variant.CONNECT6) {
                assertEquals("stonesPerTurn(" + v + ")", 1, Variants.stonesPerTurn(v));
            }
        }
    }

    @Test
    public void variantHelpersClassifyCorrectly() {
        assertTrue(Variant.CONNECT6.isConnect6());
        assertTrue(Variant.GOMOKU.isGomoku());
        assertTrue(Variant.SWAP2_PENTE.isSwap2());
        assertTrue(Variant.SWAP2_KERYO.isSwap2());
        assertTrue(Variant.D_PENTE.isDPente());
        assertTrue(Variant.DK_PENTE.isDPente());
        assertTrue(Variant.GO_9.isGo());
        assertTrue(Variant.GO_13.isGo());
        assertTrue(Variant.GO_19.isGo());
        assertFalse(Variant.PENTE.isGo());
        assertFalse(Variant.PENTE.isSwap2());
        assertFalse(Variant.PENTE.isConnect6());
    }

    @Test
    public void unknownInputsReturnNull() {
        assertEquals(null, Variants.fromGameType("Chess"));
        assertEquals(null, Variants.fromGameType(null));
        assertEquals(null, Variants.fromGameId(99));
    }
}
```

- [ ] **Step 3: Run the test, expect compile FAIL (red).** Run `./gradlew :rules:test`. Expected: `BUILD FAILED` with `error: cannot find symbol` referencing `Variant`, `CaptureRule`, and `Variants` (classes do not exist yet).

- [ ] **Step 4a: Implement `CaptureRule.java` (REAL code).** Create `rules/src/main/java/be/submanifold/pente/rules/CaptureRule.java`:
```java
package be.submanifold.pente.rules;

/** The capture mechanic a variant uses. Pure data; no Android imports. */
public enum CaptureRule {
    NONE,
    PENTE_PAIR,
    KERYO_TRIO,
    POOF,
    KERYO_POOF
}
```

- [ ] **Step 4b: Implement `Variant.java` with the descriptor table (REAL code).** Create `rules/src/main/java/be/submanifold/pente/rules/Variant.java`. Enum order matches the shared vocabulary exactly; constructor carries the single-source-of-truth descriptor (canonical odd game id, grid size, capture rule, stones per turn):
```java
package be.submanifold.pente.rules;

/**
 * Every game variant supported by pente.org.
 *
 * <p>Each constant carries its descriptor data, which is the single source of
 * truth for the rules module: the canonical (non-speed) game id, the board grid
 * size, the capture rule, and the number of stones placed per turn.
 *
 * <p>Go variants are classified and sized here for completeness; their rules
 * are out of scope for v1.
 */
public enum Variant {
    PENTE(1, 19, CaptureRule.PENTE_PAIR, 1),
    BOAT_PENTE(15, 19, CaptureRule.PENTE_PAIR, 1),
    KERYO_PENTE(3, 19, CaptureRule.KERYO_TRIO, 1),
    G_PENTE(9, 19, CaptureRule.PENTE_PAIR, 1),
    POOF_PENTE(11, 19, CaptureRule.POOF, 1),
    D_PENTE(7, 19, CaptureRule.PENTE_PAIR, 1),
    DK_PENTE(17, 19, CaptureRule.KERYO_TRIO, 1),
    O_PENTE(25, 19, CaptureRule.KERYO_POOF, 1),
    SWAP2_PENTE(27, 19, CaptureRule.PENTE_PAIR, 1),
    SWAP2_KERYO(29, 19, CaptureRule.KERYO_TRIO, 1),
    GOMOKU(5, 19, CaptureRule.NONE, 1),
    CONNECT6(13, 19, CaptureRule.NONE, 2),
    // TODO(rules): GoRules seam — Go is classified + sized only; no rule engine in v1.
    GO_9(21, 9, CaptureRule.NONE, 1),
    GO_13(23, 13, CaptureRule.NONE, 1),
    GO_19(19, 19, CaptureRule.NONE, 1);

    final int canonicalGameId;
    final int gridSize;
    final CaptureRule captureRule;
    final int stonesPerTurn;

    Variant(int canonicalGameId, int gridSize, CaptureRule captureRule, int stonesPerTurn) {
        this.canonicalGameId = canonicalGameId;
        this.gridSize = gridSize;
        this.captureRule = captureRule;
        this.stonesPerTurn = stonesPerTurn;
    }

    public boolean isConnect6() {
        return this == CONNECT6;
    }

    public boolean isGomoku() {
        return this == GOMOKU;
    }

    public boolean isSwap2() {
        return this == SWAP2_PENTE || this == SWAP2_KERYO;
    }

    public boolean isDPente() {
        return this == D_PENTE || this == DK_PENTE;
    }

    public boolean isGo() {
        return this == GO_9 || this == GO_13 || this == GO_19;
    }
}
```

- [ ] **Step 4c: Implement `Variants.java` classifier (REAL code).** Create `rules/src/main/java/be/submanifold/pente/rules/Variants.java`. `fromGameId` normalizes even (speed) ids to the canonical odd id; `fromGameType` checks most-specific substrings first so overlaps ("Gomoku" contains "Go"; "DK-Pente" is checked before "D-Pente"; "Swap2-Keryo" is caught by the Swap2 branch before "Keryo-Pente"):
```java
package be.submanifold.pente.rules;

import java.util.HashMap;
import java.util.Map;

/** Classifies server game ids and labels into {@link Variant}s, and exposes
 *  the per-variant descriptor data. Pure Java; no Android imports. */
public final class Variants {

    private static final Map<Integer, Variant> BY_CANONICAL_ID = new HashMap<>();

    static {
        for (Variant v : Variant.values()) {
            BY_CANONICAL_ID.put(v.canonicalGameId, v);
        }
    }

    private Variants() {
    }

    /**
     * Classifies a server game-type label (e.g. "Pente", "Speed Keryo-Pente",
     * "Go (9x9)") into a {@link Variant}. The leading "Speed " qualifier is
     * irrelevant. Checks are ordered most-specific first so that substring
     * overlaps do not misclassify. Returns null for unknown or null input.
     */
    public static Variant fromGameType(String gameType) {
        if (gameType == null) {
            return null;
        }
        if (gameType.contains("Swap2")) {
            return gameType.contains("Keryo") ? Variant.SWAP2_KERYO : Variant.SWAP2_PENTE;
        }
        if (gameType.contains("Connect6")) {
            return Variant.CONNECT6;
        }
        if (gameType.contains("Gomoku")) {
            return Variant.GOMOKU;
        }
        if (gameType.contains("Keryo-Pente")) {
            return Variant.KERYO_PENTE;
        }
        if (gameType.contains("DK-Pente")) {
            return Variant.DK_PENTE;
        }
        if (gameType.contains("D-Pente")) {
            return Variant.D_PENTE;
        }
        if (gameType.contains("Boat-Pente")) {
            return Variant.BOAT_PENTE;
        }
        if (gameType.contains("G-Pente")) {
            return Variant.G_PENTE;
        }
        if (gameType.contains("Poof-Pente")) {
            return Variant.POOF_PENTE;
        }
        if (gameType.contains("O-Pente")) {
            return Variant.O_PENTE;
        }
        if (gameType.contains("Pente")) {
            return Variant.PENTE;
        }
        if (gameType.contains("Go (9x9)")) {
            return Variant.GO_9;
        }
        if (gameType.contains("Go (13x13)")) {
            return Variant.GO_13;
        }
        if (gameType.contains("Go")) {
            return Variant.GO_19;
        }
        return null;
    }

    /**
     * Maps a numeric game id to a {@link Variant}. Even ids are the "Speed"
     * sibling of the canonical odd id (canonical + 1). Returns null for
     * unknown ids.
     */
    public static Variant fromGameId(int gameId) {
        int canonical = (gameId % 2 == 0) ? gameId - 1 : gameId;
        return BY_CANONICAL_ID.get(canonical);
    }

    public static int gridSize(Variant v) {
        return v.gridSize;
    }

    public static CaptureRule captureRule(Variant v) {
        return v.captureRule;
    }

    public static int stonesPerTurn(Variant v) {
        return v.stonesPerTurn;
    }
}
```

- [ ] **Step 5: Run the test, expect PASS (green).** Run `./gradlew :rules:test`. Expected: `BUILD SUCCESSFUL`, all 6 tests pass (0 failures), report at `rules/build/reports/tests/test/index.html`.

- [ ] **Step 6: Commit.** Run:
```bash
git add settings.gradle rules/build.gradle rules/src/main/java/be/submanifold/pente/rules/CaptureRule.java rules/src/main/java/be/submanifold/pente/rules/Variant.java rules/src/main/java/be/submanifold/pente/rules/Variants.java rules/src/test/java/be/submanifold/pente/rules/VariantsTest.java
git commit -m "Add :rules module with Variant/CaptureRule/Variants classifier

Descriptor table in the Variant enum is the single source of truth for
grid size, capture rule, and stones-per-turn. Equivalence test asserts
fromGameType == fromGameId across all 30 known game ids.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 2: BoardState immutable value type

**Files:**
- Create: `rules/src/main/java/be/submanifold/pente/rules/BoardState.java`
- Test: `rules/src/test/java/be/submanifold/pente/rules/BoardStateTest.java`
- (Depends on Task 1: the `:rules` pure-Java module with `junit:junit:4.13.2` on `testImplementation` and package `be.submanifold.pente.rules` already exists.)

- [ ] **Step 1: Write failing test `BoardStateTest`.** Create `rules/src/test/java/be/submanifold/pente/rules/BoardStateTest.java` with real code:
```java
package be.submanifold.pente.rules;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BoardStateTest {

    private static byte[][] grid3() {
        return new byte[][] {
                {0, 1, 2},
                {2, 0, 1},
                {1, 2, 0}
        };
    }

    @Test
    public void storesAllScalarFields() {
        BoardState s = new BoardState(grid3(), 4, 6, 3, 5,
                Integer.valueOf(1), true, false, 7);
        assertEquals(4, s.whiteCaptures);
        assertEquals(6, s.blackCaptures);
        assertEquals(3, s.gridSize);
        assertEquals(5, s.lastMove);
        assertEquals(Integer.valueOf(1), s.winner);
        assertTrue(s.swap2DecisionPoint);
        assertFalse(s.dPenteDecisionPoint);
        assertEquals(7, s.koMove);
    }

    @Test
    public void cellReturnsBoardValueAtCoordinate() {
        BoardState s = new BoardState(grid3(), 0, 0, 3, -1,
                null, false, false, -1);
        assertEquals((byte) 0, s.cell(0, 0));
        assertEquals((byte) 1, s.cell(0, 1));
        assertEquals((byte) 2, s.cell(0, 2));
        assertEquals((byte) 2, s.cell(1, 0));
        assertEquals((byte) 1, s.cell(2, 0));
        assertEquals((byte) 2, s.cell(2, 1));
    }

    @Test
    public void winnerIsNullableAndDefaultsToNoWinner() {
        BoardState s = new BoardState(grid3(), 0, 0, 3, -1,
                null, false, false, -1);
        assertNull(s.winner);
    }

    @Test
    public void constructorDefensivelyCopiesBoard() {
        byte[][] input = grid3();
        BoardState s = new BoardState(input, 0, 0, 3, -1,
                null, false, false, -1);
        // Mutate the caller's array AND its rows after construction.
        input[0][1] = (byte) 9;
        input[1] = new byte[] {7, 7, 7};
        assertEquals("cell(0,1) must reflect original value, not mutation",
                (byte) 1, s.cell(0, 1));
        assertArrayEquals("row 1 must be the original, deep-copied row",
                new byte[] {2, 0, 1}, s.board[1]);
    }

    @Test
    public void exposedBoardMutationDoesNotLeakBackIntoCellSemantics() {
        BoardState s = new BoardState(grid3(), 0, 0, 3, -1,
                null, false, false, -1);
        // board is a public final field; this test pins current expected shape.
        assertEquals(3, s.board.length);
        assertEquals(3, s.board[0].length);
    }
}
```

- [ ] **Step 2: Run the test, expect FAIL (compile error: `BoardState` does not exist).** Run: `./gradlew :rules:test`. Expected: FAILURE — `error: cannot find symbol class BoardState`.

- [ ] **Step 3: Implement `BoardState` (minimal, real).** Create `rules/src/main/java/be/submanifold/pente/rules/BoardState.java`:
```java
package be.submanifold.pente.rules;

/**
 * Immutable snapshot of a Pente-family board position.
 *
 * Cell encoding in {@link #board}: 0 empty, 1 white, 2 black, -1 forbidden.
 * Pente-only by design; Go positions are out of scope for v1.
 * // TODO(rules): GoRules seam — Go captures/territory are not modeled here.
 */
public final class BoardState {

    public final byte[][] board;
    public final int whiteCaptures;
    public final int blackCaptures;
    public final int gridSize;
    public final int lastMove;
    /** Winning color (1 white, 2 black) or {@code null} when there is no winner yet. */
    public final Integer winner;
    public final boolean swap2DecisionPoint;
    public final boolean dPenteDecisionPoint;
    public final int koMove;

    public BoardState(byte[][] board,
                      int whiteCaptures,
                      int blackCaptures,
                      int gridSize,
                      int lastMove,
                      Integer winner,
                      boolean swap2DecisionPoint,
                      boolean dPenteDecisionPoint,
                      int koMove) {
        this.board = deepCopy(board);
        this.whiteCaptures = whiteCaptures;
        this.blackCaptures = blackCaptures;
        this.gridSize = gridSize;
        this.lastMove = lastMove;
        this.winner = winner;
        this.swap2DecisionPoint = swap2DecisionPoint;
        this.dPenteDecisionPoint = dPenteDecisionPoint;
        this.koMove = koMove;
    }

    /** Value at row {@code i}, column {@code j}. */
    public byte cell(int i, int j) {
        return board[i][j];
    }

    private static byte[][] deepCopy(byte[][] src) {
        byte[][] copy = new byte[src.length][];
        for (int i = 0; i < src.length; i++) {
            byte[] row = src[i];
            copy[i] = new byte[row.length];
            System.arraycopy(row, 0, copy[i], 0, row.length);
        }
        return copy;
    }
}
```

- [ ] **Step 4: Run the test, expect PASS.** Run: `./gradlew :rules:test`. Expected: BUILD SUCCESSFUL, all 5 tests in `BoardStateTest` green.

- [ ] **Step 5: Commit.** Run:
```bash
git add rules/src/main/java/be/submanifold/pente/rules/BoardState.java rules/src/test/java/be/submanifold/pente/rules/BoardStateTest.java
git commit -m "Add immutable BoardState value type to :rules module

BoardState is a Pente-only immutable snapshot: board byte[][] (0 empty,
1 white, 2 black, -1 forbidden), capture counts, gridSize, lastMove,
nullable winner, swap2/d-pente decision flags, and koMove. Constructor
defensively deep-copies the board so post-construction mutation of the
caller's array cannot affect the snapshot.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 3: Characterization goldens harness for `Game`'s current replay path (pre-extraction)

**Files:**
- Read-only reference (DO NOT modify in this task): `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/Game.java` — dispatcher `replayGameUntilMove` (1283-1376), per-variant private replays `replayGomokuGame(int)` 1435, `replayPenteGame(int)` 1444, `replayKeryoPenteGame(int)` 1522, `replayConnect6Game(int)` 1598, `replayPoofPenteGame(int)` 1640, capture logic `detectPenteCapture` 1676, `detectKeryoPenteCapture` 1784, `detectPoof` 1900, win expression at 1462, `detectPente(byte,int)` 2220, public fields `abstractBoard` 77 / `whiteCaptures` 56 / `blackCaptures` 57, `getGridSize()` 2380.
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/GoldenSerializer.java`
- Create (TEST): `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/GameReplayGoldenTest.java`
- Create (generated, committed): `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/resources/golden/{pente_pair_capture,pente_capture_win,keryo_trio_capture,poof_self_capture,gomoku_opening,connect6_opening,swap2_decision_point}.txt`

> No `build.gradle` change needed: `testImplementation 'junit:junit:4.13.2'` (line 93) is already present and sibling tests (`ArenaEventsTest`) already run under `:app:testDebugUnitTest`. `BoardView`/Android cannot be instantiated in a JVM unit test, so the harness drives the SAME per-variant private `replay*Game(int)` methods that `replayGameUntilMove` (1289-1325) dispatches to — via reflection — and snapshots the public fields they mutate (`abstractBoard`, `whiteCaptures`, `blackCaptures`). This is the load-bearing replay computation; the Android-only `BoardView` colouring/`Toast` in `replayGame(BoardView)` carries no board state.

- [ ] **Step 1: Write the pure golden serializer (REAL code).** Create `GoldenSerializer.java` with the exact contents:
```java
package be.submanifold.pentelive;

/** Pure (no-Android) renderer of a replayed board snapshot to deterministic golden text. */
final class GoldenSerializer {

    private GoldenSerializer() {}

    static String serialize(String variant, int moveCount, byte[][] board,
                            int whiteCaptures, int blackCaptures, Integer winner) {
        StringBuilder sb = new StringBuilder();
        sb.append("variant=").append(variant).append('\n');
        sb.append("moves=").append(moveCount).append('\n');
        sb.append("whiteCaptures=").append(whiteCaptures).append('\n');
        sb.append("blackCaptures=").append(blackCaptures).append('\n');
        sb.append("winner=").append(winner == null ? "none" : String.valueOf(winner)).append('\n');
        sb.append("board:").append('\n');
        for (byte[] row : board) {
            for (byte cell : row) {
                sb.append(cellChar(cell));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static char cellChar(byte v) {
        switch (v) {
            case 1: return 'W';   // white
            case 2: return 'B';   // black
            case -1: return 'X';  // forbidden
            default: return '.';  // empty
        }
    }
}
```

- [ ] **Step 2: Write the failing characterization test (REAL code, goldens absent).** Create `GameReplayGoldenTest.java` with the exact contents:
```java
package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Characterization goldens for Game's CURRENT replay path, captured BEFORE any rules extraction so
 * the extracted engine (DefaultPenteRules) can later be proven behavior-preserving against them.
 *
 * BoardView/Android cannot be constructed in a JVM unit test, so we invoke the same per-variant
 * private replay*Game(int) methods that Game.replayGameUntilMove dispatches to, via reflection, and
 * snapshot the public board + capture fields they mutate.
 *
 * If a golden file is missing the test WRITES it from current behavior and FAILS, asking you to
 * eyeball the snapshot then rerun. Once committed, assertEquals locks the behavior.
 */
public class GameReplayGoldenTest {

    private static final File GOLDEN_DIR = new File("src/test/resources/golden");

    /** gameType, opponentName, private replay method, golden file, moves encoded as row*19+col. */
    private static final class Fixture {
        final String variant;
        final String opponent;
        final String replayMethod;
        final String goldenFile;
        final int[] moves;
        Fixture(String variant, String opponent, String replayMethod, String goldenFile, int[] moves) {
            this.variant = variant;
            this.opponent = opponent;
            this.replayMethod = replayMethod;
            this.goldenFile = goldenFile;
            this.moves = moves;
        }
    }

    private static final Fixture[] FIXTURES = {
        // Pente: white(9,9)+white(9,12) bracket and capture black pair (9,10)(9,11) -> blackCaptures=2.
        new Fixture("Pente", "rival", "replayPenteGame", "pente_pair_capture.txt",
                new int[]{180, 181, 140, 182, 183}),
        // Capture-win: black captures 5 white pairs in disjoint columns 1,5,9,13,17 -> whiteCaptures=10 (black wins).
        new Fixture("Pente", "rival", "replayPenteGame", "pente_capture_win.txt",
                new int[]{58, 39, 77, 96, 62, 43, 81, 100, 66, 47, 85, 104, 70, 51, 89, 108, 74, 55, 93, 112}),
        // Keryo-Pente: white(9,9)+white(9,13) bracket and capture black trio (9,10)(9,11)(9,12) -> blackCaptures=3.
        new Fixture("Keryo-Pente", "rival", "replayKeryoPenteGame", "keryo_trio_capture.txt",
                new int[]{180, 181, 133, 182, 134, 183, 184}),
        // Poof-Pente: white(9,9) self-poofs the flanked white(8,9) between black(7,9)/black(10,9) -> whiteCaptures=1.
        new Fixture("Poof-Pente", "rival", "replayPoofPenteGame", "poof_self_capture.txt",
                new int[]{161, 142, 18, 199, 180}),
        // Gomoku: pure stone placement, no captures.
        new Fixture("Gomoku", "rival", "replayGomokuGame", "gomoku_opening.txt",
                new int[]{180, 179, 161, 160, 142}),
        // Connect6: 1-then-2 stone cadence, no captures.
        new Fixture("Connect6", "rival", "replayConnect6Game", "connect6_opening.txt",
                new int[]{180, 181, 161, 160, 200, 199}),
        // Swap2-Pente decision point: snapshot after the 3 placement moves (replayPenteGame path).
        new Fixture("Swap2-Pente", "rival", "replayPenteGame", "swap2_decision_point.txt",
                new int[]{180, 181, 182}),
    };

    @Test
    public void replayMatchesGoldens() throws Exception {
        List<String> regenerated = new ArrayList<>();
        for (Fixture f : FIXTURES) {
            String actual = snapshot(f);
            File golden = new File(GOLDEN_DIR, f.goldenFile);
            if (!golden.exists()) {
                GOLDEN_DIR.mkdirs();
                Files.write(golden.toPath(), actual.getBytes(StandardCharsets.UTF_8));
                regenerated.add(f.goldenFile);
                continue;
            }
            String expected = new String(Files.readAllBytes(golden.toPath()), StandardCharsets.UTF_8);
            assertEquals("golden mismatch for " + f.goldenFile, expected, actual);
        }
        if (!regenerated.isEmpty()) {
            fail("Wrote missing goldens " + regenerated + " from current behavior. Inspect them under "
                    + GOLDEN_DIR.getPath() + " and rerun to lock.");
        }
    }

    private String snapshot(Fixture f) throws Exception {
        Game game = new Game(null, null, f.variant, f.opponent, null, "white",
                null, null, null, null, null);

        List<Integer> movesList = new ArrayList<>();
        for (int m : f.moves) {
            movesList.add(m);
        }
        Field movesField = Game.class.getDeclaredField("mMovesList");
        movesField.setAccessible(true);
        movesField.set(game, movesList);

        Method replay = Game.class.getDeclaredMethod(f.replayMethod, int.class);
        replay.setAccessible(true);
        replay.invoke(game, f.moves.length);

        Integer winner = computeWinner(game, f.moves);
        return GoldenSerializer.serialize(f.variant, f.moves.length,
                game.abstractBoard, game.whiteCaptures, game.blackCaptures, winner);
    }

    /** Mirrors Game.replayPenteGame's win expression (Game.java:1462): capture==10 or 5-in-a-row. */
    private Integer computeWinner(Game game, int[] moves) throws Exception {
        if (game.whiteCaptures >= 10) {
            return 2; // 10 white stones captured -> black wins
        }
        if (game.blackCaptures >= 10) {
            return 1; // 10 black stones captured -> white wins
        }
        if (moves.length == 0) {
            return null;
        }
        byte lastColor = (byte) (2 - (moves.length % 2)); // color of the player who just moved
        int lastMove = moves[moves.length - 1];
        Method detectPente = Game.class.getDeclaredMethod("detectPente", byte.class, int.class);
        detectPente.setAccessible(true);
        boolean line = (Boolean) detectPente.invoke(game, lastColor, lastMove);
        return line ? (int) lastColor : null;
    }
}
```

- [ ] **Step 3: Run the harness — expect FAIL (RED, goldens missing → written + fail()).** Run:
```bash
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.GameReplayGoldenTest"
```
Expected: BUILD FAILED. `replayMatchesGoldens` fails with `Wrote missing goldens [pente_pair_capture.txt, pente_capture_win.txt, keryo_trio_capture.txt, poof_self_capture.txt, gomoku_opening.txt, connect6_opening.txt, swap2_decision_point.txt] from current behavior. Inspect them under src/test/resources/golden and rerun to lock.` The 7 `.txt` files now exist under `app/src/test/resources/golden/`.

- [ ] **Step 4: Eyeball the generated goldens (sanity-check the locked behavior).** Read each file and confirm it matches the fixture intent:
```bash
ls /Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/resources/golden/
```
Then Read `pente_pair_capture.txt` (expect `blackCaptures=2`, `whiteCaptures=0`, `winner=none`, row 9 cells for cols 10/11 = `.`, cols 9/12 = `W`), `pente_capture_win.txt` (expect `whiteCaptures=10`, `winner=2`, all `W` cells in rows 3-4 cleared), `keryo_trio_capture.txt` (`blackCaptures=3`, `winner=none`), `poof_self_capture.txt` (`whiteCaptures=1`, `winner=none`, cells (8,9)/(9,9) = `.`). These are the committed source of truth — if any value is surprising, stop and investigate before locking.

- [ ] **Step 5: Re-run the harness — expect PASS (GREEN, goldens present and equal).** Run:
```bash
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.GameReplayGoldenTest"
```
Expected: BUILD SUCCESSFUL, `replayMatchesGoldens` passes (every fixture's serialized snapshot equals its committed golden).

- [ ] **Step 6: Confirm full app unit-test suite still green (no regression to sibling tests).** Run:
```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL — `GameReplayGoldenTest`, `ArenaEventsTest`, and `ExampleUnitTest` all pass.

- [ ] **Step 7: Commit the harness + goldens (REAL git commands).** Run:
```bash
git checkout -b rules-characterization-goldens
git add app/src/test/java/be/submanifold/pentelive/GoldenSerializer.java app/src/test/java/be/submanifold/pentelive/GameReplayGoldenTest.java app/src/test/resources/golden/
git commit -m "Add characterization goldens for Game replay before rules extraction

Snapshot board+captures+winner from the current per-variant replay*Game(int)
methods (Pente pair capture, Pente capture-win, Keryo trio, Poof self-capture,
Gomoku, Connect6, Swap2 decision point) so the upcoming DefaultPenteRules
extraction can be proven behavior-preserving.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 4: PenteRules capture rules — test-first against the goldens

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/test/java/be/submanifold/pente/rules/CaptureRulesTest.java` (new — 32 per-direction capture tests, 8 per rule)
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/test/java/be/submanifold/pente/rules/GoldenReplayTest.java` (new — 4 full-game golden replays, one per CaptureRule)
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/PenteRules.java` (the engine interface — created in Step 0 below)
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/DefaultPenteRules.java` (skeleton created in Step 0 below; its replay loop has a `// TODO(rules): apply captures (Task 4)` seam after `board[mi][mj] = color;` — Steps 3–18 fill it and add four PRIVATE detect methods before the class's closing brace)
- Test cmd: `./gradlew :rules:test`

Capture-rule → legacy-method mapping (verified against `Game.java` replay dispatch — `replayPenteGame` L1444, `replayKeryoPenteGame` L1490ish, `replayPoofPenteGame` L1640, `replayOPenteGame`): `PENTE_PAIR` = detectPenteCapture; `KERYO_TRIO` = detectPenteCapture then detectKeryoPenteCapture; `POOF` = detectPoof then detectPenteCapture; `KERYO_POOF` = detectPoof, detectKeryoPoof, detectPenteCapture, detectKeryoPenteCapture. **Order is load-bearing — preserve it exactly.** Capture-count semantics from legacy: `whiteCaptures`=count of WHITE stones removed (`caps[0]`), `blackCaptures`=BLACK stones removed (`caps[1]`); pente=+2, keryo=+3; poof self-captures the mover's own stones (+1 in-branch +1 bonus = +2), keryo-poof (+2 in-branch +1 bonus = +3).

- [ ] **Step 0: Create the `PenteRules` interface and the `DefaultPenteRules` skeleton (do this FIRST — every later step modifies this file).** Write `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/PenteRules.java`:
```java
package be.submanifold.pente.rules;

import java.util.List;

/** Deep, pure rules interface — the implementation hides all capture/win detection. */
public interface PenteRules {
    /** Recompute the board from scratch by replaying moves[0..untilMove). */
    BoardState replay(List<Integer> moves, Variant v, int untilMove);

    /** True if {@code color} (1=white, 2=black) has won given the last move. Caller decides WHEN to evaluate. */
    boolean isWin(BoardState s, int color, int lastMove);
}
```
Then write `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/DefaultPenteRules.java` with the capture seam that Steps 3–18 fill in:
```java
package be.submanifold.pente.rules;

import java.util.List;

public final class DefaultPenteRules implements PenteRules {

    @Override
    public BoardState replay(List<Integer> moves, Variant v, int untilMove) {
        int size = Variants.gridSize(v);
        byte[][] board = new byte[size][size];
        int[] caps = new int[2];                 // caps[0] = white stones removed, caps[1] = black stones removed
        int lastMove = -1;
        int n = Math.min(untilMove, moves == null ? 0 : moves.size());
        for (int i = 0; i < n; i++) {
            int m = moves.get(i);
            int mi = m / size;
            int mj = m % size;
            byte color = (byte) ((i % 2 == 0) ? 1 : 2);   // even index = white(1), odd = black(2)
            board[mi][mj] = color;
            lastMove = m;
            // TODO(rules): apply captures (Task 4)
        }
        return new BoardState(board, caps[0], caps[1], size, lastMove, null, false, false, -1);
    }

    @Override
    public boolean isWin(BoardState s, int color, int lastMove) {
        return false;   // implemented in Task 5
    }
}
```
This compiles against `BoardState` (Task 2) and `Variants` (Task 1). With the capture seam still a no-op, the Step 1 tests below compile and run but FAIL on the capture assertions — exactly what Step 2 verifies.

- [ ] **Step 1: Write failing PENTE_PAIR 8-direction tests + pente golden.** Create `CaptureRulesTest.java`:
```java
package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class CaptureRulesTest {
    private final PenteRules rules = new DefaultPenteRules();
    private static int idx(int i, int j) { return i * 19 + j; }
    private BoardState replay(Variant v, int... mv) {
        List<Integer> l = new ArrayList<>();
        for (int m : mv) l.add(m);
        return rules.replay(l, v, l.size());
    }

    // PENTE_PAIR: my-A at distance 3, two opp between; placing F (white) captures both blacks (+2 black).
    @Test public void penteUp()        { BoardState s = replay(Variant.PENTE, idx(6,9), idx(7,9), idx(0,0), idx(8,9), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.whiteCaptures); assertEquals(0, s.cell(7,9)); assertEquals(0, s.cell(8,9)); assertEquals(1, s.cell(6,9)); assertEquals(1, s.cell(9,9)); }
    @Test public void penteUpLeft()    { BoardState s = replay(Variant.PENTE, idx(6,6), idx(7,7), idx(0,0), idx(8,8), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(7,7)); assertEquals(0, s.cell(8,8)); }
    @Test public void penteLeft()      { BoardState s = replay(Variant.PENTE, idx(9,6), idx(9,7), idx(0,0), idx(9,8), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(9,7)); assertEquals(0, s.cell(9,8)); }
    @Test public void penteDownLeft()  { BoardState s = replay(Variant.PENTE, idx(12,6), idx(11,7), idx(0,0), idx(10,8), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(11,7)); }
    @Test public void penteDown()      { BoardState s = replay(Variant.PENTE, idx(12,9), idx(11,9), idx(0,0), idx(10,9), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(11,9)); }
    @Test public void penteDownRight() { BoardState s = replay(Variant.PENTE, idx(12,12), idx(11,11), idx(0,0), idx(10,10), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(11,11)); }
    @Test public void penteRight()     { BoardState s = replay(Variant.PENTE, idx(9,12), idx(9,11), idx(0,0), idx(9,10), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,11)); }
    @Test public void penteUpRight()   { BoardState s = replay(Variant.PENTE, idx(6,12), idx(7,11), idx(0,0), idx(8,10), idx(9,9)); assertEquals(2, s.blackCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(7,11)); }
}
```
And create `GoldenReplayTest.java`:
```java
package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class GoldenReplayTest {
    private final PenteRules rules = new DefaultPenteRules();
    private static int idx(int i, int j) { return i * 19 + j; }
    private BoardState replay(Variant v, int... mv) {
        List<Integer> l = new ArrayList<>();
        for (int m : mv) l.add(m);
        return rules.replay(l, v, l.size());
    }

    // Golden: a real PENTE move-list reproduces board + captures (white flanks a black pair).
    @Test public void goldenPente() {
        BoardState s = replay(Variant.PENTE, idx(9,12), idx(9,11), idx(0,0), idx(9,10), idx(9,9));
        assertEquals(19, s.gridSize);
        assertEquals(idx(9,9), s.lastMove);
        assertNull(s.winner);
        assertEquals(0, s.whiteCaptures);
        assertEquals(2, s.blackCaptures);
        assertEquals(0, s.cell(9,10));
        assertEquals(0, s.cell(9,11));
        assertEquals(1, s.cell(9,9));
        assertEquals(1, s.cell(9,12));
    }
}
```
- [ ] **Step 2: Run and confirm FAIL.** `./gradlew :rules:test --tests "be.submanifold.pente.rules.CaptureRulesTest" --tests "be.submanifold.pente.rules.GoldenReplayTest"` — expect FAIL (assertion errors: captures stay 0 / cells unchanged because the replay capture seam is still a no-op).

- [ ] **Step 3: Implement PENTE_PAIR.** In `DefaultPenteRules.java`, replace the seam line `// TODO(rules): apply captures (Task 4)` inside the replay loop with the capture-dispatch switch:
```java
            CaptureRule rule = Variants.captureRule(v);
            switch (rule) {
                case PENTE_PAIR:
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    break;
                case NONE:
                default:
                    break;
            }
```
(`size`, `board`, `caps` (int[2]), `mi`, `mj`, `color` are the locals from the Task 3 loop; `caps[0]`=white removed, `caps[1]`=black removed; BoardState is built after the loop as `new BoardState(board, caps[0], caps[1], size, lastMove, null, false, false, -1)`.) Then add this PRIVATE method before the class's closing brace:
```java
    private static void detectPenteCapture(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        int oi = (opp == 1) ? 0 : 1;
        if (i - 3 > -1 && b[i-3][j] == my && b[i-1][j] == opp && b[i-2][j] == opp) { b[i-1][j] = 0; b[i-2][j] = 0; caps[oi] += 2; }
        if (i - 3 > -1 && j - 3 > -1 && b[i-3][j-3] == my && b[i-1][j-1] == opp && b[i-2][j-2] == opp) { b[i-1][j-1] = 0; b[i-2][j-2] = 0; caps[oi] += 2; }
        if (j - 3 > -1 && b[i][j-3] == my && b[i][j-1] == opp && b[i][j-2] == opp) { b[i][j-1] = 0; b[i][j-2] = 0; caps[oi] += 2; }
        if (i + 3 < n && j - 3 > -1 && b[i+3][j-3] == my && b[i+1][j-1] == opp && b[i+2][j-2] == opp) { b[i+1][j-1] = 0; b[i+2][j-2] = 0; caps[oi] += 2; }
        if (i + 3 < n && b[i+3][j] == my && b[i+1][j] == opp && b[i+2][j] == opp) { b[i+1][j] = 0; b[i+2][j] = 0; caps[oi] += 2; }
        if (i + 3 < n && j + 3 < n && b[i+3][j+3] == my && b[i+1][j+1] == opp && b[i+2][j+2] == opp) { b[i+1][j+1] = 0; b[i+2][j+2] = 0; caps[oi] += 2; }
        if (j + 3 < n && b[i][j+3] == my && b[i][j+1] == opp && b[i][j+2] == opp) { b[i][j+1] = 0; b[i][j+2] = 0; caps[oi] += 2; }
        if (i - 3 > -1 && j + 3 < n && b[i-3][j+3] == my && b[i-1][j+1] == opp && b[i-2][j+2] == opp) { b[i-1][j+1] = 0; b[i-2][j+2] = 0; caps[oi] += 2; }
    }
```
- [ ] **Step 4: Run and confirm PASS.** `./gradlew :rules:test --tests "be.submanifold.pente.rules.CaptureRulesTest" --tests "be.submanifold.pente.rules.GoldenReplayTest"` — expect BUILD SUCCESSFUL (all 8 pente tests + goldenPente green).
- [ ] **Step 5: Commit.** `git add rules/ && git commit -m "rules: PENTE_PAIR capture replay, 8-direction + golden tests" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

- [ ] **Step 6: Write failing KERYO_TRIO 8-direction tests + keryo golden.** Add to `CaptureRulesTest.java` (before the class's closing brace) — my-A at distance 4, three opp between, two white fillers control parity so black places the captured trio; capturing F is white (+3 black):
```java
    @Test public void keryoUp()        { BoardState s = replay(Variant.KERYO_PENTE, idx(5,9), idx(6,9), idx(0,0), idx(7,9), idx(0,18), idx(8,9), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.whiteCaptures); assertEquals(0, s.cell(6,9)); assertEquals(0, s.cell(7,9)); assertEquals(0, s.cell(8,9)); }
    @Test public void keryoUpLeft()    { BoardState s = replay(Variant.KERYO_PENTE, idx(5,5), idx(6,6), idx(0,0), idx(7,7), idx(0,18), idx(8,8), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(6,6)); assertEquals(0, s.cell(7,7)); assertEquals(0, s.cell(8,8)); }
    @Test public void keryoLeft()      { BoardState s = replay(Variant.KERYO_PENTE, idx(9,5), idx(9,6), idx(0,0), idx(9,7), idx(0,18), idx(9,8), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(9,6)); assertEquals(0, s.cell(9,7)); assertEquals(0, s.cell(9,8)); }
    @Test public void keryoDownLeft()  { BoardState s = replay(Variant.KERYO_PENTE, idx(13,5), idx(12,6), idx(0,0), idx(11,7), idx(0,18), idx(10,8), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(11,7)); assertEquals(0, s.cell(12,6)); }
    @Test public void keryoDown()      { BoardState s = replay(Variant.KERYO_PENTE, idx(13,9), idx(12,9), idx(0,0), idx(11,9), idx(0,18), idx(10,9), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(11,9)); assertEquals(0, s.cell(12,9)); }
    @Test public void keryoDownRight() { BoardState s = replay(Variant.KERYO_PENTE, idx(13,13), idx(12,12), idx(0,0), idx(11,11), idx(0,18), idx(10,10), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(11,11)); assertEquals(0, s.cell(12,12)); }
    @Test public void keryoRight()     { BoardState s = replay(Variant.KERYO_PENTE, idx(9,13), idx(9,12), idx(0,0), idx(9,11), idx(0,18), idx(9,10), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,11)); assertEquals(0, s.cell(9,12)); }
    @Test public void keryoUpRight()   { BoardState s = replay(Variant.KERYO_PENTE, idx(5,13), idx(6,12), idx(0,0), idx(7,11), idx(0,18), idx(8,10), idx(9,9)); assertEquals(3, s.blackCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(7,11)); assertEquals(0, s.cell(6,12)); }
```
And add to `GoldenReplayTest.java`:
```java
    @Test public void goldenKeryo() {
        BoardState s = replay(Variant.KERYO_PENTE, idx(9,13), idx(9,12), idx(0,0), idx(9,11), idx(0,18), idx(9,10), idx(9,9));
        assertEquals(19, s.gridSize);
        assertEquals(idx(9,9), s.lastMove);
        assertNull(s.winner);
        assertEquals(0, s.whiteCaptures);
        assertEquals(3, s.blackCaptures);
        assertEquals(0, s.cell(9,10));
        assertEquals(0, s.cell(9,11));
        assertEquals(0, s.cell(9,12));
        assertEquals(1, s.cell(9,13));
        assertEquals(1, s.cell(9,9));
    }
```
- [ ] **Step 7: Run and confirm FAIL.** `./gradlew :rules:test --tests "be.submanifold.pente.rules.CaptureRulesTest" --tests "be.submanifold.pente.rules.GoldenReplayTest"` — expect FAIL (blackCaptures==0, trio cells unchanged: KERYO_TRIO not dispatched yet).

- [ ] **Step 8: Implement KERYO_TRIO.** In `DefaultPenteRules.java` add a case to the replay switch (before `case NONE:`):
```java
                case KERYO_TRIO:
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    detectKeryoPenteCapture(board, size, mi, mj, color, caps);
                    break;
```
Add this PRIVATE method:
```java
    private static void detectKeryoPenteCapture(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        int oi = (opp == 1) ? 0 : 1;
        if (i - 4 > -1 && b[i-4][j] == my && b[i-1][j] == opp && b[i-2][j] == opp && b[i-3][j] == opp) { b[i-1][j] = 0; b[i-2][j] = 0; b[i-3][j] = 0; caps[oi] += 3; }
        if (i - 4 > -1 && j - 4 > -1 && b[i-4][j-4] == my && b[i-1][j-1] == opp && b[i-2][j-2] == opp && b[i-3][j-3] == opp) { b[i-1][j-1] = 0; b[i-2][j-2] = 0; b[i-3][j-3] = 0; caps[oi] += 3; }
        if (j - 4 > -1 && b[i][j-4] == my && b[i][j-1] == opp && b[i][j-2] == opp && b[i][j-3] == opp) { b[i][j-1] = 0; b[i][j-2] = 0; b[i][j-3] = 0; caps[oi] += 3; }
        if (i + 4 < n && j - 4 > -1 && b[i+4][j-4] == my && b[i+1][j-1] == opp && b[i+2][j-2] == opp && b[i+3][j-3] == opp) { b[i+1][j-1] = 0; b[i+2][j-2] = 0; b[i+3][j-3] = 0; caps[oi] += 3; }
        if (i + 4 < n && b[i+4][j] == my && b[i+1][j] == opp && b[i+2][j] == opp && b[i+3][j] == opp) { b[i+1][j] = 0; b[i+2][j] = 0; b[i+3][j] = 0; caps[oi] += 3; }
        if (i + 4 < n && j + 4 < n && b[i+4][j+4] == my && b[i+1][j+1] == opp && b[i+2][j+2] == opp && b[i+3][j+3] == opp) { b[i+1][j+1] = 0; b[i+2][j+2] = 0; b[i+3][j+3] = 0; caps[oi] += 3; }
        if (j + 4 < n && b[i][j+4] == my && b[i][j+1] == opp && b[i][j+2] == opp && b[i][j+3] == opp) { b[i][j+1] = 0; b[i][j+2] = 0; b[i][j+3] = 0; caps[oi] += 3; }
        if (i - 4 > -1 && j + 4 < n && b[i-4][j+4] == my && b[i-1][j+1] == opp && b[i-2][j+2] == opp && b[i-3][j+3] == opp) { b[i-1][j+1] = 0; b[i-2][j+2] = 0; b[i-3][j+3] = 0; caps[oi] += 3; }
    }
```
- [ ] **Step 9: Run and confirm PASS.** `./gradlew :rules:test --tests "be.submanifold.pente.rules.CaptureRulesTest" --tests "be.submanifold.pente.rules.GoldenReplayTest"` — expect BUILD SUCCESSFUL (16 capture tests + 2 goldens green).
- [ ] **Step 10: Commit.** `git add rules/ && git commit -m "rules: KERYO_TRIO capture replay, 8-direction + golden tests" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

- [ ] **Step 11: Write failing POOF 8-direction tests + poof golden.** Add to `CaptureRulesTest.java` — a white pair (my N + placed F) flanked by two blacks self-poofs (+2 white); both flanking blacks remain:
```java
    @Test public void poofUp()        { BoardState s = replay(Variant.POOF_PENTE, idx(8,9), idx(7,9), idx(0,0), idx(10,9), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.blackCaptures); assertEquals(0, s.cell(8,9)); assertEquals(0, s.cell(9,9)); assertEquals(2, s.cell(7,9)); assertEquals(2, s.cell(10,9)); }
    @Test public void poofUpLeft()    { BoardState s = replay(Variant.POOF_PENTE, idx(8,8), idx(7,7), idx(0,0), idx(10,10), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(8,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofLeft()      { BoardState s = replay(Variant.POOF_PENTE, idx(9,8), idx(9,7), idx(0,0), idx(9,10), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(9,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofDownLeft()  { BoardState s = replay(Variant.POOF_PENTE, idx(10,8), idx(8,10), idx(0,0), idx(11,7), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofDown()      { BoardState s = replay(Variant.POOF_PENTE, idx(10,9), idx(11,9), idx(0,0), idx(8,9), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofDownRight() { BoardState s = replay(Variant.POOF_PENTE, idx(10,10), idx(8,8), idx(0,0), idx(11,11), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofRight()     { BoardState s = replay(Variant.POOF_PENTE, idx(9,10), idx(9,8), idx(0,0), idx(9,11), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,9)); }
    @Test public void poofUpRight()   { BoardState s = replay(Variant.POOF_PENTE, idx(8,10), idx(10,8), idx(0,0), idx(7,11), idx(9,9)); assertEquals(2, s.whiteCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(9,9)); }
```
And add to `GoldenReplayTest.java`:
```java
    @Test public void goldenPoof() {
        BoardState s = replay(Variant.POOF_PENTE, idx(9,10), idx(9,8), idx(0,0), idx(9,11), idx(9,9));
        assertEquals(19, s.gridSize);
        assertEquals(idx(9,9), s.lastMove);
        assertNull(s.winner);
        assertEquals(2, s.whiteCaptures);
        assertEquals(0, s.blackCaptures);
        assertEquals(0, s.cell(9,10));
        assertEquals(0, s.cell(9,9));
        assertEquals(2, s.cell(9,8));
        assertEquals(2, s.cell(9,11));
    }
```
- [ ] **Step 12: Run and confirm FAIL.** `./gradlew :rules:test --tests "be.submanifold.pente.rules.CaptureRulesTest" --tests "be.submanifold.pente.rules.GoldenReplayTest"` — expect FAIL (whiteCaptures==0, mover's pair not removed: POOF not dispatched).

- [ ] **Step 13: Implement POOF.** In `DefaultPenteRules.java` add a case to the replay switch (before `case NONE:`) — detectPoof runs BEFORE detectPenteCapture, matching `replayPoofPenteGame`:
```java
                case POOF:
                    detectPoof(board, size, mi, mj, color, caps);
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    break;
```
Add this PRIVATE method (mover self-captures own pair; `caps[mi]` += 1 per branch, +1 bonus once if any poof):
```java
    private static void detectPoof(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        int mi = (my == 1) ? 0 : 1;
        boolean poofed = false;
        if (i - 2 > -1 && i + 1 < n && b[i-1][j] == my && b[i-2][j] == opp && b[i+1][j] == opp) { b[i-1][j] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 2 > -1 && j - 2 > -1 && i + 1 < n && j + 1 < n && b[i-1][j-1] == my && b[i-2][j-2] == opp && b[i+1][j+1] == opp) { b[i-1][j-1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (j - 2 > -1 && j + 1 < n && b[i][j-1] == my && b[i][j-2] == opp && b[i][j+1] == opp) { b[i][j-1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 1 > -1 && j - 2 > -1 && i + 2 < n && j + 1 < n && b[i+1][j-1] == my && b[i-1][j+1] == opp && b[i+2][j-2] == opp) { b[i+1][j-1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i + 2 < n && i - 1 > -1 && b[i+1][j] == my && b[i+2][j] == opp && b[i-1][j] == opp) { b[i+1][j] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 1 > -1 && j - 1 > -1 && i + 2 < n && j + 2 < n && b[i+1][j+1] == my && b[i-1][j-1] == opp && b[i+2][j+2] == opp) { b[i+1][j+1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (j + 2 < n && j - 1 > -1 && b[i][j+1] == my && b[i][j-1] == opp && b[i][j+2] == opp) { b[i][j+1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (i - 2 > -1 && j - 1 > -1 && i + 1 < n && j + 2 < n && b[i-1][j+1] == my && b[i+1][j-1] == opp && b[i-2][j+2] == opp) { b[i-1][j+1] = 0; b[i][j] = 0; caps[mi]++; poofed = true; }
        if (poofed) caps[mi]++;
    }
```
- [ ] **Step 14: Run and confirm PASS.** `./gradlew :rules:test --tests "be.submanifold.pente.rules.CaptureRulesTest" --tests "be.submanifold.pente.rules.GoldenReplayTest"` — expect BUILD SUCCESSFUL (24 capture tests + 3 goldens green).
- [ ] **Step 15: Commit.** `git add rules/ && git commit -m "rules: POOF capture replay, 8-direction + golden tests" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

- [ ] **Step 16: Write failing KERYO_POOF 8-direction tests + O-Pente golden.** Add to `CaptureRulesTest.java` — a white trio (two my M + placed F) flanked by two blacks self-poofs (+3 white); flanking blacks remain. Uses `Variant.O_PENTE`:
```java
    @Test public void keryoPoofLeft()      { BoardState s = replay(Variant.O_PENTE, idx(8,9), idx(6,9), idx(7,9), idx(10,9), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.blackCaptures); assertEquals(0, s.cell(7,9)); assertEquals(0, s.cell(8,9)); assertEquals(0, s.cell(9,9)); assertEquals(2, s.cell(6,9)); assertEquals(2, s.cell(10,9)); }
    @Test public void keryoPoofUpLeft()    { BoardState s = replay(Variant.O_PENTE, idx(8,8), idx(6,6), idx(7,7), idx(10,10), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(7,7)); assertEquals(0, s.cell(8,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofUp()        { BoardState s = replay(Variant.O_PENTE, idx(9,8), idx(9,6), idx(9,7), idx(9,10), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(9,7)); assertEquals(0, s.cell(9,8)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofUpRight()   { BoardState s = replay(Variant.O_PENTE, idx(10,8), idx(8,10), idx(11,7), idx(12,6), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(10,8)); assertEquals(0, s.cell(11,7)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofRight()     { BoardState s = replay(Variant.O_PENTE, idx(10,9), idx(12,9), idx(11,9), idx(8,9), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(10,9)); assertEquals(0, s.cell(11,9)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofDownRight() { BoardState s = replay(Variant.O_PENTE, idx(10,10), idx(8,8), idx(11,11), idx(12,12), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(10,10)); assertEquals(0, s.cell(11,11)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofDown()      { BoardState s = replay(Variant.O_PENTE, idx(9,10), idx(9,8), idx(9,11), idx(9,12), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(9,10)); assertEquals(0, s.cell(9,11)); assertEquals(0, s.cell(9,9)); }
    @Test public void keryoPoofDownLeft()  { BoardState s = replay(Variant.O_PENTE, idx(8,10), idx(10,8), idx(7,11), idx(6,12), idx(9,9)); assertEquals(3, s.whiteCaptures); assertEquals(0, s.cell(8,10)); assertEquals(0, s.cell(7,11)); assertEquals(0, s.cell(9,9)); }
```
And add to `GoldenReplayTest.java`:
```java
    @Test public void goldenOPente() {
        BoardState s = replay(Variant.O_PENTE, idx(9,10), idx(9,8), idx(9,11), idx(9,12), idx(9,9));
        assertEquals(19, s.gridSize);
        assertEquals(idx(9,9), s.lastMove);
        assertNull(s.winner);
        assertEquals(3, s.whiteCaptures);
        assertEquals(0, s.blackCaptures);
        assertEquals(0, s.cell(9,10));
        assertEquals(0, s.cell(9,11));
        assertEquals(0, s.cell(9,9));
        assertEquals(2, s.cell(9,8));
        assertEquals(2, s.cell(9,12));
    }
```
- [ ] **Step 17: Run and confirm FAIL.** `./gradlew :rules:test --tests "be.submanifold.pente.rules.CaptureRulesTest" --tests "be.submanifold.pente.rules.GoldenReplayTest"` — expect FAIL (whiteCaptures==0, trio not removed: KERYO_POOF not dispatched).

- [ ] **Step 18: Implement KERYO_POOF.** In `DefaultPenteRules.java` add the final case to the replay switch (before `case NONE:`) — order detectPoof, detectKeryoPoof, detectPenteCapture, detectKeryoPenteCapture per `replayOPenteGame`:
```java
                case KERYO_POOF:
                    detectPoof(board, size, mi, mj, color, caps);
                    detectKeryoPoof(board, size, mi, mj, color, caps);
                    detectPenteCapture(board, size, mi, mj, color, caps);
                    detectKeryoPenteCapture(board, size, mi, mj, color, caps);
                    break;
```
Add this PRIVATE method (8 end-placed + 4 mid-placed branches; `caps[mi]` += 2 per branch, +1 bonus once; note legacy guard `j + 2 < n` on the "down" branch is preserved verbatim):
```java
    private static void detectKeryoPoof(byte[][] b, int n, int i, int j, byte my, int[] caps) {
        byte opp = (byte) (1 + (my % 2));
        int mi = (my == 1) ? 0 : 1;
        boolean poofed = false;
        if (i - 3 > -1 && i + 1 < n && b[i-1][j] == my && b[i-2][j] == my && b[i-3][j] == opp && b[i+1][j] == opp) { b[i-2][j] = 0; b[i-1][j] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 3 > -1 && j - 3 > -1 && i + 1 < n && j + 1 < n && b[i-1][j-1] == my && b[i-2][j-2] == my && b[i-3][j-3] == opp && b[i+1][j+1] == opp) { b[i-2][j-2] = 0; b[i-1][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (j - 3 > -1 && j + 1 < n && b[i][j-1] == my && b[i][j-2] == my && b[i][j-3] == opp && b[i][j+1] == opp) { b[i][j-2] = 0; b[i][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 1 > -1 && j - 3 > -1 && i + 3 < n && j + 1 < n && b[i+1][j-1] == my && b[i+2][j-2] == my && b[i-1][j+1] == opp && b[i+3][j-3] == opp) { b[i+2][j-2] = 0; b[i+1][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i + 3 < n && i - 1 > -1 && b[i+1][j] == my && b[i+2][j] == my && b[i+3][j] == opp && b[i-1][j] == opp) { b[i+2][j] = 0; b[i+1][j] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 1 > -1 && j - 1 > -1 && i + 3 < n && j + 3 < n && b[i+1][j+1] == my && b[i+2][j+2] == my && b[i-1][j-1] == opp && b[i+3][j+3] == opp) { b[i+2][j+2] = 0; b[i+1][j+1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (j + 2 < n && j - 1 > -1 && b[i][j+1] == my && b[i][j+2] == my && b[i][j-1] == opp && b[i][j+3] == opp) { b[i][j+1] = 0; b[i][j+2] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 3 > -1 && j - 1 > -1 && i + 1 < n && j + 3 < n && b[i-1][j+1] == my && b[i-2][j+2] == my && b[i+1][j-1] == opp && b[i-3][j+3] == opp) { b[i-2][j+2] = 0; b[i-1][j+1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 2 > -1 && i + 2 < n && b[i-1][j] == my && b[i+1][j] == my && b[i-2][j] == opp && b[i+2][j] == opp) { b[i+1][j] = 0; b[i-1][j] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 2 > -1 && j - 2 > -1 && i + 2 < n && j + 2 < n && b[i-1][j-1] == my && b[i+1][j+1] == my && b[i-2][j-2] == opp && b[i+2][j+2] == opp) { b[i+1][j+1] = 0; b[i-1][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (j - 2 > -1 && j + 2 < n && b[i][j-1] == my && b[i][j+1] == my && b[i][j-2] == opp && b[i][j+2] == opp) { b[i][j+1] = 0; b[i][j-1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (i - 2 > -1 && j - 2 > -1 && i + 2 < n && j + 2 < n && b[i+1][j-1] == my && b[i-1][j+1] == my && b[i-2][j+2] == opp && b[i+2][j-2] == opp) { b[i+1][j-1] = 0; b[i-1][j+1] = 0; b[i][j] = 0; caps[mi] += 2; poofed = true; }
        if (poofed) caps[mi]++;
    }
```
- [ ] **Step 19: Run full module suite and confirm PASS.** `./gradlew :rules:test` — expect BUILD SUCCESSFUL (32 capture tests + 4 goldens green). Then verify the app still compiles against the module: `./gradlew :app:compileDebugJavaWithJavac` — expect BUILD SUCCESSFUL.
- [ ] **Step 20: Commit.** `git add rules/ && git commit -m "rules: KERYO_POOF capture replay, 12-branch impl, 8-direction + golden tests" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

Note: opening-restriction forbidden cells (`rated() && size==2` center 5x5, G-Pente spokes) and winner detection are out of scope here — they depend on state not in `replay(moves, v, untilMove)`; `winner` stays `null` and is handled by the win-detection task (`isWin`). `// TODO(rules): GoRules seam` for GO_* variants remains untouched.

---

### Task 5: PenteRules win detection (five-in-a-row + capture wins + replay winner population)

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/test/java/be/submanifold/pente/rules/WinDetectionTest.java` (new)
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/rules/src/main/java/be/submanifold/pente/rules/DefaultPenteRules.java` — `isWin(...)` method (Task 4 leaves it as a `return false;` stub) and the final `return new BoardState(...)` statement inside `replay(...)` (Task 4 passes `null` for the `winner` arg)
- Test path: `rules/src/test/java/be/submanifold/pente/rules/WinDetectionTest.java`

Notes carried from the real source (`Game.java:2220-2335` `detectPente`, `Game.java:1248-1262` win/winner logic, `Game.java:1683-1687`,`2156-2160` capture counters):
- `detectPente` wins on `penteCounter > 4` (i.e. `>= 5`) — preserve the `>=5` threshold.
- The original loops bound with `i > 0 && i < 19` which silently **excludes row/col 0 and 18** — this task deliberately FIXES that so edges/corners win (`>= 0 && < gridSize`).
- Capture counters name the **losing** color: `whiteCaptures` = white stones lost, so `whiteCaptures == 10` ⇒ **black (2)** wins; `blackCaptures == 10` ⇒ **white (1)** wins (matches `Game.java:1252-1259`).
- `winner` holds the **absolute** winning color (1 or 2); the caller (Game/UI) compares it to "my" color — winner is only meaningful when the caller evaluates it.
- Color of move `k` (1-indexed, white first) is `2 - (k % 2)`.
- BoardState constructor arg order used here matches the vocab field listing: `(byte[][] board, int whiteCaptures, int blackCaptures, int gridSize, int lastMove, Integer winner, boolean swap2DecisionPoint, boolean dPenteDecisionPoint, int koMove)`.

---

**Cycle A — five-in-a-row detection (incl. edges/corners)**

- [ ] **Step 1: Write failing test `WinDetectionTest.java`** with all imports up front (cycle B reuses them) and the five-in-a-row cases. Real code:

```java
package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class WinDetectionTest {

    private final PenteRules rules = new DefaultPenteRules();

    private static int pos(int row, int col) {
        return row * 19 + col;
    }

    /** Pente BoardState from a raw board with no captures and no winner. */
    private static BoardState boardWith(byte[][] board, int lastMove) {
        return new BoardState(board, 0, 0, 19, lastMove, null, false, false, -1);
    }

    private static BoardState captureState(int whiteCaptures, int blackCaptures) {
        return new BoardState(new byte[19][19], whiteCaptures, blackCaptures, 19, -1, null, false, false, -1);
    }

    @Test
    public void horizontalExactlyFiveWins() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 9; c++) {
            b[5][c] = 1;
        }
        assertTrue(rules.isWin(boardWith(b, pos(5, 9)), 1, pos(5, 9)));
    }

    @Test
    public void horizontalSixWins() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 10; c++) {
            b[5][c] = 1;
        }
        assertTrue(rules.isWin(boardWith(b, pos(5, 10)), 1, pos(5, 10)));
    }

    @Test
    public void horizontalFourDoesNotWin() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 8; c++) {
            b[5][c] = 1;
        }
        assertFalse(rules.isWin(boardWith(b, pos(5, 8)), 1, pos(5, 8)));
    }

    @Test
    public void verticalFiveWins() {
        byte[][] b = new byte[19][19];
        for (int r = 5; r <= 9; r++) {
            b[r][7] = 2;
        }
        assertTrue(rules.isWin(boardWith(b, pos(9, 7)), 2, pos(9, 7)));
    }

    @Test
    public void diagonalDownRightFiveWins() {
        byte[][] b = new byte[19][19];
        for (int k = 0; k < 5; k++) {
            b[5 + k][5 + k] = 1;
        }
        assertTrue(rules.isWin(boardWith(b, pos(9, 9)), 1, pos(9, 9)));
    }

    @Test
    public void antiDiagonalFiveWinsFromMiddleStone() {
        byte[][] b = new byte[19][19];
        b[5][9] = 1;
        b[6][8] = 1;
        b[7][7] = 1;
        b[8][6] = 1;
        b[9][5] = 1;
        // last move in the middle: must count both directions of the line
        assertTrue(rules.isWin(boardWith(b, pos(7, 7)), 1, pos(7, 7)));
    }

    @Test
    public void topEdgeRowFiveWins() {
        byte[][] b = new byte[19][19];
        for (int c = 0; c <= 4; c++) {
            b[0][c] = 1;
        }
        assertTrue(rules.isWin(boardWith(b, pos(0, 4)), 1, pos(0, 4)));
    }

    @Test
    public void leftEdgeColumnFiveWinsFromCorner() {
        byte[][] b = new byte[19][19];
        for (int r = 0; r <= 4; r++) {
            b[r][0] = 2;
        }
        assertTrue(rules.isWin(boardWith(b, pos(0, 0)), 2, pos(0, 0)));
    }

    @Test
    public void bottomRightCornerFiveWins() {
        byte[][] b = new byte[19][19];
        for (int c = 14; c <= 18; c++) {
            b[18][c] = 1;
        }
        assertTrue(rules.isWin(boardWith(b, pos(18, 18)), 1, pos(18, 18)));
    }

    @Test
    public void wrongColorAtLastMoveDoesNotWin() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 9; c++) {
            b[5][c] = 1;
        }
        assertFalse(rules.isWin(boardWith(b, pos(5, 9)), 2, pos(5, 9)));
    }
}
```

- [ ] **Step 2: Run the test, expect FAIL.** Command: `./gradlew :rules:test`
  Expected: `BUILD FAILED` — every positive assertion fails because Task 4's `isWin` stub returns `false` (e.g. `horizontalExactlyFiveWins ... FAILED`). `horizontalFourDoesNotWin` and `wrongColorAtLastMoveDoesNotWin` pass against the stub; the others fail.

- [ ] **Step 3: Implement five-in-a-row in `DefaultPenteRules.java`.** Replace the stub `isWin` body and add the private helper. Match Task 4's actual stub text for the anchor; replace:

```java
    @Override
    public boolean isWin(BoardState s, int color, int lastMove) {
        return false;
    }
```

with:

```java
    @Override
    public boolean isWin(BoardState s, int color, int lastMove) {
        return fiveInARow(s.board, s.gridSize, color, lastMove);
    }

    private boolean fiveInARow(byte[][] board, int n, int color, int lastMove) {
        if (lastMove < 0) {
            return false;
        }
        int row = lastMove / n;
        int col = lastMove % n;
        if (row < 0 || row >= n || col < 0 || col >= n) {
            return false;
        }
        if (board[row][col] != color) {
            return false;
        }
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int count = 1;
            int r = row + d[0];
            int c = col + d[1];
            while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == color) {
                count++;
                r += d[0];
                c += d[1];
            }
            r = row - d[0];
            c = col - d[1];
            while (r >= 0 && r < n && c >= 0 && c < n && board[r][c] == color) {
                count++;
                r -= d[0];
                c -= d[1];
            }
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }
```

- [ ] **Step 4: Run the test, expect PASS.** Command: `./gradlew :rules:test`
  Expected: `BUILD SUCCESSFUL` — all 10 cycle-A tests green.

- [ ] **Step 5: Commit.** Command:
```
git add rules/src/test/java/be/submanifold/pente/rules/WinDetectionTest.java rules/src/main/java/be/submanifold/pente/rules/DefaultPenteRules.java && git commit -m "Add five-in-a-row win detection to DefaultPenteRules

Counts contiguous stones through lastMove in all 4 directions with
inclusive 0..gridSize-1 bounds, fixing the legacy detectPente edge/corner
miss. Wins on >=5, not on 4.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

**Cycle B — capture wins + `replay` winner population**

- [ ] **Step 6: Append failing capture/winner tests to `WinDetectionTest.java`.** Edit: anchor on the final cycle-A method + class close and insert the cycle-B methods. Replace:

```java
    @Test
    public void wrongColorAtLastMoveDoesNotWin() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 9; c++) {
            b[5][c] = 1;
        }
        assertFalse(rules.isWin(boardWith(b, pos(5, 9)), 2, pos(5, 9)));
    }
}
```

with:

```java
    @Test
    public void wrongColorAtLastMoveDoesNotWin() {
        byte[][] b = new byte[19][19];
        for (int c = 5; c <= 9; c++) {
            b[5][c] = 1;
        }
        assertFalse(rules.isWin(boardWith(b, pos(5, 9)), 2, pos(5, 9)));
    }

    @Test
    public void whiteStonesLostTenMeansBlackWins() {
        BoardState s = captureState(10, 0);
        assertTrue(rules.isWin(s, 2, -1));
        assertFalse(rules.isWin(s, 1, -1));
    }

    @Test
    public void blackStonesLostTenMeansWhiteWins() {
        BoardState s = captureState(0, 10);
        assertTrue(rules.isWin(s, 1, -1));
        assertFalse(rules.isWin(s, 2, -1));
    }

    @Test
    public void eightCapturesIsNotYetAWin() {
        BoardState s = captureState(8, 0);
        assertFalse(rules.isWin(s, 2, -1));
    }

    @Test
    public void replayPopulatesWinnerOnFiveInARow() {
        // white plays a horizontal five on row 9; black fills isolated cells (no captures)
        List<Integer> moves = Arrays.asList(
                pos(9, 4), pos(1, 1),
                pos(9, 5), pos(1, 3),
                pos(9, 6), pos(1, 5),
                pos(9, 7), pos(1, 7),
                pos(9, 8));
        BoardState s = rules.replay(moves, Variant.PENTE, moves.size());
        assertEquals(Integer.valueOf(1), s.winner);
    }

    @Test
    public void replayLeavesWinnerNullBeforeFiveInARow() {
        List<Integer> moves = Arrays.asList(
                pos(9, 4), pos(1, 1),
                pos(9, 5), pos(1, 3),
                pos(9, 6), pos(1, 5),
                pos(9, 7));
        BoardState s = rules.replay(moves, Variant.PENTE, moves.size());
        assertNull(s.winner);
    }
}
```

- [ ] **Step 7: Run the test, expect FAIL.** Command: `./gradlew :rules:test`
  Expected: `BUILD FAILED` — `whiteStonesLostTenMeansBlackWins`, `blackStonesLostTenMeansWhiteWins` fail (no capture branch yet) and `replayPopulatesWinnerOnFiveInARow` fails (`expected:<1> but was:<null>` — Task 4's replay passes `null` for `winner`). `eightCapturesIsNotYetAWin` and `replayLeavesWinnerNullBeforeFiveInARow` already pass.

- [ ] **Step 8: Add capture branch + winner population in `DefaultPenteRules.java`.** First, refactor `isWin` to delegate and add `isWinRaw`. Replace:

```java
    @Override
    public boolean isWin(BoardState s, int color, int lastMove) {
        return fiveInARow(s.board, s.gridSize, color, lastMove);
    }
```

with:

```java
    @Override
    public boolean isWin(BoardState s, int color, int lastMove) {
        return isWinRaw(s.board, s.gridSize, s.whiteCaptures, s.blackCaptures, color, lastMove);
    }

    // whiteCaptures = white stones lost => black (2) wins; blackCaptures => white (1) wins.
    private boolean isWinRaw(byte[][] board, int n, int whiteCaptures, int blackCaptures, int color, int lastMove) {
        if (color == 2 && whiteCaptures >= 10) {
            return true;
        }
        if (color == 1 && blackCaptures >= 10) {
            return true;
        }
        return fiveInARow(board, n, color, lastMove);
    }
```

Then, inside `replay(...)`, immediately before Task 4's final `return new BoardState(board, whiteCaptures, blackCaptures, gridSize, lastMove, null, swap2DecisionPoint, dPenteDecisionPoint, koMove);`, insert the winner computation and change the `winner` argument (6th position) from `null` to `winner`. The references `board`, `gridSize`, `whiteCaptures`, `blackCaptures` are the locals/fields Task 4 already passes into the constructor — keep names aligned with Task 4's actual code:

```java
        int playedCount = Math.min(untilMove, moves.size());
        int lastColorForWin = playedCount > 0 ? 2 - (playedCount % 2) : 0;
        int lastMoveForWin = playedCount > 0 ? moves.get(playedCount - 1) : -1;
        Integer winner = (lastColorForWin != 0
                && isWinRaw(board, gridSize, whiteCaptures, blackCaptures, lastColorForWin, lastMoveForWin))
                ? Integer.valueOf(lastColorForWin)
                : null;
        return new BoardState(board, whiteCaptures, blackCaptures, gridSize, lastMove, winner,
                swap2DecisionPoint, dPenteDecisionPoint, koMove);
```

- [ ] **Step 9: Run the test, expect PASS.** Command: `./gradlew :rules:test`
  Expected: `BUILD SUCCESSFUL` — all 15 tests green.

- [ ] **Step 10: Commit.** Command:
```
git add rules/src/test/java/be/submanifold/pente/rules/WinDetectionTest.java rules/src/main/java/be/submanifold/pente/rules/DefaultPenteRules.java && git commit -m "Add capture-win detection and replay winner population

isWin treats whiteCaptures>=10 as a black win and blackCaptures>=10 as a
white win (counters name the losing color, matching Game.java). replay now
populates BoardState.winner with the absolute winning color of the last
mover; null otherwise. Winner stays caller-evaluated.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 6: Wire Game to delegate to DefaultPenteRules and expose getState()

**Files:**
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/Game.java`
  - imports block (after line 19, `import com.google.gson.Gson;`)
  - fields (after line 57, `public int blackCaptures;`)
  - `replayGameUntilMove` call site (lines 1326–1328)
  - new methods (after line 1376, end of `replayGameUntilMove`)
- Test: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/GameStateTest.java` (new)

Preconditions (from Tasks 1–5, already in place): pure-Java module `:rules` exists, `app/build.gradle` declares `implementation project(":rules")`, and `be.submanifold.pente.rules` contains `Variant`, `Variants`, `CaptureRule`, `BoardState`, `PenteRules`, `DefaultPenteRules`. Existing getters `getGameType()` (L130), `getUntilMove()` (L262), `getMovesList()` (L872) are present — do NOT re-add them. Public fields `whiteCaptures`/`blackCaptures` (L56–57) and `abstractBoard` (L77–95) stay in this task.

- [ ] **Step 1: Write failing test `GameStateTest.java` (RED) with real code.** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/GameStateTest.java`:
```java
package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import be.submanifold.pente.rules.BoardState;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class GameStateTest {

    private static Game newPenteGame() {
        // gameType "Pente"; nameColor/crown null so the constructor skips Integer.parseInt
        return new Game("g1", "s1", "Pente", "opp", "1500", "white",
                "0", "rated", "0", null, null);
    }

    private static void injectMoves(Game game, List<Integer> moves, int untilMove) throws Exception {
        Field movesField = Game.class.getDeclaredField("mMovesList");
        movesField.setAccessible(true);
        movesField.set(game, moves);
        Field untilField = Game.class.getDeclaredField("untilMove");
        untilField.setAccessible(true);
        untilField.setInt(game, untilMove);
    }

    @Test
    public void getStateReplaysPenteMovesOntoBoard() throws Exception {
        Game game = newPenteGame();
        // move 180 -> (9,9) white, move 181 -> (9,10) black on a 19x19 grid
        injectMoves(game, Arrays.asList(180, 181), 2);

        BoardState state = game.getState();

        assertNotNull(state);
        assertEquals(19, state.gridSize);
        assertEquals(1, state.cell(9, 9));   // white plays first
        assertEquals(2, state.cell(9, 10));  // black plays second
        assertEquals(181, state.lastMove);
        assertEquals(0, state.whiteCaptures);
        assertEquals(0, state.blackCaptures);
    }

    @Test
    public void getStateMirrorsIntoLegacyPublicFields() throws Exception {
        Game game = newPenteGame();
        injectMoves(game, Arrays.asList(180, 181), 2);

        BoardState state = game.getState();

        assertEquals(state.whiteCaptures, game.whiteCaptures);
        assertEquals(state.blackCaptures, game.blackCaptures);
        assertEquals((byte) 1, game.abstractBoard[9][9]);
        assertEquals((byte) 2, game.abstractBoard[9][10]);
    }
}
```

- [ ] **Step 2: Run the test, expect FAIL (compile error).** Run:
```bash
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.GameStateTest"
```
Expected: FAILURE — `error: cannot find symbol  method getState()` (and `package be.submanifold.pente.rules ... BoardState` referenced before `getState()` exists).

- [ ] **Step 3: Add the rules imports (GREEN).** In `Game.java`, replace:
```java
import com.google.gson.Gson;
```
with:
```java
import com.google.gson.Gson;

import be.submanifold.pente.rules.BoardState;
import be.submanifold.pente.rules.DefaultPenteRules;
import be.submanifold.pente.rules.PenteRules;
import be.submanifold.pente.rules.Variants;
```

- [ ] **Step 4: Add the engine + state fields (GREEN).** In `Game.java`, replace:
```java
    public int whiteCaptures;
    public int blackCaptures;
```
with:
```java
    public int whiteCaptures;
    public int blackCaptures;

    private final PenteRules rules = new DefaultPenteRules();
    private BoardState state;
```

- [ ] **Step 5: Add `getState()` + private helpers after `replayGameUntilMove` (GREEN).** In `Game.java`, immediately after the closing brace of `replayGameUntilMove` (the `}` on line 1376, before `public void replayGame(byte moveI, ...`), insert:
```java

    private void computeState() {
        java.util.List<Integer> moves = getMovesList();
        if (moves == null) {
            moves = new java.util.ArrayList<>();
        }
        state = rules.replay(moves, Variants.fromGameType(getGameType()), getUntilMove());
        mirrorState();
    }

    private void mirrorState() {
        if (state == null) {
            return;
        }
        whiteCaptures = state.whiteCaptures;
        blackCaptures = state.blackCaptures;
        for (int i = 0; i < state.gridSize && i < abstractBoard.length; i++) {
            for (int j = 0; j < state.gridSize && j < abstractBoard[i].length; j++) {
                abstractBoard[i][j] = state.cell(i, j);
            }
        }
    }

    // TODO(rules): GoRules seam — Go variants still classify + size via Variants, but state/replay stays in legacy path for now.
    public BoardState getState() {
        if (state == null) {
            computeState();
        }
        return state;
    }
```

- [ ] **Step 6: Recompute state in the replay path (GREEN).** In `Game.java` `replayGameUntilMove`, replace (lines 1326–1328, the brace closing the variant if/else chain and the start of the moves-string builder):
```java
        }

        movesString = "";
```
with:
```java
        }

        computeState();

        movesString = "";
```

- [ ] **Step 7: Run the test, expect PASS.** Run:
```bash
./gradlew :app:testDebugUnitTest --tests "be.submanifold.pentelive.GameStateTest"
```
Expected: `BUILD SUCCESSFUL` — both `getStateReplaysPenteMovesOntoBoard` and `getStateMirrorsIntoLegacyPublicFields` pass.

- [ ] **Step 8: Run the full app unit suite to confirm it stays green.** Run:
```bash
./gradlew :app:testDebugUnitTest
```
Expected: `BUILD SUCCESSFUL` — `ExampleUnitTest`, `ArenaEventsTest`, and `GameStateTest` all pass.

- [ ] **Step 9: Confirm the app still compiles against the wired engine.** Run:
```bash
./gradlew :app:compileDebugJavaWithJavac
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit.** Run:
```bash
git add app/src/main/java/be/submanifold/pentelive/Game.java app/src/test/java/be/submanifold/pentelive/GameStateTest.java
git commit -m "Wire Game to DefaultPenteRules and expose getState()

Game now delegates board reconstruction to the pure-Java :rules engine
via rules.replay(...) on the replay path, stores the immutable BoardState
in a private field, and exposes it through getState(). Legacy public
fields (whiteCaptures/blackCaptures/abstractBoard) are temporarily
mirrored from the new state so existing rendering keeps working.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

### Task 7: Full pull-model in BoardView — delete Game's public board fields, derive background + red dots from `getState()`

**Files:**
- Create: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/BoardDerivation.java`
- Create (Test): `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/BoardDerivationTest.java`
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/BoardView.java` (imports 1-26; field decl 46; `setGame` 85-89; `onDraw` 132-159; readers 311, 314, 354; stone loop 491-495; new methods appended before final `}`)
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/Game.java` (field decls 56-57 and 77-95; internal caller 1242; win-check 1246-1262; `replayGameUntilMove` 1283-1376)
- Modify: `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/BoardActivity.java` (392, 416)

> Assumes Tasks 1-6 are merged: the `:rules` module exists; `Game` has `private BoardState state` (initialized non-null to an empty board in the constructor), `private final PenteRules rules = new DefaultPenteRules()`, a public `BoardState getState()`, package getters `getMovesList()/getUntilMove()/getGameType()`, and the live-play `replayGame(...)` overloads already do `state = rules.replay(...)`. The three public fields `whiteCaptures`, `blackCaptures`, `abstractBoard` survive only as the legacy bridge this task removes.

- [ ] **Step 1: Write the failing test for the pure red-dot helper.** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/test/java/be/submanifold/pentelive/BoardDerivationTest.java`:
```java
package be.submanifold.pentelive;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class BoardDerivationTest {

    @Test
    public void connect6SecondDot_returnsMoveBeforeLast() {
        assertEquals(42, BoardDerivation.connect6SecondDot(Arrays.asList(10, 42, 77), 3));
    }

    @Test
    public void connect6SecondDot_firstMoveHasNoSecondDot() {
        assertEquals(-1, BoardDerivation.connect6SecondDot(Arrays.asList(10, 42, 77), 1));
    }

    @Test
    public void connect6SecondDot_zeroUntilMoveIsMinusOne() {
        assertEquals(-1, BoardDerivation.connect6SecondDot(Arrays.asList(10, 42, 77), 0));
    }

    @Test
    public void connect6SecondDot_nullOrEmptyIsMinusOne() {
        assertEquals(-1, BoardDerivation.connect6SecondDot(null, 5));
        assertEquals(-1, BoardDerivation.connect6SecondDot(Collections.<Integer>emptyList(), 0));
    }

    @Test
    public void connect6SecondDot_untilMoveBeyondListIsMinusOne() {
        assertEquals(-1, BoardDerivation.connect6SecondDot(Arrays.asList(10, 42), 5));
    }
}
```

- [ ] **Step 2: Run the test, expect compile FAIL.** `./gradlew :app:testDebugUnitTest --tests be.submanifold.pentelive.BoardDerivationTest` — expected FAIL: `error: cannot find symbol ... BoardDerivation`.

- [ ] **Step 3: Create the pure helper (minimal real impl).** Create `/Users/waliedothman/mariposa/coding/pente.org-project/penteLive-Android/app/src/main/java/be/submanifold/pentelive/BoardDerivation.java`:
```java
package be.submanifold.pentelive;

import java.util.List;

/** Pure (Android-free) display derivations for the board view. */
final class BoardDerivation {

    private BoardDerivation() {
    }

    /**
     * The second red dot shown for Connect6 (the move played immediately before
     * the last one). Returns -1 when there is no such move.
     */
    static int connect6SecondDot(List<Integer> moves, int untilMove) {
        if (moves == null || untilMove < 2 || moves.size() < untilMove) {
            return -1;
        }
        return moves.get(untilMove - 2);
    }
}
```

- [ ] **Step 4: Run the test, expect PASS.** `./gradlew :app:testDebugUnitTest --tests be.submanifold.pentelive.BoardDerivationTest` — expected: `BUILD SUCCESSFUL`, 5 tests pass.

- [ ] **Step 5: Commit the helper.** `git checkout -b task7-pull-model-boardview && git add app/src/main/java/be/submanifold/pentelive/BoardDerivation.java app/src/test/java/be/submanifold/pentelive/BoardDerivationTest.java && git commit -m "Add pure BoardDerivation.connect6SecondDot helper

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

- [ ] **Step 6: Add rules imports to BoardView.** In `BoardView.java`, after the existing `import android.widget.TextView;` (line 26) add:
```java

import be.submanifold.pente.rules.BoardState;
import be.submanifold.pente.rules.Variant;
import be.submanifold.pente.rules.Variants;
```

- [ ] **Step 7: Delete the cached board field and its mirror in `setGame`.** In `BoardView.java` delete line 46 exactly:
```java
    public byte[][] abstractBoard;
```
and in `setGame` (lines 85-89) replace:
```java
    public void setGame(Game game) {
        this.game = game;
        abstractBoard = game.abstractBoard;
//        this.game.parseGame(this);
    }
```
with:
```java
    public void setGame(Game game) {
        this.game = game;
//        this.game.parseGame(this);
    }
```

- [ ] **Step 8: Hook the two derivations into `onDraw` before drawing.** In `BoardView.java` replace the head of `onDraw` (lines 132-138):
```java
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        drawBoard(canvas);
```
with:
```java
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (game != null) {
            applyVariantBackground();
            applyRedDots();
        }
        size = getWidth();
        canvas.scale(scaling, scaling);
        canvas.translate(translateX, translateY);
        drawBoard(canvas);
```

- [ ] **Step 9: Repoint the captures subtitle to `getState()`.** In `BoardView.java` replace line 143:
```java
                ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setSubtitle("\u2B24 x " + game.blackCaptures + " - \u25EF x " + game.whiteCaptures);
```
with:
```java
                BoardState st = game.getState();
                ((Toolbar) parentLayout.findViewById(R.id.toolbar)).setSubtitle("\u2B24 x " + st.blackCaptures + " - \u25EF x " + st.whiteCaptures);
```

- [ ] **Step 10: Repoint the three cell readers to `getState().cell(...)`.** In `BoardView.java` replace line 311 fragment `abstractBoard[stoneI][stoneJ] != 0` with `game.getState().cell(stoneI, stoneJ) != 0`; replace line 314 `} else if (abstractBoard[stoneI][stoneJ] != 0 || playedMove == game.koMove ||` with `} else if (game.getState().cell(stoneI, stoneJ) != 0 || playedMove == game.koMove ||`; replace line 354 `if (!game.isGoMarkStones() && (playedMove == -1 || abstractBoard[stoneI][stoneJ] != 0)) {` with `if (!game.isGoMarkStones() && (playedMove == -1 || game.getState().cell(stoneI, stoneJ) != 0)) {`.

- [ ] **Step 11: Repoint the stone-draw loop and guard against a null game.** In `BoardView.java` replace lines 491-495:
```java
        for (byte i = 0; i < gridSize; i++) {
            for (byte j = 0; j < gridSize; j++) {
                drawStone(canvas, i, j, abstractBoard[i][j]);
            }
        }
```
with:
```java
        if (game != null) {
            for (byte i = 0; i < gridSize; i++) {
                for (byte j = 0; j < gridSize; j++) {
                    drawStone(canvas, i, j, game.getState().cell(i, j));
                }
            }
        }
```

- [ ] **Step 12: Add `applyVariantBackground()` and `applyRedDots()` to BoardView.** In `BoardView.java`, immediately before `drawRedDot(Canvas canvas)` (line 713) insert:
```java
    private void applyVariantBackground() {
        if (game == null) {
            return;
        }
        switch (Variants.fromGameType(game.getGameType())) {
            case GOMOKU:      setBackgroundColor(gomokuColor); break;
            case PENTE:       setBackgroundColor(penteColor); break;
            case BOAT_PENTE:  setBackgroundColor(boatPenteColor); break;
            case KERYO_PENTE: setBackgroundColor(keryoPenteColor); break;
            case CONNECT6:    setBackgroundColor(connect6Color); break;
            case G_PENTE:     setBackgroundColor(gPenteColor); break;
            case POOF_PENTE:  setBackgroundColor(poofPenteColor); break;
            case D_PENTE:     setBackgroundColor(dPenteColor); break;
            case DK_PENTE:    setBackgroundColor(dkeryoColor); break;
            case O_PENTE:     setBackgroundColor(oPenteColor); break;
            case SWAP2_PENTE: setBackgroundColor(swap2PenteColor); break;
            case SWAP2_KERYO: setBackgroundColor(swap2KeryoColor); break;
            case GO_9:
            case GO_13:
            case GO_19:       setBackgroundColor(goColor); break;
            default: break;
        }
    }

    private void applyRedDots() {
        if (game == null) {
            return;
        }
        redDot = game.getState().lastMove;
        if (game.isConnect6()) {
            c6RedDot = BoardDerivation.connect6SecondDot(game.getMovesList(), game.getUntilMove());
        } else {
            c6RedDot = -1;
        }
    }

```

- [ ] **Step 13: Drop the dead `byte[][]` param from `replayGameUntilMove` and strip the background + red-dot blocks.** In `Game.java` replace the whole method body (lines 1283-1376) — signature, the variant background `if/else` chain, **and** the trailing red-dot block — with the version below, keeping the existing `movesString` builder verbatim:
```java
    public void replayGameUntilMove(BoardView boardView) {
        if (mMovesList == null) {
            return;
        }
        state = rules.replay(mMovesList, Variants.fromGameType(getGameType()), untilMove);

        movesString = "";
        if (isConnect6()) {
            for (int i = 0; i < Math.min(mMovesList.size(), untilMove); i++) {
                if (i == 0) {
                    movesString += "<b>1.</b> ";
                } else {
                    if (((i - 3) % 4) == 0) {
                        movesString += " <b>" + ((i >> 2) + 2) + ".</b> ";
                    } else if (((i - 3) % 4) == 2 || i == 1) {
                        movesString += " - ";
                    } else {
                        movesString += "-";
                    }
                }
                int move = mMovesList.get(i), moveI = move / 19, moveJ = move % 19;
                movesString += coordinateLetters[moveJ] + "" + (19 - (moveI));
            }
        } else {
            for (int i = 0; i < Math.min(mMovesList.size(), untilMove); i++) {
                if (i % 2 == 0) {
                    movesString += "<b>" + (i / 2 + 1) + ".</b> ";
                }
                int move = mMovesList.get(i), moveI = move / gridSize, moveJ = move % gridSize;
                if (move == gridSize * gridSize) {
                    movesString += "PASS ";
                } else {
                    movesString += coordinateLetters[moveJ] + "" + (gridSize - (moveI)) + " ";
                }
                if (i % 2 == 0) {
                    movesString += "- ";
                } else {
                    movesString += "  ";
                }
            }
        }

        if (boardView != null) {
            boardView.invalidate();
        }
    }
```
Add `import be.submanifold.pente.rules.Variants;` to `Game.java` if Task 6 did not already add it.

- [ ] **Step 14: Fix the internal `replayGameUntilMove` caller.** In `Game.java` replace line 1242:
```java
        replayGameUntilMove(abstractBoard, boardView);
```
with:
```java
        replayGameUntilMove(boardView);
```

- [ ] **Step 15: Repoint the computer-game win-check to `getState()`.** In `Game.java`, inside `replayGame(BoardView)` (lines 1246-1262), introduce a local snapshot and repoint the capture reads. Replace:
```java
            if (mGameType.equals("Pente") && mOpponentName.equals("computer")) {
//                System.out.println("what white: " + whiteCaptures + " black: " + blackCaptures + " pente? " + detectPente(abstractBoard, (byte) (2 - (mMovesList.size()%2)), mMovesList.get(mMovesList.size() - 1)));
                if (whiteCaptures == 10 || blackCaptures == 10 || detectPente((byte) (2 - (mMovesList.size() % 2)), mMovesList.get(mMovesList.size() - 1))) {
                    boolean iWon = false;
                    mActive = false;
                    int myColor = (mMyColor.contains("white") ? 1 : 2);
                    if (whiteCaptures == 10) {
                        if (myColor == 2) {
                            iWon = true;
                        }
                    } else if (blackCaptures == 10) {
                        if (myColor == 1) {
                            iWon = true;
                        }
                    } else if (myColor == (2 - mMovesList.size() % 2)) {
```
with:
```java
            if (mGameType.equals("Pente") && mOpponentName.equals("computer")) {
                BoardState s = getState();
                if (s.whiteCaptures == 10 || s.blackCaptures == 10 || detectPente((byte) (2 - (mMovesList.size() % 2)), mMovesList.get(mMovesList.size() - 1))) {
                    boolean iWon = false;
                    mActive = false;
                    int myColor = (mMyColor.contains("white") ? 1 : 2);
                    if (s.whiteCaptures == 10) {
                        if (myColor == 2) {
                            iWon = true;
                        }
                    } else if (s.blackCaptures == 10) {
                        if (myColor == 1) {
                            iWon = true;
                        }
                    } else if (myColor == (2 - mMovesList.size() % 2)) {
```
Add `import be.submanifold.pente.rules.BoardState;` to `Game.java` if not already present.

- [ ] **Step 16: Fix the BoardActivity navigation callers.** In `BoardActivity.java` replace line 392 and line 416 (both identical) `game.replayGameUntilMove(board.abstractBoard, board);` with `game.replayGameUntilMove(board);` (use `replace_all`).

- [ ] **Step 17: Delete the three public fields and their initializer.** In `Game.java` delete lines 56-57:
```java
    public int whiteCaptures;
    public int blackCaptures;
```
and delete the full `abstractBoard` declaration, lines 77-95 (from `public byte[][] abstractBoard = {{0, ...` through the closing `...0}};`).

- [ ] **Step 18: Compile, expect FAIL listing the remaining bridge (mirroring) sites.** `./gradlew :app:compileDebugJavaWithJavac` — expected FAIL: `error: cannot find symbol` at every leftover reader/writer of `whiteCaptures`/`blackCaptures`/`abstractBoard`. Enumerate them with `rg -n "whiteCaptures|blackCaptures|abstractBoard" app/src/main/java/be/submanifold/pentelive/Game.java`.

- [ ] **Step 19: Remove the mirroring and repoint stragglers (compiler-driven).** For each remaining error in `Game.java`: if it is a **writer** inside a legacy replay helper (`replayPenteGame`, `replayKeryoPenteGame`, `replayPoofPenteGame`, `replayGPenteGame`, `replayConnect6Game`, `replayGomokuGame`, `replayOPenteGame`, `replayGoGame`) whose result is now produced by `rules.replay(...)`, delete that helper method in full (it is the mirroring) and delete any now-orphaned calls to it; if it is a **reader** in still-live code, repoint it to `getState()` (`whiteCaptures` → `getState().whiteCaptures`, `blackCaptures` → `getState().blackCaptures`, `abstractBoard[i][j]` → `getState().cell(i, j)`). Re-run `./gradlew :app:compileDebugJavaWithJavac` after each pass until there are zero references left.

- [ ] **Step 20: Confirm clean compile.** `./gradlew :app:compileDebugJavaWithJavac` — expected: `BUILD SUCCESSFUL`. Verify no field references survive: `rg -n "game\.whiteCaptures|game\.blackCaptures|game\.abstractBoard|board\.abstractBoard" app/src/main/java` returns no matches.

- [ ] **Step 21: Run the full unit suite (regression), expect PASS.** `./gradlew :app:testDebugUnitTest` — expected: `BUILD SUCCESSFUL`; `BoardDerivationTest` (5) and `ArenaEventsTest` (4) green.

- [ ] **Step 22: Commit the pull-model migration.** `git add app/src/main/java/be/submanifold/pentelive/BoardView.java app/src/main/java/be/submanifold/pentelive/Game.java app/src/main/java/be/submanifold/pentelive/BoardActivity.java && git commit -m "Move board background + red dots into BoardView; delete Game public board fields

BoardView now pulls every render input from game.getState(): cell occupancy,
capture counts, variant background, and red dots. Game.whiteCaptures,
Game.blackCaptures and Game.abstractBoard (and their mirroring) are removed;
replayGameUntilMove drops its dead byte[][] parameter.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`

---

I now have an accurate picture. Key findings that shape this task:
- `Table.abstractBoard` (line 103, currently `public`) has exactly two external readers — `LiveBoardView:144` and `:209` — so making it private cleanly forces those repoints.
- Only plain JUnit is available (no Robolectric / `returnDefaultValues`), and `Table` can't be instantiated in unit tests (`Color.parseColor` + `MyApplication.getContext()` in field initializers). So the pure-Java unit test lives in `:rules`; the Android wiring is verified by compile + the existing `:app` tests.
- Game's `replay*Game(int)` Pente-family helpers are the dead full-replay overloads; the `(byte,…)` incremental overloads and `detect*` remain live, and `replayGoGame(int)` is Go (out of scope).

### Task 8: Repoint LiveBoardView through Table.getState(), route predicates through Variants, delete dead Game replay helpers

**Files:**
- Create: `rules/src/test/java/be/submanifold/pente/rules/VariantRoutingTest.java` (test — pure JUnit, runs in `:rules`)
- Modify: `rules/src/main/java/be/submanifold/pente/rules/Variants.java` — `fromGameType(String)` and `fromGameId(int)` switch bodies (only if Step 2 is RED)
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/Table.java` — imports (after line 18); add `getState()` (before `undoMove()` @170); make `abstractBoard` private (line 103); TODO seam above `addMove` (line 189); route `isGo()` (185–187), `isDPente()` (266–268), `isSwap2()` (270–272)
- Modify: `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveBoardView.java` — import (after line 18); repoint read at line 144; repoint draw loop at lines 206–211
- Modify: `app/src/main/java/be/submanifold/pentelive/Game.java` — `Variants` import (next to Task 7's `be.submanifold.pente.rules.BoardState` import); route `isSwap2()` (885–890), `isConnect6()` (899–904), `isGomoku()` (906–911), `isDPente()` (913–918), `isGo()` (2389–2391); delete dead Pente-family `replay*Game(int)` helpers (ranges in Step 9); TODO seams

---

- [ ] **Step 1: Write the failing predicate-equivalence test (REAL code).** This pins the exact legacy game-name / game-id → predicate mapping that the routing in this task depends on. Create `rules/src/test/java/be/submanifold/pente/rules/VariantRoutingTest.java`:

```java
package be.submanifold.pente.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the legacy game-name / game-id -> predicate equivalence that Task 8 relies on
 * when routing Game.is*() (string-based) and Table.is*() (id-based) through Variants.
 * Pure JVM, zero Android imports.
 */
public class VariantRoutingTest {

    @Test
    public void fromGameType_classifiesSwap2_includingSpeedVariants() {
        // Legacy Game.isSwap2() used startsWith("Swap2") and MISSED the "Speed " names.
        assertTrue(Variants.fromGameType("Swap2-Pente").isSwap2());
        assertTrue(Variants.fromGameType("Speed Swap2-Pente").isSwap2());
        assertTrue(Variants.fromGameType("Swap2-Keryo").isSwap2());
        assertTrue(Variants.fromGameType("Speed Swap2-Keryo").isSwap2());
        assertEquals(Variant.SWAP2_PENTE, Variants.fromGameType("Speed Swap2-Pente"));
        assertEquals(Variant.SWAP2_KERYO, Variants.fromGameType("Speed Swap2-Keryo"));
        assertFalse(Variants.fromGameType("Pente").isSwap2());
    }

    @Test
    public void fromGameType_classifiesDPente_bothDAndDK() {
        assertTrue(Variants.fromGameType("D-Pente").isDPente());
        assertTrue(Variants.fromGameType("Speed D-Pente").isDPente());
        assertTrue(Variants.fromGameType("DK-Pente").isDPente());
        assertTrue(Variants.fromGameType("Speed DK-Pente").isDPente());
        assertEquals(CaptureRule.PENTE_PAIR, Variants.captureRule(Variant.D_PENTE));
        assertEquals(CaptureRule.KERYO_TRIO, Variants.captureRule(Variant.DK_PENTE));
    }

    @Test
    public void fromGameType_classifiesGoSizes() {
        assertTrue(Variants.fromGameType("Go").isGo());
        assertTrue(Variants.fromGameType("Speed Go").isGo());
        assertEquals(Variant.GO_9, Variants.fromGameType("Go (9x9)"));
        assertEquals(Variant.GO_9, Variants.fromGameType("Speed Go (9x9)"));
        assertEquals(Variant.GO_13, Variants.fromGameType("Go (13x13)"));
        assertEquals(Variant.GO_19, Variants.fromGameType("Go"));
        assertEquals(9, Variants.gridSize(Variant.GO_9));
        assertEquals(13, Variants.gridSize(Variant.GO_13));
        assertEquals(19, Variants.gridSize(Variant.GO_19));
    }

    @Test
    public void fromGameType_classifiesGomokuAndConnect6() {
        assertTrue(Variants.fromGameType("Gomoku").isGomoku());
        assertTrue(Variants.fromGameType("Speed Gomoku").isGomoku());
        assertTrue(Variants.fromGameType("Connect6").isConnect6());
        assertTrue(Variants.fromGameType("Speed Connect6").isConnect6());
        assertFalse(Variants.fromGameType("Pente").isGomoku());
        assertFalse(Variants.fromGameType("Pente").isConnect6());
    }

    @Test
    public void fromGameId_matchesTablePredicateRanges() {
        // Table routes Table.is*() through Variants.fromGameId(game); ids per Table.gameNames.
        assertTrue(Variants.fromGameId(19).isGo());    // Go
        assertTrue(Variants.fromGameId(24).isGo());    // Speed Go (13x13)
        assertFalse(Variants.fromGameId(25).isGo());   // O-Pente
        assertTrue(Variants.fromGameId(7).isDPente()); // D-Pente
        assertTrue(Variants.fromGameId(18).isDPente());// Speed DK-Pente
        assertTrue(Variants.fromGameId(27).isSwap2()); // Swap2-Pente
        assertTrue(Variants.fromGameId(30).isSwap2()); // Speed Swap2-Keryo
        assertFalse(Variants.fromGameId(1).isSwap2()); // Pente
    }
}
```

- [ ] **Step 2: Run the test, expect FAIL.** Command: `./gradlew :rules:test --tests "be.submanifold.pente.rules.VariantRoutingTest"`. Expected: FAIL — at minimum the Speed-prefixed Swap2 rows and/or the parenthesized Go-size rows assert wrong `Variant`/`AssertionError` because the Task 1 `Variants` factory does not yet normalize the `"Speed "` prefix and the `"Go (9x9)"`/`"Go (13x13)"` names. (If it passes outright, the test still stands as the routing regression net — proceed to Step 5.)

- [ ] **Step 3: Make it pass — complete the `Variants` factory (minimal REAL code).** In `rules/src/main/java/be/submanifold/pente/rules/Variants.java`, replace the bodies of `fromGameType` and `fromGameId` with the full canonical mappings (the id table mirrors `Table.gameNames` exactly: odd = standard, even = `Speed`):

```java
    public static Variant fromGameType(String gameType) {
        if (gameType == null) {
            return Variant.PENTE;
        }
        String g = gameType.startsWith("Speed ") ? gameType.substring("Speed ".length()) : gameType;
        switch (g) {
            case "Pente":       return Variant.PENTE;
            case "Boat-Pente":  return Variant.BOAT_PENTE;
            case "Keryo-Pente": return Variant.KERYO_PENTE;
            case "G-Pente":     return Variant.G_PENTE;
            case "Poof-Pente":  return Variant.POOF_PENTE;
            case "D-Pente":     return Variant.D_PENTE;
            case "DK-Pente":    return Variant.DK_PENTE;
            case "O-Pente":     return Variant.O_PENTE;
            case "Swap2-Pente": return Variant.SWAP2_PENTE;
            case "Swap2-Keryo": return Variant.SWAP2_KERYO;
            case "Gomoku":      return Variant.GOMOKU;
            case "Connect6":    return Variant.CONNECT6;
            case "Go (9x9)":    return Variant.GO_9;
            case "Go (13x13)":  return Variant.GO_13;
            case "Go":          return Variant.GO_19;
            default:            return Variant.PENTE;
        }
    }

    public static Variant fromGameId(int id) {
        switch (id) {
            case 1:  case 2:  return Variant.PENTE;
            case 3:  case 4:  return Variant.KERYO_PENTE;
            case 5:  case 6:  return Variant.GOMOKU;
            case 7:  case 8:  return Variant.D_PENTE;
            case 9:  case 10: return Variant.G_PENTE;
            case 11: case 12: return Variant.POOF_PENTE;
            case 13: case 14: return Variant.CONNECT6;
            case 15: case 16: return Variant.BOAT_PENTE;
            case 17: case 18: return Variant.DK_PENTE;
            case 19: case 20: return Variant.GO_19;
            case 21: case 22: return Variant.GO_9;
            case 23: case 24: return Variant.GO_13;
            case 25: case 26: return Variant.O_PENTE;
            case 27: case 28: return Variant.SWAP2_PENTE;
            case 29: case 30: return Variant.SWAP2_KERYO;
            default:          return Variant.PENTE;
        }
    }
```

- [ ] **Step 4: Run the test, expect PASS.** Command: `./gradlew :rules:test --tests "be.submanifold.pente.rules.VariantRoutingTest"`. Expected: `BUILD SUCCESSFUL`, all 5 tests green.

- [ ] **Step 5: Add `Table.getState()` and the `BoardState` import (REAL code).** In `app/src/main/java/be/submanifold/pentelive/liveGameRoom/Table.java`, add the import after line 18 (`import be.submanifold.pentelive.R;`):

```java
import be.submanifold.pente.rules.BoardState;
import be.submanifold.pente.rules.Variants;
```

Then insert `getState()` immediately before `public void undoMove() {` (line 170). It wraps Table's fields into the immutable Pente-only `BoardState` using Task 3's canonical constructor `BoardState(byte[][] board, int whiteCaptures, int blackCaptures, int gridSize, int lastMove, Integer winner, boolean swap2DecisionPoint, boolean dPenteDecisionPoint, int koMove)`:

```java
    public BoardState getState() {
        int lastMove = moves.isEmpty() ? -1 : moves.get(moves.size() - 1);
        return new BoardState(abstractBoard, whiteCaptures, blackCaptures, gridSize,
                lastMove, null, false, false, -1);
    }
```

- [ ] **Step 6: Make `Table.abstractBoard` private and add the migration TODO seam (forces the encapsulation break).** In `Table.java` line 103, change the field declaration's visibility:

old:
```java
    public byte[][] abstractBoard = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
```
new:
```java
    private byte[][] abstractBoard = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
```

Then add the seam comment directly above `public void addMove(int move) {` (line 189):

old:
```java
    public void addMove(int move) {
```
new:
```java
    // TODO(rules): migrate Table replay to PenteRules (addMove/detect* still hand-rolled).
    public void addMove(int move) {
```

- [ ] **Step 7: Run the app compile, expect FAIL.** Command: `./gradlew :app:compileDebugJavaWithJavac`. Expected: FAIL — `error: abstractBoard has private access in Table` at `LiveBoardView.java:144` and `LiveBoardView.java:209`. This is the RED for the repoint.

- [ ] **Step 8: Repoint `LiveBoardView` reads through `getState()` (minimal REAL fix).** In `app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveBoardView.java`, add the import after line 18 (`import java.util.Map;`):

```java
import be.submanifold.pente.rules.BoardState;
```

Repoint the touch-handler read at line 144:

old:
```java
            boolean filled = table.abstractBoard[stoneI][stoneJ] != 0;
```
new:
```java
            boolean filled = table.getState().cell(stoneI, stoneJ) != 0;
```

Repoint the draw loop (lines 206–211) — cache one `BoardState` snapshot before the nested loop so we do not rebuild a 19x19 copy per cell:

old:
```java
        if (table != null) {
            for (byte i = 0; i < gridSize; i++) {
                for (byte j = 0; j < gridSize; j++) {
                    drawStone(canvas, i, j, table.abstractBoard[i][j]);
                }
            }
            setBackgroundColor(table.getGameColor());
        }
```
new:
```java
        if (table != null) {
            BoardState state = table.getState();
            for (byte i = 0; i < gridSize; i++) {
                for (byte j = 0; j < gridSize; j++) {
                    drawStone(canvas, i, j, state.cell(i, j));
                }
            }
            setBackgroundColor(table.getGameColor());
        }
```

- [ ] **Step 9: Run the app compile, expect PASS.** Command: `./gradlew :app:compileDebugJavaWithJavac`. Expected: `BUILD SUCCESSFUL` — the private field is now reached only through `getState()`.

- [ ] **Step 10: Route `Table` predicates through `Variants` (REAL code).** In `Table.java`, replace the three id-based predicate bodies. `isGo()` (185–187):

old:
```java
    public boolean isGo() {
        return game > 18 && game < 25;
    }
```
new:
```java
    public boolean isGo() {
        return Variants.fromGameId(game).isGo();
    }
```

`isDPente()` (266–268):

old:
```java
    public boolean isDPente() {
        return (game == 7 || game == 8 || game == 17 || game == 18);
    }
```
new:
```java
    public boolean isDPente() {
        return Variants.fromGameId(game).isDPente();
    }
```

`isSwap2()` (270–272):

old:
```java
    public boolean isSwap2() {
        return (game == 27 || game == 28 || game == 29 || game == 30);
    }
```
new:
```java
    public boolean isSwap2() {
        return Variants.fromGameId(game).isSwap2();
    }
```

- [ ] **Step 11: Route `Game` predicates through `Variants` (REAL code).** In `app/src/main/java/be/submanifold/pentelive/Game.java`, add `import be.submanifold.pente.rules.Variants;` next to the `import be.submanifold.pente.rules.BoardState;` line that Task 7 added. Then replace the five predicate bodies. `isSwap2()` (885–890) — note this also fixes the legacy `startsWith` bug so `"Speed Swap2-*"` is now recognized:

old:
```java
    public boolean isSwap2() {
        if (this.mGameType == null) {
            return false;
        }
        return this.mGameType.startsWith("Swap2");
    }
```
new:
```java
    public boolean isSwap2() {
        return Variants.fromGameType(mGameType).isSwap2();
    }
```

`isConnect6()` (899–904):

old:
```java
    public boolean isConnect6() {
        if (getGameType() == null) {
            return false;
        }
        return getGameType().contains("Connect6");
    }
```
new:
```java
    public boolean isConnect6() {
        return Variants.fromGameType(getGameType()).isConnect6();
    }
```

`isGomoku()` (906–911):

old:
```java
    public boolean isGomoku() {
        if (getGameType() == null) {
            return false;
        }
        return getGameType().contains("Gomoku");
    }
```
new:
```java
    public boolean isGomoku() {
        return Variants.fromGameType(getGameType()).isGomoku();
    }
```

`isDPente()` (913–918):

old:
```java
    public boolean isDPente() {
        if (getGameType() == null) {
            return false;
        }
        return (getGameType().contains("D-Pente") || getGameType().contains("DK-Pente"));
    }
```
new:
```java
    public boolean isDPente() {
        return Variants.fromGameType(getGameType()).isDPente();
    }
```

`isGo()` (2389–2391) — classify via the variant instead of the now write-only `go` field (the `go`/`goMarkStones`/`goEvaluateDeadStones` fields stay for the Go sub-state machine):

old:
```java
    public boolean isGo() {
        return go;
    }
```
new:
```java
    public boolean isGo() {
        return Variants.fromGameType(getGameType()).isGo();
    }
```

- [ ] **Step 12: Delete the dead `replay*Game(int)` Pente-family helpers (grep-guarded).** First confirm they are dead (Task 7 routed `replayGame`/`replayGameUntilMove` through `rules.replay(...)`). Command: `grep -nE "replayGomokuGame\(|replayPenteGame\(|replayKeryoPenteGame\(|replayOPenteGame\(|replayConnect6Game\(|replayGPenteGame\(|replayPoofPenteGame\(" app/src/main/java/be/submanifold/pentelive/Game.java`. Expected: the only matches are the method definitions themselves and any `(byte,...)` incremental overloads — zero calls to the `(int)` overloads. If a call site outside the method bodies remains, STOP (Task 7 dispatcher routing is incomplete) and do not delete. Otherwise delete these `(int)` methods, **bottom-up to keep line numbers valid**: `replayPoofPenteGame(int)` 1640–1659, `replayGPenteGame(int)` 1607–1639, `replayConnect6Game(int)` 1598–1606, `replayOPenteGame(int)` 1542–1563, `replayKeryoPenteGame(int)` 1522–1541, `replayPenteGame(int)` 1444–1473, `replayGomokuGame(int)` 1435–1443. KEEP all `(byte,…)` incremental overloads and the `detect*` methods (still referenced by the incremental path — confirm with `grep -nE "detectPenteCapture\(|detectKeryoPenteCapture\(|detectPoof\(|detectKeryoPoof\(" app/src/main/java/be/submanifold/pentelive/Game.java`, expecting live calls from `replayPenteGame(byte,...)`/`replayKeryoGame(byte,...)`/`replayOPenteGame(byte,byte)`/`replayKeryoPenteGame(byte,byte)`/`replayPoofPenteGame(byte,byte)`). KEEP `replayGoGame(int)` (2420) and add the seam above it:

old:
```java
    private void replayGoGame(int until) {
```
new:
```java
    // TODO(rules): GoRules seam — Go full-replay stays hand-rolled until GoRules exists.
    private void replayGoGame(int until) {
```

- [ ] **Step 13: Run the app compile, expect PASS.** Command: `./gradlew :app:compileDebugJavaWithJavac`. Expected: `BUILD SUCCESSFUL` — predicates resolve through `Variants`, dead helpers removed, no dangling references.

- [ ] **Step 14: Run the app unit tests, expect PASS.** Command: `./gradlew :app:testDebugUnitTest`. Expected: `BUILD SUCCESSFUL` — `ArenaEventsTest` and all Task 1–7 app tests stay green.

- [ ] **Step 15: Commit.** Command:

```bash
git add rules/src/test/java/be/submanifold/pente/rules/VariantRoutingTest.java \
        rules/src/main/java/be/submanifold/pente/rules/Variants.java \
        app/src/main/java/be/submanifold/pentelive/liveGameRoom/Table.java \
        app/src/main/java/be/submanifold/pentelive/liveGameRoom/LiveBoardView.java \
        app/src/main/java/be/submanifold/pentelive/Game.java && \
git commit -m "Repoint LiveBoardView via Table.getState(), route predicates through Variants

Add Table.getState() wrapping board/captures/gridSize/lastMove into an
immutable Pente-only BoardState; make Table.abstractBoard private and read
it through getState().cell() in LiveBoardView. Route Game/Table
isSwap2/isDPente/isGo/isGomoku/isConnect6 through Variants (fixes the
legacy startsWith bug so Speed Swap2 games are recognised). Delete the dead
replay*Game(int) Pente-family helpers PenteRules replaced; leave TODO seams
for the still-hand-rolled Table replay and Go full-replay.

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```