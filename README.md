# F1 KMP

Kotlin Multiplatform + Compose Multiplatform app with Formula 1 stats  
(standings, results, calendar, news, circuits).

Data:
- [Jolpica F1 API](https://github.com/jolpica/jolpica-f1) (Ergast-compatible) — schedule, results, standings
- [ESPN](https://site.api.espn.com/) — news, weekend scoreboard, driver photos

Same idea, other stacks:

- [f1_pet_project](https://github.com/DaniilPavlov/f1_pet_project) — Flutter (Android / iOS / Web)
- [f1_kotlin](https://github.com/DaniilPavlov/f1_kotlin) — native Android

## Stack

| Layer | Tech |
|------|------------|
| UI | Compose Multiplatform, Navigation |
| DI | Koin |
| Network | Ktor + kotlinx.serialization (Jolpica + ESPN clients) |
| Images | Coil 3 |
| Cache | JSON files (offline peek → refresh); ESPN in-memory TTL |
| Time | kotlinx-datetime |
| Android map | OSMDroid + OSMBonusPack (Carto tiles) |
| iOS map | MapKit pins |

### Differences from f1_kotlin

- Hilt → Koin  
- Retrofit/Moshi → Ktor  
- Room → file cache  
- java.time → kotlinx-datetime  
- OSM map on Android only; MapKit pins on iOS  
- Session reminders: AlarmManager (Android) / UNUserNotificationCenter (iOS)

## Structure

```
f1_kmp/
├── composeApp/          # shared + Android + iOS Kotlin
│   └── src/
│       ├── commonMain/  # UI, ViewModel, API, repository, circuit assets
│       ├── androidMain/ # Activity, OSMDroid, OkHttp, reminders, share
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

On iOS the map uses **MapKit** with pins (no clustering).  
The circuit list and circuit card work on both platforms.

## Offline

The app reads the local JSON cache first (peek), then refreshes from the network.  
If the network is unavailable but cache exists, the UI keeps the last known data.  
ESPN news/scoreboard use a short in-memory TTL.

## Features

- **Home** — current season driver and constructor standings  
- **Results** — weekend scoreboard (ESPN, live poll), latest race, race search, hall of fame, H2H (drivers / constructors), finish statuses  
- **Calendar** — monthly calendar with session times; on empty days shows next GP card (layout + countdown); local reminders 30 min before (Android)  
- **News** — F1 headlines from ESPN  
- **Circuits** — list and map (OSMDroid + Carto on Android; stub on iOS), track layouts, length/laps/turns/speed/elevation, Wikipedia, winners history  
- **Driver / Constructor cards** — ESPN photos/news, career stats with tappable wins / podiums / poles lists  
- **Localization** — Russian and English, toggle in the app bar without restarting the app  
- **Reminders** — local notifications 30 minutes before a session (up to 10 upcoming; Android AlarmManager / iOS UNUserNotificationCenter)  
- **Schedule cache** — shared file JSON cache for the calendar and reminders  
- **Offline** — file JSON cache with instant peek and network refresh  
- **Share** — career stats and race results (PNG on Android, text share sheet on iOS)  
- **Shimmer skeletons** — loading placeholders for main screens  
- **Country flags** — nationality / country as emoji in tables, career cards, circuits, scoreboard  
