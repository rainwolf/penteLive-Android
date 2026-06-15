# penteLive Android App

Android client for [pente.org](https://pente.org) — a Pente board game platform with live multiplayer, AI play, and tournament support.

## Project Overview

- **Package**: `be.submanifold.pentelive`
- **Version**: 2.9.9 (versionCode 183)
- **Language**: Java (primary), C/C++ (native AI engine)
- **Build**: Gradle
- **SDK**: compileSdk 36, minSdk 26 (Android 8.0+), targetSdk 35

## Build

```bash
./gradlew assembleDebug       # Debug build
./gradlew assembleRelease     # Release build
./gradlew installDebug        # Build and install on connected device
```

NDK must be installed for native AI build. The native "Ai" shared library is built from `app/src/main/jni/`.

## Architecture

Activity-centric architecture (pre-MVVM era). No ViewModel/LiveData — async work uses AsyncTask and BroadcastReceivers.

### Key Activities

| Activity | Purpose |
|---|---|
| `SplashActivity` | Launcher, auth check |
| `LoginActivity` / `RegisterActivity` | Authentication |
| `MainActivity` | Dashboard/home |
| `BoardActivity` | Local game vs AI |
| `MMAIActivity` | AI player (Marks/MMAI) mode |
| `LobbyActivity` / `LiveGameRoomActivity` | Real-time multiplayer |
| `KingOfTheHillActivity` | Tournament mode |
| `DatabaseActivity` | Browse past games |
| `SettingsActivity` / `SocialActivity` | Account/social |

### Source Structure

```
app/src/main/
├── java/be/submanifold/pentelive/
│   ├── *.java                    # Core activities, views, models
│   ├── liveGameRoom/             # Real-time multiplayer (State machines, Table, adapters)
│   └── org/pente/gameServer/event/  # Socket event handling
├── jni/                          # Native C++ AI engine
│   ├── Ai.cpp / Ai.h
│   ├── AiWrapper.cpp / AiWrapper.h
│   └── CPoint.cpp / CPoint.h
└── res/
    ├── layout/                   # 47 layout files
    ├── values-night/             # Dark mode
    └── values-de/                # German localization
```

### Key Classes

- **`BoardView.java`** — Custom Canvas-based board rendering
- **`Ai.java`** — JNI wrapper for native C++ AI engine
- **`Table.java`** — Core live game logic (~65KB, complex state)
- **`State.java` / `GameState.java`** — State machine for live game room
- **`JsonModels.java`** — API request/response serialization (GSON)
- **`PrefUtils.java`** — SharedPreferences helpers
- **`MyApplication.java`** — App lifecycle, global state

### Firebase

- Push notifications via `MyFcmListenerService`
- Requires `google-services.json` (not committed — get from Firebase console)

## Key Dependencies

- AndroidX AppCompat 1.7.1, ConstraintLayout 2.2.1
- Material Design 1.13.0
- Firebase Core 21.1.1, Firebase Messaging 25.0.1
- GSON 2.13.2
- MultiDex (enabled)

## Conventions

- All activities locked to **portrait orientation**
- Dark mode supported via `values-night/` resources
- German (`values-de/`) is the only non-English locale
- Network config in `res/xml/network_security_configuration.xml`
- File sharing via `be.submanifold.pentelive.fileprovider`
- No architectural pattern enforcement — follow existing Activity/View patterns when adding features

## Integration with pente.org Backend

- REST API calls via `JsonModels.java` + GSON
- Real-time game sync via raw sockets (`SocketDSGEventHandler`, `ClientSocketDSGEventHandler`)
- Events handled through `DSGEventListener` / `SynchronizedQueue`

# context-mode — MANDATORY routing rules

You have context-mode MCP tools available. These rules are NOT optional — they protect your context window from flooding. A single unrouted command can dump 56 KB into context and waste the entire session.

## BLOCKED commands — do NOT attempt these

### curl / wget — BLOCKED
Any Bash command containing `curl` or `wget` is intercepted and replaced with an error message. Do NOT retry.
Instead use:
- `ctx_fetch_and_index(url, source)` to fetch and index web pages
- `ctx_execute(language: "javascript", code: "const r = await fetch(...)")` to run HTTP calls in sandbox

### Inline HTTP — BLOCKED
Any Bash command containing `fetch('http`, `requests.get(`, `requests.post(`, `http.get(`, or `http.request(` is intercepted and replaced with an error message. Do NOT retry with Bash.
Instead use:
- `ctx_execute(language, code)` to run HTTP calls in sandbox — only stdout enters context

### WebFetch — BLOCKED
WebFetch calls are denied entirely. The URL is extracted and you are told to use `ctx_fetch_and_index` instead.
Instead use:
- `ctx_fetch_and_index(url, source)` then `ctx_search(queries)` to query the indexed content

## REDIRECTED tools — use sandbox equivalents

### Bash (>20 lines output)
Bash is ONLY for: `git`, `mkdir`, `rm`, `mv`, `cd`, `ls`, `npm install`, `pip install`, and other short-output commands.
For everything else, use:
- `ctx_batch_execute(commands, queries)` — run multiple commands + search in ONE call
- `ctx_execute(language: "shell", code: "...")` — run in sandbox, only stdout enters context

### Read (for analysis)
If you are reading a file to **Edit** it → Read is correct (Edit needs content in context).
If you are reading to **analyze, explore, or summarize** → use `ctx_execute_file(path, language, code)` instead. Only your printed summary enters context. The raw file content stays in the sandbox.

### Grep (large results)
Grep results can flood context. Use `ctx_execute(language: "shell", code: "grep ...")` to run searches in sandbox. Only your printed summary enters context.

## Tool selection hierarchy

1. **GATHER**: `ctx_batch_execute(commands, queries)` — Primary tool. Runs all commands, auto-indexes output, returns search results. ONE call replaces 30+ individual calls.
2. **FOLLOW-UP**: `ctx_search(queries: ["q1", "q2", ...])` — Query indexed content. Pass ALL questions as array in ONE call.
3. **PROCESSING**: `ctx_execute(language, code)` | `ctx_execute_file(path, language, code)` — Sandbox execution. Only stdout enters context.
4. **WEB**: `ctx_fetch_and_index(url, source)` then `ctx_search(queries)` — Fetch, chunk, index, query. Raw HTML never enters context.
5. **INDEX**: `ctx_index(content, source)` — Store content in FTS5 knowledge base for later search.

## Subagent routing

When spawning subagents (Agent/Task tool), the routing block is automatically injected into their prompt. Bash-type subagents are upgraded to general-purpose so they have access to MCP tools. You do NOT need to manually instruct subagents about context-mode.

## Output constraints

- Keep responses under 500 words.
- Write artifacts (code, configs, PRDs) to FILES — never return them as inline text. Return only: file path + 1-line description.
- When indexing content, use descriptive source labels so others can `ctx_search(source: "label")` later.

## ctx commands

| Command | Action |
|---------|--------|
| `ctx stats` | Call the `ctx_stats` MCP tool and display the full output verbatim |
| `ctx doctor` | Call the `ctx_doctor` MCP tool, run the returned shell command, display as checklist |
| `ctx upgrade` | Call the `ctx_upgrade` MCP tool, run the returned shell command, display as checklist |
