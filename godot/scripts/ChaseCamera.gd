class_name PineChaseCamera
extends Node3D

var target: Node3D
var camera: Camera3D
var follow_yaw := 0.0
var orbit_yaw := 0.0
var orbit_pitch := -0.10
var last_manual_orbit := -100.0

func _ready() -> void:
    camera = Camera3D.new()
    camera.name = "Camera"
    camera.current = true
    camera.fov = 66.0
    camera.near = 0.15
    add_child(camera)

func set_target(node: Node3D) -> void:
    target = node
    if target:
        follow_yaw = target.global_rotation.y

func add_orbit_drag(delta: Vector2) -> void:
    orbit_yaw = clamp(orbit_yaw - delta.x * 0.0042, -1.0, 1.0)
    orbit_pitch = clamp(orbit_pitch - delta.y * 0.0030, -0.45, 0.20)
    last_manual_orbit = Time.get_ticks_msec() / 1000.0

func _physics_process(delta: float) -> void:
    if target == null:
        return
    var now := Time.get_ticks_msec() / 1000.0
    if now - last_manual_orbit > 1.6:
        orbit_yaw = lerp(orbit_yaw, 0.0, 1.0 - exp(-2.2 * delta))

    var vehicle_yaw := target.global_rotation.y
    var yaw_error := wrapf(vehicle_yaw - follow_yaw, -PI, PI)
    if absf(yaw_error) > deg_to_rad(2.5):
        follow_yaw = lerp_angle(follow_yaw, vehicle_yaw, 1.0 - exp(-1.75 * delta))

    var forward := Vector3(sin(follow_yaw), 0.0, cos(follow_yaw)).normalized()
    var right := Vector3(forward.z, 0.0, -forward.x)
    var orbit_forward := forward.rotated(Vector3.UP, orbit_yaw)
    var target_point := target.global_position + Vector3.UP * 0.80 + forward * 1.0
    var horizontal_distance := 6.6
    var height := 2.55 + (-orbit_pitch) * 2.4
    var desired := target.global_position - orbit_forward * horizontal_distance + Vector3.UP * height
    desired += right * sin(orbit_yaw) * 0.45

    global_position = global_position.lerp(desired, 1.0 - exp(-7.0 * delta))
    camera.look_at(target_point, Vector3.UP)
