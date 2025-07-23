#!/usr/bin/env bash
adb uninstall --user 0 com.example.voiceapp3 
adb push app/release/app-release.apk /data/local/tmp/app-release.apk
adb shell pm install -r -g --user 0 /data/local/tmp/app-release.apk
adb shell pm grant com.example.voiceapp3 android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.example.voiceapp3 android.permission.RECORD_AUDIO
#adb shell pm grant com.example.voiceapp3 android.car.permission.CONTROL_CAR_CLIMATE
#adb shell pm grant com.example.voiceapp3 android.car.permission.CAR_PROPERTY_READ_WRITE
#adb shell pm grant com.example.voiceapp3 android.car.permission.CONTROL_CAR_WINDOWS
#adb shell pm grant com.example.voiceapp3 android.car.permission.CAR_PROPERTY_ACCESS
#adb shell pm grant com.example.voiceapp3 android.car.permission.CAR_VENDOR_EXTENSION
#adb shell pm grant com.example.voiceapp3 android.car.permission.CAR_CONTROL_APP
#adb shell pm grant com.example.voiceapp3 com.flyme.auto.hvac.permission.FAN_SPEED_CONTROL
#adb shell pm grant com.example.voiceapp3 com.meizu.flymeauto.permission.VEHICLE_CONTROL

adb shell am start-foreground-service -n com.example.voiceapp3/.VoiceAssistantService
