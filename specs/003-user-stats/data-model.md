# Data Model: User Stats & Progress Insights

**Phase**: 1 — Design & Contracts
**Feature**: 003-user-stats

---

## Persisted Entities (Room)

### Existing: SessionEntity (unchanged)

```kotlin
@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = UserProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["userProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userProfileId")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userProfileId: Int,   // FK → UserProfileEntity
    val startedAt: Long,      // epoch millis
    val endedAt: Long,        // epoch millis
    val transcript: String    // raw conversation transcript
)
```

### New: CorrectionEntity

```kotlin
@Entity(
    tableName = "corrections",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class CorrectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,       // FK → SessionEntity
    val category: String,     // serialized CorrectionCategory.name
    val description: String,
    val timestamp: Long       // epoch millis within session
)
```

**Validation rules** (enforced at repository layer, not Room):
- `category` MUST equal one of: `"GRAMMAR"`, `"VOCABULARY"`, `"FLUENCY"`
- `description` MUST NOT be blank
- `sessionId` MUST reference an existing `SessionEntity.id`

---

## Domain Enums (new)

### CorrectionCategory

```kotlin
enum class CorrectionCategory(val displayLabel: String) {
    GRAMMAR("Gramática"),
    VOCABULARY("Vocabulário"),
    FLUENCY("Fluência")
}
```

### CefrLevel

```kotlin
enum class CefrLevel(val label: String, val description: String) {
    A1("A1 — Iniciante",
       "Você consegue usar expressões básicas e frases simples."),
    A2("A2 — Elementar",
       "Você se comunica em situações cotidianas familiares."),
    B1("B1 — Intermediário",
       "Você lida com a maioria das situações em viagens ao exterior."),
    B2("B2 — Intermediário Avançado",
       "Você interage com falantes nativos com fluência razoável."),
    C1("C1 — Avançado",
       "Você se expressa com fluência e espontaneidade."),
    C2("C2 — Proficiente",
       "Você compreende e se expressa com precisão em situações complexas.")
}
```

---

## Computed Domain Models (not persisted)

### ProgressSnapshot

Top-level aggregate exposed by `StatsRepository`. Computed from Room Flows; never stored in the database.

```kotlin
data class ProgressSnapshot(
    val cefrLevel: CefrLevel?,           // null when sessionCount == 0
    val grammarErrors: Int,
    val vocabularyErrors: Int,
    val fluencyErrors: Int,
    val sessions: List<SessionSummary>,  // newest first
    val insights: List<String>           // 1–3 Portuguese-language strings
)
```

### SessionSummary

Projection used only for display; derived from `SessionEntity` + `CorrectionEntity`.

```kotlin
data class SessionSummary(
    val id: Int,
    val date: Long,                          // SessionEntity.startedAt
    val durationMinutes: Int,                // (endedAt - startedAt) / 60_000
    val corrections: List<CorrectionSummary> // capped at 5 (spec US3)
)
```

**Duration display rule**: `durationMinutes < 60` → `"Xmin"` | `≥ 60` → `"Xh Ymin"` (handles the 2+ hour edge case).

### CorrectionSummary

```kotlin
data class CorrectionSummary(
    val category: CorrectionCategory,
    val description: String
)
```

---

## DAOs

### SessionDao (new)

```kotlin
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE userProfileId = :profileId ORDER BY startedAt DESC")
    fun getSessionsForProfile(profileId: Int): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT COUNT(*) FROM sessions WHERE userProfileId = :profileId")
    fun getSessionCount(profileId: Int): Flow<Int>
}
```

### CorrectionDao (new)

```kotlin
data class CategoryCount(val category: String, val count: Int)

@Dao
interface CorrectionDao {
    @Query("SELECT * FROM corrections WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getCorrectionsForSession(sessionId: Int): Flow<List<CorrectionEntity>>

    @Query("""
        SELECT c.category AS category, COUNT(*) AS count
        FROM corrections c
        INNER JOIN sessions s ON c.sessionId = s.id
        WHERE s.userProfileId = :profileId
        GROUP BY c.category
    """)
    fun getCategoryCountsForProfile(profileId: Int): Flow<List<CategoryCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: CorrectionEntity)
}
```

---

## Entity Relationships

```
UserProfileEntity (1) ──< (N) SessionEntity (1) ──< (N) CorrectionEntity
```

- `SessionEntity.userProfileId` → `UserProfileEntity.id` (CASCADE DELETE)
- `CorrectionEntity.sessionId` → `SessionEntity.id` (CASCADE DELETE)

---

## State Transitions

| Condition | ProgressSnapshot state |
|-----------|----------------------|
| No sessions | `cefrLevel = null`, all counts = 0, sessions = [], insights = ["Complete mais sessões..."] |
| 1 session, any errors | `cefrLevel = A1`, counts populated, no multi-session insights |
| 2+ sessions with corrections | Full snapshot — CEFR computed, insights generated from rules |
| Profile switch | Snapshot recomputed for new `profileId` via `StatsRepository` Flow |
| Corrupted/missing data | Sections with intact data render; corrupted sections show empty-state messages |

---

## Database Migration

**AppDatabase version**: 1 → 2

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS corrections (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sessionId INTEGER NOT NULL,
                category TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (sessionId) REFERENCES sessions(id) ON DELETE CASCADE
            )
        """)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_corrections_sessionId ON corrections(sessionId)"
        )
    }
}
```

`AppDatabase.kt` changes:
- Add `CorrectionEntity::class` to `@Database(entities = [...])`
- Bump `version = 2`
- Add `abstract fun sessionDao(): SessionDao`
- Add `abstract fun correctionDao(): CorrectionDao`
- Pass `MIGRATION_1_2` to `.addMigrations(...)`
