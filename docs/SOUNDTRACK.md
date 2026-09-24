# Pine Creek Bonus Soundtrack

## PINE CREEK — IF IT RUNS, IT'S A CAR

Pine Creekの約3分（3:00）のメインテーマです。

タイトル画面用の短いループとは別に、まず「一曲として成立するテーマソング」を作るためのフル版です。
150 BPMのヘヴィメタル / ハードロックを軸に、ダウンピッキング風のパワーコード、ダブルキック、ブレイクダウン、リード、低いデスボイス風シャウトを組み合わせています。

実在バンドや既存楽曲のコピーではなく、Pine Creek専用のオリジナルです。ボーカルも外部録音素材ではなく、ローカル音声合成をDSPで低域化・歪ませてデスボイス風に加工しています。

**Full WAV:** [PineCreek_MainTheme_IfItRunsItsACar.wav](https://github.com/festice32570/PineCreekGame/releases/download/v0.8.4-alpha1/PineCreek_MainTheme_IfItRunsItsACar.wav)

- Length: 3:00
- Format: 48 kHz / 16-bit / stereo PCM WAV
- Peak: 約 -1 dBFS
- SHA-256: a171315d38f9954e8c13ec4091cbcba6de18c9273077fffea8e6d8e46f981e6c
- Generator: godot/tools/generate_full_theme.py

### Vocal hooks

> PINE CREEK!
> SNOW TO THE DOORS!
> IF IT RUNS, IT'S A CAR!
> BOB SAYS GOOD TRUCK!
> STILL RUNS!

Chorus:

> PINE CREEK! DRIVE IT TILL IT BREAKS!
> PINE CREEK! WINTER NEVER WAITS!
> IF IT RUNS, IT'S A CAR!
> WELCOME TO PINE CREEK!

Town verse:

> KEVIN'S LOOSE AGAIN!
> THE MAYOR BROUGHT A PLOW!
> THREE TOWN HALLS!
> NOTHING MAKES SENSE!

Breakdown:

> CHECK ENGINE?
> STILL RUNS!
> THEN DRIVE!
> MORE SNOW!
> BIGGER TRUCK!
> BAD IDEA!
> DO IT ANYWAY!

Final:

> PINE CREEK!
> DRIVE IT TILL IT BREAKS!
> IF IT RUNS, IT'S A CAR!
> NOTHING MAKES SENSE!
> WELCOME TO PINE CREEK!
> THAT'S NORMAL!

## Game implementation plan

このフル版はまだゲーム内の3分BGMとしては実装しません。

先にフルテーマを聴いて方向性を確定し、その後に以下を別工程で行います。

1. フル版からゲーム向けの自然なループ区間を作る
2. タイトル画面用ループへ差し替える
3. 通常走行 / 事件 / 追跡 / 吹雪 / BBQ / 冬祭りなどの場面別BGMを増やす
4. 雪路音は現在のノイズ系生成から、より実在感のある「低いタイヤ鳴り + 圧雪の粒立ち + 荷重変化」へ再設計する
