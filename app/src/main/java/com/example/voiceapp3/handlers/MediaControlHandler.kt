package com.example.voiceapp3.handlers

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import android.view.KeyEvent
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import ecarx.xsf.mediacenter.MusicClient

class MediaControlHandler(private val context: Context) : IntentHandler {
    private val TAG = "MediaControlHandler"

    private val playPatterns = Regex("воспроизв|играй|запусти|продолж|включи музык|play", RegexOption.IGNORE_CASE)
    private val pausePatterns = Regex("пауза|останов|стоп|pause|stop", RegexOption.IGNORE_CASE)
    private val nextPatterns = Regex("следующ|дальше|скип|вперед|next|skip", RegexOption.IGNORE_CASE)
    private val prevPatterns = Regex("предыдущ|назад|верни|previous|back", RegexOption.IGNORE_CASE)
    private val volumeUpPatterns = Regex("громче|прибав.*громкость|увелич.*громкость|louder|volume up", RegexOption.IGNORE_CASE)
    private val volumeDownPatterns = Regex("тише|убав.*громкость|уменьш.*громкость|quieter|volume down", RegexOption.IGNORE_CASE)
    private val mutePatterns = Regex("выключи звук|без звука|mute", RegexOption.IGNORE_CASE)

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val musicClient: MusicClient by lazy {
        MusicClient(context)
    }

    override fun canHandle(intent: String): Boolean = intent == "media_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val text = prediction.normalizedText

        return when {
            playPatterns.containsMatchIn(text) -> handlePlay()
            pausePatterns.containsMatchIn(text) -> handlePause()
            nextPatterns.containsMatchIn(text) -> handleNext()
            prevPatterns.containsMatchIn(text) -> handlePrevious()
            volumeUpPatterns.containsMatchIn(text) -> handleVolumeUp(prediction)
            volumeDownPatterns.containsMatchIn(text) -> handleVolumeDown(prediction)
            mutePatterns.containsMatchIn(text) -> handleMute()
            else -> handlePlayPause()
        }
    }

    private fun handlePlay(): Boolean {
        Log.i(TAG, "Play command")
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    private fun handlePause(): Boolean {
        Log.i(TAG, "Pause command")
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
    }

    private fun handlePlayPause(): Boolean {
        val controller = getActiveMediaController()
        val isPlaying = controller?.playbackState?.state == PlaybackState.STATE_PLAYING

        return if (isPlaying) {
            handlePause()
        } else {
            handlePlay()
        }
    }

    private fun handleNext(): Boolean {
        Log.i(TAG, "Next track command")
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    private fun handlePrevious(): Boolean {
        Log.i(TAG, "Previous track command")
        return sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun handleVolumeUp(prediction: PredictionResult): Boolean {
        val value = prediction.getValue()
        val steps = when {
            value != null && prediction.getUnit() == "percent" -> (value * 15 / 100).coerceAtLeast(1)
            value != null -> value.coerceIn(1, 15)
            else -> 2
        }

        Log.i(TAG, "Volume up by $steps steps")
        repeat(steps) {
            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        }
        return true
    }

    private fun handleVolumeDown(prediction: PredictionResult): Boolean {
        val value = prediction.getValue()
        val steps = when {
            value != null && prediction.getUnit() == "percent" -> (value * 15 / 100).coerceAtLeast(1)
            value != null -> value.coerceIn(1, 15)
            else -> 2
        }

        Log.i(TAG, "Volume down by $steps steps")
        repeat(steps) {
            audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        return true
    }

    private fun handleMute(): Boolean {
        Log.i(TAG, "Mute command")
        audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
        return true
    }

    private fun sendMediaKey(keyCode: Int): Boolean {
        return try {
            musicClient.sendMediaKey(keyCode, true)
            musicClient.sendMediaKey(keyCode, false)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send media key: ${e.message}")
            sendMediaKeyFallback(keyCode)
        }
    }

    private fun sendMediaKeyFallback(keyCode: Int): Boolean {
        return try {
            val controller = getActiveMediaController() ?: return false

            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> controller.transportControls.play()
                KeyEvent.KEYCODE_MEDIA_PAUSE -> controller.transportControls.pause()
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                        controller.transportControls.pause()
                    } else {
                        controller.transportControls.play()
                    }
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> controller.transportControls.skipToNext()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> controller.transportControls.skipToPrevious()
                KeyEvent.KEYCODE_MEDIA_STOP -> controller.transportControls.stop()
                else -> return false
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback media control failed: ${e.message}")
            false
        }
    }

    private fun getActiveMediaController(): MediaController? {
        return try {
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = mediaSessionManager.getActiveSessions(null)

            sessions.firstOrNull { session ->
                session.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: sessions.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get media controller: ${e.message}")
            null
        }
    }
}

