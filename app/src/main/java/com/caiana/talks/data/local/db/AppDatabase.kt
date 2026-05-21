package com.caiana.talks.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        SessionEntity::class,
        CorrectionEntity::class,
        ConversationTurnEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun conversationTurnDao(): ConversationTurnDao

    class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.execSQL(
                """
                INSERT INTO user_profiles (id, name, learning_goals, preferred_themes, ai_voice_gender, ai_voice_accent, ai_speech_rate)
                VALUES
                  (1, 'Caio', '', '', 'feminine', 'american', 'normal'),
                  (2, 'Ana',  '', '', 'feminine', 'american', 'normal')
                """.trimIndent()
            )
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS corrections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY (sessionId) REFERENCES sessions(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_corrections_sessionId ON corrections(sessionId)"
                )
            }
        }

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
    }
}
