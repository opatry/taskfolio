#!/usr/bin/env bash

# Script to decrypt signing keystore
# see https://help.github.com/en/actions/configuring-and-managing-workflows/creating-and-storing-encrypted-secrets

set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 input_file.gpg"
  exit 1
fi

input_file=${1}

if [ ! -f "${input_file}" ]; then
  echo "${input_file} doesn't exist"
  exit 1
fi

output_filename=$(basename "${input_file}" .gpg)

tmp_dir=$(mktemp -d -t ci-secrets.XXXXXX)
mkdir -p "${tmp_dir}"
output_file="${1:-"${tmp_dir}/${output_filename}"}"
# convert potentially relative path to absolute
output_file="$(cd "$(dirname "${output_file}")"; pwd)/$(basename "${output_file}")"

# --batch to prevent interactive command --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt \
    --passphrase="${PLAYSTORE_SECRET_PASSPHRASE}" \
    --output "${output_file}" "${input_file}"

# output so that caller can retrieve generated output when not provided explicitly
echo "${output_file}"
