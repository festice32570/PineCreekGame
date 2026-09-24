class_name PineVehicleAudio
extends Node

var vehicle: PineVehicle
var engine: AudioStreamPlayer
var snow: AudioStreamPlayer
var skid: AudioStreamPlayer
var horn: AudioStreamPlayer
var driving_enabled := false

func _ready() -> void:
    engine = _make_loop_player("Engine", "res://assets/audio/engine_idle.wav", -8.0)
    snow = _make_loop_player("SnowRoad", "res://assets/audio/snow_roll.wav", -40.0)
    skid = _make_loop_player("SnowSkid", "res://assets/audio/snow_skid.wav", -45.0)

    horn = AudioStreamPlayer.new()
    horn.name = "TruckHorn"
    horn.stream = load("res://assets/audio/truck_horn.wav")
    horn.bus = "Master"
    horn.volume_db = -5.0
    add_child(horn)

func _make_loop_player(name_text: String, path: String, volume: float) -> AudioStreamPlayer:
    var p := AudioStreamPlayer.new()
    p.name = name_text
    var stream := load(path)
    if stream is AudioStreamWAV:
        var wav := (stream as AudioStreamWAV).duplicate() as AudioStreamWAV
        wav.loop_mode = AudioStreamWAV.LOOP_FORWARD
        wav.loop_begin = 0
        wav.loop_end = maxi(1, int(round(wav.get_length() * float(wav.mix_rate))))
        p.stream = wav
    else:
        p.stream = stream
    p.bus = "Master"
    p.volume_db = volume
    add_child(p)
    return p

func set_driving_enabled(enabled: bool) -> void:
    driving_enabled = enabled
    set_process(enabled)
    if enabled:
        for p in [engine, snow, skid]:
            if p != null and p.stream != null and not p.playing:
                p.play()
    else:
        for p in [engine, snow, skid, horn]:
            if p != null:
                p.stop()

func attach_vehicle(v: PineVehicle) -> void:
    vehicle = v

func trigger_horn() -> void:
    if driving_enabled and horn != null:
        horn.play()

func _process(delta: float) -> void:
    if not driving_enabled or vehicle == null:
        return

    var speed := vehicle.linear_velocity.length()
    var throttle := maxf(vehicle.throttle_input, vehicle.reverse_input * 0.78)
    var forward_speed := absf(vehicle.get_forward_speed_kmh()) / 3.6
    var rpm_norm := clampf((forward_speed * 112.0 + throttle * 2100.0) / 4300.0, 0.0, 1.0)

    engine.pitch_scale = lerpf(engine.pitch_scale, lerpf(0.92, 1.82, rpm_norm), 1.0 - exp(-5.0 * delta))
    engine.volume_db = lerpf(-11.0, -3.5, clampf(0.18 + throttle * 0.72 + speed / 70.0, 0.0, 1.0))

    var road_amount := clampf(speed / 20.0, 0.0, 1.0)
    snow.pitch_scale = lerpf(0.76, 1.18, road_amount)
    if speed < 0.45:
        snow.volume_db = -80.0
    else:
        # Snow is a tyre texture, not a constant hiss bed. Bring the new
        # granular crunch loop in early enough to be audible under the V8.
        var snow_mix := sqrt(clampf((speed - 0.45) / 18.0, 0.0, 1.0))
        snow.volume_db = lerpf(-30.0, -12.0, snow_mix)

    var lateral := absf(vehicle.linear_velocity.dot(vehicle.global_transform.basis.x.normalized()))
    var scrub := clampf((lateral - 0.75) / 4.5, 0.0, 1.0) * clampf(speed / 5.0, 0.0, 1.0)
    skid.pitch_scale = lerpf(0.82, 1.18, clampf(speed / 18.0, 0.0, 1.0))
    skid.volume_db = -80.0 if scrub < 0.04 else lerpf(-32.0, -11.0, scrub)

func shutdown() -> void:
    set_driving_enabled(false)

func _exit_tree() -> void:
    shutdown()
