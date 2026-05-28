# Android Arena Mode — Design Spec

**Date:** 2026-05-28
**Target repo:** `penteLive-Android` (branch `feat/arena`)
**Source of truth:** iOS PR rainwolf/pentelive-ios #4 ("Feat/arena")

## Goal

Port the Arena feature from iOS PR #4 to the Android app at full 1:1 functional
parity. Arena is a special game room where a host creates a table with chosen
settings, players send join *requests* (rather than joining directly), and the
host accepts or rejects incoming requests from a live list with a per-request
countdown.

## Approach

Mirror the iOS implementation: add inline arena awareness (`isArena` /
`isArenaTable` flags) to the existing room and table fragments, add two new
focused UI components, and add one new branch to the central event-dispatch
chain. No new Activity. This keeps the Android structure aligned with the iOS PR
for easy cross-platform review and co-maintenance, and matches how this codebase
already branches on state and event type.

Rejected alternatives:
- **Arena subclasses** (`ArenaTableFragment extends LiveTableFragment`): the
  fragments were not built for subclassing; high regression risk, diverges from
  iOS structure.
- **Arena helper/controller**: concentrates logic but adds indirection and does
  not line up with the iOS layout for cross-referencing.

## Background

### iOS feature (what we are matching)
- Arena room detected by room name containing "arena" (iOS also keys off port
  15999).
- `ArenaTableSetupView.swift` — create-table settings form (game, rated,
  play-as color when unrated, timed, initial minutes, incremental seconds,
  "Create table") → emits `dsgArenaCreateTableEvent`.
- `ArenaJoinRequestList.swift` — host's list of join requesters; each row shows
  player name + rating with a 6-second countdown and an animated shrinking
  progress line; auto-removes on expiry; tap = accept, swipe = reject.
- Room/table controllers branch on `isArena` / `isArenaTable` to swap the
  join action, show the "+" create action, and present/dismiss the request list.

### Android architecture mapping
| iOS | Android |
|-----|---------|
| `RoomViewController` | `liveGameRoom/LiveGameRoomFragment.java` (+ `LiveGameRoomActivity.java`) |
| `TableViewController` | `liveGameRoom/LiveTableFragment.java` |
| `LobbyViewController` | `liveGameRoom/LobbyActivity.java` |
| `TableSetupView` (`live_table_settings`) | `live_table_settings.xml` inflated in `LiveTableFragment` (~L663) |
| `PenteLiveSocket` dispatch | `LiveGameRoomActivity` dispatch chain (~L230+) + `sendEvent(String)` (L410) |
| `ArenaTableSetupView` | **new** arena settings form + `arena_table_settings.xml` |
| `ArenaJoinRequestList` | **new** `ArenaJoinRequestAdapter` + `arena_join_request_row.xml` |

Key Android facts:
- Events are sent as **raw JSON strings** via `LiveGameRoomActivity.sendEvent(String)`.
- Incoming events route through a single `if / else if (jsonEvent.get("...") != null)`
  chain in `LiveGameRoomActivity`.
- The room is delivered to `LiveGameRoomActivity` as the Intent extra `"room"`
  (set in `LobbyActivity` L66-67).
- UI stack is classic Views: XML layouts + `RecyclerView`/`ListAdapter`. No Compose.
- Local test backend: `PentePlayer.development` (PentePlayer.java:33, default `false`).

## Protocol — arena events

All outgoing events are raw JSON strings via `sendEvent`. **Two deliberate
divergences from the iOS PR** (the backend protocol is authoritative; iOS may be
reconciled separately):

| Event | Direction | Payload |
|-------|-----------|---------|
| `dsgArenaCreateTableEvent` | out | `{timed, initialMinutes, incrementalSeconds, rated, game, playAs, player, table:-1, time:0}` |
| `dsgArenaRequestJoinTableEvent` | out | `{player:<me>, table:<id>, time:0}` — **adds `player`** (iOS omitted it) |
| `dsgArenaAcceptTableJoinEvent` | out | `{player:<me>, playerToAccept:<name>, table:<id>}` |
| `DSGArenaRejectTableJoinEvent` | out | `{player:<me>, playerToReject:<name>, table:<id>, message:null}` — **fixes `mesage`→`message`** (note: capital `DSG` preserved) |
| `dsgArenaRequestJoinTableEvent` | in | host receives a requester's name; routed to the active table fragment |

## Components

### New: `ArenaJoinRequestAdapter` (+ `arena_join_request_row.xml`)
Backs the host's join-request list (`RecyclerView`/`ListView` consistent with
existing adapters such as `TableListAdapter`).
- Holds an ordered list of requester names; `addPlayer(name)` appends (no dedupe,
  matching iOS) and starts a 6s countdown for that row.
- Each row: player name + rating (via `TablesAndPlayers`), plus an animated
  shrinking progress bar over the 6s window.
- Auto-removes a row when its countdown expires.
- Tap row → send `dsgArenaAcceptTableJoinEvent`. Swipe-to-dismiss → send
  `DSGArenaRejectTableJoinEvent`.
- `reset()` cancels all timers and clears the list.

### New: arena create-table form (+ `arena_table_settings.xml`)
Clone of `live_table_settings.xml` minus "private", plus a "play as" color row.
- Fields: game spinner, rated toggle, play-as color (shown only when unrated),
  timed toggle, initial-minutes and incremental-seconds inputs, "Create table".
- Submit → `dsgArenaCreateTableEvent` (`table:-1`).
- Shown via `AlertDialog`, same mechanism as the existing settings dialog.
- Guests cannot toggle rated (mirrors iOS guard).

### Changed: `LiveGameRoomFragment`
- `isArena` flag, set when the current room's name contains "arena".
- When arena: render tables-only (no players/challenges segments) and a "+"
  action that opens the arena create-table form.
- **Arena table list filter:** show only tables with **≤ 1 player**; hide any
  table that already has more than one player.
- Table tap sends `dsgArenaRequestJoinTableEvent` (request) instead of
  `dsgJoinTableEvent`, and shows a waiting state until accepted.

### Changed: `LiveTableFragment`
- `isArenaTable` flag; owns the `ArenaJoinRequestAdapter`.
- While the host sits alone (`players == 1`), present the request list.
- Incoming arena join request → `arenaTableRequestJoinEvent(name)` →
  `adapter.addPlayer(name)`.
- **Dismiss the request list only when a player actually joins** the table
  (player count transitions to 2) — not on reject and not on countdown expiry.
- On exit / `onDestroyView`, call `adapter.reset()` and dismiss.

### Changed: `LiveGameRoomActivity`
- One new `else if (jsonEvent.get("dsgArenaRequestJoinTableEvent") != null)`
  branch in the dispatch chain, routing the requester name to the active table
  fragment's `arenaTableRequestJoinEvent`.

## Behavioral flow

1. **Room detection:** `LobbyActivity` passes the `room` extra →
   `LiveGameRoomActivity`/`Fragment` sets `isArena` when the room name contains
   "arena". Arena room renders tables-only + "+" and filters the table list to
   open tables (≤ 1 player).
2. **Create:** "+" → arena settings dialog → submit → `dsgArenaCreateTableEvent`
   (`table:-1`). Server creates the table and seats the owner → opens
   `LiveTableFragment` with `isArenaTable = true`.
3. **Host waiting:** while `players == 1`, show the request list. Each incoming
   `dsgArenaRequestJoinTableEvent` → `addPlayer(name)`, starting a 6s row
   countdown. Tap = accept, swipe = reject, auto-remove on expiry.
4. **Joiner:** tapping an open arena table sends request-join and shows a waiting
   state until accepted (normal join proceeds) or it lapses.
5. **Match start:** when a player joins (`players == 2`), dismiss the request
   list; the game proceeds as a normal live table.
6. **Exit:** leaving resets the adapter (cancels all timers) and dismisses.

## Edge cases / error handling

- **Timer lifecycle:** cancel per-row countdown timers on removal, on `reset()`,
  and on fragment `onDestroyView` (mirrors iOS `deinit` invalidation) to prevent
  leaks/crashes on rapid enter/exit.
- **Stale row actions:** bounds-check before accept/reject in case a row expired
  between render and tap.
- **Guests:** cannot toggle rated in the create form.
- **Play-as:** color row visible only when unrated (rated games auto-assign).
- **Event-name collision:** `dsgArenaRequestJoinTableEvent` is used in both
  directions; the dispatch branch handles only the incoming/host case.
- **Duplicate requests:** append (match iOS — no dedupe).

## Testing / verification

- Set `PentePlayer.development = true` → connects to the fully functional
  localhost backend.
- `./gradlew assembleDebug` to compile.
- Emulator smoke test: log in, enter Arena, create a table, confirm the create
  event fires and the host-waiting UI appears, and that the table list filters
  to open tables.
- **Limitation:** the full accept/reject round-trip needs a second client; that
  part is verified on-device by the user.

## Out of scope

- Backend/server changes (arena protocol already supported).
- iOS reconciliation of the two protocol divergences (`player` key,
  `message` spelling).
- Any redesign of the existing non-arena room/table/lobby behavior.
