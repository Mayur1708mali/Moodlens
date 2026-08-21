# MoodLens 🎭✨

> **Your Real-Time On-Device AI Emotional Companion & Journal for Android**

MoodLens is a native Android application that helps you track and understand your emotions through real-time facial expression analysis. Built completely with **Jetpack Compose**, **Google ML Kit**, **TensorFlow Lite**, and **Room Database**, MoodLens runs 100% on your device — **no images or video streams ever leave your phone.**

---

## 🌟 Key Features

### ⚡ 1. Real-Time On-Device Emotion Detection
* **CameraX + ML Kit**: Detects faces with low latency (<150ms) and draws a live tracking bounding box.
* **TFLite Neural Network**: Quantized CNN model trained on FER+ classifying 7 distinct expressions:
  * 😊 **Happy**
  * 😮 **Surprise**
  * 😐 **Neutral**
  * 😢 **Sad**
  * 😠 **Angry**
  * 😨 **Fear**
  * 🤢 **Disgust**
* **Instant Feedback**: Displays real-time emotion label and confidence percentage.

### 💾 2. One-Tap Mood Check-Ins
* Tap **"Save Check-In"** directly from the live camera feed.
* Automatically saves the emotion, timestamp, confidence score, and a cropped face thumbnail to internal storage.

### 📖 3. Mood Journal
* **Interactive Grid View**: View your emotional check-in history in a beautiful Material 3 grid.
* **Color-Coded Badges**: Every mood is styled with its own distinct color theme.
* **Full Detail Modal**: Tap any entry to view full-resolution face thumbnails, confidence progress bars, timestamps, notes, and delete actions.

### 📊 4. Daily Summary & Mood Dip Detection
* **Dominant Mood Analysis**: Calculates your most frequent emotion for each day.
* **Emotion Breakdown**: Clean progress meters showing the distribution of all recorded feelings.
* **🚨 Smart Dip Detection**: Automatically spots clusters of negative emotions within short time windows (e.g., afternoon stress) and offers helpful, empathetic wellness tips.
* **Scrollable Timeline**: View all daily check-ins ordered with the latest entries on top.

### 🔥 5. Daily Streaks & Intelligent Nudges
* **Streaks Engine**: Automatically increments daily streaks when checking in on consecutive days and tracks your personal best.
* **WorkManager Daily Reminder**: A background job at 8:00 PM that only nudges you if you haven't recorded a mood check-in yet that day.

### 🧹 6. Storage Retention & Privacy First
* **30-Day Auto Cleanup**: A weekly background worker cleans expired thumbnail images to keep your device storage light.
* **100% Offline & Private**: Zero cloud dependency. No analytics, tracking, or external image uploads.

---

## 📱 Screenshots & Flow

```
+-------------------------------------------------------------------------------+
|                                  MoodLens                                     |
+---------------------+-------------------+------------------+------------------+
|   🏠 Home Tab       |   📷 Detect Tab   |   📖 Journal Tab |  📊 Summary Tab  |
| - Daily Streak      | - Live Camera     | - Grid History   | - Dominant Mood  |
| - Quick Scan Button | - Face Box & AI   | - Detail View    | - Mood Dip Alert |
| - Today Snapshot    | - "Save Check-In" | - Delete Support | - Timeline List  |
+---------------------+-------------------+------------------+------------------+
```

---

## 🛠️ Tech Stack & Architecture

* **Language**: [Kotlin](https://kotlinlang.org/) (100%)
* **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
* **Architecture**: MVVM (Model-View-ViewModel) with Kotlin Coroutines & `StateFlow`
* **Camera**: [Android CameraX](https://developer.android.com/training/camerax)
* **Face Detection**: [Google ML Kit Face Detection](https://developers.google.com/ml-kit/vision/face-detection)
* **Emotion Model**: [TensorFlow Lite](https://www.tensorflow.org/lite) (`ferplus_model_pd_best.tflite`)
* **Local Database**: [Room Database](https://developer.android.com/training/data-storage/room)
* **Key-Value Storage**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)
* **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

---

## 🚀 Getting Started (Beginner Friendly)

Follow these simple steps to get MoodLens running on your machine:

### 1. Prerequisites
* **Android Studio**: Install [Android Studio Ladybug (2024.2+) or newer](https://developer.android.com/studio).
* **JDK**: Java Development Kit (JDK 17 or JDK 21).
* **Device**: A physical Android device with Android 7.0+ (API 24+) recommended for real camera testing (or an emulator with camera enabled).

### 2. Clone the Repository
Open your terminal and clone the repository:
```bash
git clone https://github.com/Mayur1708mali/Moodlens.git
cd Moodlens
```

### 3. Open in Android Studio
1. Launch **Android Studio**.
2. Click **Open** and select the `Moodlens` folder.
3. Allow Android Studio to sync Gradle dependencies (this usually takes 1–2 minutes on first run).

### 4. Run the App
1. Connect your Android phone via USB (with **USB Debugging** enabled in Developer Options) or start an Android Emulator.
2. Select your device from the device dropdown in Android Studio.
3. Click the green **Run (▶)** button (or press `Shift + F10`).
4. When the app launches, tap **"Allow"** when prompted for **Camera** and **Notification** permissions.

---

## 📂 Project Structure

```
app/src/main/
├── assets/
│   ├── emotion_model.tflite        # Quantized FER+ Emotion Classification Model
│   └── moodlens_logo.png           # App Icon Brand Asset
│
└── java/com/example/moodlens/
    ├── MainActivity.kt             # Main entry point with bottom navigation tabs
    ├── HomeScreen.kt               # Dashboard connecting streak, summary & journal
    ├── CameraPreviewScreen.kt      # CameraX preview, face box, and save check-in action
    ├── JournalScreen.kt            # Compose grid of mood entries and details
    ├── DailySummaryScreen.kt       # Daily dominant mood, dip alerts, and timeline
    │
    ├── CameraViewModel.kt          # Camera binding, frame analysis, and save state
    ├── JournalViewModel.kt         # Journal entries and deletion coordinator
    ├── SummaryViewModel.kt         # Reactive daily summary & date navigation
    │
    ├── FrameAnalyzer.kt            # ML Kit Face detection & TFLite inference pipeline
    ├── Preprocessing.kt            # Grayscale conversion, 48x48 resize & normalization
    ├── EmotionClassifier.kt        # TFLite interpreter runner & softmax confidence
    ├── MoodSummaryService.kt       # Dominant mood calculation & negative dip clustering
    ├── StreakService.kt            # Consecutive check-in streak logic
    │
    ├── NotificationHelper.kt       # Android notification channel & reminder dispatch
    ├── CheckInReminderWorker.kt    # WorkManager daily reminder job (8:00 PM)
    ├── StorageCleanupWorker.kt     # Weekly 30-day thumbnail retention cleaner
    │
    ├── data/
    │   ├── SessionEntry.kt         # Room database entity
    │   ├── SessionDao.kt           # Room Data Access Object (CRUD queries)
    │   ├── AppDatabase.kt          # Room database singleton
    │   ├── ThumbnailStore.kt       # Internal storage JPEG face file manager
    │   ├── SessionRepository.kt    # Repository combining Room & ThumbnailStore
    │   ├── DailySummary.kt         # Data model for daily stats & mood dips
    │   ├── StreakData.kt           # Data model for active streaks
    │   └── StreakRepository.kt     # DataStore preferences repository
    │
    └── theme/
        ├── Color.kt                # Modern Material 3 color tokens
        └── Theme.kt                # Dark, light, and dynamic color theme
```

---

## 🧪 Running Unit Tests & Building APK

To run all unit tests from the command line:
```bash
./gradlew test
```

To build a fresh Debug APK:
```bash
./gradlew assembleDebug
```
*The compiled APK will be located at:* `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Privacy Guarantee

* **100% Local Execution**: Inference runs via TensorFlow Lite directly on the phone's CPU/NPU.
* **No Cloud Storage**: Images and logs are stored exclusively in internal app storage (`context.filesDir`).
* **Zero Telemetry**: No third-party tracking or ad SDKs.

---

## 🤝 Contributing

Contributions are always welcome!
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'feat: add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.
