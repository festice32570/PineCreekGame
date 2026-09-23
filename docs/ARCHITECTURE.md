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
           ├─ VehiclePhysics
           ├─ ストーリー進行
           ├─ 3Dワールド
           └─ OpenGL ES描画
```

## 車両物理

v0.6では車両挙動を `VehiclePhysics.java` へ分離しました。

扱う状態:

- signed longitudinal speed
- steering angle
- wheelbase
- heading
- x / z position

基本はキネマティックなbicycle modelです。

```text
yawRate = speed / wheelbase * tan(steeringAngle)
```

### アーケード向け補正

実車シミュレータではなく、スマホで扱いやすい挙動を優先しています。

- 低速では大きく切れる
- 高速では最大舵角を小さくする
- ステアリング入力は瞬間的に最大舵角へ飛ばさずrate limit
- 入力を離すとセルフセンタリング
- 高速ほどyaw responseを弱める
- 深雪ではグリップと最大舵角を落とす
- 牽引中は速度と旋回性能を落とす
- 前後速度の符号でバック時のyaw方向を自然に反転
- 1フレームが長い場合は1/120秒単位へ分割して更新

## 自動テスト

`tools/VehiclePhysicsSelfTest.java` をGitHub Actionsで実行します。

現在確認しているもの:

- 停車中にハンドルだけで車体が回らない
- 前進左 / 前進右の旋回方向
- バック左の車体向きと移動方向
- ブレーキで逆方向へ速度が飛び越えない
- 圧雪路最高速
- 深雪が圧雪路より遅い
- 牽引中の最高速
- ハンドルのセルフセンタリング
- 60Hz / 120Hzのシミュレーション差
- 長時間ランダム入力でNaN / Infinityが発生しない

## カメラ

車体headingをそのままカメラへ直結せず、角度差を指数的に追従させています。

速度に応じてFOVも少し広がります。

## 描画

外部3Dエンジンを使わず、自前のmeshをOpenGL ESへ送っています。

基本mesh:

- cube
- pyramid
- cylinder

ピックアップは複数プリミティブを組み合わせています。

## AudioEngine

BGM・風・エンジン・警笛・イベント音はJavaコードでPCM波形を生成しています。

## セーブ

SharedPreferencesに保存:

- campaign
- stage
- x / z
- heading
- signed speed
- fuel
- special story state
