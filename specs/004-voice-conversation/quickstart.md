# Quickstart: Voice Conversation

**Feature**: 004-voice-conversation

How to build, configure, test, and manually verify this feature.

---

## 1. One-time setup — Anthropic API key

The key is read from `local.properties` (untracked) and exposed as
`BuildConfig.ANTHROPIC_API_KEY` (research R2).

Add this line to `local.properties` at the repo root (create the line if absent):

```properties
ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

`app/build.gradle.kts` reads it and emits the `BuildConfig` field; it also enables
`buildFeatures { buildConfig = true }`. Never commit a real key.

---

## 2. Run the unit tests

This feature is test-heavy (see the plan's Unit Testing Strategy). Per project
convention, tests MUST be written and passing before the feature is considered
done.

```powershell
# all unit tests
.\gradlew testDebugUnitTest

# just this feature's pure-logic suites (fast)
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.*"

# a single class
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.SentenceChunkerTest"
```

---

## 3. Manual verification on the emulator

Build and launch (from `CLAUDE.md`):

```powershell
# 1. start the emulator
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
Start-Process -FilePath $emulator -ArgumentList "-avd CaianaTalks_Pixel6 -no-snapshot-load" -WindowStyle Normal

# 2. wait for boot
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb wait-for-device shell getprop sys.boot_completed

# 3. build + install
.\gradlew installDebug

# 4. launch
& $adb shell am start -n "com.caiana.talks/.MainActivity"
```

**Enable a microphone for the emulator**: in the emulator's Extended Controls
(`...`) → Microphone, enable "Virtual microphone uses host audio input" so STT
can hear the host mic. The host machine needs a working microphone.

---

## 4. Acceptance walkthrough

### US1 — Single-speaker session (P1)
1. On Home, tap **"Iniciar conversa"**. Grant the microphone permission when asked.
2. The conversation screen appears in `LISTENING`, showing the AI tutor's persona
   name (Michael / David / Mary / Phoebe, per the profile's voice config); speak a
   sentence in English.
3. Confirm the partial transcript appears, then the AI replies **in voice** using
   the profile's gender/accent/rate, with the first sentence starting within ~3 s.
   Confirm the AI refers to itself by its persona name, and that the animated
   audio-spectrum bars pulse while the tutor speaks and settle to a flat resting
   row once it stops.
4. Deliberately make a grammar error; confirm the AI corrects it inline and
   encouragingly without derailing the conversation.
5. Tap **"Encerrar sessão"** after at least 60 s. Confirm the summary screen shows
   duration, correction count, and vocabulary highlights.
6. Open **"Ver meu progresso"**; confirm session count and minutes increased.

### US2 — Corrections & vocabulary (P2)
1. During a session, make several grammar/vocabulary mistakes.
2. Confirm corrections are inline, encouraging, and aligned with the profile's
   learning goal/themes.

### US3 — Summary & stats (P2)
1. Complete two sessions; confirm the summary appears after each.
2. Confirm the stats screen aggregates both sessions correctly.

### US4 — Co-practice dual-speaker (P3)
1. On Home, tap **"Co-practice"**; select two distinct profiles; confirm "Iniciar"
   is disabled until two *different* profiles are chosen.
2. Start the session; confirm the screen shows whose turn it is and alternates.
3. Have each speaker make a distinct error on their turn; confirm corrections are
   attributed to the right profile.
4. End the session; confirm each profile's stats updated independently.

### Edge cases to spot-check
- Deny the microphone permission → friendly Portuguese message, no crash, return
  to Home.
- Enable airplane mode mid-session → network error state, no crash; re-enable and
  retry.
- End a session before 60 s → no summary, straight back to Home, stats unchanged.
- Background the app mid-session (Home button), reopen → session saved as partial,
  summary shown, stats updated.
- Speak a sentence in Portuguese → a gentle nudge to continue in English; the
  conversation does not break.

---

## 5. Database migration check

The schema goes from version 2 → 3 (`MIGRATION_2_3`). To verify the migration
against real v2 data:
1. Install a build from `main` (schema v2) and complete a feature-003 session.
2. Install this feature's build over it (no uninstall).
3. Confirm the app launches without an `IllegalStateException`, old sessions still
   appear in stats, and a new conversation session can be started.

An automated Room migration test (`MigrationTest`, `androidTest`) covers the same
path in CI.
