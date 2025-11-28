package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class LightControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG = "LightControlHandler"

    private val lightConfig = CarPropertyRegistry.Light.INTERIOR

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
        var success = false
        val propertyId = lightConfig.getPropertyId()

        CarPropertyRegistry.Light.SUPPORTED_AREAS.forEach { areaId ->
            try {
                vehiclePropertyHelper.setIntProperty(
                    propertyId,
                    areaId,
                    if (turnOn) 1 else 0
                )
                success = true
                Log.i(TAG, "Light control ${if (turnOn) "on" else "off"} for area $areaId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to control light for area $areaId", e)
            }
        }

        return success
    }
}