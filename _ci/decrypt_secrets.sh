#!/usr/bin/env bash

set -euo pipefail

origin=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) || exit

cd "${origin}"
./decrypt_file.sh "${origin}/tasksApp.keystore.gpg"
./decrypt_file.sh "${origin}/google-services.json.gpg" \
                  "${origin}/../tasks-app-android/google-services.json"

mkdir -p "${origin}/../tasks-app-shared/src/jvmMain/composeResources/files"
./decrypt_file.sh "${origin}/client_secret_191682949161-esokhlfh7uugqptqnu3su9vgqmvltv95.apps.googleusercontent.com.json.gpg" \
               "${origin}/../tasks-app-shared/src/jvmMain/composeResources/files/client_secret_191682949161-esokhlfh7uugqptqnu3su9vgqmvltv95.apps.googleusercontent.com.json"

# for now Android & desktop apps use the same GCP Web app credentials, kept split/duplicated in their own source set to ease changing strategy
# it's the same for `store` & `dev` flavors for now, keep in `src/main/assets` but could be dup again in `src/store/assets` & `src/dev/assets` respectively
mkdir -p "${origin}/../tasks-app-android/src/main/assets"
cp "${origin}/../tasks-app-shared/src/jvmMain/composeResources/files/client_secret_191682949161-esokhlfh7uugqptqnu3su9vgqmvltv95.apps.googleusercontent.com.json" \
   "${origin}/../tasks-app-android/src/main/assets"
