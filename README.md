# Pine Creek Game v0.1

Android向けの軽量ゲーム試作です。外部ゲームエンジンや外部アセットは使わず、Android標準APIだけで動きます。

## GitHubでAPKを作る

1. このZIPを解凍します。
2. GitHubで空のリポジトリを作ります。
3. **このフォルダの中身をそのままリポジトリ直下へアップロード**します。
4. Commitします。
5. GitHubの `Actions` → `Build Android APK` を開きます。
6. pushで自動実行されます。必要なら `Run workflow` を押してください。
7. 緑のチェックになったら、その実行画面下部の `Artifacts` から `PineCreekGame-debug-apk` をダウンロードします。
8. ArtifactのZIPを解凍すると `app-debug.apk` が入っています。

## 正しい配置

```text
リポジトリ直下/
├─ .github/
│  └─ workflows/
│     └─ build-apk.yml
├─ app/
├─ build.gradle
├─ gradle.properties
├─ settings.gradle
└─ README.md
```

`PineCreekGame_v01/PineCreekGame_v01/app` のような二重フォルダにはしないでください。

## 内容

- 横画面
- 雪の町を走る簡易疑似3Dドライブゲーム
- 架空のピックアップ
- タッチ操作（左右／アクセル／ブレーキ）
- 日本語ミッション表示
- オフライン
- 通信権限なし

## ビルド構成

- Android Gradle Plugin 8.9.2
- Gradle 8.11.1
- Java 17
- compileSdk / targetSdk 35
- minSdk 23

GitHub Actions側でAndroid SDKとGradleを準備するため、ローカルPCにAndroid StudioがなくてもAPKを生成できます。
