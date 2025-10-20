package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.ModelEnum


class ChangeScreenBrightnessHandler(val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "ChangeScreenBrightnessHandler"

    private var BRIGHTNESS_PROPERTY_ID = 624981307
    private var BRIGHTNESS_AREA_ID = 2

    private var AUTO_BRIGHTNESS_PROPERTY_ID = 555775150
    private var AUTO_BRIGHTNESS_AREA_ID = 0

    private var minBrightness: Int = 0
    private var maxBrightness: Int = 150
    private val defaultBrightnessChange: Int = 20

    override fun canHandle(intent: String): Boolean = intent == "screen_brightness"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        if (CarModel.isCoolray) {
            BRIGHTNESS_PROPERTY_ID = 687997952
        }

        if (prediction.normalizedText.contains("auto")) {
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
        return when {
            params.value != null -> {
                setBrightness(percentToBrightness(params.value))
            }
            else -> false
        }
    }

    private fun handleUnsetBrightness(): Boolean {
        return setBrightness(percentToBrightness(minBrightness))
    }

    private fun handleChangeBrightness(params: CommandParams): Boolean {
        val currentBrightness = vehiclePropertyHelper.getIntProperty(
            BRIGHTNESS_PROPERTY_ID,
            BRIGHTNESS_AREA_ID
        )

        if (currentBrightness == -1) {
            Log.d(TAG, "Current brightness is unknown")
            return false
        }

        var newValue = params.value
        if (params.value == 1) {
            newValue = 100
        }
        val newBrightness = when {
            // Increase cases
            params.action == "increase" && newValue != null -> {
                val brightnessChange = percentToBrightnessChange(newValue)
                currentBrightness + brightnessChange
            }

            params.action == "increase" ->
                currentBrightness + defaultBrightnessChange

            // Decrease cases
            params.action == "decrease" && newValue != null-> {
                val brightnessChange = percentToBrightnessChange(newValue)
                currentBrightness - brightnessChange
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
            AUTO_BRIGHTNESS_PROPERTY_ID,
            AUTO_BRIGHTNESS_AREA_ID,
            true
        )
    }

    private fun percentToBrightness(percent: Int): Int {
        val brightness = (percent * maxBrightness) / 100
        return brightness.coerceIn(minBrightness, maxBrightness)
    }

    private fun percentToBrightnessChange(percent: Int): Int {
        val change = (percent * maxBrightness) / 100
        return change.coerceAtLeast(1) // Ensure at least 1 unit change
    }

    fun setBrightness(brightness: Int): Boolean {
        val clampedBrightness = brightness.coerceIn(minBrightness, maxBrightness)

        Log.i(TAG, "Changing brightness to $clampedBrightness")
        return vehiclePropertyHelper.setIntProperty(
            BRIGHTNESS_PROPERTY_ID,
            BRIGHTNESS_AREA_ID,
            clampedBrightness
        )
    }
}
