# Architecture

## 概要

Pine CreekはAndroid標準APIとOpenGL ES 2.0を使った軽量構成です。

```text
MainActivity
 ├─ タイトル / ストーリー選択 / HUD / タッチ操作
 ├─ SharedPreferences セーブ
 ├─ AudioEngine
 └─ GameSurface
      └─ GameRenderer
           ├─ ゲームループ
           ├─ 車両物理
           ├─ ストーリー進行
           ├─ 3Dワールド
           └─ OpenGL ES描画
```

## 車両物理

v0.5以降は速度ベクトルを直接左右へずらす方式ではなく、以下で計算します。

- signed longitudinal speed
- steering angle
- wheelbase
- yaw rate

概念式:

```text
yawRate = speed / wheelbase * tan(steeringAngle)
```

speedが負ならyawRateも反転するため、バック時の操舵方向も自然に変わります。

## 描画

外部3Dエンジンを使わず、自前のmeshをOpenGL ESへ送っています。

基本mesh:

- cube
- pyramid
- cylinder

ピックアップは複数プリミティブを組み合わせています。

## AudioEngine

著作権管理を簡単にするため、現在のBGM・風・エンジン・警笛・イベント音はJavaコードでPCM波形を生成しています。

## セーブ

SharedPreferencesに保存:

- campaign
- stage
- x / z
- heading
- signed speed
- fuel
- special story state
