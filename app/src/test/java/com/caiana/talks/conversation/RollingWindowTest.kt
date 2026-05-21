package com.caiana.talks.conversation

import com.caiana.talks.data.conversation.RollingWindow
import com.caiana.talks.domain.model.ConversationMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RollingWindowTest {

    private fun msg(role: ConversationMessage.Role, text: String) =
        ConversationMessage(role, text)

    @Test
    fun `take with empty list returns empty`() {
        val result = RollingWindow.take(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `take with fewer than max turns returns all`() {
        val turns = listOf(
            msg(ConversationMessage.Role.USER, "turn1"),
            msg(ConversationMessage.Role.ASSISTANT, "reply1")
        )
        val result = RollingWindow.take(turns)
        assertEquals(turns, result)
    }

    @Test
    fun `take with exactly max turns returns all`() {
        val turns = (1..RollingWindow.MAX_TURNS).map {
            msg(ConversationMessage.Role.USER, "turn$it")
        }
        val result = RollingWindow.take(turns)
        assertEquals(turns, result)
    }

    @Test
    fun `take with more than max turns evicts oldest`() {
        val turns = (1..10).map { msg(ConversationMessage.Role.USER, "turn$it") }
        val result = RollingWindow.take(turns)
        assertEquals(RollingWindow.MAX_TURNS, result.size)
        assertEquals("turn5", result.first().text)
        assertEquals("turn10", result.last().text)
    }

    @Test
    fun `take preserves oldest-first order`() {
        val turns = listOf(
            msg(ConversationMessage.Role.USER, "first"),
            msg(ConversationMessage.Role.ASSISTANT, "second"),
            msg(ConversationMessage.Role.USER, "third"),
        )
        val result = RollingWindow.take(turns)
        assertEquals("first", result[0].text)
        assertEquals("second", result[1].text)
        assertEquals("third", result[2].text)
    }

    @Test
    fun `MAX_TURNS is 6`() {
        assertEquals(6, RollingWindow.MAX_TURNS)
    }

    @Test
    fun `take with exactly 7 turns drops first turn`() {
        val turns = (1..7).map { msg(ConversationMessage.Role.USER, "turn$it") }
        val result = RollingWindow.take(turns)
        assertEquals("turn2", result.first().text)
        assertEquals("turn7", result.last().text)
    }
}
