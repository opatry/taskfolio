#!/usr/bin/env bash

set -euo pipefail

# requirements on Fastlane documentation
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

# shellcheck disable=SC1091
. "${origin}/utils.sh"

# order is a "low-risk" priority
available_tracks=("internal" "alpha" "beta" "production")
default_track="${available_tracks[0]}"

build_type="release"

check_track() {
  # shellcheck disable=SC2076
  if [[ ! " ${available_tracks[*]} " =~ " ${1} " ]]; then
    if [ -n "${2:-}" ]; then
      echo -e "${2}"
    fi
    local available_tracks_str="${available_tracks[*]}"
    # quick & dirty array join by ',' surrounded with quotes
    step_error "You must specify the track among: '${GREEN}${available_tracks_str// /${RESET}', '${GREEN}}${RESET}'"
  fi
}

publish_aab() {
  local aab="${1:-}"
  [ -f "${aab}" ] || step_error "No App Bundle (AAB) found '${YELLOW}${aab}${RESET}'"

  local track="${2:-"${default_track}"}"
  check_track "${track}" "Invalid track '${YELLOW}${track}${RESET}'"

  metadata_path="${origin}/../fastlane/metadata/android"

  step "Dumping information from App Bundle (AAB)"
  manifest_file="${origin}/manifest.xml"
  bundletool dump manifest --bundle="${aab}" > "${manifest_file}"

  app_package=$(xmllint --xpath 'string(//manifest/@*[local-name()="package"])' "${manifest_file}")
  version_name=$(xmllint --xpath 'string(//manifest/@*[local-name()="versionName"])' "${manifest_file}")
  version_code=$(xmllint --xpath 'string(//manifest/@*[local-name()="versionCode"])' "${manifest_file}")

  rm "${manifest_file}"

  step "Publishing '${YELLOW}${aab##"${origin}/"}${RESET}' (${BLUE_BOLD}${app_package}${RESET} '${MAGENTA_BOLD}${version_name}${RESET}' #${GREEN_BOLD}${version_code}${RESET}) to '${MAGENTA}${track}${RESET}' track"

  supply_args=(
    --package_name "${app_package}"
    --track "${track}"
    --metadata_path "${metadata_path}"
  )

  if [ "${upload_binary}" = true ]; then
    supply_args+=(--aab "${aab}")
  else
    supply_args+=(
      --skip_upload_aab true
      --skip_upload_changelogs true
    )
  fi

  if [ "${upload_store_assets}" = false ]; then
    supply_args+=(
      --skip_upload_metadata true
      --skip_upload_images true
      --skip_upload_screenshots true
    )
  fi

  bundle exec fastlane supply "${supply_args[@]}"
}

# assisted mode when called on dev machine with fzf installed
if [ $# -eq 0 ] && [ -z "${CI:-}" ] && command -v fzf &> /dev/null; then
  track=$(printf "%s\n" "${available_tracks[@]}" \
            | fzf --prompt "Choose the track to use" \
                  --height ~7 \
                  --layout=reverse-list \
                  || true)
  check_track "${track}"
  
  upload_binary=$(ask_yn_choice "${MAGENTA_BOLD}Publish App Bundle (AAB) (including associated changelog)?${RESET}")
  upload_store_assets=$(ask_yn_choice "${MAGENTA_BOLD}Publish Store assets (descriptions, images & screenshots)?${RESET}")
else
  track="${1:-"${default_track}"}"
  check_track "${track}"

  upload_binary=${2:-true}
  upload_store_assets=${3:-false}
fi

aab="${origin}/../tasks-app-android/build/outputs/bundle/${build_type}/tasks-app-android-${build_type}.aab"

if [ -z "${CI:-}" ] && [ "${upload_binary}" = true ]; then
  warn "Ensure you have updated your '${BLUE}${build_type}${RESET}' build and updated '${GREEN}tasksApp-code${RESET}'."
  grep -H --color=auto 'tasksApp-code' "${origin}/../gradle/libs.versions.toml"
  if [ -f "${aab}" ]; then
    echo "${aab} last modification date"
    echo -e "${GREEN}$(date -r "${aab}")${RESET}\n\n"
  fi
fi

[ "${upload_binary}" = false ] && [ "${upload_store_assets}" = false ] && warn "Neither binary nor assets will be published."

cd "${origin}/.."

bundle exec fastlane run validate_play_store_json_key

if ! command -v bundletool &> /dev/null; then
  BUNDLETOOL_VERSION="1.17.1"
  bundletool_jar="bundletool-all-${BUNDLETOOL_VERSION}.jar"
  if [ ! -f "${bundletool_jar}" ]; then
    info "${YELLOW}bundletool${RESET} not available in the PATH, downloading it"
    curl -L -o "${bundletool_jar}" https://github.com/google/bundletool/releases/download/${BUNDLETOOL_VERSION}/bundletool-all-${BUNDLETOOL_VERSION}.jar
  fi
  alias bundletool='java -jar "${bundletool_jar}"'
fi

info "${YELLOW}bundletool${RESET} version $(bundletool version)"

publish_aab "${aab}" "${track}"