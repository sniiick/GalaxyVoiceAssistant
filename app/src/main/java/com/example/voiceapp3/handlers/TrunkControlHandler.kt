package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class TrunkControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "TrunkControlHandler"

    private val trunkConfig = CarPropertyRegistry.Trunk.CONTROL
    private val speedConfig = CarPropertyRegistry.Vehicle.SPEED

    override fun canHandle(intent: String): Boolean = intent == "trunk_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> openTrunk()
            "unset" -> closeTrunk()
            else -> false
        }
    }

    private fun openTrunk(): Boolean {
        val currentSpeed = vehiclePropertyHelper.getFloatProperty(
            speedConfig.getPropertyId(),
            speedConfig.getAreaId()
        )
        if (currentSpeed >= 5.0f) {
            Log.w(TAG, "Trunk open blocked - vehicle is moving (speed: $currentSpeed)")
            return false
        }

        Log.i(TAG, "Opening trunk")
        return vehiclePropertyHelper.setIntProperty(
            trunkConfig.getPropertyId(),
            trunkConfig.getAreaId(),
            CarPropertyRegistry.Trunk.OPEN
        )
    }

    private fun closeTrunk(): Boolean {
        Log.i(TAG, "Closing trunk")
        return vehiclePropertyHelper.setIntProperty(
            trunkConfig.getPropertyId(),
            trunkConfig.getAreaId(),
            CarPropertyRegistry.Trunk.CLOSE
        )
    }
}