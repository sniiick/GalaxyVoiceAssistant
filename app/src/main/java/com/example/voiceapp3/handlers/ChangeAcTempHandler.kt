package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry
import kotlin.math.roundToInt

class ChangeAcTempHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "ChangeAcTempHandler"
    private val acControlHandler = AcControlHandler(vehiclePropertyHelper)

    private val tempConfig = CarPropertyRegistry.Hvac.TEMPERATURE
    private val defaultTempChange = 2f
    private val minTemp = CarPropertyRegistry.Hvac.MIN_TEMP
    private val maxTemp = CarPropertyRegistry.Hvac.MAX_TEMP

    override fun canHandle(intent: String): Boolean = intent == "change_ac_temp"

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
                    1 -> setTemp(minTemp)
                    50 -> setTemp(((minTemp + maxTemp) / 2f).roundToHalf())
                    100 -> setTemp(maxTemp)
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun handleChangeTemperature(params: CommandParams): Boolean {
        val currentTemp = vehiclePropertyHelper.getFloatProperty(
            tempConfig.getPropertyId(),
            tempConfig.getAreaId()
        )

        if (currentTemp == -1f) {
            Log.d(TAG, "Current temperature is unknown")
            return false
        }

        val newTemp = when {
            params.action == "increase" && params.value != null && params.unit == "degree" ->
                currentTemp + params.value

            params.action == "increase" && params.value != null && params.unit == "percent" -> {
                val percentValue = params.value.toFloat() / 100f
                currentTemp + ((maxTemp - minTemp) * percentValue).roundToHalf()
            }

            params.action == "increase" ->
                currentTemp + defaultTempChange

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

        val clampedTemp = temp.coerceIn(minTemp, maxTemp)

        Log.i(TAG, "Changing temperature to $clampedTemp")
        return vehiclePropertyHelper.setFloatProperty(
            tempConfig.getPropertyId(),
            tempConfig.getAreaId(),
            clampedTemp
        )
    }
}