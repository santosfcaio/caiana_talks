package com.caiana.talks.domain.model

enum class AiPersona(
    val displayName: String,
    val gender: VoiceGender,
    val accent: VoiceAccent
) {
    MICHAEL("Michael", VoiceGender.MASCULINE, VoiceAccent.AMERICAN),
    DAVID("David", VoiceGender.MASCULINE, VoiceAccent.BRITISH),
    MARY("Mary", VoiceGender.FEMININE, VoiceAccent.AMERICAN),
    PHOEBE("Phoebe", VoiceGender.FEMININE, VoiceAccent.BRITISH);

    companion object {
        fun of(gender: VoiceGender, accent: VoiceAccent): AiPersona =
            entries.first { it.gender == gender && it.accent == accent }
    }
}
