package com.example.voiceapp3.handlers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings
import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.ModelEnum

class OpenAppHandler(private val context: Context) : IntentHandler {
    private val TAG: String? = "OpenAppHandler"
    private val musicIntent: Intent = Intent().apply {
        setClassName(
            "ru.yandex.music",
            "ru.yandex.music.main.MainScreenActivity")
    }
    private val naviIntent: Intent = Intent().apply {
        setClassName(
            "ru.yandex.yandexnavi",
            "ru.yandex.yandexnavi.core.NavigatorActivity")
    }

    private fun getEngineerIntent() : Intent {
        if (CarModel.getCarModel() == ModelEnum.E5) {
            return Intent()
        }

        try {
            Settings.Global.putString(
                context.contentResolver,
                "persist.switch.usbmode",
                "true"
            )
        } catch (e: SecurityException) {
            Log.i(TAG, "Error setting usbmode to true: ${e.toString()}")
        }

        return Intent().apply {
            component = ComponentName(
                "com.geely.engineermode",
                "com.geely.engineermode.MainActivity"
            )
        }
    }

    private val customAppMap: Map<String, Intent> = mapOf(
        "инженер" to getEngineerIntent(),
        "музыка" to musicIntent,
        "музыку" to musicIntent,
        "параметры" to Intent(Settings.ACTION_SETTINGS),
        "карта" to naviIntent,
        "маршрут" to naviIntent,
        "нави" to naviIntent,
        "навигатор" to naviIntent,
        "навигация" to naviIntent,
    )

    override fun canHandle(intent: String): Boolean = intent == "open_app"

    override fun handle(prediction: PredictionResult): Boolean {
        val commandText = prediction.normalizedText.lowercase()

        // Find first matching intent
        customAppMap.entries.firstOrNull { (key, _) ->
            commandText.contains(key)
        }?.let { (_, intent) ->
            return try {
                if (intent.component == null || intent.action == null) {
                    return false
                }
                intent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch activity: ${e.message}")
                false
            }
        }

        // Fall back to system search if no custom match found
        return launchAppBySystemSearch(commandText)
    }

    private fun launchAppBySystemSearch(text: String): Boolean {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Get all launchable apps once
        val apps = pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA)

        // Split text into words and filter out small words
        val words = text.split("\\s+".toRegex())
            .filter { word ->
                // Skip words shorter than 3 characters (adjust threshold as needed)
                word.length >= 3
            }
            .sortedByDescending { it.length } // Process longer words first

        // First pass: Try exact matches (case insensitive)
        for (word in words) {
            val matchingApp = apps.firstOrNull { app ->
                app.loadLabel(pm).toString().lowercase() == word.lowercase()
            }

            if (matchingApp != null) {
                return tryLaunchApp(matchingApp)
            }
        }

        // Second pass: Try contains matches with minimum length threshold
        for (word in words) {
            val matchingApp = apps.firstOrNull { app ->
                val appLabel = app.loadLabel(pm).toString().lowercase()
                // Only match if the word is a significant part of the app name
                appLabel.contains(word.lowercase()) &&
                        (word.length >= 4 || appLabel.split(" ").any { it == word.lowercase() })
            }

            if (matchingApp != null) {
                return tryLaunchApp(matchingApp)
            }
        }

        // Third pass: Try fuzzy matching for longer words
        if (words.any { it.length >= 5 }) {
            for (word in words.filter { it.length >= 5 }) {
                val matchingApp = apps.firstOrNull { app ->
                    val appLabel = app.loadLabel(pm).toString().lowercase()
                    // Simple fuzzy match - at least 70% of characters match in order
                    appLabel.containsSequence(word.lowercase(), minMatchRatio = 0.7)
                }

                if (matchingApp != null) {
                    return tryLaunchApp(matchingApp)
                }
            }
        }

        return false
    }

    private fun tryLaunchApp(appInfo: ResolveInfo): Boolean {
        return try {
            val activityInfo = appInfo.activityInfo
            context.startActivity(Intent().apply {
                component = ComponentName(activityInfo.packageName, activityInfo.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch activity: ${e.message}")
            false
        }
    }

    // Extension function for simple fuzzy matching
    private fun String.containsSequence(other: String, minMatchRatio: Double = 0.7): Boolean {
        if (other.isEmpty()) return true
        if (this.length < other.length) return false

        var matches = 0
        var otherIndex = 0

        for (c in this) {
            if (otherIndex < other.length && c == other[otherIndex]) {
                matches++
                otherIndex++
            }
        }

        return matches.toDouble() / other.length.toDouble() >= minMatchRatio
    }

    override fun extractCommonEntities(prediction: PredictionResult): CommandParams {
        return CommandParams(null, null, null)
    }
}