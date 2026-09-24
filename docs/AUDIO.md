# Audio assets

v0.8.0-alpha1で追加した `godot/assets/audio/` のWAVは、Pine Creek用に新規生成したオリジナル素材です。

## Files

- `pine_creek_radio.wav` — タイトル/走行中BGM
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
