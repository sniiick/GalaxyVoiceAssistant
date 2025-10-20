package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class ExteriorLightControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG: String = "ExteriorLightControlHandler"

    // Property ID for exterior lights
    companion object {
        private val EXTERIOR_LIGHT_CONTROL_PROPERTY = 557871126
        private val EXTERIOR_FOG_CONTROL_PROPERTY = 289410578
        private val EXTERIOR_LIGHT_AREA_ID = 0
    }

    // Light types and their corresponding values
    private enum class ExteriorLightType(
        val propertyId: Int,
        val englishName: String,
        val regex: Regex,
        val setValue: Int,
        val unsetValue: Int
    ) {
        HEADLIGHTS(
            EXTERIOR_LIGHT_CONTROL_PROPERTY,
            "headlights",
            Regex("headlight(s)?|light(s)?|beam(s)?", RegexOption.IGNORE_CASE),
            3,
            0
        ),
        PARKING(
            EXTERIOR_LIGHT_CONTROL_PROPERTY,
            "parking lights",
            Regex("parking light(s)?|position light(s)?", RegexOption.IGNORE_CASE),
            1,
            0
        ),
        FOG_LIGHTS(
            EXTERIOR_FOG_CONTROL_PROPERTY,
            "fog lights",
            Regex("fog light(s)?|fog lamp(s)?", RegexOption.IGNORE_CASE),
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
                lightType.propertyId,
                EXTERIOR_LIGHT_AREA_ID,
                lightType.setValue
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set exterior light to ${lightType.englishName}", e)
            false
        }
    }

    private fun unsetExteriorLight(lightType: ExteriorLightType): Boolean {
        return try {
            vehiclePropertyHelper.setIntProperty(
                lightType.propertyId,
                EXTERIOR_LIGHT_AREA_ID,
                lightType.unsetValue
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unset exterior light for ${lightType.englishName}", e)
            false
        }
    }
}
