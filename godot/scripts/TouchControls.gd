class_name PineTouchControls
extends Control

signal camera_dragged(delta: Vector2)
signal reset_requested
signal horn_pressed
signal action_pressed

var steer := 0.0
var throttle := 0.0
var brake := 0.0
var reverse := 0.0

var _touch_actions: Dictionary = {}
var _buttons: Dictionary = {}
var _mouse_action := ""

func _ready() -> void:
    set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    mouse_filter = Control.MOUSE_FILTER_IGNORE
    _create_visual_buttons()
    resized.connect(_layout_buttons)
    _layout_buttons()
    set_process_input(true)

func _create_visual_buttons() -> void:
    var defs := {
        "left":"◀",
        "right":"▶",
        "throttle":"アクセル",
        "brake":"ブレーキ",
        "reverse":"バック",
        "horn":"警笛",
        "action":"アクション",
        "reset":"復帰"
    }
    for action in defs:
        var b := Button.new()
        b.text = defs[action]
        b.name = "Visual_" + action
        b.mouse_filter = Control.MOUSE_FILTER_IGNORE
        b.focus_mode = Control.FOCUS_NONE
        b.add_theme_font_size_override("font_size", 30 if action in ["left","right"] else 24)
        b.add_theme_color_override("font_color", Color(1.0,1.0,1.0,0.96))
        b.add_theme_color_override("font_shadow_color", Color(0,0,0,0.9))
        b.add_theme_constant_override("shadow_offset_x", 2)
        b.add_theme_constant_override("shadow_offset_y", 2)
        var style := StyleBoxFlat.new()
        style.bg_color = Color(0.11,0.13,0.15,0.62)
        style.border_color = Color(1.0,1.0,1.0,0.22)
        style.set_border_width_all(2)
        style.set_corner_radius_all(18)
        b.add_theme_stylebox_override("normal", style)
        b.modulate = Color(1,1,1,0.90)
        add_child(b)
        _buttons[action] = b

func _layout_buttons() -> void:
    var s := size
    if s.x < 10.0:
        s = get_viewport_rect().size
    var pad := 26.0
    var steer_size := Vector2(190, 190)
    _set_button("left", Rect2(Vector2(pad, s.y-pad-steer_size.y), steer_size))
    _set_button("right", Rect2(Vector2(pad+steer_size.x+18, s.y-pad-steer_size.y), steer_size))

    var throttle_size := Vector2(210, 220)
    _set_button("throttle", Rect2(Vector2(s.x-pad-throttle_size.x, s.y-pad-throttle_size.y), throttle_size))
    var side_size := Vector2(174, 104)
    _set_button("brake", Rect2(Vector2(s.x-pad-throttle_size.x-18-side_size.x, s.y-pad-side_size.y), side_size))
    _set_button("reverse", Rect2(Vector2(s.x-pad-throttle_size.x-18-side_size.x, s.y-pad-side_size.y-14-side_size.y), side_size))
    _set_button("horn", Rect2(Vector2(s.x-pad-throttle_size.x-18-side_size.x, s.y-pad-side_size.y-28-side_size.y*2.0), Vector2(174,86)))
    _set_button("action", Rect2(Vector2(s.x-pad-throttle_size.x, s.y-pad-throttle_size.y-14-86), Vector2(210,86)))
    _set_button("reset", Rect2(Vector2(s.x-pad-126, 24), Vector2(126,72)))

func _set_button(action: String, rect: Rect2) -> void:
    var b: Button = _buttons[action]
    b.position = rect.position
    b.size = rect.size

func _input(event: InputEvent) -> void:
    if event is InputEventScreenTouch:
        _handle_touch(event.index, event.position, event.pressed)
        get_viewport().set_input_as_handled()
    elif event is InputEventScreenDrag:
        var action: String = _touch_actions.get(event.index, "")
        if action == "camera":
            camera_dragged.emit(event.relative)
        elif action != "":
            # Dragging a held finger keeps the original control claimed.
            pass
        get_viewport().set_input_as_handled()
    elif event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT:
        if event.pressed:
            _mouse_action = _hit_action(event.position)
            if _mouse_action == "":
                _mouse_action = "camera"
            if _mouse_action == "reset":
                reset_requested.emit()
                _mouse_action = ""
            elif _mouse_action == "horn":
                horn_pressed.emit()
            elif _mouse_action == "action":
                action_pressed.emit()
            _recompute()
        else:
            _mouse_action = ""
            _recompute()
    elif event is InputEventMouseMotion and _mouse_action == "camera" and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
        camera_dragged.emit(event.relative)

func _handle_touch(index: int, pos: Vector2, pressed: bool) -> void:
    if pressed:
        var action := _hit_action(pos)
        if action == "":
            action = "camera"
        if action == "reset":
            reset_requested.emit()
            _touch_actions[index] = ""
        else:
            _touch_actions[index] = action
            if action == "horn":
                horn_pressed.emit()
            elif action == "action":
                action_pressed.emit()
    else:
        _touch_actions.erase(index)
    _recompute()

func _hit_action(pos: Vector2) -> String:
    for action in ["left","right","throttle","brake","reverse","horn","action","reset"]:
        var b: Button = _buttons[action]
        if Rect2(b.position,b.size).grow(14.0).has_point(pos):
            return action
    return ""

func _recompute() -> void:
    var active := []
    for action in _touch_actions.values():
        active.append(action)
    if _mouse_action != "":
        active.append(_mouse_action)
    steer = float(active.count("right") > 0) - float(active.count("left") > 0)
    throttle = 1.0 if active.count("throttle") > 0 else 0.0
    brake = 1.0 if active.count("brake") > 0 else 0.0
    reverse = 1.0 if active.count("reverse") > 0 else 0.0
    _update_visual_feedback(active)

func _update_visual_feedback(active: Array) -> void:
    for action in _buttons:
        var b: Button = _buttons[action]
        if active.count(action) > 0:
            b.modulate = Color(1.0,0.88,0.48,1.0)
            b.scale = Vector2(0.97,0.97)
            b.pivot_offset = b.size * 0.5
        else:
            b.modulate = Color(1,1,1,0.90)
            b.scale = Vector2.ONE

# Test helpers allow automated multi-touch regression tests without Android injection.
func debug_touch_press(index: int, action: String) -> void:
    _touch_actions[index] = action
    _recompute()

func debug_touch_release(index: int) -> void:
    _touch_actions.erase(index)
    _recompute()
