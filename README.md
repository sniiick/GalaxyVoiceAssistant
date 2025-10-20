# Galaxy Voice Assistant - Installation and Usage

## Attention!
The voice assistant is developed by the community. All actions are performed at your own consent and risk.

ADB root and ADB remount allow you to perform irreversible actions with the car's firmware, the consequences can be critical and turn your tablet into a brick.

Remember: with great power comes great responsibility!

Despite the fact that the instructions and the voice assistant do not affect the standard car subsystems, if you are not confident in your actions, the creator of the assistant strongly does NOT(!) recommend independent installation.

## Attention!
This instruction is most likely not applicable to Russified firmwares, and to commercial firmwares that have voice assistants (EuropaCar, GMC). Installing this application may lead to unforeseen problems and conflicts, up to the loss of the installed Russification. The creator of the assistant is not responsible for installation over third-party solutions.

The application was tested on:
1. Starship 7 1.7.3 RUS ENcars (root)
2. Starship 7 1.7.4 RUS ENcars (root)
3. Starship 7 1.7.4 stock (non-root)
4. Starship 7 1.8.0 stock (root)
5. Boyue L patched (root) - framework patch required for installation
6. E5 patched (root) - framework patch required for installation
7. Coolray L patched (root) - framework patch required for installation

If you understand all of the above - proceed to the instructions and enjoy using it.


## Prerequisites
1. Laptop on Windows/Linux/Mac
2. Data cable usbA -> usbA
3. Enabled ADB on the car device

## Preparing the car
1. If you already have ADB open - skip this section
2. On Starship 7 you need to perform a full reset of the device.
   1. To do this, you need to enter the MA on the car's tablet and completely reset the device in the settings
   2. After the reset and loading of the initial setup manager, you need to press the logo 5 times - the engineering menu will open
   3. In the engineering menu in the second tab from the left, you can enable ADB (first line)
   4. Next, you can skip the initial setup and agree to all system proposals, after which you will get to the desktop
   5. Do not connect to WiFi until the installation is complete - this will close the ability to enable/disable adb
   6. If the engineering menu suddenly closed, you can open it through the phone application by entering the following combination: #*MMDDHH, where MM is the current month +5, DD is the current day (in Chinese time zone), HH is the current hour (in Chinese time zone)
3. (Installation for L7 is not yet available) On L7, ADB opens in the same way as on Starship 7, but does not require a device reset
   1. To open ADB, you need to open the phone application
   2. Enter the command #*30617 in the phone application. The engineering menu will open
   3. Further enabling of ADB is similar to Starship 7
4. (Installation for E5/EX5 is available only after patching the framework) On E5/EX5, ADB is open or closed depending on the firmware version, it is opened through the standard engineering menu
5. Other cars on FlymeAuto are opened and checked individually


## Quick installation (recommended method)
1. Download the latest version of the package from [Releases](https://github.com/sniiick/GalaxyVoiceAssistant/releases)
2. Unpack the archive `galaxy_voice_assistant_vX.X.zip` to a convenient location
3. **For Windows**: Run `install_assistant.bat`
4. **For Linux/Mac**: Run `install_assistant.sh`
5. Follow the instructions in the installer menu

The package already contains all the necessary files: installation scripts, applications, native libraries and scrcpy(adb) for Windows.



## Manual installation (if you are not using the ready-made installer)

### Preparing the PC
1. Download platform-tools for your OS from the link https://developer.android.com/tools/releases/platform-tools
2. Unpack the archive to a convenient location
3. Download the latest current release here [Releases](https://github.com/sniiick/GalaxyVoiceAssistant/releases
4. Unpack the archive `galaxy_voice_assistant_vX.X.zip` to a convenient location


### Connecting to the car
1. Connect the laptop to the car (usbA connector, usually on the right)
2. In the console, execute the command `adb devices`
3. The list of devices should display 1 device with its code name. If this did not happen, ADB is not activated in the car.
4. Execute the commands `adb root` and `adb remount`.
   1. If for the second command we get `remount succeeded` - congratulations, you have root
   2. If the second one gives an error `Not running as root. Try "adb root" first.` - root is not available to you

## Installation for devices with ROOT (mainly E5, Boyue L and Starship 7 starting from version 1.8.0)

```shell
adb root
adb remount

# installation
adb shell mkdir -p /system/priv-app/VoiceAssistant
adb push app-release.apk /system/priv-app/VoiceAssistant/app-release.apk

# place native libraries from the jniLibs folder into the system
adb push libonnxruntime4j_jni.so /system/lib64/libonnxruntime4j_jni.so
adb push libonnxruntime.so /system/lib64/libonnxruntime.so
adb push libjnidispatch.so /system/lib64/libjnidispatch.so
adb push libvosk.so /system/lib64/libvosk.so
adb push libsherpa-onnx-jni.so /system/lib64/libsherpa-onnx-jni.so

# disable the standard Chinese assistant
adb shell pm disable-user com.baidu.iov.dueros.activate
adb shell pm disable-user com.baidu.iov.sal

# reboot device
adb reboot
```

After rebooting, the voice assistant will respond to the standard voice assistant button.

## Installation for devices without ROOT (clean Starship 7 and maybe other models on FlymeAuto)

```shell
# installation
adb push app-release-nonroot.apk /data/local/tmp
adb shell pm install -d -r -g --user current /data/local/tmp/app-release-nonroot.apk

# disable the standard Chinese assistant
adb shell pm disable-user com.baidu.iov.dueros.activate
adb shell pm disable-user com.baidu.iov.sal
```

Within a minute, the voice assistant will be launched by the system and will begin to respond to the standard voice assistant button.

## Updating the assistant for devices with root
Download the new version of `app-release.apk`

```shell
adb root
adb install --user 0 app-release.apk
adb shell am start-foreground-service --user 0 -n com.example.voiceapp3/.VoiceAssistantService

# re-place the native libraries into the system from the jniLibs folder, in case they are updated
adb push libonnxruntime4j_jni.so /system/lib64/libonnxruntime4j_jni.so
adb push libonnxruntime.so /system/lib64/libonnxruntime.so
adb push libjnidispatch.so /system/lib64/libjnidispatch.so
adb push libvosk.so /system/lib64/libvosk.so
adb push libsherpa-onnx-jni.so /system/lib64/libsherpa-onnx-jni.so
```

## Updating the assistant for devices without root
Download the new version of `app-release-nonroot.apk`

```shell
adb push app-release-nonroot.apk /data/local/tmp
adb shell pm install -d -r -g --user current /data/local/tmp/app-release-nonroot.apk
```

## Uninstalling the assistant for devices with root

```shell
adb root
adb remount
adb shell pm uninstall --user 0 com.example.voiceapp3
adb shell rm -rf /system/priv-app/VoiceAssistant
adb reboot
```

## Uninstalling the assistant for devices without root

```shell
adb shell pm uninstall --user current com.example.voiceapp3
adb reboot
```

## Installation scripts

The package contains two apk files for root and non-root installation:

- `app-release.apk`
- `app-release-nonroot.apk`


The package contains native libraries for root installation in the jniLibs folder:

- `libonnxruntime4j_jni.so`
- `libsherpa-onnx-jni.so`
- `libonnxruntime.so`
- `libjnidispatch.so`
- `libvosk.so`


The package contains two scripts for automating the installation:

- `install_assistant.bat` - for Windows
- `install_assistant.sh` - for Linux/Mac

The scripts provide an interactive menu for:
- Installing the application (root/non-root)
- Updating the application (root/non-root)
- Uninstalling the application (root/non-root)
- Launching scrcpy to display the device screen (for Windows)


## Support the creator =)
The voice assistant is available in open access, developed and supported by one person on a voluntary basis.

There are no prohibitions on the use of the code and the application, but as a token of gratitude to the creator, you can leave a donation at the link below:

https://donate.stream/donate_68a45fabdb2f8

Feedback, constructive criticism and suggestions are welcome, but not necessarily will be implemented. Thank you.

### Development by @sniiick
### Powered by DeepSeek production
