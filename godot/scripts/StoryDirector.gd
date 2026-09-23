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
        "title":"プロローグ　BBQ小盛り非常事態",
        "stages":[
            {"pos":Vector3(-13,0,18),"mode":"action","objective":"BOB'S USED CARSで『小盛り』の意味を聞く",
             "speaker":"ボブ","line":"小盛り？　この町でその単語を使うと保安官が来るぞ。たぶん。"},
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"PINE CREEK FAMILY BBQへ向かう",
             "speaker":"店員","line":"これが小盛り。皿から落ちてる分はカロリーに入らない。"}
        ]
    },
    {
        "title":"第1話　町長と除雪車",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"町役場で町長の話を聞く",
             "speaker":"町長","line":"除雪車が埋まった。除雪車を救うために除雪が必要だ。"},
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"公共事業ヤードの除雪車を確認する",
             "speaker":"作業員","line":"大丈夫だ。春になれば自然に出てくる。たぶん四月だ。"}
        ]
    },
    {
        "title":"第2話　ジムの18台目",
        "stages":[
            {"pos":Vector3(13,0,30),"mode":"action","objective":"ジムのガレージへ行く",
             "speaker":"ジム","line":"17台目？　昨日までの話だ。今朝18台目になった。"},
            {"pos":Vector3(-13,0,18),"mode":"action","objective":"ボブに18台目の由来を確認する",
             "speaker":"ボブ","line":"あれは売ってない。置いてただけだ。値札が付いてただけで。"}
        ]
    },
    {
        "title":"第3話　役場のケビン",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"horn","required":3,"objective":"役場前で警笛を3回鳴らしてケビンを説得する",
             "speaker":"町長","line":"七面鳥のケビンが正面玄関を占拠した。交渉は警笛で頼む。"}
        ]
    },
    {
        "title":"第4話　郵便受けの冬季移住",
        "stages":[
            {"pos":Vector3(-12,0,49),"mode":"action","objective":"雪山に刺さった郵便受けを確認する",
             "speaker":"住民","line":"昨日まではうちの前にあった。今日は三軒先に住んでる。"},
            {"pos":Vector3(12,0,49),"mode":"action","objective":"郵便受けを本来の家へ届ける",
             "speaker":"住民","line":"ありがとう。明日また移住したら、もう本人の意思ってことにする。"}
        ]
    },
    {
        "title":"第5話　春の誤報",
        "stages":[
            {"pos":Vector3(13,0,13),"mode":"action","objective":"ガソリンスタンドの『+1℃』表示を調べる",
             "speaker":"店員","line":"温度計の下に排気管がある。春はまだ来てない。"},
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"中止になった春祭りのBBQを回収する",
             "speaker":"店員","line":"祭りは中止。でも肉は中止できない。食べれば解決だ。"}
        ]
    },
    {
        "title":"第6話　公共交通が発生",
        "stages":[
            {"pos":Vector3(-13,0,18),"mode":"action","objective":"ボブの『町営バス』を確認する",
             "speaker":"ボブ","line":"荷台に折りたたみ椅子を置いた。つまりバスだろ？"},
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"町役場へバス停の看板を届ける",
             "speaker":"町長","line":"公共交通ができた。運行日はボブが起きた日だ。"}
        ]
    },
    {
        "title":"第7話　雪だるま市議会",
        "stages":[
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"役場前の臨時市議会を確認する",
             "speaker":"町長","line":"議員が来ないから雪だるまで定足数を満たした。異議は溶けるまで受け付けない。"},
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"議会用の巨大コーヒーをBBQから運ぶ",
             "speaker":"店員","line":"12リットル。会議用なら普通だよ。"}
        ]
    },
    {
        "title":"第8話　凍った誕生日ケーキ",
        "stages":[
            {"pos":Vector3(-13,0,-21),"mode":"action","objective":"BBQで巨大な誕生日ケーキを受け取る",
             "speaker":"店員","line":"ケーキは凍ってる。落としてもたぶん床の方が負ける。"},
            {"pos":Vector3(12,0,49),"mode":"timed","duration":46.0,"objective":"ケーキが完全凍結する前にHOUSE 12へ届ける",
             "speaker":"住民","line":"間に合った！　ロウソクは刺さらないからドリルを持ってくる。"}
        ]
    },
    {
        "title":"第9話　非常事態レベル紫",
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
        "stages":[
            {"pos":Vector3(-12,0,49),"mode":"action","objective":"HOUSE 11からの『庭にシカ』通報を確認する",
             "speaker":"住民","line":"三時間動かない。根性のあるシカだと思った。"},
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"PUBLIC WORKSに正体を報告する",
             "speaker":"作業員","line":"それ芝生飾りだ。雪から半分出ると毎年一回は通報される。"}
        ]
    },
    {
        "title":"第11話　看板が旅に出た",
        "stages":[
            {"pos":Vector3(14,0,-37),"mode":"action","objective":"風で消えた町入口の看板について聞く",
             "speaker":"作業員","line":"看板が風に乗って旅に出た。本人の意思は確認してない。"},
            {"pos":Vector3(-12,0,49),"mode":"timed","duration":42.0,"objective":"HOUSE 11付近まで飛んだ町看板を回収する",
             "speaker":"住民","line":"うちの庭が今日から町の入口になったらしい。"},
            {"pos":Vector3(14,0,-4),"mode":"action","objective":"看板を町役場へ届ける",
             "speaker":"町長","line":"戻ったか。次に飛んだら住所変更届を書かせる。"}
        ]
    }
]

func _ready() -> void:
    rng.randomize()
    set_process(false)

func attach_vehicle(v: PineVehicle) -> void:
    vehicle = v

func start_campaign() -> void:
    if started:
        return
    started = true
    episode_index = 0
    stage_index = 0
    horn_count = 0
    stage_timer = -1.0
    _last_timer_second = -1
    set_process(true)
    _announce_stage(true)

func _process(delta: float) -> void:
    if not started or vehicle == null:
        return
    var stage := _stage()
    if stage.is_empty():
        return
    var pos: Vector3 = stage["pos"]
    marker_changed.emit(pos + Vector3.UP * 0.20, true)

    if stage.get("mode","action") == "timed":
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
    if horn_count < required:
        objective_changed.emit(_episode()["title"], stage["objective"] + "　(%d/%d)" % [horn_count,required])
        dialogue_requested.emit("ケビン","ゴボゴボ……。")
    else:
        dialogue_requested.emit("ケビン","ゴボッ！　……交渉成立らしい。")
        _complete_stage()

func _can_interact() -> bool:
    if not started:
        return false
    if not _near_target():
        dialogue_requested.emit("無線","目的地の近くまで行ってからアクション。")
        return false
    return true

func _near_target() -> bool:
    if vehicle == null:
        return false
    var stage := _stage()
    if stage.is_empty():
        return false
    var p: Vector3 = stage["pos"]
    var delta := Vector2(vehicle.global_position.x - p.x, vehicle.global_position.z - p.z)
    return delta.length() <= 5.5

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
            # The main campaign becomes an endless rotating town-incident mode.
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
    marker_changed.emit(Vector3(stage["pos"]) + Vector3.UP * 0.20,true)
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
