package com.caiana.talks.conversation

import com.caiana.talks.data.conversation.AiResponseParser
import com.caiana.talks.domain.model.CorrectionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseParserTest {

    @Test
    fun `extractSayText extracts content between say tags`() {
        val raw = "<say>Hello, how are you?</say><meta>{\"corrections\":[],\"vocab\":[],\"pt\":false}</meta>"
        assertEquals("Hello, how are you?", AiResponseParser.extractSayText(raw))
    }

    @Test
    fun `extractSayText returns empty string when no say tag`() {
        val raw = "No tags here"
        assertEquals("", AiResponseParser.extractSayText(raw))
    }

    @Test
    fun `extractSayText handles unclosed say tag by returning buffered content`() {
        val raw = "<say>Partial content"
        assertEquals("Partial content", AiResponseParser.extractSayText(raw))
    }

    @Test
    fun `parseMeta parses well-formed meta with corrections`() {
        val raw = """<say>Good!</say><meta>{"corrections":[{"cat":"GRAMMAR","note":"use past tense"}],"vocab":["tense"],"pt":false}</meta>"""
        val meta = AiResponseParser.parseMeta(raw)
        assertEquals(1, meta.corrections.size)
        assertEquals(CorrectionCategory.GRAMMAR, meta.corrections[0].category)
        assertEquals("use past tense", meta.corrections[0].note)
        assertEquals(listOf("tense"), meta.vocabulary)
        assertFalse(meta.userSpokePortuguese)
    }

    @Test
    fun `parseMeta returns empty meta when no meta tag`() {
        val raw = "<say>Hello</say>"
        val meta = AiResponseParser.parseMeta(raw)
        assertTrue(meta.corrections.isEmpty())
        assertTrue(meta.vocabulary.isEmpty())
        assertFalse(meta.userSpokePortuguese)
    }

    @Test
    fun `parseMeta returns empty meta when meta JSON is malformed`() {
        val raw = "<say>Hello</say><meta>{broken json}</meta>"
        val meta = AiResponseParser.parseMeta(raw)
        assertTrue(meta.corrections.isEmpty())
        assertTrue(meta.vocabulary.isEmpty())
        assertFalse(meta.userSpokePortuguese)
    }

    @Test
    fun `parseMeta drops unknown correction category`() {
        val raw = """<say>Hi</say><meta>{"corrections":[{"cat":"UNKNOWN_CAT","note":"note"}],"vocab":[],"pt":false}</meta>"""
        val meta = AiResponseParser.parseMeta(raw)
        assertTrue(meta.corrections.isEmpty())
    }

    @Test
    fun `parseMeta handles empty corrections and vocab arrays`() {
        val raw = """<say>Good</say><meta>{"corrections":[],"vocab":[],"pt":false}</meta>"""
        val meta = AiResponseParser.parseMeta(raw)
        assertTrue(meta.corrections.isEmpty())
        assertTrue(meta.vocabulary.isEmpty())
    }

    @Test
    fun `parseMeta sets userSpokePortuguese true when pt is true`() {
        val raw = """<say>Hi</say><meta>{"corrections":[],"vocab":[],"pt":true}</meta>"""
        val meta = AiResponseParser.parseMeta(raw)
        assertTrue(meta.userSpokePortuguese)
    }

    @Test
    fun `parseMeta handles VOCABULARY category`() {
        val raw = """<say>Hi</say><meta>{"corrections":[{"cat":"VOCABULARY","note":"wrong word"}],"vocab":["proper"],"pt":false}</meta>"""
        val meta = AiResponseParser.parseMeta(raw)
        assertEquals(CorrectionCategory.VOCABULARY, meta.corrections[0].category)
    }

    @Test
    fun `parseMeta handles FLUENCY category`() {
        val raw = """<say>Hi</say><meta>{"corrections":[{"cat":"FLUENCY","note":"pause"}],"vocab":[],"pt":false}</meta>"""
        val meta = AiResponseParser.parseMeta(raw)
        assertEquals(CorrectionCategory.FLUENCY, meta.corrections[0].category)
    }

    @Test
    fun `parseMeta handles multiple vocab items`() {
        val raw = """<say>Great</say><meta>{"corrections":[],"vocab":["apple","banana","cherry"],"pt":false}</meta>"""
        val meta = AiResponseParser.parseMeta(raw)
        assertEquals(listOf("apple", "banana", "cherry"), meta.vocabulary)
    }
}
