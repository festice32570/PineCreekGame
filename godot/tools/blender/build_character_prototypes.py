import bpy, math, os
from mathutils import Vector

ROOT='/home/festice/ChatGPT-dev/PineCreekGame'
ASSET=os.path.join(ROOT,'godot/assets')
SRC=os.path.join(ROOT,'art-source/characters')
PREVIEW=os.path.join(ASSET,'characters/character_prototypes_v1.png')
os.makedirs(os.path.join(ASSET,'characters'),exist_ok=True)
os.makedirs(os.path.join(ASSET,'portraits'),exist_ok=True)
os.makedirs(SRC,exist_ok=True)

def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    # Generated source files are reproducible; do not accumulate .blend1 backups.
    bpy.context.preferences.filepaths.save_version=0

def mat(name, rgb, rough=.65, metallic=0.0):
    m=bpy.data.materials.new(name)
    m.use_nodes=True
    bsdf=m.node_tree.nodes.get('Principled BSDF')
    bsdf.inputs['Base Color'].default_value=(*rgb,1)
    bsdf.inputs['Roughness'].default_value=rough
    bsdf.inputs['Metallic'].default_value=metallic
    return m

def cube(name, loc, scale, material, bevel=.03, parent=None):
    bpy.ops.mesh.primitive_cube_add(location=loc)
    o=bpy.context.object; o.name=name; o.scale=(scale[0]/2,scale[1]/2,scale[2]/2)
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    if bevel:
        mod=o.modifiers.new('soft','BEVEL'); mod.width=bevel; mod.segments=2
    o.data.materials.append(material)
    if parent: o.parent=parent
    return o

def cyl(name, loc, radius, depth, material, rot=(0,0,0), verts=12, parent=None):
    bpy.ops.mesh.primitive_cylinder_add(vertices=verts,radius=radius,depth=depth,location=loc,rotation=rot)
    o=bpy.context.object; o.name=name; o.data.materials.append(material)
    if parent: o.parent=parent
    return o

def sphere(name, loc, scale, material, subdivisions=2, parent=None):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=subdivisions,radius=1,location=loc)
    o=bpy.context.object; o.name=name; o.scale=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    o.data.materials.append(material)
    if parent: o.parent=parent
    return o

def make_armature(name='PineHumanRig'):
    arm_data=bpy.data.armatures.new(name+'Data')
    arm=bpy.data.objects.new(name,arm_data)
    bpy.context.collection.objects.link(arm)
    bpy.context.view_layer.objects.active=arm
    arm.select_set(True); bpy.ops.object.mode_set(mode='EDIT')
    bones={}
    def add(n,head,tail,parent=None):
        b=arm_data.edit_bones.new(n); b.head=head; b.tail=tail
        if parent: b.parent=bones[parent]
        bones[n]=b
    add('hips',(0,0,0.92),(0,0,1.10))
    add('spine',(0,0,1.08),(0,0,1.42),'hips')
    add('chest',(0,0,1.40),(0,0,1.62),'spine')
    add('neck',(0,0,1.60),(0,0,1.74),'chest')
    add('head',(0,0,1.72),(0,0,2.02),'neck')
    add('upper_arm.L',(-.22,0,1.54),(-.54,0,1.44),'chest')
    add('forearm.L',(-.54,0,1.44),(-.76,0,1.20),'upper_arm.L')
    add('hand.L',(-.76,0,1.20),(-.82,0,1.08),'forearm.L')
    add('upper_arm.R',(.22,0,1.54),(.54,0,1.44),'chest')
    add('forearm.R',(.54,0,1.44),(.76,0,1.20),'upper_arm.R')
    add('hand.R',(.76,0,1.20),(.82,0,1.08),'forearm.R')
    add('thigh.L',(-.13,0,.96),(-.14,0,.56),'hips')
    add('shin.L',(-.14,0,.56),(-.14,0,.18),'thigh.L')
    add('foot.L',(-.14,0,.18),(-.14,-.16,.08),'shin.L')
    add('thigh.R',(.13,0,.96),(.14,0,.56),'hips')
    add('shin.R',(.14,0,.56),(.14,0,.18),'thigh.R')
    add('foot.R',(.14,0,.18),(.14,-.16,.08),'shin.R')
    bpy.ops.object.mode_set(mode='OBJECT')
    arm.show_in_front=True
    return arm

def bone_parent(obj, arm, bone):
    # Preserve the authored world-space pose when binding a rigid low-poly part
    # to a bone. Without this, Blender interprets the existing transform in the
    # bone's local space and the prototype appears exploded in renders/GLB.
    world=obj.matrix_world.copy()
    obj.parent=arm; obj.parent_type='BONE'; obj.parent_bone=bone
    obj.matrix_world=world

def apply_modifiers():
    for o in list(bpy.context.scene.objects):
        if o.type=='MESH':
            bpy.context.view_layer.objects.active=o; o.select_set(True)
            for m in list(o.modifiers):
                try: bpy.ops.object.modifier_apply(modifier=m.name)
                except: pass
            o.select_set(False)

def build_character(kind='protagonist', x=0.0, root_name=None):
    palette={
      'skin': mat(kind+'_skin',(0.64,0.47,0.36) if kind=='protagonist' else (0.72,0.51,0.36),.8),
      'hair': mat(kind+'_hair',(0.025,0.022,0.020) if kind=='protagonist' else (0.12,0.075,0.045),.85),
      'eye': mat(kind+'_eye',(0.012,0.014,0.016),.75),
      'boot': mat(kind+'_boot',(0.035,0.04,0.045),.82),
      'pants': mat(kind+'_pants',(0.055,0.07,0.09) if kind=='protagonist' else (0.16,0.11,0.075),.9),
      'jacket': mat(kind+'_jacket',(0.065,0.095,0.13) if kind=='protagonist' else (0.23,0.12,0.055),.86),
      'shirt': mat(kind+'_shirt',(0.11,0.13,0.15) if kind=='protagonist' else (0.23,0.09,0.055),.88),
      'glove': mat(kind+'_glove',(0.07,0.065,0.06),.9),
      'metal': mat(kind+'_metal',(0.18,0.19,0.20),.45,.35),
    }
    root=bpy.data.objects.new(root_name or kind.title()+'Root',None); bpy.context.collection.objects.link(root)
    arm=make_armature((root_name or kind.title())+'Rig'); arm.parent=root
    if kind=='bob':
        sx=1.17; torso_w=.72; belly=.80; headz=1.83
    else:
        sx=.96; torso_w=.57; belly=.58; headz=1.81
    # torso clothing
    torso=cube(kind+'_Torso',(0,0,1.37),(torso_w,.36,.66),palette['jacket'],.08); bone_parent(torso,arm,'spine')
    waist=cube(kind+'_Waist',(0,0,1.04),(belly,.33,.30),palette['jacket'],.06); bone_parent(waist,arm,'hips')
    # legs
    for side,xx in [('L',-.15*sx),('R',.15*sx)]:
        thigh=cyl(kind+'_Thigh_'+side,(xx,0,.77),.115*sx,.42,palette['pants'],verts=10); bone_parent(thigh,arm,'thigh.'+side)
        shin=cyl(kind+'_Shin_'+side,(xx,0,.37),.105*sx,.39,palette['pants'],verts=10); bone_parent(shin,arm,'shin.'+side)
        boot=cube(kind+'_Boot_'+side,(xx,-.055,.10),(.25*sx,.38,.18),palette['boot'],.045); bone_parent(boot,arm,'foot.'+side)
    # arms
    shoulder=.37 if kind=='bob' else .31
    for side,sgn in [('L',-1),('R',1)]:
        ua=cyl(kind+'_UpperArm_'+side,(sgn*shoulder,0,1.42),.105*sx,.40,palette['jacket'],rot=(0,math.radians(18*sgn),0),verts=10); bone_parent(ua,arm,'upper_arm.'+side)
        fa=cyl(kind+'_ForeArm_'+side,(sgn*(shoulder+.22),0,1.20),.09*sx,.34,palette['jacket'],rot=(0,math.radians(25*sgn),0),verts=10); bone_parent(fa,arm,'forearm.'+side)
        hand=sphere(kind+'_Hand_'+side,(sgn*(shoulder+.34),0,1.04),(.10*sx,.085,.12),palette['glove'],1); bone_parent(hand,arm,'hand.'+side)
    # neck/head
    neck=cyl(kind+'_Neck',(0,0,1.69),.095*sx,.17,palette['skin'],verts=10); bone_parent(neck,arm,'neck')
    head_scale=(.225*sx,.205,.285 if kind=='protagonist' else .27)
    head=sphere(kind+'_Head',(0,0,headz),head_scale,palette['skin'],2); bone_parent(head,arm,'head')
    # ears/nose
    nose=sphere(kind+'_Nose',(0,-.205,headz-.015),(.047,.052,.060),palette['skin'],1); bone_parent(nose,arm,'head')
    # eyes face toward -Y (camera front)
    for side,xx in [('L',-.075*sx),('R',.075*sx)]:
        eye=sphere(kind+'_Eye_'+side,(xx,-.195,headz+.055),(.025,.018,.020),palette['eye'],1); bone_parent(eye,arm,'head')
        brow=cube(kind+'_Brow_'+side,(xx,-.211,headz+.105),(.09,.025,.018),palette['hair'],.004); bone_parent(brow,arm,'head')
    # hair/cap and Bob beard
    if kind=='protagonist':
        hair=sphere(kind+'_Hair',(0,.015,headz+.13),(.23*sx,.205,.16),palette['hair'],2); bone_parent(hair,arm,'head')
        # crop lower back illusion with a small forehead fringe
        fringe=cube(kind+'_Fringe',(0,-.190,headz+.145),(.29,.055,.08),palette['hair'],.025); bone_parent(fringe,arm,'head')
    else:
        beard=sphere(kind+'_Beard',(0,-.035,headz-.105),(.218*sx,.192,.17),palette['hair'],2); bone_parent(beard,arm,'head')
        cap=mat(kind+'_cap',(0.06,0.055,0.045),.92)
        crown=sphere(kind+'_CapCrown',(0,.005,headz+.15),(.235*sx,.21,.12),cap,2); bone_parent(crown,arm,'head')
        brim=cube(kind+'_CapBrim',(0,-.19,headz+.12),(.30,.18,.035),cap,.02); bone_parent(brim,arm,'head')
        # key ring
        ring=cyl(kind+'_KeyRing',(.31,-.04,1.00),.055,.016,palette['metal'],rot=(math.radians(90),0,0),verts=12); bone_parent(ring,arm,'hips')
        for i in range(3):
            key=cube(kind+'_Key_'+str(i),(.30+i*.025,-.055,.94-i*.035),(.025,.09,.018),palette['metal'],.005); bone_parent(key,arm,'hips')
    # Move the completed hierarchy only after bone binding, so preview offsets
    # do not contaminate local bone-space transforms.
    root.location.x=x
    return root,arm

def select_hierarchy(root):
    bpy.ops.object.select_all(action='DESELECT')
    root.select_set(True)
    for o in bpy.context.scene.objects:
        p=o.parent
        while p:
            if p==root: o.select_set(True); break
            p=p.parent
    bpy.context.view_layer.objects.active=root

def tri_count(root):
    n=0
    for o in bpy.context.scene.objects:
        p=o
        owned=False
        while p:
            if p==root: owned=True; break
            p=p.parent
        if o.type=='MESH' and owned:
            o.data.calc_loop_triangles(); n+=len(o.data.loop_triangles)
    return n

def export_character(kind):
    reset()
    root,arm=build_character(kind,0.0,kind.title()+'Root')
    apply_modifiers()
    blend=os.path.join(SRC,kind+'.blend')
    glb=os.path.join(ASSET,'characters',kind+'.glb')
    bpy.ops.wm.save_as_mainfile(filepath=blend)
    select_hierarchy(root)
    bpy.ops.export_scene.gltf(filepath=glb,export_format='GLB',use_selection=True,export_apply=True,export_yup=True,export_animations=False)
    print(kind,'TRIANGLES',tri_count(root),'GLB',glb)

def export_base():
    reset()
    # Neutral protagonist-shaped base proves the shared armature and proportions.
    root,arm=build_character('protagonist',0.0,'PineHumanBaseRoot')
    apply_modifiers()
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(SRC,'pine_human_base.blend'))
    print('base saved')

def look_at(obj,target):
    obj.rotation_euler=(Vector(target)-obj.location).to_track_quat('-Z','Y').to_euler()

def setup_render(scene, transparent=False, portrait=False):
    # VMware's virtual OpenGL caps SSBO bindings below what Eevee Next needs.
    # Use CPU Cycles for deterministic headless previews on the dev VM.
    scene.render.engine='CYCLES'
    scene.cycles.device='CPU'
    scene.cycles.samples=12 if portrait else 16
    scene.render.resolution_x=512 if portrait else 1280
    scene.render.resolution_y=512 if portrait else 720
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format='PNG'
    scene.render.film_transparent=transparent
    if scene.world is None: scene.world=bpy.data.worlds.new('World')
    scene.world.color=(0.035,0.055,0.075)

def add_lights(target=(0,0,1.1)):
    for loc,energy,size in [((-3,-4,6),900,4.0),((4,-2,4),650,3.0),((0,4,5),700,3.5)]:
        bpy.ops.object.light_add(type='AREA',location=loc)
        l=bpy.context.object; l.data.energy=energy; l.data.size=size; look_at(l,target)

def render_portrait(kind):
    reset(); root,arm=build_character(kind,0,kind.title()+'Root'); apply_modifiers()
    # Tight chest-up framing for dialogue UI rather than a full-body thumbnail.
    bpy.ops.object.camera_add(location=(0,-2.65,1.68))
    cam=bpy.context.object; cam.data.lens=72; look_at(cam,(0,0,1.58)); bpy.context.scene.camera=cam
    add_lights((0,0,1.65)); setup_render(bpy.context.scene,True,True)
    path=os.path.join(ASSET,'portraits',kind+'_neutral.png'); bpy.context.scene.render.filepath=path
    bpy.ops.render.render(write_still=True); print('portrait',path)

def render_preview():
    reset()
    snow=mat('Snow',(0.72,0.80,0.84),.92)
    ground=cube('Ground',(0,0,-.04),(9,6,.08),snow,.02)
    p,_=build_character('protagonist',-1.0,'ProtagonistRoot')
    b,_=build_character('bob',1.0,'BobRoot')
    apply_modifiers()
    bpy.ops.object.camera_add(location=(0,-7.2,2.45))
    cam=bpy.context.object; cam.data.lens=58; look_at(cam,(0,0,1.0)); bpy.context.scene.camera=cam
    add_lights((0,0,1.15)); setup_render(bpy.context.scene,False,False)
    bpy.context.scene.render.filepath=PREVIEW; bpy.ops.render.render(write_still=True)
    print('preview',PREVIEW,'protagonist tris',tri_count(p),'bob tris',tri_count(b))

export_base()
export_character('protagonist')
export_character('bob')
render_portrait('protagonist')
render_portrait('bob')
render_preview()
print('CHARACTER_PROTOTYPES=PASS')
