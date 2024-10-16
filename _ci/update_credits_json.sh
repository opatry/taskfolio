#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

cd "${origin}/.."
rm -rf tasks-app-desktop/build/generated/aboutLibraries/
./gradlew :tasks-app-desktop:exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/resources
rm -rf tasks-app-android/build/generated/aboutLibraries
./gradlew :tasks-app-android:exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/assets
