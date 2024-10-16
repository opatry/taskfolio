#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

# shellcheck disable=SC1091
. "${origin}/utils.sh"

langs=("en-US" "fr-FR")

# Play Store requirements: https://support.google.com/googleplay/android-developer/answer/113469?hl=en#details
app_name_char_count_limit=30
short_description_char_count_limit=80
full_description_char_count_limit=4000
changelog_char_count_limit=500

error_count=0

step "Google Play Store check"

for lang in "${langs[@]}"; do
  lang_dir="${origin}/../fastlane/metadata/store/${lang}"

  info "Processing store metadata for ${BLUE}${lang}${RESET}"
  for filepath in "title.txt" "short_description.txt" "full_description.txt" "changelogs/default.txt"; do
    file="${lang_dir}/${filepath}"
    filename="$(basename "${file}" .txt)"
    case "${filename}" in
      title) limit=${app_name_char_count_limit};;
      short_description) limit=${short_description_char_count_limit};;
      full_description) limit=${full_description_char_count_limit};;
      default) limit=${changelog_char_count_limit};;
    esac

    grep -q '[[:blank:]]$' "${file}" && warn "${YELLOW}${filepath}${RESET} has trailing spaces"

    char_count=$(wc -m < "${file}" | awk '{print $1}')
    if [ "${char_count}" -gt "${limit}" ]; then
      error "${YELLOW}${filepath}${RESET} is too long (${RED_BOLD}${char_count}${RESET} vs ${GREEN}${limit}${RESET})"
      ((error_count+=1))
    fi
  done
done

if [ ${error_count} -eq 0 ]; then
  step_done
  exit 0
else
  error "You have ${error_count} errors to fix"
  exit ${error_count}
fi
