Установка

```shell
adb root
adb remount
adb shell mkdir -p /system/priv-app/VoiceAssistant
adb push app-release.apk /system/priv-app/VoiceAssistant/
adb reboot

# только 1 раз после первой установки
adb shell pm grant com.example.voiceapp3 android.permission.SYSTEM_ALERT_WINDOW
adb shell pm grant com.example.voiceapp3 android.permission.RECORD_AUDIO
```
