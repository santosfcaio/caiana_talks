package com.caiana.talks.data.conversation

import com.caiana.talks.domain.model.AiResponseMeta
import com.caiana.talks.domain.model.CorrectionCategory
import com.caiana.talks.domain.model.DetectedCorrection
import org.json.JSONObject

object AiResponseParser {

    private val sayPattern = Regex("<say>(.*?)</say>", RegexOption.DOT_MATCHES_ALL)
    private val sayOpenPattern = Regex("<say>(.*)", RegexOption.DOT_MATCHES_ALL)
    private val metaPattern = Regex("<meta>(.*?)</meta>", RegexOption.DOT_MATCHES_ALL)

    fun extractSayText(raw: String): String {
        sayPattern.find(raw)?.groupValues?.getOrNull(1)?.let { return it }
        // Unclosed <say> tag — return buffered content
        sayOpenPattern.find(raw)?.groupValues?.getOrNull(1)?.let { return it }
        return ""
    }

    fun parseMeta(raw: String): AiResponseMeta {
        val metaJson = metaPattern.find(raw)?.groupValues?.getOrNull(1) ?: return emptyMeta()
        return try {
            val obj = JSONObject(metaJson)
            val corrections = mutableListOf<DetectedCorrection>()
            val corrArray = obj.optJSONArray("corrections")
            if (corrArray != null) {
                for (i in 0 until corrArray.length()) {
                    val c = corrArray.getJSONObject(i)
                    val catName = c.optString("cat", "")
                    val category = CorrectionCategory.entries.firstOrNull { it.name == catName }
                        ?: continue // drop unknown categories
                    val note = c.optString("note", "")
                    corrections.add(DetectedCorrection(category, note))
                }
            }
            val vocabArray = obj.optJSONArray("vocab")
            val vocabulary = mutableListOf<String>()
            if (vocabArray != null) {
                for (i in 0 until vocabArray.length()) {
                    vocabulary.add(vocabArray.getString(i))
                }
            }
            val pt = obj.optBoolean("pt", false)
            AiResponseMeta(corrections, vocabulary, pt)
        } catch (_: Exception) {
            emptyMeta()
        }
    }

    private fun emptyMeta() = AiResponseMeta(emptyList(), emptyList(), false)
}
