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
