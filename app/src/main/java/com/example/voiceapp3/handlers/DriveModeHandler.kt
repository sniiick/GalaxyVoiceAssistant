package com.example.voiceapp3.handlers

import android.car.VehiclePropertyIds
import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class DriveModeHandler(val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "FuelChargingHandler"
    private val mode1 = Regex("eco|intelligent|adaptive" , RegexOption.IGNORE_CASE)
    private val mode2 = Regex("electric|battery" , RegexOption.IGNORE_CASE)
    private val mode3 = Regex("hybrid" , RegexOption.IGNORE_CASE)
    private val mode4 = Regex("sport" , RegexOption.IGNORE_CASE)


    override fun canHandle(intent: String): Boolean = intent in "drive_mode"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        val value = if (mode1.containsMatchIn(prediction.normalizedText)) {
            1
        } else if (mode2.containsMatchIn(prediction.normalizedText)) {
            2
        } else if(mode3.containsMatchIn(prediction.normalizedText)) {
            3
        } else if(mode4.containsMatchIn(prediction.normalizedText)) {
            4
        } else params.value

        return when (value) {
            1 -> setDriveMode(24)
            2 -> setDriveMode(16)
            3 -> setDriveMode(0)
            4 -> setDriveMode(2)
            else -> false
        }
    }

    private fun setDriveMode(value: Int): Boolean {
        Log.i(TAG, "Setting drive mode to $value")
        val result = vehiclePropertyHelper.setIntProperty(
            557871372,
            0,
            value
        )
        return result
    }
}
