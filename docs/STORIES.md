# Pine Creek Stories

Pine Creekの基本は『大事件ではなく、住民が真顔で処理する変な日常』です。

## Current story status

ゲーム内には現在、旧プロトタイプとして24話分のepisode dataがあります。これはv0.8.xまでに遊べる形へするため作った仮ストーリーで、最終Season 1ではありません。

今後の正史・実装基準は **[SEASON1.md](SEASON1.md)** です。

Season 1の正式な背骨:

日本からPine Creekへ来る → Bobからボロいpickupを買う → Jim / Mayor / Kevinと変な日常 → 日本に残していた黒い右ハンドルの古いsports sedanを主人公自身が輸送 → 二台を使い分ける → 冬祭り。

特に黒いsedanは『誰かが勝手に持ってきた謎の車』ではありません。主人公が日本に残していた愛車を、自分の意思でPine Creekへ輸送する設定です。

## Design rules

- 人物・場所・車両は架空
- 住民は妙な状況を妙だと思っていない
- Bobは悪徳業者ではなく本気で『走るなら使える』と思っている
- Jimは収集家のつもりではなく『まだ使えるから置いてある』だけ
- Mayorは無能だから私物車を使うのではなく『自分でやるのが早い』と考える
- Kevinは悪役ではなく町の制度・天候に近い存在
- broad event zoneを基本にし、precision parkingを毎話要求しない
- 1話ごとに最低1つgameplay上の違いを作る
- fail-forwardを優先
- 話の結果を町へ残す
- 主人公が少しずつPine Creek側へ染まる

## Legacy 24 episode data

現行`StoryDirector.gd`の24話は移行期間中は削除しません。Season 1用のmap / mission systemが出来るまで回帰テストとプレイ確認に使います。

Season 1実装が始まったら、旧episodeを一度に全部消すのではなく、新しいepisode dataへ段階的に置き換えます。
