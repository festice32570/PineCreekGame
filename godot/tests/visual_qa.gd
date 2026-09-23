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

    # Story selection must be reachable from the title and render cleanly.
    main._show_story_select()
    _check(main.story_select_layer.visible, "story selection opens from title")
    await _capture("/home/festice/ChatGPT-dev/PineCreekGame/godot/build/qa-story-select.png")
    main._story_select_back()
    _check(main.title_layer.visible, "story selection returns to title")

    # QA then follows the same title -> game-start path as the real game.
    main._start_game()
    await physics_frame
    _check(main.pause_button.visible, "pause button appears during gameplay")
    main._open_pause()
    _check(main.pause_layer.visible and paused, "pause menu freezes gameplay")
    main._resume_game()
    _check(not paused and not main.pause_layer.visible, "resume closes pause menu")
    main.set_physics_process(false)

    var car: PineVehicle = main.vehicle
    var chase: PineChaseCamera = main.chase
    var out := "/home/festice/ChatGPT-dev/PineCreekGame/godot/build"

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
