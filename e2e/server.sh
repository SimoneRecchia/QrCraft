# Sourced by run.sh and screenshot.sh: a Forge server for the versions in gradle.properties, the jar from build/libs
# in its mods folder, a fresh flat world, offline mode and RCON. Needs Java 17 and the network for the installer.
#
#   start_server    installs on first use (kept in e2e/server), starts, and waits for "Done"
#   stop_server     stops it over RCON and waits for it to exit

property() { sed -n "s/^$1=//p" ../gradle.properties; }

export SERVER_DIR=server
export RCON_PASSWORD=${RCON_PASSWORD:-qrcraft-e2e}

start_server() {
  local minecraft forge mod_id jars
  minecraft=$(property minecraft_version)
  forge=$(property forge_version)
  mod_id=$(property mod_id)

  shopt -s nullglob
  jars=(../build/libs/"$mod_id"-*.jar)
  shopt -u nullglob
  if [ ${#jars[@]} -ne 1 ]; then
    echo "expected exactly one jar in build/libs, found ${#jars[@]}: run ./gradlew build first" >&2
    return 1
  fi

  if [ ! -f "$SERVER_DIR/run.sh" ]; then
    mkdir -p "$SERVER_DIR"
    echo "installing Forge $minecraft-$forge"
    curl -fsSL -o "$SERVER_DIR/installer.jar" \
      "https://maven.minecraftforge.net/net/minecraftforge/forge/$minecraft-$forge/forge-$minecraft-$forge-installer.jar"
    (cd "$SERVER_DIR" && java -jar installer.jar --installServer > installer.log 2>&1) || { tail -n 30 "$SERVER_DIR/installer.log"; return 1; }
  fi

  rm -rf "$SERVER_DIR/mods" "$SERVER_DIR/world" "$SERVER_DIR/logs"
  mkdir -p "$SERVER_DIR/mods"
  cp "${jars[0]}" "$SERVER_DIR/mods/"
  echo 'eula=true' > "$SERVER_DIR/eula.txt"
  # offline mode, so a client needs no account; RCON so the scripts can drive the server
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
  SERVER_PID=$!
  trap 'kill "$SERVER_PID" 2>/dev/null || true' EXIT

  echo "starting the server"
  for _ in $(seq 1 300); do
    grep -q 'Done (' "$SERVER_DIR/logs/latest.log" 2>/dev/null && return 0
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "the server stopped while starting" >&2
      tail -n 60 "$SERVER_DIR/console.log" >&2
      return 1
    fi
    sleep 1
  done
  echo "the server did not start in five minutes" >&2
  tail -n 60 "$SERVER_DIR/console.log" >&2
  return 1
}

stop_server() {
  node rcon.mjs stop > /dev/null || true
  for _ in $(seq 1 60); do
    kill -0 "$SERVER_PID" 2>/dev/null || return 0
    sleep 1
  done
}
