# Pine Creek Map Design — Rural Scale

Season 1のマップは、町の密度より**田舎らしい距離感**を優先します。

車両中心のゲームなので、徒歩ゲームのように建物を隙間なく並べません。町の中心を出ると、数百メートル単位で何もない雪道・林・空き地・農地・送電線・道路標識だけが続く時間を意図的に作ります。

## 1. Graybox target

最初の本格grayboxは約 **4.5 km x 4.5 km** の走行可能範囲を目標にします。

- Town core: 約 500 x 700 m
- 住宅地: core外周へ低密度に広げる
- 主人公宅: coreから約700〜1,000 m
- Bob中古車店: core外縁、幹線道路沿い
- Jim宅/garage: coreから約1.0〜1.4 km
- BBQ店: core寄りの幹線道路沿い
- 役場群: core内と外縁に分散
- Public Works: coreから約800〜1,200 m
- Gas station / supermarket: 幹線道路沿い
- Forest road / snowfield: coreから2 km以上

Season 1完成時の道路総延長は **12〜18 km程度** を目安にします。一直線ではなく、幹線道路、住宅路、林道、雪原アクセス路を組み合わせます。

将来は同じ構造で約6 km四方まで拡張できる余白を残します。

## 2. Rural feeling rules

田舎らしさは単純に地面を巨大化するだけでは出ません。次の距離ルールを使います。

- 建物cluster間に300〜900 m程度の空白を作る
- Town coreを出ると街灯密度を急に落とす
- 森・雪原・空き地をランドマーク間の『間』として使う
- 道路沿いに毎回建物を置かない
- 看板、電柱、郵便受け、除雪ポールなど小物で距離を感じさせる
- 遠景に山・森・給水塔・送電線など大きなシルエットを置く
- 目的地同士を最短距離だけで結ばず、道路の曲がりで視界を切る

目安として、通常走行で『町の端から遠いランドマークまで2〜4分』くらいを狙います。長すぎる移動はラジオ・会話・天候変化で間を持たせます。

## 3. Season 1 first six episodes — required places

第1〜6話のためにgraybox段階で必要な場所:

1. 主人公宅
2. Bob's Used Cars
3. Jim宅 / garage / 車置き場
4. BBQ店
5. Main Town Hall
6. Public Works yard
7. Gas station
8. General store / tool store
9. 住宅地cluster
10. 郊外の雪道
11. 深雪のスタック地点
12. Kevin捜索用の複数search zone

最初から完成モデルを置かず、色分けした箱・簡易看板・道路・雪面だけでゲームプレイを確認します。

## 4. World layout concept

```text
                      FOREST / SNOWFIELD
                          /
                Jim Garage ---- Forest Road
                    |               |
                    |          deep snow
                    |
     Protagonist ---+---- Town Core ---- Public Works
          |                 |   |
          |                 |   +--- Town Hall cluster
      rural road            |
          |              BBQ / store
          |                 |
      Bob's Used Cars --- Gas Station ---- highway / map edge
```

これは距離関係の概念図で、最終道路形状ではありません。

## 5. Streaming / performance plan

広いマップを一枚の高密度meshとして作りません。

- Worldを約512 m単位のcellへ分割
- 近距離cellだけ高詳細propを表示
- 遠距離は低poly silhouette / billboard / simplified mesh
- TreesはMultiMesh中心
- 雪原は低密度terrain mesh + materialで表現
- 建物内部は基本的に作らず、必要なイベントだけ別scene
- NPCは遠距離で停止または非表示
- parked vehicleも距離でLOD / visibility制御
- collisionは見た目meshより大幅に単純化

道路は大量の高解像度textureに頼らず、curve/segmentデータと再利用materialを中心にします。

## 6. Repository-friendly map data

マップを広くしてもGit容量を膨らませないため、可能な部分はデータとスクリプトで持ちます。

- road centerline / landmark: text resource / JSON / GDScript data
- terrain seed / coarse height samples: small text or compact binary
- repeated trees / poles / snowbanks: one asset + placement data
- buildings: modular GLBを再利用
- huge monolithic world.blendは作らない
- 4K/8K terrain textureを大量にcommitしない

地形の広さとリポジトリ容量は別問題として扱います。広い空間ほど『少ない種類のassetを上手く反復する』方針にします。

## 7. Graybox success criteria

次工程へ進める条件:

- 第1〜6話の全ルートが実際に走れる
- town coreだけに目的地が集中していない
- 町から外へ出た瞬間に田舎の空白を感じる
- 最遠地点までの移動が退屈すぎない
- pickupで深雪・幹線道路・住宅路の違いが分かる
- Android実機でcell分割後も安定して走れる

この条件を満たしてからfinal modelingへ進みます。
