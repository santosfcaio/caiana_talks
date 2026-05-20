# Data Model: Voice Conversation

**Phase**: 1 — Design & Contracts
**Feature**: 004-voice-conversation

---

## Overview

This feature extends the existing Room schema (currently **version 2**, with
`user_profiles`, `sessions`, `corrections`) to **version 3**:

- `sessions` table — **extended** with conversation columns.
- `conversation_turns` table — **new**, stores the full text transcript per turn.
- `corrections` table — **unchanged**; conversation corrections are inserted into
  it exactly as feature 003 expects.

Designing it this way means feature 003's `StatsRepository` keeps computing
`ProgressSnapshot` with **zero changes** — a finished conversation is just a
`SessionEntity` row plus `CorrectionEntity` rows, which is what 003 already reads.

---

## Persisted Entities (Room)

### Extended: SessionEntity

```kotlin
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "user_profile_id", index = true) val userProfileId: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    val transcript: String = "",                              // existing; unused by 004 (turns table is authoritative)
    @ColumnInfo(name = "status") val status: String = "completed",      // NEW: active | completed | partial
    @ColumnInfo(name = "mode") val mode: String = "single",             // NEW: single | dual
    @ColumnInfo(name = "vocabulary") val vocabulary: String = "",       // NEW: newline-separated vocabulary introduced (this profile)
    @ColumnInfo(name = "co_practice_group_id") val coPracticeGroupId: String? = null  // NEW: links the two rows of one dual session
)
```

**New column semantics**:
- `status` — serialized `SessionStatus.name` lowercased. `active` while the
  session runs; finalized to `completed` (deliberate end) or `partial`
  (interruption). Sessions under 60 s are deleted, never finalized.
- `mode` — serialized `SessionMode.name` lowercased.
- `vocabulary` — newline-separated list of vocabulary items the AI introduced,
  for *this* profile. Shown on the summary screen.
- `co_practice_group_id` — `null` for single sessions. For a dual session, both
  participants' rows share one UUID string so the summary screen can pair them.

**Validation rules** (enforced at repository layer):
- `status` ∈ {`active`, `completed`, `partial`}; `mode` ∈ {`single`, `dual`}.
- `endedAt ≥ startedAt`.
- A `dual` row MUST have a non-null `coPracticeGroupId`; a `single` row MUST have
  `null`.
- A finalized (`completed`/`partial`) row MUST have
  `endedAt − startedAt ≥ 60_000` ms.

### New: ConversationTurnEntity

```kotlin
@Entity(
    tableName = "conversation_turns",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id")]
)
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "session_id") val sessionId: Int,        // FK → SessionEntity
    @ColumnInfo(name = "speaker_profile_id") val speakerProfileId: Int,
    @ColumnInfo(name = "turn_index") val turnIndex: Int,        // 0-based order within the session
    @ColumnInfo(name = "user_text") val userText: String,      // full STT transcription of the user's speech
    @ColumnInfo(name = "ai_text") val aiText: String = "",     // full AI response text (the <say> content)
    val timestamp: Long                                         // epoch millis when the turn completed
)
```

**Validation rules** (repository layer):
- `userText` MUST NOT be blank (a turn exists only once the user has spoken).
- `aiText` may be blank only for a turn captured by an interruption before the AI
  replied.
- `turnIndex` is contiguous and 0-based within a session.
- No audio is ever stored — text only (spec clarification).

### Unchanged: CorrectionEntity

`corrections` is reused as-is (`id`, `sessionId`, `category`, `description`,
`timestamp`). Each AI-detected correction is inserted with `sessionId` pointing at
the **speaker's** session row, so feature 003's category counts attribute it
correctly per profile. `category` is a serialized `CorrectionCategory.name`
(`GRAMMAR` / `VOCABULARY` / `FLUENCY`).

---

## Domain Models (runtime, not persisted)

### SessionMode / SessionStatus

```kotlin
enum class SessionMode { SINGLE, DUAL }

enum class SessionStatus { ACTIVE, COMPLETED, PARTIAL }
```

### AiPersona

The AI tutor's identity. Determined entirely by the profile's voice config — the
`(VoiceGender × VoiceAccent)` pair maps to exactly one named persona. Pure mapping
→ unit-tested for all four combinations.

```kotlin
enum class AiPersona(
    val displayName: String,
    val gender: VoiceGender,
    val accent: VoiceAccent
) {
    MICHAEL("Michael", VoiceGender.MASCULINE, VoiceAccent.AMERICAN),
    DAVID("David",     VoiceGender.MASCULINE, VoiceAccent.BRITISH),
    MARY("Mary",       VoiceGender.FEMININE,  VoiceAccent.AMERICAN),
    PHOEBE("Phoebe",   VoiceGender.FEMININE,  VoiceAccent.BRITISH);

    companion object {
        /** Resolves the persona for a voice config; total over all 4 combinations. */
        fun of(gender: VoiceGender, accent: VoiceAccent): AiPersona =
            entries.first { it.gender == gender && it.accent == accent }
    }
}
```

| Gender | Accent | Persona |
|--------|--------|---------|
| Masculine | American | **Michael** |
| Masculine | British | **David** |
| Feminine | American | **Mary** |
| Feminine | British | **Phoebe** |

The persona name is injected into the system prompt's personalization block
(research R7) so the AI introduces and refers to itself by that name, and it is
surfaced on the conversation UI (contracts/conversation-ui.md).

### ConversationConfig

Built once per session from `ProfilePreferences` (+ optional CEFR hint); drives the
system prompt (research R7) and TTS voice (research R4).

```kotlin
data class ConversationConfig(
    val mode: SessionMode,
    val participants: List<ParticipantInfo>,   // size 1 (single) or 2 (dual)
    val learningGoal: LearningGoal?,
    val themes: Set<ConversationTheme>,
    val voice: VoicePreference,                 // gender + accent + rate
    val persona: AiPersona,                     // AiPersona.of(voice.gender, voice.accent)
    val cefrHint: CefrLevel?                    // from feature 003 StatsRepository; null if no history
)

data class ParticipantInfo(
    val profileId: Int,
    val name: String
)
```

### ConversationMessage

One entry of the rolling window sent to the AI (research R8).

```kotlin
data class ConversationMessage(
    val role: Role,        // USER | ASSISTANT
    val text: String
) {
    enum class Role { USER, ASSISTANT }
}
```

### AiStreamEvent

Emitted by `ConversationAiClient` as the SSE stream is consumed.

```kotlin
sealed interface AiStreamEvent {
    data class TextDelta(val text: String) : AiStreamEvent   // a fragment of <say> content
    data object SayEnded : AiStreamEvent                     // </say> reached — stop feeding TTS
    data class Completed(val meta: AiResponseMeta) : AiStreamEvent
    data class Failed(val error: ConversationError) : AiStreamEvent
}
```

### AiResponseMeta

Parsed from the `<meta>` JSON tail by `AiResponseParser` (research R6).

```kotlin
data class AiResponseMeta(
    val corrections: List<DetectedCorrection>,
    val vocabulary: List<String>,
    val userSpokePortuguese: Boolean            // drives FR-017
)

data class DetectedCorrection(
    val category: CorrectionCategory,
    val note: String
)
```

### ConversationError

```kotlin
enum class ConversationError {
    MIC_PERMISSION_DENIED,
    MIC_UNAVAILABLE,
    NETWORK_UNAVAILABLE,
    AI_API_ERROR,
    STORAGE_FULL
}
```

### SessionResult

Returned by `ConversationRepository` when a session is finalized; feeds the
summary screen.

```kotlin
data class SessionResult(
    val outcome: Outcome,                       // SAVED | DISCARDED_TOO_SHORT
    val summaries: List<SessionSummary>         // one per participant; empty when discarded
) {
    enum class Outcome { SAVED, DISCARDED_TOO_SHORT }
}
```

`SessionSummary` reuses feature 003's `domain.model.SessionSummary` (id, date,
durationMinutes, corrections) and is extended for this feature with a vocabulary
list — see contracts/conversation-ui.md.

---

## DAOs

### Extended: SessionDao

Existing functions (`getSessionsForProfile`, `insertSession`, `getSessionCount`)
are unchanged. New functions for the conversation lifecycle:

```kotlin
@Update
suspend fun updateSession(session: SessionEntity)

@Query("SELECT * FROM sessions WHERE id = :id")
suspend fun getSessionById(id: Int): SessionEntity?

@Query("SELECT * FROM sessions WHERE status = 'active'")
suspend fun getActiveSessions(): List<SessionEntity>    // process-death recovery

@Query("SELECT * FROM sessions WHERE co_practice_group_id = :groupId")
suspend fun getSessionsByGroup(groupId: String): List<SessionEntity>

@Query("DELETE FROM sessions WHERE id = :id")
suspend fun deleteSession(id: Int)                       // < 60 s discard
```

### New: ConversationTurnDao

```kotlin
@Dao
interface ConversationTurnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: ConversationTurnEntity): Long

    @Query("SELECT * FROM conversation_turns WHERE session_id = :sessionId ORDER BY turn_index ASC")
    suspend fun getTurnsForSession(sessionId: Int): List<ConversationTurnEntity>

    @Query("SELECT * FROM conversation_turns WHERE session_id = :sessionId ORDER BY turn_index ASC")
    fun observeTurnsForSession(sessionId: Int): Flow<List<ConversationTurnEntity>>
}
```

### Unchanged: CorrectionDao

`CorrectionDao` (`getCorrectionsForSession`, `getCategoryCountsForProfile`,
`insertCorrection`) is reused as-is.

---

## Entity Relationships

```
UserProfileEntity (1) ──< (N) SessionEntity (1) ──< (N) ConversationTurnEntity
                                      │
                                      └──< (N) CorrectionEntity
```

- `SessionEntity.userProfileId` → `UserProfileEntity.id` (CASCADE DELETE)
- `ConversationTurnEntity.sessionId` → `SessionEntity.id` (CASCADE DELETE)
- `CorrectionEntity.sessionId` → `SessionEntity.id` (CASCADE DELETE)

**Single-speaker session**: one `SessionEntity` row; all turns and corrections
attach to it.

**Dual-speaker session**: two `SessionEntity` rows (one per participant), sharing
one `coPracticeGroupId`. A turn/correction is written to the **speaking
participant's** row. Both rows span the full session window, so feature 003 counts
each participant's practice time and corrections independently and correctly
(FR-012).

---

## Session State Transitions

```
        start                speak                  AI replies
 IDLE ─────────► LISTENING ──────────► THINKING ──────────────► SPEAKING
                    ▲                                              │
                    └──────────────────────────────────────────────┘
                                  (next turn)

 Any state ──► END (user taps "End Session")  ──► finalize
 Any state ──► INTERRUPTED (app backgrounded / process death) ──► finalize-partial
```

| Trigger | Duration | Persisted result |
|---------|----------|------------------|
| "End Session" tapped | ≥ 60 s | `status = completed`; stats updated; summary shown |
| "End Session" tapped | < 60 s | session + turns **deleted**; no stats; Home |
| App backgrounded / crash | ≥ 60 s | `status = partial`; stats updated; summary shown on return |
| App backgrounded / crash | < 60 s | dangling `active` row deleted on next launch; no stats |
| Recovered `active` row on launch | — | finalized using last turn's timestamp as `endedAt`, then the rules above apply |

Turns are persisted incrementally as each completes, so an interruption always
leaves a consistent partial record.

---

## Database Migration

**AppDatabase version**: 2 → 3

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sessions ADD COLUMN status TEXT NOT NULL DEFAULT 'completed'")
        database.execSQL("ALTER TABLE sessions ADD COLUMN mode TEXT NOT NULL DEFAULT 'single'")
        database.execSQL("ALTER TABLE sessions ADD COLUMN vocabulary TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE sessions ADD COLUMN co_practice_group_id TEXT")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS conversation_turns (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                session_id INTEGER NOT NULL,
                speaker_profile_id INTEGER NOT NULL,
                turn_index INTEGER NOT NULL,
                user_text TEXT NOT NULL,
                ai_text TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_conversation_turns_session_id ON conversation_turns(session_id)"
        )
    }
}
```

`AppDatabase.kt` changes:
- Add `ConversationTurnEntity::class` to `@Database(entities = [...])`.
- Bump `version = 3`.
- Add `abstract fun conversationTurnDao(): ConversationTurnDao`.
- Pass `MIGRATION_2_3` alongside `MIGRATION_1_2` in `.addMigrations(...)`.

Pre-existing `sessions` rows from feature 003 migrate cleanly: the `DEFAULT`
clauses make every old session a `completed`, `single`-mode session with empty
vocabulary and no co-practice group — exactly what they were.
