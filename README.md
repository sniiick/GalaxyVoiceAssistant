Установка

adb root
adb remount
adb uninstall --user 0 com.example.voiceapp3
adb shell mkdir /system/priv-app/VoiceAssistant
adb push app-release.apk /system/priv-app/VoiceAssistant/
adb reboot
adb shell pm grant com.example.voiceapp3 android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.example.voiceapp3 android.permission.RECORD_AUDIO
