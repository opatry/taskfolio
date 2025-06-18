#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit
root=$(cd "${origin}/.." && pwd)

src="${root}/tasks-app-android/src/dev/google-services.json"
dst="${root}/tasks-app-android/google-services.json"

cp "${src}" "${dst}"
sed -i.bak 's/net\.opatry\.tasks\.app\.dev/net.opatry.tasks.app/g' "${dst}"
rm "${dst}.bak"
