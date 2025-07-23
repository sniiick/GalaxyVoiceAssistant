#!/usr/bin/env bash
adb uninstall --user 0 com.example.voiceapp3 
adb install --user 0 app/debug/app-debug.apk
adb shell pm grant com.example.voiceapp3 android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.example.voiceapp3 android.permission.RECORD_AUDIO
adb shell am start-foreground-service -n com.example.voiceapp3/.VoiceAssistantService
