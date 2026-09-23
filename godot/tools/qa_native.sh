#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GODOT="${GODOT_BIN:-/home/festice/.local/bin/godot}"
AUTH="$(ls /run/user/$(id -u)/.mutter-Xwaylandauth.* | head -n1)"

cd "$ROOT"

echo "[1/3] Regression tests"
"$GODOT" --headless --path . --script res://tests/run_tests.gd

echo "[2/3] Native visual QA"
DISPLAY=:0 XAUTHORITY="$AUTH" timeout 25s   "$GODOT" --path . --display-driver x11 --rendering-method gl_compatibility   --script res://tests/visual_qa.gd

echo "[3/3] Title capture"
DISPLAY=:0 XAUTHORITY="$AUTH" PINE_CAPTURE_PATH="$ROOT/build/title-qa.png" timeout 15s   "$GODOT" --path . --display-driver x11 --rendering-method gl_compatibility

test -s "$ROOT/build/title-qa.png"
echo "QA_NATIVE=PASS"
