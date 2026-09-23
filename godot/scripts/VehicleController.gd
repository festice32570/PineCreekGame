class_name PineVehicle
extends RigidBody3D

const WHEEL_RADIUS := 0.46
const SUSPENSION_REST := 0.38
const SUSPENSION_DROOP := 0.18
const SPRING_RATE := 60000.0
const DAMPER_RATE := 7200.0
const CORNERING_STIFFNESS := 4200.0
const ENGINE_FORCE := 9800.0
const REVERSE_FORCE := 5600.0
const BRAKE_FORCE := 22000.0
const MAX_FORWARD_SPEED := 25.0
const MAX_REVERSE_SPEED := 8.5

var steer_input := 0.0
var steering_state := 0.0
var throttle_input := 0.0
var brake_input := 0.0
var reverse_input := 0.0

var wheel_rays: Dictionary = {}
var wheel_visuals: Dictionary = {}
var wheel_spin := {"FL":0.0, "FR":0.0, "RL":0.0, "RR":0.0}
var visual_root: Node3D
var last_safe_transform := Transform3D.IDENTITY
var _safe_timer := 0.0

func _ready() -> void:
    mass = 2200.0
    linear_damp = 0.06
    angular_damp = 1.15
    can_sleep = false
    continuous_cd = true
    center_of_mass_mode = RigidBody3D.CENTER_OF_MASS_MODE_CUSTOM
    center_of_mass = Vector3(0.0, -0.24, 0.0)
    _build_collision()
    _load_visual()
    _build_wheels()
    last_safe_transform = global_transform

func _build_collision() -> void:
    var shape := BoxShape3D.new()
    shape.size = Vector3(1.86, 0.72, 4.62)
    var cs := CollisionShape3D.new()
    cs.name = "ChassisCollision"
    cs.shape = shape
    cs.position = Vector3(0.0, 0.13, 0.0)
    add_child(cs)

func _load_visual() -> void:
    var packed = load("res://assets/vehicles/pine_creek_pickup.glb") as PackedScene
    if packed == null:
        push_error("Pickup model failed to load")
        return
    visual_root = packed.instantiate() as Node3D
    visual_root.name = "PickupVisual"
    visual_root.position.y = -0.60
    add_child(visual_root)
    for key in ["FL","FR","RL","RR"]:
        var n = visual_root.find_child("Wheel_" + key, true, false)
        if n is Node3D:
            wheel_visuals[key] = n

func _build_wheels() -> void:
    var anchors := {
        "FL": Vector3(-0.93, 0.20, 1.53),
        "FR": Vector3( 0.93, 0.20, 1.53),
        "RL": Vector3(-0.93, 0.20,-1.56),
        "RR": Vector3( 0.93, 0.20,-1.56)
    }
    for key in anchors:
        var ray := RayCast3D.new()
        ray.name = "Suspension_" + key
        ray.position = anchors[key]
        ray.target_position = Vector3(0.0, -(SUSPENSION_REST + WHEEL_RADIUS + SUSPENSION_DROOP), 0.0)
        ray.enabled = true
        ray.collide_with_areas = false
        ray.collide_with_bodies = true
        ray.exclude_parent = true
        add_child(ray)
        wheel_rays[key] = ray

func set_controls(steer: float, throttle: float, brake: float, reverse: float) -> void:
    steer_input = clamp(steer, -1.0, 1.0)
    throttle_input = clamp(throttle, 0.0, 1.0)
    brake_input = clamp(brake, 0.0, 1.0)
    reverse_input = clamp(reverse, 0.0, 1.0)

func get_speed_kmh() -> float:
    return linear_velocity.length() * 3.6

func get_forward_speed_kmh() -> float:
    return linear_velocity.dot(global_transform.basis.z.normalized()) * 3.6

func _physics_process(delta: float) -> void:
    var up: Vector3 = global_transform.basis.y.normalized()
    var body_forward: Vector3 = global_transform.basis.z.normalized()
    var body_speed: float = linear_velocity.dot(body_forward)
    var speed_abs: float = absf(body_speed)
    var speed_ratio: float = clampf(speed_abs / MAX_FORWARD_SPEED, 0.0, 1.0)

    # Digital touch input is filtered through a steering-rack state so a tap
    # does not instantly become full lock. Return-to-center is intentionally
    # faster than steering-in, while high speed slows both directions.
    var steer_in_rate: float = lerpf(2.7, 1.25, speed_ratio)
    var steer_return_rate: float = lerpf(4.8, 2.1, speed_ratio)
    var rate: float = steer_return_rate if absf(steer_input) < absf(steering_state) else steer_in_rate
    steering_state = move_toward(steering_state, steer_input, rate * delta)

    var max_steer_deg: float = lerpf(31.0, 7.0, float(pow(speed_ratio, 0.55)))
    # The pickup model uses +Z as forward. Positive Y rotation therefore maps
    # opposite to the driver's left/right expectation, so invert wheel yaw here.
    var steer_angle: float = -deg_to_rad(max_steer_deg) * steering_state

    var grounded: int = 0

    for key in wheel_rays:
        var ray: RayCast3D = wheel_rays[key]
        ray.force_raycast_update()
        if not ray.is_colliding():
            _animate_wheel_air(str(key), steer_angle, delta, body_speed)
            continue

        grounded += 1
        var hit: Vector3 = ray.get_collision_point()
        var normal: Vector3 = ray.get_collision_normal().normalized()
        var origin: Vector3 = ray.global_position
        var hit_distance: float = origin.distance_to(hit)
        var current_suspension: float = clampf(hit_distance - WHEEL_RADIUS, 0.0, SUSPENSION_REST + SUSPENSION_DROOP)
        var compression: float = SUSPENSION_REST - current_suspension
        var offset: Vector3 = hit - global_position
        var point_velocity: Vector3 = linear_velocity + angular_velocity.cross(offset)
        var vertical_speed: float = point_velocity.dot(up)
        var spring_force: float = maxf(0.0, compression * SPRING_RATE - vertical_speed * DAMPER_RATE)

        apply_force(normal * spring_force, offset)

        var key_text := str(key)
        var is_front: bool = key_text == "FL" or key_text == "FR"
        var wheel_forward: Vector3 = body_forward
        if is_front:
            wheel_forward = wheel_forward.rotated(normal, steer_angle)
        wheel_forward = (wheel_forward - normal * wheel_forward.dot(normal)).normalized()
        var wheel_right: Vector3 = wheel_forward.cross(normal).normalized()

        var longitudinal_speed: float = point_velocity.dot(wheel_forward)
        var lateral_speed: float = point_velocity.dot(wheel_right)
        var collider: Object = ray.get_collider() as Object
        var grip: float = 0.82
        if collider != null and collider.has_meta("grip"):
            grip = float(collider.get_meta("grip"))
        var grip_limit: float = maxf(900.0, spring_force * grip)

        var lateral_force_mag: float = clampf(-lateral_speed * CORNERING_STIFFNESS, -grip_limit, grip_limit)
        apply_force(wheel_right * lateral_force_mag, offset)

        var is_rear: bool = key_text == "RL" or key_text == "RR"
        if is_rear:
            var drive_force: float = 0.0
            if throttle_input > 0.0 and body_speed < MAX_FORWARD_SPEED:
                var limiter: float = clampf((MAX_FORWARD_SPEED - body_speed) / 5.0, 0.0, 1.0)
                drive_force += ENGINE_FORCE * 0.5 * throttle_input * limiter
            if reverse_input > 0.0 and body_speed > -MAX_REVERSE_SPEED:
                var rlimiter: float = clampf((MAX_REVERSE_SPEED + body_speed) / 3.0, 0.0, 1.0)
                drive_force -= REVERSE_FORCE * 0.5 * reverse_input * rlimiter

            # Longitudinal drive is limited by the tyre's available contact
            # force. Packed road hooks up; deep snow spins/slips sooner.
            var traction_limit: float = grip_limit * 0.88
            drive_force = clampf(drive_force, -traction_limit, traction_limit)
            if drive_force != 0.0:
                apply_force(wheel_forward * drive_force, offset)

        if brake_input <= 0.0 and throttle_input <= 0.01 and reverse_input <= 0.01:
            var rolling_limit: float = grip_limit * 0.11
            var rolling_force: float = clampf(-longitudinal_speed * 245.0, -rolling_limit, rolling_limit)
            apply_force(wheel_forward * rolling_force, offset)

        if brake_input > 0.0 and absf(longitudinal_speed) > 0.05:
            var brake_limit: float = minf(BRAKE_FORCE * 0.25 * brake_input, grip_limit * 1.15)
            apply_force(-wheel_forward * signf(longitudinal_speed) * brake_limit, offset)

        _animate_wheel(key_text, current_suspension, steer_angle, longitudinal_speed, delta)

    # Drag and stability are applied to the body, never to camera orientation.
    var horizontal_velocity: Vector3 = linear_velocity - up * linear_velocity.dot(up)
    var drag_force: Vector3 = -horizontal_velocity * (95.0 + horizontal_velocity.length() * 16.0)
    apply_central_force(drag_force)

    # Mild angular damping around roll/pitch keeps a heavy pickup composed,
    # while leaving yaw mostly to tyre forces.
    var local_omega: Vector3 = global_transform.basis.inverse() * angular_velocity
    var stabilizing_local := Vector3(-local_omega.x * 900.0, -local_omega.y * 120.0, -local_omega.z * 900.0)
    apply_torque(global_transform.basis * stabilizing_local)

    _safe_timer += delta
    if grounded >= 2 and absf(rotation.x) < 0.55 and absf(rotation.z) < 0.55 and _safe_timer > 0.8:
        last_safe_transform = global_transform
        _safe_timer = 0.0

func _animate_wheel(key: String, current_suspension: float, steer_angle: float, longitudinal_speed: float, delta: float) -> void:
    if not wheel_visuals.has(key) or visual_root == null:
        return
    var wheel: Node3D = wheel_visuals[key]
    var ray: RayCast3D = wheel_rays[key]
    wheel.position.y = ray.position.y - current_suspension - visual_root.position.y
    wheel_spin[key] = fmod(float(wheel_spin[key]) + longitudinal_speed / WHEEL_RADIUS * delta, TAU)
    var is_front: bool = key == "FL" or key == "FR"
    wheel.rotation = Vector3(float(wheel_spin[key]), steer_angle if is_front else 0.0, 0.0)

func _animate_wheel_air(key: String, steer_angle: float, delta: float, body_speed: float) -> void:
    _animate_wheel(key, SUSPENSION_REST + SUSPENSION_DROOP, steer_angle, body_speed, delta)

func reset_to_safe() -> void:
    freeze = true
    sleeping = true
    global_transform = last_safe_transform
    linear_velocity = Vector3.ZERO
    angular_velocity = Vector3.ZERO
    steering_state = 0.0
    set_controls(0.0,0.0,1.0,0.0)
    reset_physics_interpolation()
    sleeping = false
    freeze = false
