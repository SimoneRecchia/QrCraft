#!/usr/bin/env bash
# Writes docs/screenshot.png: a vanilla 1.20.1 client - QrCraft is on the server only - in front of two oak logs, the
# upper one with an item frame on its face holding the map /qr makes of the repository's address. The picture has to
# scan back to that address, or the run fails.
#
#   ./gradlew build && npm ci --prefix e2e && pip install portablemc && e2e/screenshot.sh
#
# Linux only: the client runs on a virtual display (Xvfb) with Mesa's software OpenGL, portablemc fetches it from
# Mojang, and xdotool presses F2. The client's own files are kept in e2e/client between runs.
set -euo pipefail

cd "$(dirname "$0")"
source ./server.sh

CLIENT_DIR=$PWD/client
VIEWER=QrViewer
TEXT=https://github.com/SimoneRecchia/QrCraft
SHOT=../docs/screenshot.png
export DISPLAY=:99
rcon() { node rcon.mjs "$*" > /dev/null; }

start_server

mkdir -p "$CLIENT_DIR"
rm -rf "$CLIENT_DIR/screenshots"
# no first-launch screens and no sound; the game in Italian, so the mod answers in Italian too
cat > "$CLIENT_DIR/options.txt" <<OPTIONS
version:3465
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
tutorialStep:none
pauseOnLostFocus:false
renderDistance:4
simulationDistance:5
lang:it_it
soundCategory_master:0.0
OPTIONS

# a display of its own, without xvfb-run: that one hands its access cookie to its child only, and xdotool and import
# are not its child
Xvfb :99 -screen 0 1280x720x24 -nolisten tcp > xvfb.log 2>&1 &
XVFB_PID=$!
sleep 2

echo "starting the client"
LIBGL_ALWAYS_SOFTWARE=1 portablemc --main-dir "$CLIENT_DIR" start 1.20.1 -u "$VIEWER" --join-server 127.0.0.1 \
  --resolution 1280x720 --jvm-policy system > client.log 2>&1 &
CLIENT_PID=$!
trap 'pkill -f net.minecraft.client.main.Main 2>/dev/null || true; kill "$CLIENT_PID" "$SERVER_PID" "$XVFB_PID" 2>/dev/null || true' EXIT

# the first run downloads the game and its assets
for _ in $(seq 1 600); do
  node rcon.mjs list | grep -q "$VIEWER" && break
  if ! kill -0 "$CLIENT_PID" 2>/dev/null; then
    echo "the client stopped before joining" >&2
    tail -n 60 client.log >&2
    exit 1
  fi
  sleep 1
done
node rcon.mjs list | grep -q "$VIEWER" || { echo "the client did not join in ten minutes" >&2; tail -n 60 client.log >&2; exit 1; }
echo "$VIEWER joined"

# the pointer over the window gives it the keyboard; moved now, before the camera is set, so it turns nothing later
sleep 5
xdotool mousemove 640 360 || true

# away from the spawn first, which is where the logs go
rcon tp "$VIEWER" 0.5 -60 3.6 180 15
rcon time set noon
rcon weather clear
rcon gamerule doDaylightCycle false
rcon gamerule doWeatherCycle false
rcon gamemode creative "$VIEWER"
rcon setblock 0 -60 0 minecraft:oak_log
rcon setblock 0 -59 0 minecraft:oak_log
rcon execute as "$VIEWER" run qr "$TEXT"
sleep 1
# on the south face of the upper log, facing the player; the map is the one /qr just put in the player's hand
rcon summon minecraft:item_frame 0 -59 1 '{Facing:3b}'
rcon data modify entity '@e[type=minecraft:item_frame,limit=1]' Item set from entity "$VIEWER" SelectedItem
rcon item replace entity "$VIEWER" weapon.mainhand with minecraft:air
rcon tp "$VIEWER" 0.5 -60 3.6 180 15

# chunks, the frame and the map's colours reach the client, and the join message fades from the chat
sleep 20
xdotool key F2 || true
for _ in $(seq 1 20); do
  compgen -G "$CLIENT_DIR/screenshots/*.png" > /dev/null && break
  sleep 1
done
if compgen -G "$CLIENT_DIR/screenshots/*.png" > /dev/null; then
  cp "$(ls -t "$CLIENT_DIR"/screenshots/*.png | head -n 1)" "$SHOT"
  echo "screenshot taken with F2"
else
  # F2 never reached the game: take the display instead, which is the same picture
  import -window root "$SHOT"
  echo "F2 did not reach the game; captured the display"
fi

stop_server

convert "$SHOT" ppm:- > shot.ppm
scanned=$(node scan-image.mjs shot.ppm)
echo "the screenshot scans to: $scanned"
[ "$scanned" = "$TEXT" ]
