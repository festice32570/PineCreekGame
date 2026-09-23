extends SceneTree

func _init() -> void:
    var packed = load("res://assets/vehicles/pine_creek_pickup.glb")
    if packed == null:
        push_error("MODEL_LOAD_FAILED")
        quit(2)
        return
    var root = packed.instantiate()
    print("ROOT ", root.name, " ", root.get_class())
    _dump(root, 0)
    quit()

func _dump(n: Node, depth: int) -> void:
    var indent = "  ".repeat(depth)
    var extra = ""
    if n is Node3D:
        extra += " pos=" + str(n.position) + " rot=" + str(n.rotation_degrees) + " scale=" + str(n.scale)
    if n is MeshInstance3D and n.mesh:
        extra += " aabb=" + str(n.mesh.get_aabb())
    print(indent + n.name + " [" + n.get_class() + "]" + extra)
    for c in n.get_children():
        _dump(c, depth + 1)
