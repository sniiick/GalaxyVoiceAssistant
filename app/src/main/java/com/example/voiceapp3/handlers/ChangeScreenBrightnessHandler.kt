package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry


class ChangeScreenBrightnessHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "ChangeScreenBrightnessHandler"

    private val brightnessConfig = CarPropertyRegistry.Screen.BRIGHTNESS
    private val autoBrightnessConfig = CarPropertyRegistry.Screen.AUTO_BRIGHTNESS

    private val minBrightness = CarPropertyRegistry.Screen.MIN_BRIGHTNESS
    private val maxBrightness = CarPropertyRegistry.Screen.MAX_BRIGHTNESS
    private val defaultBrightnessChange = 20

    override fun canHandle(intent: String): Boolean = intent == "screen_brightness"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        if (prediction.normalizedText.contains("авто")) {
            return setAutoBrightness()
        }

        return when (params.action) {
            "set" -> handleSetBrightness(params)
            "increase" -> handleChangeBrightness(params)
            "decrease" -> handleChangeBrightness(params)
            "unset" -> handleUnsetBrightness()
            else -> handleSetBrightness(params)
        }
    }

    private fun handleSetBrightness(params: CommandParams): Boolean {
        return if (params.value != null) {
            setBrightness(percentToBrightness(params.value))
        } else {
            false
        }
    }

    private fun handleUnsetBrightness(): Boolean {
        return setBrightness(percentToBrightness(minBrightness))
    }

    private fun handleChangeBrightness(params: CommandParams): Boolean {
        val currentBrightness = vehiclePropertyHelper.getIntProperty(
            brightnessConfig.getPropertyId(),
            brightnessConfig.getAreaId()
        )

        if (currentBrightness == -1) {
            Log.d(TAG, "Current brightness is unknown")
            return false
        }

        val newValue = if (params.value == 1) 100 else params.value

        val newBrightness = when {
            params.action == "increase" && newValue != null -> {
                currentBrightness + percentToBrightnessChange(newValue)
            }
            params.action == "increase" ->
                currentBrightness + defaultBrightnessChange
            params.action == "decrease" && newValue != null -> {
                currentBrightness - percentToBrightnessChange(newValue)
            }
            params.action == "decrease" ->
                currentBrightness - defaultBrightnessChange
            else -> return false
        }
        return setBrightness(newBrightness)
    }

    private fun setAutoBrightness(): Boolean {
        Log.d(TAG, "Setting auto brightness")
        return vehiclePropertyHelper.setBoolProperty(
            autoBrightnessConfig.getPropertyId(),
            autoBrightnessConfig.getAreaId(),
            true
        )
    }

    private fun percentToBrightness(percent: Int): Int {
        return ((percent * maxBrightness) / 100).coerceIn(minBrightness, maxBrightness)
    }

    private fun percentToBrightnessChange(percent: Int): Int {
        return ((percent * maxBrightness) / 100).coerceAtLeast(1)
    }

    fun setBrightness(brightness: Int): Boolean {
        val clampedBrightness = brightness.coerceIn(minBrightness, maxBrightness)

        Log.i(TAG, "Changing brightness to $clampedBrightness")
        return vehiclePropertyHelper.setIntProperty(
            brightnessConfig.getPropertyId(),
            brightnessConfig.getAreaId(),
            clampedBrightness
        )
    }
}