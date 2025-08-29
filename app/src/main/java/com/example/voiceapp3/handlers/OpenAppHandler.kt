package com.example.voiceapp3.handlers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.tools.CarModel
import ecarx.xsf.mediacenter.MusicClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class OpenAppHandler(private val context: Context) : IntentHandler {
    private val TAG: String? = "OpenAppHandler"
    private val yaMusicIntent: Intent = Intent().apply {
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
        if (CarModel.isE5) {
            return Intent()
        }

        try {
            Settings.Global.putString(
                context.contentResolver,
                "persist.switch.usbmode",
                "true"
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Error setting usbmode to true: ${e.toString()}")
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
        "музык" to yaMusicIntent,
        "параметры" to Intent(Settings.ACTION_SETTINGS),
        "карта" to naviIntent, "карту" to naviIntent, "карты" to naviIntent, "маршрут" to naviIntent,
        "нави" to naviIntent, "навигатор" to naviIntent, "навигация" to naviIntent,
    )

    override fun canHandle(intent: String): Boolean = intent == "open_app"

    override fun handle(prediction: PredictionResult): Boolean {
        val commandText = prediction.normalizedText.lowercase()
        val params = extractCommonEntities(prediction)


        // Find first matching intent
        customAppMap.entries.firstOrNull { (key, _) ->
            commandText.contains(key)
        }?.let { (_, intent) ->
            return try {
                if (intent.component == null && intent.action == null) {
                    return false
                }
                intent.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

                if (params.action == "unset") {
                    return false
                } else {
                    context.startActivity(intent)
                }

                // music special handle to start/stop playing
                if (intent.component?.packageName == "ru.yandex.music") {
                    val musicClient = MusicClient(context)
                    CoroutineScope(Dispatchers.IO).launch {
                        val mediaController = waitForMediaSessionWithTimeout(10000L)
                        Thread.sleep(500)
                        Log.i(TAG, "State: ${mediaController?.playbackState?.state}")
                        if (mediaController != null) {
                            when (mediaController.playbackState?.state) {
                                PlaybackState.STATE_STOPPED, PlaybackState.STATE_PAUSED -> {
                                    musicClient.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true)
                                    musicClient.sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false)
                                }
                                else -> {
                                }
                            }
                        }
                    }
                }

                true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch activity: ${e.message}")
                false
            }
        }

        // Fall back to system search if no custom match found
        return launchAppBySystemSearch(commandText)
    }

    @SuppressLint("QueryPermissionsNeeded")
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

    private suspend fun waitForMediaSessionWithTimeout(timeoutMs: Long): MediaController? {
        return withTimeoutOrNull(timeoutMs) {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            var mediaController: MediaController? = null

            while (isActive && mediaController == null) {
                // Get active sessions
                val activeSessions = mediaSessionManager.getActiveSessions(null)

                // Look for Yandex Music session
                mediaController = activeSessions.firstOrNull { session ->
                    session.packageName == "ru.yandex.music"
                }

                // If not found, wait a bit before checking again
                if (mediaController == null) {
                    delay(200) // Check every 200ms
                }
            }

            mediaController
        }
    }

}