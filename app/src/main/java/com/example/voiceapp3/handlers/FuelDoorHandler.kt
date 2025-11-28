package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class FuelDoorHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "FuelDoorHandler"

    private val fuelDoorConfig = CarPropertyRegistry.Fuel.DOOR

    override fun canHandle(intent: String): Boolean = intent == "fuel_door_open"

    override fun handle(prediction: PredictionResult): Boolean {
        val propertyId = fuelDoorConfig.getPropertyId()
        val areaId = fuelDoorConfig.getAreaId()

        val currentState = vehiclePropertyHelper.getBoolProperty(propertyId, areaId)

        return if (currentState) {
            Log.d(TAG, "Fuel door is already opened")
            true
        } else {
            vehiclePropertyHelper.setBoolProperty(propertyId, areaId, true)
        }
    }
}