# Audio assets

v0.8.0-alpha1で追加した `godot/assets/audio/` のWAVは、Pine Creek用に新規生成したオリジナル素材です。

## Files

- `title_theme.wav` — タイトル画面用のオリジナル・ハードロック/ヘヴィメタル風テーマ
- `pine_creek_radio.wav` — 通常走行中のPine Creek Radio BGM
- `engine_idle.wav` — 古いピックアップ風エンジンループ
- `snow_roll.wav` — 圧雪路ロードノイズ
- `snow_skid.wav` — 横滑り時の雪上スキッド
- `truck_horn.wav` — 2音ホーン
- `ui_click.wav` — メニュー操作音
- `pause_cue.wav` — ポーズ音

外部楽曲、録音済みの第三者サンプル、実在車両から抽出した音は使用していません。
生成コードは `godot/tools/generate_audio.py` にあります。
リポジトリ内の他のPine Creek用素材と同様、特記がない限りMIT Licenseの対象です。

## Playback policy

- BGMは打ち込み/生成データをオフラインでレンダリングしたローカルPCM WAVを使用します。実行時MIDI音源には依存しません。
- 短い効果音もPCM WAVを基本にします。現在の容量では圧縮よりAndroidでの再生安定性と低CPU負荷を優先します。
- ループ音は `loop_begin=0` と実サンプル数の `loop_end` をコード側でも明示し、0-length loopを防ぎます。
- 現在の生成サンプルレートは48 kHz / 16-bit PCMです。


## Music contexts

- タイトル / ストーリー選択: title_theme.wav
- 通常走行: pine_creek_radio.wav
- 今後は事件、吹雪、追跡、祭りなどの場面ごとにBGMコンテキストを追加できるよう PineGameAudio 側で切り替えます。
- タイトルテーマは特定の既存楽曲を模倣せず、Pine Creek用に打ち込み・合成したオリジナル曲です。

## Vehicle noise policy

停車中に雪路・スキッド用ノイズを鳴らし続けると「サー」というノイズ床になるため、低速時は雪路音を -80 dB、横滑りが無い時はスキッド音を -80 dB までゲートします。

`snow_roll.wav` は連続ホワイトノイズと周期的なサイン波を廃止し、低域の非周期タイヤ振動に、圧雪がトレッドで砕ける細かい帯域制限ノイズの粒を多数重ねた9秒ループです。粗い「サッ、サッ」という単発感を避けるため、18–38msの小さなクランチを約45–110ms間隔で不規則に重ね、さらに低い接地ラフネスを薄く常時鳴らしています。

速度差は音量だけで表現し、`pitch_scale` は常に 1.0 です。旧方式のように走行速度で音源全体をピッチアップしないため、加速時に「音階が上がる」ようには聞こえません。v0.8.9ではミックス上の最大音量もさらに下げ、V8より15–22dBほど後ろに置く方針にしています。

エンジンループも連続ホワイトノイズを使わず、V8の燃焼パルスと低次倍音中心で生成します。
