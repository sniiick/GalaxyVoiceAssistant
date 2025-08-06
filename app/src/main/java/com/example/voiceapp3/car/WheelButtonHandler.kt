package com.example.voiceapp3.car

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent

// TODO("To be deleted or transformed to PropertyListener")
class WheelButtonHandler(private val context: Context, private val car: Car?) {
    private val TAG = "WheelButtonHandler"
    private val WHEEL_KEYS = mapOf(
        557872226 to "LEFT",    // WHEEL_HARD_KEY_LEFT
        557872227 to "RIGHT",   // WHEEL_HARD_KEY_RIGHT
        557872232 to "CONFIRM", // WHEEL_HARD_KEY_CONFIRM
    )
    private val propertyManager: CarPropertyManager by lazy {
        car?.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
    }

    // Track button states to prevent duplicate events
    private val buttonStates = mutableMapOf<String, Boolean>().apply {
        WHEEL_KEYS.values.forEach { put(it, false) }
    }

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            Log.i(TAG, "EVENT: ${value.propertyId}, ${value.value}")
            WHEEL_KEYS[value.propertyId]?.let { buttonName ->
                val state = value.value as Int
                val isPressed = state == 1 // 1 = pressed, 2 = released

                // Only handle if state changed
                if (buttonStates[buttonName] != isPressed) {
                    buttonStates[buttonName] = isPressed

                    when (buttonName) {
                        "LEFT" -> {
                            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, isPressed)
                        }
                        "RIGHT" -> {
                            sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, isPressed)
                        }
                        "CONFIRM" -> {
                            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, isPressed)
                        }
                    }
                }
            }
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.e(TAG, "Error event for property: $propId, zone: $zone")
        }
    }

    fun startListening() {
            Log.i(TAG, "Starting wheel button listener")
        registerCallbacks()
    }

    fun registerCallbacks() {
        WHEEL_KEYS.keys.forEach { propId ->
            try {
                propertyManager.registerCallback(
                    callback,
                    propId,
                    CarPropertyManager.SENSOR_RATE_ONCHANGE
                )
                Log.i(TAG, "Registered callback for $propId (${WHEEL_KEYS[propId]})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register callback for $propId", e)
            }
        }
    }

    private fun sendMediaKey(keyCode: Int, isPressed: Boolean) {
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val event = KeyEvent(
            if (isPressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
            keyCode
        )
        try {
            audioManager.dispatchMediaKeyEvent(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending ${if (isPressed) "PRESS" else "RELEASE"} for key $keyCode: $e")
        }
        Log.i(TAG, "Sent ${if (isPressed) "PRESS" else "RELEASE"} for key $keyCode")
    }

    fun stopListening() {
        try {
            WHEEL_KEYS.keys.forEach { propId ->
                propertyManager.unregisterCallback(callback, propId)
            }
            Log.i(TAG, "Stopped listening to wheel buttons")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering callbacks", e)
        }
    }
}