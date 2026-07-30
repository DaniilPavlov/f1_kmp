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
| UI | Compose Multiplatform, type-safe Navigation (`kotlinx.serialization` routes) |
| Presentation | One ViewModel file per screen; multiple `StateFlow` + `AsyncValue` |
| Domain | Plain Jolpica models; `AppError` / `toAppError()`; `ApiCallHandler`; `AppDataRefresh` |
| DI | Koin; `IF1Repository` / `IEspnRepository` |
| Network | Ktor + kotlinx.serialization DTOs; mappers DTO → domain (`JolpicaMappers`) |
| Backend | Firebase (Analytics, Crashlytics, Remote Config), AppMetrica |
| Images | Coil 3 |
| Cache | JSON files (offline peek → refresh); ESPN in-memory TTL; `AppDataRefresh.clearAll` |
| Time | kotlinx-datetime |
| Android map | OSMDroid + OSMBonusPack (Carto tiles, clustering) |
| iOS map | MapKit pins (no clustering) |

### Differences from f1_kotlin

- Hilt → Koin  
- Retrofit/Moshi → Ktor  
- Room → file cache  
- java.time → kotlinx-datetime  
- OSM map on Android; MapKit pins on iOS  
- Session reminders: AlarmManager (Android) / UNUserNotificationCenter (iOS)  
- Home widgets: Android only (same Next GP + standings as f1_kotlin)  

## Architecture

- **Navigation** — `@Serializable` routes in `F1Routes.kt`; destinations use `toRoute<T>()` / `hasRoute`.
- **Domain** — UI/ViewModel работают с `domain.model` (`Driver`, `Race`, …). Jolpica отдаёт JSON → kotlinx DTO (`data.model`) → `JolpicaMappers.toDomain()`.
- **Errors** — repositories return `Result`; failures map via `Throwable.toAppError()` → `AppError` for UI.
- **Repositories** — `IF1Repository` + `IEspnRepository`; concrete impls bound in Koin.
- **ViewModels** — one file per screen under `viewmodel/` (no giant shared files).
- **Refresh** — `AppDataRefresh.clearAll()` (Facade) resets ESPN TTL + file cache; `refreshAll()` on main screens calls it before reload (ErrorBody / pull-to-refresh).
- **Patterns (GoF)** — Factory (`HttpClient`), Facade (`AppDataRefresh`), Template Method (`ApiCallHandler`), Proxy (peek cache), Adapter (ESPN/`JolpicaMappers`/`IRemoteConfigService`), State (`AsyncValue`), Command (`ErrorBody`), Bridge (map expect/actual), Singleton (`LocaleController` / Firebase·AppMetrica bootstrap / `ForceUpdateGate`).
- **Firebase** — project `f1-kmp`; Android `composeApp/google-services.json` (gitignored); iOS `GoogleService-Info.plist` (gitignored) + SPM. Analytics/Crashlytics off in debug.
- **AppMetrica** — Android `local.properties` (`appmetrica.apiKey`); iOS `Config.local.xcconfig` (`APPMETRICA_API_KEY`). Empty → skip; crashes via Crashlytics.
- **Remote Config** — `min_app_version` (force update), `local_notifications_enabled` (reminder kill-switch).

## Secrets

Not in git.

### Firebase (`f1-kmp`)

1. [Firebase Console](https://console.firebase.google.com/project/f1-kmp/overview) → Project settings → Your apps  
2. Android package **`com.example.f1_kmp`** → `composeApp/google-services.json` (gitignored)  
3. iOS bundle **`com.example.f1kmp`** → `iosApp/iosApp/GoogleService-Info.plist` (gitignored)  
4. Enable **Analytics**, **Crashlytics**, **Remote Config**  

Without a real Android JSON, Gradle copies `tool/ci/google-services.stub.json` so CI/local still builds.

CI secret `GOOGLE_SERVICES_JSON` — полное содержимое `google-services.json` (release workflow).

Remote Config keys: `local_notifications_enabled` (bool), `min_app_version` (string semver).

### AppMetrica

Android — optional in `local.properties` (gitignored):

```properties
appmetrica.apiKey=...
```

iOS — copy example and fill:

```bash
cp iosApp/Configuration/Config.local.xcconfig.example iosApp/Configuration/Config.local.xcconfig
# set APPMETRICA_API_KEY=...
```

`Config.xcconfig` includes `Config.local.xcconfig` optionally; ключ в Info.plist через `$(APPMETRICA_API_KEY)`.

CI secret `APPMETRICA_API_KEY` — для Android release (пишется в `local.properties`).

### iOS SPM packages (Xcode)

File → Add Package Dependencies:

- `https://github.com/firebase/firebase-ios-sdk` → FirebaseAnalytics, FirebaseCrashlytics, FirebaseRemoteConfig  
- `https://github.com/appmetrica/appmetrica-sdk-ios` → AppMetricaCore  

Without packages the app still builds (`#if canImport`); Firebase/AppMetrica stay inactive and RC uses defaults.

### Release keystore

Локально (gitignored):

- `composeApp/upload-keystore.jks`
- `key.properties` в корне (`storeFile=upload-keystore.jks`, alias `upload`)

Сборка:

```bash
./gradlew :composeApp:assembleRelease
```

Для GitHub Release secrets:

```bash
base64 -i composeApp/upload-keystore.jks | pbcopy   # → ANDROID_KEYSTORE_BASE64
```

| Secret | Purpose |
|--------|---------|
| `ANDROID_KEYSTORE_BASE64` | Base64 of `upload-keystore.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | `upload` |
| `ANDROID_KEY_PASSWORD` | key password |

Без secrets release APK в CI уходит с **debug**-подписью. Пароли — в `key.properties`; файл и `.jks` не коммитить.

## Structure

```
f1_kmp/
├── composeApp/          # shared + Android + iOS Kotlin
│   └── src/
│       ├── commonMain/  # UI, ViewModel, API, repository, circuit assets
│       ├── androidMain/ # Activity, OSMDroid, OkHttp, reminders, share
│       ├── iosMain/     # MainViewController, Darwin HTTP, MapKit
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
./gradlew :composeApp:detekt
```

Covered areas include ViewModels (Home, Results, Schedule, News, Race search, H2H drivers, Finish status, Race info, Circuit detail), Jolpica mappers, CareerLoader, `ApiCallHandler`, `AppVersion`, date/flag utils.

## CI / CD

[![CI](https://github.com/DaniilPavlov/f1_kmp/actions/workflows/ci.yml/badge.svg)](https://github.com/DaniilPavlov/f1_kmp/actions/workflows/ci.yml)

| Workflow | When | What it does |
|----------|-------|------------|
| `ci.yml` | push / PR to `master` | Firebase stub, detekt, debug APK, unit tests |
| `release.yml` | tag `v*` or manual | Android APK (+ GitHub Release) |

Release:

```bash
# version in composeApp/build.gradle.kts must match the tag
git tag v1.6.0
git push origin v1.6.0
```

For release APK signing (optional) — `ANDROID_KEYSTORE_*` secrets in GitHub Actions.  
Also: `GOOGLE_SERVICES_JSON`, `APPMETRICA_API_KEY` (else stub / skip).

## Circuits map

On Android, **Circuits → On map** shows OSMDroid with pins and clusters (Carto tiles).

On iOS the same tab uses **MapKit** with pins (no clustering).  
The circuit list and circuit card work on both platforms.

## Offline

The app reads the local JSON cache first (peek), then refreshes from the network.  
If the network is unavailable but cache exists, the UI keeps the last known data.  
ESPN news/scoreboard use a short in-memory TTL.  
Forced reload (`refreshAll`) clears ESPN + file caches via `AppDataRefresh`.

## Features

- **Home** — current season driver and constructor standings  
- **Results** — weekend scoreboard (ESPN, live poll), latest race, race search, hall of fame, season rewind, H2H (drivers / constructors + points chart), finish statuses  
- **Calendar** — monthly calendar with session times; on empty days shows next GP card (layout + countdown); local reminders 30 min before (Android + iOS)  
- **News** — F1 headlines from ESPN  
- **Circuits** — list and map (OSMDroid + Carto on Android; MapKit pins on iOS), track layouts, length/laps/turns/speed/elevation, Wikipedia, winners history  
- **Driver / Constructor cards** — ESPN photos/news, career stats with tappable wins / podiums / poles lists  
- **Themes** — system / light / dark cycle in the app bar  
- **Localization** — Russian (default) and English, toggle in the app bar without restarting the app  
- **Live banner** — red session-live strip above the bottom bar → Results  
- **Deep links** — `f1pet://driver|constructor|circuit|race/...` (Android + iOS)  
- **Home widgets** (Android) — next GP countdown + top-3 standings  
- **Reminders** — local notifications 30 minutes before a session (up to 10 upcoming; Android AlarmManager / iOS UNUserNotificationCenter); Remote Config kill-switch  
- **Force update** — blocking screen when app version is below Remote Config `min_app_version`  
- **Analytics** — typed events to Firebase Analytics + AppMetrica  
- **Schedule cache** — shared file JSON cache for the calendar and reminders  
- **Offline** — file JSON cache with instant peek and network refresh  
- **Share** — career / race / weekend cards (PNG on Android), circuit `f1pet://` text; text share sheet on iOS  
- **Shimmer skeletons** — loading placeholders for main screens  
- **Country flags** — nationality / country as emoji in tables, career cards, circuits, scoreboard  
