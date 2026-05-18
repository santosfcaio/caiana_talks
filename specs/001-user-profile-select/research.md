# Research: MVP User Profile Selection

**Date**: 2026-05-18 | **Feature**: [spec.md](./spec.md)

---

## Decision 1: Active User Persistence — DataStore vs SharedPreferences vs Room

**Decision**: Jetpack DataStore Preferences API

**Rationale**: The active-user selection is a single key-value pair (which of the two users is currently active). DataStore is the modern, officially recommended replacement for SharedPreferences. It is non-blocking (coroutine-native), safe to read on the main thread via Flow collection, and integrates cleanly with ViewModel + StateFlow. SharedPreferences is the legacy approach and blocks on disk I/O. Room is appropriate for structured relational data (full UserProfile records, sessions) but is heavyweight for a single preference flag.

**Alternatives considered**:
- SharedPreferences: Discouraged — synchronous, not coroutine-native, no type safety.
- Room only: Querying Room at startup to determine navigation destination adds unnecessary DB overhead for what is effectively a config flag.

---

## Decision 2: Full UserProfile Storage — Room Database

**Decision**: Room with a `user_profiles` table seeded with exactly two fixed records (Caio, Ana)

**Rationale**: Constitution Principle VI requires each user to have a persistent profile containing learning goals, preferred themes, AI voice preferences (gender, accent, speech rate), and historical session data. Room provides typed entities, DAO queries, LiveData/Flow observation, and safe migration support — all needed as the profile schema grows across future features. Seeding two fixed records at database creation avoids any "create user" flow while still giving each user a real mutable record.

**Alternatives considered**:
- Hardcoded Kotlin data objects: Cannot be updated at runtime (e.g., user changes voice preference), no persistence, no relation to sessions.
- JSON file in internal storage: No query support, manual serialization, no migration path.

---

## Decision 3: Navigation Startup Logic — Conditional NavGraph Start Destination

**Decision**: Read the active-user preference from DataStore in a `SplashViewModel` (or `MainViewModel`) before the first navigation frame. If null, navigate to `ProfileSelectionScreen`; otherwise navigate to `HomeScreen`.

**Rationale**: Jetpack Navigation Compose allows the start destination to be determined dynamically. Reading DataStore as a suspend call in a ViewModel `init` block (collecting a Flow) before rendering the NavHost means there is no flash of the wrong screen. The Activity observes the ViewModel state and renders the NavHost only after the initial destination is resolved, preventing a visible redirect.

**Alternatives considered**:
- Hardcoding `ProfileSelectionScreen` as the start destination and always checking inside it: Causes a visible screen render before redirect for returning users — poor UX.
- Reading DataStore synchronously in `Application.onCreate()`: Blocks the main thread; discouraged by Android documentation.

---

## Decision 4: Dependency Injection — Hilt

**Decision**: Hilt for injecting Repository into ViewModels

**Rationale**: Hilt is the recommended DI solution for Android Jetpack. It reduces boilerplate for ViewModel injection and provides scoped component lifecycles aligned with Android's Activity/Fragment/ViewModel lifecycle. For this feature, Hilt injects `UserRepository` into `ProfileSelectionViewModel` and the navigation-resolving ViewModel.

**Alternatives considered**:
- Manual DI (constructor injection without a framework): Acceptable for a small app but creates boilerplate that compounds across features; inconsistent with Jetpack best practices.
- Koin: Valid alternative but Hilt has tighter Jetpack/Compose integration and official Google support.

---

## Decision 5: Fixed User Seeding Strategy

**Decision**: Seed two `UserProfileEntity` rows (`id=1, name="Caio"` and `id=2, name="Ana"`) via `RoomDatabase.Callback.onCreate()`

**Rationale**: Seeding at database creation ensures both profiles exist before any query or selection occurs, without requiring a "create user" onboarding flow. Using fixed IDs (1 and 2) simplifies the DataStore preference — it only needs to store the ID (or name string) of the active user, not a dynamically generated UUID.

**Alternatives considered**:
- Seeding via a migration: Fragile — only runs on schema version changes, not on fresh installs.
- Creating profiles lazily on first selection: Adds write latency to the selection tap; no reason to defer since the two profiles are known at build time.
