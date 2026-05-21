package com.caiana.talks.conversation

import com.caiana.talks.data.remote.SseEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseEventParserTest {

    @Test
    fun `parseLine extracts data from data line`() {
        val result = SseEventParser.parseLine("data: {\"type\":\"content_block_delta\"}")
        assertEquals("{\"type\":\"content_block_delta\"}", result)
    }

    @Test
    fun `parseLine returns null for blank line`() {
        assertNull(SseEventParser.parseLine(""))
        assertNull(SseEventParser.parseLine("   "))
    }

    @Test
    fun `parseLine returns null for event lines`() {
        assertNull(SseEventParser.parseLine("event: message_start"))
    }

    @Test
    fun `parseLine returns null for id lines`() {
        assertNull(SseEventParser.parseLine("id: 12345"))
    }

    @Test
    fun `parseLine returns null for comment lines`() {
        assertNull(SseEventParser.parseLine(": keep-alive"))
    }

    @Test
    fun `isDone returns true for DONE sentinel`() {
        assertTrue(SseEventParser.isDone("data: [DONE]"))
    }

    @Test
    fun `isDone returns false for regular data line`() {
        val line = "data: {\"type\":\"content_block_delta\"}"
        assertTrue(!SseEventParser.isDone(line))
    }

    @Test
    fun `extractDeltaText extracts text delta from Anthropic SSE JSON`() {
        val json = """{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}"""
        val result = SseEventParser.extractDeltaText(json)
        assertEquals("Hello", result)
    }

    @Test
    fun `extractDeltaText returns null for non-text-delta event`() {
        val json = """{"type":"message_start","message":{"id":"msg_1"}}"""
        val result = SseEventParser.extractDeltaText(json)
        assertNull(result)
    }

    @Test
    fun `extractDeltaText returns null for malformed JSON`() {
        val result = SseEventParser.extractDeltaText("not json")
        assertNull(result)
    }
}
