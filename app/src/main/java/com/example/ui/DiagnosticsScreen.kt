package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DiagnosticLogEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val providerState by viewModel.providerState.collectAsState()
    val characterState by viewModel.characterManager.characterState.collectAsState()
    val modelInfo by viewModel.characterManager.modelInfo.collectAsState()
    val logs by viewModel.diagnosticLogs.collectAsState()

    val runtime = Runtime.getRuntime()
    val usedMemMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    val maxMemMB = runtime.maxMemory() / (1024 * 1024)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CyanNeon
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "System Diagnostics & Telemetry",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("ACTIVE TELEMETRY", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        TelemetryRow("Gemini Live Engine", providerState.geminiStatus.label)
                        TelemetryRow("DeepSeek Engine", providerState.deepSeekStatus.label)
                        TelemetryRow("Selected Mode", providerState.mode.label)
                        TelemetryRow("Last Roundtrip Latency", "${providerState.latencyMs} ms")
                        TelemetryRow("3D Avatar State", characterState.name)
                        TelemetryRow("JVM Memory", "$usedMemMB MB / $maxMemMB MB")
                        TelemetryRow(
                            "3D Model Status",
                            if (modelInfo != null) "${modelInfo?.fileName} (${modelInfo?.animationNames?.size} clips)" else "Procedural 3D Mesh"
                        )
                    }
                }
            }

            // Logs Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LOCAL DIAGNOSTIC LOGS", color = Color(0xFFA855F7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { viewModel.clearLogs() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = ErrorRed),
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CLEAR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (logs.isEmpty()) {
                item {
                    Text("No diagnostic events logged yet", color = TextTertiary, fontSize = 12.sp)
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    DiagnosticLogCard(log)
                }
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DiagnosticLogCard(log: DiagnosticLogEntity) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = timeFormat.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${log.status} • ${log.event}",
                    color = if (log.status == "SUCCESS") SuccessGreen else ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(timeStr, color = TextTertiary, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Provider: ${log.provider}" + (log.latencyMs?.let { " (${it}ms)" } ?: "") + (log.tool?.let { " • Tool: $it" } ?: ""),
                color = TextSecondary,
                fontSize = 11.sp
            )
            log.sanitizedDetails?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(it, color = TextTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
