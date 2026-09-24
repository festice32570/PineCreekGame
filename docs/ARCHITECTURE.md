# Architecture — Godot版

## 概要

現行Pine Creekは **Godot 4.7.2 / GDScript / RigidBody3D** を中心としたAndroid向け3Dドライブゲームです。

```text
Main.tscn
 └─ Main.gd
     ├─ VehicleController.gd   車両物理
     ├─ ChaseCamera.gd         追従カメラ
     ├─ TouchControls.gd       Androidマルチタッチ
     ├─ VehicleAudio.gd        V8 / 圧雪 / skid / horn
     ├─ GameAudio.gd           タイトルBGM / 走行BGM / UI音
     └─ StoryDirector.gd       エピソード / ミッション進行
```

## Main.gd

タイトル、ストーリー選択、フリー走行、ストーリーモード、ポーズ、スポーン、道路復帰、HUD、BGMコンテキスト切替をまとめます。

スポーンは車両を上空から落として馴染ませる方式ではありません。道路上の静止姿勢へ直接配置し、最初の運転入力までRigidBodyをfreezeします。

## VehicleController.gd

`RigidBody3D` に4輪RayCastサスペンションと簡易タイヤ力を組み合わせたアーケード寄り車両物理です。前後進は車体方向基準、前輪操舵、デジタル入力平滑化、高速舵角抑制、深雪グリップ低下を扱います。

freeze中はサスペンション/タイヤforceを加えません。これはメニュー中にforceが蓄積して開始時に車が跳ね上がる問題を防ぐ必須ルールです。

## TouchControls.gd

Androidのpointer IDごとにタッチ状態を保持します。アクセル + ステア同時押し、バック、ブレーキ、ホーン、アクション、復帰、カメラドラッグを扱います。

タイトル / ストーリー選択ではゲーム入力処理そのものを無効化し、ScrollContainerへタッチを渡します。

## ChaseCamera.gd

車体yawへ瞬間追従せず、少し遅れて追う三人称カメラです。画面ドラッグ中は一時的なorbitを加えます。

## Audio

`GameAudio.gd` はタイトルテーマ、Pine Creek Radio、UI音を扱います。`VehicleAudio.gd` はV8、圧雪ロードテクスチャ、前進高速スライド時だけのsnow-skid、ホーンを扱います。

車両音はタイトルでは停止し、ゲーム開始後だけ有効です。圧雪音はpitch 1.0固定、snow-skidは通常のバック旋回では鳴りません。

## StoryDirector.gd

現在は24話のエピソードデータとミッション進行を担当します。ただし現行ストーリーは完成仕様ではなく、今後は一本につながったシーズン構成へ再設計します。

## Build / Release

GitHub Actionsは使用しません。

```text
local Godot QA
  -> signed Android release build
  -> package/signature/version verification
  -> Git commit/push
  -> GitHub Release
```

詳細は `docs/BUILD.md` と `docs/RELEASE.md` を参照してください。

## Legacy Java/OpenGL archive

旧 `app/`、root Gradle設定、`tools/*.java` はv0.6.xまでの旧実装を保存するためのアーカイブです。

**現行ゲームのアーキテクチャではなく、CI対象でもありません。** 詳細は `docs/LEGACY_JAVA.md` を参照してください。
