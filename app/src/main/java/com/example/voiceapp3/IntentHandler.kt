package com.example.voiceapp3

import android.content.Context
import com.example.voiceapp3.car.CarAudioPlayer
import com.example.voiceapp3.car.VehiclePropertyHelper
import com.example.voiceapp3.handlers.AcControlHandler
import com.example.voiceapp3.handlers.ChangeAcTempHandler
import com.example.voiceapp3.handlers.ChangeFanSpeedHandler
import com.example.voiceapp3.handlers.ChangeScreenBrightnessHandler
import com.example.voiceapp3.handlers.DriveModeHandler
import com.example.voiceapp3.handlers.ExteriorLightControlHandler
import com.example.voiceapp3.handlers.ExternalSoundHandler
import com.example.voiceapp3.handlers.ExternalSpeechHandler
import com.example.voiceapp3.handlers.FuelCharingHandler
import com.example.voiceapp3.handlers.FuelDoorHandler
import com.example.voiceapp3.handlers.LightControlHandler
import com.example.voiceapp3.handlers.OpenAppHandler
import com.example.voiceapp3.handlers.SeatClimateHandler
import com.example.voiceapp3.handlers.SeatMassageHandler
import com.example.voiceapp3.handlers.TrunkControlHandler
import com.example.voiceapp3.handlers.WindowControlHandler
import com.example.voiceapp3.handlers.getAction
import com.example.voiceapp3.handlers.getUnit
import com.example.voiceapp3.handlers.getValue


interface IntentHandler {
    fun canHandle(intent: String): Boolean
    fun handle(prediction: PredictionResult): Boolean

    fun extractCommonEntities(prediction: PredictionResult): CommandParams {
        return CommandParams(
            action = prediction.getAction(),
            value = prediction.getValue(),
            unit = prediction.getUnit()
        )
    }
}

data class CommandParams(
    val action: String?,
    val value: Int?,
    val unit: String?
)


class IntentHandlerRegistry(vehiclePropertyHelper: VehiclePropertyHelper, carAudioPlayer: CarAudioPlayer, context: Context) {
    private val handlers = mutableListOf(
        AcControlHandler(vehiclePropertyHelper),
        TrunkControlHandler(vehiclePropertyHelper),
        ChangeAcTempHandler(vehiclePropertyHelper),
        ChangeFanSpeedHandler(vehiclePropertyHelper),
        OpenAppHandler(context),
        SeatMassageHandler(vehiclePropertyHelper),
        WindowControlHandler(vehiclePropertyHelper),
        SeatClimateHandler(vehiclePropertyHelper),
        FuelDoorHandler(vehiclePropertyHelper),
        ExternalSoundHandler(carAudioPlayer),
        ExternalSpeechHandler(carAudioPlayer),
        LightControlHandler(vehiclePropertyHelper),
        ExteriorLightControlHandler(vehiclePropertyHelper),
        ChangeScreenBrightnessHandler(vehiclePropertyHelper),
        FuelCharingHandler(vehiclePropertyHelper),
        DriveModeHandler(vehiclePropertyHelper),
    )

    fun register(handler: IntentHandler) {
        handlers.add(handler)
    }

    fun handle(prediction: PredictionResult): Boolean {
        return handlers.firstOrNull { it.canHandle(prediction.intent) }
            ?.handle(prediction) ?: false
    }
}
