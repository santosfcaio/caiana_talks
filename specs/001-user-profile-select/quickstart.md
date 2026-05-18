# Quickstart: MVP User Profile Selection

**Date**: 2026-05-18 | **Feature**: [spec.md](./spec.md)

---

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK with API level matching `minSdk` in `app/build.gradle`
- A physical Android device or an AVD (Android Virtual Device)

## Running the Feature

1. Open the project root in Android Studio.
2. Sync Gradle (`File → Sync Project with Gradle Files`).
3. Select a device/emulator in the toolbar.
4. Run the `app` configuration (▶).

On first launch, the Profile Selection screen appears showing two buttons: **"Caio"** and **"Ana"**.

## Testing the Feature Manually

### First-launch flow
1. Install a fresh build (clear app data if the app was previously installed: `adb shell pm clear com.caiana.talks`).
2. Open the app.
3. Verify the Profile Selection screen is shown.
4. Tap **"Caio"** (or **"Ana"**).
5. Verify the main screen loads and shows the selected user's name.

### Returning-user flow
1. After completing the first-launch flow, close the app.
2. Reopen the app.
3. Verify the Profile Selection screen is **not** shown — the app goes directly to the main screen.
4. Verify the correct user is still active.

### Profile switch flow
1. Open the app (with an active user).
2. Navigate to **Configurações** (Settings).
3. Tap **"Trocar perfil"** (Switch Profile).
4. Verify the Profile Selection screen is shown again.
5. Tap the other user's name.
6. Close and reopen the app — verify the new user is retained.

### Storage corruption fallback
1. Using `adb shell`, clear only DataStore data:
   ```
   adb shell run-as com.caiana.talks rm -f /data/data/com.caiana.talks/files/datastore/user_preferences.preferences_pb
   ```
2. Reopen the app.
3. Verify the Profile Selection screen is shown as if it were a first launch.

## Key Dependencies (Gradle)

Add to `app/build.gradle` (versions aligned with project BOM):

```kotlin
// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Hilt
implementation("com.google.dagger:hilt-android:2.51")
ksp("com.google.dagger:hilt-compiler:2.51")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.7.7")
```

## Running Unit Tests

```bash
./gradlew :app:test
```

Tests cover:
- `UserRepositoryTest`: verifies profile seeding, active-user read/write, switch behaviour, and corrupt-state fallback.
- `ProfileSelectionViewModelTest`: verifies UI state transitions (loading → selection shown / selection skipped) based on DataStore state.
