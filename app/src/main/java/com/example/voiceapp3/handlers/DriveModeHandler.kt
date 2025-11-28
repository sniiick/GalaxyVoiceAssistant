package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class DriveModeHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "DriveModeHandler"

    private val modeConfig = CarPropertyRegistry.DriveMode.MODE

    private val ecoPattern = Regex("эко|интеллект|адаптив", RegexOption.IGNORE_CASE)
    private val electricPattern = Regex("электр|батаре", RegexOption.IGNORE_CASE)
    private val hybridPattern = Regex("гибрид", RegexOption.IGNORE_CASE)
    private val sportPattern = Regex("спорт", RegexOption.IGNORE_CASE)

    override fun canHandle(intent: String): Boolean = intent == "drive_mode"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)
        val text = prediction.normalizedText

        val modeValue = when {
            ecoPattern.containsMatchIn(text) -> CarPropertyRegistry.DriveMode.ECO
            electricPattern.containsMatchIn(text) -> CarPropertyRegistry.DriveMode.ELECTRIC
            hybridPattern.containsMatchIn(text) -> CarPropertyRegistry.DriveMode.HYBRID
            sportPattern.containsMatchIn(text) -> CarPropertyRegistry.DriveMode.SPORT
            params.value == 1 -> CarPropertyRegistry.DriveMode.ECO
            params.value == 2 -> CarPropertyRegistry.DriveMode.ELECTRIC
            params.value == 3 -> CarPropertyRegistry.DriveMode.HYBRID
            params.value == 4 -> CarPropertyRegistry.DriveMode.SPORT
            else -> return false
        }

        return setDriveMode(modeValue)
    }

    private fun setDriveMode(value: Int): Boolean {
        Log.i(TAG, "Setting drive mode to $value")
        return vehiclePropertyHelper.setIntProperty(
            modeConfig.getPropertyId(),
            modeConfig.getAreaId(),
            value
        )
    }
}