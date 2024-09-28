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

# in-app update priority mapping (none: not provided to Fastlane, feature; 1, bugfix: 3, hotfix: 5)
in_app_update_level=("none" "feature" "bugfix" "hotfix")
default_in_app_update_level="${in_app_update_level[0]}"

build_type="release"
flavor="store"

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

check_in_app_level() {
  # shellcheck disable=SC2076
  if [[ ! " ${in_app_update_level[*]} " =~ " ${1} " ]]; then
    if [ -n "${2:-}" ]; then
      echo -e "${2}"
    fi
    local in_app_update_level_str="${in_app_update_level[*]}"
    # quick & dirty array join by ',' surrounded with quotes
    step_error "You must specify the in-app level among: '${GREEN}${in_app_update_level_str// /${RESET}', '${GREEN}}${RESET}'"
  fi
}

if ! command -v bundletool &> /dev/null; then
  bundletool() {
    bundletool_version="1.17.1"
    bundletool_jar="${origin}/bundletool-all-${bundletool_version}.jar"
    if [ ! -f "${bundletool_jar}" ]; then
      curl -s -L -o "${bundletool_jar}" https://github.com/google/bundletool/releases/download/${bundletool_version}/bundletool-all-${bundletool_version}.jar
    fi
    java -jar "${bundletool_jar}" "${@}"
  }
fi

publish_aab() {
  local aab="${1:-}"
  [ -f "${aab}" ] || step_error "No App Bundle (AAB) found '${YELLOW}${aab}${RESET}'"

  local track="${2:-"${default_track}"}"
  check_track "${track}" "Invalid track '${YELLOW}${track}${RESET}'"

  metadata_path="${origin}/../fastlane/metadata/${flavor}"

  step "Dumping information from App Bundle (AAB) (using ${YELLOW}bundletool${RESET} version $(bundletool version))"
  manifest_file="${origin}/manifest.xml"
  bundletool dump manifest --bundle="${aab}" > "${manifest_file}"

  app_package=$(xmllint --xpath 'string(//manifest/@*[local-name()="package"])' "${manifest_file}")
  version_name=$(xmllint --xpath 'string(//manifest/@*[local-name()="versionName"])' "${manifest_file}")
  version_code=$(xmllint --xpath 'string(//manifest/@*[local-name()="versionCode"])' "${manifest_file}")

  rm "${manifest_file}"
  step_done

  step "Publishing '${YELLOW}${aab##"${origin}/"}${RESET}' (${BLUE_BOLD}${app_package}${RESET} '${MAGENTA_BOLD}${version_name}${RESET}' #${GREEN_BOLD}${version_code}${RESET}) to '${MAGENTA}${track}${RESET}' track"

  case "${update_level}" in
    "none") update_level_digit=-1
    ;;
    "feature") update_level_digit=1
    ;;
    "bugfix") update_level_digit=3
    ;;
    "hotfix") update_level_digit=5
    ;;
    *) update_level_digit=-1
    ;;
  esac

  supply_args=(
    --package_name "${app_package}"
    --track "${track}"
    --metadata_path "${metadata_path}"
  )

  if [ ${update_level_digit} -ge 0 ]; then
    supply_args+=(--in_app_update_priority "${update_level_digit}")
  fi

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

  step_done
}

# assisted mode when called on dev machine with fzf installed
if [ $# -eq 0 ] && [ -z "${CI:-}" ] && command -v fzf &> /dev/null; then
  echo -e "🚀 Play Store publish\n"

  track=$(printf "%s\n" "${available_tracks[@]}" \
            | fzf --prompt "Choose the track to use" \
                  --height ~7 \
                  --layout=reverse-list \
                  || true)
  check_track "${track}"

  upload_binary=$(ask_yn_choice "${MAGENTA_BOLD}Publish App Bundle (AAB) (including associated changelog)?${RESET}")
  upload_store_assets=$(ask_yn_choice "${MAGENTA_BOLD}Publish Store assets (descriptions, images & screenshots)?${RESET}")

  update_level=$(printf "%s\n" "${in_app_update_level[@]}" \
            | fzf --prompt "Choose the in-app level update to use" \
                  --height ~7 \
                  --layout=reverse-list \
                  || true)
  check_in_app_level "${update_level}"
else
  track="${1:-"${default_track}"}"
  check_track "${track}"

  upload_binary=${2:-true}
  upload_store_assets=${3:-false}

  update_level=${4:-"${default_in_app_update_level}"}
  check_in_app_level "${update_level}"
fi

aab="${origin}/../tasks-app-android/build/outputs/bundle/${flavor}${build_type^}/tasks-app-android-${flavor}-${build_type}.aab"

if [ -z "${CI:-}" ] && [ "${upload_binary}" = true ]; then
  warn "Ensure you have properly build an up to date version of '${MAGENTA}${flavor}${RESET}' '${BLUE}${build_type}${RESET}' App Bundle (AAB)."
  if [ -f "${aab}" ]; then
    echo -e "\n${aab} last modification date"
    echo -e "${GREEN}$(date -r "${aab}")${RESET}\n"
  fi
fi

[ "${upload_binary}" = false ] && [ "${upload_store_assets}" = false ] && warn "Neither binary nor assets will be published."

cd "${origin}/.."

bundle exec fastlane run validate_play_store_json_key

publish_aab "${aab}" "${track}" "${update_level}"
