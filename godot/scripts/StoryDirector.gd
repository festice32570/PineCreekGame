class_name PineStoryDirector
extends Node

signal objective_changed(chapter: String, objective: String)
signal dialogue_requested(speaker: String, line: String)
signal marker_changed(position: Vector3, visible: bool)

var vehicle: PineVehicle
var episode_index := 0
var stage_index := 0
var horn_count := 0
var started := false
var completed_cycles := 0
var stage_timer := -1.0
var _last_timer_second := -1
var rng := RandomNumberGenerator.new()

var episodes := [
    {
        "title":"Season 1 第1話　走れば車だ",
        "summary":"ボブからPine Creek最初のボロい4WDを買って試運転する。",
        "stages":[
            {"pos":PineWorldLayout.BOB_EVENT,"mode":"action","radius":13.0,
             "objective":"BOB'S USED CARSでボブと中古ピックアップを見る",
             "speaker":"ボブ","line":"普通に走ればいい？　だったら全部だ。まずこれを乗ってみろ。"},
            {"pos":PineWorldLayout.TEST_ROUTE_A,"mode":"drive","radius":22.0,
             "objective":"試運転：西側の田舎道を分岐まで走る",
             "speaker":"ボブ","line":"エンジンは掛かってる。ハンドルも付いてる。今のところ満点だ。"},
            {"pos":PineWorldLayout.TEST_ROUTE_B,"mode":"horn","required":1,"radius":22.0,
             "objective":"試運転：分岐で警笛を1回鳴らしてホーンを確認する",
             "horn_feedback_speaker":"ボブ","horn_success_line":"ホーンも生きてる。鹿とジムにはそれで十分だ。",
             "speaker":"ボブ","line":"よし。止まるし曲がるし鳴る。車だ。"},
            {"pos":PineWorldLayout.BOB_EVENT,"mode":"drive","radius":20.0,
             "objective":"BOB'S USED CARSへ戻る",
             "speaker":"ボブ","line":"どうだ？　走っただろ。つまり問題ない。"},
            {"pos":PineWorldLayout.HOME_EVENT,"mode":"drive","radius":24.0,
             "objective":"購入したピックアップで新居まで帰る",
             "speaker":"主人公","line":"……日本にいた時は、車を買うってもう少し確認事項があった気がする。"}
        ]
    },
    {
        "title":"第1話　町長と除雪車",
        "summary":"除雪車を救うための除雪車が必要になった。",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"町役場で町長の話を聞く",
             "speaker":"町長","line":"除雪車が埋まった。除雪車を救うために除雪が必要だ。"},
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"公共事業ヤードの除雪車を確認する",
             "speaker":"作業員","line":"大丈夫だ。春になれば自然に出てくる。たぶん四月だ。"}
        ]
    },
    {
        "title":"第2話　ジムの18台目",
        "summary":"17台だった車が一晩で18台になった。",
        "stages":[
            {"pos":Vector3(13,0,30),"mode":"action","objective":"ジムのガレージへ行く",
             "speaker":"ジム","line":"17台目？　昨日までの話だ。今朝18台目になった。"},
            {"pos":Vector3(-13,0,18),"mode":"action","objective":"ボブに18台目の由来を確認する",
             "speaker":"ボブ","line":"あれは売ってない。置いてただけだ。値札が付いてただけで。"}
        ]
    },
    {
        "title":"第3話　役場のケビン",
        "summary":"七面鳥ケビンとの外交手段は警笛3回。",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"horn","required":3,"objective":"役場前で警笛を3回鳴らしてケビンを説得する",
             "speaker":"町長","line":"七面鳥のケビンが正面玄関を占拠した。交渉は警笛で頼む。"}
        ]
    },
    {
        "title":"第4話　郵便受けの冬季移住",
        "summary":"郵便受けが除雪のたびに住所を変える。",
        "stages":[
            {"pos":Vector3(-12,0,49),"mode":"action","objective":"雪山に刺さった郵便受けを確認する",
             "speaker":"住民","line":"昨日まではうちの前にあった。今日は三軒先に住んでる。"},
            {"pos":Vector3(12,0,49),"mode":"action","objective":"郵便受けを本来の家へ届ける",
             "speaker":"住民","line":"ありがとう。明日また移住したら、もう本人の意思ってことにする。"}
        ]
    },
    {
        "title":"第5話　春の誤報",
        "summary":"+1℃だけで春祭りを始めた町を止める。",
        "stages":[
            {"pos":Vector3(13,0,13),"mode":"action","objective":"ガソリンスタンドの『+1℃』表示を調べる",
             "speaker":"店員","line":"温度計の下に排気管がある。春はまだ来てない。"},
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"中止になった春祭りのBBQを回収する",
             "speaker":"店員","line":"祭りは中止。でも肉は中止できない。食べれば解決だ。"}
        ]
    },
    {
        "title":"第6話　公共交通が発生",
        "summary":"荷台と折りたたみ椅子で町営バスが誕生した。",
        "stages":[
            {"pos":Vector3(-13,0,18),"mode":"action","objective":"ボブの『町営バス』を確認する",
             "speaker":"ボブ","line":"荷台に折りたたみ椅子を置いた。つまりバスだろ？"},
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"町役場へバス停の看板を届ける",
             "speaker":"町長","line":"公共交通ができた。運行日はボブが起きた日だ。"}
        ]
    },
    {
        "title":"第7話　雪だるま市議会",
        "summary":"議員不在なので雪だるまが定足数を満たす。",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"役場前の臨時市議会を確認する",
             "speaker":"町長","line":"議員が来ないから雪だるまで定足数を満たした。異議は溶けるまで受け付けない。"},
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"議会用の巨大コーヒーをBBQから運ぶ",
             "speaker":"店員","line":"12リットル。会議用なら普通だよ。"}
        ]
    },
    {
        "title":"第8話　凍った誕生日ケーキ",
        "summary":"巨大ケーキを完全凍結前に届ける。",
        "stages":[
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"BBQで巨大な誕生日ケーキを受け取る",
             "speaker":"店員","line":"ケーキは凍ってる。落としてもたぶん床の方が負ける。"},
            {"pos":Vector3(12,0,49),"mode":"timed","duration":46.0,"objective":"ケーキが完全凍結する前にHOUSE 12へ届ける",
             "speaker":"住民","line":"間に合った！　ロウソクは刺さらないからドリルを持ってくる。"}
        ]
    },
    {
        "title":"第9話　非常事態レベル紫",
        "summary":"町長のコーヒー切れは赤より上の非常事態。",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"町役場で『非常事態レベル紫』を確認する",
             "speaker":"町長","line":"コーヒーが切れた。赤より上だ。紫だ。異論はない。"},
            {"pos":Vector3(13,0,13),"mode":"timed","duration":34.0,"objective":"GAS & COFFEEへ緊急コーヒーを取りに行く",
             "speaker":"店員","line":"町長用？　じゃあバケツでいいね。"},
            {"pos":Vector3(14,0,-4),"mode":"timed","duration":38.0,"objective":"コーヒーが冷める前に役場へ戻る",
             "speaker":"町長","line":"よし。非常事態レベル紫を解除する。次は普通の吹雪だ。"}
        ]
    },
    {
        "title":"第10話　シカではありません",
        "summary":"三時間動かないシカの正体を調べる。",
        "stages":[
            {"pos":Vector3(-12,0,49),"mode":"action","objective":"HOUSE 11からの『庭にシカ』通報を確認する",
             "speaker":"住民","line":"三時間動かない。根性のあるシカだと思った。"},
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"PUBLIC WORKSに正体を報告する",
             "speaker":"作業員","line":"それ芝生飾りだ。雪から半分出ると毎年一回は通報される。"}
        ]
    },
    {
        "title":"第11話　看板が旅に出た",
        "summary":"町の入口看板が風で別住所へ転居した。",
        "stages":[
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"風で消えた町入口の看板について聞く",
             "speaker":"作業員","line":"看板が風に乗って旅に出た。本人の意思は確認してない。"},
            {"pos":Vector3(-12,0,49),"mode":"timed","duration":42.0,"objective":"HOUSE 11付近まで飛んだ町看板を回収する",
             "speaker":"住民","line":"うちの庭が今日から町の入口になったらしい。"},
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"看板を町役場へ届ける",
             "speaker":"町長","line":"戻ったか。次に飛んだら住所変更届を書かせる。"}
        ]
    },
    {
        "title":"第12話　ケビン給油所占拠",
        "summary":"ケビンが給油ノズル前で動かず燃料行政が停止。",
        "stages":[
            {"pos":Vector3(13,0,13),"mode":"action","objective":"GAS & COFFEEで給油できない理由を聞く",
             "speaker":"店員","line":"ケビンが3番ポンプを予約してる。予約した覚えはないらしい。"},
            {"pos":Vector3(13,0,13),"mode":"horn","required":2,"objective":"警笛を2回鳴らしてケビンに順番を譲ってもらう",
             "speaker":"ケビン","line":"ゴボッ。給油はしないが場所は譲る、という顔をしている。"}
        ]
    },
    {
        "title":"第13話　町内放送2014",
        "summary":"12年前の町内放送が復活し住民が全部従い始めた。",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"役場で古い町内放送の原因を確認する",
             "speaker":"町長","line":"2014年の録音が勝手に流れてる。住民が真面目だから困る。"},
            {"pos":Vector3(14,0,-37),"mode":"timed","duration":40.0,"objective":"PUBLIC WORKSの放送設備を停止しに行く",
             "speaker":"作業員","line":"止めた。次の放送は『図書館でFAX講習会』だった。危なかった。"}
        ]
    },
    {
        "title":"第14話　ジムの19台目",
        "summary":"18台目を数え終えた直後に19台目が出現。",
        "stages":[
            {"pos":Vector3(13,0,30),"mode":"action","objective":"ジムから19台目について事情聴取する",
             "speaker":"ジム","line":"安心しろ。19台目は部品取りだ。エンジンもタイヤも車検証もあるけど部品取りだ。"},
            {"pos":Vector3(-13,0,18),"mode":"action","objective":"ボブの在庫表と照合する",
             "speaker":"ボブ","line":"その車なら昨日まで20台目だった。1台売れたから19台目になった。"}
        ]
    },
    {
        "title":"第15話　四月なのに一月",
        "summary":"雪が残っているのでカレンダー故障説が有力になる。",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"町役場の『四月は誤植』会議へ行く",
             "speaker":"町長","line":"外が白い。つまり一月だ。カレンダー側が間違ってる可能性が高い。"},
            {"pos":Vector3(13,0,13),"mode":"action","objective":"店のカレンダーでも四月か確認する",
             "speaker":"店員","line":"こっちも四月。でも冷凍庫より外が寒い。判定は引き分けだ。"}
        ]
    },
    {
        "title":"第16話　除雪車の追悼式",
        "summary":"壊れたと思われた除雪車が式典中に普通に始動する。",
        "stages":[
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"PUBLIC WORKSの除雪車追悼式に参加する",
             "speaker":"作業員","line":"去年から動かない。今日は静かに見送ろう。キー？　そこに刺さってる。"},
            {"pos":Vector3(14,0,-37),"mode":"horn","required":1,"objective":"式典の合図として警笛を1回鳴らす",
             "speaker":"作業員","line":"今エンジン掛かったな。追悼式は復帰祝いに変更。ケーキはそのまま。"}
        ]
    },
    {
        "title":"第17話　コーヒー氷河",
        "summary":"屋外放置されたコーヒーが固体貨物になった。",
        "stages":[
            {"pos":Vector3(13,0,13),"mode":"action","objective":"凍ったコーヒー樽を受け取る",
             "speaker":"店員","line":"液体じゃなくなったからこぼれない。配送には向いてる。"},
            {"pos":Vector3(14,0,-4),"mode":"timed","duration":32.0,"objective":"暖房で溶け切る前に町役場へ届ける",
             "speaker":"町長","line":"ちょうどいい。今日はスプーンで飲む。"}
        ]
    },
    {
        "title":"第18話　黄色い雪かき失踪",
        "summary":"町で唯一の黄色い雪かきが消え、捜索本部が設置された。",
        "stages":[
            {"pos":Vector3(12,0,49),"mode":"action","objective":"HOUSE 12で最後の目撃情報を聞く",
             "speaker":"住民","line":"昨日ここに立て掛けた。今朝は雪しかない。つまり雪が怪しい。"},
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"BBQ前の雪山を調べる",
             "speaker":"店員","line":"あった。黄色だからカラシ用だと思って借りてた。"}
        ]
    },
    {
        "title":"第19話　GPSが雪山を道路認定",
        "summary":"配達員のGPSが雪山を最短ルートとして案内する。",
        "stages":[
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"BBQで迷子の配達員から荷物を預かる",
             "speaker":"配達員","line":"ナビが『右折』って言った場所、壁みたいな雪だった。"},
            {"pos":Vector3(-12,0,49),"mode":"timed","duration":44.0,"objective":"ナビを無視してHOUSE 11へ配達する",
             "speaker":"住民","line":"届いた。次から住所に『雪山ではない方』って書いとく。"}
        ]
    },
    {
        "title":"第20話　ボブの在庫監査",
        "summary":"車21台に対して鍵が27本、書類が22台分ある。",
        "stages":[
            {"pos":Vector3(-13,0,18),"mode":"action","objective":"BOB'S USED CARSの在庫を数える",
             "speaker":"ボブ","line":"車21台、鍵27本、書類22台分。計算上は余裕がある。"},
            {"pos":Vector3(13,0,30),"mode":"action","objective":"ジムに余った鍵6本を見せる",
             "speaker":"ジム","line":"そのうち4本は俺のだ。残り2本は……未来の車じゃないか？"}
        ]
    },
    {
        "title":"第21話　洗車機が凍った",
        "summary":"洗車機のブラシが巨大な氷の彫刻になった。",
        "stages":[
            {"pos":Vector3(13,0,13),"mode":"action","objective":"GAS & COFFEEの凍結洗車機を確認する",
             "speaker":"店員","line":"昨日洗車したら車より先に洗車機が凍った。順番を間違えた。"},
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"PUBLIC WORKSへ解氷の相談に行く",
             "speaker":"作業員","line":"春まで待てば無料で直る。町の修理予算はだいたいこれだ。"}
        ]
    },
    {
        "title":"第22話　128日連続の雪予報",
        "summary":"ラジオ局が『明日は雪』を128日連続で当てている。",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"役場で驚異的な天気予報の秘密を聞く",
             "speaker":"町長","line":"毎日『雪』って言えばだいたい当たる。統計は役に立つ。"},
            {"pos":Vector3(13,0,13),"mode":"action","objective":"ラジオ用の新しい予報原稿を受け取る",
             "speaker":"店員","line":"明日は雪。外れたら『局地的に外れた』でいこう。"}
        ]
    },
    {
        "title":"第23話　肉温計が公式気温",
        "summary":"町の温度計が壊れ、BBQの肉温計が公共インフラになる。",
        "stages":[
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"BBQの肉温計を借りる",
             "speaker":"店員","line":"先端に肉が付いてるけど精度には関係ない。たぶん。"},
            {"pos":Vector3(14,0,-4),"mode":"timed","duration":36.0,"objective":"温度が変わる前に町役場へ届ける",
             "speaker":"町長","line":"外気温68℃。よし、この温度計は肉を測ってるな。"},
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"肉温計をBBQへ返却する",
             "speaker":"店員","line":"助かった。町の気温は分からないけど肉は無事だ。"}
        ]
    }
]

func _ready() -> void:
    rng.randomize()
    set_process(false)

func attach_vehicle(v: PineVehicle) -> void:
    vehicle = v

func get_episode_count() -> int:
    return episodes.size()

func get_episode_title(index: int) -> String:
    if index < 0 or index >= episodes.size():
        return ""
    return str(episodes[index].get("title",""))

func get_episode_summary(index: int) -> String:
    if index < 0 or index >= episodes.size():
        return ""
    return str(episodes[index].get("summary",""))

func start_campaign(start_episode: int = 0) -> void:
    started = true
    episode_index = clampi(start_episode, 0, episodes.size() - 1)
    stage_index = 0
    horn_count = 0
    stage_timer = -1.0
    _last_timer_second = -1
    set_process(true)
    _announce_stage(true)

func stop_campaign() -> void:
    started = false
    stage_timer = -1.0
    _last_timer_second = -1
    set_process(false)
    marker_changed.emit(Vector3.ZERO, false)

func _process(delta: float) -> void:
    if not started or vehicle == null:
        return
    var stage := _stage()
    if stage.is_empty():
        return
    var pos := get_current_target_position()
    marker_changed.emit(pos, true)

    var mode: String = str(stage.get("mode","action"))
    if mode == "drive":
        if _near_target():
            _complete_stage()
        return

    if mode == "timed":
        stage_timer -= delta
        var shown_second := maxi(0, int(ceil(stage_timer)))
        if shown_second != _last_timer_second:
            _last_timer_second = shown_second
            objective_changed.emit(_episode()["title"],
                stage["objective"] + "　残り %d秒" % shown_second)

        if _near_target():
            _complete_stage()
            return

        if stage_timer <= 0.0:
            stage_timer = float(stage.get("duration",30.0))
            _last_timer_second = -1
            dialogue_requested.emit("無線","時間切れ。Pine Creek式では『もう一回』が正式な手順だ。")
            objective_changed.emit(_episode()["title"],
                stage["objective"] + "　残り %d秒" % int(ceil(stage_timer)))

func on_action() -> void:
    if not _can_interact():
        return
    var stage := _stage()
    if stage.get("mode","action") == "action":
        _complete_stage()

func on_horn() -> void:
    if not started:
        return
    var stage := _stage()
    if stage.is_empty() or stage.get("mode","action") != "horn":
        dialogue_requested.emit("警笛","BEEP!　雪だけが少し驚いた。")
        return
    if not _near_target():
        dialogue_requested.emit("警笛","BEEP!　ケビンにはたぶん聞こえてない。")
        return
    horn_count += 1
    var required: int = int(stage.get("required",1))
    var feedback_speaker := str(stage.get("horn_feedback_speaker","ケビン"))
    if horn_count < required:
        objective_changed.emit(_episode()["title"], stage["objective"] + "　(%d/%d)" % [horn_count,required])
        dialogue_requested.emit(feedback_speaker,str(stage.get("horn_feedback_line","ゴボゴボ……。遭遇判定は続行。")))
    else:
        if stage.has("horn_success_line"):
            dialogue_requested.emit(feedback_speaker,str(stage["horn_success_line"]))
        _complete_stage()

func _can_interact() -> bool:
    if not started:
        return false
    if not _near_target():
        dialogue_requested.emit("無線","目的地の近くまで行ってからアクション。")
        return false
    return true

func get_current_target_position() -> Vector3:
    var stage := _stage()
    if stage.is_empty():
        return Vector3.ZERO
    var p: Vector3 = stage["pos"]
    p.y = 0.085
    return p

func _near_target() -> bool:
    if vehicle == null:
        return false
    var stage := _stage()
    if stage.is_empty():
        return false
    var p := get_current_target_position()
    var delta := Vector2(vehicle.global_position.x - p.x, vehicle.global_position.z - p.z)
    return delta.length() <= float(stage.get("radius",10.0))

func _complete_stage() -> void:
    var stage := _stage()
    dialogue_requested.emit(stage.get("speaker","Pine Creek"),stage.get("line",""))
    stage_index += 1
    horn_count = 0
    stage_timer = -1.0
    _last_timer_second = -1

    var stages: Array = _episode()["stages"]
    if stage_index >= stages.size():
        episode_index += 1
        stage_index = 0
        if episode_index >= episodes.size():
            completed_cycles += 1
            episode_index = rng.randi_range(2,episodes.size()-1)
            dialogue_requested.emit("無線","町民認定。なお事件は終わらない。次の連絡が来た。")
        else:
            dialogue_requested.emit("無線","次の町内案件が入った。")
    _announce_stage(false)
func _announce_stage(first: bool) -> void:
    var ep := _episode()
    var stage := _stage()
    var objective_text: String = stage["objective"]

    if stage.get("mode","action") == "timed":
        stage_timer = float(stage.get("duration",30.0))
        _last_timer_second = int(ceil(stage_timer))
        objective_text += "　残り %d秒" % _last_timer_second
        dialogue_requested.emit("無線","時間制限あり。道路状況についての苦情は春に受け付ける。")
    else:
        stage_timer = -1.0
        _last_timer_second = -1

    objective_changed.emit(ep["title"], objective_text)
    marker_changed.emit(get_current_target_position(),true)
    if first:
        dialogue_requested.emit("無線","Pine Creekへようこそ。道路が白いなら、たぶんそこが道路だ。")

func _episode() -> Dictionary:
    return episodes[episode_index]

func _stage() -> Dictionary:
    if not started:
        return {}
    var stages: Array = _episode()["stages"]
    if stage_index < 0 or stage_index >= stages.size():
        return {}
    return stages[stage_index]
