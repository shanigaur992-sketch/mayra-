package com.example.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent

class AndroidToolExecutor(private val context: Context) {

    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    private var isTorchOn = false

    fun execute(toolId: String, params: Map<String, String>, isConfirmed: Boolean = false): ToolExecutionResult {
        return try {
            when (toolId) {
                "device_flashlight" -> toggleFlashlight(params["state"] ?: "toggle")
                "device_volume" -> adjustVolume(params["action"] ?: "up")
                "device_settings" -> openSettings(params["type"] ?: "main")
                "apps_open" -> openApp(params["target"] ?: "camera")
                "media_control" -> controlMedia(params["action"] ?: "play_pause")
                "web_search" -> searchWeb(params["query"] ?: "")
                "youtube_search" -> searchYouTube(params["query"] ?: "")
                "maps_navigation" -> openNavigation(params["destination"] ?: "")
                "timer_set" -> setTimer(
                    seconds = params["seconds"]?.toIntOrNull() ?: ((params["minutes"]?.toIntOrNull() ?: 1) * 60),
                    label = params["label"] ?: "MYRAA Timer"
                )
                "alarm_set" -> setAlarm(
                    hour = params["hour"]?.toIntOrNull() ?: 8,
                    minute = params["minute"]?.toIntOrNull() ?: 0,
                    message = params["message"] ?: "MYRAA Alarm"
                )
                "calendar_event" -> createCalendarEvent(
                    title = params["title"] ?: "Meeting",
                    description = params["description"] ?: "Created by MYRAA"
                )
                "communication_call" -> {
                    val phone = params["phone"] ?: ""
                    if (!isConfirmed) {
                        ToolExecutionResult(
                            success = false,
                            toolId = toolId,
                            message = "Call confirmation required",
                            requiresConfirmation = true,
                            pendingConfirmationData = PendingConfirmationAction(
                                toolId = toolId,
                                prompt = "Are you sure you want to call $phone?",
                                parameters = params,
                                actionType = "Call"
                            )
                        )
                    } else {
                        makeCall(phone)
                    }
                }
                "communication_sms" -> {
                    val recipient = params["recipient"] ?: ""
                    val message = params["message"] ?: ""
                    if (!isConfirmed) {
                        ToolExecutionResult(
                            success = false,
                            toolId = toolId,
                            message = "SMS confirmation required",
                            requiresConfirmation = true,
                            pendingConfirmationData = PendingConfirmationAction(
                                toolId = toolId,
                                prompt = "Send SMS to $recipient:\n\"$message\"?",
                                parameters = params,
                                actionType = "SMS"
                            )
                        )
                    } else {
                        sendSms(recipient, message)
                    }
                }
                "communication_whatsapp" -> {
                    val recipient = params["recipient"] ?: ""
                    val message = params["message"] ?: ""
                    if (!isConfirmed) {
                        ToolExecutionResult(
                            success = false,
                            toolId = toolId,
                            message = "WhatsApp confirmation required",
                            requiresConfirmation = true,
                            pendingConfirmationData = PendingConfirmationAction(
                                toolId = toolId,
                                prompt = "Open WhatsApp message for $recipient:\n\"$message\"?",
                                parameters = params,
                                actionType = "WhatsApp"
                            )
                        )
                    } else {
                        sendWhatsApp(recipient, message)
                    }
                }
                "screen_vision" -> ToolExecutionResult(
                    success = true,
                    toolId = toolId,
                    message = "Screen Vision is ready. Point to the screen or activate the capture overlay."
                )
                else -> ToolExecutionResult(
                    success = false,
                    toolId = toolId,
                    message = "Tool '$toolId' is not recognized in the registry."
                )
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                toolId = toolId,
                message = "Execution failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun toggleFlashlight(state: String): ToolExecutionResult {
        val manager = cameraManager ?: return ToolExecutionResult(false, "device_flashlight", "Camera service not available")
        val cameraId = try { manager.cameraIdList.firstOrNull() } catch (_: CameraAccessException) { null }
            ?: return ToolExecutionResult(false, "device_flashlight", "No camera with flash available on device")

        val turnOn = when (state.lowercase()) {
            "on", "enable", "true" -> true
            "off", "disable", "false" -> false
            else -> !isTorchOn
        }

        return try {
            manager.setTorchMode(cameraId, turnOn)
            isTorchOn = turnOn
            ToolExecutionResult(true, "device_flashlight", "Flashlight turned ${if (turnOn) "ON" else "OFF"}")
        } catch (e: Exception) {
            ToolExecutionResult(false, "device_flashlight", "Could not toggle torch: ${e.message}")
        }
    }

    private fun adjustVolume(action: String): ToolExecutionResult {
        val am = audioManager ?: return ToolExecutionResult(false, "device_volume", "Audio service unavailable")
        val direction = when (action.lowercase()) {
            "up", "increase", "raise" -> AudioManager.ADJUST_RAISE
            "down", "decrease", "lower" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> AudioManager.ADJUST_SAME
        }
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return ToolExecutionResult(true, "device_volume", "Volume adjusted ($action)")
    }

    private fun openSettings(type: String): ToolExecutionResult {
        val action = when (type.lowercase()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "hotspot" -> Settings.ACTION_WIRELESS_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
        return ToolExecutionResult(true, "device_settings", "Opened $type settings")
    }

    private fun openApp(target: String): ToolExecutionResult {
        when (target.lowercase()) {
            "camera" -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
                return ToolExecutionResult(true, "apps_open", "Opened Camera")
            }
            "gallery" -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return ToolExecutionResult(true, "apps_open", "Opened Gallery")
            }
            else -> {
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(target)
                return if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(launchIntent)
                    ToolExecutionResult(true, "apps_open", "Opened app: $target")
                } else {
                    ToolExecutionResult(false, "apps_open", "Application package '$target' not found")
                }
            }
        }
    }

    private fun controlMedia(action: String): ToolExecutionResult {
        val am = audioManager ?: return ToolExecutionResult(false, "media_control", "Audio service unavailable")
        val keyCode = when (action.lowercase()) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        am.dispatchMediaKeyEvent(eventDown)
        am.dispatchMediaKeyEvent(eventUp)
        return ToolExecutionResult(true, "media_control", "Media command dispatched: $action")
    }

    private fun searchWeb(query: String): ToolExecutionResult {
        if (query.isBlank()) return ToolExecutionResult(false, "web_search", "Search query is empty")
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(true, "web_search", "Initiated web search for: $query")
    }

    private fun searchYouTube(query: String): ToolExecutionResult {
        if (query.isBlank()) return ToolExecutionResult(false, "youtube_search", "YouTube query is empty")
        val intent = try {
            Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (_: Exception) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
        return ToolExecutionResult(true, "youtube_search", "Searching YouTube for: $query")
    }

    private fun openNavigation(destination: String): ToolExecutionResult {
        if (destination.isBlank()) return ToolExecutionResult(false, "maps_navigation", "Destination empty")
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(mapIntent)
            ToolExecutionResult(true, "maps_navigation", "Starting navigation to $destination")
        } catch (_: Exception) {
            val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webMapIntent)
            ToolExecutionResult(true, "maps_navigation", "Opening Maps directions for $destination")
        }
    }

    private fun setTimer(seconds: Int, label: String): ToolExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ToolExecutionResult(true, "timer_set", "Timer set for ${seconds / 60} min ${seconds % 60} sec: $label")
        } catch (e: Exception) {
            ToolExecutionResult(false, "timer_set", "Unable to launch clock timer: ${e.message}")
        }
    }

    private fun setAlarm(hour: Int, minute: Int, message: String): ToolExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            ToolExecutionResult(true, "alarm_set", "Alarm set for $timeStr: $message")
        } catch (e: Exception) {
            ToolExecutionResult(false, "alarm_set", "Clock alarm could not be set: ${e.message}")
        }
    }

    private fun createCalendarEvent(title: String, description: String): ToolExecutionResult {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ToolExecutionResult(true, "calendar_event", "Calendar event created: $title")
        } catch (e: Exception) {
            ToolExecutionResult(false, "calendar_event", "Calendar application not found: ${e.message}")
        }
    }

    private fun makeCall(phoneNumber: String): ToolExecutionResult {
        val cleanNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        if (cleanNumber.isBlank()) return ToolExecutionResult(false, "communication_call", "Invalid phone number")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolExecutionResult(true, "communication_call", "Dialing $cleanNumber")
    }

    private fun sendSms(recipient: String, message: String): ToolExecutionResult {
        val uri = Uri.parse("smsto:${recipient.trim()}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ToolExecutionResult(true, "communication_sms", "SMS prepared for $recipient: \"$message\"")
        } catch (e: Exception) {
            ToolExecutionResult(false, "communication_sms", "Messaging app not available: ${e.message}")
        }
    }

    private fun sendWhatsApp(recipient: String, message: String): ToolExecutionResult {
        val cleanPhone = recipient.filter { it.isDigit() }
        val url = if (cleanPhone.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
        } else {
            "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            ToolExecutionResult(true, "communication_whatsapp", "WhatsApp opened with prepared message")
        } catch (_: Exception) {
            // Fallback to web browser
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ToolExecutionResult(true, "communication_whatsapp", "WhatsApp web link opened")
        }
    }
}
