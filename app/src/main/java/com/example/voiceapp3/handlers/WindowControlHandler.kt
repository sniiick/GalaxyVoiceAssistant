package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.CommandParams
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarModel
import com.example.voiceapp3.tools.ModelEnum

class WindowControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) :
    IntentHandler {
    private val TAG: String? = "WindowControlHandler"

    // Property ID for all windows
    private var WINDOW_CONTROL = 322964416

    // Area IDs
    private val LEFT_FRONT_WINDOW = 16
    private val RIGHT_FRONT_WINDOW = 64
    private val LEFT_REAR_WINDOW = 256
    private val RIGHT_REAR_WINDOW = 1024
    private val SUNROOF = 65536
    private val SUNROOF_CURTAIN = 131072

    // Value constraints
    private var MIN_VALUE = 0
    private val MAX_VALUE = 100
    private val VALUE_STEP = 4

    // Real min/max values differs to set from CarPropertyManager
    private var MIN_WINDOW_OPERATIONAL_VALUE = 12
    private var MIN_SUNROOF_OPERATIONAL_VALUE = 12
    private var MIN_CURTAIN_OPERATIONAL_VALUE = 12
    private var MAX_WINDOW_OPERATIONAL_VALUE = 88
    private var MAX_SUNROOF_OPERATIONAL_VALUE = 88
    private var MAX_CURTAIN_OPERATIONAL_VALUE = 88

    // Regex patterns for special cases
    private val sunroofRegex = Regex("sunroof|roof|top|panorama", RegexOption.IGNORE_CASE)
    private val curtainRegex = Regex("curtain|shade", RegexOption.IGNORE_CASE)
    private val pluralRegex = Regex("windows|glass", RegexOption.IGNORE_CASE)
    private val aBitRegex = Regex("a bit|slightly|crack", RegexOption.IGNORE_CASE)
    private var aBit: Boolean = false

    override fun canHandle(intent: String): Boolean = intent == "window_control"

    override fun handle(prediction: PredictionResult): Boolean {
        var params = extractCommonEntities(prediction)

        if (CarModel.isCoolray) {
            WINDOW_CONTROL = 591405227
            MIN_CURTAIN_OPERATIONAL_VALUE = 16
        }

        if (aBitRegex.containsMatchIn(prediction.normalizedText) && params.value == null) {
            aBit = true
        }

        return when {
            // Handle sunroof cases. Curtain is first (case "open the sunroof curtain")
            curtainRegex.containsMatchIn(prediction.normalizedText) -> handleCurtain(params) // case "open the sunroof curtain"
            sunroofRegex.containsMatchIn(prediction.normalizedText) -> handleSunroof(params)
            else -> handleWindows(params, prediction)
        }
    }

    private fun handleSunroof(params: CommandParams): Boolean {
        return when (params.action) {
            "set" -> {
                val sunroofValue = calculateFinalValue(params.value ?: MAX_VALUE, false, sunRoof = true, curtain = false)

                // Calculate desired curtain value (sunroof + STEP, but not exceeding MAX_VALUE)
                val desiredCurtainValue = calculateFinalValue(minOf(sunroofValue + VALUE_STEP, MAX_VALUE), false, sunRoof = false, curtain = true)
                val currentCurtainValue = vehiclePropertyHelper.getIntProperty(WINDOW_CONTROL, SUNROOF_CURTAIN)

                setWindowValue(SUNROOF, sunroofValue)
                // Only adjust curtain if current value is less than desired value
                if (currentCurtainValue < desiredCurtainValue) {
                    setWindowValue(SUNROOF_CURTAIN, desiredCurtainValue)
                }
                true
            }
            "unset" -> {
                // When closing sunroof, don't touch curtain
                setWindowValue(SUNROOF, calculateFinalValue(params.value ?: MAX_VALUE, true, sunRoof = true, curtain = false))
                true
            }
            else -> false
        }
    }

    private fun handleCurtain(params: CommandParams): Boolean {
        return when (params.action) {
            "set" -> setWindowValue(SUNROOF_CURTAIN, calculateFinalValue(params.value ?: MAX_VALUE, false, sunRoof = false, curtain = true))
            "unset" -> setWindowValue(SUNROOF_CURTAIN, calculateFinalValue(params.value ?: MAX_VALUE, true, sunRoof = false, curtain = true))
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

    private fun calculateFinalValue(requestedValue: Int, isCloseCommand: Boolean, sunRoof: Boolean = false, curtain: Boolean = false): Int {
        var minOperationalValue = MIN_WINDOW_OPERATIONAL_VALUE
        var maxOperationalValue = MAX_WINDOW_OPERATIONAL_VALUE
        if (sunRoof) {
            minOperationalValue = MIN_SUNROOF_OPERATIONAL_VALUE
            maxOperationalValue = MAX_SUNROOF_OPERATIONAL_VALUE
        } else if (curtain) {
            minOperationalValue = MIN_CURTAIN_OPERATIONAL_VALUE
            maxOperationalValue = MAX_CURTAIN_OPERATIONAL_VALUE
        }

        val correctedValue = if (aBit) {
            minOperationalValue
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
            steppedValue < minOperationalValue -> {
                if (minOperationalValue % VALUE_STEP == 0) minOperationalValue
                else minOperationalValue + (VALUE_STEP - minOperationalValue % VALUE_STEP)
            }
            steppedValue > maxOperationalValue -> {
                maxOperationalValue - (maxOperationalValue % VALUE_STEP)
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
