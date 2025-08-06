package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class SeatClimateHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "SeatClimateHandler"

    // Property IDs
    private val VENTILATION_PROPERTY = 356517139
    private val HEAT_PROPERTY = 356517131

    // Area IDs
    private val PILOT_SEAT = 1
    private val PASSENGER_SEAT = 4

    // Value ranges
    private val MIN_POWER = 1
    private val MAX_POWER = 3

    override fun canHandle(intent: String): Boolean = intent == "seat_ventilation" || intent == "seat_heat"

    override fun handle(prediction: PredictionResult): Boolean {
        val isVentilation = prediction.intent == "seat_ventilation"
        val params = extractCommonEntities(prediction)
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
        return when (prediction.getString("direction")) {
            "left" -> setOf(PILOT_SEAT)
            "right" -> setOf(PASSENGER_SEAT)
            "both" -> setOf(PILOT_SEAT, PASSENGER_SEAT)
            else -> setOf(PILOT_SEAT)
        }
    }

    private fun handleSetClimate(isVentilation: Boolean, params: CommandParams, targetSeats: Set<Int>): Boolean {
        val propertyId = if (isVentilation) VENTILATION_PROPERTY else HEAT_PROPERTY
        val power = when {
            params.value != null && params.unit == "percent" -> {
                 when (params.value) {
                    1 -> 1
                    50 -> 2
                    100 -> 3
                    else -> 2
                }
            }
            params.value != null && params.unit == "number" -> {
                params.value
            }
            else -> 2
        }
        return setClimatePower(propertyId, targetSeats, power)
    }

    private fun handleUnsetClimate(isVentilation: Boolean, targetSeats: Set<Int>): Boolean {
        var success = true
        val propertyId = if (isVentilation) VENTILATION_PROPERTY else HEAT_PROPERTY
        for (seat in targetSeats) {
            success = success && vehiclePropertyHelper.setIntProperty(
                propertyId,
                seat,
                0
            )
        }
        return success
    }

    private fun handleChangeClimatePower(
        isVentilation: Boolean,
        params: CommandParams,
        targetSeats: Set<Int>,
        increase: Boolean,
    ): Boolean {
        val propertyId = if (isVentilation) VENTILATION_PROPERTY else HEAT_PROPERTY
        var success = true

        for (seat in targetSeats) {
            val currentPower = vehiclePropertyHelper.getIntProperty(propertyId, seat)
            if (currentPower == -1) {
                Log.i(TAG, "Current power is unknown for seat $seat")
                success = false
                continue
            }

            val powerChange = when {
                params.value != null && params.unit == "percent" -> {
                    when (params.value) {
                        100 -> if (increase) MAX_POWER - currentPower else currentPower - MIN_POWER
                        else -> if (increase) 1 else -1
                    }
                }
                else -> if (increase) 1 else -1
            }

            val newPower = currentPower + powerChange
            success = success && setClimatePower(propertyId, setOf(seat), newPower)
        }

        return success
    }

    private fun setClimatePower(propertyId: Int, targetSeats: Set<Int>, power: Int): Boolean {
        var success = true
        val clampedPower = when {
            power > MAX_POWER -> MAX_POWER
            power < MIN_POWER -> MIN_POWER
            else -> power
        }

        for (seat in targetSeats) {
            success = success && vehiclePropertyHelper.setIntProperty(
                propertyId,
                seat,
                clampedPower
            )
        }
        return success
    }
}