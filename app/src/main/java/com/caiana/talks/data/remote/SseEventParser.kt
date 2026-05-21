package com.caiana.talks.data.remote

import org.json.JSONObject

object SseEventParser {

    fun parseLine(line: String): String? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("data:")) return null
        val data = trimmed.removePrefix("data:").trim()
        return if (data == "[DONE]") null else data
    }

    fun isDone(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed == "data: [DONE]" || trimmed == "data:[DONE]"
    }

    fun extractDeltaText(json: String): String? {
        return try {
            val obj = JSONObject(json)
            if (obj.optString("type") != "content_block_delta") return null
            val delta = obj.optJSONObject("delta") ?: return null
            if (delta.optString("type") != "text_delta") return null
            delta.optString("text").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
