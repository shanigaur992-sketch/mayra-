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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MemoryCategory
import com.example.data.MemoryEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidBlack

@Composable
fun MemoryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf<MemoryCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredMemories = if (selectedCategoryFilter != null) {
        memories.filter { it.category == selectedCategoryFilter?.name }
    } else {
        memories
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MYRAA Memory Vault",
                        color = CyanNeon,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explicit user facts & assistant behaviors",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (memories.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllMemories() },
                        modifier = Modifier.testTag("clear_all_memories")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear all",
                            tint = ErrorRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All (${memories.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon,
                            selectedLabelColor = VoidBlack,
                            containerColor = SurfaceVariantDark,
                            labelColor = TextSecondary
                        )
                    )
                }
                items(MemoryCategory.values()) { cat ->
                    val count = memories.count { it.category == cat.name }
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat },
                        label = { Text("${cat.displayName} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon,
                            selectedLabelColor = VoidBlack,
                            containerColor = SurfaceVariantDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Memory Items List
            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No stored memories in this category",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add one below to personalize MYRAA's intelligence",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onToggle = { enabled -> viewModel.toggleMemory(memory.id, enabled) },
                            onDelete = { viewModel.deleteMemory(memory.id) }
                        )
                    }
                }
            }
        }

        // Add Memory FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = CyanNeon,
            contentColor = VoidBlack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_memory_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Memory")
        }

        // Add Memory Dialog
        if (showAddDialog) {
            AddMemoryDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { cat, key, value ->
                    viewModel.addMemory(cat, key, value)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MemoryCard(
    memory: MemoryEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (memory.isEnabled) Color(0x3300F2FE) else Color(0x1A94A3B8), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memory.category.uppercase(),
                    color = Color(0xFFA855F7),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = memory.key,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = memory.value,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Switch(
                checked = memory.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = VoidBlack,
                    checkedTrackColor = CyanNeon,
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = SurfaceVariantDark
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onAdd: (MemoryCategory, String, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(MemoryCategory.PREFERENCE) }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Add Persistent Memory", color = CyanNeon, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Category", color = TextSecondary, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MemoryCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = VoidBlack
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Topic / Title (e.g., Favorite Coffee)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Details (e.g., Oat milk cappuccino with honey)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        onAdd(selectedCategory, key, value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = VoidBlack)
            ) {
                Text("SAVE MEMORY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
