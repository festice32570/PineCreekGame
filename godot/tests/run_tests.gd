extends SceneTree

var failures := 0

func check(cond: bool, message: String) -> void:
    if not cond:
        failures += 1
        push_error("FAIL: " + message)
    else:
        print("PASS: " + message)

func _init() -> void:
    call_deferred("_run")

func _make_ground(parent: Node3D) -> void:
    var body := StaticBody3D.new()
    body.position = Vector3(0,-0.25,0)
    body.set_meta("grip",0.9)
    var cs := CollisionShape3D.new()
    var shape := BoxShape3D.new()
    shape.size = Vector3(30,0.5,80)
    cs.shape = shape
    body.add_child(cs)
    parent.add_child(body)

func _run() -> void:
    # Multi-touch regression.
    var ui := PineTouchControls.new()
    root.add_child(ui)
    await process_frame
    ui.debug_touch_press(11,"throttle")
    ui.debug_touch_press(23,"left")
    check(ui.throttle == 1.0, "finger 11 holds throttle")
    check(ui.steer == -1.0, "finger 23 holds left while throttle remains active")
    ui.debug_touch_release(23)
    check(ui.throttle == 1.0 and ui.steer == 0.0, "releasing steering does not release throttle")
    ui.debug_touch_press(31,"right")
    check(ui.throttle == 1.0 and ui.steer == 1.0, "throttle + right multi-touch")
    ui.debug_touch_release(11)
    ui.debug_touch_release(31)
    ui.queue_free()

    # Vehicle body-direction regression.
    var world := Node3D.new()
    root.add_child(world)
    _make_ground(world)
    var car := PineVehicle.new()
    car.position = Vector3(0,0.62,0)
    world.add_child(car)
    await physics_frame
    await physics_frame

    var start := car.global_position
    car.set_controls(0.0,1.0,0.0,0.0)
    for i in range(180):
        await physics_frame
    var forward_delta := car.global_position - start
    check(forward_delta.z > 0.8, "accelerator moves along vehicle forward axis")
    check(absf(forward_delta.x) < maxf(0.8,absf(forward_delta.z)*0.35), "straight acceleration does not strafe sideways")

    # Deep snow must reduce longitudinal traction, not only lateral grip.
    var ground := world.get_child(0)
    ground.set_meta("grip",0.34)
    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(0,0.62,0))
    car.linear_velocity = Vector3.ZERO
    car.angular_velocity = Vector3.ZERO
    car.steering_state = 0.0
    car.freeze = false
    await physics_frame
    start = car.global_position
    car.set_controls(0.0,1.0,0.0,0.0)
    for i in range(180):
        await physics_frame
    var snow_delta := car.global_position - start
    check(snow_delta.z < forward_delta.z * 0.86,
        "deep snow reduces acceleration through tyre traction limit")
    ground.set_meta("grip",0.9)

    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(0,0.62,0))
    car.linear_velocity = Vector3.ZERO
    car.angular_velocity = Vector3.ZERO
    car.freeze = false
    await physics_frame
    start = car.global_position
    car.set_controls(0.0,0.0,0.0,1.0)
    for i in range(180):
        await physics_frame
    var reverse_delta := car.global_position - start
    check(reverse_delta.z < -0.35, "reverse moves opposite the vehicle forward axis")

    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(0,0.62,0))
    car.linear_velocity = Vector3.ZERO
    car.angular_velocity = Vector3.ZERO
    car.steering_state = 0.0
    car.freeze = false
    await physics_frame
    car.set_controls(-1.0,1.0,0.0,0.0)
    for i in range(10):
        await physics_frame
    check(absf(car.steering_state) > 0.05 and absf(car.steering_state) < 0.40,
        "digital steering ramps in instead of snapping to full lock")
    for i in range(170):
        await physics_frame
    var left_x := car.global_position.x
    check(left_x > 0.08, "left steering curves vehicle to its driver-left (+X with +Z forward)")

    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(0,0.62,0))
    car.linear_velocity = Vector3.ZERO
    car.angular_velocity = Vector3.ZERO
    car.freeze = false
    await physics_frame
    car.set_controls(1.0,1.0,0.0,0.0)
    for i in range(180):
        await physics_frame
    var right_x := car.global_position.x
    check(right_x < -0.08, "right steering curves vehicle to its driver-right (-X with +Z forward)")
    check(absf(absf(left_x) - absf(right_x)) < maxf(1.2, absf(left_x)*0.5), "left/right steering response stays reasonably symmetric")

    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(0,0.62,0))
    car.linear_velocity = Vector3.ZERO
    car.angular_velocity = Vector3.ZERO
    car.steering_state = 0.0
    car.freeze = false
    await physics_frame
    car.set_controls(-1.0,0.0,0.0,1.0)
    for i in range(220):
        await physics_frame
    check(car.global_position.x < -0.03, "left wheel angle produces the expected opposite yaw/path while reversing")

    # Timed-story regression: timeout restarts, arrival advances.
    var story := PineStoryDirector.new()
    story.attach_vehicle(car)
    world.add_child(story)
    story.started = true
    story.episode_index = 8
    story.stage_index = 1
    story.stage_timer = 0.01
    story.set_process(false)
    var timed_target := story.get_current_target_position()
    check(absf(timed_target.x - 6.25) < 0.01 and absf(timed_target.z - 49.0) < 0.01,
        "building mission target is converted to a roadside parking bay")
    car.freeze = true
    car.global_position = Vector3.ZERO
    story._process(0.05)
    check(story.stage_index == 1 and story.stage_timer > 40.0,
        "timed mission restarts after timeout")

    car.global_position = Vector3(6.25,0.62,49)
    story.stage_timer = 20.0
    story._process(0.01)
    check(story.episode_index == 9 and story.stage_index == 0,
        "reaching timed destination advances to next episode")
    check(story.episodes.size() >= 24, "campaign contains at least 24 crazy town episodes")

    story.start_campaign(0)
    var action_target := story.get_current_target_position()
    car.global_position = Vector3(action_target.x,0.62,action_target.z)
    story.on_action()
    check(story.episode_index == 0 and story.stage_index == 1,
        "action inside the roadside parking bay advances the mission")

    story.start_campaign(12)
    check(story.episode_index == 12 and story.stage_index == 0, "story selector can start an arbitrary episode")
    check(story.get_episode_title(12).contains("ケビン"), "story selector exposes episode titles")
    var title_source := load("res://assets/audio/title_theme.wav") as AudioStreamWAV
    var bg_source := load("res://assets/audio/pine_creek_radio.wav") as AudioStreamWAV
    var engine_source := load("res://assets/audio/engine_idle.wav") as AudioStreamWAV
    var skid_source := load("res://assets/audio/snow_skid.wav") as AudioStreamWAV
    check(title_source != null, "hard-rock title theme WAV imports")
    check(bg_source != null, "original driving BGM WAV imports")
    check(engine_source != null, "engine loop WAV imports")
    check(skid_source != null, "snow skid WAV imports")
    check(title_source.format == AudioStreamWAV.FORMAT_16_BITS and title_source.mix_rate == 48000,
        "title theme imports as local 48 kHz PCM instead of QOA")
    check(absf(title_source.get_length() - 25.6) < 0.05,
        "title loop is the 16-bar section derived from the full theme")
    check(bg_source.format == AudioStreamWAV.FORMAT_16_BITS and bg_source.mix_rate == 48000,
        "driving BGM imports as local 48 kHz PCM instead of QOA")
    check(engine_source.format == AudioStreamWAV.FORMAT_16_BITS and engine_source.mix_rate == 48000,
        "engine loop imports as local 48 kHz PCM instead of QOA")
    var snow_source := load("res://assets/audio/snow_roll.wav") as AudioStreamWAV
    check(snow_source != null and snow_source.format == AudioStreamWAV.FORMAT_16_BITS and snow_source.mix_rate == 48000,
        "packed-snow loop imports as local 48 kHz PCM")
    check(absf(snow_source.get_length() - 6.0) < 0.05,
        "packed-snow loop uses the six-second granular tyre texture")

    # Runtime audio regression: imported streams are not enough; looping audio
    # must have a real non-zero loop end and must keep advancing after startup.
    var game_audio := PineGameAudio.new()
    root.add_child(game_audio)
    await process_frame
    await process_frame
    var master_bus := AudioServer.get_bus_index("Master")
    check(master_bus >= 0 and not AudioServer.is_bus_mute(master_bus),
        "Master audio bus is available and unmuted")
    check(game_audio.current_music_path.ends_with("title_theme.wav"),
        "title context starts the hard-rock theme")
    var bg_loop := game_audio.music.stream as AudioStreamWAV
    check(bg_loop != null and bg_loop.loop_mode == AudioStreamWAV.LOOP_FORWARD and bg_loop.loop_end > 0,
        "title music has a non-zero forward loop range")
    await create_timer(0.25).timeout
    check(game_audio.music.playing and game_audio.music.get_playback_position() > 0.05,
        "title theme keeps playing instead of restarting as clicks")
    game_audio.play_drive_music()
    await process_frame
    check(game_audio.current_music_path.ends_with("pine_creek_radio.wav") and game_audio.music.playing,
        "gameplay context switches from title metal to driving radio")
    game_audio.click()
    check(game_audio.ui != null and game_audio.ui.playing,
        "UI sound effect enters playing state")

    car.linear_velocity = Vector3.ZERO
    car.angular_velocity = Vector3.ZERO
    car.set_controls(0.0,0.0,0.0,0.0)
    var runtime_vehicle_audio := PineVehicleAudio.new()
    runtime_vehicle_audio.attach_vehicle(car)
    root.add_child(runtime_vehicle_audio)
    await process_frame
    check(not runtime_vehicle_audio.engine.playing and not runtime_vehicle_audio.driving_enabled,
        "vehicle audio stays silent before driving mode starts")
    runtime_vehicle_audio.set_driving_enabled(true)
    var engine_loop := runtime_vehicle_audio.engine.stream as AudioStreamWAV
    check(engine_loop != null and engine_loop.loop_end > 0,
        "engine audio has a non-zero loop range")
    runtime_vehicle_audio._process(0.016)
    check(runtime_vehicle_audio.snow.volume_db <= -79.0 and runtime_vehicle_audio.skid.volume_db <= -79.0,
        "stationary vehicle gates road and skid hiss")
    car.linear_velocity = Vector3(0.0,0.0,8.0)
    runtime_vehicle_audio._process(0.016)
    check(runtime_vehicle_audio.snow.volume_db > -25.0 and runtime_vehicle_audio.snow.playing,
        "packed-snow tyre texture becomes audible at normal driving speed")
    car.linear_velocity = Vector3.ZERO
    await create_timer(0.25).timeout
    check(runtime_vehicle_audio.engine.playing and runtime_vehicle_audio.engine.get_playback_position() > 0.05,
        "engine loop keeps advancing instead of restarting as clicks")
    runtime_vehicle_audio.queue_free()
    game_audio.queue_free()

    world.queue_free()
    await process_frame

    if failures == 0:
        print("GODOT_TESTS=PASS")
        quit(0)
    else:
        print("GODOT_TESTS=FAIL count=",failures)
        quit(1)
