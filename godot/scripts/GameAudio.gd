class_name PineGameAudio
extends Node

var music: AudioStreamPlayer
var ui: AudioStreamPlayer
var cue: AudioStreamPlayer
var current_music_path := ""
var music_base_volume_db := -8.0
var ducked := false

func _ready() -> void:
    process_mode = Node.PROCESS_MODE_ALWAYS
    _ensure_master_output()

    music = AudioStreamPlayer.new()
    music.name = "Music"
    music.bus = "Master"
    add_child(music)

    ui = AudioStreamPlayer.new()
    ui.name = "UiClick"
    ui.stream = load("res://assets/audio/ui_click.wav")
    ui.bus = "Master"
    ui.volume_db = -4.0
    add_child(ui)

    cue = AudioStreamPlayer.new()
    cue.name = "PauseCue"
    cue.stream = load("res://assets/audio/pause_cue.wav")
    cue.bus = "Master"
    cue.volume_db = -4.0
    add_child(cue)

    call_deferred("play_title_theme")

func _ensure_master_output() -> void:
    var master := AudioServer.get_bus_index("Master")
    if master >= 0:
        AudioServer.set_bus_mute(master, false)
        AudioServer.set_bus_volume_db(master, 0.0)

func _looping_wav(path: String) -> AudioStream:
    var stream := load(path)
    if stream is AudioStreamWAV:
        var wav := (stream as AudioStreamWAV).duplicate() as AudioStreamWAV
        wav.loop_mode = AudioStreamWAV.LOOP_FORWARD
        wav.loop_begin = 0
        wav.loop_end = maxi(1, int(round(wav.get_length() * float(wav.mix_rate))))
        return wav
    return stream

func _switch_music(path: String, base_volume_db: float) -> void:
    _ensure_master_output()
    if music == null:
        return
    if current_music_path == path and music.playing:
        music_base_volume_db = base_volume_db
        _apply_music_volume()
        return
    var stream := _looping_wav(path)
    if stream == null:
        push_error("Music failed to load: " + path)
        return
    music.stop()
    music.stream = stream
    current_music_path = path
    music_base_volume_db = base_volume_db
    _apply_music_volume()
    music.play()

func _apply_music_volume() -> void:
    if music != null:
        music.volume_db = music_base_volume_db - (10.0 if ducked else 0.0)

func play_title_theme() -> void:
    _switch_music("res://assets/audio/title_theme.wav", -7.0)

func play_drive_music() -> void:
    _switch_music("res://assets/audio/pine_creek_radio.wav", -12.0)

func click() -> void:
    if ui != null:
        ui.play()

func pause_cue() -> void:
    if cue != null:
        cue.play()

func set_ducked(value: bool) -> void:
    ducked = value
    _apply_music_volume()
