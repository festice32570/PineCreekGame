# Testing — Godot版

現在のPine Creekのテスト対象は `godot/` です。旧Java/OpenGL版のCIは廃止され、GitHub Actionsは使用していません。

## ローカル回帰テスト

```bash
cd godot
godot --headless --path . --script res://tests/run_tests.gd
```

主な確認内容:

- Androidマルチタッチ（アクセル + 左右ステア）
- 前進 / バック / 左右操舵
- 高速時のステア抑制と左右対称性
- 深雪のグリップ低下
- ストーリー選択と24話データ
- ミッション駐車枠と進行条件
- タイトル / 走行BGM切替
- 48 kHz / 16-bit PCM音声
- V8エンジン、圧雪路、snow-skidの再生条件
- バック旋回で旧snow-skidレイヤーが鳴らないこと
- タイトル / メニュー中に車両音が漏れないこと

成功時は `GODOT_TESTS=PASS` が出ます。

## 実描画QA

```bash
cd godot
./tools/qa_native.sh
```

Linux上で実際に描画しながら、タイトル、ストーリー選択、フリー走行、スポーン、前後進、左右操舵、ミッション駐車枠、道路復帰、一時停止などを確認します。

スクリーンショットは `godot/build/*.png` に保存されます。成功時は `VISUAL_QA=PASS` と `QA_NATIVE=PASS` が出ます。

## Android release build検証

```bash
cd godot
./tools/build_android.sh
```

このスクリプトはGodot import、回帰テスト、release APK export、署名、package/version/SDK検証、SHA-256生成までローカルで実行します。成功時は `BUILD_ANDROID=PASS` が出ます。

## 実機確認

Linux VMでは音声ドライバがdummyへフォールバックする場合があるため、可聴音・Android固有のタッチ感・端末GPUについては実機確認が必要です。

特に、タイトルで車両音が漏れないこと、BGM/V8/ホーン/UI音、圧雪音のバランス、バック旋回で旧スキッド音が鳴らないこと、スポーンが道路上から始まること、ストーリー選択のスクロール、復帰ボタンを確認します。

## GitHub Actions

**使用していません。**

`.github/workflows/` は削除済みです。pushごとの自動ビルドは行わず、現行Godot版はローカルQA → 署名APK生成 → GitHub Release公開を正式経路とします。

旧Java版のテスト・ビルド手順はアーカイブ資料としてのみ残しています。新しい開発判断には使用しません。
