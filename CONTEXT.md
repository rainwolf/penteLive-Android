# Context — penteLive-Android domain & architecture vocabulary

Shared language for this codebase. Domain terms describe Pente itself; architecture terms
(module, interface, seam, deep/shallow, adapter, leverage, locality) follow the standard
architecture glossary. Keep these names consistent across code, reviews, and design notes.

## Domain

- **Variant** — a specific rule-set the platform plays: Pente, Boat-Pente, Keryo-Pente,
  G-Pente, Poof-Pente, D-Pente, DK-Pente, O-Pente, Swap2(-Keryo), Gomoku, Connect6, and Go
  (9/13/19). Today a Variant is classified two incompatible ways — by `gameType` string in
  `Game.java` and by numeric game-id in `Table.java`. Target: one `Variant` enum + a
  descriptor table as the single source of truth (`gridSize`, `captureRule`, `winType`,
  `stonesPerTurn`).
- **Capture rule** — how stones are removed: `PENTE_PAIR` (+2, flanked pair), `KERYO_TRIO`
  (+3), `POOF` / `KERYO_POOF` (pattern increment + one-time bonus), or `NONE` (Gomoku,
  Connect6, Go).
- **Capture-win** — reaching 10 captured stones wins, independent of five-in-a-row.
- **Set** — one game within a match; a live `Table` can span multiple sets (`HALFSET` state).
- **Swap2 / D-Pente decision point** — the move where the second player chooses to swap
  colours (Swap2 at move 3) or D-Pente's swap fork. The board state must flag when play is
  paused at such a fork.
- **Rated opening** — restricted centre cells marked forbidden (`-1`) during a rated game's
  opening moves.
- **Board encoding** — a single `byte[][]`: `0` empty, `1` white, `2` black, `-1` forbidden,
  `3`/`4` Go territory.

## Architecture (deepening targets crystallized in design review)

- **PenteRules** — the deep rules module being extracted from the `Game` god class. Pure,
  JVM-only, **zero Android imports**, living in a separate `:rules` Gradle module
  (`be.submanifold.pente.rules`) so the compiler forbids re-coupling. Narrow interface:
  `replay(moves, variant, untilMove) → BoardState` (recompute-from-scratch, matching the
  app's existing per-refresh rebuild) plus a separate `isWin(state, color, lastMove)` the
  caller invokes only where win is evaluated today. All `detect*` capture/win logic is
  private behind it.
- **BoardState** — the immutable value that crosses the PenteRules seam: board, white/black
  captures, gridSize, lastMove, nullable winner, swap2/dPente decision-point flags, koMove.
  Lean and Pente-only — Go does **not** share this type.
- **GoRules / GoState** — Go's separate module and result (territory, groups, dead-stones,
  ko/pass), kept apart so neither it nor `BoardState` becomes a shallow union.
- **getState() seam** — `Game` holds the latest `BoardState` privately and exposes
  `getState()`; the public `abstractBoard`/`whiteCaptures`/`blackCaptures` fields are deleted.
  Views pull from `getState()` rather than caching the board array by reference (which was the
  aliasing trap). The engine never touches a view (full pull-model).

- **PenteApi** — the deep transport module replacing ~47 hand-rolled `HttpsURLConnection`
  sites and 35 `AsyncTask`s. One fat, synchronous, thread-agnostic interface (~26 methods:
  `login`, `loadIndex`, `loadGame`, `submitMove`, `replyInvitation`, `registerPushToken`, …)
  that buries URL building, cookie injection, dev/prod resolution, timeouts, single re-auth
  retry, and parsing. Real adapter backed by **OkHttp**; a `FakePenteApi` is the second adapter
  for tests.
- **Result<T>** — the typed value crossing the PenteApi seam: `Ok(value)` or `Err(reason)`
  where reason ∈ `NETWORK / AUTH_EXPIRED / INVALID_CREDENTIALS / SERVER(code) / PARSE`.
  Replaces today's swallowed exceptions and HTML-substring success detection. Threading is
  NOT in the interface.
- **Session / BaseUrlProvider** — the adapter owns one `Session` (credentials + a single
  OkHttp `CookieJar`) and a `BaseUrlProvider` (dev/prod resolved once). Collapses the two
  simultaneous cookie stores and the ~18 duplicated `if(development)` toggles. Callers never
  see `name2/password2` or `10.0.2.2`.
- **PenteApiClient** — the thin, lifecycle-aware scheduler that runs synchronous `PenteApi`
  calls off the main thread (single serial `Executor` + main `Handler`) and returns a
  `Cancelable` the Activity cancels in `onDestroy`. Threading and Android lifecycle live here,
  never inside `PenteApi`. Replaces `AsyncTask`.

## Decisions on record (candidate 2: extract PenteRules)

- Seam at the **replay engine**, not the individual detectors.
- **Full pull-model**: engine returns `BoardState`; views read it; engine has no view reference.
- **Win authority preserved**: `isWin()` is called only where it fires today (computer-opponent
  Pente). The server stays authoritative for human games — no eager win-eval.
- **Characterization tests first**: golden snapshots from recorded move-lists before any extraction.
- **C++ (`Ai.cpp`) and `MarksAIPlayer` left independent** — accepted as a parallel, unguarded
  capture implementation for now (the Keryo divergence remains a latent field bug).
- `replay()` only in v1; incremental `applyMove()` deferred until a profiler justifies it.

## Decisions on record (candidate 1: extract PenteApi transport)

- One **fat, deep, synchronous `PenteApi`** returning typed `Result<T>`. Not a generic
  `request()` (shallow) and not grouped sub-interfaces (scatters the shared session concern).
- Threading stays OUT of the interface, in **`PenteApiClient`** (single serial executor first
  — preserves today's `SERIAL_EXECUTOR` ordering, incl. move submission — widened only where
  audited safe).
- Real adapter backed by **OkHttp**: its `CookieJar` is the single session store; its
  `Authenticator` does **transparent single-flight re-auth** (one re-login under a lock on
  concurrent 401s, one retry, else `AUTH_EXPIRED`). Call OkHttp synchronously so the serial
  executor owns ordering. OkHttp honors `network_security_configuration.xml` on minSdk 26.
- **Fix the prod credential-drop bug** (`PentePlayer.java:678`): re-auth keeps
  `name2/password2`, converging to the dev branch — a deliberate behavior change.
- **Strangle migration**: ship interface + both adapters + one low-risk GET with a fake-backed
  test, then convert task-by-task, converging the cookie-filter/retry divergences as you go.
- Legacy HTML endpoints (73/81) map known markers to typed `Result`s; one shared `Gson` for
  the JSON endpoints. Base URL to `BuildConfig`/flavors is a follow-up behind `BaseUrlProvider`.
- **Open integration point**: OkHttp `CookieJar` owns the session, so in-app `WebView`s need a
  one-way sync of the session cookie into `webkit.CookieManager`.

## Known legacy quirks preserved (candidates for a deliberate, separate fix)

The PenteRules extraction is behavior-preserving — it reproduces the old `Game` logic exactly,
bugs included, so fixes can be made later as isolated, reviewable changes rather than smuggled
into a refactor. Two quirks were found and deliberately preserved:

- **`detectPente` misses edge-0 wins** (`Game.java:2227`+): the five-in-a-row walk guards
  `i > 0 && i < 19 && j > 0 && j < 19` on *both* coordinates in every direction, so a horizontal
  five lying on **row 0** or a vertical five on **column 0** is never detected (rows/cols 18 *are*
  detected — asymmetric). Only affects computer-opponent Pente win declaration (server is
  authoritative for human games). Preserved in `DefaultPenteRules.isWin`; `WinDetectionTest`
  documents it (`...NotDetectedLegacyQuirk`).
- **`detectKeryoPoof` had a latent `AIOOBE`** (`Game.java` ~2118): a branch guarded `j+2<n` while
  accessing `[i][j+3]`. This one WAS corrected to `j+3<n` in `DefaultPenteRules` because the
  pattern needs an off-board stone at the boundary, so the fix is provably behavior-preserving on
  every valid board (it only removes a crash) — unlike the edge-0 quirk, which would change real
  outcomes and so was left intact.
- **`Game.isSwap2()` misses "Speed Swap2" games** (`startsWith("Swap2")`): Speed Swap2-Pente /
  Swap2-Keryo are not recognized as Swap2. `Variants` classifies them correctly (`contains("Swap2-")`),
  so `Game.isSwap2()` was deliberately NOT rerouted through `Variants` (it would change behavior);
  `VariantPredicateEquivalenceTest` pins the divergence. Candidate for a deliberate fix.
- **`G_PENTE` is excluded from engine delegation**: `replayGPenteGame` applies a move-2 cross-restriction
  (`-1` cells) the engine doesn't model; differential testing (`PenteRulesEquivalenceTest`) found it,
  so G-Pente keeps the legacy replay path.

## PenteRules wiring status (delivered)

The engine is built, tested, and wired in for the **proven-equivalent, non-rated** variants only —
**PENTE, KERYO_PENTE, POOF_PENTE, O_PENTE, GOMOKU, CONNECT6** (`Game` delegates to `PenteRules.replay`
behind an allowlist + `!rated()` gate, verified bit-identical over 500 random games/variant). All other
variants (G-Pente, Swap2, D-Pente, Go, and any rated game) retain the legacy replay path. The legacy
`replay*/detect*` methods are therefore NOT deleted yet — that awaits broader characterization
(Go/Swap2/rated) or dedicated `GoRules`/swap support. `Game`'s board/capture fields are now private,
exposed via `getState()`; views pull the immutable `BoardState` (aliasing trap closed).
