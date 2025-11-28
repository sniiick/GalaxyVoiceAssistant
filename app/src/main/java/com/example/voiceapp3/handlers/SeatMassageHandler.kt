package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.CarPropertyRegistry
import com.example.voiceapp3.tools.ModelEnum

class SeatMassageHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "SeatMassageHandler"

    private val switchConfig = CarPropertyRegistry.Seat.Massage.SWITCH
    private val powerConfig = CarPropertyRegistry.Seat.Massage.POWER
    private val typeConfig = CarPropertyRegistry.Seat.Massage.TYPE

    private val maxType: Int
        get() = if (CarModel.isE5) {
            CarPropertyRegistry.Seat.Massage.MAX_TYPE_E5
        } else {
            CarPropertyRegistry.Seat.Massage.MAX_TYPE_STARSHIP
        }

    private val TYPE_KEYWORDS = setOf("тип", "режим", "вариант")
    private var typeAction = false

    override fun canHandle(intent: String): Boolean = intent == "seat_massage"

    override fun handle(prediction: PredictionResult): Boolean {
        val carModel = CarModel.getCarModel()
        if (carModel == ModelEnum.COOLRAY || carModel == ModelEnum.UNKNOWN) {
            return false
        }

        val params = extractCommonEntities(prediction)
        val targetSeats = determineTargetSeats(prediction)

        if (TYPE_KEYWORDS.any { it in prediction.normalizedText }) {
            typeAction = true
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
            "left" -> setOf(CarPropertyRegistry.Seat.PILOT)
            "right" -> setOf(CarPropertyRegistry.Seat.PASSENGER)
            "both" -> setOf(CarPropertyRegistry.Seat.PILOT, CarPropertyRegistry.Seat.PASSENGER)
            else -> setOf(CarPropertyRegistry.Seat.PILOT)
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
                if (typeAction) {
                    return setMassageType(targetSeats, params.value)
                }
                params.value
            }
            else -> 2
        }
        return setMassagePower(targetSeats, power)
    }

    private fun handleUnsetMassage(targetSeats: Set<Int>): Boolean {
        return targetSeats.all { seat ->
            vehiclePropertyHelper.setBoolProperty(switchConfig.getPropertyId(), seat, false) &&
            vehiclePropertyHelper.setIntProperty(powerConfig.getPropertyId(), seat, 0)
        }
    }

    private fun handleChangeMassagePower(params: CommandParams, targetSeats: Set<Int>, increase: Boolean): Boolean {
        return targetSeats.all { seat ->
            val currentPower = vehiclePropertyHelper.getIntProperty(powerConfig.getPropertyId(), seat)
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

            setMassagePower(setOf(seat), currentPower + powerChange)
        }
    }

    private fun turnOnMassage(targetSeats: Set<Int>): Boolean {
        return targetSeats.all { seat ->
            vehiclePropertyHelper.setBoolProperty(switchConfig.getPropertyId(), seat, true)
        }
    }

    private fun setMassagePower(targetSeats: Set<Int>, power: Int): Boolean {
        if (!turnOnMassage(targetSeats)) return false

        val clampedPower = power.coerceIn(CarPropertyRegistry.Seat.MIN_POWER, CarPropertyRegistry.Seat.MAX_POWER)

        return targetSeats.all { seat ->
            vehiclePropertyHelper.setIntProperty(powerConfig.getPropertyId(), seat, clampedPower)
        }
    }

    private fun setMassageType(targetSeats: Set<Int>, type: Int): Boolean {
        if (!turnOnMassage(targetSeats)) return false

        val clampedType = type.coerceIn(1, maxType)

        return targetSeats.all { seat ->
            val currentPower = vehiclePropertyHelper.getIntProperty(powerConfig.getPropertyId(), seat)
            if (currentPower == 0) {
                setMassagePower(setOf(seat), 2)
            }
            vehiclePropertyHelper.setIntProperty(typeConfig.getPropertyId(), seat, clampedType)
        }
    }
}