#!/bin/bash

# Script to create a zip archive with Galaxy Voice Assistant
set -e

# Check if a version is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 2.2"
    exit 1
fi

VERSION="$1"
OUTPUT_ZIP="galaxy_voice_assistant_v${VERSION}.zip"
TEMP_DIR="./temp_package"

echo "Creating Galaxy Voice Assistant package v${VERSION}..."
echo "Output file: $OUTPUT_ZIP"
echo

# Create a temporary directory
mkdir -p "$TEMP_DIR"
mkdir -p "$TEMP_DIR/jniLibs"
mkdir -p "$TEMP_DIR/scrcpy-win64"

# Copy files to the archive root
echo "Copying main files..."
cp "./scripts/install_assistant.bat" "$TEMP_DIR/"
cp "./app-release.apk" "$TEMP_DIR/"
cp "./app-release-nonroot.apk" "$TEMP_DIR/"

# Copy JNI libraries
echo "Copying JNI libraries..."
cp "./app/src/main/jniLibs/arm64-v8a/libvosk.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libsherpa-onnx-jni.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libonnxruntime4j_jni.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libonnxruntime.so" "$TEMP_DIR/jniLibs/"
cp "./app/src/main/jniLibs/arm64-v8a/libjnidispatch.so" "$TEMP_DIR/jniLibs/"

# Copy scrcpy-win64
echo "Copying scrcpy-win64..."
cp -R "./scrcpy-win64/"* "$TEMP_DIR/scrcpy-win64/"

# Create README file with instructions
cat > "$TEMP_DIR/README.txt" << EOF
Galaxy Voice Assistant v${VERSION} - Installation Instructions

Package Contents:
- install_assistant.bat - Installation script for Windows
- app-release.apk - Application for devices with ROOT
- app-release-nonroot.apk - Application for devices without ROOT
- jniLibs/ - Native libraries
- scrcpy-win64/ - Utility for displaying the device screen

Instructions:
1. For Windows: Run install_assistant.bat
2. Follow the instructions in the menu

Important: Before installation, make sure that:
- ADB is enabled on the device
- The device is connected via USB
- USB debugging is allowed

Version: ${VERSION}
EOF

# Create zip archive
echo "Creating zip archive..."
cd "$TEMP_DIR"
zip -r "../$OUTPUT_ZIP" ./*
cd ..

# Clean up temporary files
echo "Cleaning up temporary files..."
rm -rf "$TEMP_DIR"

echo
echo "Done! Archive created: $OUTPUT_ZIP"
echo "Archive size: $(du -h "$OUTPUT_ZIP" | cut -f1)"
