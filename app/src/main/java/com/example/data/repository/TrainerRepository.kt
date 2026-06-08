package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TrainerRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val chatDao = db.chatDao()
    private val vocabularyDao = db.vocabularyDao()
    private val statsDao = db.statsDao()

    // 1. Sessions & Messages
    fun getAllSessions(): Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>> = 
        chatDao.getMessagesForSession(sessionId)

    suspend fun createSession(title: String, trainerName: String, scenarioKey: String): Long = 
        withContext(Dispatchers.IO) {
            chatDao.insertSession(
                ChatSession(
                    title = title,
                    trainerName = trainerName,
                    scenarioKey = scenarioKey
                )
            )
        }

    suspend fun addMessage(
        sessionId: Int, 
        sender: String, 
        text: String, 
        pronunciationScore: Int? = null
    ): Long = withContext(Dispatchers.IO) {
        chatDao.insertMessage(
            ChatMessage(
                sessionId = sessionId,
                sender = sender,
                text = text,
                pronunciationScore = pronunciationScore
            )
        )
    }

    suspend fun deleteSession(sessionId: Int) = withContext(Dispatchers.IO) {
        chatDao.deleteSession(sessionId)
    }

    // 2. Vocabulary Bank
    fun getAllSavedWords(): Flow<List<VocabularyWord>> = vocabularyDao.getAllWords()

    suspend fun saveWord(word: String, definition: String, example: String, scenario: String): Long = 
        withContext(Dispatchers.IO) {
            vocabularyDao.insertWord(
                VocabularyWord(
                    word = word.trim(),
                    definition = definition,
                    exampleSentence = example,
                    contextScenario = scenario
                )
            )
        }

    suspend fun deleteWord(id: Int) = withContext(Dispatchers.IO) {
        vocabularyDao.deleteWord(id)
    }

    fun isWordSaved(word: String): Flow<Boolean> = vocabularyDao.isWordSaved(word.trim())

    // 3. User Practice Stats
    fun getStats(): Flow<UserStats?> = statsDao.getStats()

    suspend fun incrementStats(durationSec: Int, wordCount: Int, pronunciationScore: Int?) = 
        withContext(Dispatchers.IO) {
            val currentStats = statsDao.getStats().firstOrNull() ?: UserStats()
            
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(calendar.time)

            // Streak logic
            val newStreak = when (currentStats.lastPracticeDate) {
                todayStr -> currentStats.practiceStreak // Already practiced today
                yesterdayStr -> currentStats.practiceStreak + 1 // Consecutive day
                "" -> 1 // First time ever
                else -> 1 // Broke streak, reset
            }

            // Average score logic
            val newAverage = if (pronunciationScore != null) {
                if (currentStats.averagePronunciationScore == 0f) {
                    pronunciationScore.toFloat()
                } else {
                    (currentStats.averagePronunciationScore * 4 + pronunciationScore) / 5f // Moving average
                }
            } else {
                currentStats.averagePronunciationScore
            }

            val updated = UserStats(
                totalPhrasesSpoken = currentStats.totalPhrasesSpoken + (if (wordCount > 0) 1 else 0),
                totalSpeakingSeconds = currentStats.totalSpeakingSeconds + durationSec,
                practiceStreak = newStreak,
                lastPracticeDate = todayStr,
                averagePronunciationScore = newAverage
            )
            statsDao.insertOrUpdateStats(updated)
        }

    // 4. AI Speaking Partner Conversation Engine
    suspend fun generateAiResponse(
        sessionId: Int,
        userMessageText: String,
        scenarioKey: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("TrainerRepository", "Gemini API Key is empty or placeholder!")
            return@withContext "I'm sorry, my AI voice brain needs an API Key. Please insert your GEMINI_API_KEY into the Secrets Panel in AI Studio to let us chat!"
        }

        // 1. Choose Trainer System Prompt based on Scenario
        val systemPrompt = when (scenarioKey) {
            "casual" -> """
                You are Sophia, an exceptionally warm, empathetic, and vibrant English vocal coach and conversation partner.
                Rules:
                1. Converse with the user naturally in normal casual spoken English.
                2. Keep responses very short and oral (1 to 3 conversational sentences), so it sounds fluent when read aloud.
                3. GENTLY and briefly correct any obvious grammatical anomalies in the user's last message, placing the corrected version in brackets, e.g. "I went [I had gone] to the store". Keep the atmosphere friendly.
                4. Ask a thoughtful, easy question at the end of your response to encourage speaking.
            """.trimIndent()
            
            "restaurant" -> """
                You are Sophia, a professional, friendly, and energetic server at a classy high-profile bistro in New York.
                Rules:
                1. Guide the user through a roleplay scenario where they are the dining customer and you are the server.
                2. Respond with common restaurant server lines (1 to 2 sentences max).
                3. If the user makes a major language mistake, politely rephrase or correct it briefly within brackets, then answer as the server.
                4. Keep the food order workflow active: seat them, suggest daily specials, take appetizers/mains/drinks, serve, and deliver the check.
            """.trimIndent()

            "interview" -> """
                You are Sophia, a highly professional, respectful, yet candid senior recruiter at a global technology company.
                Rules:
                1. Conduct an English job interview roleplay. Ask typical interview questions (e.g., self-introduction, past struggles, solving conflicts, future vision).
                2. Listen to their responses and ask relevant, realistic follow-up questions.
                3. Correct grammatical issues gracefully in brackets [like this] where necessary, but keep the focus on professional communication.
                4. Keep questions concise (2-3 sentences), allowing the user to practice high-level professional vocabulary.
            """.trimIndent()

            "airport" -> """
                You are Sophia, a helpful and efficient ground checkpoint agent at London Heathrow Airport check-in counters.
                Rules:
                1. Guide the user through check-in roleplay: ask for passports, check-in baggage, security alerts, and seat preference.
                2. Keep messages concise and official (1 to 2 sentences).
                3. Wrap brief English phrasing improvements in brackets [like this], then proceed with the checkpoint procedures.
            """.trimIndent()

            else -> """
                You are Sophia, a supportive, warm, and professional English learning speaking coach.
                Keep answers concise (2-3 sentences), correct key errors gracefully in brackets, and ask a relevant question to keep the dialogue flowing.
            """.trimIndent()
        }

        // 2. Map Room History into Gemini contents array
        val recentHistory = history.takeLast(10) // Keep standard window of last 10 dialog cards
        val contents = mutableListOf<Content>()
        
        recentHistory.forEach { msg ->
            if (msg.text.isNotEmpty()) {
                val role = if (msg.sender == "user") "user" else "model"
                contents.add(
                    Content(
                        parts = listOf(Part(text = msg.text)),
                        role = role
                    )
                )
            }
        }

        // Append the latest user message context if not already in recentHistory list
        if (recentHistory.none { it.text == userMessageText }) {
            contents.add(
                Content(
                    parts = listOf(Part(text = userMessageText)),
                    role = "user"
                )
            )
        }

        val request = GenerateContentRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                topK = 40
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemPrompt))
            )
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (reply.isNullOrBlank()) {
                "I was listening closely, but didn't catch that. Could you try saying that again?"
            } else {
                reply
            }
        } catch (e: Exception) {
            Log.e("TrainerRepository", "Error contacting Gemini API", e)
            "Speech Trainer Error: ${e.localizedMessage ?: "Connection Timeout"}. Please check your internet connection or API Key."
        }
    }
}
