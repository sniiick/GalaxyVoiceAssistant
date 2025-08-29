package com.example.voiceapp3.tools

import android.annotation.SuppressLint
import android.util.Log

enum class ModelEnum(val value: String) {
    STARSHIP("P145"),
    E5("E245"),
    E5v2("E5"),
    EX5("EX5"),
    COOLRAY("IHU624G"),
    UNKNOWN("UNKNOWN")
}


object CarModel {
    @SuppressLint("PrivateApi")
    fun getCarModel(): ModelEnum {
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)

            val propertiesToCheck = listOf(
                "ro.product.name",
                "ro.product.model",
                "ro.product.device",
                "ro.product.system.name",
                "ro.product.system.model",
                "ro.product.system.device",
                "ro.product.vendor.name",
                "ro.product.vendor.model",
                "ro.product.vendor.device",
            )
            for (propertyName in propertiesToCheck) {
                try {
                    val value = getMethod.invoke(null, propertyName) as? String

                    if (!value.isNullOrBlank() && value != " ") {
                        ModelEnum.values().forEach {
                            if (value.lowercase().contains(it.value.lowercase())) {
                                Log.i("CarModel", "Selected MODEL: ${getModelName(it)}")
                                return it
                            }
                        }
                    }
                } catch (_: Exception) {
                    continue
                }
            }

        } catch (e: Exception) {
            Log.e("CarModel", "Failed to get car model", e)
        }
        return ModelEnum.UNKNOWN
    }

    fun getModelName(modelEnum: ModelEnum): String {
        return when (modelEnum) {
            ModelEnum.E5, ModelEnum.E5v2, ModelEnum.EX5  -> "Geely Galaxy E5"
            ModelEnum.STARSHIP -> "Geely Galaxy Starship 7"
            ModelEnum.COOLRAY -> "Geely CoolRay"
            ModelEnum.UNKNOWN -> "Unknown Model"
        }
    }

    val isCoolray: Boolean get() = this.getCarModel() == ModelEnum.COOLRAY
    val isE5: Boolean get() = this.getCarModel() == ModelEnum.E5 ||
                              this.getCarModel() == ModelEnum.E5v2 ||
                              this.getCarModel() == ModelEnum.EX5
}