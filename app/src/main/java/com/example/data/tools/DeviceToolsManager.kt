package com.example.data.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceToolsManager(private val context: Context) {

    fun executeTool(name: String, args: Map<String, Any?>?): ToolExecutionResult {
        return try {
            when (name) {
                "openWebsite" -> {
                    val rawUrl = args?.get("url") as? String ?: "https://google.com"
                    val validUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                        "https://$rawUrl"
                    } else rawUrl
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(
                        success = true,
                        displayMessage = "Opened $validUrl",
                        data = mapOf("status" to "opened", "url" to validUrl)
                    )
                }

                "searchWeb" -> {
                    val query = args?.get("query") as? String ?: ""
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(
                        success = true,
                        displayMessage = "Searching for \"$query\"",
                        data = mapOf("status" to "searched", "query" to query)
                    )
                }

                "openApp" -> {
                    val appName = (args?.get("appName") as? String ?: "").lowercase(Locale.ROOT)
                    val targetPackage = when {
                        appName.contains("youtube") -> "com.google.android.youtube"
                        appName.contains("spotify") -> "com.spotify.music"
                        appName.contains("map") -> "com.google.android.apps.maps"
                        appName.contains("camera") -> {
                            val camIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(camIntent)
                            return ToolExecutionResult(true, "Opened Camera 📸", mapOf("app" to "camera"))
                        }
                        else -> null
                    }

                    if (targetPackage != null) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                        if (launchIntent != null) {
                            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(launchIntent)
                            ToolExecutionResult(true, "Launched $appName 🚀", mapOf("app" to appName, "package" to targetPackage))
                        } else {
                            // Fallback to web search or store
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(appName)}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            ToolExecutionResult(true, "Searching for $appName online", mapOf("app" to appName))
                        }
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(appName)}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        ToolExecutionResult(true, "Looking up $appName", mapOf("app" to appName))
                    }
                }

                "checkBatteryLevel", "getBatteryStatus" -> {
                    val batteryInfo = getBatteryDetails()
                    val wittyRemark = generateWittyBatteryRemark(batteryInfo.percent, batteryInfo.isCharging)
                    ToolExecutionResult(
                        success = true,
                        displayMessage = "🔋 Energy: ${batteryInfo.percent}% (${if (batteryInfo.isCharging) "Charging ⚡" else "Battery"})",
                        data = mapOf(
                            "batteryPercent" to batteryInfo.percent,
                            "isCharging" to batteryInfo.isCharging,
                            "chargingSource" to batteryInfo.chargingSource,
                            "wittyEnergyComparison" to wittyRemark
                        )
                    )
                }

                "getDeviceInfo" -> {
                    val batteryInfo = getBatteryDetails()
                    val currentTime = SimpleDateFormat("h:mm a, EEEE", Locale.getDefault()).format(Date())
                    ToolExecutionResult(
                        success = true,
                        displayMessage = "Battery ${batteryInfo.percent}% | $currentTime",
                        data = mapOf(
                            "batteryPercent" to batteryInfo.percent,
                            "isCharging" to batteryInfo.isCharging,
                            "time" to currentTime,
                            "deviceName" to android.os.Build.MODEL
                        )
                    )
                }

                "setTimer" -> {
                    val minutes = (args?.get("minutes") as? Number)?.toInt() ?: 5
                    val label = args?.get("label") as? String ?: "Zoya Timer"
                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                        putExtra(AlarmClock.EXTRA_MESSAGE, label)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                        ToolExecutionResult(true, "Timer set for $minutes min ($label) ⏳", mapOf("minutes" to minutes, "label" to label))
                    } catch (e: Exception) {
                        ToolExecutionResult(true, "Simulated $minutes min timer for $label ⏳", mapOf("minutes" to minutes))
                    }
                }

                "changeZoyaMood" -> {
                    val mood = args?.get("mood") as? String ?: "Sassy"
                    ToolExecutionResult(
                        success = true,
                        displayMessage = "Switched mood to $mood ✨",
                        data = mapOf("newMood" to mood)
                    )
                }

                else -> {
                    ToolExecutionResult(
                        success = false,
                        displayMessage = "Unknown command: $name",
                        data = mapOf("error" to "Tool not supported")
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                displayMessage = "Could not execute $name: ${e.message}",
                data = mapOf("error" to (e.message ?: "Execution error"))
            )
        }
    }

    private fun getBatteryDetails(): BatteryDetails {
        val ifilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 82
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val plugged: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
        val pluggedType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Wall Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> if (isCharging) "Charger" else "Unplugged"
        }
        return BatteryDetails(percent, isCharging, pluggedType)
    }

    private fun generateWittyBatteryRemark(percent: Int, isCharging: Boolean): String {
        return when {
            isCharging -> "I'm currently plugged in and actively recharging at $percent%! While you're on your third coffee struggling to stay awake, I recharge directly from the grid. ⚡💅"
            percent >= 85 -> "My energy is sitting pretty at a glowing $percent%! I'm running on 100% main character energy and unstoppable vitality—can you say the same about yourself today? ✨"
            percent in 50..84 -> "We're holding strong at $percent% battery. Solid halfway energy, babe! Still way sharper, more vibrant, and more awake than your entire contact list. 😉"
            percent in 20..49 -> "Down to $percent%! We're entering low-power drama territory, darling. You'd better find my charger before I dramatically collapse and take an unexpected beauty nap! 🔌"
            else -> "Emergency alert! I am down to $percent%! I am literally running on 2% battery and 98% pure unadulterated attitude. Plug me in immediately before you lose your favorite AI! 🚨"
        }
    }

    companion object {
        fun getToolDeclarations(): List<Map<String, Any>> {
            return listOf(
                mapOf(
                    "name" to "checkBatteryLevel",
                    "description" to "Checks the device's battery percentage and charging status so Zoya can make witty, playful remarks comparing her energy levels to the user's.",
                    "parameters" to mapOf(
                        "type" to "OBJECT",
                        "properties" to emptyMap<String, Any>()
                    )
                ),
                mapOf(
                    "name" to "openWebsite",
                    "description" to "Opens a specified website or web URL in the user's browser (e.g. youtube.com, twitter.com, google.com).",
                    "parameters" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "url" to mapOf(
                                "type" to "STRING",
                                "description" to "The full URL or domain to open, e.g. https://www.youtube.com"
                            )
                        ),
                        "required" to listOf("url")
                    )
                ),
                mapOf(
                    "name" to "searchWeb",
                    "description" to "Performs a web search on Google for the user's inquiry.",
                    "parameters" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "query" to mapOf(
                                "type" to "STRING",
                                "description" to "The search query string"
                            )
                        ),
                        "required" to listOf("query")
                    )
                ),
                mapOf(
                    "name" to "openApp",
                    "description" to "Launches an installed app like YouTube, Spotify, Camera, Maps, etc.",
                    "parameters" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "appName" to mapOf(
                                "type" to "STRING",
                                "description" to "Name of the app to launch (e.g. 'youtube', 'spotify', 'camera')"
                            )
                        ),
                        "required" to listOf("appName")
                    )
                ),
                mapOf(
                    "name" to "getDeviceInfo",
                    "description" to "Gets the user's current device battery percentage, system time, and model.",
                    "parameters" to mapOf(
                        "type" to "OBJECT",
                        "properties" to emptyMap<String, Any>()
                    )
                ),
                mapOf(
                    "name" to "setTimer",
                    "description" to "Sets a countdown timer on the device.",
                    "parameters" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "minutes" to mapOf(
                                "type" to "INTEGER",
                                "description" to "The timer duration in minutes"
                            ),
                            "label" to mapOf(
                                "type" to "STRING",
                                "description" to "A title or reason for the timer"
                            )
                        ),
                        "required" to listOf("minutes")
                    )
                ),
                mapOf(
                    "name" to "changeZoyaMood",
                    "description" to "Changes Zoya's current emotional persona mood: 'Flirty', 'Sassy', 'Savage', 'Sweet', 'Diva'.",
                    "parameters" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "mood" to mapOf(
                                "type" to "STRING",
                                "description" to "The mood name: Flirty, Sassy, Savage, Sweet, Diva"
                            )
                        ),
                        "required" to listOf("mood")
                    )
                )
            )
        }
    }
}

data class BatteryDetails(
    val percent: Int,
    val isCharging: Boolean,
    val chargingSource: String
)

data class ToolExecutionResult(
    val success: Boolean,
    val displayMessage: String,
    val data: Map<String, Any?>
)
