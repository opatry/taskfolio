#!/usr/bin/env bash

# Script to decrypt signing keystore
# see https://help.github.com/en/actions/configuring-and-managing-workflows/creating-and-storing-encrypted-secrets

set -euo pipefail

if [ $# -ne 1 ] && [ $# -ne 2 ] ; then
  echo "Usage: $0 input_file.gpg [output_file]"
  echo "  Decrypts in-place if no output_file is provided"
  exit 1
fi

input_file=${1}
output_file=${2:-""}

if [ -z "${output_file}" ]; then
  output_file="$(dirname "${input_file}")/$(basename "${input_file}" .gpg)"
fi

if [ ! -f "${input_file}" ]; then
  echo "${input_file} doesn't exist"
  exit 1
fi

output_dir=$(dirname "${output_file}")
mkdir -p "${output_dir}"

# convert potentially relative path to absolute
output_file="$(cd "$(dirname "${output_file}")"; pwd)/$(basename "${output_file}")"

# --batch to prevent interactive command --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt \
    --passphrase="${PLAYSTORE_SECRET_PASSPHRASE}" \
    --output "${output_file}" "${input_file}"
