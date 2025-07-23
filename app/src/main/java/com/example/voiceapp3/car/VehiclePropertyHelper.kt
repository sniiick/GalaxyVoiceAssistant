package com.example.voiceapp3.car
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log

class VehiclePropertyHelper(context: Context) {
    private val TAG: String = "VehiclePropertyHelper"
    private var car: Car? = null
    var propertyManager: CarPropertyManager? = null

    init {
        try {
            car = Car.createCar(context)
            car?.let {
                propertyManager = it.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to car service", e)
        }
    }

    fun getIntProperty(propertyId: Int, areaId: Int): Int {
        return try {
            propertyManager?.let { pm ->
                if (pm.isPropertyAvailable(propertyId, areaId)) {
                    pm.getIntProperty(propertyId, areaId)
                } else {
                    Log.e(TAG, "Property not available or not writable for area $areaId")
                    -1
                }
            } ?: run {
                Log.e(TAG, "Property manager not available")
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting float property", e)
            -1
        }
    }

    fun getBoolProperty(propertyId: Int, areaId: Int): Boolean {
        return try {
            propertyManager?.let { pm ->

                if (pm.isPropertyAvailable(propertyId, areaId)) {
                    pm.getBooleanProperty(propertyId, areaId)
                } else {
                    Log.e(TAG, "Property not available or not writable for area $areaId")
                    false
                }
            } ?: run {
                Log.e(TAG, "Property manager not available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting boolean property", e)
            false
        }
    }

    fun getFloatProperty(propertyId: Int, areaId: Int): Float {
        return try {
            propertyManager?.let { pm ->

                if (pm.isPropertyAvailable(propertyId, areaId)) {
                    pm.getFloatProperty(propertyId, areaId)
                } else {
                    Log.e(TAG, "Property not available or not writable for area $areaId")
                    -1f
                }
            } ?: run {
                Log.e(TAG, "Property manager not available")
                -1f
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting float property", e)
            -1f
        }
    }

    fun setIntProperty(propertyId: Int, areaId: Int, value: Int): Boolean {
        Log.i(TAG, "Setting property $propertyId in area $areaId to $value")
        return try {
            propertyManager?.let { pm ->

                if (pm.isPropertyAvailable(propertyId, areaId)) {
                    pm.setIntProperty(propertyId, areaId, value)
                    true
                } else {
                    Log.e(TAG, "Property not available or not writable for area $areaId")
                    false
                }
            } ?: run {
                Log.e(TAG, "Property manager not available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting float property", e)
            false
        }
    }

    fun setBoolProperty(propertyId: Int, areaId: Int, value: Boolean): Boolean {
        Log.i(TAG, "Setting property $propertyId in area $areaId to $value")
        return try {
            propertyManager?.let { pm ->

                if (pm.isPropertyAvailable(propertyId, areaId)) {
                    pm.setBooleanProperty(propertyId, areaId, value)
                    true
                } else {
                    Log.e(TAG, "Property not available or not writable for area $areaId")
                    false
                }
            } ?: run {
                Log.e(TAG, "Property manager not available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting boolean property", e)
            false
        }
    }

    fun setFloatProperty(propertyId: Int, areaId: Int, value: Float): Boolean {
        Log.i(TAG, "Setting property $propertyId in area $areaId to $value")
        return try {
            propertyManager?.let { pm ->

                if (pm.isPropertyAvailable(propertyId, areaId)) {
                    pm.setFloatProperty(propertyId, areaId, value)
                    true
                } else {
                    Log.e(TAG, "Property not available or not writable for area $areaId")
                    false
                }
            } ?: run {
                Log.e(TAG, "Property manager not available")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting float property", e)
            false
        }
    }
    fun disconnect() {
        car?.disconnect()
        car = null
        propertyManager = null
    }
}