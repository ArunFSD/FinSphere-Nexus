#!/usr/bin/env bash
# End-to-end check of a running stack through the gateway (default http://localhost:7080).
#   docker compose up -d --build && scripts/smoke-test.sh
#   scripts/smoke-test.sh http://192.168.1.20:7080
# Needs JDK 25 on the PATH. On Windows run: java scripts\SmokeTest.java
set -euo pipefail
cd "$(dirname "$0")/.."
exec java scripts/SmokeTest.java "$@"
