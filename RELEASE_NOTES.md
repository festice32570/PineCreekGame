# Pine Creek v0.8.7-alpha1 — スポーン根治・圧雪サウンド修正版

v0.8.6実機確認で残っていた「車が上へ跳ねてから落ちる」「走行音が音階のように上がる」を根本から追い直した検証版です。

## スポーンの根本原因

車両はタイトル/メニュー中に `freeze=true` でしたが、VehicleControllerの `_physics_process()` 自体は動き続け、サスペンションとタイヤのforceを毎フレーム加えていました。

その結果、freeze中にforceが蓄積され、ゲーム開始でfreezeを解除した瞬間にまとめて解放されていました。実測では車高0.600mから直後に上方向速度が約+1.96m/sまで跳ね、その後落下していました。これが「空から降ってくる」見え方の正体です。

## 修正

- VehicleControllerは `freeze` 中にphysics forceを一切加えない
- スポーン時に落下/安定化シミュレーションをしない
- 車両を道路上の静止姿勢 y=0.60m へ直接配置
- 最初の運転入力まで車体をfreezeしたまま維持
- 初回アクセル/ステア/バック入力でphysicsを解放
- カメラとphysics interpolationは道路上の静止姿勢へ同期

## 圧雪サウンド

前版では `snow_roll.wav` の `pitch_scale` を速度0.76→1.18で動かしていたため、加速すると粒状音全体の音程が上がり、効果音ではなく音階のように聞こえていました。

- snow pitchは常に1.0固定
- 速度変化はvolumeだけで表現
- 周期的なサイン波ベースを廃止
- 低域の非周期タイヤ振動 + 短い帯域制限ノイズの雪クランチへ再生成
- 停止時は従来どおりほぼ無音

## QA

修正前のトレースではfreeze解除直後に `vy ≈ +1.96 m/s` まで上方向へ跳ねていました。

修正後は40 physics frames追跡しても車高は約 `0.6000 → 0.6002 m`、垂直速度もほぼ0のままです。

- フリー走行: 初回入力前は完全固定
- ストーリー: 初回入力前は完全固定
- 初回入力でfreeze解除後も垂直ジャンプ/落下なし
- 圧雪AudioStreamPlayerの `pitch_scale == 1.0`
- 既存のフリー走行 / ストーリー選択 / 復帰 / BGM / 操舵QAも継続

## Android

- package: `com.pinecreek.game.godot`
- versionCode: `18`
- version: `0.8.7-alpha1`
- min SDK: 24
- target SDK: 36

3分フル版テーマ `PineCreek_MainTheme_IfItRunsItsACar.wav` も同じReleaseへ添付します。
