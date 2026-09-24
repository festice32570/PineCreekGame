class_name PineGameAudio
extends Node

var music: AudioStreamPlayer
var ui: AudioStreamPlayer
var cue: AudioStreamPlayer

func _ready() -> void:
    process_mode = Node.PROCESS_MODE_ALWAYS
    _ensure_master_output()
    music = AudioStreamPlayer.new()
    music.name = "PineCreekRadio"
    music.stream = _looping_wav("res://assets/audio/pine_creek_radio.wav")
    music.bus = "Master"
    music.volume_db = -8.0
    add_child(music)
    call_deferred("_ensure_music_playing")

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

func _ensure_master_output() -> void:
    var master := AudioServer.get_bus_index("Master")
    if master >= 0:
        AudioServer.set_bus_mute(master, false)
        AudioServer.set_bus_volume_db(master, 0.0)

func _ensure_music_playing() -> void:
    _ensure_master_output()
    if music != null and music.stream != null and not music.playing:
        music.play()

func _process(_delta: float) -> void:
    if music != null and music.stream != null and not music.playing:
        _ensure_music_playing()

func _looping_wav(path: String) -> AudioStream:
    var stream := load(path)
    if stream is AudioStreamWAV:
        var wav := (stream as AudioStreamWAV).duplicate() as AudioStreamWAV
        wav.loop_mode = AudioStreamWAV.LOOP_FORWARD
        return wav
    return stream

func click() -> void:
    if ui != null:
        ui.play()

func pause_cue() -> void:
    if cue != null:
        cue.play()

func set_ducked(ducked: bool) -> void:
    if music != null:
        music.volume_db = -18.0 if ducked else -8.0
