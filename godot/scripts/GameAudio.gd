class_name PineGameAudio
extends Node

var music: AudioStreamPlayer
var ui: AudioStreamPlayer
var cue: AudioStreamPlayer

func _ready() -> void:
    process_mode = Node.PROCESS_MODE_ALWAYS
    music = AudioStreamPlayer.new()
    music.name = "PineCreekRadio"
    music.stream = _looping_wav("res://assets/audio/pine_creek_radio.wav")
    music.volume_db = -15.0
    add_child(music)
    music.play()

    ui = AudioStreamPlayer.new()
    ui.name = "UiClick"
    ui.stream = load("res://assets/audio/ui_click.wav")
    ui.volume_db = -7.0
    add_child(ui)
    cue = AudioStreamPlayer.new()
    cue.name = "PauseCue"
    cue.stream = load("res://assets/audio/pause_cue.wav")
    cue.volume_db = -5.0
    add_child(cue)

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
        music.volume_db = -24.0 if ducked else -15.0
