package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.DeepSeekService
import com.example.ai.GeminiService
import com.example.model.AiProviderMode
import com.example.model.ProviderStatus
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val providerState by viewModel.providerState.collectAsState()
    val modelInfo by viewModel.characterManager.modelInfo.collectAsState()
    val modelScale by viewModel.characterManager.modelScale.collectAsState()
    val isBackgroundRunning by viewModel.isBackgroundServiceRunning.collectAsState()

    var geminiInputKey by remember { mutableStateOf("") }
    var deepSeekInputKey by remember { mutableStateOf("") }
    var geminiTestResult by remember { mutableStateOf<String?>(null) }
    var deepSeekTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingGemini by remember { mutableStateOf(false) }
    var isTestingDeepSeek by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadCustomGlbModel(uri)
            Toast.makeText(context, "Importing custom 3D model...", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "MYRAA Configuration",
                color = CyanNeon,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Dual AI providers, 3D character, and system security",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Section: AI Router Mode
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI ROUTER MODE", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiProviderMode.values().forEach { mode ->
                            FilterChip(
                                selected = providerState.mode == mode,
                                onClick = { viewModel.setAiMode(mode) },
                                label = { Text(mode.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = VoidBlack,
                                    containerColor = SurfaceVariantDark,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (providerState.mode) {
                            AiProviderMode.AUTO -> "Auto mode routes voice calls to Gemini Live and reasoning to DeepSeek."
                            AiProviderMode.GEMINI -> "Gemini handles real-time voice, vision, and tool execution."
                            AiProviderMode.DEEPSEEK -> "DeepSeek handles reasoning and conversational intelligence."
                        },
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section: Google Gemini Provider
        item {
            val configuredKey = viewModel.keyManager.getGeminiKey()
            val maskedKey = viewModel.keyManager.maskKey(configuredKey)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GOOGLE GEMINI", color = CyanNeon, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        StatusPill(
                            status = if (configuredKey.isNotBlank()) "CONFIGURED" else "NOT CONFIGURED",
                            isSuccess = configuredKey.isNotBlank()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Stored Key: $maskedKey", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = geminiInputKey,
                        onValueChange = { geminiInputKey = it },
                        placeholder = { Text("Enter Gemini API key", color = TextTertiary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = SurfaceVariantDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (geminiInputKey.isNotBlank()) {
                                    viewModel.keyManager.saveGeminiKey(geminiInputKey)
                                    geminiInputKey = ""
                                    viewModel.updateProviderStatuses()
                                    Toast.makeText(context, "Gemini key saved securely in Keystore", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingGemini = true
                                    geminiTestResult = "Testing connection..."
                                    val keyToTest = if (geminiInputKey.isNotBlank()) geminiInputKey else configuredKey
                                    val (success, msg) = GeminiService().testConnection(keyToTest, providerState.geminiModel)
                                    isTestingGemini = false
                                    geminiTestResult = if (success) "✓ Connected successfully" else "⚠️ $msg"
                                }
                            },
                            enabled = !isTestingGemini && (geminiInputKey.isNotBlank() || configuredKey.isNotBlank())
                        ) {
                            Text("TEST", fontSize = 12.sp)
                        }

                        if (configuredKey.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.keyManager.removeGeminiKey()
                                    viewModel.updateProviderStatuses()
                                    geminiTestResult = null
                                    Toast.makeText(context, "Gemini key removed", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                            ) {
                                Text("REMOVE", fontSize = 12.sp)
                            }
                        }
                    }

                    geminiTestResult?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = if (it.startsWith("✓")) SuccessGreen else WarningAmber, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Model Selection:", color = TextSecondary, fontSize = 12.sp)
                    val geminiModels = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        geminiModels.forEach { model ->
                            FilterChip(
                                selected = providerState.geminiModel == model,
                                onClick = { viewModel.setGeminiModel(model) },
                                label = { Text(model.removePrefix("gemini-"), fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanNeon,
                                    selectedLabelColor = VoidBlack
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: DeepSeek Provider
        item {
            val configuredKey = viewModel.keyManager.getDeepSeekKey()
            val maskedKey = viewModel.keyManager.maskKey(configuredKey)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33A855F7), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DEEPSEEK", color = Color(0xFFA855F7), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        StatusPill(
                            status = if (configuredKey.isNotBlank()) "CONFIGURED" else "OPTIONAL",
                            isSuccess = configuredKey.isNotBlank()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Stored Key: $maskedKey", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deepSeekInputKey,
                        onValueChange = { deepSeekInputKey = it },
                        placeholder = { Text("Enter DeepSeek API key (sk-...)", color = TextTertiary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = SurfaceVariantDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deepseek_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (deepSeekInputKey.isNotBlank()) {
                                    viewModel.keyManager.saveDeepSeekKey(deepSeekInputKey)
                                    deepSeekInputKey = ""
                                    viewModel.updateProviderStatuses()
                                    Toast.makeText(context, "DeepSeek key saved securely", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isTestingDeepSeek = true
                                    deepSeekTestResult = "Testing connection..."
                                    val keyToTest = if (deepSeekInputKey.isNotBlank()) deepSeekInputKey else configuredKey
                                    val (success, msg) = DeepSeekService().testConnection(keyToTest, providerState.deepSeekModel)
                                    isTestingDeepSeek = false
                                    deepSeekTestResult = if (success) "✓ Connected successfully" else "⚠️ $msg"
                                }
                            },
                            enabled = !isTestingDeepSeek && (deepSeekInputKey.isNotBlank() || configuredKey.isNotBlank())
                        ) {
                            Text("TEST", fontSize = 12.sp)
                        }

                        if (configuredKey.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.keyManager.removeDeepSeekKey()
                                    viewModel.updateProviderStatuses()
                                    deepSeekTestResult = null
                                    Toast.makeText(context, "DeepSeek key removed", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                            ) {
                                Text("REMOVE", fontSize = 12.sp)
                            }
                        }
                    }

                    deepSeekTestResult?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = if (it.startsWith("✓")) SuccessGreen else WarningAmber, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Model Selection:", color = TextSecondary, fontSize = 12.sp)
                    val deepSeekModels = listOf("deepseek-chat", "deepseek-reasoner")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        deepSeekModels.forEach { model ->
                            FilterChip(
                                selected = providerState.deepSeekModel == model,
                                onClick = { viewModel.setDeepSeekModel(model) },
                                label = { Text(model, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFA855F7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section: Custom 3D Character Asset Import
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D CHARACTER AVATAR", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Upload, contentDescription = null, tint = CyanNeon)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (modelInfo != null) {
                            "Custom Model: ${modelInfo?.fileName} (${modelInfo?.animationNames?.size} animations detected)"
                        } else {
                            "Status: Procedural 3D Neural Avatar Active (Ready for custom .glb / .gltf)"
                        },
                        color = if (modelInfo != null) SuccessGreen else TextSecondary,
                        fontSize = 12.sp
                    )

                    if (modelInfo?.animationNames?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Detected Animations: ${modelInfo?.animationNames?.joinToString(", ")}", color = Color(0xFFA855F7), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack),
                        modifier = Modifier.fillMaxWidth().testTag("upload_glb_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IMPORT CUSTOM 3D MODEL (.GLB / .GLTF)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Model Scale (${String.format("%.1f", modelScale)}x):", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = modelScale,
                        onValueChange = { viewModel.characterManager.setModelScale(it) },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
                    )
                }
            }
        }

        // Section: Background Service & Diagnostics
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Background Assistant Mode", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Persistent foreground service listener", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isBackgroundRunning,
                            onCheckedChange = { viewModel.toggleBackgroundMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = VoidBlack, checkedTrackColor = CyanNeon)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceVariantDark)
                            .clickable { onNavigateToDiagnostics() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open Developer Diagnostics & Logs", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanNeon)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun StatusPill(status: String, isSuccess: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSuccess) SuccessGreen.copy(alpha = 0.15f) else Color(0x2094A3B8)
    ) {
        Text(
            text = status,
            color = if (isSuccess) SuccessGreen else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
