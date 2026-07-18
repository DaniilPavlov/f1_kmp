# F1 KMP

Kotlin Multiplatform + Compose Multiplatform приложение со статистикой Formula 1  
(турнирные таблицы, результаты, календарь, зал славы, трассы).

Порт Android-проекта **[f1_kotlin](https://github.com/DaniilPavlov/f1_kotlin)** на общий код для **Android** и **iOS**.  
Данные — [Jolpica F1 API](https://github.com/jolpica/jolpica-f1) (совместим с Ergast).

## Стек

| Слой | Технологии |
|------|------------|
| UI | Compose Multiplatform, Navigation |
| DI | Koin |
| Сеть | Ktor + kotlinx.serialization |
| Кэш | JSON-файлы (offline peek → refresh) |
| Время | kotlinx-datetime |
| Android-карта | OSMDroid |
| iOS-карта | заглушка (см. ниже) |

### Отличия от f1_kotlin

- Hilt → Koin  
- Retrofit/Moshi → Ktor  
- Room → файловый кэш  
- java.time → kotlinx-datetime  
- Карта OSM только на Android  
- Напоминания о сессиях — только Android (AlarmManager)

## Структура

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

## Требования

- JDK 17+
- Android Studio / Android SDK (для Android)
- Xcode 15+ (для iOS)
- macOS (для сборки iOS)

## Android

```bash
./gradlew :composeApp:assembleDebug
# или установка на устройство/эмулятор:
./gradlew :composeApp:installDebug
```

В Android Studio: Run → конфигурация **composeApp**.

## iOS

Сборку iOS нужно запускать **через Xcode** (Compose-ресурсы требуют параметров от Xcode):

```bash
open iosApp/iosApp.xcodeproj
```

1. Выбери схему **iosApp** и симулятор (например iPhone 16).
2. Run (⌘R).

Xcode перед сборкой вызовет:

```text
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

На реальное устройство в `iosApp/Configuration/Config.xcconfig` укажи свой `TEAM_ID`.

**Не запускай** `embedAndSignAppleFrameworkForXcode` «голым» Gradle из терминала/Android Studio — будет ошибка  
`Could not infer iOS target architectures`. Нужен Xcode / `xcodebuild`.

Только Kotlin-фреймворк для симулятора (Apple Silicon), без установки приложения:

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Тесты

```bash
./gradlew :composeApp:testDebugUnitTest
```

## Карта трасс на iOS

На Android вкладка «Трассы → На карте» показывает OSMDroid с пинами и кластерами.

На iOS карта — **заглушка**: текст с предложением открыть список.  
Список трасс и карточка трассы работают на обеих платформах.  
Полноценная карта (MapKit) пока не подключена.

## Offline

Приложение сначала читает локальный JSON-кэш (peek), затем обновляет данные с сети.  
Если сеть недоступна, а кэш есть — UI остаётся с последними данными.

## Возможности

- **Главная** — турнирные таблицы пилотов и конструкторов текущего сезона  
- **Результаты** — последняя гонка, поиск гонки по году и раунду, детальная карточка (гонка, спринт, квалификация, пит-стопы)  
- **Календарь** — расписание сезона с сессиями уик-энда (практики, квалификация, спринт, спринт-квалификация, гонка)  
- **Зал славы** — итоговые таблицы пилотов и конструкторов за выбранный год  
- **Трассы** — список и карта (OSMDroid на Android; на iOS — заглушка), карточка трассы и ссылка на Wikipedia  
- **Карточка пилота** — по нажатию на строку в таблицах (код, номер, национальность, дата рождения, Wikipedia)  
- **Локализация** — русский и английский, переключатель в верхней панели без перезапуска приложения  
- **Напоминания** — локальные уведомления за 30 минут до сессий на Android (в ОС держим 10 ближайших, окно обновляется при открытии приложения)  
- **Offline** — файловый JSON-кэш с мгновенным peek и обновлением с сети  
