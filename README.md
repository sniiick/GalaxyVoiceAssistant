# Galaxy Voice Assistant - Установка и использование

## Внимание!
Голосовой ассистент разрабатывается силами community. Все действия Вы выполняете со своего согласия и на свой страх и риск.

ADB root и ADB remount позволяют Вам выполнить необратимые действия с прошивкой автомобиля, последствия могут быть критичными и превратить Ваш планшет в кирпич.

Помните: чем больше сила - тем больше ответственность!

Несмотря на то, что инструкция и голосовой ассистент никак не задевают штатные подсистемы автомобиля, если вы не уверены в своих действиях - создатель ассистента крайне НЕ(!) рекомендует самостоятельную установку.

## Внимание!
Данная инструкция скорее всего не применима к русифицированным прошивкам, и к коммерческим прошивкам, в которых есть голосовые ассистенты (ЕвропаКар, GMC). Установка данного приложения может привести к непредвиденным проблемам и конфликтам, вплоть до потери установленной русификации. Создатель ассистента не несет ответственности за установку поверх сторонних решений.

Приложение тестировалось на:
1. Starship 7 1.7.3 RUS ENcars (root)
2. Starship 7 1.7.4 RUS ENcars (root)
3. Starship 7 1.7.4 stock (nonroot)
4. Starship 7 1.8.0 stock (root)
5. Boyue L patched (root) - для установки необходим патч фреймворка
6. E5 patched (root) - для установки необходим патч фреймворка
7. Coolray L patched (root) - для установки необходим патч фреймворка

Если Все вышеописанное Вам понятно - переходите к инструкции и приятного использования.


## Предварительные требования
1. Ноутбук на Windows/Linux/Mac
2. Data-кабель usbA -> usbA
3. Включенный ADB на устройстве автомобиля

## Подготовка автомобиля
1. Если у вас уже открыт ADB - пропустите эту секцию
2. На Starship 7 вам необходимо выполнить полный сброс устройства.
   1. Для этого необходим вход в МА на планшете автомобиля и в настройках необходимо полностью сбросить устройство
   2. По окончанию сброса и загрузке менеджера первичной настройки необходимо 5 раз нажать на логотип - откроется инженерное меню
   3. В инженерном меню во второй слева вкладке можно будет включить ADB (первая строчка)
   4. Далее можно пропустить первичную настройку и согласиться со всеми предложениями системы, после чего Вы попадете на рабочий стол
   5. Не подключайте WiFi до тех пор пока установка не будет выполнена - это закроет возможность включать/отключать adb
   6. Если вдруг закрылось инженерное меню, открыть его можно через приложение телефон, введя следующую комбинацию: #*MMDDHH, где MM - текущий месяц +5, DD - текущий день (по китайскому часовому поясу), HH - текущий час (по китайскому часовому поясу)
3. (Установка для L7 пока недоступна) На L7 ADB открывается аналогичным образом как и на Starship 7, но не требует сброс устройства
   1. Для открытия ADB необходимо открыть приложение телефона
   2. Ввести команду #*30617 в приложении телефона. Откроется инженерное меню
   3. Далее включение ADB происходит аналогично Starship 7
4. (Установка для E5/EX5 доступна только после патча фреймворка) На Е5/EX5 ADB открыт или закрыт в зависимости от версии прошивки, открывается через штатное инженерное меню
5. Остальные автомобили на FlymeAuto открываются и проверяются индивидуально


## Быстрая установка (рекомендуемый способ)
1. Скачайте последнюю версию пакета с [Releases](https://github.com/sniiick/GalaxyVoiceAssistant/releases)
2. Распакуйте архив `galaxy_voice_assistant_vX.X.zip` в удобное место
3. **Для Windows**: Запустите `install_assistant.bat`
4. **Для Linux/Mac**: Запустите `install_assistant.sh`
5. Следуйте инструкциям в меню установщика

Пакет уже содержит все необходимые файлы: скрипты установки, приложения, нативные библиотеки и scrcpy(adb) для Windows.



## Ручная установка (если не используете готовый установщик)

### Подготовка ПК
1. Скачайте platform-tools для вашей ОС по ссылке https://developer.android.com/tools/releases/platform-tools
2. Распакуйте архив в удобное Вам место
3. Скачайте последний актуальный релиз здесь [Releases](https://github.com/sniiick/GalaxyVoiceAssistant/releases
4. Распакуйте архив `galaxy_voice_assistant_vX.X.zip` в удобное место


### Подключение к машине
1. Соединияем ноутбук с машиной (разъем usbA, обычно справа)
2. В консоли выполняем команду `adb devices`
3. В списке устройств должно отобразиться 1 устройство с его кодовым именем. Если этого не произошло - в машине не активирован ADB.
4. Выполняем команду `adb root` и `adb remount`.
   1. Если на вторую команду мы получаем `remount succeeded` - поздравляю, у Вас есть root
   2. Если вторая выдает ошибку `Not running as root. Try "adb root" first.` - root у Вас недоступен

## Установка для устройств с ROOT (в основном E5, Boyue L и Starship 7 начиная с версии 1.8.0)

```shell
adb root
adb remount

# установка
adb shell mkdir -p /system/priv-app/VoiceAssistant
adb push app-release.apk /system/priv-app/VoiceAssistant/app-release.apk

# помещаем нативные библиотеки из папки jniLibs в систему
adb push libonnxruntime4j_jni.so /system/lib64/libonnxruntime4j_jni.so
adb push libonnxruntime.so /system/lib64/libonnxruntime.so
adb push libjnidispatch.so /system/lib64/libjnidispatch.so
adb push libvosk.so /system/lib64/libvosk.so
adb push libsherpa-onnx-jni.so /system/lib64/libsherpa-onnx-jni.so

# отключаем штатного китайского ассистента
adb shell pm disable-user com.baidu.iov.dueros.activate
adb shell pm disable-user com.baidu.iov.sal

# перезагрузка устройства
adb reboot
```

После перезагрузки голосовой ассистент будет реагировать на штатную кнопку голосового ассистента.

## Установка для устройств без ROOT (чистые Starship 7 и может быть другие модели на FlymeAuto)

```shell
# установка
adb push app-release-nonroot.apk /data/local/tmp
adb shell pm install -d -r -g --user current /data/local/tmp/app-release-nonroot.apk

# отключаем штатного китайского ассистента
adb shell pm disable-user com.baidu.iov.dueros.activate
adb shell pm disable-user com.baidu.iov.sal
```

В пределах минуты голосовой ассистент будет запущен системной и начнет реагировать на штатную кнопку голосового ассистента.

## Обновление ассистента для устройств с root
Скачиваем новую версию `app-release.apk`

```shell
adb root
adb install --user 0 app-release.apk
adb shell am start-foreground-service --user 0 -n com.example.voiceapp3/.VoiceAssistantService

# повторно помещаем нативные библиотеки в систему из папки jniLibs, на случай если они обновляются
adb push libonnxruntime4j_jni.so /system/lib64/libonnxruntime4j_jni.so
adb push libonnxruntime.so /system/lib64/libonnxruntime.so
adb push libjnidispatch.so /system/lib64/libjnidispatch.so
adb push libvosk.so /system/lib64/libvosk.so
adb push libsherpa-onnx-jni.so /system/lib64/libsherpa-onnx-jni.so
```

## Обновление ассистента для устройств без root
Скачиваем новую версию `app-release-nonroot.apk`

```shell
adb push app-release-nonroot.apk /data/local/tmp
adb shell pm install -d -r -g --user current /data/local/tmp/app-release-nonroot.apk
```

## Удаление ассистента для устройств с root

```shell
adb root
adb remount
adb shell pm uninstall --user 0 com.example.voiceapp3
adb shell rm -rf /system/priv-app/VoiceAssistant
adb reboot
```

## Удаление ассистента для устройств без root

```shell
adb shell pm uninstall --user current com.example.voiceapp3
adb reboot
```

## Скрипты установки

В пакете содержатся два apk-файла для root и non-root установки:

- `app-release.apl`
- `app-release-nonroot.apk`


В пакете содержатся нативные библиотеки для root установки в папке jniLibs:

- `libonnxruntime4j_jni.so`
- `libsherpa-onnx-jni.so`
- `libonnxruntime.so`
- `libjnidispatch.so`
- `libvosk.so`


В пакете содержатся два скрипта для автоматизации установки:

- `install_assistant.bat` - для Windows
- `install_assistant.sh` - для Linux/Mac

Скрипты предоставляют интерактивное меню для:
- Установки приложения (root/non-root)
- Обновления приложения (root/non-root)
- Удаления приложения (root/non-root)
- Запуска scrcpy для отображения экрана устройства (для Windows)


## Поддержать создателя =)
Голосовой ассистент выложен в открытый доступ, развивается и поддерживается силами одного человека на безвозмездной основе.

Никаких запретов на использование кода и приложения нет, но в качестве благодарности создателю Вы можете оставить донат по ссылке ниже:

https://donate.stream/donate_68a45fabdb2f8

Обратная связь, конструктивная критика и предложения приветствуются, но не обязательно будут выполнены. Спасибо.

### Development by @sniiick
### Powered by DeepSeek production