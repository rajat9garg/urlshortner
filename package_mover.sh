#!/bin/bash

# rename-package.sh - A script to rename Java/Kotlin package names in a project

set -e

if [ $# -ne 3 ]; then
    echo "Usage: $0 <project_dir> <old_package> <new_package>"
    echo "Example: $0 . com.biding com.url.shortner"
    exit 1
fi

PROJECT_DIR=$1
OLD_PACKAGE=$2
NEW_PACKAGE=$3

# Convert package names to directory paths
OLD_DIR=${OLD_PACKAGE//.//}
NEW_DIR=${NEW_PACKAGE//.//}

# Find all relevant files and update package names
find "$PROJECT_DIR" -type f \( -name "*.kt" -o -name "*.java" -o -name "*.xml" -o -name "*.properties" -o -name "*.gradle" -o -name "*.kts" \) -exec sed -i '' "s/$OLD_PACKAGE/$NEW_PACKAGE/g" {} \;

# Move files to new directory structure
TEMP_DIR=$(mktemp -d)
mkdir -p "$PROJECT_DIR/src/main/kotlin/$NEW_DIR"
mkdir -p "$PROJECT_DIR/src/test/kotlin/$NEW_DIR"

# Move main source files
if [ -d "$PROJECT_DIR/src/main/kotlin/$OLD_DIR" ]; then
    mv "$PROJECT_DIR/src/main/kotlin/$OLD_DIR"/* "$PROJECT_DIR/src/main/kotlin/$NEW_DIR/" 2>/dev/null || true
    rmdir "$PROJECT_DIR/src/main/kotlin/${OLD_DIR%%/*}" 2>/dev/null || true
fi

# Move test source files
if [ -d "$PROJECT_DIR/src/test/kotlin/$OLD_DIR" ]; then
    mkdir -p "$PROJECT_DIR/src/test/kotlin/$NEW_DIR"
    mv "$PROJECT_DIR/src/test/kotlin/$OLD_DIR"/* "$PROJECT_DIR/src/test/kotlin/$NEW_DIR/" 2>/dev/null || true
    rmdir "$PROJECT_DIR/src/test/kotlin/${OLD_DIR%%/*}" 2>/dev/null || true
fi

# Update application properties if they exist
if [ -f "$PROJECT_DIR/src/main/resources/application.properties" ]; then
    sed -i '' "s/$OLD_PACKAGE/$NEW_PACKAGE/g" "$PROJECT_DIR/src/main/resources/application.properties"
fi

if [ -f "$PROJECT_DIR/src/main/resources/application.yml" ]; then
    sed -i '' "s/$OLD_PACKAGE/$NEW_PACKAGE/g" "$PROJECT_DIR/src/main/resources/application.yml"
fi

# Update build.gradle.kts if it exists
if [ -f "$PROJECT_DIR/build.gradle.kts" ]; then
    sed -i '' "s/$OLD_PACKAGE/$NEW_PACKAGE/g" "$PROJECT_DIR/build.gradle.kts"
fi

# Update settings.gradle.kts if it exists
if [ -f "$PROJECT_DIR/settings.gradle.kts" ]; then
    sed -i '' "s/$OLD_PACKAGE/$NEW_PACKAGE/g" "$PROJECT_DIR/settings.gradle.kts"
fi

echo "Package renamed from $OLD_PACKAGE to $NEW_PACKAGE"
echo "Please review the changes and commit them to version control."