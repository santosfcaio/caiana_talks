# Quickstart: Testing User Stats

**Phase**: 1 — Design & Contracts
**Feature**: 003-user-stats

---

## Overview

The Stats screen reads from Room's `sessions` and `corrections` tables. Because real session recording is not yet implemented, you must seed test data manually to exercise the UI during development.

---

## Option 1: Unit Tests (No Emulator)

ViewModel and repository tests use MockK fakes — no database seeding needed.

```powershell
# Run all stats unit tests
.\gradlew testDebugUnitTest --tests "com.caiana.talks.ui.stats.*"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.data.repository.StatsRepositoryTest"

# Run all unit tests
.\gradlew testDebugUnitTest
```

---

## Option 2: Seed via AppDatabase.SeedCallback (Emulator / Device)

Add test sessions and corrections to `AppDatabase.SeedCallback.onCreate()`. This runs only on a **fresh** database install.

```kotlin
// AppDatabase.kt — inside SeedCallback.onCreate(), after existing profile seeds:

// Seed session 1 for profileId=1 (1 hour ago, 15 min duration)
db.execSQL("""
    INSERT INTO sessions (userProfileId, startedAt, endedAt, transcript)
    VALUES (1, ${System.currentTimeMillis() - 75 * 60_000L},
               ${System.currentTimeMillis() - 60 * 60_000L}, 'Test transcript 1')
""")
// Corrections for session 1
db.execSQL("""
    INSERT INTO corrections (sessionId, category, description, timestamp)
    VALUES (1, 'GRAMMAR', 'Used "make" instead of "do"', ${System.currentTimeMillis() - 74 * 60_000L})
""")
db.execSQL("""
    INSERT INTO corrections (sessionId, category, description, timestamp)
    VALUES (1, 'VOCABULARY', 'Said "commence" — prefer "start" in casual speech', ${System.currentTimeMillis() - 73 * 60_000L})
""")

// Seed session 2 for profileId=1 (yesterday, 8 min duration)
db.execSQL("""
    INSERT INTO sessions (userProfileId, startedAt, endedAt, transcript)
    VALUES (1, ${System.currentTimeMillis() - 24 * 3600_000L - 8 * 60_000L},
               ${System.currentTimeMillis() - 24 * 3600_000L}, 'Test transcript 2')
""")
// Session 2 has no corrections → should show "Nenhuma correção registrada."
```

**After seeding**: uninstall the app before reinstalling so `onCreate` fires on a clean database.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb uninstall com.caiana.talks
.\gradlew installDebug
& $adb shell am start -n "com.caiana.talks/.MainActivity"
```

---

## Option 3: Full Emulator Flow

```powershell
# 1. Start emulator
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
Start-Process -FilePath $emulator -ArgumentList "-avd CaianaTalks_Pixel6 -no-snapshot-load" -WindowStyle Normal

# 2. Wait for boot (~20 seconds)
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb wait-for-device shell getprop sys.boot_completed

# 3. Build and install
.\gradlew installDebug

# 4. Launch
& $adb shell am start -n "com.caiana.talks/.MainActivity"
```

Navigate: select a profile → Home → "Ver meu progresso" → Stats screen.

---

## Verifying Empty State (no seed data)

Fresh install with no seed override:

| Section | Expected |
|---------|----------|
| Nível de Inglês | "Conclua uma sessão para ver seu nível estimado." |
| Erros por Categoria | "0 erros" in each category row |
| Histórico de Sessões | "Nenhuma sessão concluída ainda." |
| Insights | "Conclua mais sessões para desbloquear insights." |

---

## Verifying CEFR Level

Use seed data matching these scenarios:

| Sessions | Total Errors | Avg Errors/Session | Expected CEFR |
|----------|--------------|--------------------|---------------|
| 0 | — | — | Placeholder (null) |
| 1 | 20 | 20 | A1 |
| 7 | 56 | 8 | B1 |
| 15 | 60 | 4 | B2 |
| 25 | 30 | 1.2 | C1 |

---

## Verifying Insights

| Scenario | Expected insight |
|----------|-----------------|
| Only 1 session | "Complete mais sessões para desbloquear insights." |
| 2 sessions, Grammar > 50% of total errors | Grammar-focused insight |
| 5 sessions, error rate decreasing | Positive progress insight |
| 2 sessions, no corrections in any | "Continue praticando! Cada sessão te aproxima da fluência." (default) |

---

## Profile Switch Test

1. Create or select a second profile (Ana).
2. Open Stats — verify all sections show Ana's data (or empty state if she has no sessions).
3. Switch back to the first profile — verify stats immediately update to their data.
4. No stale data from the previous profile should remain visible (SC-005, FR-006).
