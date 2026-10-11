#!/usr/bin/env bash
# Builds every module with its tests, then checks the Docker Compose file.
#   scripts/verify.sh                 # full build and tests
#   scripts/verify.sh -pl auth-service -am   # extra arguments go to Maven
set -euo pipefail
cd "$(dirname "$0")/.."

./mvnw -B -ntp verify "$@"

if command -v docker > /dev/null 2>&1; then
    docker compose config --quiet
    echo "docker-compose.yml is valid"
fi
