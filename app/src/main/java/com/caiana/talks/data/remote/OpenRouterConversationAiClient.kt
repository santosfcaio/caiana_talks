package com.caiana.talks.data.remote

import com.caiana.talks.data.conversation.AiResponseParser
import com.caiana.talks.data.local.preferences.UserPreferencesDataStore
import com.caiana.talks.domain.model.AiStreamEvent
import com.caiana.talks.domain.model.ConversationError
import com.caiana.talks.domain.model.ConversationMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class OpenRouterConversationAiClient(
    private val httpClient: OkHttpClient,
    private val userPrefs: UserPreferencesDataStore,
    private val defaultApiKey: String,
    private val defaultModel: String
) : ConversationAiClient {

    override fun streamReply(
        system: SystemPrompt,
        window: List<ConversationMessage>,
        userInput: String
    ): Flow<AiStreamEvent> = flow {
        val userKey = userPrefs.openrouterApiKey.first()
        val userModel = userPrefs.aiModel.first()
        val apiKey = userKey.takeIf { it.isNotEmpty() } ?: defaultApiKey
        val model = userModel.takeIf { it.isNotEmpty() } ?: defaultModel

        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", "${system.staticBlock}\n\n${system.personalizationBlock}")
        })
        window.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.role == ConversationMessage.Role.USER) "user" else "assistant")
                put("content", msg.text)
            })
        }
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userInput)
        })

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 400)
            put("stream", true)
            put("messages", messagesArray)
        }.toString()

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("content-type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(AiStreamEvent.Failed(ConversationError.AI_API_ERROR))
                return@flow
            }

            val source = response.body?.source() ?: run {
                emit(AiStreamEvent.Failed(ConversationError.AI_API_ERROR))
                return@flow
            }

            val fullResponse = StringBuilder()
            var sayEnded = false

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (SseEventParser.isDone(line)) break

                val data = SseEventParser.parseLine(line) ?: continue
                val delta = SseEventParser.extractDeltaText(data) ?: continue

                fullResponse.append(delta)

                if (!sayEnded) {
                    if (fullResponse.contains("</say>")) {
                        sayEnded = true
                        emit(AiStreamEvent.SayEnded)
                    } else {
                        val sayStart = fullResponse.indexOf("<say>")
                        if (sayStart >= 0) {
                            val content = fullResponse.substring(sayStart + 5)
                            if (content.isNotEmpty()) emit(AiStreamEvent.TextDelta(delta))
                        }
                    }
                }
            }

            val rawFull = fullResponse.toString()
            val meta = AiResponseParser.parseMeta(rawFull)
            emit(AiStreamEvent.Completed(meta))
        } catch (_: IOException) {
            emit(AiStreamEvent.Failed(ConversationError.NETWORK_UNAVAILABLE))
        } catch (_: Exception) {
            emit(AiStreamEvent.Failed(ConversationError.AI_API_ERROR))
        }
    }.flowOn(Dispatchers.IO)
}
