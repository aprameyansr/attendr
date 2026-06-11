# Attendr - College Attendance Tracker

Attendr is a modern, offline-first collegiate attendance tracker designed to streamline schedule management, calculate academic safety nets, and log daily attendance with absolute speed. 

Built entirely with **Jetpack Compose** and **Material Design 3**, Attendr removes the friction of tracking academic records. With background automation powered by **WorkManager**, Attendr sends notification alerts at the end of each scheduled period, allowing you to mark yourself present or absent directly from your device's notification shade—without ever opening the app.

---

## 🚀 Key Features

* **📅 Smart Timetable Integration**: Define weekly period blocks, map custom courses/classes, and configure schedule slots down to the exact minute.
* **🔄 Drag-and-Drop Schedule Reordering**: Dynamically reorder classes on the fly for any day's view with seamless list reordering mechanics.
* **⚡ Smart Notifications (Interactive Logging)**: 
  * Powered by **WorkManager** background workers.
  * Receive alerts at the exact moment a class ends.
  * Direct action handles (`Present` / `Absent` / `Cancelled`) allow one-tap attendance marking right from the notification panel.
* **📈 Dual Attendance Tracking Engines**:
  * **Normal Mode**: Tracks standard class metrics (Lectures Attended / Total Held) with custom period bounds.
  * **Fixed Total / Hours Earned Mode**: Perfect for course systems requiring target hour completions (e.g., earn 2 hours per session toward an 80-hour total target).
* **🛠️ Temporary Schedule Overrides**: Handle sudden room changes, extra classes, or cancellations for any specific date without disrupting your weekly master timetable template.
* **🧮 Interactive Bunk Calculator**: Real-time analytical simulator that calculates exactly how many consecutive lectures you can safely skip (or must attend) to stay above your custom target percentage (e.g., 75%).
* **🎨 Material 3 Aesthetic & Dynamic Styling**: Beautiful dark visual layout with custom accent selectors, container card elevations, and robust custom modern colors mapped per-course for scanning speed.

---

## 🏗️ Architecture & Stack

Attendr utilizes standard modern Android engineering practices:

* **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) (100% Kotlin-based declarative UI)
* **Architecture Pattern**: MVVM (Model-View-ViewModel) + Repository Pattern
* **Database Engine**: [Room Database](https://developer.android.com/training/data-storage/room) (Local SQLite compilation with KSP compile-time check verification)
* **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (Reliable scheduling of end-of-class reminders that runs synchronously and persists across system boots)
* **Data Stream Reactive flow**: Kotlin [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/) for real-time reactivity between database layers and UI states.
* **Serialization**: Moshi / Kotlinx Serialization

---

## 📂 Project Structure

```text
com.example/
├── AttendanceApplication.kt     # App context setup, channels creation & WorkManager trigger init
├── MainActivity.kt              # Top-level entry activity, Edge-to-Edge activation, Navigation host
├── data/
│   ├── database/                # AppDatabase instantiation, Room migrations
│   ├── dao/                     # Room DAOs for Courses, Records, Timetable, Notifications
│   ├── model/                   # Data classes / Room Entities (Course, AttendanceRecord, PeriodDefinition)
│   └── repository/              # Main repository orchestrators consolidating data operations
├── ui/
│   ├── screens/                 # Compose Views (HomeScreen, AttendanceScreen, TimetableScreen, etc.)
│   ├── viewmodel/               # ViewModels managing state flows and screen interactions
│   ├── components/              # Shared Custom Composables (cards, stat-circles, dialogs)
│   └── theme/                   # Material 3 typography, custom color schemes, shapes
├── worker/
│   ├── PeriodNotificationWorker.kt # WorkManager task dispatching individual end-of-class alerts
│   └── NotificationScheduler.kt    # Logic calculating upcoming period markers and scheduling alarms
└── receiver/
    ├── AttendanceActionReceiver.kt # Broadcast receiver capturing notification direct action clicks
    └── BootReceiver.kt             # Re-aligns alarms/notifications schedule when device reboots
```

---

## ⚙️ How to Build and Run in Android Studio

If you imported this project into Android Studio, follow these short instructions to compile and install:

### 1. Requirements
* **Android Studio**: Ladybug / Meerkat (or newer)
* **JDK Version**: Java 17+ or Java 21
* **Android SDK**: `compileSdk = 35` / `minSdk = 26` (Android 8.0+)

### 2. Gradle & Dependencies
Your project configuration is managed entirely in `gradle/libs.versions.toml` and module-level `build.gradle.kts` scripts. 
* **Gradle Wrapper Version**: `9.3.1` (or your current active stable wrapper)
* **Android Gradle Plugin (AGP)**: `9.1.1` (or local stable release)

### 3. Smart Debug Signing Handling
The build configuration in `app/build.gradle.kts` has been updated to support standard flexible keystore configurations:
```kotlin
val hasCustomDebugKeystore = file("${rootDir}/debug.keystore").exists()

signingConfigs {
    if (hasCustomDebugKeystore) {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
}
```
If you run inside Android Studio directly, it will build seamlessly with your machine's default system key fallback if a local `debug.keystore` file is not present in the workspace root directory.

### 4. Running the App
1. Connect an **Android Device** via USB (with Developer Options / USB Debugging enabled) or start an **Android Virtual Device (AVD)** emulator.
2. Click **Sync Project with Gradle Files** in Android Studio to confirm all dependencies are fetched.
3. Click the green **Run (Play)** button in your toolbar to build and run the app.

---

## 📊 Database Schema Details (Room)

Attendr holds high data integrity across five local tables:

| Entity / Table | Primary Key | Key Columns | Purpose |
| :--- | :--- | :--- | :--- |
| **Course** (`courses`) | `id` (Auto) | `name`, `color`, `attendanceMode`, `targetHours` | Holds master courses metadata and mode thresholds. |
| **AttendanceRecord** (`attendance_records`) | `id` (Auto) | `courseId`, `date`, `status`, `periodIndex`, `units` | Sequential log of every mark registered. |
| **PeriodDefinition** (`period_definitions`) | `id` (Auto) | `periodNumber`, `startTime`, `endTime` | Establishes the school/college period schedule bounds. |
| **TimetableSlot** (`timetable_slots`) | `id` (Auto) | `dayOfWeek`, `periodId`, `courseId`, `orderIndex` | Maps course entities to weekly repeating blocks. |
| **TemporaryScheduleOverride** | `id` (Auto) | `date`, `periodId`, `courseId`, `status` | Registers class adjustments, room edits, or cancellations. |
| **NotificationLog** (`notification_logs`) | `id` (String) | `date`, `periodId`, `courseId`, `status` | Dedupes notifications, tracks background click responses. |

---

## 🔮 Clean Offline Security
Your academic attendance is yours alone. Attendr runs **100% offline**, stores zero diagnostic or tracker metrics, does not ship your routine logs to remote clusters, and requires **no external API credentials** (nor any AI LLM components). It is written to be secure, lightweight, and incredibly fast.
