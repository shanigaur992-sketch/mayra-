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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tools.ToolCategory
import com.example.tools.ToolDefinition
import com.example.tools.ToolRegistry
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningAmber

@Composable
fun ToolsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categories = ToolCategory.values()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Centralized Tool Registry",
                color = CyanNeon,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Secured native Android APIs accessible by MYRAA",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        categories.forEach { category ->
            val tools = ToolRegistry.getToolsByCategory(category)
            if (tools.isNotEmpty()) {
                item {
                    Text(
                        text = category.title.uppercase(),
                        color = Color(0xFFA855F7),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(tools, key = { it.id }) { tool ->
                    ToolCardItem(
                        tool = tool,
                        onExecute = {
                            when (tool.id) {
                                "device_flashlight" -> viewModel.toolExecutor.execute("device_flashlight", mapOf("state" to "toggle"))
                                "device_volume" -> viewModel.toolExecutor.execute("device_volume", mapOf("action" to "up"))
                                "device_settings" -> viewModel.toolExecutor.execute("device_settings", mapOf("type" to "wifi"))
                                "apps_open" -> viewModel.toolExecutor.execute("apps_open", mapOf("target" to "camera"))
                                "media_control" -> viewModel.toolExecutor.execute("media_control", mapOf("action" to "play_pause"))
                                "web_search" -> viewModel.toolExecutor.execute("web_search", mapOf("query" to "Latest AI advancements"))
                                "youtube_search" -> viewModel.toolExecutor.execute("youtube_search", mapOf("query" to "Android Compose tutorials"))
                                "maps_navigation" -> viewModel.toolExecutor.execute("maps_navigation", mapOf("destination" to "Coffee"))
                                "timer_set" -> viewModel.toolExecutor.execute("timer_set", mapOf("minutes" to "5", "label" to "MYRAA Timer"))
                                "alarm_set" -> viewModel.toolExecutor.execute("alarm_set", mapOf("hour" to "8", "minute" to "0", "message" to "Wake up"))
                                "screen_vision" -> viewModel.toggleScreenVision()
                                else -> viewModel.processUserPrompt("Run ${tool.name}", isVoice = false)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolCardItem(
    tool: ToolDefinition,
    onExecute: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2600F2FE), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getToolIcon(tool.id),
                    contentDescription = tool.name,
                    tint = CyanNeon,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (tool.requiresConfirmation) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarningAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "REQUIRES CONFIRMATION",
                                color = WarningAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onExecute,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = CyanNeon),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("tool_run_${tool.id}")
            ) {
                Text("RUN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun getToolIcon(id: String): ImageVector {
    return when {
        id.contains("flashlight") -> Icons.Default.FlashlightOn
        id.contains("volume") -> Icons.Default.VolumeUp
        id.contains("settings") -> Icons.Default.Settings
        id.contains("apps") -> Icons.Default.Apps
        id.contains("media") -> Icons.Default.PlayArrow
        id.contains("web") -> Icons.Default.Search
        id.contains("youtube") -> Icons.Default.VideoLibrary
        id.contains("maps") -> Icons.Default.Map
        id.contains("timer") -> Icons.Default.Timer
        id.contains("alarm") -> Icons.Default.Alarm
        id.contains("call") -> Icons.Default.Call
        id.contains("sms") || id.contains("whatsapp") -> Icons.Default.Chat
        id.contains("screen") -> Icons.Default.Visibility
        else -> Icons.Default.Settings
    }
}
