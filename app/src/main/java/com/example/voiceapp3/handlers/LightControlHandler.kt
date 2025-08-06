package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class LightControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG: String? = "LightControlHandler"

    // Property ID for reading lights
    private val LIGHT_CONTROL_PROPERTY = 356544592

    override fun canHandle(intent: String): Boolean = intent == "light_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> setLight(true)
            "unset" -> setLight(false)
            else -> false
        }
    }

    fun setLight(turnOn: Boolean): Boolean {
        // The area IDs from the config (1, 2, 4, 16, 64)
        // You might want to handle these differently based on your requirements
        val supportedAreas = listOf(1, 2, 4, 16, 64)

        // For simplicity, we'll apply the action to all supported areas
        var success = true
        supportedAreas.forEach { areaId ->
            try {
                vehiclePropertyHelper.setIntProperty(
                    LIGHT_CONTROL_PROPERTY,
                    areaId,
                    if (turnOn) 1 else 0
                )
                Log.i(TAG, "Light control ${if (turnOn) "on" else "off"} for area $areaId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to control light for area $areaId", e)
                success = false
            }
        }

        return success
    }
}