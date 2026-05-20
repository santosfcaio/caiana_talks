package com.caiana.talks.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProfileEntity::class, SessionEntity::class, CorrectionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun sessionDao(): SessionDao
    abstract fun correctionDao(): CorrectionDao

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
    }
}
