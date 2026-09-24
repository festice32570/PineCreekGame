# Build Guide — Godot版

現在の開発対象は `godot/` です。GitHub Actionsは使用せず、Linuxから直接QA・署名APK生成を行います。旧Java/OpenGL版はarchive扱いです。

## 検証済み環境

- Godot 4.7.2 stable
- Blender 4.5 LTS系
- OpenJDK 17
- Android SDK Platform 36
- Android Build Tools 36.0.0
- Android command-line tools
- Zorin OS / Ubuntu系Linux

## 1. GodotとAndroid SDK

Godot実行ファイルはPATHへ置くか、`GODOT_BIN`で指定できます。

```bash
export GODOT_BIN="$HOME/.local/bin/godot"
export ANDROID_HOME="$HOME/Android/Sdk"
```

Android側には最低限以下を用意します。

```bash
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

JDKは17を使用します。

## 2. プロジェクトを開く

```bash
cd PineCreekGame/godot
godot --editor project.godot
```

## 3. 自動テスト

```bash
godot --headless --path . --script res://tests/run_tests.gd
```

現在の回帰テスト:

- 2本指アクセル + 左
- 2本指アクセル + 右
- 片方の指を離しても別操作を保持
- 車体方向への前進
- 車体後方へのバック
- デジタルステアが瞬時にフルロックしないこと
- 左操作で左、右操作で右へ曲がること
- 後退時の左舵で逆向きヨーが発生すること
- 建物ミッション座標が道路脇の駐車枠へ変換されること
- 駐車枠内のアクションでミッションが進むこと
- 左右応答が極端に非対称でないこと

## 4. Zorin/Linux上の実描画QA

```bash
./tools/qa_native.sh
```

このスクリプトはテスト後、X11/Xwayland上でゲームを実描画して、

- ストーリー選択
- 黄色いミッション駐車枠
- 開始時の車高安定
- 復帰ボタンによる縦道路 / 横道路救出
- 地面下落下からの自動救出
- 直進
- 左旋回
- バック
- 後退左操舵
- タイトル画面

を `godot/build/*.png` へ保存します。

VMware上ではAndroid EmulatorのSwiftShaderに制約があるため、**画面品質のQAはZorinネイティブGodot描画を基準**にしています。

## 5. Android署名鍵

release keystoreはリポジトリへcommitしません。

標準では以下を読みます。

```text
~/.config/pinecreek/android-release.env
```

必要な環境変数:

```bash
export GODOT_ANDROID_KEYSTORE_RELEASE_PATH=/path/to/release.keystore
export GODOT_ANDROID_KEYSTORE_RELEASE_USER=pinecreek
export GODOT_ANDROID_KEYSTORE_RELEASE_PASSWORD=your-secret
```

## 6. release APKを作る

```bash
./tools/build_android.sh
```

成功時は最後に `BUILD_ANDROID=PASS` が出ます。

生成物:

```text
godot/build/PineCreek-Godot-v0.9.1.apk
godot/build/PineCreek-Godot-v0.9.1.apk.sha256
godot/build/apk-manifest-summary.txt
```

スクリプトは `apksigner` と `apkanalyzer` を使い、

- APK署名
- application ID
- versionCode / versionName
- minSdk / targetSdk

を確認します。

## 7. 3Dモデルを作り直す

編集用Blend:

```text
art-source/vehicles/pine_creek_pickup.blend
```

モデル生成スクリプト:

```bash
blender -b --python godot/tools/build_pickup.py
```

出力GLB:

```text
godot/assets/vehicles/pine_creek_pickup.glb
```

## 8. GitHub Actions / Legacy Java

`.github/workflows/` は削除済みで、GitHub Actionsによる自動ビルドは行いません。

現行Godot版の正式経路は `./tools/qa_native.sh` → `./tools/build_android.sh` → git commit/push → GitHub Release です。

旧Java/OpenGL版は `app/`、root Gradle設定、`tools/*.java` にarchiveとして残しますが、CI・Release・新機能開発の対象外です。詳細は `docs/LEGACY_JAVA.md` を参照してください。
