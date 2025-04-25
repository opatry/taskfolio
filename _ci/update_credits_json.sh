#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

# shellcheck disable=SC1091
. "${origin}/utils.sh"

cd "${origin}/.."

for module in "tasks-app-desktop" "tasks-app-android"; do
  step "Updating credits for ${GREEN_BOLD}:${module}${RESET}"
  rm -rf "${module}/build/resources/main/"licences_*.json
  ./gradlew ":${module}:exportLibraryDefinitions"
  step_done
done
