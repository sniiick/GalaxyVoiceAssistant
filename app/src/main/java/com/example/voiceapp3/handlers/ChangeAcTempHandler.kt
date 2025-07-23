package com.example.voiceapp3.handlers

import android.car.VehicleAreaSeat
import android.car.VehiclePropertyIds
import android.util.Log
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import kotlin.math.roundToInt

class ChangeAcTempHandler(val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "ChangeAcTempHandler"
    private val acControlHandler: AcControlHandler = AcControlHandler(vehiclePropertyHelper)
    private val areaId: Int = VehicleAreaSeat.SEAT_ROW_1_LEFT
    private val defaultTempChange: Float = 2f
    private val minTemp: Float = 15.5f
    private val maxTemp: Float = 28.5f
    override fun canHandle(intent: String): Boolean = intent in "change_ac_temp"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> handleSetTemperature(params)
            "increase" -> handleChangeTemperature(params)
            "decrease" -> handleChangeTemperature(params)
            "unset" -> acControlHandler.turnOffAc()
            else -> handleSetTemperature(params)
        }
    }

    private fun handleSetTemperature(params: CommandParams): Boolean {
        return when {
            params.value != null && params.unit == "degree" -> {
                setTemp(params.value.toFloat())
            }
            params.value != null && params.unit == "percent" -> {
                when (params.value) {
                    1 -> { setTemp(minTemp) }
                    100 -> { setTemp(maxTemp) }
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun handleChangeTemperature(params: CommandParams): Boolean {
        val currentTemp = vehiclePropertyHelper.getFloatProperty(
            VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            areaId
        )

        if (currentTemp == -1f) {
            Log.i(TAG, "Current temperature is unknown")
            return false
        }

        val newTemp = when {
            // Increase cases
            params.action == "increase" && params.value != null && params.unit == "degree" ->
                currentTemp + params.value

            params.action == "increase" && params.value != null && params.unit == "percent" -> {
                val percentValue = params.value.toFloat() / 100f
                currentTemp + ((maxTemp - minTemp) * percentValue).roundToHalf()
            }

            params.action == "increase" ->
                currentTemp + defaultTempChange

            // Decrease cases
            params.action == "decrease" && params.value != null && params.unit == "degree" ->
                currentTemp - params.value

            params.action == "decrease" && params.value != null && params.unit == "percent" -> {
                val percentValue = params.value.toFloat() / 100f
                currentTemp - ((maxTemp - minTemp) * percentValue).roundToHalf()
            }

            params.action == "decrease" ->
                currentTemp - defaultTempChange

            else -> return false
        }
        return setTemp(newTemp)
    }

    private fun Float.roundToHalf(): Float {
        return (this * 2).roundToInt().toFloat() / 2
    }

    fun setTemp(temp: Float): Boolean {
        if (!acControlHandler.turnOnAc(setAuto = false)) {
            return false
        }

        val clampedTemp = when {
            temp > maxTemp -> maxTemp
            temp < minTemp -> minTemp
            else -> temp
        }

        Log.i(TAG, "Changing temperature to $clampedTemp")
        return vehiclePropertyHelper.setFloatProperty(
            VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            areaId,
            clampedTemp
        )
    }
}