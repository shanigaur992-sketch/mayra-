package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ChatScreen
import com.example.ui.DiagnosticsScreen
import com.example.ui.FirstLaunchScreen
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.MemoryScreen
import com.example.ui.SettingsScreen
import com.example.ui.ToolsScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

enum class Screen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CHAT("Chat", Icons.Default.Chat),
    TOOLS("Tools", Icons.Default.Build),
    MEMORY("Memory", Icons.Default.Psychology),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MyraaApp(viewModel)
            }
        }
    }
}

@Composable
fun MyraaApp(viewModel: MainViewModel) {
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val isScreenVisionActive by viewModel.isScreenVisionActive.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showDiagnostics by remember { mutableStateOf(false) }

    if (isFirstLaunch) {
        FirstLaunchScreen(
            viewModel = viewModel,
            onFinished = {
                currentScreen = Screen.HOME
            }
        )
        return
    }

    // Handle back button on sub-screens
    if (showDiagnostics) {
        BackHandler { showDiagnostics = false }
    } else if (currentScreen != Screen.HOME) {
        BackHandler { currentScreen = Screen.HOME }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VoidBlack,
        bottomBar = {
            if (!showDiagnostics) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    NavigationBar(
                        containerColor = SurfaceGlass,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0x3300F2FE), RoundedCornerShape(24.dp)),
                        tonalElevation = 8.dp
                    ) {
                        Screen.values().forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    showDiagnostics = false
                                    currentScreen = screen
                                },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VoidBlack,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanNeon,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            when {
                showDiagnostics -> {
                    DiagnosticsScreen(
                        viewModel = viewModel,
                        onBack = { showDiagnostics = false }
                    )
                }
                currentScreen == Screen.HOME -> {
                    HomeScreen(viewModel = viewModel)
                }
                currentScreen == Screen.CHAT -> {
                    ChatScreen(viewModel = viewModel)
                }
                currentScreen == Screen.TOOLS -> {
                    ToolsScreen(viewModel = viewModel)
                }
                currentScreen == Screen.MEMORY -> {
                    MemoryScreen(viewModel = viewModel)
                }
                currentScreen == Screen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToDiagnostics = { showDiagnostics = true }
                    )
                }
            }

            // Screen Vision persistent top banner indicator
            AnimatedVisibility(
                visible = isScreenVisionActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color(0xEE0A0E1A),
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(1.dp, CyanNeon, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(CyanNeon)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SCREEN VISION ACTIVE",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Inspecting Display",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
