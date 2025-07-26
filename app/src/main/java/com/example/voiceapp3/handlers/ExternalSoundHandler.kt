package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.CarAudioPlayer

class ExternalSoundHandler(private val carAudioPlayer: CarAudioPlayer) : IntentHandler {
    private val TAG = "ExternalSoundHandler"

    override fun canHandle(intent: String) = intent == "play_sound"

    override fun handle(prediction: PredictionResult): Boolean {
        val sound = extractSoundName(prediction.normalizedText) ?: return false

        val soundName = when (sound) {
            "кошка" -> "cat.wav"
            "нахуй" -> "nah.mp3"
            else -> return false
        }

        try {
            carAudioPlayer.playWithCustomUsage(soundName)
            return true
        } catch (e: Exception) {
            Log.i(TAG, "Playing external sound failed: $e")
            return false
        }
    }
    private fun extractSoundName(command: String): String? {
        val soundKeywords = listOf("кошка", "нахуй")
        return soundKeywords.firstOrNull { command.contains(it) }
    }
}