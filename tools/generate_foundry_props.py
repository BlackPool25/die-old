"""
tools/generate_foundry_props.py
Generates thematic props and micro-architecture floor tiles for Biome 1 (The Obsidian Foundry):
- floor_magma_fissure (64x32)
- floor_iron_grate (64x32)
- prop_forge_anvil (64x64)
- prop_slag_cauldron (64x64)
- prop_chrono_chest (64x64)
- prop_chain_spool (64x64)
Generates matching diffuse (*.png) and normal (*_n.png) maps.
All pixels strictly adhere to the 32-color Obsidian Foundry palette.
"""

import os
import math
import numpy as np
from PIL import Image

import sys
sys.path.append(os.path.dirname(__file__))
from palette import OBSIDIAN_FOUNDRY_PALETTE_RGB, get_closest_palette_color, hex_to_rgb

# Color shorthands from authorized palette
C_DARK_VOID = hex_to_rgb("#0B0C0E")
C_BASALT_DEEP = hex_to_rgb("#121417")
C_BASALT_DARK = hex_to_rgb("#181B20")
C_BASALT_BASE = hex_to_rgb("#1B1E23")
C_BASALT_MID = hex_to_rgb("#282D35")
C_BASALT_LIGHT = hex_to_rgb("#363C46")
C_BASALT_HIGHLIGHT = hex_to_rgb("#454D59")
C_SLATE_LIGHT = hex_to_rgb("#57606E")

C_IRON_SHADOW = hex_to_rgb("#201A17")
C_IRON_DARK = hex_to_rgb("#2E2520")
C_IRON_BASE = hex_to_rgb("#3B312B")
C_IRON_MID = hex_to_rgb("#4D3E36")
C_BRONZE_BASE = hex_to_rgb("#5E493E")
C_BRONZE_MID = hex_to_rgb("#735B4D")
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
C_CYAN_ACCENT = hex_to_rgb("#627D9E")


def create_blank(width, height):
    return np.zeros((height, width, 4), dtype=np.uint8)


def create_blank_normal(width, height):
    norm = np.zeros((height, width, 4), dtype=np.uint8)
    norm[:, :, 0] = 128
    norm[:, :, 1] = 128
    norm[:, :, 2] = 255
    norm[:, :, 3] = 0
    return norm


def inside_diamond(x, y, cx=31.5, cy=15.5, rx=31.5, ry=15.5):
    return (abs(x - cx) / rx + abs(y - cy) / ry) <= 1.0


def quantize_to_palette(img_arr):
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

def generate_floor_magma_fissure():
    """64x32: Fractured basalt flagstone split open by incandescent molten magma."""
    diff = create_blank(64, 32)
    norm = create_blank_normal(64, 32)

    for y in range(32):
        for x in range(64):
            if not inside_diamond(x, y):
                continue

            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            d = abs(x - 31.5) / 31.5 + abs(y - 15.5) / 15.5

            # Distance to a jagged fissure path: from (12, 6) to (52, 26) with branch to (40, 6)
            # Parametric center line: y_line = 6 + (x - 12) * 0.5 + sin(x*0.4)*2.5
            y_fissure1 = 6.0 + (x - 12.0) * 0.5 + math.sin(x * 0.35) * 2.8
            dist_fissure1 = abs(y - y_fissure1)

            # Secondary tributary
            y_fissure2 = 18.0 - (x - 20.0) * 0.4 + math.cos(x * 0.4) * 2.0
            dist_fissure2 = abs(y - y_fissure2) if 20 <= x <= 45 else 99.0

            dist_fissure = min(dist_fissure1, dist_fissure2)

            if dist_fissure < 1.2:
                # White-hot magma core
                diff[y, x, :3] = C_SPARK_YELLOW
                norm[y, x, :3] = (128, 128, 255)
            elif dist_fissure < 2.5:
                # Amber-orange incandescent slag
                diff[y, x, :3] = C_AMBER_GOLD
                norm[y, x, :3] = (128, 128, 240)
            elif dist_fissure < 3.8:
                # Bright orange-red magma
                diff[y, x, :3] = C_LAVA_BRIGHT
                norm[y, x, :3] = (128, 128, 220)
            elif dist_fissure < 5.2:
                # Deep molten core & slag crust
                diff[y, x, :3] = C_LAVA_CORE
                norm[y, x, :3] = (128, 128, 200)
            elif dist_fissure < 6.8:
                # Scorched black basalt lip
                diff[y, x, :3] = C_BASALT_DEEP
                # Slanted normal at fissure edge
                ny = math.copysign(0.4, y - y_fissure1)
                norm[y, x, 1] = int(ny * 127 + 128)
                norm[y, x, 2] = 230
            elif d > 0.88:
                # Outer flagstone bevel
                diff[y, x, :3] = C_BASALT_DARK
                norm[y, x, :3] = (128, 128, 230)
            else:
                # Chilled basalt slab surface
                if (x + y * 2) % 7 == 0:
                    diff[y, x, :3] = C_BASALT_MID
                elif (x * 3 + y * 5) % 11 == 0:
                    diff[y, x, :3] = C_BASALT_LIGHT
                else:
                    diff[y, x, :3] = C_BASALT_BASE
                norm[y, x, :3] = (128, 128, 255)

    return quantize_to_palette(diff), norm


def generate_floor_iron_grate():
    """64x32: Cast-iron industrial mesh grating tile with bronze border rivets."""
    diff = create_blank(64, 32)
    norm = create_blank_normal(64, 32)

    for y in range(32):
        for x in range(64):
            if not inside_diamond(x, y):
                continue

            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            d = abs(x - 31.5) / 31.5 + abs(y - 15.5) / 15.5

            if d > 0.78:
                # Heavy iron border
                diff[y, x, :3] = C_IRON_BASE
                norm[y, x, :3] = (128, 128, 245)
                if d > 0.90:
                    diff[y, x, :3] = C_IRON_SHADOW
                    norm[y, x, :3] = (128, 110, 215)
                elif d > 0.84:
                    diff[y, x, :3] = C_BRONZE_LIGHT
                    norm[y, x, :3] = (128, 140, 240)
            else:
                # Criss-cross diamond mesh grate
                mesh_u = (x + y * 2) % 8
                mesh_v = (x - y * 2) % 8

                if mesh_u in (0, 1) or mesh_v in (0, 1):
                    # Iron slat bar
                    if mesh_u == 1 or mesh_v == 1:
                        diff[y, x, :3] = C_BRONZE_LIGHT # Top specular ridge
                        norm[y, x, :3] = (140, 140, 240)
                    else:
                        diff[y, x, :3] = C_IRON_MID
                        norm[y, x, :3] = (120, 120, 240)
                else:
                    # Mesh opening showing dark exhaust pit with amber embers
                    if (x * 7 + y * 13) % 19 == 0:
                        diff[y, x, :3] = C_LAVA_BRIGHT # Stray ember
                    elif (x * 3 + y * 5) % 11 == 0:
                        diff[y, x, :3] = C_LAVA_DEEP
                    else:
                        diff[y, x, :3] = C_DARK_VOID
                    norm[y, x, :3] = (128, 128, 200)

    # Rivets along border
    rivets = [(20, 7), (44, 7), (20, 25), (44, 25), (32, 3), (32, 29)]
    for rx, ry in rivets:
        for dy in range(-1, 2):
            for dx in range(-1, 2):
                px, py = rx + dx, ry + dy
                if 0 <= px < 64 and 0 <= py < 32 and diff[py, px, 3] > 0:
                    if dx*dx + dy*dy <= 1:
                        diff[py, px, :3] = C_AMBER_GOLD
                        norm[py, px, :3] = (128 + dx * 20, 128 - dy * 20, 230)

    return quantize_to_palette(diff), norm


# ==============================================================================
# PROPS & OBSTACLES (64x64)
# ==============================================================================

def generate_prop_forge_anvil():
    """64x64: Heavy cast-iron master anvil mounted on a beveled basalt plinth."""
    diff = create_blank(64, 64)
    norm = create_blank_normal(64, 64)

    # 1. Beveled Basalt Stone Plinth at Base (y: 44 to 62)
    for y in range(44, 64):
        for x in range(64):
            # Base diamond
            if inside_diamond(x, y - 48, cx=31.5, cy=7.5, rx=24.0, ry=8.0):
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255
                d = abs(x - 31.5) / 24.0 + abs(y - 55.5) / 8.0

                if y >= 60:
                    diff[y, x, :3] = C_BASALT_DEEP
                    norm[y, x, :3] = (128, 110, 200)
                elif y == 44 or y == 45:
                    # Bronze binding band at top of plinth
                    diff[y, x, :3] = C_AMBER_GOLD if x % 6 == 0 else C_BRONZE_LIGHT
                    norm[y, x, :3] = (128, 145, 235)
                elif d > 0.8:
                    diff[y, x, :3] = C_BASALT_DARK
                    norm[y, x, :3] = (128, 128, 220)
                else:
                    diff[y, x, :3] = C_BASALT_BASE
                    norm[y, x, :3] = (128, 128, 255)

    # 2. Anvil Waist / Stem (y: 32 to 44, x: 24 to 40)
    for y in range(32, 45):
        # Flaring width
        hw = int(6 + (y - 32) * 0.5)
        for x in range(32 - hw, 32 + hw):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255
            nx = (x - 32) / float(hw) * 0.4
            diff[y, x, :3] = C_IRON_MID if x < 32 else C_IRON_BASE
            norm[y, x, 0] = int(nx * 127 + 128)
            norm[y, x, 1] = 128
            norm[y, x, 2] = int(math.sqrt(max(0.1, 1.0 - nx*nx)) * 127 + 128)

    # 3. Anvil Top Body & Horn (y: 18 to 32)
    # Left Horn: x from 10 to 24 (tapering to point at x=10, y=24)
    # Center Face: x from 24 to 48 (flat face on top, y=18..22)
    # Right Heel: x from 48 to 54 (square heel, y=18..25)
    for y in range(16, 33):
        for x in range(8, 56):
            in_anvil = False

            # Horn section (x: 10 to 24)
            if 10 <= x < 24:
                horn_top = 22 - int((x - 10) * 0.3)
                horn_bot = 24 + int((x - 10) * 0.5)
                if horn_top <= y <= horn_bot:
                    in_anvil = True
            # Main body & heel (x: 24 to 54)
            elif 24 <= x <= 53:
                top_edge = 18 if x <= 48 else (18 + int((x - 48) * 0.4))
                bot_edge = 31
                if top_edge <= y <= bot_edge:
                    in_anvil = True

            if in_anvil:
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255

                # Top face highlight (strike table)
                if y in (18, 19) and 24 <= x <= 48:
                    # Chiseled strike face with hardy hole at x=44
                    if x in (43, 44) and y == 19:
                        diff[y, x, :3] = C_DARK_VOID # Hardy hole
                        norm[y, x, :3] = (128, 128, 180)
                    else:
                        diff[y, x, :3] = C_BASALT_HIGHLIGHT
                        norm[y, x, 1] = 210 # Facing up
                elif y == 20 and 24 <= x <= 48:
                    diff[y, x, :3] = C_BRONZE_LIGHT
                    norm[y, x, 1] = 170
                elif x < 24:
                    # Conical horn shading
                    if y == horn_top:
                        diff[y, x, :3] = C_BASALT_HIGHLIGHT
                    elif y > horn_bot - 2:
                        diff[y, x, :3] = C_IRON_SHADOW
                    else:
                        diff[y, x, :3] = C_IRON_MID
                    norm[y, x, 0] = int(-0.5 * 127 + 128)
                    norm[y, x, 1] = int(0.2 * 127 + 128)
                    norm[y, x, 2] = 220
                elif x >= 48:
                    # Square heel end
                    diff[y, x, :3] = C_IRON_DARK
                    norm[y, x, 0] = int(0.5 * 127 + 128)
                    norm[y, x, 2] = 220
                else:
                    # Front face of anvil body
                    if (x + y * 3) % 8 == 0:
                        diff[y, x, :3] = C_BRONZE_BASE
                    elif x < 34:
                        diff[y, x, :3] = C_IRON_MID
                    else:
                        diff[y, x, :3] = C_IRON_BASE
                    norm[y, x, :3] = (128, 128, 255)

    return quantize_to_palette(diff), norm


def generate_prop_slag_cauldron():
    """64x64: Industrial cast-iron smelting crucible bubbling with incandescent slag."""
    diff = create_blank(64, 64)
    norm = create_blank_normal(64, 64)

    # 1. Tripod Iron Claw Base (y: 48 to 62)
    feet = [(18, 58), (32, 60), (46, 58)]
    for fx, fy in feet:
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                px, py = fx + dx, fy + dy
                if 0 <= px < 64 and 0 <= py < 64 and dx*dx + dy*dy <= 9:
                    diff[py, px, 3] = 255
                    norm[py, px, 3] = 255
                    diff[py, px, :3] = C_IRON_SHADOW
                    norm[py, px, :3] = (128, 110, 210)

    # 2. Spherical Crucible Bowl (y: 26 to 52, x: 14 to 50)
    cx, cy = 32.0, 38.0
    rx, ry = 18.0, 14.0

    for y in range(22, 54):
        for x in range(12, 52):
            dx = (x - cx) / rx
            dy = (y - cy) / ry
            dist_sq = dx*dx + dy*dy

            if dist_sq <= 1.0:
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255

                # Normal map on sphere
                nx = dx * 0.6
                ny = -dy * 0.6
                nz = math.sqrt(max(0.1, 1.0 - nx*nx - ny*ny))
                norm[y, x, 0] = int(nx * 127 + 128)
                norm[y, x, 1] = int(ny * 127 + 128)
                norm[y, x, 2] = int(nz * 127 + 128)

                # Shading bands
                if y > 46:
                    diff[y, x, :3] = C_IRON_SHADOW
                elif y in (36, 37):
                    # Bronze reinforcement band
                    diff[y, x, :3] = C_BRONZE_LIGHT if x % 5 == 0 else C_BRONZE_BASE
                elif dx < -0.3:
                    diff[y, x, :3] = C_IRON_MID
                elif dx > 0.3:
                    diff[y, x, :3] = C_IRON_DARK
                else:
                    diff[y, x, :3] = C_IRON_BASE

    # 3. Cauldron Lip and Molten Slag Interior (y: 22 to 34, x: 15 to 49)
    # Elliptical opening: cx=32, cy=27, rx=16, ry=6
    for y in range(21, 35):
        for x in range(15, 49):
            edx = (x - 32.0) / 16.0
            edy = (y - 27.0) / 6.0
            edist_sq = edx*edx + edy*edy

            if edist_sq <= 1.0:
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255

                if edist_sq > 0.72:
                    # Heavy iron rim
                    diff[y, x, :3] = C_BRONZE_LIGHT
                    norm[y, x, :3] = (128, 170, 240)
                else:
                    # Molten glowing slag with bubbles
                    heat = math.sin(x * 0.4) * math.cos(y * 0.6)
                    # Central bright bubble
                    if (x - 32)**2 + (y - 27)**2 <= 4:
                        diff[y, x, :3] = C_SPARK_WHITE
                        norm[y, x, :3] = (128, 128, 255)
                    elif heat > 0.3:
                        diff[y, x, :3] = C_SPARK_YELLOW
                        norm[y, x, :3] = (128, 128, 240)
                    elif heat > -0.2:
                        diff[y, x, :3] = C_AMBER_GOLD
                        norm[y, x, :3] = (128, 128, 220)
                    else:
                        diff[y, x, :3] = C_LAVA_BRIGHT
                        norm[y, x, :3] = (128, 128, 200)

    # 4. Spilling molten slag drip on front lip (y: 30 to 42, x: 30 to 34)
    for y in range(28, 42):
        for x in range(30, 34):
            if diff[y, x, 3] > 0:
                diff[y, x, :3] = C_SPARK_YELLOW if y < 35 else C_LAVA_BRIGHT
                norm[y, x, :3] = (128, 128, 240)

    return quantize_to_palette(diff), norm


def generate_prop_chrono_chest():
    """64x64: Reinforced obsidian and alchemical brass treasure trove with cyan rune dial."""
    diff = create_blank(64, 64)
    norm = create_blank_normal(64, 64)

    # 1. Base Skids / Feet (y: 52 to 60)
    for y in range(52, 60):
        for x in (18, 19, 20, 44, 45, 46):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255
            diff[y, x, :3] = C_BASALT_DEEP
            norm[y, x, :3] = (128, 110, 200)

    # 2. Chest Lower Body (y: 36 to 54, x: 16 to 48)
    for y in range(36, 54):
        for x in range(16, 49):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            # Corners and bronze edge trim
            if x in (16, 17, 47, 48) or y in (52, 53):
                diff[y, x, :3] = C_BRONZE_BASE
                norm[y, x, :3] = (128, 128, 230)
            elif x in (18, 46):
                diff[y, x, :3] = C_AMBER_GOLD # Corner brass rivets
                norm[y, x, :3] = (128, 140, 240)
            else:
                # Polished dark obsidian panels
                diff[y, x, :3] = C_BASALT_DARK if (x + y) % 6 == 0 else C_BASALT_BASE
                norm[y, x, :3] = (128, 128, 255)

    # 3. Vaulted Lid (y: 22 to 36, x: 15 to 49)
    for y in range(22, 37):
        for x in range(15, 50):
            # Vault arch
            arch_dy = (y - 28)
            arch_dx = (x - 32)
            if arch_dx*arch_dx / 17.0**2 + (y - 36)**2 / 14.0**2 <= 1.0 or (26 <= y <= 36 and 16 <= x <= 48):
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255

                if y == 22 or y == 23:
                    # Top lid ridge with amber brass crest
                    diff[y, x, :3] = C_AMBER_GOLD
                    norm[y, x, 1] = 210
                elif x in (15, 16, 48, 49) or y == 36:
                    diff[y, x, :3] = C_BRONZE_MID
                    norm[y, x, :3] = (128, 128, 230)
                else:
                    diff[y, x, :3] = C_BASALT_MID if (x * 3 + y * 7) % 7 == 0 else C_BASALT_DARK
                    norm[y, x, :3] = (128, 128, 255)

    # 4. Central Chrono Lock Dial (y: 33 to 43, x: 27 to 37)
    for y in range(33, 44):
        for x in range(27, 38):
            d_sq = (x - 32)**2 + (y - 38)**2
            if d_sq <= 25:
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255

                if d_sq > 16:
                    # Outer bronze bezel
                    diff[y, x, :3] = C_AMBER_GOLD
                    norm[y, x, :3] = (128, 140, 240)
                elif d_sq <= 4:
                    # Glowing cyan core needle
                    diff[y, x, :3] = C_SPARK_WHITE
                    norm[y, x, :3] = (128, 128, 255)
                else:
                    # Cyan chronomantic glass
                    diff[y, x, :3] = C_CYAN_ACCENT if (x + y) % 2 == 0 else C_SOOT_LIGHT
                    norm[y, x, :3] = (128, 128, 230)

    return quantize_to_palette(diff), norm


def generate_prop_chain_spool():
    """64x64: Industrial hoist winch drum wrapped in heavy steel chains."""
    diff = create_blank(64, 64)
    norm = create_blank_normal(64, 64)

    # 1. Base Anchor Mount (y: 46 to 62, x: 12 to 52)
    for y in range(46, 62):
        for x in range(12, 53):
            if inside_diamond(x, y - 48, cx=31.5, cy=7.5, rx=20.0, ry=7.0):
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255
                diff[y, x, :3] = C_IRON_SHADOW if y >= 56 else C_IRON_BASE
                norm[y, x, :3] = (128, 115, 215)

    # 2. Side Iron Flanges / Stanchions (x: 16 to 22 and 42 to 48, y: 22 to 50)
    for y in range(22, 52):
        for x in range(15, 50):
            is_left_flange = (16 <= x <= 22)
            is_right_flange = (42 <= x <= 48)

            if is_left_flange or is_right_flange:
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255

                # Flange perimeter bevel
                if x in (16, 22, 42, 48) or y in (22, 51):
                    diff[y, x, :3] = C_BRONZE_BASE
                    norm[y, x, :3] = (128, 128, 230)
                else:
                    diff[y, x, :3] = C_IRON_MID if is_left_flange else C_IRON_BASE
                    norm[y, x, :3] = (128, 128, 255)

                # Bolt rivets
                if y in (26, 47) and x in (19, 45):
                    diff[y, x, :3] = C_AMBER_GOLD
                    norm[y, x, :3] = (128, 140, 240)

    # 3. Chain Drum Wrapped in Heavy Links (x: 23 to 41, y: 26 to 48)
    for y in range(26, 48):
        for x in range(23, 42):
            diff[y, x, 3] = 255
            norm[y, x, 3] = 255

            # Chain link pattern
            link_u = (x * 2 + y * 3) % 7
            link_v = (x - y * 2) % 5

            if y in (26, 47):
                diff[y, x, :3] = C_IRON_SHADOW
                norm[y, x, :3] = (128, 110, 200)
            elif link_u < 2:
                # Steel link highlight
                diff[y, x, :3] = C_BASALT_HIGHLIGHT
                norm[y, x, :3] = (140, 140, 240)
            elif link_v < 1:
                # Shadow between chain coils
                diff[y, x, :3] = C_BASALT_DEEP
                norm[y, x, :3] = (120, 120, 200)
            else:
                diff[y, x, :3] = C_BASALT_MID
                norm[y, x, :3] = (128, 128, 250)

    # 4. Large Ratchet Cog Wheel on Right Flange (x: 46 to 54, y: 28 to 44)
    for y in range(28, 45):
        for x in range(46, 55):
            d_cog = math.hypot(x - 49.0, y - 36.0)
            if d_cog <= 7.0:
                diff[y, x, 3] = 255
                norm[y, x, 3] = 255
                # Cog teeth
                angle = math.atan2(y - 36.0, x - 49.0)
                tooth = math.cos(angle * 8.0)
                if tooth > 0.2:
                    diff[y, x, :3] = C_AMBER_GOLD
                    norm[y, x, :3] = (140, 140, 240)
                else:
                    diff[y, x, :3] = C_BRONZE_MID
                    norm[y, x, :3] = (128, 128, 220)

    return quantize_to_palette(diff), norm


def main():
    raw_dir = "assets/sprites_raw/tilesets"
    os.makedirs(raw_dir, exist_ok=True)

    items = [
        ("floor_magma_fissure", generate_floor_magma_fissure),
        ("floor_iron_grate", generate_floor_iron_grate),
        ("prop_forge_anvil", generate_prop_forge_anvil),
        ("prop_slag_cauldron", generate_prop_slag_cauldron),
        ("prop_chrono_chest", generate_prop_chrono_chest),
        ("prop_chain_spool", generate_prop_chain_spool),
    ]

    print("Generating genuine Obsidian Foundry props and floor variants...")
    for name, gen_fn in items:
        diff, norm = gen_fn()
        diff_path = os.path.join(raw_dir, f"{name}.png")
        norm_path = os.path.join(raw_dir, f"{name}_n.png")
        Image.fromarray(diff).save(diff_path)
        Image.fromarray(norm).save(norm_path)
        print(f"  -> Generated {name} ({diff.shape[1]}x{diff.shape[0]})")

    print("All props generated successfully.")


if __name__ == "__main__":
    main()
