package com.caiana.talks.conversation

import app.cash.turbine.test
import com.caiana.talks.data.local.preferences.UserPreferencesDataStore
import com.caiana.talks.data.remote.OpenRouterConversationAiClient
import com.caiana.talks.data.remote.SystemPrompt
import com.caiana.talks.domain.model.AiStreamEvent
import com.caiana.talks.domain.model.ConversationError
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OpenRouterConversationAiClientTest {

    private lateinit var httpClient: OkHttpClient
    private lateinit var userPrefs: UserPreferencesDataStore
    private lateinit var client: OpenRouterConversationAiClient

    private val defaultApiKey = "test-default-key"
    private val defaultModel = "test-default-model"
    private val system = SystemPrompt(staticBlock = "static", personalizationBlock = "personal")

    @Before
    fun setUp() {
        httpClient = mockk()
        userPrefs = mockk()
        client = OpenRouterConversationAiClient(
            httpClient = httpClient,
            userPrefs = userPrefs,
            defaultApiKey = defaultApiKey,
            defaultModel = defaultModel
        )
        every { userPrefs.openrouterApiKey } returns flowOf("")
        every { userPrefs.aiModel } returns flowOf("")
    }

    private fun setupStream(
        lines: List<String>,
        requestSlot: io.mockk.CapturingSlot<Request> = slot()
    ): io.mockk.CapturingSlot<Request> {
        val call = mockk<Call>()
        val response = mockk<Response>(relaxed = true)
        val body = mockk<ResponseBody>(relaxed = true)
        val source = mockk<BufferedSource>(relaxed = true)

        val exhaustedValues = lines.map { false }.toMutableList<Boolean>()
        exhaustedValues += true
        every { source.exhausted() } returnsMany exhaustedValues
        every { source.readUtf8Line() } returnsMany lines
        every { body.source() } returns source
        every { response.isSuccessful } returns true
        every { response.body } returns body
        every { call.execute() } returns response
        every { httpClient.newCall(capture(requestSlot)) } returns call
        return requestSlot
    }

    @Test
    fun `streams TextDelta SayEnded and Completed from say block`() = runTest {
        // delta from line1 = "<say>Hello" — the full raw SSE content is emitted as-is
        val line1 = """data: {"choices":[{"index":0,"delta":{"content":"<say>Hello"},"finish_reason":null}]}"""
        val line2 = """data: {"choices":[{"index":0,"delta":{"content":" world<\/say><meta>{\"corrections\":[],\"vocab\":[],\"pt\":false}<\/meta>"},"finish_reason":null}]}"""
        setupStream(listOf(line1, line2, "data: [DONE]"))

        client.streamReply(system, emptyList(), "Hi").test {
            // TextDelta carries the full raw delta string (including <say> tag)
            assertEquals(AiStreamEvent.TextDelta("<say>Hello"), awaitItem())
            assertEquals(AiStreamEvent.SayEnded, awaitItem())
            val completed = awaitItem()
            assertTrue(completed is AiStreamEvent.Completed)
            assertTrue((completed as AiStreamEvent.Completed).meta.corrections.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `non-2xx response emits Failed with AI_API_ERROR`() = runTest {
        val call = mockk<Call>()
        val response = mockk<Response>(relaxed = true)
        every { response.isSuccessful } returns false
        every { call.execute() } returns response
        every { httpClient.newCall(any()) } returns call

        client.streamReply(system, emptyList(), "Hi").test {
            assertEquals(AiStreamEvent.Failed(ConversationError.AI_API_ERROR), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `IOException emits Failed with NETWORK_UNAVAILABLE`() = runTest {
        val call = mockk<Call>()
        every { call.execute() } throws IOException("timeout")
        every { httpClient.newCall(any()) } returns call

        client.streamReply(system, emptyList(), "Hi").test {
            assertEquals(AiStreamEvent.Failed(ConversationError.NETWORK_UNAVAILABLE), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `user key override takes precedence over default key`() = runTest {
        every { userPrefs.openrouterApiKey } returns flowOf("user-key")
        every { userPrefs.aiModel } returns flowOf("")
        val requestSlot = setupStream(listOf("data: [DONE]"))

        client.streamReply(system, emptyList(), "Hi").test {
            awaitItem() // consume Completed(meta) emitted after empty stream
            awaitComplete()
        }

        assertEquals("Bearer user-key", requestSlot.captured.header("Authorization"))
    }

    @Test
    fun `empty user key falls back to default key`() = runTest {
        val requestSlot = setupStream(listOf("data: [DONE]"))

        client.streamReply(system, emptyList(), "Hi").test {
            awaitItem()
            awaitComplete()
        }

        assertEquals("Bearer $defaultApiKey", requestSlot.captured.header("Authorization"))
    }

    @Test
    fun `user model override takes precedence over default model`() = runTest {
        every { userPrefs.openrouterApiKey } returns flowOf("")
        every { userPrefs.aiModel } returns flowOf("user-model")
        val requestSlot = setupStream(listOf("data: [DONE]"))

        client.streamReply(system, emptyList(), "Hi").test {
            awaitItem()
            awaitComplete()
        }

        val bodyBuffer = Buffer()
        requestSlot.captured.body?.writeTo(bodyBuffer)
        val bodyJson = JSONObject(bodyBuffer.readUtf8())
        assertEquals("user-model", bodyJson.getString("model"))
    }

    @Test
    fun `empty user model falls back to default model`() = runTest {
        val requestSlot = setupStream(listOf("data: [DONE]"))

        client.streamReply(system, emptyList(), "Hi").test {
            awaitItem()
            awaitComplete()
        }

        val bodyBuffer = Buffer()
        requestSlot.captured.body?.writeTo(bodyBuffer)
        val bodyJson = JSONObject(bodyBuffer.readUtf8())
        assertEquals(defaultModel, bodyJson.getString("model"))
    }

    @Test
    fun `both keys blank sends request with empty bearer and surfaces AI_API_ERROR`() = runTest {
        val clientWithEmptyDefaults = OpenRouterConversationAiClient(
            httpClient = httpClient,
            userPrefs = userPrefs,
            defaultApiKey = "",
            defaultModel = ""
        )
        val call = mockk<Call>()
        val response = mockk<Response>(relaxed = true)
        every { response.isSuccessful } returns false
        every { call.execute() } returns response
        every { httpClient.newCall(any()) } returns call

        clientWithEmptyDefaults.streamReply(system, emptyList(), "Hi").test {
            assertEquals(AiStreamEvent.Failed(ConversationError.AI_API_ERROR), awaitItem())
            awaitComplete()
        }
    }
}
