package com.caiana.talks.di

import android.content.Context
import com.caiana.talks.BuildConfig
import com.caiana.talks.data.conversation.AndroidSpeechRecognizerService
import com.caiana.talks.data.conversation.AndroidTextToSpeechService
import com.caiana.talks.data.conversation.SpeechRecognizerService
import com.caiana.talks.data.conversation.SystemPromptBuilder
import com.caiana.talks.data.conversation.SystemPromptBuilderImpl
import com.caiana.talks.data.conversation.TextToSpeechService
import com.caiana.talks.data.local.preferences.UserPreferencesDataStore
import com.caiana.talks.data.remote.ConversationAiClient
import com.caiana.talks.data.remote.OpenRouterConversationAiClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConversationProvideModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideConversationAiClient(
        httpClient: OkHttpClient,
        userPrefs: UserPreferencesDataStore
    ): ConversationAiClient = OpenRouterConversationAiClient(
        httpClient = httpClient,
        userPrefs = userPrefs,
        defaultApiKey = BuildConfig.OPENROUTER_API_KEY,
        defaultModel = BuildConfig.OPENROUTER_MODEL
    )

    @Provides
    @Singleton
    fun provideSpeechRecognizerService(
        @ApplicationContext context: Context
    ): SpeechRecognizerService = AndroidSpeechRecognizerService(context)

    @Provides
    @Singleton
    fun provideTextToSpeechService(
        @ApplicationContext context: Context
    ): TextToSpeechService = AndroidTextToSpeechService(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ConversationBindModule {

    @Binds
    @Singleton
    abstract fun bindSystemPromptBuilder(
        impl: SystemPromptBuilderImpl
    ): SystemPromptBuilder
}
