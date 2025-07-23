package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class WindowControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
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

    // Regex patterns for special cases
    private val sunroofRegex = Regex("люк|крыш[ауеи]|верхнее|сверху|наверху", RegexOption.IGNORE_CASE)
    private val curtainRegex = Regex("шторк[ау]", RegexOption.IGNORE_CASE)
    private val pluralRegex = Regex("окна|ст[ёе]кла", RegexOption.IGNORE_CASE)

    override fun canHandle(intent: String): Boolean = intent == "window_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when {
            // Handle sunroof cases
            sunroofRegex.containsMatchIn(prediction.normalizedText) -> handleSunroof(params)
            curtainRegex.containsMatchIn(prediction.normalizedText) -> handleCurtain(params)
            else -> handleWindows(params, prediction)
        }
    }

    private fun handleSunroof(params: CommandParams): Boolean {
        return when (params.action) {
            "set" -> {
                // When opening sunroof, also open curtain
                val success = setWindowValue(SUNROOF_CURTAIN, MAX_VALUE)
                success && setWindowValue(SUNROOF, params.value ?: MAX_VALUE)
            }
            "unset" -> {
                // When closing sunroof, don't touch curtain
                setWindowValue(SUNROOF, MIN_VALUE)
            }
            else -> false
        }
    }

    private fun handleCurtain(params: CommandParams): Boolean {
        return when (params.action) {
            "set" -> setWindowValue(SUNROOF_CURTAIN, params.value ?: MAX_VALUE)
            "unset" -> setWindowValue(SUNROOF_CURTAIN, MIN_VALUE)
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
                "set" -> params.value ?: MAX_VALUE
                "unset" -> MIN_VALUE
                else -> return false
            }
            success = success && setWindowValue(area, value)
        }
        return success
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
        val clampedValue = when {
            value > MAX_VALUE -> MAX_VALUE
            value < MIN_VALUE -> MIN_VALUE
            else -> value - (value % VALUE_STEP) // ensure step of 4
        }

        Log.i(TAG, "Setting window area $areaId to $clampedValue")
        return vehiclePropertyHelper.setIntProperty(
            WINDOW_CONTROL,
            areaId,
            clampedValue
        )
    }
}