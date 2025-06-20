#!/usr/bin/env bash
set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

# shellcheck disable=SC1091
. "${origin}/_ci/utils.sh"

app_bundle_id="net.opatry.tasks.app"
ios_target="${1:-"iPhone 16"}"

step "Looking for iOS simulator device UUID for ${MAGENTA}${ios_target}${RESET}"
device_uuid=$(xcrun simctl list devices | grep "${ios_target}" | grep "Booted" | grep -oE '[A-F0-9\-]{36}' | head -1)

step "Getting app container for bundle ID '${GREEN}${app_bundle_id}${RESET}' on device UUID '${MAGENTA}${device_uuid}${RESET}'"
app_container=$(xcrun simctl get_app_container "${device_uuid}" "${app_bundle_id}" data)
dest_dir="${app_container}/Documents"

step "Copying '${BLUE}~/.taskfolio/google_auth_token_cache.json${RESET}' to ${YELLOW}${dest_dir}${RESET}"
cp "${HOME}/.taskfolio/google_auth_token_cache.json" "${dest_dir}/hacky_token_cache.json"

step_done
