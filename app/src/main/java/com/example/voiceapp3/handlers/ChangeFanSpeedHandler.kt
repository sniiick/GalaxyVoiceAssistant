package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.CarPropertyRegistry


class ChangeFanSpeedHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "ChangeFanSpeedHandler"
    private val acControlHandler = AcControlHandler(vehiclePropertyHelper)

    private val fanSpeedConfig = CarPropertyRegistry.Hvac.FAN_SPEED
    private val defaultSpeedChange = 1
    private val minSpeed = CarPropertyRegistry.Hvac.MIN_FAN_SPEED_STARSHIP

    private val maxSpeed: Int
        get() = if (CarModel.isCoolray) {
            CarPropertyRegistry.Hvac.MAX_FAN_SPEED_COOLRAY
        } else {
            CarPropertyRegistry.Hvac.MAX_FAN_SPEED_STARSHIP
        }

    override fun canHandle(intent: String): Boolean = intent == "change_fan_speed"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)
        val directions = mutableSetOf<Int>()

        if (prediction.normalizedText.contains(Regex("салон|внутр|лицо"))) directions.add(1)
        if (prediction.normalizedText.contains(Regex("ног|вниз"))) directions.add(2)
        if (prediction.normalizedText.contains(Regex("стекл|стёкл|вверх|лобов"))) directions.add(4)
        if (prediction.normalizedText.contains(Regex("везде|всюду|всего|всё|вместе"))) {
            directions.clear()
            directions.add(7)
        }

        if (directions.isNotEmpty()) {
            handleSetFanSpeed(params, shouldSetAuto = false)

            val isMultiple = prediction.normalizedText.contains("плюс") || directions.size > 1
            return if (isMultiple) {
                val combinedDirection = directions.reduce { acc, dir -> acc + dir }
                acControlHandler.setAcDirection(combinedDirection)
            } else {
                acControlHandler.setAcDirection(directions.first())
            }
        }

        return when (params.action) {
            "set" -> handleSetFanSpeed(params)
            "increase" -> handleChangeFanSpeed(params)
            "decrease" -> handleChangeFanSpeed(params)
            "unset" -> acControlHandler.turnOffAc()
            else -> handleSetFanSpeed(params)
        }
    }

    private fun handleSetFanSpeed(params: CommandParams, shouldSetAuto: Boolean = true): Boolean {
        return when {
            params.value != null && params.unit == "number" -> {
                setFanSpeed(params.value)
            }
            params.value != null && params.unit == "percent" -> {
                when (params.value) {
                    1 -> setFanSpeed(minSpeed)
                    50 -> setFanSpeed((minSpeed + maxSpeed) / 2)
                    100 -> setFanSpeed(maxSpeed)
                    else -> false
                }
            }
            else -> {
                if (shouldSetAuto) {
                    acControlHandler.turnOnAc(setAuto = true)
                } else {
                    false
                }
            }
        }
    }

    private fun handleChangeFanSpeed(params: CommandParams): Boolean {
        val currentSpeed = vehiclePropertyHelper.getIntProperty(
            fanSpeedConfig.getPropertyId(),
            fanSpeedConfig.getAreaId()
        )

        if (currentSpeed == -1) {
            Log.d(TAG, "Current speed is unknown")
            return false
        }

        val newSpeed = when {
            params.action == "increase" && params.value != null && params.unit == "number" ->
                currentSpeed + params.value

            params.action == "increase" && params.value != null && params.unit == "percent" -> {
                if (params.value == 100) maxSpeed else currentSpeed + defaultSpeedChange
            }

            params.action == "increase" ->
                currentSpeed + defaultSpeedChange

            params.action == "decrease" && params.value != null && params.unit == "number" ->
                currentSpeed - params.value

            params.action == "decrease" && params.value != null && params.unit == "percent" -> {
                if (params.value == 100) minSpeed else currentSpeed - defaultSpeedChange
            }

            params.action == "decrease" ->
                currentSpeed - defaultSpeedChange

            else -> return false
        }
        return setFanSpeed(newSpeed)
    }

    private fun setFanSpeed(speed: Int): Boolean {
        if (!acControlHandler.turnOnAc(setAuto = false)) {
            return false
        }

        val clampedSpeed = speed.coerceIn(minSpeed, maxSpeed)

        Log.i(TAG, "Changing fan speed to $clampedSpeed")
        return vehiclePropertyHelper.setIntProperty(
            fanSpeedConfig.getPropertyId(),
            fanSpeedConfig.getAreaId(),
            clampedSpeed
        )
    }
}