extends SceneTree

var failures := 0

func _init() -> void:
    call_deferred("_run")

func _check(cond: bool, message: String) -> void:
    if cond:
        print("PASS: ", message)
    else:
        failures += 1
        push_error("FAIL: " + message)

func _capture(path: String) -> void:
    await process_frame
    await process_frame
    var image := root.get_viewport().get_texture().get_image()
    var err := image.save_png(path)
    _check(err == OK, "capture " + path)
    if err == OK:
        print("CAPTURE=", path)

func _reset(car: PineVehicle, chase: PineChaseCamera) -> void:
    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(0,0.62,22))
    car.linear_velocity = Vector3.ZERO
    car.angular_velocity = Vector3.ZERO
    car.freeze = false
    car.sleeping = false
    car.set_controls(0.0,0.0,1.0,0.0)
    chase.follow_yaw = 0.0
    chase.orbit_yaw = 0.0
    chase.global_position = car.global_position + Vector3(0,3,-7)
    for i in range(12):
        await physics_frame
    car.set_controls(0.0,0.0,0.0,0.0)

func _run() -> void:
    var packed := load("res://scenes/Main.tscn") as PackedScene
    var main := packed.instantiate()
    root.add_child(main)
    await process_frame
    await physics_frame

    var car: PineVehicle = main.vehicle
    var chase: PineChaseCamera = main.chase

    _check(main.get_node_or_null("RuralSnowGround") != null,
        "rural graybox builds the 4.5 km snow field")
    _check(main.get_node_or_null("RuralTreeCrowns") is MultiMeshInstance3D,
        "rural forest uses MultiMesh instead of hundreds of tree nodes")
    _check(PineWorldLayout.total_road_length_m() > 15000.0,
        "rural graybox exposes more than 15 km of modular roads")
    _check(main.get_node_or_null("BOB") != null,
        "Bob prototype is placed at the used-car lot")

    # Story selection is a real menu: gameplay input must be completely disabled
    # so touch drags belong to ScrollContainer instead of steering/camera.
    main._show_story_select()
    _check(main.story_select_layer.visible, "story selection opens from title")
    _check(not main.touch.gameplay_enabled and not main.touch.is_processing_input(),
        "story selection disables driving touch input")
    _check(main.story_scroll != null and main.story_scroll.scroll_deadzone <= 16,
        "story list starts scrolling with a short touch drag")
    _check(not main.vehicle_audio.engine.playing and not main.vehicle_audio.snow.playing,
        "title and story-selection menus do not leak vehicle audio")
    await _capture("/home/festice/ChatGPT-dev/PineCreekGame/godot/build/qa-story-select.png")
    main._story_select_back()
    _check(main.title_layer.visible, "story selection returns to title")
    _check(main.game_audio.current_music_path.ends_with("title_theme.wav"),
        "title screen uses the hard-rock theme")

    # Free drive is a first-class mode: no campaign, no marker, no Action button.
    # Spawn is no longer simulated as a fall/settle at all: it is placed at the
    # road-rest height and remains frozen until the first driving input.
    main._start_free_drive()
    await process_frame
    await process_frame
    _check(main.is_free_drive() and car.visible, "free drive starts from a pre-positioned road pose")
    _check(main._spawn_lock and car.freeze, "free-drive spawn stays physics-locked before first input")
    _check(absf(car.global_position.y - main.ROAD_RESCUE_Y) < 0.015,
        "free-drive spawn is placed directly at the road-rest height")
    _check(not main.story.started and not main.mission_marker.visible,
        "free drive has no campaign or mission marker")
    _check(main.touch.gameplay_enabled and not (main.touch._buttons["action"] as Button).visible,
        "free drive enables driving controls without story Action")
    _check(main.vehicle_audio.driving_enabled and main.vehicle_audio.engine.playing,
        "free drive enables vehicle audio only after gameplay begins")
    var free_spawn_y := car.global_position.y
    for i in range(30):
        await physics_frame
    _check(absf(car.global_position.y - free_spawn_y) < 0.001,
        "locked free-drive spawn cannot fall while the player is idle")
    main.touch.debug_touch_press(71, "throttle")
    await physics_frame
    main.touch.debug_touch_release(71)
    _check(not main._spawn_lock and not car.freeze, "first driving input releases the spawn lock")
    for i in range(12):
        await physics_frame
    _check(absf(car.global_position.y - free_spawn_y) < 0.08,
        "releasing physics at road-rest height does not create a visible drop")
    main._pause_to_title()
    _check(main.title_layer.visible and not main.game_started and not main.touch.gameplay_enabled,
        "leaving free drive returns to a non-driving title menu")
    _check(not main.vehicle_audio.driving_enabled and not main.vehicle_audio.engine.playing,
        "returning to title stops engine and tyre audio")

    # Story start uses the same direct road placement. Calling start twice must
    # not launch competing async start sequences.
    main._start_game()
    main._start_game()
    _check(main._starting_game, "duplicate start is guarded by one start sequence")
    await process_frame
    await process_frame
    _check(main.game_started and car.visible, "story vehicle appears at the prepared road pose")
    _check(main._spawn_lock and car.freeze, "story spawn remains locked until driving input")
    _check(absf(car.global_position.y - main.ROAD_RESCUE_Y) < 0.015,
        "story spawn is placed directly at road-rest height")
    var story_spawn_y := car.global_position.y
    for i in range(30):
        await physics_frame
    _check(absf(car.global_position.y - story_spawn_y) < 0.001,
        "idle story spawn cannot fall because physics is still locked")
    main.touch.debug_touch_press(72, "throttle")
    await physics_frame
    main.touch.debug_touch_release(72)
    _check(not main._spawn_lock and not car.freeze, "story spawn unlocks on first driving input")
    for i in range(12):
        await physics_frame
    _check(absf(car.global_position.y - story_spawn_y) < 0.08,
        "first story movement starts without a vertical drop")
    _check(main.game_audio.current_music_path.ends_with("pine_creek_radio.wav"),
        "gameplay switches to the driving radio track")
    _check(main.story.started and (main.touch._buttons["action"] as Button).visible,
        "story mode starts campaign and restores Action control")
    _check(main.game_audio != null and main.game_audio.music != null and main.game_audio.music.playing,
        "background music playback is active after startup")
    _check(main.pause_button.visible, "pause button appears during gameplay")
    main._open_pause()
    _check(main.pause_layer.visible and paused and not main.touch.gameplay_enabled,
        "pause menu freezes gameplay and disables driving touch")
    main._resume_game()
    _check(not paused and not main.pause_layer.visible and main.touch.gameplay_enabled,
        "resume closes pause menu and restores driving touch")

    var out := "/home/festice/ChatGPT-dev/PineCreekGame/godot/build"

    # Season 1 destinations are broad event zones rather than precision parking.
    var first_target: Vector3 = main.story.get_current_target_position()
    _check(first_target.distance_to(PineWorldLayout.BOB_EVENT) < 0.2,
        "first Season 1 mission begins beside Bob's Used Cars")
    _check(main.mission_marker.get_node_or_null("EventZoneFill") != null and
        main.mission_marker.get_node_or_null("EventZonePointer") != null,
        "mission marker contains a broad event area and pointer")
    main._teleport_vehicle(PineWorldLayout.start_transform())
    for i in range(12):
        await physics_frame
    await _capture(out + "/qa-rural-bob-event-zone.png")
    main.story.on_action()
    _check(main.story.stage_index == 1 and main.dialogue_portrait.visible and
        main.dialogue_portrait.texture != null,
        "Episode 1 starts Bob dialogue with the Blender-rendered portrait")
    await _capture(out + "/qa-season1-bob-dialogue.png")

    # Prove the pickup remains numerically/physically usable far from world origin.
    var far_road := PineWorldLayout.nearest_road_transform(PineWorldLayout.DEEP_SNOW_SITE)
    main._teleport_vehicle(far_road)
    main.set_physics_process(false)
    var far_start := car.global_position
    car.set_controls(0.0,1.0,0.0,0.0)
    for i in range(150):
        await physics_frame
    var far_distance := Vector2(car.global_position.x-far_start.x,
        car.global_position.z-far_start.z).length()
    _check(far_distance > 2.0 and car.get_speed_kmh() > 10.0 and
        car.global_position.y > 0.30 and car.global_position.y < 1.2,
        "vehicle physics stays stable on the forest road more than 1 km from origin")
    await _capture(out + "/qa-rural-forest-road.png")
    car.set_controls(0.0,0.0,1.0,0.0)
    main.set_physics_process(true)

    # The touch reset signal must always rescue to the nearest road centerline.
    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(20,0.62,20))
    car.freeze = false
    main.touch.reset_requested.emit()
    for i in range(4):
        await physics_frame
    _check(absf(car.global_position.x) < 0.12 and absf(car.global_position.z - 20.0) < 0.25,
        "reset signal rescues an off-road vehicle to the main road")

    # Exercise the same screen-hit path used by an Android touch on the visible
    # reset button, not only the signal directly.
    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(34,0.62,35))
    car.freeze = false
    var reset_button: Button = main.touch._buttons["reset"]
    var reset_point := reset_button.position + reset_button.size * 0.5
    main.touch._handle_touch(77, reset_point, true)
    main.touch._handle_touch(77, reset_point, false)
    for i in range(4):
        await physics_frame
    _check(absf(car.global_position.x) < 0.12 and absf(car.global_position.z - 35.0) < 0.35,
        "actual touch on reset returns a vehicle from snow to the road")

    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(200,0.62,30))
    car.freeze = false
    main._reset_vehicle_to_road()
    for i in range(4):
        await physics_frame
    _check(absf(car.global_position.z) < 0.30 and absf(car.global_position.x - 200.0) < 0.35,
        "rescue chooses the long town crossroad when it is nearer")

    car.freeze = true
    car.global_transform = Transform3D(Basis.IDENTITY, Vector3(8,-5.0,10))
    car.freeze = false
    var expected_fall_rescue := PineWorldLayout.nearest_road_transform(car.global_position)
    main._physics_process(0.016)
    for i in range(4):
        await physics_frame
    var fall_error := Vector2(car.global_position.x-expected_fall_rescue.origin.x,
        car.global_position.z-expected_fall_rescue.origin.z).length()
    _check(car.global_position.y > 0.35 and fall_error < 0.40,
        "falling below the world automatically rescues to the nearest modular road")

    main.set_physics_process(false)

    # Straight acceleration.
    car.set_controls(0.0,1.0,0.0,0.0)
    for i in range(240):
        await physics_frame
    var straight_speed := car.get_speed_kmh()
    print("STRAIGHT pos=",car.global_position," speed=",straight_speed," yaw=",rad_to_deg(car.rotation.y))
    _check(straight_speed > 10.0, "vehicle actually accelerates after title start")
    _check(absf(rad_to_deg(car.rotation.y)) < 3.0, "straight input keeps heading straight")
    await _capture(out + "/qa-straight.png")

    # Left turn while still on throttle. Camera should lag instead of snapping.
    car.set_controls(-0.72,1.0,0.0,0.0)
    for i in range(180):
        await physics_frame
    var car_yaw := rad_to_deg(car.rotation.y)
    var camera_yaw := rad_to_deg(chase.follow_yaw)
    print("LEFT pos=",car.global_position," speed=",car.get_speed_kmh()," car_yaw=",car_yaw," cam_yaw=",camera_yaw)
    _check(car.global_position.x > 0.4, "left steering moves vehicle to driver-left (+X)")
    _check(car_yaw > 5.0, "left steering rotates vehicle toward driver-left")
    _check(absf(car_yaw - camera_yaw) > 2.0, "camera does not rotate instantly with vehicle")
    await _capture(out + "/qa-left.png")

    # Reverse from rest.
    await _reset(car,chase)
    car.set_controls(0.0,0.0,0.0,1.0)
    for i in range(180):
        await physics_frame
    var reverse_speed := car.get_forward_speed_kmh()
    print("REVERSE pos=",car.global_position," speed=",car.get_speed_kmh()," forward_speed=",reverse_speed)
    _check(reverse_speed < -3.0, "reverse produces negative vehicle-forward speed")
    _check(car.global_position.z < 21.7, "reverse moves vehicle backward in world from identity heading")
    await _capture(out + "/qa-reverse.png")

    # Reverse with left lock must also follow the driver's left input.
    await _reset(car,chase)
    car.set_controls(-0.72,0.0,0.0,1.0)
    for i in range(220):
        await physics_frame
    print("REVERSE_LEFT pos=",car.global_position," yaw=",rad_to_deg(car.rotation.y))
    _check(car.global_position.x < -0.03 and rad_to_deg(car.rotation.y) < -4.0, "left wheel angle gives correct reverse yaw/path")
    await _capture(out + "/qa-reverse-left.png")

    main.queue_free()
    await process_frame

    if failures == 0:
        print("VISUAL_QA=PASS")
        quit(0)
    else:
        print("VISUAL_QA=FAIL count=", failures)
        quit(1)
