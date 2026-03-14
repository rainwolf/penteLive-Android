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
