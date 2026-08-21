# MoodLens — Build Tasks (Native Android, Kotlin)

Companion to `moodlens-system-design-android.md`. Tasks are ordered by priority — **validate the detection pipeline completely before building any journal/summary/streak features.** Do not proceed to Phase 2 until Phase 1's exit criteria are met.

---

## Phase 1: Detection Pipeline Validation (build this first, in isolation)

Goal: prove camera → face detection → emotion classification → live label on screen works end-to-end, with no journal, no storage, no notifications, minimal UI.

- [ ] **1.1 Project scaffold**
  - Add dependencies: CameraX, ML Kit Face Detection, TFLite, Coroutines
  - Confirm project builds and runs a blank Compose screen on a physical device

- [ ] **1.2 Camera preview**
  - Implement `CameraViewModel` + CameraX `Preview` use case
  - Request `CAMERA` runtime permission with rationale UI
  - Confirm live camera preview renders in a Compose `AndroidView`/`PreviewView`

- [ ] **1.3 Face detection (ML Kit)**
  - Add `ImageAnalysis` use case with `FrameAnalyzer`
  - Wire ML Kit `FaceDetector` to run on each analyzed frame
  - Draw a bounding box overlay on detected face(s) — no emotion yet, just prove detection works
  - Test: bounding box tracks your face in real time, disappears when no face present

- [ ] **1.4 Source the emotion model**
  - use ferplus_model_pd_best.tflite file
  - Inspect input tensor shape (expected: grayscale, 48x48) and output shape (7 classes)
  - Confirm label order matches: angry, disgust, fear, happy, sad, surprise, neutral
  - Place file at `app/src/main/assets/emotion_model.tflite`

- [ ] **1.5 Preprocessing**
  - Implement `Preprocessing.kt`: crop `Bitmap` to face bounding box, convert to grayscale (if model expects it), resize to model's input dims, normalize pixel values (0–1 or -1–1, match model's training preprocessing)
  - Unit test preprocessing output shape/values against a known input, if feasible

- [ ] **1.6 TFLite inference**
  - Implement `EmotionClassifier.kt`: load model from assets, run `Interpreter.run()` on preprocessed tensor
  - Return top label + confidence score
  - Log raw output scores for all 7 classes to Logcat for sanity-checking (not just top-1)

- [ ] **1.7 Wire it together**
  - `FrameAnalyzer` → face crop → preprocess → classify → emit result via `StateFlow`
  - Display live label + confidence as text overlay on camera preview (no fancy UI yet)

- [ ] **1.8 Validate accuracy & performance**
  - Test on physical device with varied expressions (smile, frown, surprised face, neutral)
  - Confirm reasonable classification (won't be perfect — FER2013 models cap ~65-75% accuracy; disgust/fear will be weakest)
  - Measure end-to-end latency per frame (target <150ms) — log timestamps around detection+inference
  - Test under different lighting conditions and face angles
  - Tune frame throttling (analyzer backpressure strategy, frame skip rate) until preview stays smooth and device doesn't overheat

**Exit criteria for Phase 1**: Live camera feed correctly draws a bounding box and displays a plausible emotion label + confidence in real time, on a physical device, at acceptable latency and battery draw, for at least 5 minutes of continuous use without crashing or overheating.

---

## Phase 2: Local Persistence (only after Phase 1 passes)

- [ ] **2.1** Set up Room database, `SessionEntry` entity + DAO
- [ ] **2.2** Implement `ThumbnailStore` (save cropped face as downscaled JPEG to internal storage)
- [ ] **2.3** Add "save" action to camera screen — writes entry + thumbnail on tap
- [ ] **2.4** `SessionRepository` wrapping Room + file storage

## Phase 3: Mood Journal

- [ ] **3.1** `JournalScreen` — Compose `LazyVerticalGrid` of saved entries (thumbnail + emotion + time)
- [ ] **3.2** Entry detail view (tap to expand)
- [ ] **3.3** Delete entry support

## Phase 4: Daily Summary

- [ ] **4.1** `MoodSummaryService` — aggregate today's entries, compute dominant emotion
- [ ] **4.2** Dip detection logic (cluster of negative emotions + time window)
- [ ] **4.3** `DailySummaryScreen` UI card

## Phase 5: Streaks & Check-In Nudges

- [ ] **5.1** DataStore-backed `StreakData` + `StreakRepository`
- [ ] **5.2** `StreakService` increment/reset logic on check-in
- [ ] **5.3** `StreakBadge` UI component
- [ ] **5.4** `POST_NOTIFICATIONS` permission request (Android 13+)
- [ ] **5.5** `WorkManager` daily job (`CheckInReminderWorker`) — fires only if no check-in yet that day

## Phase 6: Polish

- [ ] **6.1** Home screen tying journal/summary/streak together
- [ ] **6.2** App icon, theming, empty states
- [ ] **6.3** Storage cleanup for old thumbnails (retention policy)
- [ ] **6.4** Final battery/performance pass

---

## Notes for Kiro

- Do not build Phase 2+ UI or storage code until Phase 1's exit criteria are explicitly confirmed working on a physical device.
- If Phase 1.8 reveals the model is too inaccurate or too slow, stop and flag it — do not silently proceed to Phase 2 with a broken detection core.
- No backend, no networking library, at any phase.
