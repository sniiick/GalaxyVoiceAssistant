package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class ExteriorLightControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG: String = "ExteriorLightControlHandler"

    // Property ID for exterior lights
    private val EXTERIOR_LIGHT_CONTROL_PROPERTY = 557871126
    private val EXTERIOR_LIGHT_AREA_ID = 0

    // Light types and their corresponding values
    private enum class ExteriorLightType(
        val russianName: String,
        val regex: Regex,
        val setValue: Int,
        val unsetValue: Int
    ) {
        HEADLIGHTS(
            "фары",
            Regex("фар[ыae]|свет|ближний", RegexOption.IGNORE_CASE),
            3,
            0
        ),
        PARKING(
            "габариты",
            Regex("габарит", RegexOption.IGNORE_CASE),
            1,
            3
        ),
        FOG_LIGHTS(
            "туманки",
            Regex("туман", RegexOption.IGNORE_CASE),
            2,
            3
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
                EXTERIOR_LIGHT_CONTROL_PROPERTY,
                EXTERIOR_LIGHT_AREA_ID,
                lightType.setValue
            )
            Log.i(TAG, "Exterior light set to ${lightType.russianName} (value: ${lightType.setValue})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set exterior light to ${lightType.russianName}", e)
            false
        }
    }

    private fun unsetExteriorLight(lightType: ExteriorLightType): Boolean {
        return try {
            vehiclePropertyHelper.setIntProperty(
                EXTERIOR_LIGHT_CONTROL_PROPERTY,
                EXTERIOR_LIGHT_AREA_ID,
                lightType.unsetValue
            )
            Log.i(TAG, "Exterior light unset for ${lightType.russianName}, set to ${if (lightType.unsetValue == 0) "off" else "фары (auto)"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unset exterior light for ${lightType.russianName}", e)
            false
        }
    }
}