# Build Guide

## GitHub Actionsでビルド

この方法が最も簡単です。

1. リポジトリをforkまたはclone
2. 変更をpush
3. GitHubの **Actions** を開く
4. **Build Pine Creek APK** を開く
5. 緑のチェックになるまで待つ
6. 実行画面の **Artifacts** からAPK ZIPを取得
7. ZIP内の `app-debug.apk` をAndroidへインストール

Workflow:

```text
.github/workflows/build-apk.yml
```

push時に自動実行されます。

## ローカルビルド

必要なもの:

- JDK 17
- Android SDK Platform 35
- Android Build Tools 35.0.0
- Gradle 8.11.1

Android SDKのライセンスを承認:

```bash
sdkmanager --licenses
```

必要なSDK:

```bash
sdkmanager "platforms;android-35" "build-tools;35.0.0"
```

ビルド:

```bash
gradle --no-daemon :app:assembleDebug
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

### sdkmanagerでpackageが見つからない

古い `tools` packageを明示的に入れないでください。
現在のWorkflowはrunnerにあるcmdline-toolsの `sdkmanager` を直接利用します。

### Javaバージョン

AGP 8.9系ではJDK 17を使用してください。

### APKが見つからない

```text
app/build/outputs/apk/debug/app-debug.apk
```

を確認してください。
