package com.example.voiceapp3.handlers

import android.util.Log
import com.example.voiceapp3.IntentHandler
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.tools.CarPropertyRegistry

class AcControlHandler(private val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG = "AcControlHandler"

    private val acOnConfig = CarPropertyRegistry.Hvac.AC_ON
    private val acAutoConfig = CarPropertyRegistry.Hvac.AC_AUTO
    private val acPowerConfig = CarPropertyRegistry.Hvac.AC_POWER
    private val fanDirectionConfig = CarPropertyRegistry.Hvac.FAN_DIRECTION
    private val windowDefrosterConfig = CarPropertyRegistry.Hvac.WINDOW_DEFROSTER

    override fun canHandle(intent: String): Boolean = intent == "ac_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> turnOnAc()
            "unset" -> turnOffAc()
            else -> false
        }
    }

    fun turnOnAc(setAuto: Boolean = true): Boolean {
        Log.d(TAG, "Turning AC ON")
        val acOn = vehiclePropertyHelper.setBoolProperty(
            acOnConfig.getPropertyId(),
            acOnConfig.getAreaId(),
            true
        )

        val acAutoOn = if (setAuto) {
            vehiclePropertyHelper.setBoolProperty(
                acAutoConfig.getPropertyId(),
                acAutoConfig.getAreaId(),
                true
            )
        } else {
            true
        }

        val acPowerOn = vehiclePropertyHelper.setBoolProperty(
            acPowerConfig.getPropertyId(),
            acPowerConfig.getAreaId(),
            true
        )

        return acOn && acAutoOn && acPowerOn
    }

    fun turnOffAc(): Boolean {
        Log.d(TAG, "Turning AC OFF")
        val acOn = vehiclePropertyHelper.setBoolProperty(
            acOnConfig.getPropertyId(),
            acOnConfig.getAreaId(),
            false
        )
        val acAutoOn = vehiclePropertyHelper.setBoolProperty(
            acAutoConfig.getPropertyId(),
            acAutoConfig.getAreaId(),
            false
        )
        val acPowerOn = vehiclePropertyHelper.setBoolProperty(
            acPowerConfig.getPropertyId(),
            acPowerConfig.getAreaId(),
            false
        )
        return acOn && acAutoOn && acPowerOn
    }

    fun setAcDirection(direction: Int): Boolean {
        return vehiclePropertyHelper.setIntProperty(
            fanDirectionConfig.getPropertyId(),
            fanDirectionConfig.getAreaId(),
            direction
        )
    }

    fun setWindowHeat(on: Boolean): Boolean {
        return vehiclePropertyHelper.setBoolProperty(
            windowDefrosterConfig.getPropertyId(),
            windowDefrosterConfig.getAreaId(),
            on
        )
    }
}