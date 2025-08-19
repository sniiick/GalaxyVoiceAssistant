package com.example.voiceapp3.tools

import android.annotation.SuppressLint

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
            val modelValue = Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, "ro.product.system.model") as? String

            ModelEnum.values().firstOrNull { it.value == modelValue } ?: ModelEnum.UNKNOWN

        } catch (_: Exception) {
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