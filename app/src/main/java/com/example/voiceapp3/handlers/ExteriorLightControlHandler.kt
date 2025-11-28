package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class ExteriorLightControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG = "ExteriorLightControlHandler"

    private val exteriorConfig = CarPropertyRegistry.Light.EXTERIOR
    private val fogConfig = CarPropertyRegistry.Light.FOG

    private enum class ExteriorLightType(
        val configGetter: () -> Int,
        val russianName: String,
        val regex: Regex,
        val setValue: Int,
        val unsetValue: Int
    ) {
        HEADLIGHTS(
            { CarPropertyRegistry.Light.EXTERIOR.getPropertyId() },
            "фары",
            Regex("фар[ыae]|свет|ближний", RegexOption.IGNORE_CASE),
            CarPropertyRegistry.Light.HEADLIGHTS_ON,
            CarPropertyRegistry.Light.OFF
        ),
        PARKING(
            { CarPropertyRegistry.Light.EXTERIOR.getPropertyId() },
            "габариты",
            Regex("габарит", RegexOption.IGNORE_CASE),
            CarPropertyRegistry.Light.PARKING_ON,
            CarPropertyRegistry.Light.OFF
        ),
        FOG_LIGHTS(
            { CarPropertyRegistry.Light.FOG.getPropertyId() },
            "туманки",
            Regex("туман", RegexOption.IGNORE_CASE),
            1,
            0
        );

        companion object {
            fun fromString(type: String): ExteriorLightType? {
                return values().firstOrNull { it.regex.containsMatchIn(type) }
            }
        }
    }

    override fun canHandle(intent: String): Boolean = intent == "exterior_light_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)
        val lightType = ExteriorLightType.fromString(prediction.normalizedText)

        if (lightType == null) {
            Log.e(TAG, "Unknown light type")
            return false
        }

        return when (params.action?.lowercase()) {
            "set" -> setExteriorLight(lightType)
            "unset" -> unsetExteriorLight(lightType)
            else -> false
        }
    }

    private fun setExteriorLight(lightType: ExteriorLightType): Boolean {
        return try {
            vehiclePropertyHelper.setIntProperty(
                lightType.configGetter(),
                0,
                lightType.setValue
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set exterior light to ${lightType.russianName}", e)
            false
        }
    }

    private fun unsetExteriorLight(lightType: ExteriorLightType): Boolean {
        return try {
            vehiclePropertyHelper.setIntProperty(
                lightType.configGetter(),
                0,
                lightType.unsetValue
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unset exterior light for ${lightType.russianName}", e)
            false
        }
    }
}