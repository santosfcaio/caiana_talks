# Research: User Stats & Progress Insights

**Phase**: 0 — Research & Unknowns Resolution
**Feature**: 003-user-stats

All NEEDS CLARIFICATION items from the Technical Context have been resolved through codebase exploration.

---

## 1. CorrectionEntity Design

**Decision**: Add `CorrectionEntity` as a new Room entity with a CASCADE foreign key to `SessionEntity`.

**Rationale**: `SessionEntity` already exists with `id`, `userProfileId`, `startedAt`, `endedAt`, `transcript`. Corrections are a 1:N child of a session. A separate table is cleaner than JSON-encoding corrections into `SessionEntity.transcript`, and enables efficient category-grouped queries.

**Alternatives considered**:
- Embed corrections as a JSON string in `SessionEntity.transcript` — rejected; requires parsing, cannot be queried by category efficiently.
- Store corrections in a separate database — rejected; single `AppDatabase` is the established pattern.

**Fields**:
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
    val sessionId: Int,
    val category: String,    // serialized CorrectionCategory name
    val description: String,
    val timestamp: Long      // epoch millis within the session
)
```

---

## 2. Room Migration v1 → v2

**Decision**: Add `MIGRATION_1_2` to `AppDatabase` that creates the `corrections` table and its index.

**Rationale**: Room requires explicit migrations for schema changes in production builds. Destructive migration is not acceptable because users may have existing `UserProfileEntity` and `SessionEntity` data.

**Migration SQL**:
```sql
CREATE TABLE IF NOT EXISTS corrections (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    sessionId INTEGER NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    FOREIGN KEY (sessionId) REFERENCES sessions(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_corrections_sessionId ON corrections (sessionId);
```

**Alternatives considered**: `fallbackToDestructiveMigration()` — rejected because it deletes existing profile data.

---

## 3. CEFR Level Heuristic

**Decision**: Estimate CEFR from two signals: total completed sessions and average errors per session (all categories combined).

**Rationale**: CEFR estimation without AI must use observable proxies. Session count reflects experience/engagement; error rate reflects current accuracy. Both are computable directly from Room with no external calls (FR-005).

**Algorithm**:
```
avgErrorsPerSession = totalErrors / max(1, sessionCount)

if   sessionCount == 0               → null (show placeholder)
elif sessionCount < 3  OR  avg > 15  → A1
elif sessionCount in [3,5]  AND  avg in (10, 15] → A2
elif sessionCount in [6,10] AND  avg in (5, 10]  → B1
elif sessionCount in [11,20] AND avg in (2, 5]   → B2
elif sessionCount > 20 AND avg in [1, 2]          → C1
elif sessionCount > 30 AND avg < 1                → C2
else                                              → B1  (safe fallback)
```

**Alternatives considered**:
- AI-based level assessment (stream transcript to model) — rejected; violates Principle II (token cost) and FR-005 (no network calls).
- NLP text analysis of `transcript` field — rejected; transcript structure is not guaranteed in v1, making analysis unreliable.

---

## 4. Insight Generation Rules

**Decision**: Apply a deterministic, priority-ordered rule set; return up to 3 insights as Portuguese-language strings.

**Rationale**: Natural-language insights require no AI when driven by threshold-based rules on correction counts. Deterministic rules are auditable, cost-free per Principle II, and directly testable.

**Rules** (evaluated in order; max 3 returned):
1. `sessionCount < 2` → `"Complete mais sessões para desbloquear insights."` *(early exit)*
2. Grammar errors > 50% of total → `"Você comete mais erros de gramática. Foque nos tempos verbais e estrutura das frases."`
3. Vocabulary errors > 50% of total → `"Expandir seu vocabulário é sua maior área de crescimento. Leia e ouça em inglês diariamente."`
4. Fluency errors > 50% of total → `"Seu principal desafio é a fluência. Tente falar de forma mais contínua, sem pausas longas."`
5. Same top-error category in last 3 sessions → `"Você comete erros frequentes de [categoria] nas últimas sessões — considere prática dedicada nessa área."`
6. Session count ≥ 5 AND error rate of last 3 sessions < error rate of first 3 sessions → `"Ótimo progresso! Sua taxa de erros está diminuindo. Continue assim!"`
7. Default → `"Continue praticando! Cada sessão te aproxima da fluência."`

**Alternatives considered**:
- LLM-generated insights per session (batch call) — rejected; violates Principle II and FR-005.
- Single hardcoded generic message — rejected; too generic to satisfy FR-004 (actionable, pattern-specific).

---

## 5. Navigation Entry Point

**Decision**: Add a "Ver meu progresso" `Button` (or card) to `HomeScreen`, navigating to the `"stats"` route. One tap from Home satisfies FR-008 (≤2 taps).

**Rationale**: A button on HomeScreen is the minimal viable change. The current HomeScreen is intentionally minimal (`HomeScreen.kt` shows only a greeting and settings icon), so there is room for a stats entry without redesign.

**Alternative considered**: Bottom navigation bar (Home + Stats tabs) — better long-term UX, but requires structural changes to NavHost and Scaffold that are out of scope for this feature. Can be introduced in a future redesign when more top-level destinations exist.

---

## 6. SessionDao & CorrectionDao Interfaces

**Decision**: Create `SessionDao` and `CorrectionDao` as separate DAO interfaces, both registered in `AppDatabase`.

**Rationale**: Follows the existing `UserProfileDao` pattern. Separation keeps each DAO focused on its own entity and simplifies testing with fakes.

**Key queries**:

`SessionDao`:
- `getSessionsForProfile(profileId: Int): Flow<List<SessionEntity>>` — all sessions for active profile, newest first
- `insertSession(session: SessionEntity): Long`
- `getSessionCount(profileId: Int): Flow<Int>`

`CorrectionDao`:
- `getCorrectionsForSession(sessionId: Int): Flow<List<CorrectionEntity>>` — corrections per session, chronological
- `getCategoryCountsForProfile(profileId: Int): Flow<List<CategoryCount>>` — grouped aggregate
- `insertCorrection(correction: CorrectionEntity)`

```kotlin
data class CategoryCount(val category: String, val count: Int)
```

---

## 7. ProgressSnapshot Computation

**Decision**: `StatsRepository` computes `ProgressSnapshot` reactively using `combine()` on `SessionDao` and `CorrectionDao` Flows, so stats automatically update when new sessions or corrections are written.

**Rationale**: No caching or polling layer needed at v1 scale. `combine()` keeps the snapshot consistent without manual invalidation.

```kotlin
interface StatsRepository {
    fun getProgressSnapshot(profileId: Int): Flow<ProgressSnapshot>
}
```

The snapshot is computed in-memory (not persisted):
```kotlin
data class ProgressSnapshot(
    val cefrLevel: CefrLevel?,           // null when sessionCount == 0
    val grammarErrors: Int,
    val vocabularyErrors: Int,
    val fluencyErrors: Int,
    val sessions: List<SessionSummary>,  // newest first
    val insights: List<String>           // 1–3 Portuguese-language strings
)

data class SessionSummary(
    val id: Int,
    val date: Long,
    val durationMinutes: Int,
    val corrections: List<CorrectionSummary>  // capped at 5 per spec US3
)

data class CorrectionSummary(
    val category: CorrectionCategory,
    val description: String
)
```

---

## 8. Confirmed Tech Stack (Unknowns Resolved)

| Item | Value | Source |
|------|-------|--------|
| minSdk | 26 | `app/build.gradle.kts` |
| compileSdk | 34 | `app/build.gradle.kts` |
| Test runner | JUnit4 + MockK + Turbine + coroutines-test | `gradle/libs.versions.toml` |
| Existing sessions entity | `SessionEntity` (id, userProfileId, startedAt, endedAt, transcript) | `data/local/db/SessionEntity.kt` |
| Session DAO | MISSING — must be created | codebase exploration |
| Correction entity | MISSING — must be created | codebase exploration |
| Stats entry point | MISSING from HomeScreen — must be added | `ui/home/HomeScreen.kt` |
| Nav routes | profileSelection, profileEdit, home, settings — stats missing | `ui/navigation/AppNavGraph.kt` |
