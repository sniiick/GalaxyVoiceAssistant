#!/bin/bash

# Script to reverse the transformation - replace com.baidu.che.codriver with com.example.voiceapp3
# and move the directory structure back (macOS compatible)

echo "Starting reverse transformation..."

# Recursively replace all occurrences of com.baidu.che.codriver
# macOS sed requires empty string argument for -i
find . -type f \( -name "*.kts" -o -name "*.java" -o -name "*.xml" -o -name "*.gradle" -o -name "*.kt" -o -name "*.txt" -o -name "*.md" -o -name "*.json" -o -name "*.properties" \) -exec sed -i '' 's/com\.baidu\.che\.codriver/com.example.voiceapp3/g' {} +

# Create original directory if it doesn't exist
mkdir -p app/src/main/java/com/example/voiceapp3

# Move the directory back to original location
if [ -d "app/src/main/java/com/baidu/che/codriver" ]; then
    mv app/src/main/java/com/baidu/che/codriver/* app/src/main/java/com/example/voiceapp3/
    
    # Remove the target directory if empty
    if [ -d "app/src/main/java/com/baidu/che/codriver" ]; then
        rm -rf app/src/main/java/com/baidu/che/codriver
    fi
    
    # Remove com/baidu/che if empty
    if [ -d "app/src/main/java/com/baidu/che" ] && [ -z "$(ls -A app/src/main/java/com/baidu/che)" ]; then
        rm -rf app/src/main/java/com/baidu/che
    fi
    
    # Remove com/baidu if empty
    if [ -d "app/src/main/java/com/baidu" ] && [ -z "$(ls -A app/src/main/java/com/baidu)" ]; then
        rm -rf app/src/main/java/com/baidu
    fi
else
    echo "Warning: Target directory app/src/main/java/com/baidu/che/codriver not found"
fi

echo "Reverse transformation completed successfully!"
