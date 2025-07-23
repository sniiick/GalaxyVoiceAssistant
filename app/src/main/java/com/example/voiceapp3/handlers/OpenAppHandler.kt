package com.example.voiceapp3.handlers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.example.voiceapp3.PredictionResult

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

    private val customAppMap: Map<String, Intent> = mapOf(
        "инженер" to Intent().apply {
            component = ComponentName(
                "com.geely.engineermode",
                "com.geely.engineermode.MainActivity"
            )
        },
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

        // Split text into words and check each one
        val words = text.split("\\s+".toRegex())

        for (word in words) {
            val matchingApp = apps.firstOrNull { app ->
                app.loadLabel(pm).toString().lowercase().contains(word)
            }

            if (matchingApp != null) {
                return try {
                    val appInfo = matchingApp.activityInfo
                    context.startActivity(Intent().apply {
                        component = ComponentName(appInfo.packageName, appInfo.name)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch activity: ${e.message}")
                    false
                }
            }
        }

        return false
    }

    override fun extractCommonEntities(prediction: PredictionResult): CommandParams {
        return CommandParams(null, null, null)
    }
}