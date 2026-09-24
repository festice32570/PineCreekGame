# Release Guide — Godot版

Godot版ではGitHub Actionsを使用しません。Zorin/Linuxでテスト・release APK生成を行い、その確認済みAPKをGitHub Releaseへ公開します。旧Java/OpenGL版はarchiveとして残すだけで、CI・Release対象にはしません。

**v0.9.0以降は `--prerelease` を付けず、通常のGitHub Releaseとして公開します。** 0.8.xのPre-releaseは開発履歴として残します。通常Release化はコンテンツ完成を意味せず、ストーリー・3Dモデル・マップ・NPC表現は引き続き開発中です。

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
sha256sum build/PineCreek-Godot-v0.9.0.apk
```

署名鍵は `~/.config/pinecreek/` 等のリポジトリ外に保存し、絶対にcommitしません。

## 4. Git

ソース、GLB、Blenderソース、ドキュメントをcommitします。  
`godot/build/` と `godot/.godot/` はcommitしません。

## 5. GitHub Release

tagとReleaseを作り、以下をAssetsへ添付します。

```text
PineCreek-Godot-v0.9.0.apk
PineCreek-Godot-v0.9.0.apk.sha256
PineCreek_MainTheme_IfItRunsItsACar.wav
PineCreek_MainTheme_IfItRunsItsACar.wav.sha256
pine_creek_keyart.png
```

Release本文には `RELEASE_NOTES.md` の内容を使用します。

## CI / Legacy

`.github/workflows/` は削除済みです。pushによる自動CIはありません。

旧Java/OpenGL版はarchiveとして残しますが、Release対象ではありません。詳細は `docs/LEGACY_JAVA.md` を参照してください。
