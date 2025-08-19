package com.example.voiceapp3.tools

import android.annotation.SuppressLint
import android.util.Log

enum class ModelEnum(val value: String) {
    STARSHIP("P145"),
    E5("E245"),
    UNKNOWN("")
}


object CarModel {
    private const val CAR_EXTERNAL_USAGE_STARSHIP = 29
    private const val CAR_EXTERNAL_USAGE_E5 = 73
    private const val DEFAULT_USAGE = 1

    @SuppressLint("PrivateApi")
    fun getCarModel(): ModelEnum {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)

            val propertiesToCheck = listOf("ro.product.system.model", "ro.product.model")
            var modelValue: String? = null

            for (propertyName in propertiesToCheck) {
                val value = getMethod.invoke(null, propertyName) as? String
                Log.i("CarModel", "Property $propertyName = '$value'")

                if (!value.isNullOrBlank()) {
                    modelValue = value
                    break
                }
            }

            Log.i("CarModel", "Selected MODEL: $modelValue")
            ModelEnum.values().firstOrNull { it.value == modelValue } ?: ModelEnum.UNKNOWN

        } catch (e: Exception) {
            Log.e("CarModel", "Failed to get car model", e)
            ModelEnum.UNKNOWN
        }
    }

    fun getExternalUsage(): Int {
        return when (getCarModel()) {
            ModelEnum.E5 -> CAR_EXTERNAL_USAGE_E5
            ModelEnum.STARSHIP -> CAR_EXTERNAL_USAGE_STARSHIP
            ModelEnum.UNKNOWN -> DEFAULT_USAGE
        }
    }

    fun getModelName(): String {
        return when (getCarModel()) {
            ModelEnum.E5 -> "Geely Galaxy E5"
            ModelEnum.STARSHIP -> "Geely Galaxy Starship 7"
            ModelEnum.UNKNOWN -> "Unknown Model"
        }
    }
}