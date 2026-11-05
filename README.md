# Tasks (Android)

Thin Android client for the personal Tasks API (`https://api.matias-ceau.fr`).
Pure network client: no local database, no backend code.

Kotlin 2.1 · Jetpack Compose (Material3, dark) · Retrofit + kotlinx.serialization · coroutines · ViewModel/StateFlow · DataStore Preferences.
minSdk 26, targetSdk 35. Package: `fr.matiasceau.tasks`.

## Features

- Task list sorted by `urgency` (descending); overdue tasks in red, tasks due today in orange
- Pull-to-refresh
- `+` FAB → add dialog (description required, ≤ 500 chars; project; tags as CSV; priority H/M/L; optional due date, sent as 18:00 local time)
- Swipe right → `POST /v1/tasks/{id}/done`, then refresh
- Long press → confirmation → `DELETE /v1/tasks/{id}`
- Network/API errors (401/403/400/502, offline) → snackbar, never a crash

## Where to put the API key

In the app: the **Settings** screen (gear icon). It opens automatically on first launch while the key is empty.
- **Base URL**: defaults to `https://api.matias-ceau.fr`
- **API key**: sent as the `X-API-Key` header

Both values are stored in DataStore Preferences (`settings.preferences_pb`, app-private storage, **not encrypted**; `allowBackup=false`).

## Project structure

```
app/src/main/java/fr/matiasceau/tasks/
├── MainActivity.kt          # entry point, Settings ⇄ list switching, snackbar
├── TasksViewModel.kt        # UI state (StateFlow), API calls, error → message
├── data/
│   ├── Models.kt            # Task, TaskList, NewTask (@Serializable)
│   ├── TasksApi.kt          # Retrofit interface + client (X-API-Key interceptor), error mapping
│   └── SettingsRepository.kt# DataStore: base URL + API key
└── ui/
    ├── TasksScreen.kt       # list, swipe-to-done, long-press delete, FAB, pull-to-refresh
    ├── AddTaskDialog.kt     # add form + DatePicker
    ├── SettingsScreen.kt    # base URL / API key
    ├── Due.kt               # due date parsing, overdue/today, API format
    └── theme/Theme.kt       # dark Material3 colors
gradle/libs.versions.toml    # version catalog
```

## Build

Requirements: JDK 17, Android SDK with `platforms;android-35` and `build-tools;35.x`.

The Gradle wrapper jar is not committed. Generate it once (requires a system Gradle ≥ 8.11, e.g. `pacman -S gradle`),
or simply open the project in Android Studio:

```bash
gradle wrapper --gradle-version 8.11.1
```

Then:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=/opt/android-sdk        # or create local.properties with sdk.dir=...
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
