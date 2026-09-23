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
    check(left_x < -0.08, "left steering curves vehicle to its left")

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
    check(right_x > 0.08, "right steering curves vehicle to its right")
    check(absf(absf(left_x) - absf(right_x)) < maxf(1.2, absf(left_x)*0.5), "left/right steering response stays reasonably symmetric")

    # Timed-story regression: timeout restarts, arrival advances.
    var story := PineStoryDirector.new()
    story.attach_vehicle(car)
    world.add_child(story)
    story.started = true
    story.episode_index = 8
    story.stage_index = 1
    story.stage_timer = 0.01
    story.set_process(false)
    car.freeze = true
    car.global_position = Vector3.ZERO
    story._process(0.05)
    check(story.stage_index == 1 and story.stage_timer > 40.0,
        "timed mission restarts after timeout")

    car.global_position = Vector3(12,0.62,49)
    story.stage_timer = 20.0
    story._process(0.01)
    check(story.episode_index == 9 and story.stage_index == 0,
        "reaching timed destination advances to next episode")
    check(story.episodes.size() >= 12, "campaign contains multiple crazy town episodes")

    world.queue_free()
    await process_frame

    if failures == 0:
        print("GODOT_TESTS=PASS")
        quit(0)
    else:
        print("GODOT_TESTS=FAIL count=",failures)
        quit(1)
