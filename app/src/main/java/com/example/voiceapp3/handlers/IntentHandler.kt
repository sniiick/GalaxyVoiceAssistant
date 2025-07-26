package com.example.voiceapp3.handlers

import android.content.Context
import com.example.voiceapp3.PredictionResult
import com.example.voiceapp3.car.CarAudioPlayer
import com.example.voiceapp3.car.VehiclePropertyHelper


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
        LightControlHandler(vehiclePropertyHelper)
    )

    fun register(handler: IntentHandler) {
        handlers.add(handler)
    }

    fun handle(prediction: PredictionResult): Boolean {
        return handlers.firstOrNull { it.canHandle(prediction.intent) }
            ?.handle(prediction) ?: false
    }
}
