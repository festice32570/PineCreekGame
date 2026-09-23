#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GODOT="${GODOT_BIN:-/home/festice/.local/bin/godot}"
AUTH="$(ls /run/user/$(id -u)/.mutter-Xwaylandauth.* | head -n1)"

cd "$ROOT"
mkdir -p "$ROOT/build"

echo "[1/4] Scan/import project metadata"
"$GODOT" --headless --path . --import >/tmp/pine-godot-qa-import.log 2>&1

echo "[2/4] Regression tests"
"$GODOT" --headless --path . --script res://tests/run_tests.gd

echo "[3/4] Native visual QA"
DISPLAY=:0 XAUTHORITY="$AUTH" timeout 25s   "$GODOT" --path . --display-driver x11 --rendering-method gl_compatibility   --script res://tests/visual_qa.gd

echo "[4/4] Title capture"
DISPLAY=:0 XAUTHORITY="$AUTH" PINE_CAPTURE_PATH="$ROOT/build/title-qa.png" timeout 15s   "$GODOT" --path . --display-driver x11 --rendering-method gl_compatibility

test -s "$ROOT/build/title-qa.png"
echo "QA_NATIVE=PASS"
