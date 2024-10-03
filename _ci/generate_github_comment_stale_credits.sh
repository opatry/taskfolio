#!/usr/bin/env bash

set -euo pipefail

# need to make explicit `\n` char line ending to be properly managed by
# Github action output variable processing

case "$1" in
  ":tasks-app-android")
  diff=$(git diff tasks-app-android/src/main/assets/licenses_android.json | awk '{printf "%s\\n", $0}')
  update_cmd="./gradlew :tasks-app-android:exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/assets"
  ;;
  ":tasks-app-desktop")
  diff=$(git diff tasks-app-desktop/src/main/resources/licenses_desktop.json | awk '{printf "%s\\n", $0}')
  update_cmd="./gradlew :tasks-app-desktop:exportLibraryDefinitions -PaboutLibraries.exportPath=src/main/resources"
  ;;
  *)
  echo "Unsupported app module"
  exit 1
  ;;
esac

cat << __END
## ©️ Stale credits for \`$1\`\n\n
\`\`\`diff\n
${diff}\n
\`\`\`\n\n
Run \`${update_cmd}\` and commit resulting diff to fix the issue.
__END