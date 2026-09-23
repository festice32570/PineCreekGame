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
- Android SDK Platform 35
- Android Build-Tools 35.0.1

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
- 左操作で左、右操作で右へ旋回すること
- 左右の応答が極端に非対称にならないこと

## ローカルQA

```bash
./tools/qa_native.sh
```

Zorin上で実際に3D描画し、直進 / 左旋回 / バック / タイトル画面をPNGへ保存します。

## Android APK

署名鍵とパスワードはリポジトリ外に置きます。

```bash
./tools/build_android.sh
```

生成物:

`build/PineCreek-Godot-v0.7.0-alpha1.apk`

## 3Dモデル

編集用Blenderファイルはリポジトリ直下の `art-source/vehicles/` にあります。

ゲーム側は `assets/vehicles/pine_creek_pickup.glb` を使用します。実在メーカーのロゴや車名は使っていません。

## タイトル画像

`assets/ui/pine_creek_keyart.jpg` は提供された1254×1254画像を、解像度を変えずJPEG Q95 / 4:4:4へ形式変換したものです。画像生成による描き直しはしていません。

SHA-256:

`f1a78c378d91fe724d4e2ad437ca9cbd9306b8c614a9d7a5ad0114d3942a861c`
