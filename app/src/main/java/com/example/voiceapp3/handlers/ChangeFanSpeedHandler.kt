package com.example.voiceapp3.handlers

import android.car.VehiclePropertyIds
import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.ModelEnum
import java.lang.Integer.sum


class ChangeFanSpeedHandler(val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "ChangeFanSpeedHandler"
    private val acControlHandler: AcControlHandler = AcControlHandler(vehiclePropertyHelper)
    private val areaId: Int = 5
    private val defaultSpeedChange: Int = 1
    private var minSpeed: Int = 1
    private var maxSpeed: Int = 9

    override fun canHandle(intent: String): Boolean = intent == "change_fan_speed"

    override fun handle(prediction: PredictionResult): Boolean {
        if (CarModel.isCoolray) {
            maxSpeed = 8
        }

        val params = extractCommonEntities(prediction)
        val directions = mutableSetOf<Int>()

        if (prediction.normalizedText.contains(Regex("салон|внутр|лицо"))) directions.add(1)
        if (prediction.normalizedText.contains(Regex("ног|вниз"))) directions.add(2)
        if (prediction.normalizedText.contains(Regex("стекл|стёкл|вверх|лобов"))) directions.add(4)
        if (prediction.normalizedText.contains(Regex("везде|всюду|всего|всё|вместе"))) {
            directions.clear()
            directions.add(7)
        }
        if (!directions.isEmpty()) {
            handleSetFanSpeed(params, shouldSetAuto = false)

            val isMultiple = prediction.normalizedText.contains("плюс") || directions.size > 1
            if (isMultiple) {
                val combinedDirection = directions.reduce { acc, dir -> acc + dir }
                return acControlHandler.setAcDirection(combinedDirection)
            } else {
                return acControlHandler.setAcDirection(directions.first())
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
                    50 -> setFanSpeed(sum(minSpeed, maxSpeed) / 2)
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
            VehiclePropertyIds.HVAC_FAN_SPEED,
            areaId
        )

        if (currentSpeed == -1) {
            Log.d(TAG, "Current speed is unknown")
            return false
        }

        val newSpeed = when {
            // Increase cases
            params.action == "increase" && params.value != null && params.unit == "number" ->
                currentSpeed + params.value

            params.action == "increase" && params.value != null && params.unit == "percent" -> {
                when (params.value) {
                    100 -> maxSpeed
                    else -> currentSpeed + defaultSpeedChange
                }
            }

            params.action == "increase" ->
                currentSpeed + defaultSpeedChange

            // Decrease cases
            params.action == "decrease" && params.value != null && params.unit == "number" ->
                currentSpeed - params.value

            params.action == "decrease" && params.value != null && params.unit == "percent" -> {
                when (params.value) {
                    100 -> minSpeed
                    else -> currentSpeed - defaultSpeedChange
                }
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

        val clampedSpeed = when {
            speed > maxSpeed -> maxSpeed
            speed < minSpeed -> minSpeed
            else -> speed
        }

        Log.i(TAG, "Changing fan speed from to $clampedSpeed")
        return vehiclePropertyHelper.setIntProperty(
            VehiclePropertyIds.HVAC_FAN_SPEED,
            areaId,
            clampedSpeed
        )
    }
}