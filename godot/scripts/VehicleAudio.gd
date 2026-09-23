class_name PineVehicleAudio
extends Node

const MIX_RATE := 22050.0

var vehicle: PineVehicle
var player: AudioStreamPlayer
var playback: AudioStreamGeneratorPlayback
var engine_phase := 0.0
var second_phase := 0.0
var horn_phase_a := 0.0
var horn_phase_b := 0.0
var horn_time := 0.0
var filtered_rpm := 850.0

func _ready() -> void:
    var generator := AudioStreamGenerator.new()
    generator.mix_rate = MIX_RATE
    generator.buffer_length = 0.22

    player = AudioStreamPlayer.new()
    player.stream = generator
    player.volume_db = -5.0
    add_child(player)
    player.play()
    playback = player.get_stream_playback() as AudioStreamGeneratorPlayback
    set_process(true)

func attach_vehicle(v: PineVehicle) -> void:
    vehicle = v

func trigger_horn() -> void:
    horn_time = 0.34

func _process(delta: float) -> void:
    if vehicle == null or playback == null:
        return

    horn_time = maxf(0.0, horn_time - delta)

    var speed_mps := vehicle.linear_velocity.length()
    var throttle := maxf(vehicle.throttle_input, vehicle.reverse_input * 0.72)
    var rpm_target := 820.0 + speed_mps * 105.0 + throttle * 1850.0
    rpm_target = clampf(rpm_target, 760.0, 4300.0)
    filtered_rpm = lerpf(filtered_rpm, rpm_target, 1.0 - exp(-5.8 * delta))

    var frames := playback.get_frames_available()
    if frames <= 0:
        return

    var engine_hz := filtered_rpm / 60.0 * 2.0
    var engine_amp := 0.045 + throttle * 0.075 + clampf(speed_mps / 28.0,0.0,1.0) * 0.025

    for _i in range(frames):
        engine_phase = fmod(engine_phase + TAU * engine_hz / MIX_RATE, TAU)
        second_phase = fmod(second_phase + TAU * engine_hz * 0.5 / MIX_RATE, TAU)

        var combustion := sin(engine_phase) * 0.60
        combustion += sin(engine_phase * 2.0 + 0.36) * 0.22
        combustion += sin(second_phase) * 0.18
        var sample := combustion * engine_amp

        if horn_time > 0.0:
            horn_phase_a = fmod(horn_phase_a + TAU * 330.0 / MIX_RATE, TAU)
            horn_phase_b = fmod(horn_phase_b + TAU * 415.0 / MIX_RATE, TAU)
            sample += (sin(horn_phase_a) * 0.15 + sin(horn_phase_b) * 0.11)

        sample = clampf(sample,-0.72,0.72)
        playback.push_frame(Vector2(sample,sample))

func shutdown() -> void:
    set_process(false)
    if player != null:
        player.stop()
    playback = null
    if player != null:
        player.stream = null

func _exit_tree() -> void:
    shutdown()
