package com.example.voiceapp3.handlers

import android.car.VehiclePropertyIds
import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class FuelCharingHandler(val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "FuelChargingHandler"

    override fun canHandle(intent: String): Boolean = intent in "fuel_charge"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> setCharging(true)
            "unset" -> setCharging(false)
            else -> false
        }
    }

    private fun setCharging(on: Boolean): Boolean {
        val currentSpeed = vehiclePropertyHelper.getFloatProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)
        if (currentSpeed > 0.0f) {
            Log.w(TAG, "Charging is locked - vehicle is moving (speed: $currentSpeed)")
            return false
        }

        Log.i(TAG, "Setting charge status to $on")
        val result = vehiclePropertyHelper.setBoolProperty(
            555774850,
            0,
            on
        )
        return result
    }
}