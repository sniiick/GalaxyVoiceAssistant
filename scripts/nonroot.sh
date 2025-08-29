#!/bin/bash

# Script to replace com.example.voiceapp3 with com.baidu.che.codriver
# and move the corresponding directory structure (macOS compatible)

echo "Starting transformation..."

# Recursively replace all occurrences of com.example.voiceapp3
# macOS sed requires empty string argument for -i
find . -type f \( -name "*.kts" -o -name "*.java" -o -name "*.xml" -o -name "*.gradle" -o -name "*.kt" -o -name "*.txt" -o -name "*.md" -o -name "*.json" -o -name "*.properties" \) -exec sed -i '' 's/com\.example\.voiceapp3/com.baidu.che.codriver/g' {} +

# Create target directory if it doesn't exist
mkdir -p app/src/main/java/com/baidu/che/codriver

# Move the source directory to target location
if [ -d "app/src/main/java/com/example/voiceapp3" ]; then
    mv app/src/main/java/com/example/voiceapp3/* app/src/main/java/com/baidu/che/codriver/
    
    # Remove the original directory if empty
    if [ -d "app/src/main/java/com/example/voiceapp3" ]; then
        rm -rf app/src/main/java/com/example/voiceapp3
    fi
    
    # Remove com/example if empty
    if [ -d "app/src/main/java/com/example" ] && [ -z "$(ls -A app/src/main/java/com/example)" ]; then
        rm -rf app/src/main/java/com/example
    fi
else
    echo "Warning: Source directory app/src/main/java/com/example/voiceapp3 not found"
fi

echo "Transformation completed successfully!"
