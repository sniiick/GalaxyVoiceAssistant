#!/usr/bin/env bash
adb uninstall --user 0 com.example.voiceapp3 
adb push app/release/app-release.apk /data/local/tmp/app-release.apk
adb shell pm install -r -g --user 0 /data/local/tmp/app-release.apk
adb shell pm grant com.example.voiceapp3 android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.example.voiceapp3 android.permission.RECORD_AUDIO

adb shell am start-foreground-service -n com.example.voiceapp3/.VoiceAssistantService
