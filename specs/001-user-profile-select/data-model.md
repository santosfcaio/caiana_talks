# Data Model: MVP User Profile Selection

**Date**: 2026-05-18 | **Feature**: [spec.md](./spec.md)

---

## Entities

### UserProfileEntity (Room — `user_profiles` table)

Represents one of the two fixed app users. Seeded at database creation.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `Int` | PRIMARY KEY | Fixed: 1 = Caio, 2 = Ana |
| `name` | `String` | NOT NULL, UNIQUE | Display name shown in UI |
| `learning_goals` | `String` | NOT NULL, default `""` | Comma-separated goal tags (e.g., "travel,business") |
| `preferred_themes` | `String` | NOT NULL, default `""` | Comma-separated conversation theme tags |
| `ai_voice_gender` | `String` | NOT NULL, default `"feminine"` | `"feminine"` or `"masculine"` |
| `ai_voice_accent` | `String` | NOT NULL, default `"american"` | `"american"` or `"british"` |
| `ai_speech_rate` | `String` | NOT NULL, default `"normal"` | `"slow"`, `"normal"`, or `"fast"` |

**Seed data** (inserted via `RoomDatabase.Callback.onCreate()`):

```
id=1, name="Caio", learning_goals="", preferred_themes="", ai_voice_gender="feminine", ai_voice_accent="american", ai_speech_rate="normal"
id=2, name="Ana",  learning_goals="", preferred_themes="", ai_voice_gender="feminine", ai_voice_accent="american", ai_speech_rate="normal"
```

**Relationships**:
- One-to-many with `SessionEntity` (future feature): a UserProfile owns many Sessions.

---

### SessionEntity (Room — `sessions` table) *(referenced, not implemented in this feature)*

Placeholder entity established in this feature's database setup so the schema is ready for the sessions feature.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `Int` | PRIMARY KEY, AUTOINCREMENT | |
| `user_profile_id` | `Int` | NOT NULL, FOREIGN KEY → `user_profiles.id` | Which user this session belongs to |
| `started_at` | `Long` | NOT NULL | Unix epoch milliseconds |
| `ended_at` | `Long` | NOT NULL | Unix epoch milliseconds |
| `transcript` | `String` | NOT NULL, default `""` | Raw session transcript |

---

## DataStore Preference

### UserPreferencesDataStore (Jetpack DataStore Preferences)

Stores a single key indicating which user profile is currently active.

| Key | Type | Default | Notes |
|-----|------|---------|-------|
| `active_user_id` | `Int` | `null` (absent) | ID of the active `UserProfileEntity`. Absent means no user selected yet (triggers profile selection screen). |

**State transitions**:

```
[No key present] ──(user taps name)──▶ [active_user_id = 1 or 2]
[active_user_id = 1] ──(switch from Settings)──▶ [active_user_id = 2]  (or vice versa)
[active_user_id = X] ──(storage corruption / read error)──▶ [treat as No key present]
```

---

## Validation Rules

- `UserProfileEntity.name` MUST be exactly `"Caio"` or `"Ana"` — no other values are valid in this version.
- `active_user_id` MUST reference an existing `UserProfileEntity.id` (1 or 2). Any other value is treated as absent.
- `ai_voice_gender` MUST be one of `"feminine"` or `"masculine"`.
- `ai_voice_accent` MUST be one of `"american"` or `"british"`.
- `ai_speech_rate` MUST be one of `"slow"`, `"normal"`, or `"fast"`.
