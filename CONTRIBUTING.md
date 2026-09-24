# Contributing to Pine Creek

Pine Creekは、雪に埋もれた架空の田舎町を舞台にしたオープンソース3Dドライブゲームです。現在の主開発環境は **Godot 4.7.2** です。

## 歓迎する変更

- 車両挙動、タイヤ、サスペンションの改善
- Androidマルチタッチ/UI改善
- 低ポリ3Dモデルや町並み改善
- 雪、天候、ライト、音響
- Pine Creekらしい変な日常ストーリー
- テスト、端末互換性、パフォーマンス改善

## 開発開始

[docs/BUILD.md](docs/BUILD.md) を参照してください。

大きな変更ではfeature branch推奨です。

```bash
git checkout -b feature/better-snow
```

## 方針

- 現行: Godot 4.7.2 / GDScript
- JDK 17 / Android SDK 36
- `app/` とroot Gradle/Java関連は読み取り専用archive。新機能は追加しない
- 駆動方向にカメラ向きを使わない
- タッチ操作はマルチタッチを壊さない
- 実在メーカーのロゴ・車名・無断素材を持ち込まない
- 人物・町・車両は架空として扱う
- UI / ストーリーは日本語を基本にする
- 新規アセットは出所とライセンスを明記する
- GitHub Actionsは使わない。QA/buildは `godot/tools/` のローカルスクリプトを使う
- release keystoreやパスワードをcommitしない

## Pull Request前

```bash
cd godot
./tools/qa_native.sh
./tools/build_android.sh
```

少なくとも `GODOT_TESTS=PASS` と `BUILD_ANDROID=PASS` を確認してください。

PRには「変更内容」「理由」「実機確認の有無」「操作系なら変更前後の挙動」「新規アセットの出所」を書いてください。
