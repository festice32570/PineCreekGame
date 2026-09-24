extends Node3D

const ROAD_RESCUE_Y := 0.62
const START_POINT := Vector3(0.0, ROAD_RESCUE_Y, 22.0)
const START_STABILIZE_FRAMES := 42

var vehicle: PineVehicle
var vehicle_audio: PineVehicleAudio
var game_audio: PineGameAudio
var touch: PineTouchControls
var chase: PineChaseCamera
var hud: Label
var info_label: Label
var title_layer: CanvasLayer
var story_select_layer: CanvasLayer
var pause_layer: CanvasLayer
var pause_button: Button
var story: PineStoryDirector
var objective_label: Label
var dialogue_label: Label
var mission_marker: Node3D
var game_started := false
var game_paused := false
var selected_episode := 0
var _reset_key_was_down := false
var _horn_key_was_down := false
var _action_key_was_down := false
var _dialogue_hide_at := 0.0

func _ready() -> void:
    _build_environment()
    _build_vehicle()
    _build_camera()
    _build_ui()
    _build_story()
    _build_audio()
    _build_title()
    _build_story_select()
    _build_pause_menu()
    if OS.has_environment("PINE_SKIP_TITLE"):
        _start_game()
    if OS.has_environment("PINE_CAPTURE_PATH"):
        _capture_after_startup.call_deferred()
    elif "--qa-screenshot" in OS.get_cmdline_user_args():
        _capture_qa_screenshot.call_deferred()

func _capture_after_startup() -> void:
    await get_tree().create_timer(1.5).timeout
    var image := get_viewport().get_texture().get_image()
    var output_path := OS.get_environment("PINE_CAPTURE_PATH")
    var err := image.save_png(output_path)
    if err == OK:
        print("PINE_CAPTURE_SAVED=", output_path)
    else:
        push_error("Failed to save capture: " + str(err))
    get_tree().quit()

func _build_environment() -> void:
    var world_env := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color("#b9cada")
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color("#b7c5d3")
    env.ambient_light_energy = 0.75
    env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    world_env.environment = env
    add_child(world_env)

    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-48,-28,0)
    sun.light_energy = 1.15
    sun.shadow_enabled = true
    add_child(sun)

    _make_surface("SnowGround", Vector3(0,-0.28,0), Vector3(80,0.5,180), Color("#d7e3ea"), 0.42)
    _make_surface("PackedSnowRoad", Vector3(0,0.00,0), Vector3(9.0,0.10,150), Color("#7f8d96"), 0.86)
    _make_surface("CrossRoad", Vector3(0,0.005,-24), Vector3(55,0.09,8), Color("#7f8d96"), 0.84)
    _make_road_details()

    for z in range(-60,61,12):
        _make_snowbank(Vector3(-5.2,0.25,float(z)))
        _make_snowbank(Vector3(5.2,0.25,float(z)))
    for i in range(18):
        var side := -1.0 if i % 2 == 0 else 1.0
        _make_tree(Vector3(side*(9.0+float((i*7)%8)),0.0,-65.0+float(i*8)))

    for z in range(-52,53,18):
        var side_x := -5.4 if int(z / 18) % 2 == 0 else 5.4
        _make_streetlamp(Vector3(side_x,0.0,float(z)))

    _make_building(Vector3(-13,1.65,-21), Vector3(9,3.3,7), Color("#8a3330"), "PINE CREEK FAMILY BBQ")
    _make_building(Vector3(14,1.8,-4), Vector3(8,3.6,8), Color("#596b78"), "町役場")
    _make_building(Vector3(-13,1.45,18), Vector3(7,2.9,6), Color("#6c5542"), "BOB'S USED CARS")
    _make_building(Vector3(13,1.55,13), Vector3(7,3.1,6), Color("#516b63"), "GAS & COFFEE")
    _make_building(Vector3(13,1.65,30), Vector3(8,3.3,7), Color("#5b4b43"), "JIM'S GARAGE")
    _make_building(Vector3(14,1.55,-37), Vector3(9,3.1,7), Color("#465764"), "PUBLIC WORKS")
    _make_building(Vector3(-12,1.35,49), Vector3(6,2.7,6), Color("#7a5e4f"), "HOUSE 11")
    _make_building(Vector3(12,1.35,49), Vector3(6,2.7,6), Color("#66764e"), "HOUSE 12")

    # Town props make story locations visible before dialogue is triggered.
    _make_parked_pickup(Vector3(-15.5,0.54,20.5), -18.0)
    _make_parked_pickup(Vector3(16.8,0.54,31.5), 165.0)
    _make_parked_pickup(Vector3(17.0,0.54,-39.0), 10.0)
    _make_turkey(Vector3(10.5,0.0,-2.0))
    _make_snowman(Vector3(11.7,0.0,-7.0), 1.0)
    _make_snowman(Vector3(13.0,0.0,-7.3), 0.92)
    _make_snowman(Vector3(14.3,0.0,-7.0), 1.08)

func _make_surface(name: String, pos: Vector3, size3: Vector3, color: Color, grip: float) -> void:
    var body := StaticBody3D.new()
    body.name = name
    body.position = pos
    body.set_meta("grip",grip)
    var mesh := MeshInstance3D.new()
    var bm := BoxMesh.new()
    bm.size = size3
    mesh.mesh = bm
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    mat.roughness = 0.92
    mesh.material_override = mat
    body.add_child(mesh)
    var cs := CollisionShape3D.new()
    var bs := BoxShape3D.new()
    bs.size = size3
    cs.shape = bs
    body.add_child(cs)
    add_child(body)

func _make_road_details() -> void:
    var rut_mat := _simple_material(Color("#66737b"),0.98)
    for x in [-1.55,1.55]:
        var rut := MeshInstance3D.new()
        var rut_mesh := BoxMesh.new()
        rut_mesh.size = Vector3(0.58,0.018,146.0)
        rut.mesh = rut_mesh
        rut.position = Vector3(float(x),0.065,0)
        rut.material_override = rut_mat
        add_child(rut)

    for z in range(-68,69,10):
        var marker := MeshInstance3D.new()
        var marker_mesh := BoxMesh.new()
        marker_mesh.size = Vector3(0.12,0.022,3.4)
        marker.mesh = marker_mesh
        marker.position = Vector3(0,0.076,float(z))
        marker.material_override = _simple_material(Color("#d8d9cb"),0.88)
        add_child(marker)

    for zoff in [-1.35,1.35]:
        var cross_rut := MeshInstance3D.new()
        var cross_mesh := BoxMesh.new()
        cross_mesh.size = Vector3(52.0,0.018,0.52)
        cross_rut.mesh = cross_mesh
        cross_rut.position = Vector3(0,0.071,-24.0 + float(zoff))
        cross_rut.material_override = rut_mat
        add_child(cross_rut)

func _make_streetlamp(pos: Vector3) -> void:
    var root := StaticBody3D.new()
    root.position = pos

    var pole := MeshInstance3D.new()
    var pole_mesh := CylinderMesh.new()
    pole_mesh.top_radius = 0.055
    pole_mesh.bottom_radius = 0.075
    pole_mesh.height = 4.4
    pole.mesh = pole_mesh
    pole.position.y = 2.2
    pole.material_override = _simple_material(Color("#30373b"),0.52,0.55)
    root.add_child(pole)

    var arm := MeshInstance3D.new()
    var arm_mesh := BoxMesh.new()
    arm_mesh.size = Vector3(0.75,0.08,0.08)
    arm.mesh = arm_mesh
    arm.position = Vector3(-0.34,4.24,0)
    arm.material_override = pole.material_override
    root.add_child(arm)

    var lamp := MeshInstance3D.new()
    var lamp_mesh := SphereMesh.new()
    lamp_mesh.radius = 0.13
    lamp_mesh.height = 0.24
    lamp.mesh = lamp_mesh
    lamp.position = Vector3(-0.70,4.18,0)
    var lamp_mat := _simple_material(Color("#ffe3a1"),0.18)
    lamp_mat.emission_enabled = true
    lamp_mat.emission = Color("#ffd27a")
    lamp_mat.emission_energy_multiplier = 2.8
    lamp.material_override = lamp_mat
    root.add_child(lamp)

    var light := OmniLight3D.new()
    light.position = Vector3(-0.70,4.05,0)
    light.light_color = Color("#ffdca0")
    light.light_energy = 0.45
    light.omni_range = 8.0
    light.shadow_enabled = false
    root.add_child(light)

    var cs := CollisionShape3D.new()
    var shape := CylinderShape3D.new()
    shape.radius = 0.11
    shape.height = 4.4
    cs.shape = shape
    cs.position.y = 2.2
    root.add_child(cs)
    add_child(root)

func _make_snowbank(pos: Vector3) -> void:
    var m := MeshInstance3D.new()
    var mesh := SphereMesh.new()
    mesh.radius = 1.25
    mesh.height = 1.05
    m.mesh = mesh
    m.scale = Vector3(1.0,0.55,2.8)
    m.position = pos
    var mat := StandardMaterial3D.new()
    mat.albedo_color = Color("#ecf3f6")
    mat.roughness = 0.95
    m.material_override = mat
    add_child(m)

func _make_tree(pos: Vector3) -> void:
    var root := StaticBody3D.new()
    root.position = pos
    var trunk := MeshInstance3D.new()
    var cm := CylinderMesh.new()
    cm.top_radius = 0.13
    cm.bottom_radius = 0.18
    cm.height = 2.2
    trunk.mesh = cm
    trunk.position.y = 1.1
    var tm := StandardMaterial3D.new()
    tm.albedo_color = Color("#3b2d25")
    trunk.material_override = tm
    root.add_child(trunk)
    var trunk_collision := CollisionShape3D.new()
    var trunk_shape := CylinderShape3D.new()
    trunk_shape.radius = 0.24
    trunk_shape.height = 2.2
    trunk_collision.shape = trunk_shape
    trunk_collision.position.y = 1.1
    root.add_child(trunk_collision)
    for j in range(3):
        var crown := MeshInstance3D.new()
        var cone := CylinderMesh.new()
        cone.top_radius = 0.0
        cone.bottom_radius = 1.15 - j*0.16
        cone.height = 2.1
        crown.mesh = cone
        crown.position.y = 2.0 + j*0.8
        var gm := StandardMaterial3D.new()
        gm.albedo_color = Color("#153f38")
        gm.roughness = 0.9
        crown.material_override = gm
        root.add_child(crown)
    add_child(root)

func _make_building(pos: Vector3, size3: Vector3, color: Color, title: String) -> void:
    var root := StaticBody3D.new()
    root.position = pos
    var m := MeshInstance3D.new()
    var box := BoxMesh.new()
    box.size = size3
    m.mesh = box
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    mat.roughness = 0.75
    m.material_override = mat
    root.add_child(m)

    var cs := CollisionShape3D.new()
    var bs := BoxShape3D.new()
    bs.size = size3
    cs.shape = bs
    root.add_child(cs)

    var roof := MeshInstance3D.new()
    var roof_mesh := BoxMesh.new()
    roof_mesh.size = Vector3(size3.x + 0.55,0.22,size3.z + 0.55)
    roof.mesh = roof_mesh
    roof.position.y = size3.y * 0.5 + 0.14
    roof.material_override = _simple_material(Color("#d9e4e9"),0.96)
    root.add_child(roof)

    var front_sign := 1.0 if pos.x < 0.0 else -1.0
    var front_x := front_sign * (size3.x * 0.5 + 0.018)
    var glass_mat := _simple_material(Color("#6e8791"),0.18,0.08)
    glass_mat.emission_enabled = true
    glass_mat.emission = Color("#c7a66b")
    glass_mat.emission_energy_multiplier = 0.55

    for zoff in [-size3.z*0.27, size3.z*0.27]:
        var window := MeshInstance3D.new()
        var window_mesh := BoxMesh.new()
        window_mesh.size = Vector3(0.055,0.78,minf(1.25,size3.z*0.22))
        window.mesh = window_mesh
        window.position = Vector3(front_x,0.20,float(zoff))
        window.material_override = glass_mat
        root.add_child(window)

    var door := MeshInstance3D.new()
    var door_mesh := BoxMesh.new()
    door_mesh.size = Vector3(0.065,1.75,0.88)
    door.mesh = door_mesh
    door.position = Vector3(front_x,-size3.y*0.5 + 0.88,0)
    door.material_override = _simple_material(Color("#3a302a"),0.82)
    root.add_child(door)

    var awning := MeshInstance3D.new()
    var awning_mesh := BoxMesh.new()
    awning_mesh.size = Vector3(0.55,0.09,1.25)
    awning.mesh = awning_mesh
    awning.position = Vector3(front_x + front_sign*0.23,0.86,0)
    awning.material_override = _simple_material(Color("#e4ecef"),0.92)
    root.add_child(awning)

    var label := Label3D.new()
    label.text = title
    label.font_size = 46
    label.outline_size = 8
    label.position = Vector3(front_x + front_sign*0.12,size3.y*0.22,0)
    label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
    root.add_child(label)
    add_child(root)


func _make_parked_pickup(pos: Vector3, yaw_deg: float) -> void:
    var packed := load("res://assets/vehicles/pine_creek_pickup.glb") as PackedScene
    if packed == null:
        return
    var body := StaticBody3D.new()
    body.position = pos
    body.rotation_degrees.y = yaw_deg
    body.name = "ParkedPickup"

    var visual := packed.instantiate() as Node3D
    visual.position.y = -0.54
    body.add_child(visual)

    var cs := CollisionShape3D.new()
    var shape := BoxShape3D.new()
    shape.size = Vector3(1.90,0.78,4.65)
    cs.shape = shape
    body.add_child(cs)
    add_child(body)

func _simple_material(color: Color, roughness := 0.75, metallic := 0.0) -> StandardMaterial3D:
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    mat.roughness = roughness
    mat.metallic = metallic
    return mat

func _make_turkey(pos: Vector3) -> void:
    var root := Node3D.new()
    root.name = "Kevin"
    root.position = pos

    var body := MeshInstance3D.new()
    var body_mesh := SphereMesh.new()
    body_mesh.radius = 0.52
    body_mesh.height = 1.02
    body.mesh = body_mesh
    body.scale = Vector3(0.82,1.05,1.0)
    body.position.y = 0.62
    body.material_override = _simple_material(Color("#3b2118"),0.95)
    root.add_child(body)

    var neck := MeshInstance3D.new()
    var neck_mesh := CylinderMesh.new()
    neck_mesh.top_radius = 0.13
    neck_mesh.bottom_radius = 0.16
    neck_mesh.height = 0.62
    neck.mesh = neck_mesh
    neck.position = Vector3(0,1.24,0.27)
    neck.material_override = _simple_material(Color("#7b1d20"),0.85)
    root.add_child(neck)

    var head := MeshInstance3D.new()
    var head_mesh := SphereMesh.new()
    head_mesh.radius = 0.23
    head_mesh.height = 0.44
    head.mesh = head_mesh
    head.position = Vector3(0,1.58,0.30)
    head.material_override = _simple_material(Color("#315b77"),0.75)
    root.add_child(head)

    var beak := MeshInstance3D.new()
    var beak_mesh := CylinderMesh.new()
    beak_mesh.top_radius = 0.0
    beak_mesh.bottom_radius = 0.12
    beak_mesh.height = 0.32
    beak.mesh = beak_mesh
    beak.rotation_degrees.x = 90
    beak.position = Vector3(0,1.55,0.55)
    beak.material_override = _simple_material(Color("#e49b32"),0.7)
    root.add_child(beak)

    for i in range(7):
        var tail := MeshInstance3D.new()
        var tail_mesh := SphereMesh.new()
        tail_mesh.radius = 0.42
        tail_mesh.height = 0.78
        tail.mesh = tail_mesh
        var angle := deg_to_rad(-72.0 + float(i)*24.0)
        tail.position = Vector3(sin(angle)*0.58,0.83,-0.40 + cos(angle)*0.16)
        tail.rotation_degrees.z = -72.0 + float(i)*24.0
        tail.scale = Vector3(0.38,1.15,0.22)
        tail.material_override = _simple_material(Color("#513126"),0.95)
        root.add_child(tail)

    var label := Label3D.new()
    label.text = "ケビン"
    label.position = Vector3(0,2.15,0)
    label.font_size = 34
    label.outline_size = 7
    label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
    root.add_child(label)
    add_child(root)

func _make_snowman(pos: Vector3, scale_factor: float) -> void:
    var root := Node3D.new()
    root.position = pos
    root.scale = Vector3.ONE * scale_factor
    for data in [
        [0.48,Vector3(0,0.48,0)],
        [0.37,Vector3(0,1.14,0)],
        [0.28,Vector3(0,1.69,0)]
    ]:
        var part := MeshInstance3D.new()
        var sphere := SphereMesh.new()
        sphere.radius = float(data[0])
        sphere.height = float(data[0]) * 2.0
        part.mesh = sphere
        part.position = data[1]
        part.material_override = _simple_material(Color("#f1f5f6"),0.98)
        root.add_child(part)

    var nose := MeshInstance3D.new()
    var cone := CylinderMesh.new()
    cone.top_radius = 0.0
    cone.bottom_radius = 0.07
    cone.height = 0.30
    nose.mesh = cone
    nose.rotation_degrees.x = 90
    nose.position = Vector3(0,1.69,0.31)
    nose.material_override = _simple_material(Color("#e8832f"),0.85)
    root.add_child(nose)
    add_child(root)

func _build_vehicle() -> void:
    vehicle = PineVehicle.new()
    vehicle.name = "PlayerPickup"
    vehicle.freeze = true
    vehicle.position = START_POINT
    vehicle.visible = false
    add_child(vehicle)

    vehicle_audio = PineVehicleAudio.new()
    vehicle_audio.name = "VehicleAudio"
    vehicle_audio.attach_vehicle(vehicle)
    add_child(vehicle_audio)

func _build_camera() -> void:
    chase = PineChaseCamera.new()
    chase.name = "ChaseCamera"
    add_child(chase)
    chase.set_target(vehicle)
    chase.global_position = vehicle.global_position + Vector3(0,3,-7)

func _build_ui() -> void:
    var layer := CanvasLayer.new()
    layer.name = "UI"
    add_child(layer)

    touch = PineTouchControls.new()
    layer.add_child(touch)
    touch.camera_dragged.connect(chase.add_orbit_drag)
    touch.reset_requested.connect(_reset_vehicle_to_road)
    touch.visible = false

    hud = Label.new()
    hud.position = Vector2(26,22)
    hud.add_theme_font_size_override("font_size",24)
    hud.add_theme_color_override("font_color",Color.WHITE)
    hud.add_theme_color_override("font_shadow_color",Color(0,0,0,0.85))
    hud.add_theme_constant_override("shadow_offset_x",2)
    hud.add_theme_constant_override("shadow_offset_y",2)
    layer.add_child(hud)
    hud.visible = false

    info_label = Label.new()
    info_label.position = Vector2(26,58)
    info_label.text = "PINE CREEK RADIO  CH 7  |  雪道では早めの減速"
    info_label.add_theme_font_size_override("font_size",18)
    info_label.modulate = Color(1,1,1,0.88)
    layer.add_child(info_label)
    info_label.visible = false

    objective_label = Label.new()
    objective_label.position = Vector2(26,92)
    objective_label.size = Vector2(650,72)
    objective_label.add_theme_font_size_override("font_size",20)
    objective_label.add_theme_color_override("font_color",Color("#fff3b0"))
    objective_label.add_theme_color_override("font_shadow_color",Color(0,0,0,0.95))
    objective_label.add_theme_constant_override("shadow_offset_x",2)
    objective_label.add_theme_constant_override("shadow_offset_y",2)
    objective_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    var objective_panel := StyleBoxFlat.new()
    objective_panel.bg_color = Color(0.03,0.045,0.055,0.58)
    objective_panel.set_corner_radius_all(12)
    objective_panel.content_margin_left = 12
    objective_panel.content_margin_right = 12
    objective_panel.content_margin_top = 7
    objective_panel.content_margin_bottom = 7
    objective_label.add_theme_stylebox_override("normal",objective_panel)
    layer.add_child(objective_label)
    objective_label.visible = false

    dialogue_label = Label.new()
    dialogue_label.position = Vector2(350,22)
    dialogue_label.size = Vector2(580,94)
    dialogue_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    dialogue_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    dialogue_label.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    dialogue_label.add_theme_font_size_override("font_size",19)
    dialogue_label.add_theme_color_override("font_color",Color.WHITE)
    dialogue_label.add_theme_color_override("font_shadow_color",Color(0,0,0,0.95))
    dialogue_label.add_theme_constant_override("shadow_offset_x",2)
    dialogue_label.add_theme_constant_override("shadow_offset_y",2)
    var dialogue_panel := StyleBoxFlat.new()
    dialogue_panel.bg_color = Color(0.02,0.028,0.035,0.68)
    dialogue_panel.set_corner_radius_all(14)
    dialogue_panel.content_margin_left = 14
    dialogue_panel.content_margin_right = 14
    dialogue_panel.content_margin_top = 8
    dialogue_panel.content_margin_bottom = 8
    dialogue_label.add_theme_stylebox_override("normal",dialogue_panel)
    layer.add_child(dialogue_label)
    dialogue_label.visible = false

    pause_button = Button.new()
    pause_button.text = "Ⅱ"
    pause_button.position = Vector2(1008,24)
    pause_button.size = Vector2(104,72)
    pause_button.add_theme_font_size_override("font_size",30)
    pause_button.focus_mode = Control.FOCUS_NONE
    pause_button.pressed.connect(_open_pause)
    layer.add_child(pause_button)
    pause_button.visible = false

func _build_story() -> void:
    story = PineStoryDirector.new()
    story.name = "StoryDirector"
    story.attach_vehicle(vehicle)
    story.objective_changed.connect(_on_objective_changed)
    story.dialogue_requested.connect(_on_dialogue_requested)
    story.marker_changed.connect(_on_marker_changed)
    add_child(story)

    touch.action_pressed.connect(story.on_action)
    touch.horn_pressed.connect(story.on_horn)
    touch.horn_pressed.connect(vehicle_audio.trigger_horn)

    mission_marker = Node3D.new()
    mission_marker.name = "MissionMarker"
    mission_marker.visible = false

    var marker_mat := StandardMaterial3D.new()
    marker_mat.albedo_color = Color("#ffd34f")
    marker_mat.emission_enabled = true
    marker_mat.emission = Color("#ffb52e")
    marker_mat.emission_energy_multiplier = 2.4
    marker_mat.roughness = 0.72

    var fill_mat := StandardMaterial3D.new()
    fill_mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    fill_mat.albedo_color = Color(1.0,0.72,0.12,0.24)
    fill_mat.emission_enabled = true
    fill_mat.emission = Color("#ffb52e")
    fill_mat.emission_energy_multiplier = 0.75
    fill_mat.roughness = 0.82

    var parking_fill := MeshInstance3D.new()
    var fill_mesh := BoxMesh.new()
    fill_mesh.size = Vector3(3.46,0.012,6.32)
    parking_fill.mesh = fill_mesh
    parking_fill.position = Vector3(0,0.012,0)
    parking_fill.material_override = fill_mat
    mission_marker.add_child(parking_fill)

    # Roadside destination is drawn as a real parking bay instead of a beacon
    # placed inside the destination building.
    for x in [-1.7, 1.7]:
        var side := MeshInstance3D.new()
        var side_mesh := BoxMesh.new()
        side_mesh.size = Vector3(0.22,0.035,6.4)
        side.mesh = side_mesh
        side.position = Vector3(float(x),0.025,0)
        side.material_override = marker_mat
        mission_marker.add_child(side)
    for z in [-3.2, 3.2]:
        var end_line := MeshInstance3D.new()
        var end_mesh := BoxMesh.new()
        end_mesh.size = Vector3(3.62,0.035,0.22)
        end_line.mesh = end_mesh
        end_line.position = Vector3(0,0.025,float(z))
        end_line.material_override = marker_mat
        mission_marker.add_child(end_line)

    var arrow := MeshInstance3D.new()
    var arrow_mesh := CylinderMesh.new()
    arrow_mesh.top_radius = 0.0
    arrow_mesh.bottom_radius = 0.46
    arrow_mesh.height = 0.92
    arrow.mesh = arrow_mesh
    arrow.rotation_degrees.x = 180.0
    arrow.position = Vector3(0,2.15,0)
    arrow.material_override = marker_mat
    mission_marker.add_child(arrow)

    var label := Label3D.new()
    label.text = "目的地\n黄色い駐車枠"
    label.font_size = 34
    label.outline_size = 8
    label.position = Vector3(0,3.35,0)
    label.billboard = BaseMaterial3D.BILLBOARD_ENABLED
    label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    mission_marker.add_child(label)
    add_child(mission_marker)

func _on_objective_changed(chapter: String, objective: String) -> void:
    objective_label.text = chapter + "\n" + objective

func _on_dialogue_requested(speaker: String, line: String) -> void:
    dialogue_label.text = speaker + "： " + line
    dialogue_label.visible = true
    _dialogue_hide_at = Time.get_ticks_msec() / 1000.0 + 5.0

func _on_marker_changed(pos: Vector3, visible_now: bool) -> void:
    mission_marker.global_position = pos
    mission_marker.visible = visible_now

func _build_audio() -> void:
    game_audio = PineGameAudio.new()
    game_audio.name = "GameAudio"
    add_child(game_audio)

func _build_title() -> void:
    title_layer = CanvasLayer.new()
    title_layer.name = "Title"
    title_layer.layer = 20
    add_child(title_layer)

    var art_bg := TextureRect.new()
    art_bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    art_bg.texture = load("res://assets/ui/pine_creek_keyart.png")
    art_bg.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
    art_bg.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
    art_bg.modulate = Color(0.32,0.32,0.32,1.0)
    art_bg.mouse_filter = Control.MOUSE_FILTER_IGNORE
    title_layer.add_child(art_bg)

    var art := TextureRect.new()
    art.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    art.texture = load("res://assets/ui/pine_creek_keyart.png")
    art.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
    art.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
    art.mouse_filter = Control.MOUSE_FILTER_IGNORE
    title_layer.add_child(art)

    var shade := ColorRect.new()
    shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    shade.color = Color(0.0,0.0,0.0,0.18)
    shade.mouse_filter = Control.MOUSE_FILTER_IGNORE
    title_layer.add_child(shade)

    var panel := VBoxContainer.new()
    panel.set_anchors_preset(Control.PRESET_CENTER)
    panel.position = Vector2(-230,62)
    panel.size = Vector2(460,300)
    panel.alignment = BoxContainer.ALIGNMENT_CENTER
    panel.add_theme_constant_override("separation",12)
    title_layer.add_child(panel)

    var start := Button.new()
    start.text = "最初から開始"
    start.custom_minimum_size = Vector2(460,76)
    start.add_theme_font_size_override("font_size",28)
    start.pressed.connect(_start_game)
    panel.add_child(start)

    var select := Button.new()
    select.text = "ストーリーを選ぶ"
    select.custom_minimum_size = Vector2(460,68)
    select.add_theme_font_size_override("font_size",26)
    select.pressed.connect(_show_story_select)
    panel.add_child(select)

    var note := Label.new()
    note.text = "v0.8.3 alpha  •  PCM音声 / ループ修正 / 道路復帰強化"
    note.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    note.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    note.add_theme_font_size_override("font_size",17)
    note.add_theme_color_override("font_color",Color.WHITE)
    note.add_theme_color_override("font_shadow_color",Color.BLACK)
    note.add_theme_constant_override("shadow_offset_x",2)
    note.add_theme_constant_override("shadow_offset_y",2)
    panel.add_child(note)

    vehicle.freeze = true

func _build_story_select() -> void:
    story_select_layer = CanvasLayer.new()
    story_select_layer.name = "StorySelect"
    story_select_layer.layer = 25
    story_select_layer.visible = false
    add_child(story_select_layer)

    var shade := ColorRect.new()
    shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    shade.color = Color(0.015,0.02,0.025,0.95)
    story_select_layer.add_child(shade)

    var panel := VBoxContainer.new()
    panel.set_anchors_preset(Control.PRESET_CENTER)
    panel.position = Vector2(-470,-320)
    panel.size = Vector2(940,640)
    panel.add_theme_constant_override("separation",10)
    story_select_layer.add_child(panel)

    var heading := Label.new()
    heading.text = "PINE CREEK　ストーリー選択"
    heading.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    heading.add_theme_font_size_override("font_size",32)
    panel.add_child(heading)

    var hint := Label.new()
    hint.text = "好きな事件から開始できます。クリア後は次の事件へ続きます。"
    hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    hint.add_theme_font_size_override("font_size",18)
    panel.add_child(hint)

    var scroll := ScrollContainer.new()
    scroll.custom_minimum_size = Vector2(920,500)
    scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    panel.add_child(scroll)

    var list := VBoxContainer.new()
    list.custom_minimum_size = Vector2(890,0)
    list.add_theme_constant_override("separation",8)
    scroll.add_child(list)

    for i in range(story.get_episode_count()):
        var b := Button.new()
        b.text = "%s\n%s" % [story.get_episode_title(i), story.get_episode_summary(i)]
        b.custom_minimum_size = Vector2(870,74)
        b.alignment = HORIZONTAL_ALIGNMENT_LEFT
        b.add_theme_font_size_override("font_size",19)
        b.pressed.connect(_start_selected_episode.bind(i))
        list.add_child(b)

    var back := Button.new()
    back.text = "タイトルへ戻る"
    back.custom_minimum_size = Vector2(940,58)
    back.add_theme_font_size_override("font_size",22)
    back.pressed.connect(_story_select_back)
    panel.add_child(back)

func _build_pause_menu() -> void:
    pause_layer = CanvasLayer.new()
    pause_layer.name = "PauseMenu"
    pause_layer.layer = 30
    pause_layer.process_mode = Node.PROCESS_MODE_WHEN_PAUSED
    pause_layer.visible = false
    add_child(pause_layer)

    var shade := ColorRect.new()
    shade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    shade.color = Color(0.0,0.0,0.0,0.72)
    pause_layer.add_child(shade)

    var panel := VBoxContainer.new()
    panel.set_anchors_preset(Control.PRESET_CENTER)
    panel.position = Vector2(-230,-190)
    panel.size = Vector2(460,380)
    panel.alignment = BoxContainer.ALIGNMENT_CENTER
    panel.add_theme_constant_override("separation",14)
    pause_layer.add_child(panel)

    var title := Label.new()
    title.text = "一時停止"
    title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    title.add_theme_font_size_override("font_size",38)
    panel.add_child(title)

    var resume := Button.new()
    resume.text = "ゲームに戻る"
    resume.custom_minimum_size = Vector2(460,72)
    resume.add_theme_font_size_override("font_size",25)
    resume.pressed.connect(_resume_game)
    panel.add_child(resume)

    var stories := Button.new()
    stories.text = "ストーリー選択へ"
    stories.custom_minimum_size = Vector2(460,66)
    stories.add_theme_font_size_override("font_size",23)
    stories.pressed.connect(_pause_to_story_select)
    panel.add_child(stories)

    var title_button := Button.new()
    title_button.text = "タイトルへ戻る"
    title_button.custom_minimum_size = Vector2(460,66)
    title_button.add_theme_font_size_override("font_size",23)
    title_button.pressed.connect(_pause_to_title)
    panel.add_child(title_button)

func _story_select_back() -> void:
    if game_audio != null:
        game_audio.click()
    story_select_layer.visible = false
    title_layer.visible = true

func _show_story_select() -> void:
    if game_audio != null:
        game_audio.click()
    title_layer.visible = false
    story_select_layer.visible = true

func _set_game_ui_visible(visible_now: bool) -> void:
    touch.visible = visible_now
    hud.visible = visible_now
    info_label.visible = visible_now
    objective_label.visible = visible_now
    pause_button.visible = visible_now
    if not visible_now:
        dialogue_label.visible = false

func _teleport_vehicle(target_transform: Transform3D) -> void:
    vehicle.freeze = true
    vehicle.sleeping = true
    vehicle.global_transform = target_transform
    vehicle.linear_velocity = Vector3.ZERO
    vehicle.angular_velocity = Vector3.ZERO
    vehicle.steering_state = 0.0
    vehicle.set_controls(0.0,0.0,1.0,0.0)
    vehicle.last_safe_transform = target_transform
    vehicle._safe_timer = 0.0
    vehicle.reset_physics_interpolation()
    vehicle.sleeping = false
    vehicle.freeze = false

    if chase != null:
        chase.follow_yaw = vehicle.global_rotation.y
        chase.orbit_yaw = 0.0
        var forward := vehicle.global_transform.basis.z.normalized()
        chase.global_position = vehicle.global_position - forward * 6.6 + Vector3.UP * 2.55

func _nearest_road_rescue_transform(from_pos: Vector3) -> Transform3D:
    var main_point := Vector3(0.0, ROAD_RESCUE_Y, clampf(from_pos.z,-68.0,68.0))
    var cross_point := Vector3(clampf(from_pos.x,-24.0,24.0), ROAD_RESCUE_Y, -24.0)

    var main_distance := Vector2(from_pos.x - main_point.x, from_pos.z - main_point.z).length()
    var cross_distance := Vector2(from_pos.x - cross_point.x, from_pos.z - cross_point.z).length()

    if cross_distance + 0.5 < main_distance:
        return Transform3D(Basis(Vector3.UP, PI * 0.5), cross_point)
    return Transform3D(Basis.IDENTITY, main_point)

func _reset_vehicle_to_road() -> void:
    if vehicle == null:
        return
    var target := _nearest_road_rescue_transform(vehicle.global_position)

    # RigidBody transforms changed directly from a touch/input callback can be
    # overwritten by the next physics step on some devices. Hold the body frozen
    # for one physics frame, then wake it on the road and verify the relocation.
    vehicle.freeze = true
    vehicle.sleeping = true
    vehicle.global_transform = target
    vehicle.linear_velocity = Vector3.ZERO
    vehicle.angular_velocity = Vector3.ZERO
    vehicle.steering_state = 0.0
    vehicle.set_controls(0.0,0.0,1.0,0.0)
    vehicle.last_safe_transform = target
    vehicle._safe_timer = 0.0
    vehicle.reset_physics_interpolation()
    if chase != null:
        chase.follow_yaw = vehicle.global_rotation.y
        chase.orbit_yaw = 0.0
        var forward := vehicle.global_transform.basis.z.normalized()
        chase.global_position = vehicle.global_position - forward * 6.6 + Vector3.UP * 2.55

    await get_tree().physics_frame
    vehicle.sleeping = false
    vehicle.freeze = false
    await get_tree().physics_frame

    var horizontal_error := Vector2(
        vehicle.global_position.x - target.origin.x,
        vehicle.global_position.z - target.origin.z
    ).length()
    if horizontal_error > 0.65 or vehicle.global_position.y < 0.30:
        _teleport_vehicle(target)
        await get_tree().physics_frame

    if game_audio != null:
        game_audio.click()
    if game_started:
        _on_dialogue_requested("無線","復帰完了。最寄りの道路へ戻した。")

func _reset_vehicle_for_story() -> void:
    _teleport_vehicle(Transform3D(Basis.IDENTITY, START_POINT))

func _start_selected_episode(index: int) -> void:
    if game_audio != null:
        game_audio.click()
        game_audio.set_ducked(false)
    get_tree().paused = false
    game_paused = false
    game_started = false
    selected_episode = index

    # Keep the vehicle hidden behind the title/story screen while the rigid body
    # and suspension settle on the road. This prevents Android from ever
    # exposing the initial physics/interpolation drop as a visible spawn.
    vehicle.visible = false
    _reset_vehicle_for_story()
    for i in range(START_STABILIZE_FRAMES):
        await get_tree().physics_frame
    vehicle.linear_velocity = Vector3.ZERO
    vehicle.angular_velocity = Vector3.ZERO
    vehicle.last_safe_transform = vehicle.global_transform
    vehicle._safe_timer = 0.0
    vehicle.reset_physics_interpolation()
    if chase != null:
        chase.follow_yaw = vehicle.global_rotation.y
        chase.orbit_yaw = 0.0
        var forward := vehicle.global_transform.basis.z.normalized()
        chase.global_position = vehicle.global_position - forward * 6.6 + Vector3.UP * 2.55
    await get_tree().physics_frame

    vehicle.visible = true
    game_started = true
    _set_game_ui_visible(true)
    if story != null:
        story.start_campaign(index)
    title_layer.visible = false
    story_select_layer.visible = false
    pause_layer.visible = false

func _open_pause() -> void:
    if not game_started or game_paused:
        return
    game_paused = true
    if game_audio != null:
        game_audio.pause_cue()
        game_audio.set_ducked(true)
    pause_layer.visible = true
    touch.visible = false
    pause_button.visible = false
    get_tree().paused = true

func _resume_game() -> void:
    get_tree().paused = false
    game_paused = false
    pause_layer.visible = false
    touch.visible = true
    pause_button.visible = true
    if game_audio != null:
        game_audio.click()
        game_audio.set_ducked(false)

func _leave_game_to_menu() -> void:
    get_tree().paused = false
    game_paused = false
    game_started = false
    vehicle.freeze = true
    vehicle.set_controls(0.0,0.0,1.0,0.0)
    if story != null:
        story.stop_campaign()
    _set_game_ui_visible(false)
    pause_layer.visible = false
    if game_audio != null:
        game_audio.set_ducked(false)

func _pause_to_story_select() -> void:
    _leave_game_to_menu()
    if game_audio != null:
        game_audio.click()
    title_layer.visible = false
    story_select_layer.visible = true

func _pause_to_title() -> void:
    _leave_game_to_menu()
    if game_audio != null:
        game_audio.click()
    story_select_layer.visible = false
    title_layer.visible = true

func _start_game() -> void:
    if game_started:
        return
    _start_selected_episode(0)

func _physics_process(_delta: float) -> void:
    if not game_started:
        vehicle.set_controls(0.0,0.0,1.0,0.0)
        return

    # Falling through the world should never strand the player.
    if vehicle.global_position.y < -3.0:
        _reset_vehicle_to_road()
        return

    var keyboard_steer := float(Input.is_key_pressed(KEY_D) or Input.is_key_pressed(KEY_RIGHT)) - float(Input.is_key_pressed(KEY_A) or Input.is_key_pressed(KEY_LEFT))
    var keyboard_throttle := 1.0 if (Input.is_key_pressed(KEY_W) or Input.is_key_pressed(KEY_UP)) else 0.0
    var keyboard_reverse := 1.0 if (Input.is_key_pressed(KEY_S) or Input.is_key_pressed(KEY_DOWN)) else 0.0
    var keyboard_brake := 1.0 if Input.is_key_pressed(KEY_SPACE) else 0.0

    vehicle.set_controls(
        clamp(touch.steer + keyboard_steer, -1.0, 1.0),
        max(touch.throttle, keyboard_throttle),
        max(touch.brake, keyboard_brake),
        max(touch.reverse, keyboard_reverse)
    )

    var reset_down := Input.is_key_pressed(KEY_R)
    if reset_down and not _reset_key_was_down:
        _reset_vehicle_to_road()
    _reset_key_was_down = reset_down

    var horn_down := Input.is_key_pressed(KEY_H)
    if horn_down and not _horn_key_was_down:
        if story != null:
            story.on_horn()
        if vehicle_audio != null:
            vehicle_audio.trigger_horn()
    _horn_key_was_down = horn_down

    var action_down := Input.is_key_pressed(KEY_E)
    if action_down and not _action_key_was_down and story != null:
        story.on_action()
    _action_key_was_down = action_down

    var gear := "D"
    if vehicle.reverse_input > 0.0:
        gear = "R"
    elif vehicle.brake_input > 0.0:
        gear = "B"
    var distance_text := ""
    if mission_marker != null and mission_marker.visible:
        var delta_to_target := Vector2(
            vehicle.global_position.x - mission_marker.global_position.x,
            vehicle.global_position.z - mission_marker.global_position.z
        )
        distance_text = "   目的地 %.0fm" % delta_to_target.length()
    hud.text = "速度 %3.0f km/h   %s   ステア %+.0f%%%s" % [
        vehicle.get_speed_kmh(), gear, vehicle.steering_state*100.0, distance_text
    ]
func _capture_qa_screenshot() -> void:
    await get_tree().create_timer(2.0).timeout
    var output_path := OS.get_environment("PINE_QA_SCREENSHOT")
    if output_path.is_empty():
        output_path = ProjectSettings.globalize_path("user://pine-creek-qa.png")
    var image := get_viewport().get_texture().get_image()
    var err := image.save_png(output_path)
    if err == OK:
        print("QA_SCREENSHOT=", output_path)
    else:
        push_error("Failed to save QA screenshot: " + str(err))
    get_tree().quit()
