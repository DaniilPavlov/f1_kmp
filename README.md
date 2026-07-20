# F1 KMP

Kotlin Multiplatform + Compose Multiplatform app with Formula 1 stats  
(standings, results, calendar, hall of fame, circuits).

Data — [Jolpica F1 API](https://github.com/jolpica/jolpica-f1) (Ergast-compatible).

Same idea, other stacks:

- [f1_pet_project](https://github.com/DaniilPavlov/f1_pet_project) — Flutter (Android / iOS)
- [f1_kotlin](https://github.com/DaniilPavlov/f1_kotlin) — native Android (source for this repo)

## Stack

| Layer | Tech |
|------|------------|
| UI | Compose Multiplatform, Navigation |
| DI | Koin |
| Network | Ktor + kotlinx.serialization |
| Cache | JSON files (offline peek → refresh) |
| Time | kotlinx-datetime |
| Android map | OSMDroid |
| iOS map | stub (see below) |

### Differences from f1_kotlin

- Hilt → Koin  
- Retrofit/Moshi → Ktor  
- Room → file cache  
- java.time → kotlinx-datetime  
- OSM map on Android only  
- Session reminders on Android only (AlarmManager)

## Structure

```
f1_kmp/
├── composeApp/          # shared + Android + iOS Kotlin
│   └── src/
│       ├── commonMain/  # UI, ViewModel, API, repository
│       ├── androidMain/ # Activity, OSMDroid, OkHttp
│       ├── iosMain/     # MainViewController, Darwin HTTP
│       ├── commonTest/
│       └── androidUnitTest/
├── iosApp/              # Xcode host (SwiftUI → Compose)
└── gradle/
```

## Requirements

- JDK 17+
- Android Studio / Android SDK (for Android)
- Xcode 15+ (for iOS)
- macOS (for iOS builds)

## Android

```bash
./gradlew :composeApp:assembleDebug
# install on device/emulator:
./gradlew :composeApp:installDebug
```

In Android Studio: Run → **composeApp** configuration.

## iOS

Build iOS **through Xcode** (Compose resources need Xcode-provided parameters):

```bash
open iosApp/iosApp.xcodeproj
```

1. Select the **iosApp** scheme and a simulator (e.g. iPhone 16).
2. Run (⌘R).

Xcode will call before the build:

```text
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

For a real device, set your `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.

**Do not run** `embedAndSignAppleFrameworkForXcode` from Gradle alone in the terminal/Android Studio — you will get  
`Could not infer iOS target architectures`. Use Xcode / `xcodebuild`.

Kotlin framework for the simulator only (Apple Silicon), without installing the app:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Tests

```bash
./gradlew :composeApp:testDebugUnitTest
```

## CI / CD

[![CI](https://github.com/DaniilPavlov/f1_kmp/actions/workflows/ci.yml/badge.svg)](https://github.com/DaniilPavlov/f1_kmp/actions/workflows/ci.yml)

| Workflow | When | What it does |
|----------|-------|------------|
| `ci.yml` | push / PR to `master` | build debug APK, unit tests |
| `release.yml` | tag `v*` or manual | Android APK (+ GitHub Release) |

Release:

```bash
# version in composeApp/build.gradle.kts must match the tag
git tag v1.2.0
git push origin v1.2.0
```

For release APK signing (optional) — `ANDROID_KEYSTORE_*` secrets in GitHub Actions.

## Circuits map on iOS

On Android, the **Circuits → On map** tab shows OSMDroid with pins and clusters.

On iOS the map is a **stub**: text suggesting to open the list.  
The circuit list and circuit card work on both platforms.  
A full map (MapKit) is not connected yet.

## Offline

The app reads the local JSON cache first (peek), then refreshes from the network.  
If the network is unavailable but cache exists, the UI keeps the last known data.

## Features

- **Home** — current season driver and constructor standings  
- **Results** — latest race, search by season and race (pickers), detail card (race, sprint, qualifying, pit stops)  
- **Calendar** — season schedule with weekend sessions (practice, qualifying, sprint, sprint qualifying, race)  
- **Hall of fame** — final driver and constructor tables for a selected year (season picker from Jolpica)  
- **Circuits** — list and map (OSMDroid + Carto on Android; stub on iOS), circuit card, Wikipedia link, and race winners history  
- **Driver card** — full screen with passport data and career stats (races, wins, podiums, poles, teams) from Jolpica endpoints  
- **Constructor card** — nationality, Wikipedia link, career stats, and drivers list  
- **Localization** — Russian and English, toggle in the app bar without restarting the app  
- **Reminders** — local notifications 30 minutes before a session on Android (up to 10 upcoming kept in the OS; window refreshes when the app opens)  
- **Schedule cache** — shared file JSON cache for the calendar and reminders  
- **Offline** — file JSON cache with instant peek and network refresh  
