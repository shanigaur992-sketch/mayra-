package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.IndigoElectric
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import kotlinx.coroutines.delay

@Composable
fun FirstLaunchScreen(
    viewModel: MainViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) } // 0: Cinematic Logo, 1: Provider Setup, 2: Mic Permission, 3: Ready
    var geminiKeyInput by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        step = 3
    }

    val infiniteTransition = rememberInfiniteTransition(label = "first_launch_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    LaunchedEffect(step) {
        if (step == 0) {
            delay(2200)
            step = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VoidBlack,
                        Color(0xFF090E1F),
                        Color(0xFF0E162B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Step 0: Cinematic startup
            if (step == 0) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(CyanNeon, IndigoElectric))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "MYRAA Logo",
                        tint = VoidBlack,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "MYRAA",
                    color = CyanNeon,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "INITIALIZING AI ASSISTANT...",
                    color = Color(0xFFA855F7),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                LinearProgressIndicator(
                    modifier = Modifier.width(180.dp),
                    color = CyanNeon,
                    trackColor = SurfaceVariantDark
                )
            }

            // Step 1: Provider Setup
            if (step == 1) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Configure AI Provider",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter your Google Gemini API key to activate real-time voice and native device tools. Stored securely in Android Keystore.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            placeholder = { Text("Gemini API Key (or configure later)", color = TextSecondary, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = SurfaceVariantDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("first_launch_gemini_key")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (geminiKeyInput.isNotBlank()) {
                                    viewModel.keyManager.saveGeminiKey(geminiKeyInput)
                                    viewModel.updateProviderStatuses()
                                }
                                step = 2
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack),
                            modifier = Modifier.fillMaxWidth().testTag("first_launch_continue_button")
                        ) {
                            Text("CONTINUE", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            // Step 2: Microphone Permission Explanation
            if (step == 2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Microphone Access",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "MYRAA requires microphone access for real-time speech conversation, voice interruption, and waveform analysis. You can enable or disable this anytime.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val hasAudioPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasAudioPerm) {
                                    step = 3
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack),
                            modifier = Modifier.fillMaxWidth().testTag("first_launch_mic_grant_button")
                        ) {
                            Text("GRANT PERMISSION", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Step 3: Initialization Completed
            if (step == 3) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x3310B981), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "MYRAA ONLINE",
                            color = SuccessGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "3D character initialized • Dual AI routing ready • Native Android tools loaded",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.completeFirstLaunch()
                                onFinished()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack),
                            modifier = Modifier.fillMaxWidth().testTag("enter_myraa_button")
                        ) {
                            Text("ENTER MYRAA DASHBOARD", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
