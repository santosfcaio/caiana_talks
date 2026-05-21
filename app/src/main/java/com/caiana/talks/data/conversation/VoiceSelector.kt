package com.caiana.talks.data.conversation

import com.caiana.talks.domain.model.SpeechRate
import com.caiana.talks.domain.model.VoiceAccent
import com.caiana.talks.domain.model.VoiceGender
import java.util.Locale

data class VoiceDescriptor(
    val name: String,
    val locale: Locale,
    val isFeminine: Boolean
)

object VoiceSelector {

    fun localeFor(accent: VoiceAccent): Locale = when (accent) {
        VoiceAccent.AMERICAN -> Locale.US
        VoiceAccent.BRITISH -> Locale.UK
    }

    fun rateFor(rate: SpeechRate): Float = when (rate) {
        SpeechRate.SLOW -> 0.8f
        SpeechRate.NORMAL -> 1.0f
        SpeechRate.FAST -> 1.4f
    }

    fun pickVoice(
        candidates: List<VoiceDescriptor>,
        gender: VoiceGender,
        locale: Locale
    ): VoiceDescriptor? {
        val localeMatches = candidates.filter { it.locale.language == locale.language &&
            it.locale.country == locale.country }
        if (localeMatches.isEmpty()) return null
        val wantFeminine = gender == VoiceGender.FEMININE
        return localeMatches.firstOrNull { it.isFeminine == wantFeminine }
            ?: localeMatches.first() // locale-default fallback
    }
}
