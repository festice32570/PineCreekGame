# Pine Creek v0.9.1 — 田舎マップ / Season 1 テスト版

v0.9.1は、車両基盤が安定したv0.9.0の次の段階として、**広い田舎マップと新Season 1を実際のゲームへ入れ始めたテストビルド**です。

GitHub上ではv0.9.0からの方針どおり、Pre-releaseではなく通常Releaseとして公開します。ただし内容面はまだ開発途中で、今回の建物・道路景観・人物の多くはgrayboxまたは試作モデルです。

## 広いPine Creek graybox

- 約4.5 km × 4.5 kmの走行可能範囲
- 23本のmodular road segment
- 道路総延長 約15.4 km
- 小さなtown coreと、数百m〜km離れた田舎landmark
- Bob's Used Cars / 主人公宅 / Jim's Garage / Public Works
- Gas & Coffee / General / Tools / 3つのTown Hall
- 住宅cluster / forest road / snowfield
- 260本相当の遠景treeをMultiMesh化

建物を密集させず、町の外へ出ると「何もない雪道」が長く続く田舎らしい距離感を優先しています。

## 復帰処理

以前は小さい旧マップの2本の道路だけを前提にしていました。v0.9.1では全road segmentから最寄りのcenterlineを計算し、広いマップのどこで復帰しても近い道路へ戻す方式へ変更しています。

## Season 1開始

新正史の最初のvertical sliceとして、**Season 1 第1話「走れば車だ」**を実装開始しました。

流れ:

- BOB'S USED CARSでBobとボロい4WDを見る
- 西側の田舎道へ試運転
- hornが動くか確認
- Bobの店へ戻る
- 購入したpickupで主人公宅へ帰る

StoryDirectorへ「目的地へ入れば自動進行するdrive stage」を追加し、毎回正確な黄色い駐車枠へ停める旧方式から、**広いevent zone**へ変更しています。

旧24話は一度に削除せず、Season 1の新episodeへ段階的に置き換えます。

## Bob / portrait prototype

Blenderで作ったBobの低ポリprototypeを実際に中古車店へ配置しました。

会話UIでは、同じBob 3D modelからBlenderでrenderしたtransparent portraitを表示します。主人公portraitも同じpipelineで生成しています。

つまり今後は「ゲーム内NPC」と「会話の挿絵」が別人にならない方式で制作します。

## QA

Linux native QAで以下を確認しています。

- rural graybox生成
- 15 km以上のroad layout
- MultiMesh forest
- Bob NPC配置
- broad event zone
- Bob portrait dialogue
- Season 1 Episode 1のaction / drive / horn progression
- 1 km以上world originから離れたforest roadでvehicle physicsが安定
- 新road networkでの最寄り道路復帰
- 従来のspawn / steering / reverse / audio / pause / touch controls回帰

`GODOT_TESTS=PASS`

`VISUAL_QA=PASS`

`QA_NATIVE=PASS`

Android release buildも署名・package・SDK・version検証まで行います。

Linux VMは実音声出力がdummy driverになるため、**可聴音とAndroid実機での広いマップの体感/性能は端末で確認してください。**

## 未完成 / 仮置き

このReleaseで完成していないもの:

- Season 1第2話以降の新gameplay
- tow / cargo / NPC followなどSeason 1専用system
- Jim / Mayor / Kevinの最終3D model
- 建物のfinal model
- road decoration / snowbanks / rural propsの本仕上げ
- 黒い日本のsedan
- 最終的なNPC animation
- story dialogueの本仕上げ
- map cell streaming / full LOD tuning

ここからストーリー、NPC、モデル、町の履歴を順番に追加し、Pine Creekを**もっとストーリーが濃く、もっと田舎で、もっと狂ったゲーム**へ進化させます。

## Android

- package: `com.pinecreek.game.godot`
- versionCode: `22`
- version: `0.9.1`
- min SDK: 24
- target SDK: 36

3分フル版テーマ `PineCreek_MainTheme_IfItRunsItsACar.wav` も同じReleaseへ添付します。
