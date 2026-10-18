"""
tools/generate_tileset.py
Generates the Biome 1 (The Obsidian Foundry) modular 2:1 dimetric tileset:
- Strict adherence to the 32-color Obsidian Foundry palette.
- 64x32 Floor tiles (Basalt flagstones, obsidian borders, molten slag vents, runes).
- 64x64 & 64x96 Wall and Pillar tiles (Cast-iron modular walls, basalt columns).
- Matching mathematically accurate tangent-space normal maps (flat #8080FF, Z > 0, Blue > 120).
- Tiled .tsx tileset with Box2D collision polygons and hazard metadata.
"""

import os
import math
import numpy as np
from PIL import Image
from palette import OBSIDIAN_FOUNDRY_PALETTE_RGB, get_closest_palette_color, hex_to_rgb

TILE_W = 64
TILE_H = 32

# Palette shorthand colors
C_DARK_VOID = hex_to_rgb("#0B0C0E")
C_BASALT_DEEP = hex_to_rgb("#121417")
C_BASALT_DARK = hex_to_rgb("#181B20")
C_BASALT_BASE = hex_to_rgb("#1B1E23")
C_BASALT_MID = hex_to_rgb("#282D35")
C_BASALT_LIGHT = hex_to_rgb("#363C46")
C_BASALT_HIGHLIGHT = hex_to_rgb("#454D59")

C_IRON_SHADOW = hex_to_rgb("#201A17")
C_IRON_DARK = hex_to_rgb("#2E2520")
C_IRON_BASE = hex_to_rgb("#3B312B")
C_IRON_MID = hex_to_rgb("#4D3E36")
C_BRONZE_BASE = hex_to_rgb("#5E493E")
C_BRONZE_LIGHT = hex_to_rgb("#8B6D5A")
C_BRONZE_HIGHLIGHT = hex_to_rgb("#A5836D")

C_LAVA_DEEP = hex_to_rgb("#8C2200")
C_LAVA_CORE = hex_to_rgb("#C43B00")
C_LAVA_BRIGHT = hex_to_rgb("#FF6600")
C_LAVA_HOT = hex_to_rgb("#FF8800")
C_AMBER_GOLD = hex_to_rgb("#FFAA00")
C_SPARK_YELLOW = hex_to_rgb("#FFE57F")
C_SPARK_WHITE = hex_to_rgb("#FFFFFF")

C_SOOT_DARK = hex_to_rgb("#2D3A4A")
C_SOOT_MID = hex_to_rgb("#3A4B5F")
C_SOOT_LIGHT = hex_to_rgb("#4A607A")


def create_blank_tile(width=64, height=32):
    return np.zeros((height, width, 4), dtype=np.uint8)


def create_blank_normal(width=64, height=32):
    """Initializes normal map to flat #8080FF (128, 128, 255, 0)."""
    norm = np.zeros((height, width, 4), dtype=np.uint8)
    norm[:, :, 0] = 128
    norm[:, :, 1] = 128
    norm[:, :, 2] = 255
    norm[:, :, 3] = 0 # Transparent by default
    return norm


def inside_diamond(x, y, cx=31.5, cy=15.5, rx=31.5, ry=15.5):
    """Tests if point (x, y) is inside 2:1 dimetric diamond."""
    return (abs(x - cx) / rx + abs(y - cy) / ry) <= 1.0


def quantize_to_palette(img_arr):
    """Enforces strict palette adherence for all non-transparent pixels."""
    h, w, c = img_arr.shape
    for y in range(h):
        for x in range(w):
            if img_arr[y, x, 3] > 0:
                orig_rgb = tuple(img_arr[y, x, :3])
                img_arr[y, x, :3] = get_closest_palette_color(orig_rgb)
    return img_arr


# ==============================================================================
# FLOOR TILES (64x32)
# ==============================================================================

def generate_basalt_flagstone_01():
    """Tile 1: Clean beveled basalt flagstone with mortar grid."""
    diff = create_blank_tile(64, 32)
    norm = create_blank_normal(64, 32)

    for y in range(32):
        for x in range(64):
            if not inside_diamond(x, y):
                continue

            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            # Distance from center
            d = abs(x - 31.5) / 31.5 + abs(y - 15.5) / 15.5

            # Bevel edges
            if d > 0.90:
                diff[y, x, :3] = C_BASALT_DEEP
                # Slanted normal at border
                nx = (x - 31.5) / 31.5 * 0.4
                ny = -(y - 15.5) / 15.5 * 0.4
                nz = math.sqrt(max(0.1, 1.0 - nx*nx - ny*ny))
                norm[y, x, 0] = int(nx * 127 + 128)
                norm[y, x, 1] = int(ny * 127 + 128)
                norm[y, x, 2] = int(nz * 127 + 128)
            elif d > 0.82:
                diff[y, x, :3] = C_BASALT_DARK
            else:
                # Sub-divided stone flagstones pattern
                grid_u = (x + y * 2) % 16
                grid_v = (x - y * 2) % 16
                if grid_u < 1 or grid_v < 1:
                    # Mortar seam
                    diff[y, x, :3] = C_BASALT_DEEP
                    norm[y, x, :3] = (128, 128, 200)
                elif grid_u == 1 or grid_v == 1:
                    # Chiseled highlight
                    diff[y, x, :3] = C_BASALT_LIGHT
                    norm[y, x, :3] = (140, 140, 240)
                else:
                    # Basalt surface
                    if (x * 7 + y * 13) % 9 == 0:
                        diff[y, x, :3] = C_BASALT_HIGHLIGHT
                    elif (x * 11 + y * 5) % 7 == 0:
                        diff[y, x, :3] = C_BASALT_MID
                    else:
                        diff[y, x, :3] = C_BASALT_BASE
                    norm[y, x, :3] = (128, 128, 255)

    return quantize_to_palette(diff), norm


def generate_basalt_flagstone_02():
    """Tile 2: Weathered basalt with deep fracture."""
    diff, norm = generate_basalt_flagstone_01()

    # Etch a diagonal fracture
    for t in range(25):
        fx = int(20 + t * 0.8)
        fy = int(8 + t * 0.6)
        if 0 <= fx < 64 and 0 <= fy < 32 and diff[fy, fx, 3] > 0:
            diff[fy, fx, :3] = C_DARK_VOID
            norm[fy, fx, :3] = (110, 110, 180)
            if fy + 1 < 32 and diff[fy+1, fx, 3] > 0:
                diff[fy+1, fx, :3] = C_BASALT_LIGHT
                norm[fy+1, fx, :3] = (145, 145, 235)

    return quantize_to_palette(diff), norm


def generate_obsidian_border():
    """Tile 3: Reinforced floor with cast iron border trim and brass rivets."""
    diff = create_blank_tile(64, 32)
    norm = create_blank_normal(64, 32)

    for y in range(32):
        for x in range(64):
            if not inside_diamond(x, y):
                continue

            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            d = abs(x - 31.5) / 31.5 + abs(y - 15.5) / 15.5

            if d > 0.65:
                # Heavy cast iron boundary trim
                diff[y, x, :3] = C_IRON_BASE
                norm[y, x, :3] = (128, 128, 250)
                if d > 0.90:
                    diff[y, x, :3] = C_IRON_SHADOW
                    norm[y, x, :3] = (128, 110, 210)
                elif d > 0.85:
                    diff[y, x, :3] = C_BRONZE_LIGHT
                    norm[y, x, :3] = (128, 145, 240)
            else:
                # Inner polished obsidian field
                if (x + y) % 6 == 0:
                    diff[y, x, :3] = C_BASALT_DEEP
                elif (x * 3 + y * 7) % 8 == 0:
                    diff[y, x, :3] = C_SOOT_DARK
                else:
                    diff[y, x, :3] = C_BASALT_DARK
                norm[y, x, :3] = (128, 128, 255)

    # Rivets at corners
    rivet_pos = [(16, 8), (48, 8), (16, 24), (48, 24), (32, 4), (32, 28)]
    for rx, ry in rivet_pos:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                px, py = rx + dx, ry + dy
                if 0 <= px < 64 and 0 <= py < 32 and diff[py, px, 3] > 0:
                    dist_sq = dx*dx + dy*dy
                    if dist_sq <= 1:
                        diff[py, px, :3] = C_AMBER_GOLD
                        nx = dx * 0.45
                        ny = -dy * 0.45
                        nz = math.sqrt(max(0.1, 1.0 - nx*nx - ny*ny))
                        norm[py, px, 0] = int(nx * 127 + 128)
                        norm[py, px, 1] = int(ny * 127 + 128)
                        norm[py, px, 2] = int(nz * 127 + 128)

    return quantize_to_palette(diff), norm


def generate_molten_heat_vent():
    """Tile 4: Cast-iron grating floor tile over glowing molten slag."""
    diff = create_blank_tile(64, 32)
    norm = create_blank_normal(64, 32)

    for y in range(32):
        for x in range(64):
            if not inside_diamond(x, y):
                continue

            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            d = abs(x - 31.5) / 31.5 + abs(y - 15.5) / 15.5

            if d > 0.80:
                # Solid iron perimeter border
                diff[y, x, :3] = C_IRON_BASE
                norm[y, x, :3] = (128, 128, 240)
            else:
                # Slotted heat vent grate
                slot_u = (x + y * 2) % 6
                if slot_u in (0, 1):
                    # Iron grate bar
                    diff[y, x, :3] = C_IRON_MID
                    norm[y, x, :3] = (128, 140, 240)
                else:
                    # Glowing molten slag beneath
                    heat = math.sin(x * 0.2) * math.cos(y * 0.3)
                    if heat > 0.4:
                        diff[y, x, :3] = C_SPARK_YELLOW
                    elif heat > -0.1:
                        diff[y, x, :3] = C_LAVA_BRIGHT
                    elif heat > -0.5:
                        diff[y, x, :3] = C_LAVA_CORE
                    else:
                        diff[y, x, :3] = C_LAVA_DEEP
                    norm[y, x, :3] = (128, 128, 160)

    return quantize_to_palette(diff), norm


def generate_rune_circle():
    """Tile 5: Alchemical chrono rune engraved in basalt."""
    diff, norm = generate_basalt_flagstone_01()

    # Engrave glowing concentric rune rings
    for y in range(32):
        for x in range(64):
            if diff[y, x, 3] == 0:
                continue
            # Isometric radial coordinate
            dx = (x - 31.5)
            dy = (y - 15.5) * 2.0
            r = math.sqrt(dx*dx + dy*dy)

            if abs(r - 18.0) < 1.4 or abs(r - 9.0) < 1.2:
                # Glowing rune etch
                diff[y, x, :3] = C_AMBER_GOLD
                norm[y, x, :3] = (128, 128, 220)
            elif abs(r - 18.0) < 2.2 or abs(r - 9.0) < 2.0:
                diff[y, x, :3] = C_LAVA_BRIGHT
                norm[y, x, :3] = (128, 128, 235)

    return quantize_to_palette(diff), norm


# ==============================================================================
# WALL TILES (64x64 & 64x96)
# ==============================================================================

WALL_SLOPE = 16.0 / 27.712812  # Exact 2.5D dimetric projection slope: ~0.577350288


def generate_wall_cast_iron_nw():
    """Wall 1 (64x64): Cast-iron modular wall facing North-West (along world X axis, slope +16/27.71)."""
    diff = create_blank_tile(64, 64)
    norm = create_blank_normal(64, 64)

    # North-West wall slopes up-right on screen (ScreenY increases with X).
    # In PIL image coordinates (where y=0 is top): floor_y decreases as X increases.
    for x in range(64):
        floor_y = int(48 - (x - 31.5) * WALL_SLOPE)
        front_top_y = floor_y - 28
        cap_back_y = max(0, front_top_y - 8)

        # 1. Top Horizontal Cap (depth 8px viewed from above)
        for y in range(cap_back_y, front_top_y):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            norm[y, x, 0] = 128
            norm[y, x, 1] = 220
            norm[y, x, 2] = 220

            if y == cap_back_y:
                diff[y, x, :3] = C_BASALT_DARK
            elif y == front_top_y - 1:
                diff[y, x, :3] = C_BRONZE_HIGHLIGHT
            else:
                diff[y, x, :3] = C_BASALT_MID if (x + y) % 3 == 0 else C_BASALT_BASE

        # 2. Front Vertical Face (Ashlar Iron Plating)
        for y in range(front_top_y, min(64, floor_y + 1)):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            rel_y = y - front_top_y

            nx = -0.42
            ny = 0.25
            nz = math.sqrt(max(0.1, 1.0 - nx*nx - ny*ny))
            norm[y, x, 0] = int(nx * 127 + 128)
            norm[y, x, 1] = int(ny * 127 + 128)
            norm[y, x, 2] = int(nz * 127 + 128)

            if rel_y == 0:
                diff[y, x, :3] = C_DARK_VOID
                norm[y, x, 1] = 100
            elif rel_y in (8, 17):
                diff[y, x, :3] = C_IRON_SHADOW
                norm[y, x, 1] = int((ny - 0.35) * 127 + 128)
            elif rel_y in (9, 18):
                diff[y, x, :3] = C_BRONZE_BASE
                norm[y, x, 1] = int((ny + 0.35) * 127 + 128)
            elif rel_y == 25:
                diff[y, x, :3] = C_BRONZE_LIGHT
                norm[y, x, 1] = int((ny + 0.40) * 127 + 128)
            elif rel_y in (26, 27):
                diff[y, x, :3] = C_IRON_DARK
                norm[y, x, 1] = 120
            elif rel_y >= 28:
                diff[y, x, :3] = C_DARK_VOID
                norm[y, x, 1] = 80
            else:
                diff[y, x, :3] = C_IRON_MID if rel_y % 3 == 0 else C_IRON_BASE

    # Domed Rivets along horizontal seams
    rivet_x_positions = [10, 24, 38, 52]
    for rx in rivet_x_positions:
        front_top_at_rx = int(48 - (rx - 31.5) * WALL_SLOPE) - 28
        for seam_rel_y in (8, 17):
            ry = front_top_at_rx + seam_rel_y
            for dy in range(-1, 2):
                for dx in range(-1, 2):
                    px, py = rx + dx, ry + dy
                    if 0 <= px < 64 and 0 <= py < 64 and diff[py, px, 3] > 0:
                        dist_sq = dx*dx + dy*dy
                        if dist_sq <= 1:
                            diff[py, px, :3] = C_AMBER_GOLD if dist_sq == 0 else C_BRONZE_HIGHLIGHT
                            norm[py, px, 0] = int((dx * 0.4) * 127 + 128)
                            norm[py, px, 1] = int((-dy * 0.4) * 127 + 128)
                            norm[py, px, 2] = 230

    return quantize_to_palette(diff), norm


def generate_wall_cast_iron_ne():
    """Wall 2 (64x64): Cast-iron modular wall facing North-East (along world Y axis, slope -16/27.71)."""
    diff = create_blank_tile(64, 64)
    norm = create_blank_normal(64, 64)

    # North-East wall slopes down-right on screen: floor_y increases as X increases.
    for x in range(64):
        floor_y = int(48 + (x - 31.5) * WALL_SLOPE)
        front_top_y = floor_y - 28
        cap_back_y = max(0, front_top_y - 8)

        # 1. Top Horizontal Cap
        for y in range(cap_back_y, front_top_y):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            norm[y, x, 0] = 128
            norm[y, x, 1] = 220
            norm[y, x, 2] = 220

            if y == cap_back_y:
                diff[y, x, :3] = C_BASALT_DARK
            elif y == front_top_y - 1:
                diff[y, x, :3] = C_BRONZE_HIGHLIGHT
            else:
                diff[y, x, :3] = C_BASALT_MID if (x + y) % 3 == 0 else C_BASALT_BASE

        # 2. Front Vertical Face
        for y in range(front_top_y, min(64, floor_y + 1)):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            rel_y = y - front_top_y

            nx = 0.42
            ny = 0.25
            nz = math.sqrt(max(0.1, 1.0 - nx*nx - ny*ny))
            norm[y, x, 0] = int(nx * 127 + 128)
            norm[y, x, 1] = int(ny * 127 + 128)
            norm[y, x, 2] = int(nz * 127 + 128)

            if rel_y == 0:
                diff[y, x, :3] = C_DARK_VOID
                norm[y, x, 1] = 100
            elif rel_y in (8, 17):
                diff[y, x, :3] = C_IRON_SHADOW
                norm[y, x, 1] = int((ny - 0.35) * 127 + 128)
            elif rel_y in (9, 18):
                diff[y, x, :3] = C_BRONZE_BASE
                norm[y, x, 1] = int((ny + 0.35) * 127 + 128)
            elif rel_y == 25:
                diff[y, x, :3] = C_BRONZE_LIGHT
                norm[y, x, 1] = int((ny + 0.40) * 127 + 128)
            elif rel_y in (26, 27):
                diff[y, x, :3] = C_IRON_DARK
                norm[y, x, 1] = 120
            elif rel_y >= 28:
                diff[y, x, :3] = C_DARK_VOID
                norm[y, x, 1] = 80
            else:
                diff[y, x, :3] = C_IRON_MID if rel_y % 3 == 0 else C_IRON_BASE

    # Domed Rivets along horizontal seams
    rivet_x_positions = [10, 24, 38, 52]
    for rx in rivet_x_positions:
        front_top_at_rx = int(48 + (rx - 31.5) * WALL_SLOPE) - 28
        for seam_rel_y in (8, 17):
            ry = front_top_at_rx + seam_rel_y
            for dy in range(-1, 2):
                for dx in range(-1, 2):
                    px, py = rx + dx, ry + dy
                    if 0 <= px < 64 and 0 <= py < 64 and diff[py, px, 3] > 0:
                        dist_sq = dx*dx + dy*dy
                        if dist_sq <= 1:
                            diff[py, px, :3] = C_AMBER_GOLD if dist_sq == 0 else C_BRONZE_HIGHLIGHT
                            norm[py, px, 0] = int((dx * 0.4) * 127 + 128)
                            norm[py, px, 1] = int((-dy * 0.4) * 127 + 128)
                            norm[py, px, 2] = 230

    return quantize_to_palette(diff), norm


def generate_wall_corner_outer():
    """Wall 3 (64x64): Outer corner junction with reinforced monolithic corner pylon."""
    diff_nw, norm_nw = generate_wall_cast_iron_nw()
    diff_ne, norm_ne = generate_wall_cast_iron_ne()

    diff = create_blank_tile(64, 64)
    norm = create_blank_normal(64, 64)

    # Left half (x < 32) is NW wall, Right half (x >= 32) is NE wall
    diff[:, :32] = diff_nw[:, :32]
    norm[:, :32] = norm_nw[:, :32]
    diff[:, 32:] = diff_ne[:, 32:]
    norm[:, 32:] = norm_ne[:, 32:]

    # Reinforced monolithic bronze corner buttress / pylon at apex (x in 29..34)
    for y in range(16, 52):
        for cx in range(29, 35):
            if diff_nw[y, cx, 3] > 0 or diff_ne[y, cx, 3] > 0 or y in range(20, 48):
                diff[y, cx, 3] = 255
                norm[y, cx, 3] = 255

                dx = (cx - 31.5) / 3.0
                norm[y, cx, 0] = int(dx * 60 + 128)
                norm[y, cx, 1] = 140
                norm[y, cx, 2] = 240

                if cx in (31, 32):
                    diff[y, cx, :3] = C_BRONZE_HIGHLIGHT if y % 4 == 0 else C_AMBER_GOLD
                elif cx in (30, 33):
                    diff[y, cx, :3] = C_BRONZE_LIGHT
                else:
                    diff[y, cx, :3] = C_BRONZE_BASE

                if y % 8 == 4 and cx in (30, 31, 32, 33):
                    diff[y, cx, :3] = C_SPARK_YELLOW if cx in (31, 32) else C_AMBER_GOLD

    return quantize_to_palette(diff), norm


def generate_pillar_column():
    """Pillar (64x96): Basalt cylindrical pillar with bronze collar bands."""
    diff = create_blank_tile(64, 96)
    norm = create_blank_normal(64, 96)

    col_cx = 31.5
    col_rad = 14.0

    for y in range(12, 92):
        for x in range(16, 48):
            dx = (x - col_cx) / col_rad
            if abs(dx) <= 1.0:
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255

                nx = dx * 0.70
                ny = 0.15
                nz = math.sqrt(max(0.1, 1.0 - nx*nx - ny*ny))
                norm[y, x, 0] = int(nx * 127 + 128)
                norm[y, x, 1] = int(ny * 127 + 128)
                norm[y, x, 2] = int(nz * 127 + 128)

                if y in range(12, 18) or y in range(48, 52) or y in range(86, 92):
                    if dx < -0.3:
                        diff[y, x, :3] = C_BRONZE_BASE
                    elif dx < 0.3:
                        diff[y, x, :3] = C_AMBER_GOLD
                    else:
                        diff[y, x, :3] = C_BRONZE_LIGHT
                else:
                    if dx < -0.4:
                        diff[y, x, :3] = C_BASALT_DEEP
                    elif dx < 0.2:
                        diff[y, x, :3] = C_BASALT_BASE
                    elif dx < 0.6:
                        diff[y, x, :3] = C_BASALT_MID
                    else:
                        diff[y, x, :3] = C_BASALT_LIGHT

    for y in range(78, 96):
        for x in range(64):
            if inside_diamond(x, y - 64, cx=31.5, cy=23.5, rx=24.0, ry=12.0):
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255
                diff[y, x, :3] = C_IRON_BASE
                norm[y, x, :3] = (128, 128, 240)

    return quantize_to_palette(diff), norm


def generate_wall_furnace_vent():
    """Wall 4 (64x64): Cast-iron wall with authentic arched industrial blast furnace embrasure."""
    diff, norm = generate_wall_cast_iron_nw()

    # Arched blast furnace embrasure centered at cx=31.5 on the NW wall face
    for x in range(22, 42):
        front_top_x = int(48 - (x - 31.5) * WALL_SLOPE) - 28
        for y in range(front_top_x + 3, front_top_x + 22):
            if not (0 <= y < 64 and diff[y, x, 3] > 0):
                continue

            rel_y = y - front_top_x
            dx = (x - 31.5) / 8.5
            dy = (rel_y - 12.0) / 8.0
            dist_sq = dx*dx + dy*dy

            if dist_sq < 0.65:
                heat = math.cos(dx * 1.5) * math.cos(dy * 1.5)
                if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                    diff[y, x, :3] = C_IRON_SHADOW
                    norm[y, x, :3] = (128, 128, 210)
                elif heat > 0.6:
                    diff[y, x, :3] = C_SPARK_YELLOW
                    norm[y, x, :3] = (128, 128, 255)
                elif heat > 0.3:
                    diff[y, x, :3] = C_LAVA_BRIGHT
                    norm[y, x, :3] = (128, 128, 230)
                else:
                    diff[y, x, :3] = C_LAVA_CORE
                    norm[y, x, :3] = (128, 128, 190)
            elif dist_sq <= 1.05:
                if dy < -0.3:
                    diff[y, x, :3] = C_BRONZE_HIGHLIGHT
                    norm[y, x, 1] = 200
                elif dist_sq > 0.88:
                    diff[y, x, :3] = C_BRONZE_LIGHT
                    norm[y, x, :3] = (int(dx * 40 + 128), int(dy * 40 + 128), 230)
                else:
                    diff[y, x, :3] = C_IRON_SHADOW
                    norm[y, x, :3] = (128, 128, 200)

    return quantize_to_palette(diff), norm


# ==============================================================================
# MAIN TILESET COMPILER & TILED .TSX EXPORT
# ==============================================================================

def main():
    raw_dir = "assets/sprites_raw/tilesets"
    tileset_dir = "assets/tilesets"
    os.makedirs(raw_dir, exist_ok=True)
    os.makedirs(tileset_dir, exist_ok=True)

    print("Generating Obsidian Foundry modular tileset...")

    # Registered tile definitions: (name, generator_fn, width, height)
    tiles = [
        ("floor_basalt_flagstone_01", generate_basalt_flagstone_01, 64, 32),
        ("floor_basalt_flagstone_02", generate_basalt_flagstone_02, 64, 32),
        ("floor_obsidian_border", generate_obsidian_border, 64, 32),
        ("floor_molten_heat_vent", generate_molten_heat_vent, 64, 32),
        ("floor_rune_circle", generate_rune_circle, 64, 32),
        ("wall_cast_iron_nw", generate_wall_cast_iron_nw, 64, 64),
        ("wall_cast_iron_ne", generate_wall_cast_iron_ne, 64, 64),
        ("wall_corner_outer", generate_wall_corner_outer, 64, 64),
        ("wall_furnace_vent", generate_wall_furnace_vent, 64, 64),
        ("pillar_basalt_column", generate_pillar_column, 64, 96),
    ]

    # Save individual raw frames for TexturePacker
    for name, gen_fn, w, h in tiles:
        diff, norm = gen_fn()
        Image.fromarray(diff).save(os.path.join(raw_dir, f"{name}.png"))
        Image.fromarray(norm).save(os.path.join(raw_dir, f"{name}_n.png"))
        print(f"  -> Generated {name} ({w}x{h})")

    # Assemble master tileset texture sheet (256x256 power of two)
    sheet_w = 256
    sheet_h = 256
    sheet_diff = np.zeros((sheet_h, sheet_w, 4), dtype=np.uint8)
    sheet_norm = np.zeros((sheet_h, sheet_w, 4), dtype=np.uint8)
    sheet_norm[:, :, 0] = 128
    sheet_norm[:, :, 1] = 128
    sheet_norm[:, :, 2] = 255

    # Layout coordinates on sheet
    # Row 0: 4 floor tiles (64x32) at y=0, x=0, 64, 128, 192
    # Row 1: 1 floor tile at (0, 32), 3 walls (64x64) at (64, 32), (128, 32), (192, 32)
    # Row 2: 1 wall (64x64) at (0, 96), 1 pillar (64x96) at (64, 96)
    layout = [
        (0, 0),    # flagstone 1
        (64, 0),   # flagstone 2
        (128, 0),  # obsidian border
        (192, 0),  # molten vent
        (0, 32),   # rune circle
        (64, 32),  # wall nw
        (128, 32), # wall ne
        (192, 32), # wall corner
        (0, 96),   # wall furnace
        (64, 96),  # pillar
    ]

    for (name, gen_fn, w, h), (lx, ly) in zip(tiles, layout):
        diff, norm = gen_fn()
        sheet_diff[ly:ly+h, lx:lx+w] = diff
        sheet_norm[ly:ly+h, lx:lx+w] = norm

    sheet_diff_path = os.path.join(tileset_dir, "obsidian_foundry.png")
    sheet_norm_path = os.path.join(tileset_dir, "obsidian_foundry_n.png")
    Image.fromarray(sheet_diff).save(sheet_diff_path)
    Image.fromarray(sheet_norm).save(sheet_norm_path)
    print(f"Master tileset sheet saved to {sheet_diff_path} and {sheet_norm_path}")

    # Generate Tiled .tsx XML definition with Box2D collision polygons
    tsx_content = f"""<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.11.0" name="obsidian_foundry" tilewidth="64" tileheight="96" tilecount="10" columns="4">
  <image source="obsidian_foundry.png" width="{sheet_w}" height="{sheet_h}"/>
  
  <!-- Tile 0: Basalt Flagstone 01 -->
  <tile id="0">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="material" value="basalt"/>
    </properties>
  </tile>

  <!-- Tile 1: Basalt Flagstone 02 -->
  <tile id="1">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="material" value="basalt"/>
    </properties>
  </tile>

  <!-- Tile 2: Obsidian Border -->
  <tile id="2">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="material" value="obsidian_iron"/>
    </properties>
  </tile>

  <!-- Tile 3: Molten Heat Vent (Hazard) -->
  <tile id="3">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="hazard" type="bool" value="true"/>
      <property name="hazard_type" value="molten_slag"/>
      <property name="damage" type="int" value="15"/>
      <property name="tick_interval" type="float" value="0.5"/>
    </properties>
  </tile>

  <!-- Tile 4: Rune Circle -->
  <tile id="4">
    <properties>
      <property name="walkable" type="bool" value="true"/>
      <property name="chrono_rune" type="bool" value="true"/>
    </properties>
  </tile>

  <!-- Tile 5: Cast Iron Wall NW (Box2D Collider) -->
  <tile id="5">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="occludes_vision" type="bool" value="true"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <!-- Box2D Diamond Footprint Collider -->
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 6: Cast Iron Wall NE (Box2D Collider) -->
  <tile id="6">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="occludes_vision" type="bool" value="true"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 7: Wall Corner Outer (Box2D Collider) -->
  <tile id="7">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="occludes_vision" type="bool" value="true"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 8: Wall Furnace Vent (Box2D Collider & Heat Source) -->
  <tile id="8">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="light_emitter" type="bool" value="true"/>
      <property name="light_color" value="#FF6600"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <object id="1" x="0" y="32">
        <polygon points="0,16 32,0 64,16 32,32"/>
      </object>
    </objectgroup>
  </tile>

  <!-- Tile 9: Basalt Pillar (Box2D Circular/Octagonal Base Collider) -->
  <tile id="9">
    <properties>
      <property name="walkable" type="bool" value="false"/>
      <property name="pillar" type="bool" value="true"/>
      <property name="y_sort_offset" type="int" value="16"/>
    </properties>
    <objectgroup draworder="index" id="2">
      <!-- 2.5D Base Collider at Pillar Feet -->
      <object id="1" x="8" y="72">
        <polygon points="12,0 36,0 48,12 36,24 12,24 0,12"/>
      </object>
    </objectgroup>
  </tile>
</tileset>
"""
    tsx_path = os.path.join(tileset_dir, "obsidian_foundry.tsx")
    with open(tsx_path, "w", encoding="utf-8") as f:
        f.write(tsx_content)
    print(f"Tiled TSX definition with Box2D collision metadata saved to {tsx_path}")

    # Generate props and pack atlas
    import generate_foundry_props
    import pack_tileset_atlas
    generate_foundry_props.main()
    pack_tileset_atlas.main()


if __name__ == "__main__":
    main()

