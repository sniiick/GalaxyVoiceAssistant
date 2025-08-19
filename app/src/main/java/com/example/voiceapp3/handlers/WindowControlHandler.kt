package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class WindowControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG: String? = "WindowControlHandler"

    // Property ID for all windows
    private val WINDOW_CONTROL = 322964416

    // Area IDs
    private val LEFT_FRONT_WINDOW = 16
    private val RIGHT_FRONT_WINDOW = 64
    private val LEFT_REAR_WINDOW = 256
    private val RIGHT_REAR_WINDOW = 1024
    private val SUNROOF = 65536
    private val SUNROOF_CURTAIN = 131072

    // Value constraints
    private val MIN_VALUE = 0
    private val MAX_VALUE = 100
    private val VALUE_STEP = 4
    private val MIN_OPERATIONAL_VALUE = 12
    private val MAX_OPERATIONAL_VALUE = 88

    // Regex patterns for special cases
    private val sunroofRegex = Regex("люк|крыш[ауеи]|верхнее|сверху|наверху", RegexOption.IGNORE_CASE)
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
            // Handle sunroof cases. Curtain is first (case "открой шторку люка")
            curtainRegex.containsMatchIn(prediction.normalizedText) -> handleCurtain(params)
            sunroofRegex.containsMatchIn(prediction.normalizedText) -> handleSunroof(params)
            else -> handleWindows(params, prediction)
        }
    }

    private fun handleSunroof(params: CommandParams): Boolean {
        return when (params.action) {
            "set" -> {
                // When opening sunroof, also open curtain
                val success = setWindowValue(SUNROOF_CURTAIN, MAX_VALUE)
                success && setWindowValue(SUNROOF, calculateFinalValue(params.value ?: MAX_VALUE, false))
            }
            "unset" -> {
                // When closing sunroof, don't touch curtain
                setWindowValue(SUNROOF, calculateFinalValue(params.value ?: MAX_VALUE, true))
            }
            else -> false
        }
    }

    private fun handleCurtain(params: CommandParams): Boolean {
        return when (params.action) {
            "set" -> setWindowValue(SUNROOF_CURTAIN, calculateFinalValue(params.value ?: MAX_VALUE, false))
            "unset" -> setWindowValue(SUNROOF_CURTAIN, calculateFinalValue(params.value ?: MAX_VALUE, true))
            else -> false
        }
    }

    private fun handleWindows(params: CommandParams, prediction: PredictionResult): Boolean {
        val targetAreas = determineTargetAreas(prediction)

        // Special case: close all windows including sunroof if no specific target and full close
        if (targetAreas.isEmpty() && params.action == "unset" && (params.value == null || params.value == MIN_VALUE)) {
            var success = true
            val allWindows = setOf(
                LEFT_FRONT_WINDOW, RIGHT_FRONT_WINDOW,
                LEFT_REAR_WINDOW, RIGHT_REAR_WINDOW,
                SUNROOF
            )
            for (area in allWindows) {
                success = success && setWindowValue(area, MIN_VALUE)
            }
            return success
        }

        var success = true
        for (area in targetAreas) {
            val value = when (params.action) {
                "set" -> calculateFinalValue(params.value ?: MAX_VALUE, false)
                "unset" -> calculateFinalValue(params.value ?: MAX_VALUE, true)
                else -> return false
            }
            success = success && setWindowValue(area, value)
        }
        return success
    }

    private fun calculateFinalValue(requestedValue: Int, isCloseCommand: Boolean): Int {
        val correctedValue = if (aBit) {
            MIN_OPERATIONAL_VALUE
        } else if (requestedValue > 100) {
            MAX_VALUE
        } else if (requestedValue < 0) {
            MIN_VALUE
        } else {
            requestedValue
        }

        val baseValue = if (isCloseCommand) {
            MAX_VALUE - correctedValue
        } else {
            correctedValue
        }

        val steppedValue = baseValue - (baseValue % VALUE_STEP)

        return when {
            steppedValue == 0 -> {
                MIN_VALUE
            }
            steppedValue == 100 -> {
                MAX_VALUE
            }
            steppedValue < MIN_OPERATIONAL_VALUE -> {
                if (MIN_OPERATIONAL_VALUE % VALUE_STEP == 0) MIN_OPERATIONAL_VALUE
                else MIN_OPERATIONAL_VALUE + (VALUE_STEP - MIN_OPERATIONAL_VALUE % VALUE_STEP)
            }
            steppedValue > MAX_OPERATIONAL_VALUE -> {
                MAX_OPERATIONAL_VALUE - (MAX_OPERATIONAL_VALUE % VALUE_STEP)
            }
            else -> steppedValue
        }
    }

    private fun determineTargetAreas(prediction: PredictionResult): Set<Int> {
        val direction = prediction.getString("direction")
        val position = prediction.getString("position")
        val isPlural = pluralRegex.containsMatchIn(prediction.normalizedText)

        // Determine final direction and position considering defaults and plural forms
        val finalDirection = when {
            direction != null -> direction
            isPlural -> "both"
            else -> "left" // default direction
        }

        val finalPosition = when {
            position != null -> position
            isPlural -> "both"
            else -> "front" // default position
        }

        return when (finalPosition) {
            "front" -> when (finalDirection) {
                "left" -> setOf(LEFT_FRONT_WINDOW)
                "right" -> setOf(RIGHT_FRONT_WINDOW)
                "both" -> setOf(LEFT_FRONT_WINDOW, RIGHT_FRONT_WINDOW)
                else -> setOf(LEFT_FRONT_WINDOW) // fallback
            }
            "rear" -> when (finalDirection) {
                "left" -> setOf(LEFT_REAR_WINDOW)
                "right" -> setOf(RIGHT_REAR_WINDOW)
                "both" -> setOf(LEFT_REAR_WINDOW, RIGHT_REAR_WINDOW)
                else -> setOf(LEFT_REAR_WINDOW) // fallback
            }
            "both" -> when (finalDirection) {
                "left" -> setOf(LEFT_FRONT_WINDOW, LEFT_REAR_WINDOW)
                "right" -> setOf(RIGHT_FRONT_WINDOW, RIGHT_REAR_WINDOW)
                "both" -> setOf(
                    LEFT_FRONT_WINDOW, RIGHT_FRONT_WINDOW,
                    LEFT_REAR_WINDOW, RIGHT_REAR_WINDOW
                )
                else -> setOf(LEFT_FRONT_WINDOW, LEFT_REAR_WINDOW) // fallback
            }
            else -> setOf(LEFT_FRONT_WINDOW)
        }
    }

    private fun setWindowValue(areaId: Int, value: Int): Boolean {
        Log.i(TAG, "Setting window area $areaId to $value")
        return vehiclePropertyHelper.setIntProperty(
            WINDOW_CONTROL,
            areaId,
            value
        )
    }
}