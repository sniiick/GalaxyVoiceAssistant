package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class FuelCharingHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "FuelChargingHandler"

    private val chargingConfig = CarPropertyRegistry.Fuel.CHARGING
    private val speedConfig = CarPropertyRegistry.Vehicle.SPEED

    override fun canHandle(intent: String): Boolean = intent == "fuel_charge"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> setCharging(true)
            "unset" -> setCharging(false)
            else -> false
        }
    }

    private fun setCharging(on: Boolean): Boolean {
        val currentSpeed = vehiclePropertyHelper.getFloatProperty(
            speedConfig.getPropertyId(),
            speedConfig.getAreaId()
        )
        if (currentSpeed > 0.0f) {
            Log.w(TAG, "Charging is locked - vehicle is moving (speed: $currentSpeed)")
            return false
        }

        Log.i(TAG, "Setting charge status to $on")
        return vehiclePropertyHelper.setBoolProperty(
            chargingConfig.getPropertyId(),
            chargingConfig.getAreaId(),
            on
        )
    }
}