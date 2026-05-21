package com.caiana.talks.data.conversation

class SentenceChunker {

    private val buffer = StringBuilder()

    // Abbreviations that contain a dot but are NOT sentence terminators
    private val abbreviations = setOf("mr", "mrs", "ms", "dr", "prof", "e.g", "i.e", "etc", "vs")

    fun accept(delta: String): List<String> {
        buffer.append(delta)
        return drain()
    }

    fun flush(): List<String> {
        val remaining = buffer.toString().trim()
        buffer.clear()
        return if (remaining.isNotEmpty()) listOf(remaining) else emptyList()
    }

    private fun drain(): List<String> {
        val sentences = mutableListOf<String>()
        var searchFrom = 0
        val text = buffer.toString()

        var i = searchFrom
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\n' -> {
                    val sentence = text.substring(searchFrom, i).trim()
                    if (sentence.isNotEmpty()) sentences.add(sentence)
                    searchFrom = i + 1
                }
                c in ".!?" -> {
                    if (c == '.' && isFalseSplit(text, i)) {
                        // skip — abbreviation or decimal
                    } else {
                        val sentence = text.substring(searchFrom, i + 1).trim()
                        if (sentence.isNotEmpty()) sentences.add(sentence)
                        searchFrom = i + 1
                    }
                }
            }
            i++
        }

        buffer.clear()
        buffer.append(text.substring(searchFrom))
        return sentences
    }

    private fun isFalseSplit(text: String, dotIndex: Int): Boolean {
        // Check if it's a decimal number: digit.digit
        if (dotIndex > 0 && dotIndex < text.length - 1) {
            if (text[dotIndex - 1].isDigit() && text[dotIndex + 1].isDigit()) return true
        }
        // Check abbreviation: word before the dot is a known abbreviation
        val wordBefore = wordBefore(text, dotIndex).lowercase()
        if (abbreviations.contains(wordBefore)) return true
        // Check single-letter abbreviations like "e.g." or "i.e."
        if (wordBefore.length == 1 && wordBefore[0].isLetter()) {
            // Backward: char two positions back is a dot (second dot in "e.g.")
            if (dotIndex >= 2 && text[dotIndex - 2] == '.') return true
            // Forward: followed by single-letter then dot (first dot in "e.g.")
            if (dotIndex + 2 < text.length && text[dotIndex + 1].isLetter() && text[dotIndex + 2] == '.') return true
        }
        return false
    }

    private fun wordBefore(text: String, index: Int): String {
        var end = index - 1
        while (end >= 0 && text[end] == ' ') end--
        var start = end
        while (start > 0 && text[start - 1].isLetter()) start--
        return if (start > end) "" else text.substring(start, end + 1)
    }
}
