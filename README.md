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

# поместить библиотеки в систему
adb push libonnxruntime4j_jni.so /system/lib64/
adb push libonnxruntime.so /system/lib64/
adb push libjnidispatch.so /system/lib64/
adb push libvosk.so /system/lib64/
```


Обновление
```shell
adb root
adb install --user 0 app-release.apk
adb shell am start-foreground-service --user 0 -n com.example.voiceapp3/.VoiceAssistantService
```



### Powered by DeepSeek production =)