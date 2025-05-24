#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

cd "${origin}/.."

find . -name "licenses*.json" -delete
./gradlew "exportLibraryDefinitions" -Pci=true
