package com.caiana.talks.data.remote

import com.caiana.talks.BuildConfig
import com.caiana.talks.data.conversation.AiResponseParser
import com.caiana.talks.domain.model.AiResponseMeta
import com.caiana.talks.domain.model.AiStreamEvent
import com.caiana.talks.domain.model.ConversationError
import com.caiana.talks.domain.model.ConversationMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

class AnthropicConversationAiClient @Inject constructor(
    private val httpClient: OkHttpClient
) : ConversationAiClient {

    override fun streamReply(
        system: SystemPrompt,
        window: List<ConversationMessage>,
        userInput: String
    ): Flow<AiStreamEvent> = flow {
        val systemArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", system.staticBlock)
                put("cache_control", JSONObject().put("type", "ephemeral"))
            })
            put(JSONObject().apply {
                put("type", "text")
                put("text", system.personalizationBlock)
            })
        }

        val messagesArray = JSONArray()
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
            put("model", "claude-haiku-4-5")
            put("max_tokens", 400)
            put("stream", true)
            put("system", systemArray)
            put("messages", messagesArray)
        }.toString()

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
            .header("anthropic-version", "2023-06-01")
            .header("anthropic-beta", "prompt-caching-2024-07-31")
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

                // Emit text deltas only from within the <say> block
                if (!sayEnded) {
                    if (fullResponse.contains("</say>")) {
                        sayEnded = true
                        if (!sayEnded) emit(AiStreamEvent.SayEnded)
                        emit(AiStreamEvent.SayEnded)
                    } else {
                        // Only emit delta if it's inside <say>
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
