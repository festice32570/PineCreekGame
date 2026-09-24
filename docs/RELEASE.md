# Release Guide — Godot版

Godot版ではGitHub Actionsを必須にしていません。現在はZorin/Linuxでテスト・release APK生成を行い、その確認済みAPKをGitHub Releaseへ公開します。

## 1. QA

```bash
cd godot
./tools/qa_native.sh
```

`QA_NATIVE=PASS` を確認します。

## 2. Android release build

```bash
./tools/build_android.sh
```

`BUILD_ANDROID=PASS` を確認します。

## 3. 出力確認

```bash
sha256sum build/PineCreek-Godot-v0.8.8-alpha1.apk
```

署名鍵は `~/.config/pinecreek/` 等のリポジトリ外に保存し、絶対にcommitしません。

## 4. Git

ソース、GLB、Blenderソース、ドキュメントをcommitします。  
`godot/build/` と `godot/.godot/` はcommitしません。

## 5. GitHub Release

tagとReleaseを作り、以下をAssetsへ添付します。

```text
PineCreek-Godot-v0.8.8-alpha1.apk
pine_creek_keyart.png
```

Release本文には `RELEASE_NOTES.md` の内容を使用します。

## Legacy CI

既存の `.github/workflows/build-apk.yml` はJava/OpenGL legacy版の確認用です。Godot版の正式リリースAPKをこのworkflowから作る必要はありません。
