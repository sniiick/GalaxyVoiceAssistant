#!/bin/bash

# Скрипт для создания zip-архива с Galaxy Voice Assistant
set -e

# Проверяем, что передана версия
if [ -z "$1" ]; then
    echo "Использование: $0 <версия>"
    echo "Пример: $0 2.2"
    exit 1
fi

VERSION="$1"
OUTPUT_ZIP="galaxy_voice_assistant_v${VERSION}.zip"
TEMP_DIR="./temp_package"

echo "Создание пакета Galaxy Voice Assistant v${VERSION}..."
echo "Выходной файл: $OUTPUT_ZIP"
echo

# Создаем временную директорию
mkdir -p "$TEMP_DIR"
mkdir -p "$TEMP_DIR/jniLibs"
mkdir -p "$TEMP_DIR/scrcpy-win64"

# Копируем файлы в корень архива
echo "Копирование основных файлов..."
cp "./scripts/install_assistant.bat" "$TEMP_DIR/"
cp "./app-release.apk" "$TEMP_DIR/"
cp "./app-release-nonroot.apk" "$TEMP_DIR/"

# Копируем JNI библиотеки
echo "Копирование JNI библиотек..."
cp "./app/src/main/jniLibs/arm64-v8a/libvosk.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libsherpa-onnx-jni.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libonnxruntime4j_jni.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libonnxruntime.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libjnidispatch.so" "$TEMP_DIR/jniLibs/"

# Копируем scrcpy-win64
echo "Копирование scrcpy-win64..."
cp -R "./scrcpy-win64/"* "$TEMP_DIR/scrcpy-win64/"

# Создаем README файл с инструкциями
cat > "$TEMP_DIR/README.txt" << EOF
Galaxy Voice Assistant v${VERSION} - Инструкция по установке

Содержимое пакета:
- install_assistant.bat - Скрипт установки для Windows
- app-release.apk - Приложение для устройств с ROOT
- app-release-nonroot.apk - Приложение для устройств без ROOT
- jniLibs/ - Нативные библиотеки
- scrcpy-win64/ - Утилита для отображения экрана устройства

Инструкция:
1. Для Windows: Запустите install_assistant.bat
2. Следуйте инструкциям в меню

Важно: Перед установкой убедитесь, что:
- ADB включен на устройстве
- Устройство подключено по USB
- Разрешена отладка по USB

Версия: ${VERSION}
EOF

# Создаем zip-архив
echo "Создание zip-архива..."
cd "$TEMP_DIR"
zip -r "../$OUTPUT_ZIP" ./*
cd ..

# Очищаем временные файлы
echo "Очистка временных файлов..."
rm -rf "$TEMP_DIR"

echo
echo "Готово! Создан архив: $OUTPUT_ZIP"
echo "Размер архива: $(du -h "$OUTPUT_ZIP" | cut -f1)"