package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CharacterState
import com.example.scene3d.Character3DView
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.IndigoElectric
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack
import kotlin.math.sin

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val characterState by viewModel.characterManager.characterState.collectAsState()
    val statusText by viewModel.characterManager.statusText.collectAsState()
    val audioAmplitude by viewModel.characterManager.audioAmplitude.collectAsState()
    val modelInfo by viewModel.characterManager.modelInfo.collectAsState()
    val activeAnimation = viewModel.characterManager.getActiveAnimationName()

    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val partialText by viewModel.voiceManager.partialText.collectAsState()
    val providerState by viewModel.providerState.collectAsState()
    val isScreenVisionActive by viewModel.isScreenVisionActive.collectAsState()
    val isBackgroundRunning by viewModel.isBackgroundServiceRunning.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()

    var quickInputText by remember { mutableStateOf("") }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    // System Speech Recognizer Activity Fallback Launcher (guarantees voice input on all Android systems)
    val speechActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.processUserPrompt(spoken, isVoice = true)
            }
        }
    }

    // Microphone Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (viewModel.voiceManager.isSpeechRecognitionAvailable()) {
                viewModel.startVoiceInteraction()
            } else {
                speechActivityLauncher.launch(viewModel.voiceManager.createSystemSpeechIntent())
            }
        } else {
            showPermissionExplanation = true
            viewModel.characterManager.setState(
                CharacterState.ERROR,
                "Microphone permission is required to speak with MYRAA"
            )
        }
    }

    fun handleMicClick() {
        if (isListening || isSpeaking) {
            viewModel.stopVoiceInteraction()
            return
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (viewModel.voiceManager.isSpeechRecognitionAvailable()) {
                viewModel.startVoiceInteraction()
            } else {
                try {
                    speechActivityLauncher.launch(viewModel.voiceManager.createSystemSpeechIntent())
                } catch (_: Exception) {
                    viewModel.startVoiceInteraction()
                }
            }
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VoidBlack,
                        Color(0xFF0A0E1A),
                        Color(0xFF0E1428)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: MYRAA Title + Provider State + Screen Vision + Background Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MYRAA",
                            color = CyanNeon,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (characterState == CharacterState.OFFLINE) Color.Gray else SuccessGreen)
                        )
                    }
                    Text(
                        text = "● ${providerState.activeProviderName.uppercase()}",
                        color = Color(0xFFA855F7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Screen Vision Quick Toggle
                    IconButton(
                        onClick = { viewModel.toggleScreenVision() },
                        modifier = Modifier
                            .testTag("toggle_screen_vision")
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isScreenVisionActive) CyanNeon.copy(alpha = 0.2f) else SurfaceVariantDark)
                    ) {
                        Icon(
                            imageVector = if (isScreenVisionActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Screen Vision",
                            tint = if (isScreenVisionActive) CyanNeon else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Background Mode Service Toggle
                    IconButton(
                        onClick = { viewModel.toggleBackgroundService() },
                        modifier = Modifier
                            .testTag("toggle_background_service")
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isBackgroundRunning) SuccessGreen.copy(alpha = 0.2f) else SurfaceVariantDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Background Service",
                            tint = if (isBackgroundRunning) SuccessGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Screen Vision Active Banner
            AnimatedVisibility(visible = isScreenVisionActive) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyanNeon.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Screen Vision Active • Context inspection ready", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 3D Avatar Centerpiece View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Character3DView(
                    state = characterState,
                    audioAmplitude = audioAmplitude,
                    modelInfo = modelInfo,
                    activeAnimation = activeAnimation,
                    onAvatarClick = {
                        if (characterState == CharacterState.IDLE) {
                            handleMicClick()
                        }
                    }
                )
            }

            // Dynamic Voice Waveform
            VoiceWaveformView(
                isActive = isListening || isSpeaking || characterState == CharacterState.SPEAKING,
                amplitude = audioAmplitude,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(30.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status Text / Live Speech Bubble
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceGlass,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x2600F2FE), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayText = if (isListening && partialText.isNotBlank()) {
                        "Listening: \"$partialText\""
                    } else {
                        statusText
                    }

                    Text(
                        text = displayText,
                        color = if (isListening && partialText.isNotBlank()) CyanNeon else TextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Suggestions Chips (Hindi & English)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = listOf(
                    "नमस्ते MYRAA",
                    "टॉर्च चालू करो",
                    "Open Camera",
                    "Open YouTube",
                    "Open Settings",
                    "Who are you?"
                )
                suggestions.forEach { text ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.processUserPrompt(text, isVoice = true) },
                        label = { Text(text, fontSize = 11.sp, color = CyanNeon) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceVariantDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = CyanNeon.copy(alpha = 0.3f),
                            enabled = true,
                            selected = false
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Interruption Action when Speaking
            AnimatedVisibility(
                visible = isSpeaking || characterState == CharacterState.SPEAKING,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cancelActiveResponse() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STOP SPEAKING", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Control Row: Quick Text Input + Big Interactive Mic Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quickInputText,
                    onValueChange = { quickInputText = it },
                    placeholder = { Text("बोलिए या टाइप करें...", color = TextTertiary, fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (quickInputText.isNotBlank()) {
                            viewModel.processUserPrompt(quickInputText, isVoice = true)
                            quickInputText = ""
                        }
                    }),
                    trailingIcon = {
                        if (quickInputText.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.processUserPrompt(quickInputText, isVoice = true)
                                quickInputText = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = CyanNeon, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = SurfaceVariantDark,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_quick_input")
                )

                // Big Mic Button (Handles Voice Recognition & Interruption)
                BigMicButton(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    onClick = { handleMicClick() }
                )
            }
        }

        // Action Confirmation Modal
        pendingConfirmation?.let { action ->
            AlertDialog(
                onDismissRequest = { viewModel.cancelPendingAction() },
                containerColor = SurfaceDark,
                title = {
                    Text(
                        text = "Confirmation Required",
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = action.prompt,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmPendingAction() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack)
                    ) {
                        Text("EXECUTE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelPendingAction() }) {
                        Text("CANCEL", color = ErrorRed)
                    }
                }
            )
        }

        // Microphone Permission Explanation Modal
        if (showPermissionExplanation) {
            AlertDialog(
                onDismissRequest = { showPermissionExplanation = false },
                containerColor = SurfaceDark,
                title = {
                    Text(
                        text = "Microphone Permission Required",
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "MYRAA needs microphone access to listen to your voice and speak responses. Please grant microphone access to enable voice commands.",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionExplanation = false
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack)
                    ) {
                        Text("ALLOW MICROPHONE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionExplanation = false }) {
                        Text("CANCEL", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun BigMicButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val activeScale = if (isListening) pulseScale else 1f

    val glowBrush = when {
        isListening -> Brush.radialGradient(listOf(CyanNeon, IndigoElectric))
        isSpeaking -> Brush.radialGradient(listOf(Color(0xFFA855F7), CyanNeon))
        else -> Brush.radialGradient(listOf(CyanNeon, Color(0xFF0077FE)))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(58.dp)
            .shadow(if (isListening) 16.dp else 6.dp, CircleShape, spotColor = CyanGlow)
            .clip(CircleShape)
            .background(glowBrush)
            .clickable { onClick() }
            .testTag("mic_talk_button")
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isListening) "Stop Talking" else "Talk to MYRAA",
            tint = VoidBlack,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun VoiceWaveformView(
    isActive: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val barCount = 28
        val spacing = size.width / barCount
        val barWidth = spacing * 0.45f
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.85f

        for (i in 0 until barCount) {
            val x = i * spacing + barWidth / 2f
            val wave = sin(phase + i * 0.35f)

            val heightFactor = if (isActive) {
                ((wave + 1f) * 0.5f * 0.3f + amplitude * 0.7f).coerceIn(0.12f, 1f)
            } else {
                0.08f
            }

            val barHeight = maxBarHeight * heightFactor
            val top = centerY - barHeight / 2f
            val bottom = centerY + barHeight / 2f

            val barColor = if (isActive) {
                if (i % 2 == 0) CyanNeon else Color(0xFFA855F7)
            } else {
                Color(0x3394A3B8)
            }

            drawLine(
                color = barColor,
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
