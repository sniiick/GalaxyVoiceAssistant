package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class SeatMassageHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "SeatMassageHandler"

    // Property IDs
    private val MASSAGE_SWITCH = 622883040
    private val MASSAGE_POWER = 624980189
    private val MASSAGE_TYPE = 624980193

    // Area IDs
    private val PILOT_SEAT = 1
    private val PASSENGER_SEAT = 4

    // Value ranges
    private val MIN_POWER = 1
    private val MAX_POWER = 3
    private val MIN_TYPE = 1
    private val MAX_TYPE = 8
    private val TYPE_KEYWORDS = setOf("тип", "режим", "вариант")
    private var TYPE_ACTION: Boolean = false

    override fun canHandle(intent: String): Boolean = intent == "seat_massage"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)
        val targetSeats = determineTargetSeats(prediction)

        if (TYPE_KEYWORDS.any { it in prediction.normalizedText }) {
            TYPE_ACTION = true
        }

        return when (params.action) {
            "set" -> handleSetMassage(params, targetSeats)
            "unset" -> handleUnsetMassage(targetSeats)
            "increase" -> handleChangeMassagePower(params, targetSeats, true)
            "decrease" -> handleChangeMassagePower(params, targetSeats, false)
            else -> handleSetMassage(params, targetSeats)
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

    private fun handleSetMassage(params: CommandParams, targetSeats: Set<Int>): Boolean {
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
                if (TYPE_ACTION) {
                    return setMassageType(targetSeats, params.value)
                }
                params.value
            }
            else -> 2
        }
        return setMassagePower(targetSeats, power)
    }

    private fun handleUnsetMassage(targetSeats: Set<Int>): Boolean {
        var success = true
        for (seat in targetSeats) {
            // Turn off massage
            success = success && vehiclePropertyHelper.setBoolProperty(
                MASSAGE_SWITCH,
                seat,
                false
            )

            // Set power off
            success = success && vehiclePropertyHelper.setIntProperty(
                MASSAGE_POWER,
                seat,
                0
            )
        }
        return success
    }

    private fun handleChangeMassagePower(params: CommandParams, targetSeats: Set<Int>, increase: Boolean): Boolean {
        var success = true

        for (seat in targetSeats) {
            val currentPower = vehiclePropertyHelper.getIntProperty(MASSAGE_POWER, seat)
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
            success = success && setMassagePower(setOf(seat), newPower)
        }

        return success
    }

    private fun turnOnMassage(targetSeats: Set<Int>): Boolean {
        var success = true
        for (seat in targetSeats) {
            success = success && vehiclePropertyHelper.setBoolProperty(
                MASSAGE_SWITCH,
                seat,
                true
            )
        }
        return success
    }
    private fun setMassagePower(targetSeats: Set<Int>, power: Int): Boolean {
        var success = turnOnMassage(targetSeats)

        val clampedPower = when {
            power > MAX_POWER -> MAX_POWER
            power < MIN_POWER -> MIN_POWER
            else -> power
        }

        for (seat in targetSeats) {
            success = success && vehiclePropertyHelper.setIntProperty(
                MASSAGE_POWER,
                seat,
                clampedPower
            )
        }
        return success
    }

    private fun setMassageType(targetSeats: Set<Int>, type: Int): Boolean {
        var success = turnOnMassage(targetSeats)

        val clampedType = when {
            type > MAX_TYPE -> MAX_TYPE
            type < MIN_TYPE -> MIN_TYPE
            else -> type
        }

        for (seat in targetSeats) {
            val currentPower = vehiclePropertyHelper.getIntProperty(MASSAGE_POWER, seat)
            if (currentPower == 0) {
                success = success && setMassagePower(setOf(seat), 2)
            }

            success = success && vehiclePropertyHelper.setIntProperty(
                MASSAGE_TYPE,
                seat,
                clampedType
            )
        }
        return success
    }
}