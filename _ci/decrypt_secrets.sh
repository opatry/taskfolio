#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

cd "${origin}"
./decrypt_file.sh "${origin}/google-services.json.gpg" \
                  "${origin}/../tasks-app-android/google-services.json"
