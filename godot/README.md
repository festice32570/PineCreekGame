# Pine Creek — Godot版

Pine CreekをAndroid標準API + 自前OpenGL実装から **Godot 4.7.2** へ段階移行する新エンジン版です。

## 方針

- 車両: `RigidBody3D` + 4輪 `RayCast3D`
- 前輪のみ操舵
- 後輪駆動
- サスペンション / ダンパー / タイヤ横力を接地点へ適用
- 駆動方向は**車体ローカル座標**のみ。カメラ方向を物理へ使わない
- カメラは遅れて追従する独立ノード
- Android操作はpointer IDごとに保持する本物のマルチタッチ
- デスクトップQA: Compatibility renderer
- Android: Mobile renderer (Vulkan)

## 開発環境

- Godot 4.7.2 stable
- Blender 4.5 LTS系
- OpenJDK 17
- Android SDK Platform 36
- Android Build-Tools 36.0.0

## 自動テスト

```bash
cd godot
godot --headless --path . --script res://tests/run_tests.gd
```

テスト対象:

- アクセル + 左右の同時押し
- 指を片方離しても別の操作が維持されること
- 前進が車体前方向であること
- バックが車体後方向であること
- 前進時の左/右操舵が運転者基準で正しいこと
- 後退時に左舵で逆向きヨーが発生すること
- 左右の応答が極端に非対称にならないこと
- 24話の任意エピソード開始
- 建物ミッション座標が道路脇の駐車枠へ変換されること
- 駐車枠内のアクションでミッションが進むこと
- BGM/エンジン/スキッドWAVのインポート

## ローカルQA

```bash
./tools/qa_native.sh
```

Zorin上で実際に3D描画し、ストーリー選択 / 黄色い駐車枠 / スポーン安定 / 道路復帰 / 落下救出 / 直進 / 左旋回 / バック / 後退左操舵 / タイトル画面をPNGへ保存します。

## Android APK

署名鍵とパスワードはリポジトリ外に置きます。

```bash
./tools/build_android.sh
```

生成物:

`build/PineCreek-Godot-v0.8.1-alpha1.apk`

## 3Dモデル

編集用Blenderファイルはリポジトリ直下の `art-source/vehicles/` にあります。

ゲーム側は `assets/vehicles/pine_creek_pickup.glb` を使用します。実在メーカーのロゴや車名は使っていません。

## タイトル画像

`assets/ui/pine_creek_keyart.png` は提供された1254×1254の添付画像そのものを使用しています。再生成・描き直し・JPEG変換はしていません。

SHA-256:

`dd11256d952c29b28249640eaeae1edc25d2bf7c79eecc89ee3ce2072b834d6c`
