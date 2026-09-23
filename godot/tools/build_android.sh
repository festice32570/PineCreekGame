#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GODOT="${GODOT_BIN:-/home/festice/.local/bin/godot}"
SDK="${ANDROID_HOME:-/home/festice/Android/Sdk}"
SECRETS="${PINE_ANDROID_ENV:-/home/festice/.config/pinecreek/android-release.env}"
APK="$ROOT/build/PineCreek-Godot-v0.8.0-alpha1.apk"

export ANDROID_HOME="$SDK"
export PATH="$SDK/platform-tools:$PATH"

if [[ ! -x "$GODOT" ]]; then
  echo "Godot not found: $GODOT" >&2
  exit 2
fi
if [[ ! -f "$SECRETS" ]]; then
  echo "Release signing env missing: $SECRETS" >&2
  exit 2
fi

source "$SECRETS"

mkdir -p "$ROOT/build"
cd "$ROOT"

echo "[1/5] Scan/import project metadata"
"$GODOT" --headless --path . --import >/tmp/pine-godot-import.log 2>&1

echo "[2/5] Godot regression tests"
"$GODOT" --headless --path . --script res://tests/run_tests.gd

echo "[3/5] Export signed Android release"
rm -f "$APK" "$APK.idsig"
"$GODOT" --headless --path . --export-release Android "$APK"

echo "[4/5] Verify package + signature"
BUILD_TOOLS_DIR="$(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n1)"
APK_ANALYZER="$SDK/cmdline-tools/latest/bin/apkanalyzer"
if [[ -z "$BUILD_TOOLS_DIR" || ! -x "$BUILD_TOOLS_DIR/apksigner" || ! -x "$APK_ANALYZER" ]]; then
  echo "Android verification tools not found" >&2
  exit 3
fi
echo "Using Build Tools: $(basename "$BUILD_TOOLS_DIR")"
"$BUILD_TOOLS_DIR/apksigner" verify --verbose "$APK"

APP_ID="$("$APK_ANALYZER" manifest application-id "$APK")"
VERSION_CODE="$("$APK_ANALYZER" manifest version-code "$APK")"
VERSION_NAME="$("$APK_ANALYZER" manifest version-name "$APK")"
MIN_SDK="$("$APK_ANALYZER" manifest min-sdk "$APK")"
TARGET_SDK="$("$APK_ANALYZER" manifest target-sdk "$APK")"

{
  echo "application_id=$APP_ID"
  echo "version_code=$VERSION_CODE"
  echo "version_name=$VERSION_NAME"
  echo "min_sdk=$MIN_SDK"
  echo "target_sdk=$TARGET_SDK"
} | tee "$ROOT/build/apk-manifest-summary.txt"

[[ "$APP_ID" == "com.pinecreek.game.godot" ]]
[[ "$VERSION_CODE" == "11" ]]
[[ "$VERSION_NAME" == "0.8.0-alpha1" ]]
[[ "$MIN_SDK" == "24" ]]
[[ "$TARGET_SDK" == "36" ]]

echo "[5/5] Artifact digest"
sha256sum "$APK" | tee "$ROOT/build/PineCreek-Godot-v0.8.0-alpha1.apk.sha256"
ls -lh "$APK"
echo "BUILD_ANDROID=PASS"
