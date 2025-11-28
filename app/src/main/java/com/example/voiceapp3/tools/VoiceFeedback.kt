package com.example.voiceapp3.tools

import android.util.Log
import com.example.voiceapp3.car.CarAudioPlayer

class VoiceFeedback(private val carAudioPlayer: CarAudioPlayer) {
    private val TAG = "VoiceFeedback"

    private val successMessages = mapOf(
        "ac_control" to "Климат настроен",
        "change_ac_temp" to "Температура установлена",
        "change_fan_speed" to "Обдув настроен",
        "window_control" to "Готово",
        "trunk_control" to "Багажник",
        "seat_massage" to "Массаж включён",
        "seat_ventilation" to "Вентиляция включена",
        "seat_heat" to "Подогрев включён",
        "light_control" to "Свет",
        "exterior_light_control" to "Фары",
        "screen_brightness" to "Яркость установлена",
        "drive_mode" to "Режим изменён",
        "fuel_door_open" to "Лючок открывается",
        "fuel_charge" to "Зарядка",
        "open_app" to null,
        "media_control" to null,
        "play_text" to null,
        "play_sound" to null
    )

    private val failureMessages = mapOf(
        "trunk_control" to "Багажник недоступен на ходу",
        "fuel_charge" to "Зарядка недоступна на ходу",
        "default" to "Не удалось выполнить"
    )

    var isEnabled: Boolean = true

    fun confirm(intent: String, success: Boolean) {
        if (!isEnabled) return

        val message = if (success) {
            successMessages[intent]
        } else {
            failureMessages[intent] ?: failureMessages["default"]
        }

        if (message != null) {
            try {
                carAudioPlayer.playText(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play voice feedback: ${e.message}")
            }
        }
    }

    fun speak(text: String) {
        if (!isEnabled) return

        try {
            carAudioPlayer.playText(text)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak: ${e.message}")
        }
    }
}

