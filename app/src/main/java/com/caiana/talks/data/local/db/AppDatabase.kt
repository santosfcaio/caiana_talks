package com.caiana.talks.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProfileEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao

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
}
