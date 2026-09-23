# Release Guide

## 通常の開発push

mainへ通常のcommitをpushすると、

- 車両物理セルフテスト
- Android build
- APK署名検証
- package検証
- Actions Artifact作成

まで実行します。

GitHub Releaseは作りません。

## リリース確定

### 1. バージョン更新

`app/build.gradle`:

```gradle
versionCode 6
versionName '0.6.0'
```

### 2. ドキュメント更新

- `CHANGELOG.md`
- `RELEASE_NOTES.md`
- `README.md`

### 3. 通常pushでCI確認

Releaseを出す前に、一度通常のcommitでActionsが全成功することを確認します。

### 4. 最終commit

commit messageに **[release]** を含めます。

例:

```text
[release] Pine Creek v0.6.0
```

このpushでActionsがテスト・ビルド・検証した後、自動的にGitHub Releaseへ

```text
PineCreek-v0.6.0.apk
```

をアップロードします。

### 5. 手動再公開

Actionsの **Run workflow** でもRelease publish stepが実行されます。

## APK確認項目

最低限:

- 新規インストール
- タイトル画面
- 前進
- 左右ステアリング
- バック
- ブレーキ
- 復帰
- ストーリー開始
- セーブ / ロード
- 音
- アイコン

CIでは実機タッチ操作までは再現できないため、最終的な操作感は実機テストも必要です。

## 署名について

Releasesへ出すAPKは現在 **debug signed APK** です。

GitHub Actions cacheでdebug keystoreを維持し、v0.6以降のCIビルド同士では上書きインストールしやすくしています。

Google Playへ公開する場合はrelease keystoreをGitHub Secrets等で安全に管理する別の署名フローを用意してください。

秘密鍵をリポジトリへcommitしないでください。
