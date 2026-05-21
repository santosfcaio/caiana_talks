package com.caiana.talks.di

import android.content.Context
import androidx.room.Room
import com.caiana.talks.data.local.db.AppDatabase
import com.caiana.talks.data.local.db.ConversationTurnDao
import com.caiana.talks.data.local.db.CorrectionDao
import com.caiana.talks.data.local.db.SessionDao
import com.caiana.talks.data.local.db.UserProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "caiana_talks_db")
            .addCallback(AppDatabase.SeedCallback())
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideCorrectionDao(db: AppDatabase): CorrectionDao = db.correctionDao()

    @Provides
    fun provideConversationTurnDao(db: AppDatabase): ConversationTurnDao = db.conversationTurnDao()
}
