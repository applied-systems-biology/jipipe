#!/bin/bash
set -euo pipefail

PACKAGE_PATH="${1:?Usage: build.sh <path-to-linux64-tar.gz> <version>}"
VERSION="${2:?Usage: build.sh <path-to-linux64-tar.gz> <version>}"

IMAGE_NAME="appsysbiohkijena/jipipe"

echo "Building Docker image ${IMAGE_NAME}:${VERSION} from ${PACKAGE_PATH}"

docker build \
    --build-arg PACKAGE_PATH="${PACKAGE_PATH}" \
    -t "${IMAGE_NAME}:${VERSION}" \
    -t "${IMAGE_NAME}:latest" \
    -f Dockerfile \
    .

echo "Built ${IMAGE_NAME}:${VERSION} and ${IMAGE_NAME}:latest"
