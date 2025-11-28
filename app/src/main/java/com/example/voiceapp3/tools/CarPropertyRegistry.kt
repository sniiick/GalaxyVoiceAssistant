package com.example.voiceapp3.tools

import android.car.VehicleAreaSeat
import android.car.VehiclePropertyIds

data class PropertyConfig(
    val starship: Int,
    val e5: Int,
    val coolray: Int,
    val areaStarship: Int = 0,
    val areaE5: Int = 0,
    val areaCoolray: Int = 0
) {
    fun getPropertyId(): Int = when (CarModel.getCarModel()) {
        ModelEnum.STARSHIP -> starship
        ModelEnum.E5, ModelEnum.E5v2, ModelEnum.EX5 -> e5
        ModelEnum.COOLRAY -> coolray
        ModelEnum.UNKNOWN -> starship
    }

    fun getAreaId(): Int = when (CarModel.getCarModel()) {
        ModelEnum.STARSHIP -> areaStarship
        ModelEnum.E5, ModelEnum.E5v2, ModelEnum.EX5 -> areaE5
        ModelEnum.COOLRAY -> areaCoolray
        ModelEnum.UNKNOWN -> areaStarship
    }
}

object CarPropertyRegistry {

    object Hvac {
        val AC_ON = PropertyConfig(
            starship = VehiclePropertyIds.HVAC_AC_ON,
            e5 = VehiclePropertyIds.HVAC_AC_ON,
            coolray = VehiclePropertyIds.HVAC_AC_ON,
            areaStarship = 117,
            areaE5 = 117,
            areaCoolray = 117
        )

        val AC_AUTO = PropertyConfig(
            starship = VehiclePropertyIds.HVAC_AUTO_ON,
            e5 = VehiclePropertyIds.HVAC_AUTO_ON,
            coolray = VehiclePropertyIds.HVAC_AUTO_ON,
            areaStarship = 1,
            areaE5 = 1,
            areaCoolray = 1
        )

        val AC_POWER = PropertyConfig(
            starship = VehiclePropertyIds.HVAC_POWER_ON,
            e5 = VehiclePropertyIds.HVAC_POWER_ON,
            coolray = VehiclePropertyIds.HVAC_POWER_ON,
            areaStarship = 5,
            areaE5 = 5,
            areaCoolray = 5
        )

        val TEMPERATURE = PropertyConfig(
            starship = VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            e5 = VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            coolray = VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            areaStarship = VehicleAreaSeat.SEAT_ROW_1_LEFT,
            areaE5 = VehicleAreaSeat.SEAT_ROW_1_LEFT,
            areaCoolray = VehicleAreaSeat.SEAT_ROW_1_LEFT
        )

        val FAN_SPEED = PropertyConfig(
            starship = VehiclePropertyIds.HVAC_FAN_SPEED,
            e5 = VehiclePropertyIds.HVAC_FAN_SPEED,
            coolray = VehiclePropertyIds.HVAC_FAN_SPEED,
            areaStarship = 5,
            areaE5 = 5,
            areaCoolray = 5
        )

        val FAN_DIRECTION = PropertyConfig(
            starship = VehiclePropertyIds.HVAC_FAN_DIRECTION,
            e5 = VehiclePropertyIds.HVAC_FAN_DIRECTION,
            coolray = VehiclePropertyIds.HVAC_FAN_DIRECTION,
            areaStarship = 1,
            areaE5 = 1,
            areaCoolray = 1
        )

        val WINDOW_DEFROSTER = PropertyConfig(
            starship = 354419988,
            e5 = 354419988,
            coolray = 354419988,
            areaStarship = 2,
            areaE5 = 2,
            areaCoolray = 2
        )

        const val MIN_TEMP = 15.5f
        const val MAX_TEMP = 28.5f
        const val MIN_FAN_SPEED_STARSHIP = 1
        const val MAX_FAN_SPEED_STARSHIP = 9
        const val MAX_FAN_SPEED_COOLRAY = 8
    }

    object Window {
        val CONTROL = PropertyConfig(
            starship = 322964416,
            e5 = 322964416,
            coolray = 591405227
        )

        const val LEFT_FRONT = 16
        const val RIGHT_FRONT = 64
        const val LEFT_REAR = 256
        const val RIGHT_REAR = 1024
        const val SUNROOF = 65536
        const val SUNROOF_CURTAIN = 131072

        const val MIN_VALUE = 0
        const val MAX_VALUE = 100
        const val VALUE_STEP = 4

        const val MIN_WINDOW_OPERATIONAL = 12
        const val MAX_WINDOW_OPERATIONAL = 88
        const val MIN_CURTAIN_OPERATIONAL_COOLRAY = 16
    }

    object Trunk {
        val CONTROL = PropertyConfig(
            starship = 373295873,
            e5 = 373295873,
            coolray = 554768640,
            areaStarship = 536870912,
            areaE5 = 536870912,
            areaCoolray = 0
        )

        const val OPEN = 1
        const val CLOSE = 0
    }

    object Seat {
        object Massage {
            val SWITCH = PropertyConfig(
                starship = 622883040,
                e5 = 622883021,
                coolray = -1
            )

            val POWER = PropertyConfig(
                starship = 624980189,
                e5 = 624980170,
                coolray = -1
            )

            val TYPE = PropertyConfig(
                starship = 624980193,
                e5 = 624980174,
                coolray = -1
            )

            const val MAX_TYPE_STARSHIP = 8
            const val MAX_TYPE_E5 = 6
        }

        object Climate {
            val VENTILATION = PropertyConfig(
                starship = 356517139,
                e5 = 356517139,
                coolray = 356517139
            )

            val HEAT = PropertyConfig(
                starship = 356517131,
                e5 = 356517131,
                coolray = 356517131
            )
        }

        const val PILOT = 1
        const val PASSENGER = 4
        const val MIN_POWER = 1
        const val MAX_POWER = 3
    }

    object Light {
        val INTERIOR = PropertyConfig(
            starship = 356544592,
            e5 = 356544592,
            coolray = 356519684
        )

        val EXTERIOR = PropertyConfig(
            starship = 557871126,
            e5 = 557871126,
            coolray = 557871126
        )

        val FOG = PropertyConfig(
            starship = 289410578,
            e5 = 289410578,
            coolray = 289410578
        )

        val SUPPORTED_AREAS = listOf(1, 2, 4, 16, 64)

        const val HEADLIGHTS_ON = 3
        const val PARKING_ON = 1
        const val OFF = 0
    }

    object Screen {
        val BRIGHTNESS = PropertyConfig(
            starship = 624981307,
            e5 = 624981307,
            coolray = 687997952,
            areaStarship = 2,
            areaE5 = 2,
            areaCoolray = 2
        )

        val AUTO_BRIGHTNESS = PropertyConfig(
            starship = 555775150,
            e5 = 555775150,
            coolray = 555775150
        )

        const val MIN_BRIGHTNESS = 0
        const val MAX_BRIGHTNESS = 150
    }

    object Fuel {
        val DOOR = PropertyConfig(
            starship = 287310600,
            e5 = 287310600,
            coolray = 287310600
        )

        val CHARGING = PropertyConfig(
            starship = 555774850,
            e5 = 555774850,
            coolray = 555774850
        )
    }

    object DriveMode {
        val MODE = PropertyConfig(
            starship = 557871372,
            e5 = 557871372,
            coolray = 557871372
        )

        const val ECO = 24
        const val ELECTRIC = 16
        const val HYBRID = 0
        const val SPORT = 2
    }

    object Vehicle {
        val SPEED = PropertyConfig(
            starship = VehiclePropertyIds.PERF_VEHICLE_SPEED,
            e5 = VehiclePropertyIds.PERF_VEHICLE_SPEED,
            coolray = VehiclePropertyIds.PERF_VEHICLE_SPEED
        )
    }
}

