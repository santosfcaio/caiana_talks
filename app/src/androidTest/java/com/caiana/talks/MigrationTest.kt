package com.caiana.talks

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.caiana.talks.data.local.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateV2ToV3_existingSessionsUnchanged() {
        helper.createDatabase(testDbName, 2).apply {
            execSQL(
                """INSERT INTO sessions (user_profile_id, started_at, ended_at, transcript)
                   VALUES (1, 1000, 2000, 'test transcript')"""
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName, 3, true, AppDatabase.MIGRATION_2_3
        )

        val cursor = db.query("SELECT * FROM sessions WHERE transcript = 'test transcript'")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("completed", cursor.getString(cursor.getColumnIndexOrThrow("status")))
        assertEquals("single", cursor.getString(cursor.getColumnIndexOrThrow("mode")))
        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("vocabulary")))
        cursor.close()
        db.close()
    }

    @Test
    fun migrateV2ToV3_conversationTurnsTableExists() {
        helper.createDatabase(testDbName + "_turns", 2).apply { close() }

        val db = helper.runMigrationsAndValidate(
            testDbName + "_turns", 3, true, AppDatabase.MIGRATION_2_3
        )

        // Verify we can insert into conversation_turns
        db.execSQL(
            """INSERT INTO sessions (user_profile_id, started_at, ended_at, transcript)
               VALUES (1, 1000, 2000, '')"""
        )
        val sessionCursor = db.query("SELECT id FROM sessions LIMIT 1")
        sessionCursor.moveToFirst()
        val sessionId = sessionCursor.getInt(0)
        sessionCursor.close()

        db.execSQL(
            """INSERT INTO conversation_turns (session_id, speaker_profile_id, turn_index, user_text, ai_text, timestamp)
               VALUES ($sessionId, 1, 0, 'hello', 'hi there', 12345)"""
        )
        val turnCursor = db.query("SELECT COUNT(*) FROM conversation_turns")
        turnCursor.moveToFirst()
        assertEquals(1, turnCursor.getInt(0))
        turnCursor.close()
        db.close()
    }

    @Test
    fun fullMigration_appDatabaseOpensSuccessfully() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, AppDatabase::class.java, "full-migration-test")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
        db.openHelper.writableDatabase
        db.close()
    }
}
