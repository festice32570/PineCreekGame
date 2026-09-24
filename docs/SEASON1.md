# Pine Creek Season 1 — Canon Design

Status: **design canon for the Godot rebuild**.

旧24話データは試作として残っていますが、今後の実装基準はこのSeason 1です。

## Series spine

日本からPine Creekへ来る
→ Bobからボロい4WDピックアップを買う
→ Jim・Mayor・Kevinと町の変な日常に巻き込まれる
→ 主人公が少しずつPine Creekの常識を覚える
→ 日本に残していた黒い右ハンドルの古いスポーツセダンを**主人公自身が輸送する**
→ pickupとsedanを用途で使い分ける
→ 冬祭りで今までの遊びを総動員
→ 主人公が自分でも気づかないうちに町側へ染まっている

黒いセダンが謎の経路で勝手に現れる案は採用しません。主人公の愛車であり、日本から正規に輸送することを正史にします。

## Gameplay rule

各話は『目的地へ行ってアクションを押す』だけにしません。最低1つ、その話固有または過去に学んだゲームメカニクスを使います。

- broad event zones instead of precision parking
- tow
- cargo stability
- follow NPC vehicle
- search area
- horn clue / horn control
- route choice
- vehicle choice
- deep snow
- blizzard
- timed sectionは中盤以降

失敗は基本fail-forward。ロープが外れた、荷物が崩れた、NPCを見失った等は台詞や再試行で続行します。

## Chapter 1 — Welcome to Pine Creek

### Episode 1 — 走れば車だ

Characters: 主人公、Bob

Places: Bob's Used Cars、短い試運転路、主人公宅

Gameplay:
1. Bobとの会話から車両へ
2. accelerator / brake / steering
3. reverse
4. horn
5. short road test
6. Bobへ戻る
7. pickup購入
8. 主人公宅まで運転

Fail-forward: 道路外は復帰。衝突してもBobは『もともと凹んでた』で続行。

World change: 主人公pickup解禁、Bob's Used Carsが恒久拠点、Pine Creek Radio解禁。

### Episode 2 — 新品

Characters: 主人公、Jim、Bob少し

Places: 主人公宅、Jim garage、gas station、general/tool store

New gameplay: NPC車を追走、複数stop。

Jimは20年以上前の主人公pickupを『新しい』と呼ぶ。自分の庭にはさらに古い車が大量にある。

Player follows Jim and gathers winter washer fluid, shovel, tow gear and other winter supplies.

Fail-forward: Jimを見失うと次の待機地点を地図へ表示。失敗扱いにしない。

World change: pickup荷台に冬用品、Jim garage解禁、Jim宅の車が話数ごとに増えるランニングギャグ開始。

### Episode 3 — これでダメだと分かった

Characters: 主人公、Jim

Places: 郊外雪道、深雪、Jim garage

New gameplay: tow system.

主人公が雪でスタック。Jimが古い牽引ワイヤーで助けようとして切れる。

Jim: 『よし』
主人公: 『何が？』
Jim: 『これがダメだと分かった』

後半では主人公が別の埋まった古い車を牽引してJim宅へ持ち帰る。

World change: tow system解禁、Jim宅の車が1台増える。

### Episode 4 — 小盛り

Characters: 主人公、BBQ店員、住民

Places: BBQ店、住宅地、主人公宅

New gameplay: cargo stability / careful driving.

『小盛り』が普通の店の数人前。主人公のpickupを見た店員が他の配達分まで積む。

急加速、急ブレーキ、大きな衝撃でcargo conditionが悪化。完全失敗にはせず、店員の評価と台詞が変化。

World change: BBQ店が恒久拠点、主人公宅にBBQ用品、radio CM追加。

### Episode 5 — 町長が除雪車

Characters: 主人公、Mayor、Jim、住民

Places: Town Hall、main road、郊外道路、Public Works

Gameplay: follow Mayor + reuse tow + road state change.

Mayorが自分の大型pickupへ除雪bladeを付けて普通に公務をしている。主人公はsupport vehicleとして後ろを走り、邪魔な故障車を安全な場所へ牽引する。

World change: main roadの一部が実際に除雪状態へ変わる。MayorとPublic Works解禁。冬季規則の掲示が増える。

### Episode 6 — ケビン

Characters: 主人公、Jim、Mayor、Bob、Kevin、住民

Places: town-wide search areas

New gameplay: search zone + radio clue + horn interaction.

Kevinが逃げ、町全体が真剣に捜索する。Pine Creek Radioが目撃情報を流し、正確な一点markerではなくsearch areaだけを表示する。

search area内でhornを使うと近くのKevinが反応。発見後は車で追い詰めず、hornと位置取りで誘導する。

最後はKevinがBobのpickup bedへ勝手に乗る。主人公がTown Hallへ運ぶ。

World change: Kevinが常駐NPC化、NO TURKEYS INSIDE看板、radio目撃情報追加。

## Chapter 2 — この町では普通です

7. **市役所が多い** — 3つの役場をたらい回し。行政とtown layoutを紹介。
8. **牛乳** — 牛乳だけの買い物が大量食料配送へ変わり、帰宅後勝手にBBQ。
9. **一番ボロい車** — 『まだ動く』が最重要審査項目の車イベント。
10. **公共交通** — Kevinとpickup bedがなぜか公共交通扱い。
11. **GPSは道路と言っている** — GPSの雪原shortcutを疑い、安全routeを自分で選ぶ。
12. **冬季生活設備** — pickupが単なる車ではなく町の生活インフラになる。

## Chapter 3 — 日本から車が来た

13. **日本に置いてきた車** — 主人公が日本の黒い愛車をPine Creekへ持ってくると決め、輸送・登録準備をする。
14. **日本から車が来た** — 郊外の受渡地点へ行き、輸送truckをPine Creekまで先導。黒い右ハンドルの古いsports sedanが到着。
15. **黒い車** — 初始動、住民の妙に真剣な観察、除雪路で初試乗。sedan解禁。
16. **雪が負けた後に乗る車** — pickupとsedanを同じ道路条件で比較。車両選択の意味を教える。
17. **部品はある** — Jim/Bobとsedan冬点検。部品調達と整備。
18. **日本車サービス** — 主人公が日本車を所有しているだけで町から専門家扱いされる。

Vehicle roles:

- Pickup: deep snow / tow / cargo / rough roads
- Black sedan: plowed road / speed / handling / pursuit / time trial

黒いsedanはpickupの上位互換ではありません。『主人公が好きで乗る車』と『Pine Creekで生きる車』の対比にします。

## Chapter 4 — お前もPine Creekだ

19. **雪が降ると法律が変わる** — 冬季parking規則、車両移動、tow。
20. **Jimの19台目** — 行方不明車search + sedan pursuit + pickup recovery。
21. **停電** — generator / fuel delivery、夜の町。
22. **冬祭り前日** — BBQ、装飾、generator、看板などtown-wide preparation。
23. **Kevinがいない** — blizzard search、horn、tow、vehicle choice。
24. **Pine Creekは平常運転です** — Season 1 mechanics総決算で冬祭りを成立させる。

## Persistent world changes

エピソード終了後に町を完全resetしません。

- Jim宅の車が増える
- Kevin注意看板
- BBQ用品やイベント残骸
- Mayorの冬季規則
- Bob lotの車配置変化
- 道路の除雪状態
- winter festival decorations
- black sedanが主人公宅に常駐

Season 1終了時の町はEpisode 1開始時と見た目から違う状態にします。

## Character arc

序盤の主人公: 『意味が分からない』

中盤: 『Pine Creekならそうなるか』

終盤: 自分からhorn、tow、pickup/sedan選択を使い分けて町の問題を解決する。

最終話翌朝、主人公が無意識に『この町なら普通だろ』と言ってから自分で気づく。

Season 1のテーマは『町の狂気を解決する』ではなく、**主人公がPine Creekの狂気を理解できるようになってしまう**こと。
