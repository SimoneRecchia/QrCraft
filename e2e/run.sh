#!/usr/bin/env bash
# Starts a Forge server with the jar from build/libs (server.sh) and runs test.mjs against it as a vanilla client,
# then stops it.
#
#   ./gradlew build && npm ci --prefix e2e && e2e/run.sh
#
# Needs Java 17 and Node 22 on the PATH. The server's log is e2e/server/logs/latest.log.
set -euo pipefail

cd "$(dirname "$0")"
source ./server.sh

start_server
status=0
node test.mjs || status=$?
stop_server

# a stack trace through the mod fails the run even when every check passed
if grep -n -B5 -P '^\s+at io\.github\.simonerecchia\.qrcraft\.' "$SERVER_DIR/logs/latest.log"; then
  echo "the server logged an exception thrown through qrcraft" >&2
  status=1
fi
exit $status
