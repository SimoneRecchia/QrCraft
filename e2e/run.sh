#!/usr/bin/env bash
# Installs a Forge server for the Minecraft and Forge versions in gradle.properties, puts the jar from build/libs in
# its mods folder, starts it and runs test.mjs against it as a vanilla client, then stops it.
#
#   ./gradlew build && npm ci --prefix e2e && e2e/run.sh
#
# Needs Java 17 and Node 22 on the PATH, and the network for the Forge installer. The server is kept in e2e/server
# between runs (only mods/ and the world are renewed); its log is e2e/server/logs/latest.log.
set -euo pipefail

cd "$(dirname "$0")"
property() { sed -n "s/^$1=//p" ../gradle.properties; }
minecraft=$(property minecraft_version)
forge=$(property forge_version)
mod_id=$(property mod_id)

export SERVER_DIR=server
export RCON_PASSWORD=${RCON_PASSWORD:-qrcraft-e2e}

shopt -s nullglob
jars=(../build/libs/"$mod_id"-*.jar)
if [ ${#jars[@]} -ne 1 ]; then
  echo "expected exactly one jar in build/libs, found ${#jars[@]}: run ./gradlew build first" >&2
  exit 1
fi

if [ ! -f "$SERVER_DIR/run.sh" ]; then
  mkdir -p "$SERVER_DIR"
  echo "installing Forge $minecraft-$forge"
  curl -fsSL -o "$SERVER_DIR/installer.jar" \
    "https://maven.minecraftforge.net/net/minecraftforge/forge/$minecraft-$forge/forge-$minecraft-$forge-installer.jar"
  (cd "$SERVER_DIR" && java -jar installer.jar --installServer > installer.log 2>&1) || { tail -n 30 "$SERVER_DIR/installer.log"; exit 1; }
fi

rm -rf "$SERVER_DIR/mods" "$SERVER_DIR/world" "$SERVER_DIR/logs"
mkdir -p "$SERVER_DIR/mods"
cp "${jars[0]}" "$SERVER_DIR/mods/"
echo 'eula=true' > "$SERVER_DIR/eula.txt"
# offline mode, so the bot needs no account; RCON so the test can give it items
cat > "$SERVER_DIR/server.properties" <<PROPERTIES
online-mode=false
enable-rcon=true
rcon.port=${RCON_PORT:-25575}
rcon.password=$RCON_PASSWORD
server-port=${MC_PORT:-25565}
level-type=minecraft\:flat
generate-structures=false
difficulty=peaceful
spawn-protection=0
view-distance=4
simulation-distance=4
motd=QrCraft end-to-end test
PROPERTIES

(cd "$SERVER_DIR" && exec ./run.sh nogui > console.log 2>&1) &
server=$!
trap 'kill "$server" 2>/dev/null || true' EXIT

echo "starting the server"
for _ in $(seq 1 300); do
  grep -q 'Done (' "$SERVER_DIR/logs/latest.log" 2>/dev/null && break
  if ! kill -0 "$server" 2>/dev/null; then
    echo "the server stopped while starting" >&2
    tail -n 60 "$SERVER_DIR/console.log" >&2
    exit 1
  fi
  sleep 1
done
if ! grep -q 'Done (' "$SERVER_DIR/logs/latest.log" 2>/dev/null; then
  echo "the server did not start in five minutes" >&2
  tail -n 60 "$SERVER_DIR/console.log" >&2
  exit 1
fi

status=0
node test.mjs || status=$?

node rcon.mjs stop > /dev/null || true
for _ in $(seq 1 60); do
  kill -0 "$server" 2>/dev/null || break
  sleep 1
done

# a stack trace through the mod fails the run even when every check passed
if grep -n -B5 -P '^\s+at io\.github\.simonerecchia\.qrcraft\.' "$SERVER_DIR/logs/latest.log"; then
  echo "the server logged an exception thrown through qrcraft" >&2
  status=1
fi
exit $status
