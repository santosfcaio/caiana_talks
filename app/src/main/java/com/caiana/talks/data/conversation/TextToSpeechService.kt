package com.caiana.talks.data.conversation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.caiana.talks.domain.model.VoicePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

interface TextToSpeechService {
    suspend fun configure(voice: VoicePreference)
    fun enqueue(sentence: String)
    fun stop()
    val isSpeaking: StateFlow<Boolean>
}

class AndroidTextToSpeechService(
    context: Context
) : TextToSpeechService {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private var tts: TextToSpeech? = null
    private var initialized = false

    init {
        tts = TextToSpeech(context) { status ->
            initialized = status == TextToSpeech.SUCCESS
        }
    }

    override suspend fun configure(voice: VoicePreference) {
        val t = tts ?: return
        val locale = VoiceSelector.localeFor(voice.accent)
        t.language = locale
        t.setSpeechRate(VoiceSelector.rateFor(voice.rate))

        val candidates = t.voices?.map { v ->
            VoiceDescriptor(
                name = v.name,
                locale = v.locale,
                isFeminine = v.name.contains("female", ignoreCase = true) ||
                    v.name.contains("woman", ignoreCase = true)
            )
        } ?: emptyList()

        val selected = VoiceSelector.pickVoice(candidates, voice.gender, locale)
        if (selected != null) {
            t.voices?.firstOrNull { it.name == selected.name }?.let { t.voice = it }
        }
    }

    override fun enqueue(sentence: String) {
        _isSpeaking.value = true
        tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }
}
