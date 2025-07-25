package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class FuelDoorHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "FuelDoorHandler"

    // Property ID
    private val FUEL_DOOR_PROPERTY = 287310600

    override fun canHandle(intent: String): Boolean = intent == "fuel_door_open"

    override fun handle(prediction: PredictionResult): Boolean {
        // Check current state first
        val currentState = vehiclePropertyHelper.getBoolProperty(FUEL_DOOR_PROPERTY, 0)

        return when {
            currentState -> {
                Log.i(TAG, "Fuel door is already opened")
                true
            }
            else -> {
                vehiclePropertyHelper.setBoolProperty(
                    FUEL_DOOR_PROPERTY,
                    0,
                    true
                )
            }
        }
    }
}