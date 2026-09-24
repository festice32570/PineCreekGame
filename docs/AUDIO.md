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

`snow_roll.wav` は連続ホワイトノイズを廃止し、42/67/93 Hzの低いタイヤ・車体振動に、圧雪がトレッドで砕ける短い非整数倍音の粒を重ねた6秒ループです。高速時でも最大 -20 dB程度に抑え、V8エンジン音が主役になるようにしています。

エンジンループも連続ホワイトノイズを使わず、V8の燃焼パルスと低次倍音中心で生成します。
