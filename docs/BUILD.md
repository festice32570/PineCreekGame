# Build Guide

## 普通に遊ぶだけなら

GitHubの **Releases** から最新のAPKをダウンロードしてください。

```text
PineCreek-v0.6.3.apk
```

APKをAndroidで開けばインストールできます。

## GitHub Actionsでビルド

1. リポジトリをforkまたはclone
2. 変更をpush
3. GitHubの **Actions** を開く
4. **Build Pine Creek APK** を開く
5. 緑のチェックになるまで待つ
6. 実行画面の **Artifacts** からAPKを取得

Workflow:

```text
.github/workflows/build-apk.yml
```

push時に自動実行されます。

## CIで自動確認する内容

- Java 17
- `VehiclePhysicsSelfTest`
- Android SDK 35
- 安定したdebug署名鍵
- Gradle build
- `apksigner verify`
- `aapt dump badging`
- package名 `com.pinecreek.game`
- APK artifact

## 車両物理テストだけ実行

JDK 17があればAndroid SDKなしで実行できます。

```bash
rm -rf build/physics-test
mkdir -p build/physics-test

javac -encoding UTF-8 -d build/physics-test \
  app/src/main/java/com/pinecreek/game/VehiclePhysics.java \
  tools/VehiclePhysicsSelfTest.java

java -cp build/physics-test com.pinecreek.game.VehiclePhysicsSelfTest
```

## ローカルAndroidビルド

必要なもの:

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- GradleはWrapperで8.11.1を取得

```bash
sdkmanager --licenses
sdkmanager "platforms;android-35" "build-tools;35.0.0"
./gradlew --no-daemon :app:assembleDebug :app:lintDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Android Studio

1. Android Studioでリポジトリルートを開く
2. Gradle Sync
3. SDK 35が無ければSDK Managerから導入
4. `app` configurationを実行

## よくある問題

### 旧版から更新できない

v0.5以前のGitHub Actions製debug APKは、runnerごとにdebug署名が異なっていた可能性があります。

v0.6以降はActions cacheでdebug keystoreを維持します。
v0.5以前から一度だけ更新に失敗する場合は、旧版をアンインストールしてからv0.6を入れてください。

### sdkmanagerでpackageが見つからない

古い `tools` packageを明示的に入れないでください。
Workflowはrunnerのcmdline-toolsにある `sdkmanager` を直接利用します。

### Javaバージョン

AGP 8.9系ではJDK 17を使用してください。