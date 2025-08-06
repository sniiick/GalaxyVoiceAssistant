package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
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


class ExternalSpeechHandler(private val carAudioPlayer: CarAudioPlayer) : IntentHandler {
    private val TAG = "ExternalSpeechHandler"
    private val speechPrefixes = listOf("скажи", "сказать", "озвучь", "произнеси")

    override fun canHandle(intent: String) = intent == "play_text"

    override fun handle(prediction: PredictionResult): Boolean {
        try {
            // TODO("To be implemented")
//            val cleanText = removeSpeechPrefix(prediction.text)
//            if (cleanText.isNotBlank()) {
//                carAudioPlayer.playTextAsSpeech(cleanText)
//                return true
//            }
            return false
        } catch (e: Exception) {
            Log.i(TAG, "Playing external sound failed: $e")
            return false
        }
    }

    private fun removeSpeechPrefix(text: String): String {
        val lowerText = text.lowercase()

        for (prefix in speechPrefixes) {
            if (lowerText.startsWith(prefix)) {
                return text.substring(prefix.length).trim()
            }
        }
        return text.trim()
    }
}
