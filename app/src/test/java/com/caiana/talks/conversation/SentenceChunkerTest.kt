package com.caiana.talks.conversation

import com.caiana.talks.data.conversation.SentenceChunker
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SentenceChunkerTest {

    private lateinit var chunker: SentenceChunker

    @Before
    fun setUp() {
        chunker = SentenceChunker()
    }

    @Test
    fun `accept splits on period`() {
        val result = chunker.accept("Hello. World.")
        assertEquals(listOf("Hello.", "World."), result)
    }

    @Test
    fun `accept splits on exclamation mark`() {
        val result = chunker.accept("Great! Really great!")
        assertEquals(listOf("Great!", "Really great!"), result)
    }

    @Test
    fun `accept splits on question mark`() {
        val result = chunker.accept("How are you? Fine?")
        assertEquals(listOf("How are you?", "Fine?"), result)
    }

    @Test
    fun `accept splits on newline`() {
        val result = chunker.accept("Line one\nLine two")
        assertEquals(listOf("Line one"), result)
        val remainder = chunker.flush()
        assertEquals(listOf("Line two"), remainder)
    }

    @Test
    fun `accept does not split on Mr dot`() {
        val result = chunker.accept("Mr. Smith went to Washington.")
        assertEquals(listOf("Mr. Smith went to Washington."), result)
    }

    @Test
    fun `accept does not split on Mrs dot`() {
        val result = chunker.accept("Mrs. Jones called back.")
        assertEquals(listOf("Mrs. Jones called back."), result)
    }

    @Test
    fun `accept does not split on e dot g dot`() {
        val result = chunker.accept("Use words e.g. apple or banana.")
        assertEquals(listOf("Use words e.g. apple or banana."), result)
    }

    @Test
    fun `accept does not split on i dot e dot`() {
        val result = chunker.accept("That is i.e. the answer.")
        assertEquals(listOf("That is i.e. the answer."), result)
    }

    @Test
    fun `accept does not split on decimal numbers`() {
        val result = chunker.accept("The value is 3.5 meters.")
        assertEquals(listOf("The value is 3.5 meters."), result)
    }

    @Test
    fun `accept handles multi-sentence delta`() {
        val result = chunker.accept("First sentence. Second sentence. Third sentence.")
        assertEquals(listOf("First sentence.", "Second sentence.", "Third sentence."), result)
    }

    @Test
    fun `accept accumulates partial sentence across multiple calls`() {
        val r1 = chunker.accept("Hello, this is a ")
        assertEquals(emptyList<String>(), r1)
        val r2 = chunker.accept("complete sentence.")
        assertEquals(listOf("Hello, this is a complete sentence."), r2)
    }

    @Test
    fun `flush returns remainder`() {
        chunker.accept("No terminator here")
        val result = chunker.flush()
        assertEquals(listOf("No terminator here"), result)
    }

    @Test
    fun `flush returns empty when buffer empty`() {
        val result = chunker.flush()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `accept with empty string returns empty`() {
        val result = chunker.accept("")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `sentence split across two deltas`() {
        chunker.accept("The quick brown fox ")
        val result = chunker.accept("jumps over the lazy dog.")
        assertEquals(listOf("The quick brown fox jumps over the lazy dog."), result)
    }

    @Test
    fun `accept emits sentence and keeps remainder`() {
        val result = chunker.accept("Done. Still going")
        assertEquals(listOf("Done."), result)
        val flushed = chunker.flush()
        assertEquals(listOf("Still going"), flushed)
    }
}
