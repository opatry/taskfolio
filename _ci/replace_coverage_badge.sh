#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit
root=$(cd "${origin}/.." && pwd) || exit

# shellcheck disable=SC1091
. "${origin}/utils.sh"

xml_report=${1:-"${root}/build/reports/kover/report.xml"}

[ -f "${xml_report}" ] || step_error "Kover XML report not found at '${YELLOW}${xml_report}${RESET}'"

covered=$(xmllint --xpath "string(/report/counter[@type='INSTRUCTION']/@covered)" "${xml_report}")
missed=$(xmllint --xpath "string(/report/counter[@type='INSTRUCTION']/@missed)" "${xml_report}")
total=$((covered + missed))
percent=$((100 * covered / total))

info "Kover coverage: ${MAGENTA}${percent}%${RESET} (${GREEN}${covered}${RESET}/${BLUE}${total}${RESET})"

# outputs `rgb(rrr,ggg,bbb)` URL Encoded (%28='(', %2C=',' and %29=')')
if [ ${percent} -ge 95 ]; then
  # MediumSeaGreen rgb(60,179,113)
  color="rgb%2860%2C179%2C113%29"
elif [ ${percent} -ge 85 ]; then
  # YellowGreen rgb(154,205,50)
  color="rgb%28154%2C205%2C50%29"
elif [ ${percent} -ge 75 ]; then
  # Gold rgb(255,215, 0)
  color="rgb%28225%2C215%2C0%29"
elif [ ${percent} -ge 65 ]; then
  # OrangeRed rgb(255,69,0)
  color="rgb%28255%2C69%2C0%29"
elif [ ${percent} -ge 55 ]; then
  # Crimson rgb(220,20,60)
  color="rgb%28220%2C20%2C60%29"
else
  # DarkRed rgb(139,0,0)
  color="rgb%28139%2C0%2C0%29"
fi

awk -v coverage_percent="${percent}" \
    -v coverage_color="${color}" \
    -f "${origin}/replace_coverage_badge.awk" "${root}/README.md" > tmp && mv tmp "${root}/README.md"
