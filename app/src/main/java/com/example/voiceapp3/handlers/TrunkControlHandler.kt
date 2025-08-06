package com.example.voiceapp3.handlers

import android.car.VehiclePropertyIds
import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class TrunkControlHandler(val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "TrunkControlHandler"
    val TRUNK_PROPERTY_ID = 373295873
    val TRUNK_AREA_ID = 536870912
    val TRUNK_OPEN = 1
    val TRUNK_CLOSE = 0

    override fun canHandle(intent: String): Boolean = intent in "trunk_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> openTrunk()
            "unset" -> closeTrunk()
            else -> false
        }
    }

    private fun openTrunk(): Boolean {
        val currentSpeed = vehiclePropertyHelper.getFloatProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
        if (currentSpeed >= 5.0f) {
            Log.w(TAG, "Trunk open blocked - vehicle is moving (speed: $currentSpeed)")
            return false
        }

        Log.i(TAG, "Opening trunk")
        val result = vehiclePropertyHelper.setIntProperty(
            TRUNK_PROPERTY_ID,
            TRUNK_AREA_ID,
            TRUNK_OPEN
        )
        return result
    }

    private fun closeTrunk(): Boolean {
        Log.i(TAG, "Closing trunk")
        val result = vehiclePropertyHelper.setIntProperty(
            TRUNK_PROPERTY_ID,
            TRUNK_AREA_ID,
            TRUNK_CLOSE
        )
        return result
    }
}