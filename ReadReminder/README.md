# Read Reminder

Personal reading-habit app for Android 8.0+ (tested target: Android 12). Kotlin, Jetpack Compose (Material 3), Room, DataStore,
AlarmManager. Everything is stored on the phone: no account, no internet, no analytics, no ads.

## Build
* GitHub: push this folder, the workflow in `.github/workflows/build-apk.yml` builds `app-debug.apk` (artifact `ReadReminder-debug-apk`)
  and runs the unit tests (artifact `unit-test-report`).
* Android Studio: open the folder, Build > Build APK(s). CLI: `gradle assembleDebug`, tests: `gradle testDebugUnitTest`,
  UI tests (device/emulator): `gradle connectedDebugAndroidTest`.

## Permissions (and why)
* **Notifications** (`POST_NOTIFICATIONS`, asked on Android 13+; not needed on Android 12): to show reminders.
* **Exact alarms** (`SCHEDULE_EXACT_ALARM`): on-time reminders. Granted by default on Android 12; if revoked, the app falls back to
  inexact alarms (Android may delay them) and the Reminder Schedule screen explains how to allow exact timing.
* **Receive boot completed**: re-arm the alarm after a restart. Time-zone / clock changes are handled the same way.

## How reminders work
One alarm is armed at a time: the next valid reminder (`domain/ReminderPlanner.kt`, unit-tested). Modes: fixed schedule, interval within
active hours, adaptive (fixed schedule, skips reminders within 90 min of logging, adds a "last call"). Quiet hours (may cross midnight),
stop-after-goal, snooze and a single follow-up are respected. A reminder never logs pages by itself; only the "I read N pages" button does.
Battery-saver / OEM "app killers" can still delay alarms on some phones: set the app's battery usage to "Unrestricted" if needed.

## Not implemented
Book cover images, weekly/custom-range history views, per-reminder snooze length, system-ringtone picker (a choice of default sounds is offered).
