package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.TrainerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

// Speech recognition and TTS state enumerables
enum class RecordingState {
    IDLE,
    REQUESTING_PERMISSION,
    LISTENING,
    PROCESSING,
    ERROR
}

data class PronunciationResult(
    val score: Int = 0,
    val wordAssessments: List<WordAssessment> = emptyList(),
    val textUserSpoke: String = ""
)

data class WordAssessment(
    val word: String,
    val isCorrect: Boolean
)

class TrainerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrainerRepository(application)
    
    // Central UI UI state variables
    val sessions = repository.getAllSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savedWords = repository.getAllSavedWords().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val userStats = repository.getStats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    val currentSession: StateFlow<ChatSession?> = _currentSession.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    private val _isTtsMuted = MutableStateFlow(false)
    val isTtsMuted: StateFlow<Boolean> = _isTtsMuted.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(1.0f) // 0.8f for slow, 1.0f for normal, 1.2f for fast
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _aiWritingReply = MutableStateFlow(false)
    val aiWritingReply: StateFlow<Boolean> = _aiWritingReply.asStateFlow()

    private val _recordedText = MutableStateFlow("")
    val recordedText: StateFlow<String> = _recordedText.asStateFlow()

    private val _speechRecognizeError = MutableStateFlow<String?>(null)
    val speechRecognizeError: StateFlow<String?> = _speechRecognizeError.asStateFlow()

    private val _lastPronunciationResult = MutableStateFlow<PronunciationResult?>(null)
    val lastPronunciationResult: StateFlow<PronunciationResult?> = _lastPronunciationResult.asStateFlow()

    // Scenarios data
    val scenariosList = listOf(
        ScenarioItem("casual", "Casual Chit-Chat", "Practice daily conversations, hobbies, weekend plans.", "🗣️"),
        ScenarioItem("restaurant", "ordering food", "Roleplay buying meals, asking for menu, paying tip.", "🍔"),
        ScenarioItem("interview", "job interview prep", "Practice answering behavioral professional queries.", "💼"),
        ScenarioItem("airport", "airport boarding", "Practice talking checking bags, boarding credentials.", "✈️")
    )

    private var activeScenarioKey = "casual"

    // Text to speech objects
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // Speech recognition engines
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechRecognizerIntent: Intent? = null
    private var messageCollectionJob: Job? = null

    init {
        initTextToSpeech()
        initSpeechRecognizer()
        
        // Setup initial default session if none exists
        viewModelScope.launch {
            val existing = sessions.firstOrNull() ?: emptyList()
            if (existing.isNotEmpty()) {
                selectSession(existing.first())
            } else {
                startNewTrainerSession("Daily Conversation", "casual")
            }
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.US)
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isTtsInitialized = true
                        tts.setSpeechRate(_ttsSpeed.value)
                        
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _isTtsSpeaking.value = true
                            }

                            override fun onDone(utteranceId: String?) {
                                _isTtsSpeaking.value = false
                            }

                            @Deprecated("Deprecated in Java", ReplaceWith("_isTtsSpeaking.value = false"))
                            override fun onError(utteranceId: String?) {
                                _isTtsSpeaking.value = false
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                _isTtsSpeaking.value = false
                                Log.e("TrainerViewModel", "TTS error code: $errorCode")
                            }
                        })
                    }
                }
            } else {
                Log.e("TrainerViewModel", "TTS Initialization failed!")
            }
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication())
                speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _recordingState.value = RecordingState.LISTENING
                        _recordedText.value = ""
                        _speechRecognizeError.value = null
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _recordingState.value = RecordingState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your mic."
                            SpeechRecognizer.ERROR_CLIENT -> "Client error. Make sure Google speech app is active."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied! Please enable mic permissions in Settings."
                            SpeechRecognizer.ERROR_NETWORK -> "Network issue occurred during speech process."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout. Try speaking louder or check Wi-Fi."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No spoken words matching English speech were captured."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech system is busy. Hold and talk again check."
                            SpeechRecognizer.ERROR_SERVER -> "Server error. Try speaking closer to microphone."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Speak quickly after clicking."
                            else -> "Mic recognition helper error: code $error"
                        }
                        _speechRecognizeError.value = message
                        _recordingState.value = RecordingState.ERROR
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _recordedText.value = text
                        _recordingState.value = RecordingState.IDLE
                        
                        if (text.isNotBlank()) {
                            sendMessage(text, true)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _recordedText.value = "${it}..."
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            } else {
                Log.w("TrainerViewModel", "Speech recognition not available on this device!")
            }
        } catch (e: Exception) {
            Log.e("TrainerViewModel", "Error creating speech recognizer", e)
        }
    }

    // ==========================================
    // Core User Actions
    // ==========================================

    fun selectSession(session: ChatSession) {
        _currentSession.value = session
        activeScenarioKey = session.scenarioKey
        _lastPronunciationResult.value = null
        
        messageCollectionJob?.cancel()
        messageCollectionJob = viewModelScope.launch {
            repository.getMessagesForSession(session.id)
                .collect { list ->
                    _messages.value = list
                }
        }
    }

    fun startNewTrainerSession(title: String, scenarioKey: String) {
        viewModelScope.launch {
            val trainerName = "Sophia"
            val newSessionId = repository.createSession(title, trainerName, scenarioKey)
            val all = repository.getAllSessions().firstOrNull() ?: emptyList()
            all.find { it.id == newSessionId.toInt() }?.let { session ->
                selectSession(session)
                // Spawn a friendly trainer greeting first if newly initiated Empty session
                viewModelScope.launch {
                    val starterGreeting = when(scenarioKey) {
                        "casual" -> "Hello! I am Sophia, your vocal English practice tutor. Let's talk about how your day is going!"
                        "restaurant" -> "Welcome to Hudson Bistro! I'll be your server today. Would you like to start with some drinks or hear the specials?"
                        "interview" -> "Welcome! Thank you for taking the time to join this English job interview simulation today. Can you start by introducing yourself?"
                        "airport" -> "Hello, welcome to ticket check. May I please have your passport and confirmation code to begin baggage check?"
                        else -> "Hello! I am Sophia, your friendly speaking trainer. Say anything, and let's start practicing!"
                    }
                    repository.addMessage(session.id, "trainer", starterGreeting)
                    speakOut(starterGreeting)
                }
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            val currentId = _currentSession.value?.id
            if (currentId == sessionId) {
                // Return to another session or generate default
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first())
                } else {
                    startNewTrainerSession("Daily Conversation", "casual")
                }
            }
        }
    }

    fun sendMessage(text: String, isSpoken: Boolean = false) {
        if (text.isBlank()) return
        val session = _currentSession.value ?: return
        
        viewModelScope.launch {
            _aiWritingReply.value = true
            
            // Calculate Pronunciation Match if user clicked Voice speak input
            var scoreResult: PronunciationResult? = null
            if (isSpoken) {
                // If it is casual dialog, we grade against the same typed result, but 
                // if we are checking pronunciation practice, we assess expectation vs actual speaking
                scoreResult = evaluatePronunciation(text, text)
                _lastPronunciationResult.value = scoreResult
            }

            // Save User message to Session Database
            repository.addMessage(session.id, "user", text, scoreResult?.score)
            
            // Update stats
            val wordCount = text.split("\\s+".toRegex()).size
            val speakDuration = if (isSpoken) Math.max(2, wordCount / 2) else 0
            repository.incrementStats(speakDuration, wordCount, scoreResult?.score)

            // Make API request to Gemini on background
            val activeHistory = _messages.value
            val responseText = repository.generateAiResponse(
                sessionId = session.id,
                userMessageText = text,
                scenarioKey = session.scenarioKey,
                history = activeHistory
            )

            // Save AI reply response
            repository.addMessage(session.id, "trainer", responseText)
            _aiWritingReply.value = false

            // Automatically speak the Trainer's response, unless muted!
            if (!_isTtsMuted.value) {
                speakOut(responseText)
            }
        }
    }

    // ==========================================
    // Speech Recognition Trigger handlers
    // ==========================================

    fun startListening() {
        _speechRecognizeError.value = null
        _recordingState.value = RecordingState.REQUESTING_PERMISSION
    }

    fun startSpeechRecognizerService() {
        viewModelScope.launch {
            _recordingState.value = RecordingState.LISTENING
            speechRecognizer?.startListening(speechRecognizerIntent)
        }
    }

    fun stopListeningAndSubmit() {
        speechRecognizer?.stopListening()
        _recordingState.value = RecordingState.PROCESSING
    }

    fun cancelListening() {
        speechRecognizer?.cancel()
        _recordingState.value = RecordingState.IDLE
    }

    fun injectSpeechRecognizeError(errorMsg: String) {
        _speechRecognizeError.value = errorMsg
        _recordingState.value = RecordingState.ERROR
    }

    // ==========================================
    // TTS triggers
    // ==========================================

    fun speakOut(text: String) {
        if (!isTtsInitialized) return
        textToSpeech?.let { tts ->
            val cleanText = text.replace(Regex("\\[.*?\\]"), "") // omit corrections during speech audio
            tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "UtteranceId")
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isTtsSpeaking.value = false
    }

    fun toggleMuteState() {
        _isTtsMuted.value = !_isTtsMuted.value
        if (_isTtsMuted.value) {
            stopSpeaking()
        }
    }

    fun changeTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
        textToSpeech?.setSpeechRate(speed)
    }

    // ==========================================
    // Pronunciation Match Assessment
    // ==========================================

    fun evaluatePronunciation(expectedText: String, userSpokenText: String): PronunciationResult {
        val pattern = "[^a-zA-Z\\s]".toRegex()
        val cleanExpectedWords = expectedText.lowercase().replace(pattern, "").split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val cleanActualWords = userSpokenText.lowercase().replace(pattern, "").split("\\s+".toRegex()).filter { it.isNotEmpty() }

        if (cleanExpectedWords.isEmpty() || cleanActualWords.isEmpty()) {
            return PronunciationResult(0, emptyList(), userSpokenText)
        }

        var correctMatchesCount = 0
        val wordAssessments = mutableListOf<WordAssessment>()

        for (expWord in cleanExpectedWords) {
            // Find if this exact word exists within the actual word cloud spoke by user
            val matched = cleanActualWords.any { actWord ->
                actWord == expWord || (actWord.length > 4 && expWord.length > 4 && 
                        (actWord.startsWith(expWord.substring(0, 3)) || expWord.startsWith(actWord.substring(0, 3))))
            }
            if (matched) {
                correctMatchesCount++
                wordAssessments.add(WordAssessment(expWord, true))
            } else {
                wordAssessments.add(WordAssessment(expWord, false))
            }
        }

        val percentageScore = ((correctMatchesCount.toFloat() / cleanExpectedWords.size) * 100).toInt().coerceIn(15, 100)
        return PronunciationResult(
            score = percentageScore,
            wordAssessments = wordAssessments,
            textUserSpoke = userSpokenText
        )
    }

    // ==========================================
    // Word Bank storage updates
    // ==========================================

    fun toggleSaveVocabularyWord(word: String, definition: String, example: String) {
        viewModelScope.launch {
            val exists = repository.isWordSaved(word).firstOrNull() ?: false
            if (exists) {
                // Delete if already saved (toggle mechanics)
                val all = savedWords.value
                all.find { it.word.lowercase() == word.trim().lowercase() }?.let {
                    repository.deleteWord(it.id)
                }
            } else {
                repository.saveWord(word, definition, example, _currentSession.value?.scenarioKey ?: "casual")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}

// Helper models for trainer metadata
data class ScenarioItem(
    val key: String,
    val title: String,
    val description: String,
    val iconEmoji: String
)
