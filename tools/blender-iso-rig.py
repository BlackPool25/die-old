"""
tools/blender-iso-rig.py
Headless 2.5D Isometric 8-Directional Multi-Pass Baking Rig for Die Old.

Standards Enforced:
- True 2:1 dimetric orthographic projection (Pitch: 60.0°, Yaw: 45.0°).
- 8 Isometric Rotations: [0°, 45°, 90°, 135°, 180°, 225°, 270°, 315°].
- Multi-Pass Output per frame:
  1. Diffuse Pass: Clean cel-shaded colors with crisp silhouettes.
  2. Normal Map Pass: Camera/tangent space normals encoded to RGB (#8080FF flat, Z > 0, Blue > 120).
  3. Emission Pass: Black-and-white mask for glowing runic eyes/runes/furnace cores.
- Uniform Texel Density: Exactly 32 pixels per world meter (96x96 px @ ortho_scale 3.0).
"""

import bpy
import math
import os
import sys
from mathutils import Vector, Euler

# 2:1 Dimetric Camera Configuration:
# Pitch 60.0° (elevation 30.0° above ground), Yaw 45.0°
CAMERA_ROTATIONS = [0, 45, 90, 135, 180, 225, 270, 315]


def setup_iso_camera(scene, resolution=96, ortho_scale=3.0, camera_z_offset=0.9):
    """Sets up the standardized 2:1 dimetric orthographic camera."""
    scene.render.engine = 'BLENDER_EEVEE'
    scene.render.resolution_x = resolution
    scene.render.resolution_y = resolution
    scene.render.film_transparent = True
    scene.render.image_settings.color_mode = 'RGBA'
    scene.render.image_settings.file_format = 'PNG'

    # Remove existing cameras
    for obj in list(scene.collection.objects):
        if obj.type == 'CAMERA':
            bpy.data.objects.remove(obj, do_unlink=True)

    cam_data = bpy.data.cameras.new('IsoCamera')
    cam_data.type = 'ORTHO'
    cam_data.ortho_scale = ortho_scale
    cam_obj = bpy.data.objects.new('IsoCamera', cam_data)
    scene.collection.objects.link(cam_obj)
    scene.camera = cam_obj

    # 60° pitch (30° elevation), 45° yaw
    cam_obj.rotation_euler = (math.radians(60), 0, math.radians(45))
    bpy.context.view_layer.update()

    # Calculate position along backward view vector aiming at (0, 0, camera_z_offset)
    fwd = cam_obj.matrix_world.to_3x3() @ Vector((0, 0, -1))
    cam_obj.location = -fwd * 12.0 + Vector((0, 0, camera_z_offset))
    return cam_obj


def setup_iso_lighting(scene):
    """Sets up a three-point high-contrast cel-shaded lighting rig."""
    for obj in list(scene.collection.objects):
        if obj.type == 'LIGHT':
            bpy.data.objects.remove(obj, do_unlink=True)

    # Key light: Sun light from upper-left (elevation 55 deg, azimuth -45 deg)
    key_data = bpy.data.lights.new('KeySun', 'SUN')
    key_data.energy = 4.0
    key_data.color = (1.0, 0.98, 0.95)
    key_obj = bpy.data.objects.new('KeySun', key_data)
    key_obj.rotation_euler = (math.radians(55), math.radians(15), math.radians(-45))
    scene.collection.objects.link(key_obj)

    # Fill light: Cool ambient sun to prevent pitch-black shadows
    fill_data = bpy.data.lights.new('FillSun', 'SUN')
    fill_data.energy = 2.0
    fill_data.color = (0.75, 0.85, 0.95)
    fill_obj = bpy.data.objects.new('FillSun', fill_data)
    fill_obj.rotation_euler = (math.radians(-30), math.radians(-30), math.radians(135))
    scene.collection.objects.link(fill_obj)


def create_normal_material():
    """Creates normal map shader node tree mapping camera normals to RGB with gamma compensation."""
    mat = bpy.data.materials.new('BakeRig_NormalShader')
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()

    geo = nodes.new('ShaderNodeNewGeometry')
    trans = nodes.new('ShaderNodeVectorTransform')
    trans.vector_type = 'NORMAL'
    trans.convert_from = 'WORLD'
    trans.convert_to = 'CAMERA'

    sep = nodes.new('ShaderNodeSeparateXYZ')
    comb = nodes.new('ShaderNodeCombineXYZ')
    emit = nodes.new('ShaderNodeEmission')
    out = nodes.new('ShaderNodeOutputMaterial')

    mx = nodes.new('ShaderNodeMath')
    mx.operation = 'MULTIPLY_ADD'
    mx.inputs[1].default_value = 0.5
    mx.inputs[2].default_value = 0.5

    my = nodes.new('ShaderNodeMath')
    my.operation = 'MULTIPLY_ADD'
    my.inputs[1].default_value = 0.5
    my.inputs[2].default_value = 0.5

    mz = nodes.new('ShaderNodeMath')
    mz.operation = 'MULTIPLY_ADD'
    mz.inputs[1].default_value = -0.5 # Normal facing camera is negative Z in Blender camera coords
    mz.inputs[2].default_value = 0.5

    gamma = nodes.new('ShaderNodeGamma')
    gamma.inputs['Gamma'].default_value = 2.2 # Linear-to-sRGB compensation for exact RGB normal mapping

    links.new(geo.outputs['Normal'], trans.inputs['Vector'])
    links.new(trans.outputs['Vector'], sep.inputs['Vector'])
    links.new(sep.outputs['X'], mx.inputs[0])
    links.new(sep.outputs['Y'], my.inputs[0])
    links.new(sep.outputs['Z'], mz.inputs[0])

    links.new(mx.outputs['Value'], comb.inputs['X'])
    links.new(my.outputs['Value'], comb.inputs['Y'])
    links.new(mz.outputs['Value'], comb.inputs['Z'])

    links.new(comb.outputs['Vector'], gamma.inputs['Color'])
    links.new(gamma.outputs['Color'], emit.inputs['Color'])
    links.new(emit.outputs['Emission'], out.inputs['Surface'])
    return mat


def create_flat_emission_materials():
    """Creates pure white emission and pure black materials for the emission mask pass."""
    # White emission
    mat_white = bpy.data.materials.new('BakeRig_EmitWhite')
    mat_white.use_nodes = True
    nodes_w = mat_white.node_tree.nodes
    nodes_w.clear()
    out_w = nodes_w.new('ShaderNodeOutputMaterial')
    emit_w = nodes_w.new('ShaderNodeEmission')
    emit_w.inputs['Color'].default_value = (1.0, 1.0, 1.0, 1.0)
    emit_w.inputs['Strength'].default_value = 1.0
    mat_white.node_tree.links.new(emit_w.outputs['Emission'], out_w.inputs['Surface'])

    # Black non-emission
    mat_black = bpy.data.materials.new('BakeRig_EmitBlack')
    mat_black.use_nodes = True
    nodes_b = mat_black.node_tree.nodes
    nodes_b.clear()
    out_b = nodes_b.new('ShaderNodeOutputMaterial')
    emit_b = nodes_b.new('ShaderNodeEmission')
    emit_b.inputs['Color'].default_value = (0.0, 0.0, 0.0, 1.0)
    emit_b.inputs['Strength'].default_value = 0.0
    mat_black.node_tree.links.new(emit_b.outputs['Emission'], out_b.inputs['Surface'])

    return mat_white, mat_black


def bake_character(output_dir, prefix, actions_subset=None):
    """Bakes all actions and 8 isometric directions into diffuse, normal, and emission passes."""
    scene = bpy.context.scene
    rig = bpy.data.objects.get('CharacterRig')
    if not rig:
        for obj in bpy.data.objects:
            if obj.type == 'ARMATURE':
                rig = obj
                break

    if not rig:
        print("[BakeRig] ERROR: No armature CharacterRig found in scene!")
        return

    # Ensure XYZ rotation mode on armature so rotation_euler[2] updates transforms cleanly
    rig.rotation_mode = 'XYZ'

    # Setup Camera and Lighting
    z_center = 0.9 if 'player' in prefix.lower() else 0.45
    setup_iso_camera(scene, resolution=96, ortho_scale=3.0, camera_z_offset=z_center)
    setup_iso_lighting(scene)

    # Shaders
    mat_normal = create_normal_material()
    mat_white, mat_black = create_flat_emission_materials()

    os.makedirs(output_dir, exist_ok=True)

    # Collect actions
    actions = [a for a in bpy.data.actions]
    if actions_subset:
        actions = [a for a in actions if a.name in actions_subset]

    print(f"[BakeRig] Baking character '{prefix}' with {len(actions)} actions across 8 directions...")

    total_baked = 0

    for act in actions:
        rig.animation_data.action = act
        frame_start = int(act.frame_range[0])
        frame_end = int(act.frame_range[1])
        frame_count = frame_end - frame_start + 1

        print(f"  -> Action '{act.name}' ({frame_count} frames)...")

        for dir_idx, angle in enumerate(CAMERA_ROTATIONS):
            # Rotate rig around Z axis
            rig.rotation_euler = (0, 0, math.radians(angle))
            bpy.context.view_layer.update()

            for frame in range(frame_start, frame_end + 1):
                scene.frame_set(frame)
                bpy.context.view_layer.update()
                frame_idx = frame - frame_start

                base_filename = f"{prefix}_{act.name}_dir{dir_idx}_{frame_idx:02d}"

                # 1. PASS 1: DIFFUSE PASS
                bpy.context.view_layer.material_override = None
                scene.render.filepath = os.path.join(output_dir, f"{base_filename}.png")
                bpy.ops.render.render(write_still=True)

                # 2. PASS 2: NORMAL MAP PASS
                bpy.context.view_layer.material_override = mat_normal
                scene.render.filepath = os.path.join(output_dir, f"{base_filename}_n.png")
                bpy.ops.render.render(write_still=True)

                # 3. PASS 3: EMISSION MASK PASS
                bpy.context.view_layer.material_override = None
                # Temporarily swap materials across ALL mesh objects
                saved_materials = {}
                for obj in bpy.data.objects:
                    if obj.type == 'MESH':
                        saved_materials[obj.name] = []
                        for slot in obj.material_slots:
                            orig_mat = slot.material
                            saved_materials[obj.name].append(orig_mat)
                            is_emit = False
                            if orig_mat:
                                is_emit = (
                                    orig_mat.get('is_emissive', False)
                                    or ('Rune' in orig_mat.name)
                                    or ('Optics' in orig_mat.name)
                                    or ('Furnace' in orig_mat.name)
                                    or ('Core' in orig_mat.name)
                                )
                            slot.material = mat_white if is_emit else mat_black

                scene.render.filepath = os.path.join(output_dir, f"{base_filename}_e.png")
                bpy.ops.render.render(write_still=True)

                # Restore original materials
                for obj_name, mats in saved_materials.items():
                    obj = bpy.data.objects.get(obj_name)
                    if obj:
                        for i, orig_mat in enumerate(mats):
                            obj.material_slots[i].material = orig_mat

                total_baked += 3

    # Reset rig rotation
    rig.rotation_euler = (0, 0, 0)
    bpy.context.view_layer.material_override = None
    print(f"[BakeRig] Successfully baked {total_baked} total pass frames for '{prefix}' to {output_dir}")


def main():
    argv = sys.argv
    cli_args = []
    if "--" in argv:
        cli_args = argv[argv.index("--") + 1:]

    output_dir = "assets/sprites_raw/characters"
    prefix = None
    bake_all = False

    i = 0
    while i < len(cli_args):
        arg = cli_args[i]
        if arg in ('-o', '--output-dir') and i + 1 < len(cli_args):
            output_dir = cli_args[i + 1]
            i += 1
        elif arg in ('-p', '--prefix') and i + 1 < len(cli_args):
            prefix = cli_args[i + 1]
            i += 1
        elif arg == '--all':
            bake_all = True
        i += 1

    if bake_all:
        models = [
            ("assets/models/player_youth.blend", "player_youth"),
            ("assets/models/foundry_hound.blend", "foundry_hound")
        ]
        for blend_path, model_prefix in models:
            abs_blend = os.path.abspath(blend_path)
            if not os.path.exists(abs_blend):
                print(f"[BakeRig] Model not found: {abs_blend}. Running build_character_models first...")
                import subprocess
                subprocess.run(['blender', '-b', '-P', 'tools/build_character_models.py'], check=True)

            bpy.ops.wm.open_mainfile(filepath=abs_blend)
            bake_character(output_dir, model_prefix)
    else:
        if not prefix:
            filepath = bpy.data.filepath
            if filepath:
                prefix = os.path.splitext(os.path.basename(filepath))[0]
            else:
                prefix = "character"
        bake_character(output_dir, prefix)


if __name__ == '__main__':
    main()
