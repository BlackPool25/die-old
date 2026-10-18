"""
tools/build_character_models.py
Builds production-grade 3D character models for Die Old:
1. Player Youth ("The Acrobat"): Low-poly humanoid rogue with dual daggers, dark leather/linen vestments,
   and glowing cyan runic visor.
2. Biome 1 Automaton ("Foundry Hound"): Low-poly clockwork quadruped with exposed furnace core,
   brass exhaust pistons, and molten optic sensors.

Adheres strictly to:
- docs/GDD/08_art_and_visual_direction.md
- docs/TDD/05_rendering_and_art_pipeline.md
"""

import bpy
import os
import math
from mathutils import Vector, Euler


def scale_action(act, target_frames):
    """Scales an action's keyframes so it spans exactly target_frames (0 to target_frames-1)."""
    orig_start = act.frame_range[0]
    orig_end = act.frame_range[1]
    orig_dur = orig_end - orig_start
    if orig_dur <= 0:
        return
    scale = (target_frames - 1) / orig_dur
    for layer in act.layers:
        for strip in layer.strips:
            for cb in strip.channelbags:
                for fc in cb.fcurves:
                    for kp in fc.keyframe_points:
                        kp.co[0] = (kp.co[0] - orig_start) * scale
                        kp.handle_left[0] = (kp.handle_left[0] - orig_start) * scale
                        kp.handle_right[0] = (kp.handle_right[0] - orig_start) * scale
    act.use_frame_range = True
    act.frame_start = 0
    act.frame_end = target_frames - 1


def scale_act_in_place(act, offset, target_dur):
    orig_start = act.frame_range[0]
    orig_dur = act.frame_range[1] - orig_start
    scale = target_dur / orig_dur if orig_dur > 0 else 1.0
    for l in act.layers:
        for s in l.strips:
            for cb in s.channelbags:
                for fc in cb.fcurves:
                    for kp in fc.keyframe_points:
                        kp.co[0] = offset + (kp.co[0] - orig_start) * scale
                        kp.handle_left[0] = offset + (kp.handle_left[0] - orig_start) * scale
                        kp.handle_right[0] = offset + (kp.handle_right[0] - orig_start) * scale


def merge_action_curves(dest_act, src_act):
    dest_fc_map = {}
    for l in dest_act.layers:
        for s in l.strips:
            for cb in s.channelbags:
                for fc in cb.fcurves:
                    dest_fc_map[(fc.data_path, fc.array_index)] = fc

    for l in src_act.layers:
        for s in l.strips:
            for cb in s.channelbags:
                for fc in cb.fcurves:
                    key = (fc.data_path, fc.array_index)
                    if key in dest_fc_map:
                        target_fc = dest_fc_map[key]
                        for kp in fc.keyframe_points:
                            target_fc.keyframe_points.insert(kp.co[0], kp.co[1])


def build_combo_action(act1, act2, act3, frames_per_hit=4):
    combo = act1.copy()
    combo.name = "attack_light_combo"
    combo.use_fake_user = True
    
    # Hit 1: frames 0 to 4
    scale_act_in_place(combo, 0, frames_per_hit)
    
    # Hit 2: frames 4 to 8
    act2_copy = act2.copy()
    scale_act_in_place(act2_copy, frames_per_hit, frames_per_hit)
    merge_action_curves(combo, act2_copy)
    bpy.data.actions.remove(act2_copy)
    
    # Hit 3: frames 8 to 12
    act3_copy = act3.copy()
    scale_act_in_place(act3_copy, frames_per_hit * 2, frames_per_hit)
    merge_action_curves(combo, act3_copy)
    bpy.data.actions.remove(act3_copy)
    
    total_frames = frames_per_hit * 3 + 1
    combo.use_frame_range = True
    combo.frame_start = 0
    combo.frame_end = total_frames - 1
    return combo


def build_player_youth(output_blend="assets/models/player_youth.blend"):
    print("[BuildModels] Building Player Youth ('The Acrobat')...")
    bpy.ops.wm.read_factory_settings(use_empty=True)
    
    raw_path = "assets/raw_models/Rogue_Hooded.glb"
    bpy.ops.import_scene.gltf(filepath=raw_path)
    
    rig = bpy.data.objects.get("Rig")
    if not rig:
        for o in bpy.data.objects:
            if o.type == 'ARMATURE':
                rig = o
                break
    rig.name = "CharacterRig"
    
    # Remove unwanted objects
    unwanted = ['1H_Crossbow', '2H_Crossbow', 'Throwable', 'Icosphere']
    for name in unwanted:
        obj = bpy.data.objects.get(name)
        if obj:
            bpy.data.objects.remove(obj, do_unlink=True)
            
    # Add Glowing Runic Eye / Visor
    # Create small glowing cyan ocular sphere parented to head bone
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.04, location=(0, -0.18, 1.48))
    rune_eye = bpy.context.active_object
    rune_eye.name = "Rune_Visor"
    rune_eye.parent = rig
    rune_eye.parent_type = 'BONE'
    rune_eye.parent_bone = 'head'
    
    # Material for Runic Visor
    mat_rune = bpy.data.materials.new("Rune_ChronoCyan")
    mat_rune.use_nodes = True
    mat_rune["is_emissive"] = True
    nodes = mat_rune.node_tree.nodes
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    emit = nodes.new("ShaderNodeEmission")
    emit.inputs["Color"].default_value = (0.0, 0.94, 1.0, 1.0) # Runic Cyan #00F0FF
    emit.inputs["Strength"].default_value = 5.0
    mat_rune.node_tree.links.new(emit.outputs["Emission"], out.inputs["Surface"])
    rune_eye.data.materials.append(mat_rune)

    # Actions to produce
    src_idle = bpy.data.actions.get("Idle")
    src_sprint = bpy.data.actions.get("Running_A")
    src_block = bpy.data.actions.get("Block")
    src_hit = bpy.data.actions.get("Hit_A")
    src_death = bpy.data.actions.get("Death_A")
    
    src_hit1 = bpy.data.actions.get("Dualwield_Melee_Attack_Slice")
    src_hit2 = bpy.data.actions.get("Dualwield_Melee_Attack_Chop")
    src_hit3 = bpy.data.actions.get("Dualwield_Melee_Attack_Stab")

    # 1. idle (10 frames)
    act_idle = src_idle.copy()
    act_idle.name = "idle"
    act_idle.use_fake_user = True
    scale_action(act_idle, 10)

    # 2. sprint (8 frames)
    act_sprint = src_sprint.copy()
    act_sprint.name = "sprint"
    act_sprint.use_fake_user = True
    scale_action(act_sprint, 8)

    # 3. deflect_stance (6 frames)
    act_deflect = src_block.copy()
    act_deflect.name = "deflect_stance"
    act_deflect.use_fake_user = True
    scale_action(act_deflect, 6)

    # 4. attack_light_combo (13 frames)
    act_combo = build_combo_action(src_hit1, src_hit2, src_hit3, frames_per_hit=4)
    act_combo.name = "attack_light_combo"
    act_combo.use_fake_user = True

    # 5. hit_react (6 frames)
    act_hit = src_hit.copy()
    act_hit.name = "hit_react"
    act_hit.use_fake_user = True
    scale_action(act_hit, 6)

    # 6. death (10 frames)
    act_death = src_death.copy()
    act_death.name = "death"
    act_death.use_fake_user = True
    scale_action(act_death, 10)

    # Purge all other actions
    final_actions = {"idle", "sprint", "deflect_stance", "attack_light_combo", "hit_react", "death"}
    for act in list(bpy.data.actions):
        if act.name not in final_actions:
            bpy.data.actions.remove(act)

    rig.animation_data.action = act_idle
    bpy.context.scene.frame_set(0)

    os.makedirs(os.path.dirname(output_blend), exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=output_blend)
    print(f"[BuildModels] Player Youth saved to {output_blend} with {len(bpy.data.actions)} production actions.")


def build_foundry_hound(output_blend="assets/models/foundry_hound.blend"):
    print("[BuildModels] Building Biome 1 Automaton ('Foundry Hound')...")
    bpy.ops.wm.read_factory_settings(use_empty=True)

    raw_path = "assets/raw_models/Wolf.glb"
    bpy.ops.import_scene.gltf(filepath=raw_path)

    rig = bpy.data.objects.get("AnimalArmature")
    if not rig:
        for o in bpy.data.objects:
            if o.type == 'ARMATURE':
                rig = o
                break
    rig.name = "CharacterRig"

    # Remove extra empties / icospheres
    for obj in list(bpy.data.objects):
        if obj.type == 'EMPTY' or obj.name == 'Icosphere':
            bpy.data.objects.remove(obj, do_unlink=True)

    # Overhaul materials for clockwork foundry aesthetic
    # 1. Main plates -> Cast Iron / Dark Basalt (#1B1E23)
    mat_main = bpy.data.materials.get("Main")
    if mat_main and mat_main.use_nodes:
        bsdf = next((n for n in mat_main.node_tree.nodes if n.type == 'BSDF_PRINCIPLED'), None)
        if bsdf:
            bsdf.inputs["Base Color"].default_value = (0.07, 0.08, 0.09, 1.0)
            bsdf.inputs["Metallic"].default_value = 0.8
            bsdf.inputs["Roughness"].default_value = 0.45

    # 2. Main_Light -> Weathered Bronze (#5E493E)
    mat_light = bpy.data.materials.get("Main_Light")
    if mat_light and mat_light.use_nodes:
        bsdf = next((n for n in mat_light.node_tree.nodes if n.type == 'BSDF_PRINCIPLED'), None)
        if bsdf:
            bsdf.inputs["Base Color"].default_value = (0.25, 0.18, 0.14, 1.0)
            bsdf.inputs["Metallic"].default_value = 0.7
            bsdf.inputs["Roughness"].default_value = 0.5

    # 3. Nose -> Polished Brass Rivet (#C49A45)
    mat_nose = bpy.data.materials.get("Nose")
    if mat_nose and mat_nose.use_nodes:
        bsdf = next((n for n in mat_nose.node_tree.nodes if n.type == 'BSDF_PRINCIPLED'), None)
        if bsdf:
            bsdf.inputs["Base Color"].default_value = (0.77, 0.60, 0.27, 1.0)
            bsdf.inputs["Metallic"].default_value = 0.9
            bsdf.inputs["Roughness"].default_value = 0.3

    # 4. Eyes -> Molten Amber Optics (#FF6600)
    mat_optics = bpy.data.materials.get("Eyes_Black")
    if mat_optics:
        mat_optics.name = "Optics_Eye"
        mat_optics["is_emissive"] = True
        nodes = mat_optics.node_tree.nodes
        nodes.clear()
        out = nodes.new("ShaderNodeOutputMaterial")
        emit = nodes.new("ShaderNodeEmission")
        emit.inputs["Color"].default_value = (1.0, 0.4, 0.0, 1.0) # #FF6600
        emit.inputs["Strength"].default_value = 6.0
        mat_optics.node_tree.links.new(emit.outputs["Emission"], out.inputs["Surface"])

    # Add Clockwork Components:
    # A. Exposed Furnace Core (Glowing Molten Core inside Torso)
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.12, location=(0.0, 0.05, 0.50))
    core_obj = bpy.context.active_object
    core_obj.name = "Furnace_Core_Mesh"
    core_obj.parent = rig
    core_obj.parent_type = 'BONE'
    core_obj.parent_bone = 'Torso'

    mat_core = bpy.data.materials.new("Furnace_Core")
    mat_core.use_nodes = True
    mat_core["is_emissive"] = True
    c_nodes = mat_core.node_tree.nodes
    c_nodes.clear()
    c_out = c_nodes.new("ShaderNodeOutputMaterial")
    c_emit = c_nodes.new("ShaderNodeEmission")
    c_emit.inputs["Color"].default_value = (1.0, 0.45, 0.0, 1.0) # Molten furnace amber #FF7300
    c_emit.inputs["Strength"].default_value = 8.0
    mat_core.node_tree.links.new(c_emit.outputs["Emission"], c_out.inputs["Surface"])
    core_obj.data.materials.append(mat_core)

    # B. Twin Brass Exhaust Pistons on back/spine
    bpy.ops.mesh.primitive_cylinder_add(radius=0.035, depth=0.22, location=(0.08, -0.05, 0.70))
    piston_r = bpy.context.active_object
    piston_r.name = "Piston_Right"
    piston_r.rotation_euler = (math.radians(-25), 0, math.radians(10))
    piston_r.parent = rig
    piston_r.parent_type = 'BONE'
    piston_r.parent_bone = 'Torso'

    bpy.ops.mesh.primitive_cylinder_add(radius=0.035, depth=0.22, location=(-0.08, -0.05, 0.70))
    piston_l = bpy.context.active_object
    piston_l.name = "Piston_Left"
    piston_l.rotation_euler = (math.radians(-25), 0, math.radians(-10))
    piston_l.parent = rig
    piston_l.parent_type = 'BONE'
    piston_l.parent_bone = 'Torso'

    mat_piston = bpy.data.materials.new("Pistons_Brass")
    mat_piston.use_nodes = True
    p_nodes = mat_piston.node_tree.nodes
    p_nodes.clear()
    p_out = p_nodes.new("ShaderNodeOutputMaterial")
    p_bsdf = p_nodes.new("ShaderNodeBsdfPrincipled")
    p_bsdf.inputs["Base Color"].default_value = (0.77, 0.60, 0.27, 1.0)
    p_bsdf.inputs["Metallic"].default_value = 0.85
    p_bsdf.inputs["Roughness"].default_value = 0.35
    mat_piston.node_tree.links.new(p_bsdf.outputs["BSDF"], p_out.inputs["Surface"])
    piston_r.data.materials.append(mat_piston)
    piston_l.data.materials.append(mat_piston)

    # Actions for Foundry Hound
    src_idle = bpy.data.actions.get("AnimalArmature|Idle")
    src_trot = bpy.data.actions.get("AnimalArmature|Walk")
    src_pounce = bpy.data.actions.get("AnimalArmature|Attack")
    src_hit = bpy.data.actions.get("AnimalArmature|Idle_HitReact_Left")
    src_death = bpy.data.actions.get("AnimalArmature|Death")

    # 1. idle (10 frames)
    act_idle = src_idle.copy()
    act_idle.name = "idle"
    act_idle.use_fake_user = True
    scale_action(act_idle, 10)

    # 2. trot (8 frames)
    act_trot = src_trot.copy()
    act_trot.name = "trot"
    act_trot.use_fake_user = True
    scale_action(act_trot, 8)

    # 3. pounce_telegraph (10 frames)
    act_pounce = src_pounce.copy()
    act_pounce.name = "pounce_telegraph"
    act_pounce.use_fake_user = True
    scale_action(act_pounce, 10)

    # 4. hit_react (6 frames)
    act_hit = src_hit.copy()
    act_hit.name = "hit_react"
    act_hit.use_fake_user = True
    scale_action(act_hit, 6)

    # 5. death (10 frames)
    act_death = src_death.copy()
    act_death.name = "death"
    act_death.use_fake_user = True
    scale_action(act_death, 10)

    final_actions = {"idle", "trot", "pounce_telegraph", "hit_react", "death"}
    for act in list(bpy.data.actions):
        if act.name not in final_actions:
            bpy.data.actions.remove(act)

    rig.animation_data.action = act_idle
    bpy.context.scene.frame_set(0)

    os.makedirs(os.path.dirname(output_blend), exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=output_blend)
    print(f"[BuildModels] Foundry Hound saved to {output_blend} with {len(bpy.data.actions)} production actions.")


def main():
    build_player_youth()
    build_foundry_hound()


if __name__ == "__main__":
    main()
