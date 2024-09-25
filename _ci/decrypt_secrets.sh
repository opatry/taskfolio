#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

cd "${origin}"
./decrypt_file.sh "${origin}/tasksApp.keystore.gpg"
./decrypt_file.sh "${origin}/../tasks-app-android/google-services.json.gpg"
for gpg_file in "${origin}/../tasks-app-shared/src/commonMain/composeResources/files/"client_secret_*.gpg; do
  ./decrypt_file.sh "${gpg_file}"
done
