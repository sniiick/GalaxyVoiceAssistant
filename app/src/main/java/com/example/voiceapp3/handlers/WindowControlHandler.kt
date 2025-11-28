package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.CarPropertyRegistry

class WindowControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG = "WindowControlHandler"

    private val windowConfig = CarPropertyRegistry.Window.CONTROL

    private val minCurtainOperational: Int
        get() = if (CarModel.isCoolray) {
            CarPropertyRegistry.Window.MIN_CURTAIN_OPERATIONAL_COOLRAY
        } else {
            CarPropertyRegistry.Window.MIN_WINDOW_OPERATIONAL
        }

    private val sunroofRegex = Regex("люк|крыш[ауеи]|верхнее|сверху|наверху|панорам[ау]]", RegexOption.IGNORE_CASE)
    private val curtainRegex = Regex("шторк[ау]", RegexOption.IGNORE_CASE)
    private val pluralRegex = Regex("окна|ст[ёе]кла", RegexOption.IGNORE_CASE)
    private val aBitRegex = Regex("чуть|при откр|приоткр|приподн|призакр|приопуст|немного|слегка", RegexOption.IGNORE_CASE)
    private var aBit: Boolean = false

    override fun canHandle(intent: String): Boolean = intent == "window_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        if (aBitRegex.containsMatchIn(prediction.normalizedText) && params.value == null) {
            aBit = true
        }

        return when {
            curtainRegex.containsMatchIn(prediction.normalizedText) -> handleCurtain(params)
            sunroofRegex.containsMatchIn(prediction.normalizedText) -> handleSunroof(params)
            else -> handleWindows(params, prediction)
        }
    }

    private fun handleSunroof(params: CommandParams): Boolean {
        return when (params.action) {
            "set" -> {
                val sunroofValue = calculateFinalValue(
                    params.value ?: CarPropertyRegistry.Window.MAX_VALUE,
                    false,
                    sunRoof = true,
                    curtain = false
                )

                val desiredCurtainValue = calculateFinalValue(
                    minOf(sunroofValue + CarPropertyRegistry.Window.VALUE_STEP, CarPropertyRegistry.Window.MAX_VALUE),
                    false,
                    sunRoof = false,
                    curtain = true
                )
                val currentCurtainValue = vehiclePropertyHelper.getIntProperty(
                    windowConfig.getPropertyId(),
                    CarPropertyRegistry.Window.SUNROOF_CURTAIN
                )

                setWindowValue(CarPropertyRegistry.Window.SUNROOF, sunroofValue)
                if (currentCurtainValue < desiredCurtainValue) {
                    setWindowValue(CarPropertyRegistry.Window.SUNROOF_CURTAIN, desiredCurtainValue)
                }
                true
            }
            "unset" -> {
                setWindowValue(
                    CarPropertyRegistry.Window.SUNROOF,
                    calculateFinalValue(
                        params.value ?: CarPropertyRegistry.Window.MAX_VALUE,
                        true,
                        sunRoof = true,
                        curtain = false
                    )
                )
                true
            }
            else -> false
        }
    }

    private fun handleCurtain(params: CommandParams): Boolean {
        val maxValue = CarPropertyRegistry.Window.MAX_VALUE
        return when (params.action) {
            "set" -> setWindowValue(
                CarPropertyRegistry.Window.SUNROOF_CURTAIN,
                calculateFinalValue(params.value ?: maxValue, false, sunRoof = false, curtain = true)
            )
            "unset" -> setWindowValue(
                CarPropertyRegistry.Window.SUNROOF_CURTAIN,
                calculateFinalValue(params.value ?: maxValue, true, sunRoof = false, curtain = true)
            )
            else -> false
        }
    }

    private fun handleWindows(params: CommandParams, prediction: PredictionResult): Boolean {
        val targetAreas = determineTargetAreas(prediction)

        if (targetAreas.isEmpty() && params.action == "unset" &&
            (params.value == null || params.value == CarPropertyRegistry.Window.MIN_VALUE)) {
            val allWindows = setOf(
                CarPropertyRegistry.Window.LEFT_FRONT,
                CarPropertyRegistry.Window.RIGHT_FRONT,
                CarPropertyRegistry.Window.LEFT_REAR,
                CarPropertyRegistry.Window.RIGHT_REAR,
                CarPropertyRegistry.Window.SUNROOF
            )
            return allWindows.all { setWindowValue(it, CarPropertyRegistry.Window.MIN_VALUE) }
        }

        return targetAreas.all { area ->
            val value = when (params.action) {
                "set" -> calculateFinalValue(params.value ?: CarPropertyRegistry.Window.MAX_VALUE, false)
                "unset" -> calculateFinalValue(params.value ?: CarPropertyRegistry.Window.MAX_VALUE, true)
                else -> return false
            }
            setWindowValue(area, value)
        }
    }

    private fun calculateFinalValue(
        requestedValue: Int,
        isCloseCommand: Boolean,
        sunRoof: Boolean = false,
        curtain: Boolean = false
    ): Int {
        val minOperational = when {
            curtain -> minCurtainOperational
            else -> CarPropertyRegistry.Window.MIN_WINDOW_OPERATIONAL
        }
        val maxOperational = CarPropertyRegistry.Window.MAX_WINDOW_OPERATIONAL

        val correctedValue = when {
            aBit -> minOperational
            requestedValue > 100 -> CarPropertyRegistry.Window.MAX_VALUE
            requestedValue < 0 -> CarPropertyRegistry.Window.MIN_VALUE
            else -> requestedValue
        }

        val baseValue = if (isCloseCommand) {
            CarPropertyRegistry.Window.MAX_VALUE - correctedValue
        } else {
            correctedValue
        }

        val step = CarPropertyRegistry.Window.VALUE_STEP
        val steppedValue = baseValue - (baseValue % step)

        return when {
            steppedValue == 0 -> CarPropertyRegistry.Window.MIN_VALUE
            steppedValue == 100 -> CarPropertyRegistry.Window.MAX_VALUE
            steppedValue < minOperational -> {
                if (minOperational % step == 0) minOperational
                else minOperational + (step - minOperational % step)
            }
            steppedValue > maxOperational -> maxOperational - (maxOperational % step)
            else -> steppedValue
        }
    }

    private fun determineTargetAreas(prediction: PredictionResult): Set<Int> {
        val direction = prediction.getString("direction")
        val position = prediction.getString("position")
        val isPlural = pluralRegex.containsMatchIn(prediction.normalizedText)

        val finalDirection = direction ?: if (isPlural) "both" else "left"
        val finalPosition = position ?: if (isPlural) "both" else "front"

        return when (finalPosition) {
            "front" -> when (finalDirection) {
                "left" -> setOf(CarPropertyRegistry.Window.LEFT_FRONT)
                "right" -> setOf(CarPropertyRegistry.Window.RIGHT_FRONT)
                "both" -> setOf(CarPropertyRegistry.Window.LEFT_FRONT, CarPropertyRegistry.Window.RIGHT_FRONT)
                else -> setOf(CarPropertyRegistry.Window.LEFT_FRONT)
            }
            "rear" -> when (finalDirection) {
                "left" -> setOf(CarPropertyRegistry.Window.LEFT_REAR)
                "right" -> setOf(CarPropertyRegistry.Window.RIGHT_REAR)
                "both" -> setOf(CarPropertyRegistry.Window.LEFT_REAR, CarPropertyRegistry.Window.RIGHT_REAR)
                else -> setOf(CarPropertyRegistry.Window.LEFT_REAR)
            }
            "both" -> when (finalDirection) {
                "left" -> setOf(CarPropertyRegistry.Window.LEFT_FRONT, CarPropertyRegistry.Window.LEFT_REAR)
                "right" -> setOf(CarPropertyRegistry.Window.RIGHT_FRONT, CarPropertyRegistry.Window.RIGHT_REAR)
                "both" -> setOf(
                    CarPropertyRegistry.Window.LEFT_FRONT, CarPropertyRegistry.Window.RIGHT_FRONT,
                    CarPropertyRegistry.Window.LEFT_REAR, CarPropertyRegistry.Window.RIGHT_REAR
                )
                else -> setOf(CarPropertyRegistry.Window.LEFT_FRONT, CarPropertyRegistry.Window.LEFT_REAR)
            }
            else -> setOf(CarPropertyRegistry.Window.LEFT_FRONT)
        }
    }

    private fun setWindowValue(areaId: Int, value: Int): Boolean {
        Log.i(TAG, "Setting window area $areaId to $value")
        return vehiclePropertyHelper.setIntProperty(
            windowConfig.getPropertyId(),
            areaId,
            value
        )
    }
}