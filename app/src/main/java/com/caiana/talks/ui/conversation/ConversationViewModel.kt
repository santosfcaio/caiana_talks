package com.caiana.talks.ui.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caiana.talks.data.conversation.RollingWindow
import com.caiana.talks.data.conversation.SentenceChunker
import com.caiana.talks.data.conversation.SpeechRecognizerService
import com.caiana.talks.data.conversation.SttEvent
import com.caiana.talks.data.conversation.SystemPromptBuilder
import com.caiana.talks.data.conversation.TextToSpeechService
import com.caiana.talks.data.local.db.buildDualConfig
import com.caiana.talks.data.local.db.toConversationConfig
import com.caiana.talks.data.remote.ConversationAiClient
import com.caiana.talks.data.repository.ConversationRepository
import com.caiana.talks.data.repository.SessionHandle
import com.caiana.talks.data.repository.UserRepository
import com.caiana.talks.domain.model.AiResponseMeta
import com.caiana.talks.domain.model.AiStreamEvent
import com.caiana.talks.domain.model.ConversationConfig
import com.caiana.talks.domain.model.ConversationError
import com.caiana.talks.domain.model.ConversationMessage
import com.caiana.talks.domain.model.SessionMode
import com.caiana.talks.domain.model.SessionResult
import com.caiana.talks.domain.model.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiState(
    val phase: Phase = Phase.IDLE,
    val mode: SessionMode = SessionMode.SINGLE,
    val aiPersonaName: String = "",
    val activeSpeakerName: String? = null,
    val liveTranscript: String = "",
    val turns: List<TurnUi> = emptyList(),
    val elapsedSeconds: Int = 0,
    val portugueseNudgeVisible: Boolean = false,
    val error: ConversationError? = null,
    val sessionResult: SessionResult? = null
) {
    enum class Phase { IDLE, LISTENING, THINKING, SPEAKING, ENDED }
}

data class TurnUi(
    val speakerName: String,
    val userText: String,
    val aiText: String,
    val vocabulary: List<String> = emptyList()
)

@HiltViewModel
class ConversationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sttService: SpeechRecognizerService,
    private val ttsService: TextToSpeechService,
    private val aiClient: ConversationAiClient,
    private val repository: ConversationRepository,
    private val promptBuilder: SystemPromptBuilder,
    private val userRepository: UserRepository
) : ViewModel() {

    // Navigation args: secondProfileId is only set for dual mode
    private val secondProfileId: Int? = savedStateHandle.get<String>("secondProfileId")?.toIntOrNull()

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var config: ConversationConfig? = null
    private val configDeferred = CompletableDeferred<ConversationConfig?>()
    private var sessionHandle: SessionHandle? = null
    private val conversationHistory = mutableListOf<ConversationMessage>()
    private var activeSpeakerIndex = 0
    private var listenJob: Job? = null

    init {
        viewModelScope.launch {
            val profiles = userRepository.getAllProfiles().first()
            val activeProfile = userRepository.getActiveUserProfile().first()
            if (activeProfile == null) {
                configDeferred.complete(null)
                return@launch
            }

            config = if (secondProfileId != null) {
                val secondProfile = profiles.firstOrNull { it.id == secondProfileId }
                if (secondProfile != null) buildDualConfig(activeProfile, secondProfile)
                else activeProfile.toConversationConfig()
            } else {
                activeProfile.toConversationConfig()
            }

            val cfg = config ?: run { configDeferred.complete(null); return@launch }
            configDeferred.complete(cfg)
            _uiState.update {
                it.copy(
                    mode = cfg.mode,
                    aiPersonaName = cfg.persona.displayName,
                    activeSpeakerName = cfg.participants.firstOrNull()?.name
                )
            }
        }
    }

    fun onStart() {
        viewModelScope.launch {
            val cfg = configDeferred.await() ?: return@launch
            try {
                ttsService.configure(cfg.voice)
            } catch (_: Exception) { }
            sessionHandle = repository.startSession(cfg)
            startListening()
        }
    }

    private fun startListening() {
        _uiState.update { it.copy(phase = ConversationUiState.Phase.LISTENING, liveTranscript = "") }
        listenJob = viewModelScope.launch {
            sttService.listen().collect { event ->
                when (event) {
                    is SttEvent.Partial -> _uiState.update { it.copy(liveTranscript = event.text) }
                    is SttEvent.Final -> handleFinalTranscript(event.text)
                    is SttEvent.Silence -> _uiState.update { it.copy(liveTranscript = "") }
                    is SttEvent.Failed -> _uiState.update {
                        it.copy(error = event.error, phase = ConversationUiState.Phase.IDLE)
                    }
                }
            }
        }
    }

    private fun handleFinalTranscript(text: String) {
        _uiState.update { it.copy(phase = ConversationUiState.Phase.THINKING, liveTranscript = text) }
        viewModelScope.launch { streamAiResponse(text) }
    }

    private suspend fun streamAiResponse(userInput: String) {
        val cfg = config ?: return
        val prompt = promptBuilder.build(cfg)
        val window = RollingWindow.take(conversationHistory)
        val chunker = SentenceChunker()
        val aiResponseBuffer = StringBuilder()
        var meta = AiResponseMeta(emptyList(), emptyList(), false)
        var phaseSetToSpeaking = false
        var sayDone = false

        aiClient.streamReply(prompt, window, userInput).collect { event ->
            when (event) {
                is AiStreamEvent.TextDelta -> {
                    aiResponseBuffer.append(event.text)
                    if (!phaseSetToSpeaking) {
                        phaseSetToSpeaking = true
                        _uiState.update { it.copy(phase = ConversationUiState.Phase.SPEAKING) }
                    }
                    if (!sayDone) {
                        val sentences = chunker.accept(event.text)
                        sentences.forEach { ttsService.enqueue(it) }
                    }
                }
                is AiStreamEvent.SayEnded -> {
                    sayDone = true
                    chunker.flush().forEach { ttsService.enqueue(it) }
                }
                is AiStreamEvent.Completed -> {
                    meta = event.meta
                    if (!sayDone) chunker.flush().forEach { ttsService.enqueue(it) }
                }
                is AiStreamEvent.Failed -> {
                    _uiState.update { it.copy(error = event.error) }
                    return@collect
                }
            }
        }

        persistTurn(userInput, aiResponseBuffer.toString(), meta, cfg)

        conversationHistory.add(ConversationMessage(ConversationMessage.Role.USER, userInput))
        conversationHistory.add(ConversationMessage(ConversationMessage.Role.ASSISTANT, aiResponseBuffer.toString()))

        val activeSpeaker = cfg.participants.getOrNull(activeSpeakerIndex)
        val nextIndex = if (cfg.mode == SessionMode.DUAL)
            (activeSpeakerIndex + 1) % cfg.participants.size
        else activeSpeakerIndex

        _uiState.update { state ->
            state.copy(
                turns = state.turns + TurnUi(
                    speakerName = activeSpeaker?.name ?: "",
                    userText = userInput,
                    aiText = aiResponseBuffer.toString(),
                    vocabulary = meta.vocabulary
                ),
                portugueseNudgeVisible = meta.userSpokePortuguese,
                phase = ConversationUiState.Phase.LISTENING,
                liveTranscript = "",
                activeSpeakerName = cfg.participants.getOrNull(nextIndex)?.name
            )
        }
        activeSpeakerIndex = nextIndex
        startListening()
    }

    private suspend fun persistTurn(
        userText: String,
        aiText: String,
        meta: AiResponseMeta,
        cfg: ConversationConfig
    ) {
        val handle = sessionHandle ?: return
        val speakerProfileId = cfg.participants.getOrNull(activeSpeakerIndex)?.profileId ?: return
        try {
            repository.appendTurn(handle, speakerProfileId, userText, aiText, meta)
        } catch (_: java.io.IOException) {
            listenJob?.cancel()
            ttsService.stop()
            repository.finalizeSession(handle, SessionStatus.PARTIAL)
            _uiState.update { it.copy(error = ConversationError.STORAGE_FULL, phase = ConversationUiState.Phase.IDLE) }
        }
    }

    fun onEndSession() {
        listenJob?.cancel()
        ttsService.stop()
        viewModelScope.launch {
            val handle = sessionHandle ?: return@launch
            val result = repository.finalizeSession(handle, SessionStatus.COMPLETED)
            _uiState.update { it.copy(phase = ConversationUiState.Phase.ENDED, sessionResult = result) }
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(error = ConversationError.MIC_PERMISSION_DENIED) }
    }

    fun onAppBackgrounded() {
        listenJob?.cancel()
        ttsService.stop()
        viewModelScope.launch {
            val handle = sessionHandle ?: return@launch
            val result = repository.finalizeSession(handle, SessionStatus.PARTIAL)
            _uiState.update { it.copy(phase = ConversationUiState.Phase.ENDED, sessionResult = result) }
        }
    }

    fun onRetry() {
        _uiState.update { it.copy(error = null) }
        startListening()
    }
}
