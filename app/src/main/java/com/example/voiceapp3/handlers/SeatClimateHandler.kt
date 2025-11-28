package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class SeatClimateHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "SeatClimateHandler"

    private val ventilationConfig = CarPropertyRegistry.Seat.Climate.VENTILATION
    private val heatConfig = CarPropertyRegistry.Seat.Climate.HEAT
    private val pluralRegex = Regex("кресел|сидений|сидушек|диванов", RegexOption.IGNORE_CASE)

    override fun canHandle(intent: String): Boolean = intent == "seat_ventilation" || intent == "seat_heat"

    override fun handle(prediction: PredictionResult): Boolean {
        val isVentilation = prediction.intent == "seat_ventilation"
        val params = extractCommonEntities(prediction)

        if (prediction.normalizedText.contains(Regex("зеркал|стекл|стекол|стёкл|стёкол"))) {
            val acControlHandler = AcControlHandler(vehiclePropertyHelper)
            return when (params.action) {
                "set" -> acControlHandler.setWindowHeat(true)
                "unset" -> acControlHandler.setWindowHeat(false)
                else -> acControlHandler.setWindowHeat(true)
            }
        }

        val targetSeats = determineTargetSeats(prediction)

        return when (params.action) {
            "set" -> handleSetClimate(isVentilation, params, targetSeats)
            "unset" -> handleUnsetClimate(isVentilation, targetSeats)
            "increase" -> handleChangeClimatePower(isVentilation, params, targetSeats, increase = true)
            "decrease" -> handleChangeClimatePower(isVentilation, params, targetSeats, increase = false)
            else -> handleSetClimate(isVentilation, params, targetSeats)
        }
    }

    private fun determineTargetSeats(prediction: PredictionResult): Set<Int> {
        if (pluralRegex.containsMatchIn(prediction.normalizedText)) {
            return setOf(CarPropertyRegistry.Seat.PILOT, CarPropertyRegistry.Seat.PASSENGER)
        }

        return when (prediction.getString("direction")) {
            "left" -> setOf(CarPropertyRegistry.Seat.PILOT)
            "right" -> setOf(CarPropertyRegistry.Seat.PASSENGER)
            "both" -> setOf(CarPropertyRegistry.Seat.PILOT, CarPropertyRegistry.Seat.PASSENGER)
            else -> setOf(CarPropertyRegistry.Seat.PILOT)
        }
    }

    private fun getPropertyId(isVentilation: Boolean): Int {
        return if (isVentilation) ventilationConfig.getPropertyId() else heatConfig.getPropertyId()
    }

    private fun handleSetClimate(isVentilation: Boolean, params: CommandParams, targetSeats: Set<Int>): Boolean {
        val power = when {
            params.value != null && params.unit == "percent" -> {
                when (params.value) {
                    1 -> 1
                    50 -> 2
                    100 -> 3
                    else -> 2
                }
            }
            params.value != null && params.unit == "number" -> params.value
            else -> 2
        }
        return setClimatePower(getPropertyId(isVentilation), targetSeats, power)
    }

    private fun handleUnsetClimate(isVentilation: Boolean, targetSeats: Set<Int>): Boolean {
        val propertyId = getPropertyId(isVentilation)
        return targetSeats.all { seat ->
            vehiclePropertyHelper.setIntProperty(propertyId, seat, 0)
        }
    }

    private fun handleChangeClimatePower(
        isVentilation: Boolean,
        params: CommandParams,
        targetSeats: Set<Int>,
        increase: Boolean,
    ): Boolean {
        val propertyId = getPropertyId(isVentilation)

        return targetSeats.all { seat ->
            val currentPower = vehiclePropertyHelper.getIntProperty(propertyId, seat)
            if (currentPower == -1) {
                Log.d(TAG, "Current power is unknown for seat $seat")
                return@all false
            }

            val powerChange = when {
                params.value != null && params.unit == "percent" -> {
                    when (params.value) {
                        100 -> if (increase) CarPropertyRegistry.Seat.MAX_POWER - currentPower
                               else currentPower - CarPropertyRegistry.Seat.MIN_POWER
                        else -> if (increase) 1 else -1
                    }
                }
                else -> if (increase) 1 else -1
            }

            setClimatePower(propertyId, setOf(seat), currentPower + powerChange)
        }
    }

    private fun setClimatePower(propertyId: Int, targetSeats: Set<Int>, power: Int): Boolean {
        val clampedPower = power.coerceIn(CarPropertyRegistry.Seat.MIN_POWER, CarPropertyRegistry.Seat.MAX_POWER)

        return targetSeats.all { seat ->
            vehiclePropertyHelper.setIntProperty(propertyId, seat, clampedPower)
        }
    }
}