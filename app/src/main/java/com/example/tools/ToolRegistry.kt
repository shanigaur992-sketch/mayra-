package com.example.tools

object ToolRegistry {

    val tools: List<ToolDefinition> = listOf(
        ToolDefinition(
            id = "device_flashlight",
            category = ToolCategory.DEVICE,
            name = "Flashlight",
            description = "Turns the camera flashlight/torch on or off or toggles state.",
            parameters = listOf(
                ToolParameter("state", "string", "State to set: 'on', 'off', or 'toggle'", required = false)
            )
        ),
        ToolDefinition(
            id = "device_volume",
            category = ToolCategory.DEVICE,
            name = "Volume Control",
            description = "Adjusts device media volume up, down, mute, or unmute.",
            parameters = listOf(
                ToolParameter("action", "string", "One of: 'up', 'down', 'mute', 'unmute'", required = true)
            )
        ),
        ToolDefinition(
            id = "device_settings",
            category = ToolCategory.DEVICE,
            name = "System Settings",
            description = "Opens specific device settings screens such as Wi-Fi, Bluetooth, Battery, Display, Sound, Apps.",
            parameters = listOf(
                ToolParameter("type", "string", "Type of settings: 'wifi', 'bluetooth', 'battery', 'display', 'sound', 'accessibility', 'hotspot', 'apps'", required = true)
            )
        ),
        ToolDefinition(
            id = "apps_open",
            category = ToolCategory.DEVICE,
            name = "App Launcher",
            description = "Launches an application such as Camera, Gallery, or installed package.",
            parameters = listOf(
                ToolParameter("target", "string", "'camera', 'gallery', or package name", required = true)
            )
        ),
        ToolDefinition(
            id = "media_control",
            category = ToolCategory.MEDIA,
            name = "Media Playback",
            description = "Controls media playback on the device.",
            parameters = listOf(
                ToolParameter("action", "string", "'play', 'pause', 'next', 'previous'", required = true)
            )
        ),
        ToolDefinition(
            id = "web_search",
            category = ToolCategory.WEB,
            name = "Web Search",
            description = "Performs an internet search for current facts, news, and queries.",
            parameters = listOf(
                ToolParameter("query", "string", "Search keywords or question", required = true)
            )
        ),
        ToolDefinition(
            id = "youtube_search",
            category = ToolCategory.WEB,
            name = "YouTube Search",
            description = "Searches YouTube videos, music, tutorials, and channels.",
            parameters = listOf(
                ToolParameter("query", "string", "Keywords or video topic to search", required = true)
            )
        ),
        ToolDefinition(
            id = "maps_navigation",
            category = ToolCategory.WEB,
            name = "Maps Navigation",
            description = "Searches locations or starts turn-by-turn Google Maps navigation.",
            parameters = listOf(
                ToolParameter("destination", "string", "Destination name or address", required = true)
            )
        ),
        ToolDefinition(
            id = "timer_set",
            category = ToolCategory.PRODUCTIVITY,
            name = "Set Timer",
            description = "Sets a countdown timer on the Android clock.",
            parameters = listOf(
                ToolParameter("minutes", "integer", "Number of minutes", required = false),
                ToolParameter("seconds", "integer", "Number of seconds", required = false),
                ToolParameter("label", "string", "Timer title or label", required = false)
            )
        ),
        ToolDefinition(
            id = "alarm_set",
            category = ToolCategory.PRODUCTIVITY,
            name = "Set Alarm",
            description = "Sets an alarm clock on the Android system.",
            parameters = listOf(
                ToolParameter("hour", "integer", "Hour (0-23)", required = true),
                ToolParameter("minute", "integer", "Minute (0-59)", required = true),
                ToolParameter("message", "string", "Alarm label or note", required = false)
            )
        ),
        ToolDefinition(
            id = "calendar_event",
            category = ToolCategory.PRODUCTIVITY,
            name = "Calendar Event",
            description = "Creates a calendar event or reminder.",
            parameters = listOf(
                ToolParameter("title", "string", "Event title", required = true),
                ToolParameter("description", "string", "Event description or location", required = false)
            )
        ),
        ToolDefinition(
            id = "communication_call",
            category = ToolCategory.COMMUNICATION,
            name = "Phone Call",
            description = "Dials a contact or phone number. Requires user confirmation before initiation.",
            parameters = listOf(
                ToolParameter("phone", "string", "Phone number or contact name", required = true)
            ),
            requiresConfirmation = true,
            confirmationTextBuilder = { params -> "Call ${params["phone"]}?" }
        ),
        ToolDefinition(
            id = "communication_sms",
            category = ToolCategory.COMMUNICATION,
            name = "Send SMS",
            description = "Prepares and sends an SMS text message. Requires user confirmation.",
            parameters = listOf(
                ToolParameter("recipient", "string", "Recipient phone number", required = true),
                ToolParameter("message", "string", "Message body text", required = true)
            ),
            requiresConfirmation = true,
            confirmationTextBuilder = { params -> "Send SMS to ${params["recipient"]}: \"${params["message"]}\"?" }
        ),
        ToolDefinition(
            id = "communication_whatsapp",
            category = ToolCategory.COMMUNICATION,
            name = "WhatsApp Message",
            description = "Prepares a WhatsApp message. Requires user confirmation.",
            parameters = listOf(
                ToolParameter("recipient", "string", "Phone number (optional)", required = false),
                ToolParameter("message", "string", "Message text", required = true)
            ),
            requiresConfirmation = true,
            confirmationTextBuilder = { params -> "Send WhatsApp message: \"${params["message"]}\"?" }
        ),
        ToolDefinition(
            id = "screen_vision",
            category = ToolCategory.VISION,
            name = "Screen Vision",
            description = "Analyzes what is currently on the screen with explicit user activation.",
            parameters = emptyList()
        )
    )

    fun getToolById(id: String): ToolDefinition? = tools.find { it.id == id }

    fun getToolsByCategory(category: ToolCategory): List<ToolDefinition> =
        tools.filter { it.category == category }
}
