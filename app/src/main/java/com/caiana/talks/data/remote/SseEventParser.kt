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
            val choices = obj.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val delta = choices.getJSONObject(0).optJSONObject("delta") ?: return null
            delta.optString("content").takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
