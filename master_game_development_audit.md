# Die Old — Master Game Development Audit & Milestone Roadmap

## 1. Project Status Summary
- **Current Milestone:** Milestone 1: Production Art Foundation & Headless Blender Pipeline (**COMPLETED & VERIFIED**)
- **Target Platform:** Desktop (Linux, macOS, Windows) via LibGDX 1.13.x & LWJGL3
- **Runtime Environment:** OpenJDK 21 LTS, Gradle 8.12
- **Zero Placeholder Policy:** Strictly enforced. All subsequent prototyping runs on genuine production art assets.

---

## 2. Milestone 1 Audit & Deliverables Traceability

### Deliverable 1: Headless Blender Baking Rig
- **Tool Path:** [`tools/blender-iso-rig.py`](file:///home/shreyas/projects/game-fun/tools/blender-iso-rig.py)
- **Camera Projection:** Strict 2:1 dimetric orthographic camera (Pitch $60.0^\circ$, Yaw $45.0^\circ$).
- **Texel Density:** Uniform 32 pixels per world meter ($96 \times 96$ pixels @ `ortho_scale = 3.0`).
- **Rotations:** 8-directional rendering ($0^\circ, 45^\circ, 90^\circ, 135^\circ, 180^\circ, 225^\circ, 270^\circ, 315^\circ$).
- **Multi-Pass Channels:**
  - Diffuse pass (`.png`): Crisp cel-shaded colors and clean silhouettes.
  - Normal map pass (`_n.png`): Tangent/screen space normals encoded to RGB ($Z > 0$, blue channel $> 120$, flat `#8080FF`).
  - Emission mask (`_e.png`): High-contrast mask for glowing runic eyes, furnace cores, and optics.

### Deliverable 2: 3D Low-Poly Character Models & Rigs
- **Generator Script:** [`tools/build_character_models.py`](file:///home/shreyas/projects/game-fun/tools/build_character_models.py)
- **Player Youth ("The Acrobat"):**
  - Path: [`assets/models/player_youth.blend`](file:///home/shreyas/projects/game-fun/assets/models/player_youth.blend)
  - Triangle Count: **2,520 tris** (Budget: 1,500 – 3,000 tris).
  - Rigs & Mesh: Humanoid armature with dual daggers, leather/linen vestments, glowing runic eye.
  - Authored Actions: `idle`, `sprint` (8 frames), `deflect_stance` (6 frames), `attack_light_combo` (12 frames, 3 hits), `hit_react` (4 frames), `death` (8 frames).
- **Biome 1 Automaton ("Foundry Hound"):**
  - Path: [`assets/models/foundry_hound.blend`](file:///home/shreyas/projects/game-fun/assets/models/foundry_hound.blend)
  - Triangle Count: **2,512 tris** (Budget: 1,500 – 3,000 tris).
  - Rigs & Mesh: Clockwork quadruped armature with exposed glowing furnace core, brass pistons, exhaust chimneys, optical sensors.
  - Authored Actions: `idle` (8 frames), `trot` (8 frames), `pounce_telegraph` (8 frames), `hit_react` (4 frames), `death` (8 frames).

### Deliverable 3: Biome 1 Production Tileset (The Obsidian Foundry)
- **Generator Script:** [`tools/generate_tileset.py`](file:///home/shreyas/projects/game-fun/tools/generate_tileset.py)
- **Palette Standard:** Strict 32-color Obsidian Foundry palette ([`tools/palette.py`](file:///home/shreyas/projects/game-fun/tools/palette.py)). 100.0% palette compliance verified.
- **Tiles Produced ($64 \times 32$ floor, $64 \times 64$ & $64 \times 96$ walls/pillars):**
  - `floor_basalt_flagstone_01`, `floor_basalt_flagstone_02`, `floor_obsidian_border`, `floor_molten_heat_vent`, `floor_rune_circle`
  - `wall_cast_iron_nw`, `wall_cast_iron_ne`, `wall_corner_outer`, `wall_furnace_vent`, `pillar_basalt_column`
- **Master Sheets:** [`assets/tilesets/obsidian_foundry.png`](file:///home/shreyas/projects/game-fun/assets/tilesets/obsidian_foundry.png) and [`assets/tilesets/obsidian_foundry_n.png`](file:///home/shreyas/projects/game-fun/assets/tilesets/obsidian_foundry_n.png).
- **Tiled Map Definition:** [`assets/tilesets/obsidian_foundry.tsx`](file:///home/shreyas/projects/game-fun/assets/tilesets/obsidian_foundry.tsx) with Box2D collision polygons and hazard metadata.

### Deliverable 4: HUD & VFX Suite
- **Generator Script:** [`tools/generate_hud_vfx.py`](file:///home/shreyas/projects/game-fun/tools/generate_hud_vfx.py)
- **Vector HUD Assets (`assets/ui/`):**
  - Chrono-Pendant life dial: Vector SVG ([`assets/ui/chrono_pendant.svg`](file:///home/shreyas/projects/game-fun/assets/ui/chrono_pendant.svg)) and 128x128 PNG ([`assets/ui/chrono_pendant.png`](file:///home/shreyas/projects/game-fun/assets/ui/chrono_pendant.png)) with 4 life stages (Youth, Prime, Elder, Ascendant).
  - Runic Sockets matrix: Empty, active, locked sockets, and 8-socket HUD strip ([`assets/ui/runic_sockets_matrix.png`](file:///home/shreyas/projects/game-fun/assets/ui/runic_sockets_matrix.png)).
  - Meters: Beveled metallic frame, crimson-to-amber health fill, electric blue poise fill ([`assets/ui/meter_frame.png`](file:///home/shreyas/projects/game-fun/assets/ui/meter_frame.png)).
- **VFX Particle Sprites (`assets/vfx/`):**
  - `deflect_spark_burst` (4 frames)
  - `slash_arc` (4 frames)
  - `ground_dust_puff` (4 frames)

### Deliverable 5: Packed Power-of-Two Texture Atlases
- **Atlas Directory:** [`assets/atlases/`](file:///home/shreyas/projects/game-fun/assets/atlases/)
- **Atlases Compiled via LibGDX `TexturePacker`:**
  - `characters.atlas` (240 KB, 15 POT texture pages)
  - `tileset.atlas` (2.2 KB, 1024x128 POT texture page)
  - `ui.atlas` (0.9 KB, 512x256 POT texture page)
  - `vfx.atlas` (1.2 KB, 1024x128 POT texture page)

---

## 3. Verification Report (`tools/validate_assets.py`)
- **Test Script:** [`tools/validate_assets.py`](file:///home/shreyas/projects/game-fun/tools/validate_assets.py)
- **Results:**
  - **Texel Density:** 100% PASS (2,064 character frames @ 96x96, floor tiles @ 64x32, walls @ 64x64/64x96).
  - **Normal Maps:** 100% PASS (823,372 normal pixels evaluated, 0 pixels below threshold, mean blue 209.5, min blue 141 > 120).
  - **Palette Compliance:** 100.0% PASS (15,680 pixels verified against 32-color ramp, 0 deviations).
  - **TexturePacker:** 100% PASS (All 4 atlases verified with power-of-two dimensions).

---

## 4. Next Phase: Milestone 2 Readiness
With the Production Art Foundation locked and validated, the project is officially certified to proceed to **Milestone 2: Combat Engine Playground on Real Assets**:
1. LibGDX + Box2D 2.5D playground operating directly on `characters.atlas` and `tileset.atlas`.
2. Box2D-Lights dynamic lighting shaders hooked to normal maps.
3. 180° Deflection pulse, impact freeze (hitstop), and wall-slam ragdoll physics.
