package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DialogueData
import com.example.data.PracticePhrase
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.data.local.VocabularyWord
import com.example.ui.viewmodel.RecordingState
import com.example.ui.viewmodel.TrainerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHostScreen(
    viewModel: TrainerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Coach, 1: Practice, 2: Vocabulary, 3: Stats
    
    // Check and request microphone permission at runtime safely
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startSpeechRecognizerService()
        } else {
            viewModel.injectSpeechRecognizeError("Microphone permission was denied. Please allow mic permissions in settings to practice speaking.")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF49454F)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👩‍🏫",
                                fontSize = 18.sp
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Sophia",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "English Trainer • Online",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {},
                actions = {
                    val isMuted by viewModel.isTtsMuted.collectAsStateWithLifecycle()
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { viewModel.toggleMuteState() }
                            .testTag("app_mute_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute trainer speech" else "Mute trainer speech",
                            tint = Color(0xFF381E72),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("app_navigation_bar"),
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Speaking Coach") },
                    label = { Text("Coach", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_coach")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = "Oral Lessons") },
                    label = { Text("Practice", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_practice")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Word Bank") },
                    label = { Text("Vocabulary", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_words")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics Progress") },
                    label = { Text("Progress", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_progress")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                0 -> CoachTabContent(viewModel, onCheckMicPermission = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.startSpeechRecognizerService()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                })
                1 -> PracticeTabContent(viewModel, onCheckMicPermission = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.startSpeechRecognizerService()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                })
                2 -> VocabularyTabContent(viewModel)
                3 -> ProgressTabContent(viewModel) {
                    selectedTab = 0 // Redirect user to Coach tab to chat
                }
            }
        }
    }
}

// =======================================================
// COACH / AI CHAT TAB CONTENTS
// =======================================================

@Composable
fun CoachTabContent(
    viewModel: TrainerViewModel,
    onCheckMicPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val isWritingReply by viewModel.aiWritingReply.collectAsStateWithLifecycle()
    val speakError by viewModel.speechRecognizeError.collectAsStateWithLifecycle()
    val userSpokenProgress by viewModel.recordedText.collectAsStateWithLifecycle()

    var showScenarioSelector by remember { mutableStateOf(false) }
    var textInputText by remember { mutableStateOf("") }
    
    val chatListState = rememberLazyListState()
    
    // Auto-scroll to lowest chats
    LaunchedEffect(messages.size, isWritingReply) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Scenario Picker Row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Practice Scenario:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = when (currentSession?.scenarioKey) {
                            "casual" -> "🗣️ Casual Dialogue with Sophia"
                            "restaurant" -> "🍔 Ordering at New York Bistro"
                            "interview" -> "💼 Corporate Interview Sim"
                            "airport" -> "✈️ London Heathrow Check-in"
                            else -> "💬 General Speaking"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = { showScenarioSelector = true },
                    modifier = Modifier.testTag("choose_scenario_button")
                ) {
                    Text("Change", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Chat Log Bubbles
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("👋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the microphone below and say hello to Sophia to start practicing!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageBubble(message = message, viewModel = viewModel)
                    }
                    
                    if (isWritingReply) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sophia is thinking & speaking...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error message notification details
        if (speakError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error detail indication",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = speakError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Active Speaking / Atmospheric Waveform overlay
        AnimatedVisibility(visible = recordingState != RecordingState.IDLE) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern atmospheric speaking glow and wave from design HTML
                    AtmosphericSpeakingVisualization(
                        recordingState = recordingState,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Text(
                        text = when (recordingState) {
                            RecordingState.REQUESTING_PERMISSION -> "Requesting microphone..."
                            RecordingState.LISTENING -> "Sophia is listening... speak now!"
                            RecordingState.PROCESSING -> "Analyzing speech clarity..."
                            else -> "Connecting..."
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    if (userSpokenProgress.isNotEmpty() && recordingState == RecordingState.LISTENING) {
                        Text(
                            text = "\"$userSpokenProgress\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Audio Speech Speeds Controller Bar
        TtsSpeedControllerWidget(viewModel)

        // Lower Input Controller Deck styled as modern Immersive UI interaction bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Text Keyboard field fallback
            OutlinedTextField(
                value = textInputText,
                onValueChange = { textInputText = it },
                placeholder = { Text("Type English response...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                trailingIcon = {
                    if (textInputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(textInputText, isSpoken = false)
                                textInputText = ""
                            },
                            modifier = Modifier.testTag("text_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send text",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true
            )

            // Massive vocal record pulse button with dual-box premium design from HTML
            val scale by animateFloatAsState(
                targetValue = if (recordingState == RecordingState.LISTENING) 1.2f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "mic_scale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFD0BCFF))
                    .clickable {
                        if (recordingState == RecordingState.LISTENING) {
                            viewModel.stopListeningAndSubmit()
                        } else if (recordingState == RecordingState.IDLE || recordingState == RecordingState.ERROR) {
                            onCheckMicPermission()
                        }
                    }
                    .testTag("vocal_record_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            color = if (recordingState == RecordingState.LISTENING) Color(0xFFC70039) else Color(0xFF381E72)
                        )
                ) {
                    Icon(
                        imageVector = if (recordingState == RecordingState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Speak microphone input button",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }

    // Modal Scenario dialog
    if (showScenarioSelector) {
        AlertDialog(
            onDismissRequest = { showScenarioSelector = false },
            title = { Text("Choose Practice Session", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.scenariosList) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.startNewTrainerSession(item.title, item.key)
                                    showScenarioSelector = false
                                    Toast.makeText(context, "Lesson scenario: '${item.title}' started!", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentSession?.scenarioKey == item.key) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            border = if (currentSession?.scenarioKey == item.key) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.iconEmoji,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScenarioSelector = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    viewModel: TrainerViewModel
) {
    val context = LocalContext.current
    val isUser = message.sender == "user"
    
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                // Trainer Profile avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👱‍♀️", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Highlights correction brackets if trainer message
                    if (!isUser) {
                        HighlightedCorrectionText(message.text, textColor)
                    } else {
                        Text(
                            text = message.text,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Score indicator on user message if spoken via speech recognizer
                    if (isUser && message.pronunciationScore != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Pronunciation score emblem",
                                modifier = Modifier.size(12.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Accent: ${message.pronunciationScore}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(6.dp))
                // User avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎓", fontSize = 16.sp)
                }
            }
        }

        // Action tray beneath bubbles
        Row(
            modifier = Modifier
                .padding(horizontal = 38.dp, vertical = 2.dp)
                .fillMaxWidth(0.85f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // Speaker audio playback
                IconButton(
                    onClick = { viewModel.speakOut(message.text) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Speak reply text out loud",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Add key vocabulary option
                IconButton(
                    onClick = {
                        // Extract words (simple fallback)
                        val wordsClean = message.text.split(" ").filter { it.length > 5 }
                        if (wordsClean.isNotEmpty()) {
                            val wordToSave = wordsClean.first().replace(Regex("[^a-zA-Z]"), "")
                            viewModel.toggleSaveVocabularyWord(
                                wordToSave,
                                "Key term derived from scenario interaction.",
                                "Example: \"$wordToSave is crucial for fluent expression.\""
                            )
                            Toast.makeText(context, "'$wordToSave' saved to custom Vocabulary Bank!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Highlighting vocabulary is easiest by double-clicking list elements.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick save vocabulary term",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun HighlightedCorrectionText(text: String, defaultColor: Color) {
    // Regex splits text keeping brackets inside arrays
    val segments = text.split("(?=\\[)|(?<=\\])".toRegex())
    
    Text(
        text = buildAnnotatedString {
            segments.forEach { segment ->
                if (segment.startsWith("[") && segment.endsWith("]")) {
                    val content = segment.substring(1, segment.length - 1)
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFFC70039), // Dark vibrant correction red
                            background = Color(0xFFFFD1D1), // Soft highlight
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(" $content ")
                    }
                } else {
                    withStyle(style = SpanStyle(color = defaultColor)) {
                        append(segment)
                    }
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge
    )
}

// =======================================================
// ORAL PRACTICE / PHRASE PRACTICE TAB CONTENTS
// =======================================================

@Composable
fun PracticeTabContent(
    viewModel: TrainerViewModel,
    onCheckMicPermission: () -> Unit
) {
    val context = LocalContext.current
    var activeCategory by remember { mutableStateOf("☕ Cafe & Diner") }
    var selectedPhrase by remember { mutableStateOf<PracticePhrase?>(null) }
    
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
    val pronunciationResult by viewModel.lastPronunciationResult.collectAsStateWithLifecycle()
    val spokenProgress by viewModel.recordedText.collectAsStateWithLifecycle()

    val categories = listOf("☕ Cafe & Diner", "✈️ Airport & Travel", "💼 Career & Office")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "Accented Pronunciation Practice",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Listen to native audio references, speak the words yourself, and get accurate grading on your clarity.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Horizontal Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = activeCategory == category,
                    onClick = { activeCategory = category },
                    label = { Text(category) },
                    modifier = Modifier.testTag("practice_category_chip_$category")
                )
            }
        }

        // Selected Phrase grading display card
        selectedPhrase?.let { phrase ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("grading_display_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TARGET PHRASE (${phrase.level})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        IconButton(
                            onClick = { selectedPhrase = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close practice widget")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = phrase.english,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Meaning: ${phrase.definition}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Pronunciation feedback scoring results
                    if (pronunciationResult != null) {
                        Text(
                            text = "YOUR FEEDBACK SCORE:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "${pronunciationResult?.score}%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = if ((pronunciationResult?.score ?: 0) >= 80) Color(0xFF2E7D32) else Color(0xFFD84315)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            LinearProgressIndicator(
                                progress = { (pronunciationResult?.score ?: 0) / 100f },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(CircleShape),
                                color = if ((pronunciationResult?.score ?: 0) >= 80) Color(0xFF4CAF50) else Color(0xFFFF5722)
                            )
                        }

                        // Detailed Word Level Analysis Colors (Correct=Green, Missed=Red)
                        Text(
                            text = "Word-by-word accent breakdown:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pronunciationResult?.wordAssessments?.forEach { item ->
                                val bgWord = if (item.isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                val txWord = if (item.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                Text(
                                    text = item.word,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgWord)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = txWord,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (pronunciationResult?.textUserSpoke?.isNotBlank() == true) {
                            Text(
                                text = "Spoken: \"${pronunciationResult?.textUserSpoke}\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (recordingState == RecordingState.LISTENING) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Listening to your voice...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = spokenProgress.ifEmpty { "Say: \"${phrase.english}\"" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        // Helpful pronunciation hint
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💡", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = phrase.suggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Native vocal reference
                        OutlinedButton(
                            onClick = { viewModel.speakOut(phrase.english) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Play Native Accent Audio")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hear Audio")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Mic speak trigger button
                        Button(
                            onClick = {
                                if (recordingState == RecordingState.LISTENING) {
                                    viewModel.stopListeningAndSubmit()
                                } else {
                                    // Target user voice matching
                                    onCheckMicPermission()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("practice_speak_check"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recordingState == RecordingState.LISTENING) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (recordingState == RecordingState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Toggle practice speech recording"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (recordingState == RecordingState.LISTENING) "Stop" else "Practice"
                            )
                        }
                    }
                }
            }
        }

        // Preset Practice Phrase Inventory
        Text(
            text = "Choose a dialogue phrase:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        
        val filteredList = DialogueData.phrases.filter { it.category == activeCategory }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList) { phrase ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPhrase = phrase },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPhrase?.id == phrase.id) {
                            MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (selectedPhrase?.id == phrase.id) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    } else null
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = phrase.level,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            IconButton(
                                onClick = { viewModel.speakOut(phrase.english) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak native reference audio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = phrase.english,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Text(
                            text = phrase.definition,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// =======================================================
// VOCABULARY WORD BANK TAB CONTENTS
// =======================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabularyTabContent(viewModel: TrainerViewModel) {
    val words by viewModel.savedWords.collectAsStateWithLifecycle()
    var displayWordDialog by remember { mutableStateOf<VocabularyWord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "Personal Vocabulary Bank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Your custom notepad of complex words discovered during your interactive AI trainer dialogues.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (words.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("📚", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Word Bank is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Connect with Sophia in conversations. Click the '+' icon below her replies to save essential words automatically!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(words) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { displayWordDialog = item }
                            .testTag("vocabulary_word_${item.word}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.word,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = item.definition,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            IconButton(onClick = { viewModel.speakOut(item.word) }) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Pronounce word",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            IconButton(onClick = { viewModel.toggleSaveVocabularyWord(item.word, "", "") }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove vocabulary word",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Vocabulary word detail popup
    if (displayWordDialog != null) {
        val word = displayWordDialog!!
        AlertDialog(
            onDismissRequest = { displayWordDialog = null },
            title = {
                Text(
                    text = word.word.replaceFirstChar { it.titlecase() },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        text = "Definition:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = word.definition,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "Example Usage:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = word.exampleSentence,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { displayWordDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}

// =======================================================
// PROGRESS / STATISTICS TAB CONTENTS
// =======================================================

@Composable
fun ProgressTabContent(
    viewModel: TrainerViewModel,
    onNavigateToChat: () -> Unit
) {
    val stats by viewModel.userStats.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Practice Statistics Dashboard",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Track your learning consistency, spoken vocabulary, and accent convergence progress metrics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Metrics Grid (Streak & Practice mins)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💪 Daily Streak", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stats?.practiceStreak ?: 1} Days",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Keep practicing daily to build habit!", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⏱️ speaking Time", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(stats?.totalSpeakingSeconds ?: 0) / 60}m ${stats?.totalSpeakingSeconds ?: 0}s",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("Active microphone conversation hours.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Average Pronunciation indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(stats?.averagePronunciationScore ?: 0f).toInt()}%",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Accent Clarity Score",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Moving average of your voice recognition matching scores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Session entry logs list
        Text(
            text = "Previous Speaker Sessions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (sessions.isEmpty()) {
            Text(
                text = "No history recorded yet. Start talking with your AI coach!",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sessions.forEach { sess ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectSession(sess)
                                onNavigateToChat()
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = sess.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Coach: ${sess.trainerName} | ${sess.scenarioKey}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            IconButton(onClick = { viewModel.deleteSession(sess.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete custom dialogue history logs",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ADDITIONAL BEAUTIFUL HELPER SUB-COMPOSABLES
// ==========================================

@Composable
fun AnimatedWaveformBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val height1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(28.dp).width(40.dp)
    ) {
        Box(modifier = Modifier.width(4.dp).height(height1.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Box(modifier = Modifier.width(4.dp).height(height2.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Box(modifier = Modifier.width(4.dp).height(height3.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Box(modifier = Modifier.width(4.dp).height(height2.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Box(modifier = Modifier.width(4.dp).height(height1.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}

@Composable
fun TtsSpeedControllerWidget(viewModel: TrainerViewModel) {
    val currentSpeed by viewModel.ttsSpeed.collectAsStateWithLifecycle()
    val ttsMuted by viewModel.isTtsMuted.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Trainer Voice Pace:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Small segmented buttons for speeds
        val speeds = listOf(0.7f to "Sluggish", 1.0f to "Normal", 1.3f to "Brisk")
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            speeds.forEach { speedPair ->
                val isSelected = currentSpeed == speedPair.first
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !ttsMuted) { viewModel.changeTtsSpeed(speedPair.first) },
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (ttsMuted) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = speedPair.second,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (ttsMuted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            } else if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}

@Composable
fun AtmosphericSpeakingVisualization(
    recordingState: RecordingState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "immersive_waveform")
    
    // Wave multipliers if LISTENING to animate them elegantly
    val multiplier by animateFloatAsState(
        targetValue = if (recordingState == RecordingState.LISTENING) 1.2f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "listening_multiplier"
    )
    
    val pulseGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric Glow Behind Waves
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(pulseGlowScale)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD0BCFF).copy(alpha = 0.18f),
                                Color(0xFFD0BCFF).copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        // Wave lines (Varying height and opacities matching HTML design)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            val h1 by infiniteTransition.animateFloat(
                initialValue = 16f, targetValue = 42f,
                animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
                label = "h1"
            )
            val h2 by infiniteTransition.animateFloat(
                initialValue = 28f, targetValue = 74f,
                animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "h2"
            )
            val h3 by infiniteTransition.animateFloat(
                initialValue = 48f, targetValue = 110f,
                animationSpec = infiniteRepeatable(tween(400, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
                label = "h3"
            )
            val h4 by infiniteTransition.animateFloat(
                initialValue = 64f, targetValue = 136f,
                animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "h4"
            )
            val h5 by infiniteTransition.animateFloat(
                initialValue = 36f, targetValue = 88f,
                animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
                label = "h5"
            )
            val h6 by infiniteTransition.animateFloat(
                initialValue = 20f, targetValue = 54f,
                animationSpec = infiniteRepeatable(tween(420, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
                label = "h6"
            )
            val h7 by infiniteTransition.animateFloat(
                initialValue = 10f, targetValue = 30f,
                animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "h7"
            )

            // Bar 1 - 40% Opacity
            Box(modifier = Modifier.width(6.dp).height((h1 * multiplier).coerceAtLeast(8f).dp).background(Color(0xFFD0BCFF).copy(alpha = 0.4f), CircleShape))
            // Bar 2 - 60% Opacity
            Box(modifier = Modifier.width(6.dp).height((h2 * multiplier).coerceAtLeast(14f).dp).background(Color(0xFFD0BCFF).copy(alpha = 0.6f), CircleShape))
            // Bar 3 - 80% Opacity
            Box(modifier = Modifier.width(6.dp).height((h3 * multiplier).coerceAtLeast(20f).dp).background(Color(0xFFD0BCFF).copy(alpha = 0.8f), CircleShape))
            // Bar 4 - Full intensity + glow shadow representation
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height((h4 * multiplier).coerceAtLeast(32f).dp)
                    .background(Color(0xFFD0BCFF), CircleShape)
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFFD0BCFF).copy(alpha = 0.4f),
                            size = size.copy(width = size.width + 8f, height = size.height + 8f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                        )
                    }
            )
            // Bar 5 - 80% Opacity
            Box(modifier = Modifier.width(6.dp).height((h5 * multiplier).coerceAtLeast(18f).dp).background(Color(0xFFD0BCFF).copy(alpha = 0.8f), CircleShape))
            // Bar 6 - 50% Opacity
            Box(modifier = Modifier.width(6.dp).height((h6 * multiplier).coerceAtLeast(10f).dp).background(Color(0xFFD0BCFF).copy(alpha = 0.5f), CircleShape))
            // Bar 7 - 30% Opacity
            Box(modifier = Modifier.width(6.dp).height((h7 * multiplier).coerceAtLeast(6f).dp).background(Color(0xFFD0BCFF).copy(alpha = 0.3f), CircleShape))
        }
    }
}

