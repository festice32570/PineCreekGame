import bpy, math, os
from mathutils import Vector

ROOT = "/home/festice/ChatGPT-dev/PineCreekGame/godot"
OUT_GLB = os.path.join(ROOT, "assets/vehicles/pine_creek_pickup.glb")
OUT_BLEND = "/home/festice/ChatGPT-dev/PineCreekGame/art-source/vehicles/pine_creek_pickup.blend"
OUT_PREVIEW = os.path.join(ROOT, "assets/vehicles/pine_creek_pickup_preview.png")

bpy.ops.wm.read_factory_settings(use_empty=True)

# ---------- Materials ----------
def mat(name, color, metallic=0.0, roughness=0.45):
    m=bpy.data.materials.new(name)
    m.diffuse_color=(*color,1)
    m.use_nodes=True
    bsdf=m.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value=(*color,1)
    bsdf.inputs["Metallic"].default_value=metallic
    bsdf.inputs["Roughness"].default_value=roughness
    return m

BODY=mat("Body_Red",(0.28,0.025,0.018),0.35,0.28)
BODY_DARK=mat("Body_Rust_Dark",(0.12,0.018,0.012),0.25,0.42)
BLACK=mat("Rubber_Black",(0.015,0.015,0.018),0.0,0.62)
DARK=mat("Chassis_Dark",(0.035,0.04,0.045),0.55,0.42)
CHROME=mat("Chrome",(0.62,0.65,0.68),0.92,0.16)
GLASS=mat("Glass",(0.025,0.055,0.075),0.18,0.08)
AMBER=mat("Amber",(0.9,0.28,0.02),0.2,0.22)
RED=mat("Tail_Red",(0.65,0.01,0.01),0.15,0.18)
WHITE=mat("Lamp",(0.95,0.88,0.62),0.15,0.12)
RIM=mat("Wheel_Rim",(0.20,0.21,0.22),0.78,0.22)

# ---------- Helpers ----------
def box(name, loc, scale, material, bevel=0.06):
    bpy.ops.mesh.primitive_cube_add(location=loc)
    o=bpy.context.object
    o.name=name
    o.scale=(scale[0]/2,scale[1]/2,scale[2]/2)
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    if bevel>0:
        mod=o.modifiers.new("Bevel","BEVEL")
        mod.width=bevel
        mod.segments=3
    o.data.materials.append(material)
    return o

def cyl(name, loc, radius, depth, material, rot=(0,0,0), vertices=24):
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices, radius=radius, depth=depth, location=loc, rotation=rot)
    o=bpy.context.object
    o.name=name
    o.data.materials.append(material)
    bev=o.modifiers.new("EdgeSoft","BEVEL")
    bev.width=0.025
    bev.segments=2
    return o

def parent(child, root):
    child.parent=root

# ---------- Root ----------
root=bpy.data.objects.new("PickupRoot",None)
bpy.context.collection.objects.link(root)

# Dimensions: approx 5.25m long, 2.0m wide, 1.82m tall.
parts=[]
parts += [
    box("Frame", (0,0.10,0.42),(1.48,4.55,0.18),DARK,0.04),
    box("LowerBody",(0,-0.05,0.78),(1.92,4.90,0.52),BODY,0.11),
    box("FrontBumper",(0,-2.52,0.68),(1.92,0.20,0.25),CHROME,0.04),
    box("RearBumper",(0,2.48,0.67),(1.88,0.19,0.24),CHROME,0.04),
    box("Hood",(0,-1.56,1.15),(1.80,1.45,0.27),BODY,0.10),
    box("CabLower",(0,-0.35,1.13),(1.82,1.72,0.64),BODY,0.10),
    box("CabUpper",(0,-0.35,1.60),(1.69,1.34,0.62),BODY,0.11),
    box("Roof",(0,-0.35,1.94),(1.73,1.39,0.16),BODY_DARK,0.08),
    box("BedFloor",(0,1.39,1.02),(1.72,1.88,0.14),BODY,0.04),
    box("BedLeft",(-0.84,1.38,1.28),(0.15,1.95,0.55),BODY,0.06),
    box("BedRight",(0.84,1.38,1.28),(0.15,1.95,0.55),BODY,0.06),
    box("Tailgate",(0,2.30,1.28),(1.62,0.14,0.55),BODY,0.06),
    box("BedFront",(0,0.52,1.28),(1.62,0.14,0.55),BODY,0.05),
    box("Grille",(0,-2.30,1.06),(1.36,0.09,0.38),DARK,0.025),
    box("GrilleChromeTop",(0,-2.36,1.27),(1.55,0.06,0.065),CHROME,0.018),
    box("Windshield",(0,-1.04,1.65),(1.48,0.07,0.48),GLASS,0.025),
    box("RearGlass",(0,0.35,1.63),(1.36,0.06,0.39),GLASS,0.025),
    box("LeftWindow",(-0.856,-0.34,1.62),(0.055,0.83,0.40),GLASS,0.015),
    box("RightWindow",(0.856,-0.34,1.62),(0.055,0.83,0.40),GLASS,0.015),
    box("LeftMirror",(-1.04,-0.72,1.64),(0.24,0.13,0.18),DARK,0.04),
    box("RightMirror",(1.04,-0.72,1.64),(0.24,0.13,0.18),DARK,0.04),
]
# Head lamps and fog lamps.
for x in (-0.64,0.64):
    parts.append(box("Headlamp_L" if x<0 else "Headlamp_R",(x,-2.37,1.10),(0.42,0.07,0.27),WHITE,0.025))
    parts.append(box("Fog_L" if x<0 else "Fog_R",(x,-2.60,0.66),(0.23,0.07,0.13),WHITE,0.02))
# Amber roof markers.
for i,x in enumerate((-0.55,-0.28,0,0.28,0.55)):
    parts.append(box(f"RoofMarker_{i}",(x,-0.36,2.055),(0.11,0.16,0.07),AMBER,0.018))
# Tail lamps.
for x in (-0.77,0.77):
    parts.append(box("TailLamp_L" if x<0 else "TailLamp_R",(x,2.39,1.27),(0.18,0.07,0.36),RED,0.025))

# Running boards.
parts.append(box("Step_L",(-1.02,-0.28,0.76),(0.20,1.70,0.10),CHROME,0.025))
parts.append(box("Step_R",(1.02,-0.28,0.76),(0.20,1.70,0.10),CHROME,0.025))

# Exhaust and hitch.
parts.append(cyl("Exhaust",(0.70,2.36,0.41),0.055,0.56,DARK,rot=(math.radians(90),0,0),vertices=16))
parts.append(box("TowHitch",(0,2.65,0.48),(0.55,0.35,0.12),DARK,0.02))

for p in parts: parent(p,root)

# Wheels: each wheel root remains separately named for Godot animation.
wheel_data=[
    ("Wheel_FL",-0.93,-1.53,0.52),
    ("Wheel_FR", 0.93,-1.53,0.52),
    ("Wheel_RL",-0.93, 1.56,0.52),
    ("Wheel_RR", 0.93, 1.56,0.52),
]
for name,x,y,z in wheel_data:
    wr=bpy.data.objects.new(name,None)
    bpy.context.collection.objects.link(wr)
    wr.location=(x,y,z)
    parent(wr,root)
    tire=cyl(name+"_Tire",(0,0,0),0.46,0.31,BLACK,rot=(0,math.radians(90),0),vertices=32)
    tire.parent=wr
    tire.location=(0,0,0)
    rim=cyl(name+"_Rim",(0,0,0),0.255,0.325,RIM,rot=(0,math.radians(90),0),vertices=24)
    rim.parent=wr
    rim.location=(0,0,0)
    hub=cyl(name+"_Hub",(0,0,0),0.10,0.34,CHROME,rot=(0,math.radians(90),0),vertices=20)
    hub.parent=wr
    hub.location=(0,0,0)

# Small wheel arch trim, visual only.
for x in (-0.96,0.96):
    for y in (-1.53,1.56):
        parts.append(box(f"FenderTrim_{x}_{y}",(x*0.92,y,0.87),(0.09,0.86,0.12),BODY_DARK,0.035))

# Apply modifiers on mesh objects for predictable GLB.
bpy.context.view_layer.objects.active=None
for o in list(bpy.context.scene.objects):
    if o.type=="MESH":
        bpy.context.view_layer.objects.active=o
        o.select_set(True)
        for mod in list(o.modifiers):
            try: bpy.ops.object.modifier_apply(modifier=mod.name)
            except: pass
        o.select_set(False)

# Save source blend.
os.makedirs(os.path.dirname(OUT_BLEND),exist_ok=True)
bpy.ops.wm.save_as_mainfile(filepath=OUT_BLEND)

# Export only vehicle.
bpy.ops.object.select_all(action='DESELECT')
for o in bpy.context.scene.objects:
    if o==root or o.parent==root or (o.parent and o.parent.parent==root):
        o.select_set(True)
bpy.context.view_layer.objects.active=root
bpy.ops.export_scene.gltf(
    filepath=OUT_GLB,
    export_format='GLB',
    use_selection=True,
    export_apply=True,
    export_yup=True,
)

# ---------- Preview render ----------
# Add ground/camera/light only after GLB export.
ground=box("PreviewGround",(0,0,0.03),(12,12,0.08),mat("Snow",(0.82,0.88,0.92),0.0,0.78),0.02)
# Camera looking toward cab/front quarter.
bpy.ops.object.camera_add(location=(7.4,-8.0,4.9))
cam=bpy.context.object
cam.data.lens=52
bpy.context.scene.camera=cam

def look_at(obj, target):
    direction=Vector(target)-obj.location
    obj.rotation_euler=direction.to_track_quat('-Z','Y').to_euler()
look_at(cam,(0,-0.2,1.0))

bpy.ops.object.light_add(type='AREA', location=(2.5,-4.5,7.5))
key=bpy.context.object
key.data.energy=1400
key.data.shape='DISK'
key.data.size=5.0
look_at(key,(0,0,0.8))
bpy.ops.object.light_add(type='AREA', location=(-4.5,2.0,4.0))
fill=bpy.context.object
fill.data.energy=850
fill.data.size=5
look_at(fill,(0,0,1.1))
bpy.ops.object.light_add(type='AREA', location=(0,5,6))
riml=bpy.context.object
riml.data.energy=1000
riml.data.size=4
look_at(riml,(0,0,1.1))

scene=bpy.context.scene
scene.render.engine='CYCLES'
scene.cycles.device='CPU'
scene.cycles.samples=24
scene.render.resolution_x=960
scene.render.resolution_y=540
scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG'
scene.render.filepath=OUT_PREVIEW
if scene.world is None:
    scene.world=bpy.data.worlds.new("PreviewWorld")
scene.world.color=(0.035,0.05,0.09)
scene.render.film_transparent=False
bpy.ops.render.render(write_still=True)
print("WROTE",OUT_BLEND,OUT_GLB,OUT_PREVIEW)
