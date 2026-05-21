package com.caiana.talks.data.conversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.caiana.talks.domain.model.ConversationError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface SttEvent {
    data class Partial(val text: String) : SttEvent
    data class Final(val text: String) : SttEvent
    data object Silence : SttEvent
    data class Failed(val error: ConversationError) : SttEvent
}

interface SpeechRecognizerService {
    fun listen(languageTag: String = "en-US"): Flow<SttEvent>
    fun stop()
}

class AndroidSpeechRecognizerService(
    private val context: Context
) : SpeechRecognizerService {

    private var recognizer: SpeechRecognizer? = null

    override fun listen(languageTag: String): Flow<SttEvent> = callbackFlow {
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> trySend(SttEvent.Silence)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> trySend(
                        SttEvent.Failed(ConversationError.MIC_PERMISSION_DENIED)
                    )
                    else -> trySend(SttEvent.Failed(ConversationError.MIC_UNAVAILABLE))
                }
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                trySend(SttEvent.Final(text))
                channel.close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                trySend(SttEvent.Partial(text))
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        sr.startListening(intent)

        awaitClose {
            sr.destroy()
            recognizer = null
        }
    }

    override fun stop() {
        recognizer?.stopListening()
    }
}
