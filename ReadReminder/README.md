# Read Reminder — Stage 1 (source only, not yet compiled)

Native Android (Kotlin, Compose Material 3, Room, DataStore, manual DI).

## Build
1. Install Android Studio (Koala or newer, JDK 17+).
2. File > Open > this folder; let Gradle sync (it downloads Gradle 8.9 + dependencies).
3. Run on a device/emulator, or Build > Build APK(s). CLI: `gradle wrapper` once, then `./gradlew assembleDebug`
   -> app/build/outputs/apk/debug/app-debug.apk
4. Unit tests: `./gradlew testDebugUnitTest`

## Implemented so far
- Room schema v1 (books, reading_sessions, daily_goals snapshots, reminders, achievements), DAOs
- DataStore settings (daily goal, reset time)
- Streak + logical-day (reset time) logic, with unit tests
- Home: animated progress ring, mascot, messages, streak, quick-add logging with validation, records list, delete with confirmation
- Settings: daily goal presets + custom, persisted; goal snapshot per day
- Bottom navigation Home / History / Settings with state restore

## Not yet implemented
History/charts/stats, book library, session edit, achievements, reminders & notifications,
backup/CSV, dark theme, UI tests. App icon is a simple placeholder vector.
