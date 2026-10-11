#!/bin/bash
# Container health check without curl or wget: asks the service's actuator over a plain TCP socket.
# Usage: healthcheck <port> [path]
set -u
exec 3<>"/dev/tcp/127.0.0.1/$1" || exit 1
printf 'GET %s HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' "${2:-/actuator/health}" >&3
timeout 5 grep -q '"status":"UP"' <&3
