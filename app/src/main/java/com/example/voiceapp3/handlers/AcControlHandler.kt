package com.example.voiceapp3.handlers

import android.car.VehiclePropertyIds
import android.util.Log
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.VehiclePropertyHelper

class AcControlHandler(val vehiclePropertyHelper: VehiclePropertyHelper) : IntentHandler {
    private val TAG: String? = "AcControlHandler"

    override fun canHandle(intent: String): Boolean = intent in "ac_control"

    override fun handle(prediction: PredictionResult): Boolean {
        val params = extractCommonEntities(prediction)

        return when (params.action) {
            "set" -> turnOnAc()
            "unset" -> turnOffAc()
            else -> false
        }
    }

    fun turnOnAc(setAuto: Boolean = true): Boolean {
        Log.i(TAG, "Turning AC ON")
        val acOn = vehiclePropertyHelper.setBoolProperty(
            VehiclePropertyIds.HVAC_AC_ON,
            117,
            true
        )

        val acAutoOn = if (setAuto) {
            vehiclePropertyHelper.setBoolProperty(
                VehiclePropertyIds.HVAC_AUTO_ON,
                1,
                true
            )
        } else {
            true
        }
        val acPowerOn = vehiclePropertyHelper.setBoolProperty(
            VehiclePropertyIds.HVAC_POWER_ON,
            5,
            true
        )
        return acOn && acAutoOn and acPowerOn
    }

    fun turnOffAc(): Boolean {
        Log.i(TAG, "Turning AC OFF")
        val acOn = vehiclePropertyHelper.setBoolProperty(
            VehiclePropertyIds.HVAC_AC_ON,
            117,
            false
        )
        val acAutoOn = vehiclePropertyHelper.setBoolProperty(
            VehiclePropertyIds.HVAC_AUTO_ON,
            1,
            false
        )
        val acPowerOn = vehiclePropertyHelper.setBoolProperty(
            VehiclePropertyIds.HVAC_POWER_ON,
            5,
            false
        )
        return acOn && acAutoOn and acPowerOn
    }
}