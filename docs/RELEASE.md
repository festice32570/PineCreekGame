# Release Guide

## 1. バージョンを更新

`app/build.gradle`:

```gradle
versionCode 6
versionName '0.6.0'
```

`versionCode` は必ず増やします。

## 2. CHANGELOGを更新

`CHANGELOG.md` に以下を記載します。

- Added
- Changed
- Fixed

## 3. mainへmerge

Pull Requestをmainへmergeします。

## 4. GitHub Actionsを確認

**Build Pine Creek APK** が成功することを確認します。

## 5. APKを実機確認

最低限:

- 新規インストール
- 上書きインストール
- タイトル画面
- 全操作
- バック走行
- ストーリー開始
- セーブ / ロード
- 音
- アイコン

## 6. GitHub Release

例:

```text
v0.5.0
```

Release notesはCHANGELOGを元に作成し、Actionsで生成したAPKを添付します。

## 署名APKについて

現在のWorkflowは開発用debug APKです。
Google Play等へ公開する場合はrelease keystoreを安全なSecretsとして管理し、署名用Workflowを別途用意してください。

秘密鍵をリポジトリへcommitしないでください。
