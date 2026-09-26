package com.example.tools

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true
)

data class ToolDefinition(
    val id: String,
    val category: ToolCategory,
    val name: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList(),
    val requiredPermission: String? = null,
    val requiresConfirmation: Boolean = false,
    val confirmationTextBuilder: ((Map<String, String>) -> String)? = null
)

enum class ToolCategory(val title: String) {
    DEVICE("Device Control"),
    MEDIA("Media"),
    COMMUNICATION("Communication"),
    PRODUCTIVITY("Productivity"),
    WEB("Web & Search"),
    VISION("Vision")
}

data class ToolExecutionResult(
    val success: Boolean,
    val toolId: String,
    val message: String,
    val requiresConfirmation: Boolean = false,
    val pendingConfirmationData: PendingConfirmationAction? = null
)

data class PendingConfirmationAction(
    val toolId: String,
    val prompt: String,
    val parameters: Map<String, String>,
    val actionType: String
)
