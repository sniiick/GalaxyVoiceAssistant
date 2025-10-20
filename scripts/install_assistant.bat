@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

title Galaxy Voice Assistant Installer
echo ========================================
echo    Galaxy Voice Assistant Installer
echo ========================================
echo.

:: Set ADB paths
set ADB_PATH=adb.exe

:: If scrcpy-win64 folder exists, use ADB from there
if exist "scrcpy-win64\adb.exe" (
    set ADB_PATH=scrcpy-win64\adb.exe
)


:main_menu
cls
echo Select action:
echo 1 - Install application
echo 2 - Update application
echo 3 - Remove application
echo 4 - Run scrcpy
echo 5 - Disable native assistant
echo 6 - Restore native assistant
echo 7 - Exit
echo.
set /p choice="Enter action number (1-7): "

if "%choice%"=="1" goto install_menu
if "%choice%"=="2" goto update_menu
if "%choice%"=="3" goto remove_menu
if "%choice%"=="4" goto run_scrcpy
if "%choice%"=="5" goto remove_native_assistant
if "%choice%"=="6" goto restore_native_assistant
if "%choice%"=="7" exit /b
echo Invalid choice, try again.
echo.
pause
goto main_menu


:: Function to check device connection
:check_device
echo Checking device connection...
%ADB_PATH% devices | findstr /r /c:"device$" >nul
if !errorlevel! neq 0 (
    echo Error: Device not connected or ADB not enabled.
    echo Please ensure:
    echo 1. Device connected via USB
    echo 2. ADB enabled in engineering menu
    echo 3. USB debugging allowed
    echo.
    pause
    exit /b 1
)
echo Device found.
exit /b 0


:install_menu
echo.
echo Application installation:
echo 1 - For ROOT devices
echo 2 - For non-ROOT devices
echo 3 - Back
echo.
set /p install_choice="Enter number (1-3): "

if "%install_choice%"=="1" call :install_root
if "%install_choice%"=="2" call :install_nonroot
if "%install_choice%"=="3" goto main_menu
echo Invalid choice, try again.
echo.
pause
goto install_menu

:update_menu
echo.
echo Application update:
echo 1 - For ROOT devices
echo 2 - For non-ROOT devices
echo 3 - Back
echo.
set /p update_choice="Enter number (1-3): "

if "%update_choice%"=="1" call :install_root
if "%update_choice%"=="2" call :update_nonroot
if "%update_choice%"=="3" goto main_menu
echo Invalid choice, try again.
echo.
pause
goto update_menu

:remove_menu
echo.
echo Application removal:
echo 1 - For ROOT devices
echo 2 - For non-ROOT devices
echo 3 - Back
echo.
set /p remove_choice="Enter number (1-3): "

if "%remove_choice%"=="1" call :remove_root
if "%remove_choice%"=="2" call :remove_nonroot
if "%remove_choice%"=="3" goto main_menu
echo Invalid choice, try again.
echo.
pause
goto remove_menu

:run_scrcpy
if exist "scrcpy-win64\scrcpy.exe" (
    echo Starting scrcpy...
    start "" "scrcpy-win64\scrcpy.exe"
    echo scrcpy started in separate window.
) else (
    echo scrcpy not found in scrcpy-win64 folder.
)
echo.
pause
goto main_menu


:remove_native_assistant
call :check_device
echo Disabling stock assistant...
%ADB_PATH% shell pm disable-user com.baidu.iov.sal >nul 2>&1
if !errorlevel! neq 0 (
    echo Warning: Failed to disable com.baidu.iov.sal (correct, model specific)
)
%ADB_PATH% shell pm disable-user com.baidu.iov.dueros.activate >nul 2>&1
if !errorlevel! neq 0 (
    echo Warning: Failed to disable com.baidu.iov.dueros.activate (correct, model specific)
)
echo.
pause
goto main_menu


:restore_native_assistant
call :check_device
echo Restoring stock assistant...
%ADB_PATH% shell pm enable com.baidu.iov.sal >nul 2>&1
if !errorlevel! neq 0 (
    echo Warning: Failed to enable com.baidu.iov.sal
)
%ADB_PATH% shell pm enable com.baidu.iov.dueros.activate >nul 2>&1
if !errorlevel! neq 0 (
    echo Warning: Failed to enable com.baidu.iov.dueros.activate
)
echo.
echo Native assistant has been restored.
pause
goto main_menu


:check_root
echo Checking root access...
%ADB_PATH% root >nul 2>&1
if !errorlevel! neq 0 (
    echo Error: adb root not available
    echo.
    pause
    exit /b 1
)

%ADB_PATH% remount >nul 2>&1
if !errorlevel! neq 0 (
    echo Error: adb remount not available. Device does not have root rights.
    echo.
    pause
    exit /b 1
)
echo Root access confirmed.
exit /b 0

:install_root
call :check_device
if !errorlevel! neq 0 (
    goto main_menu
)

echo Checking root access...
call :check_root
if !errorlevel! neq 0 (
    echo Installation for root devices is not possible.
    echo.
    pause
    goto main_menu
)

echo Installing for ROOT devices...
%ADB_PATH% shell mkdir -p /system/priv-app/VoiceAssistant
if !errorlevel! neq 0 (
    echo Error: Failed to create directory
    echo.
    pause
    goto main_menu
)

%ADB_PATH% push app-release.apk /system/priv-app/VoiceAssistant/app-release.apk
if !errorlevel! neq 0 (
    echo Error: Failed to push APK file
    echo.
    pause
    goto main_menu
)

echo Installing native libraries...
%ADB_PATH% push jniLibs\libonnxruntime4j_jni.so /system/lib64/libonnxruntime4j_jni.so
%ADB_PATH% push jniLibs\libonnxruntime.so /system/lib64/libonnxruntime.so
%ADB_PATH% push jniLibs\libjnidispatch.so /system/lib64/libjnidispatch.so
%ADB_PATH% push jniLibs\libvosk.so /system/lib64/libvosk.so
%ADB_PATH% push jniLibs\libsherpa-onnx-jni.so /system/lib64/libsherpa-onnx-jni.so

echo Rebooting device...
%ADB_PATH% reboot
echo Installation completed. Device is rebooting.
echo.
pause
goto main_menu

:install_nonroot
call :check_device
if !errorlevel! neq 0 (
    goto main_menu
)

echo Installing for non-ROOT devices...
%ADB_PATH% push app-release-nonroot.apk /data/local/tmp
if !errorlevel! neq 0 (
    echo Error: Failed to push APK file
    echo.
    pause
    goto main_menu
)

%ADB_PATH% shell pm install -d -r -g --user current /data/local/tmp/app-release-nonroot.apk
if !errorlevel! neq 0 (
    echo Error: Failed to install application
    echo.
    pause
    goto main_menu
)

%ADB_PATH% shell rm -f /data/local/tmp/app-release-nonroot.apk


echo Installation completed. Application will start within a minute.
echo.
pause
goto main_menu

:update_root
call :check_device
if !errorlevel! neq 0 (
    goto main_menu
)

echo Checking root access...
call :check_root
if !errorlevel! neq 0 (
    echo Update for root devices is not possible.
    echo.
    pause
    goto main_menu
)

echo Updating for ROOT devices...
%ADB_PATH% root
%ADB_PATH% install --user 0 app-release.apk
if !errorlevel! neq 0 (
    echo Error: Failed to install application
    echo.
    pause
    goto main_menu
)

echo Updating native libraries...
%ADB_PATH% push jniLibs\libonnxruntime4j_jni.so /system/lib64/libonnxruntime4j_jni.so
%ADB_PATH% push jniLibs\libonnxruntime.so /system/lib64/libonnxruntime.so
%ADB_PATH% push jniLibs\libjnidispatch.so /system/lib64/libjnidispatch.so
%ADB_PATH% push jniLibs\libvosk.so /system/lib64/libvosk.so
%ADB_PATH% push jniLibs\libsherpa-onnx-jni.so /system/lib64/libsherpa-onnx-jni.so


%ADB_PATH% shell am start-foreground-service --user 0 -n com.example.voiceapp3/.VoiceAssistantService

echo Update completed.
echo.
pause
goto main_menu

:update_nonroot
call :check_device
if !errorlevel! neq 0 (
    goto main_menu
)

echo Updating for non-ROOT devices...
%ADB_PATH% push app-release-nonroot.apk /data/local/tmp
if !errorlevel! neq 0 (
    echo Error: Failed to push APK file
    echo.
    pause
    goto main_menu
)

%ADB_PATH% shell pm install -d -r -g --user current /data/local/tmp/app-release-nonroot.apk
if !errorlevel! neq 0 (
    echo Error: Failed to install application
    echo.
    pause
    goto main_menu
)

%ADB_PATH% shell rm -f /data/local/tmp/app-release-nonroot.apk

echo Update completed.
echo.
pause
goto main_menu

:remove_root
call :check_device
if !errorlevel! neq 0 (
    goto main_menu
)

echo Checking root access...
call :check_root
if !errorlevel! neq 0 (
    echo Removal for root devices is not possible.
    echo.
    pause
    goto main_menu
)

echo Removing for ROOT devices...
%ADB_PATH% root
%ADB_PATH% remount
%ADB_PATH% shell pm uninstall --user 0 com.example.voiceapp3
%ADB_PATH% shell rm -rf /system/priv-app/VoiceAssistant
echo Removing native libraries...
%ADB_PATH% shell rm -f /system/lib64/libonnxruntime4j_jni.so
%ADB_PATH% shell rm -f /system/lib64/libonnxruntime.so
%ADB_PATH% shell rm -f /system/lib64/libjnidispatch.so
%ADB_PATH% shell rm -f /system/lib64/libvosk.so
%ADB_PATH% shell rm -f /system/lib64/libsherpa-onnx-jni.so
%ADB_PATH% reboot

echo Removal completed. Device is rebooting.
echo.
pause
goto main_menu

:remove_nonroot
call :check_device
if !errorlevel! neq 0 (
    goto main_menu
)

echo Removing for non-ROOT devices...
%ADB_PATH% shell pm uninstall --user current com.example.voiceapp3
if !errorlevel! neq 0 (
    echo Error: Failed to uninstall application
    echo.
    pause
    goto main_menu
)

%ADB_PATH% reboot

echo Removal completed. Device is rebooting.
echo.
pause
goto main_menu
