"""
tools/generate_hud_vfx.py
Authors the Production-Grade Gothic Vector HUD Suite and VFX Particle Sprites for Die Old:

1. Chrono-Pendant Life Relic (assets/ui/ & assets/sprites_raw/ui/):
   - chrono_dial_base.png (128x128): Cast-iron & bronze astrolabe housing with 4 engraved quadrants
     (Youth I, Prime II, Elder III, Ascendant IV), astrological markings, and central gear hub.
   - chrono_needle.png (128x128): Independent rotating filigree clockwork pointer pivoting at (64,64)
     with incandescent amber jewel tip (LibGDX 360° animation ready).
   - chrono_glass_overlay.png (128x128): Convex antique pocket-watch glass reflection with specular arc.
   - chrono_pendant.png (128x128): Master composite dial for static icons and menus.
   - Corresponding master SVG vector sources.

2. Gothic NinePatch Health, Poise & Boss Meters:
   - meter_frame.png (256x24): Heavy cast-iron modular frame with gothic pointed corner brackets & rivets.
   - meter_health_fill.png (248x16): Deep crimson-to-incandescent amber alchemical blood-vitriol gradient.
   - meter_ghost_fill.png (248x16): Translucent lingering damage chip bar for visceral impact feedback.
   - meter_poise_fill.png (248x16): Kinetic electric blue/cyan gradient for Box2D impact stability.
   - meter_shield_fill.png (248x16): Alchemical hexagonal barrier overlay.
   - meter_segment_notch.png (4x16): Metallic tick divider for 25/50 HP intervals.
   - boss_meter_frame.png (384x28): Grand ornamental boss frame with horned crest and phase markers.
   - boss_meter_fill.png (376x20): Molten magma core boss health bar.

3. Alchemical Runic Sockets Reliquary:
   - socket_empty.png (40x40): Beveled stone octagon with carved dormant alchemical groove.
   - socket_active_cyan.png / socket_active.png (40x40): Radiant cyan Chrono-Sigil rune.
   - socket_active_amber.png (40x40): Incandescent amber Foundry-Ignition rune.
   - socket_active_purple.png (40x40): Temporal Void/Decay rune.
   - socket_locked.png (40x40): Forged cross-chains with brass seal.
   - runic_sockets_matrix.png (336x48): 8-socket reliquary mount strip.

4. Combat Mobility & Status Gauges:
   - dash_pip_ready.png (20x20): Kinetic energy diamond pip.
   - dash_pip_empty.png (20x20): Depleted iron diamond outline.
   - age_banner_frame.png (160x32): Gothic metallic ribbon banner.
   - alchemical_flask.png (36x48): Glass elixir vial with volumetric lines.

5. VFX Particle Sprites (assets/vfx/ & assets/sprites_raw/vfx/):
   - deflect_spark_00..03.png (64x64): Directional deflect spark bursts with motion trails.
   - slash_arc_00..03.png (64x64): Dynamic 2.5D crescent slash waves.
   - dust_puff_00..03.png (64x64): Stylized alchemical ground dust puffs.
"""

import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

UI_DIR = "assets/ui"
UI_RAW_DIR = "assets/sprites_raw/ui"
VFX_DIR = "assets/vfx"
VFX_RAW_DIR = "assets/sprites_raw/vfx"

os.makedirs(UI_DIR, exist_ok=True)
os.makedirs(UI_RAW_DIR, exist_ok=True)
os.makedirs(VFX_DIR, exist_ok=True)
os.makedirs(VFX_RAW_DIR, exist_ok=True)


def save_ui_image(img, filename):
    img.save(os.path.join(UI_DIR, filename))
    img.save(os.path.join(UI_RAW_DIR, filename))


def save_vfx_image(img, filename):
    img.save(os.path.join(VFX_DIR, filename))
    img.save(os.path.join(VFX_RAW_DIR, filename))


# ==============================================================================
# 1. MODULAR CHRONO-PENDANT LIFE RELIC
# ==============================================================================

def generate_chrono_dial_base_svg():
    """Generates master vector SVG for the Chrono-Pendant Dial Base."""
    svg = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256">
  <defs>
    <linearGradient id="bronzeBezel" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#D4B49C"/>
      <stop offset="25%" stop-color="#8B6D5A"/>
      <stop offset="60%" stop-color="#4D3E36"/>
      <stop offset="100%" stop-color="#201A17"/>
    </linearGradient>
    <radialGradient id="hubGrad" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#FFE57F"/>
      <stop offset="50%" stop-color="#FFAA00"/>
      <stop offset="85%" stop-color="#8C2200"/>
      <stop offset="100%" stop-color="#121417"/>
    </radialGradient>
  </defs>

  <!-- Outer Bezel Ring -->
  <circle cx="128" cy="128" r="122" fill="#181B20" stroke="url(#bronzeBezel)" stroke-width="8"/>
  <circle cx="128" cy="128" r="112" fill="#121417" stroke="#282D35" stroke-width="3"/>

  <!-- 12 Astrolabe Perimeter Studs -->
  <g fill="#FFAA00" stroke="#3B312B" stroke-width="1.5">
    <circle cx="128" cy="12" r="5"/><circle cx="186" cy="27" r="5"/><circle cx="229" cy="70" r="5"/>
    <circle cx="244" cy="128" r="5"/><circle cx="229" cy="186" r="5"/><circle cx="186" cy="229" r="5"/>
    <circle cx="128" cy="244" r="5"/><circle cx="70" cy="229" r="5"/><circle cx="27" cy="186" r="5"/>
    <circle cx="12" cy="128" r="5"/><circle cx="27" cy="70" r="5"/><circle cx="70" cy="27" r="5"/>
  </g>

  <!-- 4 Quadrants -->
  <!-- Q1: Youth (Top-Right: 20-34) -->
  <path d="M 128 128 L 128 20 A 108 108 0 0 1 236 128 Z" fill="#282D35" fill-opacity="0.55" stroke="#FFAA00" stroke-width="2"/>
  <text x="165" y="70" fill="#FFE57F" font-family="serif" font-size="14" font-weight="bold">I</text>
  <text x="155" y="90" fill="#BE9B83" font-family="sans-serif" font-size="10" font-weight="bold">YOUTH</text>
  <text x="155" y="104" fill="#8B6D5A" font-family="sans-serif" font-size="8">20-34</text>

  <!-- Q2: Prime (Bottom-Right: 35-54) -->
  <path d="M 128 128 L 236 128 A 108 108 0 0 1 128 236 Z" fill="#22272E" fill-opacity="0.65" stroke="#8B6D5A" stroke-width="1.5"/>
  <text x="165" y="175" fill="#D4B49C" font-family="serif" font-size="14" font-weight="bold">II</text>
  <text x="155" y="195" fill="#BE9B83" font-family="sans-serif" font-size="10" font-weight="bold">PRIME</text>
  <text x="155" y="209" fill="#8B6D5A" font-family="sans-serif" font-size="8">35-54</text>

  <!-- Q3: Elder (Bottom-Left: 55-74) -->
  <path d="M 128 128 L 128 236 A 108 108 0 0 1 20 128 Z" fill="#181B20" fill-opacity="0.75" stroke="#4A607A" stroke-width="1.5"/>
  <text x="75" y="175" fill="#627D9E" font-family="serif" font-size="14" font-weight="bold">III</text>
  <text x="65" y="195" fill="#4A607A" font-family="sans-serif" font-size="10" font-weight="bold">ELDER</text>
  <text x="65" y="209" fill="#3A4B5F" font-family="sans-serif" font-size="8">55-74</text>

  <!-- Q4: Ascendant (Top-Left: 75-80) -->
  <path d="M 128 128 L 20 128 A 108 108 0 0 1 128 20 Z" fill="#121417" fill-opacity="0.85" stroke="#FF6600" stroke-width="2"/>
  <text x="70" y="70" fill="#FF8800" font-family="serif" font-size="14" font-weight="bold">IV</text>
  <text x="42" y="90" fill="#FFAA00" font-family="sans-serif" font-size="9" font-weight="bold">ASCENDANT</text>
  <text x="65" y="104" fill="#C43B00" font-family="sans-serif" font-size="8">75-80</text>

  <!-- Astrological / Year Graduation Ticks -->
  <g stroke="#BE9B83" stroke-width="1.5">
    <line x1="128" y1="20" x2="128" y2="32"/><line x1="236" y1="128" x2="224" y2="128"/>
    <line x1="128" y1="236" x2="128" y2="224"/><line x1="20" y1="128" x2="32" y2="128"/>
    <line x1="204" y1="52" x2="195" y2="61"/><line x1="204" y1="204" x2="195" y2="195"/>
    <line x1="52" y1="204" x2="61" y2="195"/><line x1="52" y1="52" x2="61" y2="61"/>
  </g>

  <!-- Central Gear Medallion Hub -->
  <circle cx="128" cy="128" r="38" fill="url(#hubGrad)" stroke="#121417" stroke-width="3"/>
  <circle cx="128" cy="128" r="26" fill="#181B20" stroke="#FFAA00" stroke-width="1.5"/>
  <circle cx="128" cy="128" r="10" fill="#FFAA00" stroke="#3B312B" stroke-width="2"/>
</svg>"""
    with open(os.path.join(UI_DIR, "chrono_dial_base.svg"), "w", encoding="utf-8") as f:
        f.write(svg)


def generate_chrono_pendant_suite():
    """Generates the modular 128x128 Chrono-Pendant suite: base, needle, glass, and composite."""
    size = 128
    cx, cy = size // 2, size // 2

    # 1. Base Dial Face (chrono_dial_base.png)
    img_base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_base = ImageDraw.Draw(img_base)

    r_outer = 61
    r_inner = 53
    r_quad = 50

    # Outer bronze bezel
    d_base.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer],
                   fill=(24, 27, 32, 255), outline=(139, 109, 90, 255), width=4)
    d_base.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner],
                   fill=(18, 20, 23, 255), outline=(40, 45, 53, 255), width=2)

    # 4 Quadrants
    # Q1: Youth (270 to 360 deg: top-right)
    d_base.pieslice([cx - r_quad, cy - r_quad, cx + r_quad, cy + r_quad],
                    start=270, end=360, fill=(40, 45, 53, 200), outline=(255, 170, 0, 255), width=1)
    # Q2: Prime (0 to 90 deg: bottom-right)
    d_base.pieslice([cx - r_quad, cy - r_quad, cx + r_quad, cy + r_quad],
                    start=0, end=90, fill=(34, 39, 46, 210), outline=(139, 109, 90, 255), width=1)
    # Q3: Elder (90 to 180 deg: bottom-left)
    d_base.pieslice([cx - r_quad, cy - r_quad, cx + r_quad, cy + r_quad],
                    start=90, end=180, fill=(24, 27, 32, 220), outline=(74, 96, 122, 255), width=1)
    # Q4: Ascendant (180 to 270 deg: top-left)
    d_base.pieslice([cx - r_quad, cy - r_quad, cx + r_quad, cy + r_quad],
                    start=180, end=270, fill=(18, 20, 23, 230), outline=(255, 102, 0, 255), width=1)

    # 12 Perimeter Studs
    for deg in range(0, 360, 30):
        rad = math.radians(deg)
        sx = int(cx + (r_outer - 3) * math.cos(rad))
        sy = int(cy + (r_outer - 3) * math.sin(rad))
        d_base.ellipse([sx - 2, sy - 2, sx + 2, sy + 2], fill=(255, 170, 0, 255), outline=(59, 49, 43, 255))

    # Ticks every 45 deg
    for deg in range(0, 360, 45):
        rad = math.radians(deg)
        x1 = int(cx + 42 * math.cos(rad))
        y1 = int(cy + 42 * math.sin(rad))
        x2 = int(cx + 49 * math.cos(rad))
        y2 = int(cy + 49 * math.sin(rad))
        d_base.line([(x1, y1), (x2, y2)], fill=(190, 155, 131, 255), width=1)

    # Central Hub
    d_base.ellipse([cx - 18, cy - 18, cx + 18, cy + 18], fill=(255, 102, 0, 255), outline=(255, 229, 127, 255), width=2)
    d_base.ellipse([cx - 12, cy - 12, cx + 12, cy + 12], fill=(24, 27, 32, 255), outline=(255, 170, 0, 255), width=1)
    d_base.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=(255, 229, 127, 255))

    save_ui_image(img_base, "chrono_dial_base.png")

    # 2. Independent Rotating Needle (chrono_needle.png)
    # Origin is exact center (64, 64), pointing North (upward at 0° in LibGDX)
    img_needle = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_needle = ImageDraw.Draw(img_needle)

    # Needle blade pointing upward from (64, 64) to (64, 20)
    needle_pts = [(cx - 3, cy + 12), (cx + 3, cy + 12), (cx + 2, cy - 36), (cx, cy - 45), (cx - 2, cy - 36)]
    d_needle.polygon(needle_pts, fill=(255, 170, 0, 255), outline=(59, 49, 43, 255))
    # Glowing ember jewel tip
    d_needle.ellipse([cx - 3, cy - 47, cx + 3, cy - 41], fill=(255, 229, 127, 255), outline=(255, 255, 255, 255))
    # Central brass pivot collar
    d_needle.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], fill=(255, 170, 0, 255), outline=(18, 20, 23, 255), width=2)
    d_needle.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(255, 255, 255, 255))

    save_ui_image(img_needle, "chrono_needle.png")

    # 3. Glass Overlay with Specular Arc (chrono_glass_overlay.png)
    img_glass = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_glass = ImageDraw.Draw(img_glass)
    # Upper left specular arc
    d_glass.arc([cx - r_quad + 2, cy - r_quad + 2, cx + r_quad - 2, cy + r_quad - 2],
                start=190, end=310, fill=(255, 255, 255, 110), width=3)
    # Subtle inner reflection crescent
    d_glass.ellipse([cx - 35, cy - 42, cx + 25, cy - 20], fill=(255, 255, 255, 35))

    save_ui_image(img_glass, "chrono_glass_overlay.png")

    # 4. Composite Dial (chrono_pendant.png - with needle rotated to Youth: 35 deg)
    img_comp = img_base.copy()
    rotated_needle = img_needle.rotate(-35, resample=Image.Resampling.BICUBIC)
    img_comp.alpha_composite(rotated_needle)
    img_comp.alpha_composite(img_glass)
    save_ui_image(img_comp, "chrono_pendant.png")

    generate_chrono_dial_base_svg()
    print("[HUD] Generated Chrono-Pendant modular suite: base, needle, glass, composite.")


# ==============================================================================
# 2. GOTHIC NINEPATCH HEALTH, POISE & BOSS METERS
# ==============================================================================

def generate_meters_suite():
    """Generates high-contrast gothic health, poise, ghost, shield, and boss meters."""
    # 1. Standard Combat Meter Frame (256x24) - NinePatch ready (split: 8, 8, 4, 4)
    w, h = 256, 24
    img_frame = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    df = ImageDraw.Draw(img_frame)

    # Cast-iron beveled outer frame with gothic pointed end-caps
    poly_frame = [
        (6, 0), (w - 7, 0), (w - 1, 6), (w - 1, h - 7),
        (w - 7, h - 1), (6, h - 1), (0, h - 7), (0, 6)
    ]
    df.polygon(poly_frame, fill=(27, 30, 35, 255), outline=(94, 73, 62, 255), width=2)
    # Inner dark channel recess
    df.rectangle([5, 4, w - 6, h - 5], fill=(11, 12, 14, 255), outline=(40, 45, 53, 255), width=1)

    # Ornamental bronze rivets on left/right end brackets
    df.ellipse([1, 10, 4, 13], fill=(255, 170, 0, 255))
    df.ellipse([w - 5, 10, w - 2, 13], fill=(255, 170, 0, 255))

    save_ui_image(img_frame, "meter_frame.png")

    # 2. Health Fill (246x16)
    fill_w, fill_h = w - 10, h - 8
    img_hp = Image.new("RGBA", (fill_w, fill_h), (0, 0, 0, 0))
    d_hp = ImageDraw.Draw(img_hp)

    for x in range(fill_w):
        prog = x / float(fill_w)
        # Deep crimson (#8C2200) to incandescent amber (#FFAA00)
        r = int(140 + 115 * prog)
        g = int(34 + 136 * (prog ** 1.5))
        b = int(10 * (1.0 - prog))
        d_hp.line([(x, 0), (x, fill_h - 1)], fill=(r, g, b, 255))

    # Top specular sheen & energy highlight
    d_hp.line([(0, 1), (fill_w - 1, 1)], fill=(255, 229, 127, 220))
    d_hp.line([(0, 2), (fill_w - 1, 2)], fill=(255, 255, 255, 120))
    save_ui_image(img_hp, "meter_health_fill.png")

    # 3. Ghost / Lingering Damage Fill (246x16)
    img_ghost = Image.new("RGBA", (fill_w, fill_h), (0, 0, 0, 0))
    d_ghost = ImageDraw.Draw(img_ghost)
    for x in range(fill_w):
        prog = x / float(fill_w)
        # Translucent amber-cream lingering damage trail
        d_ghost.line([(x, 0), (x, fill_h - 1)], fill=(255, 200, 100, int(150 + 60 * prog)))
    d_ghost.line([(0, 1), (fill_w - 1, 1)], fill=(255, 255, 255, 200))
    save_ui_image(img_ghost, "meter_ghost_fill.png")

    # 4. Poise Fill (246x16)
    img_poise = Image.new("RGBA", (fill_w, fill_h), (0, 0, 0, 0))
    d_poise = ImageDraw.Draw(img_poise)
    for x in range(fill_w):
        prog = x / float(fill_w)
        # Kinetic electric cyan / ozone blue
        r = int(0 + 56 * prog)
        g = int(140 + 90 * prog)
        b = int(210 + 45 * prog)
        d_poise.line([(x, 0), (x, fill_h - 1)], fill=(r, g, b, 255))
    d_poise.line([(0, 1), (fill_w - 1, 1)], fill=(255, 255, 255, 230))
    save_ui_image(img_poise, "meter_poise_fill.png")

    # 5. Shield / Barrier Fill (246x16)
    img_shield = Image.new("RGBA", (fill_w, fill_h), (0, 0, 0, 0))
    d_shield = ImageDraw.Draw(img_shield)
    for x in range(fill_w):
        d_shield.line([(x, 0), (x, fill_h - 1)], fill=(120, 180, 255, 160))
    d_shield.line([(0, 1), (fill_w - 1, 1)], fill=(255, 255, 255, 240))
    save_ui_image(img_shield, "meter_shield_fill.png")

    # 6. Segment Tick Notch (4x16)
    img_notch = Image.new("RGBA", (4, fill_h), (0, 0, 0, 0))
    d_notch = ImageDraw.Draw(img_notch)
    d_notch.line([(1, 0), (1, fill_h - 1)], fill=(18, 20, 23, 255), width=1)
    d_notch.line([(2, 0), (2, fill_h - 1)], fill=(94, 73, 62, 220), width=1)
    save_ui_image(img_notch, "meter_segment_notch.png")

    # 7. Boss Health Bar Frame (384x28)
    bw, bh = 384, 28
    img_boss_frame = Image.new("RGBA", (bw, bh), (0, 0, 0, 0))
    dbf = ImageDraw.Draw(img_boss_frame)

    boss_poly = [
        (10, 0), (bw - 11, 0), (bw - 1, 10), (bw - 1, bh - 11),
        (bw - 11, bh - 1), (10, bh - 1), (0, bh - 11), (0, 10)
    ]
    dbf.polygon(boss_poly, fill=(24, 27, 32, 255), outline=(139, 109, 90, 255), width=2)
    dbf.rectangle([7, 4, bw - 8, bh - 5], fill=(11, 12, 14, 255), outline=(40, 45, 53, 255), width=1)

    # Boss Crest Finials
    dbf.polygon([(0, 4), (6, 0), (10, 8), (4, 14)], fill=(255, 170, 0, 255))
    dbf.polygon([(bw - 1, 4), (bw - 7, 0), (bw - 11, 8), (bw - 5, 14)], fill=(255, 170, 0, 255))

    # Phase dividers at 25%, 50%, 75%
    for px_ratio in [0.25, 0.50, 0.75]:
        div_x = int(bw * px_ratio)
        dbf.line([(div_x, 2), (div_x, bh - 3)], fill=(255, 170, 0, 255), width=2)

    save_ui_image(img_boss_frame, "boss_meter_frame.png")

    # 8. Boss Health Fill (370x20)
    bf_w, bf_h = bw - 14, bh - 8
    img_boss_fill = Image.new("RGBA", (bf_w, bf_h), (0, 0, 0, 0))
    db_fill = ImageDraw.Draw(img_boss_fill)
    for x in range(bf_w):
        prog = x / float(bf_w)
        # Molten magma core
        r = int(180 + 75 * prog)
        g = int(20 + 130 * (prog ** 1.3))
        b = 0
        db_fill.line([(x, 0), (x, bf_h - 1)], fill=(r, g, b, 255))
    db_fill.line([(0, 1), (bf_w - 1, 1)], fill=(255, 240, 180, 240))
    save_ui_image(img_boss_fill, "boss_meter_fill.png")

    print("[HUD] Generated Gothic Meters suite (NinePatch frame, health, ghost, poise, shield, notch, boss bar).")


# ==============================================================================
# 3. ALCHEMICAL RUNIC SOCKETS RELIQUARY
# ==============================================================================

def generate_runic_sockets_suite():
    """Generates 40x40 carved reliquary sockets and 8-slot matrix."""
    size = 40
    cx, cy = size // 2, size // 2

    # Helper: draw beveled stone octagon socket base
    def draw_socket_base(draw, border_col=(94, 73, 62, 255), fill_col=(27, 30, 35, 255)):
        oct_pts = [
            (8, 0), (31, 0), (39, 8), (39, 31),
            (31, 39), (8, 39), (0, 31), (0, 8)
        ]
        draw.polygon(oct_pts, fill=fill_col, outline=border_col, width=2)
        draw.ellipse([8, 8, 31, 31], fill=(14, 16, 18, 255), outline=(40, 45, 53, 255), width=2)
        # Corner rivets
        for rx, ry in [(8, 4), (31, 4), (36, 12), (36, 28), (31, 36), (8, 36), (4, 28), (4, 12)]:
            draw.point((rx, ry), fill=(255, 170, 0, 255))

    # 1. Empty Socket (socket_empty.png)
    img_empty = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_empty = ImageDraw.Draw(img_empty)
    draw_socket_base(d_empty)
    # Dormant carved concentric grooves
    d_empty.ellipse([13, 13, 26, 26], outline=(34, 39, 46, 255), width=1)
    d_empty.point((cx, cy), fill=(54, 60, 70, 255))
    save_ui_image(img_empty, "socket_empty.png")

    # 2. Active Cyan Chrono-Sigil Socket (socket_active_cyan.png & socket_active.png)
    img_cyan = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_cyan = ImageDraw.Draw(img_cyan)
    draw_socket_base(d_cyan, border_col=(0, 240, 255, 255), fill_col=(30, 38, 48, 255))
    # Glowing rune energy core
    d_cyan.ellipse([10, 10, 29, 29], fill=(0, 160, 255, 180), outline=(0, 240, 255, 255), width=2)
    # Alchemical Chrono-Spiral / Hourglass Glyph
    d_cyan.polygon([(14, 14), (25, 14), (14, 25), (25, 25)], fill=(224, 255, 255, 255))
    d_cyan.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(255, 255, 255, 255))
    save_ui_image(img_cyan, "socket_active_cyan.png")
    save_ui_image(img_cyan, "socket_active.png")

    # 3. Active Amber Foundry-Flame Socket (socket_active_amber.png)
    img_amber = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_amber = ImageDraw.Draw(img_amber)
    draw_socket_base(d_amber, border_col=(255, 170, 0, 255), fill_col=(40, 32, 24, 255))
    d_amber.ellipse([10, 10, 29, 29], fill=(255, 102, 0, 200), outline=(255, 229, 127, 255), width=2)
    # Alchemical Sulfur / Fire triangle glyph
    d_amber.polygon([(cx, 13), (27, 26), (13, 26)], outline=(255, 255, 255, 255), width=2)
    d_amber.point((cx, 20), fill=(255, 255, 255, 255))
    save_ui_image(img_amber, "socket_active_amber.png")

    # 4. Active Purple Void/Decay Socket (socket_active_purple.png)
    img_purple = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_purple = ImageDraw.Draw(img_purple)
    draw_socket_base(d_purple, border_col=(180, 80, 255, 255), fill_col=(36, 24, 44, 255))
    d_purple.ellipse([10, 10, 29, 29], fill=(140, 30, 220, 190), outline=(220, 160, 255, 255), width=2)
    # Alchemical Salt / Crescent glyph
    d_purple.arc([13, 13, 26, 26], start=45, end=315, fill=(255, 255, 255, 255), width=2)
    save_ui_image(img_purple, "socket_active_purple.png")

    # 5. Locked Socket (socket_locked.png)
    img_locked = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d_locked = ImageDraw.Draw(img_locked)
    draw_socket_base(d_locked, border_col=(59, 49, 43, 255), fill_col=(18, 20, 23, 255))
    # Forged crossed iron chains
    d_locked.line([(8, 8), (31, 31)], fill=(108, 119, 135, 255), width=3)
    d_locked.line([(31, 8), (8, 31)], fill=(108, 119, 135, 255), width=3)
    # Brass padlock / wax seal
    d_locked.rectangle([14, 16, 25, 26], fill=(139, 109, 90, 255), outline=(255, 170, 0, 255), width=1)
    d_locked.ellipse([17, 12, 22, 18], outline=(255, 170, 0, 255), width=2)
    d_locked.point((cx, 21), fill=(18, 20, 23, 255))
    save_ui_image(img_locked, "socket_locked.png")

    # 6. Sockets Matrix Strip (336x48) - Holds 8 sockets
    mw, mh = 336, 48
    img_matrix = Image.new("RGBA", (mw, mh), (0, 0, 0, 0))
    d_mat = ImageDraw.Draw(img_matrix)
    d_mat.rounded_rectangle([0, 0, mw - 1, mh - 1], radius=6, fill=(18, 20, 23, 240), outline=(94, 73, 62, 255), width=2)
    d_mat.rectangle([4, 4, mw - 5, mh - 5], outline=(40, 45, 53, 255), width=1)

    # Paste sample loadout: 3 Active (Cyan, Amber, Purple), 3 Empty, 2 Locked
    loadout = [img_cyan, img_amber, img_purple, img_empty, img_empty, img_empty, img_locked, img_locked]
    for idx, s_img in enumerate(loadout):
        ox = 6 + idx * 41
        img_matrix.paste(s_img, (ox, 4), s_img)

    save_ui_image(img_matrix, "runic_sockets_matrix.png")
    print("[HUD] Generated Alchemical Runic Sockets suite: empty, cyan, amber, purple, locked, matrix.")


# ==============================================================================
# 4. COMBAT MOBILITY & STATUS ASSETS
# ==============================================================================

def generate_status_mobility_suite():
    """Generates Dash pips, Age banner frame, and Alchemical elixir flask."""
    # 1. Dash Pips (20x20)
    # Ready pip: Glowing diamond
    img_dash_ready = Image.new("RGBA", (20, 20), (0, 0, 0, 0))
    d_dr = ImageDraw.Draw(img_dash_ready)
    diamond_pts = [(10, 1), (18, 10), (10, 18), (1, 10)]
    d_dr.polygon(diamond_pts, fill=(0, 229, 255, 230), outline=(255, 255, 255, 255), width=1)
    d_dr.polygon([(10, 5), (14, 10), (10, 14), (5, 10)], fill=(255, 255, 255, 255))
    save_ui_image(img_dash_ready, "dash_pip_ready.png")

    # Empty pip: Depleted iron diamond
    img_dash_empty = Image.new("RGBA", (20, 20), (0, 0, 0, 0))
    d_de = ImageDraw.Draw(img_dash_empty)
    d_de.polygon(diamond_pts, fill=(24, 27, 32, 200), outline=(74, 96, 122, 255), width=1)
    save_ui_image(img_dash_empty, "dash_pip_empty.png")

    # 2. Age Banner Placard Frame (160x32)
    img_banner = Image.new("RGBA", (160, 32), (0, 0, 0, 0))
    d_ban = ImageDraw.Draw(img_banner)
    banner_poly = [
        (8, 0), (151, 0), (159, 8), (159, 23),
        (151, 31), (8, 31), (0, 23), (0, 8)
    ]
    d_ban.polygon(banner_poly, fill=(24, 27, 32, 250), outline=(139, 109, 90, 255), width=2)
    d_ban.rectangle([6, 4, 153, 27], outline=(40, 45, 53, 255), width=1)
    d_ban.ellipse([2, 14, 5, 17], fill=(255, 170, 0, 255))
    d_ban.ellipse([154, 14, 157, 17], fill=(255, 170, 0, 255))
    save_ui_image(img_banner, "age_banner_frame.png")

    # 3. Alchemical Elixir Flask (36x48)
    img_flask = Image.new("RGBA", (36, 48), (0, 0, 0, 0))
    d_flask = ImageDraw.Draw(img_flask)

    # Cork & Brass neck
    d_flask.rectangle([13, 2, 22, 7], fill=(139, 109, 90, 255), outline=(59, 49, 43, 255))
    d_flask.rectangle([11, 7, 24, 12], fill=(255, 170, 0, 255), outline=(59, 49, 43, 255))

    # Glass bulb body
    bulb_pts = [(12, 12), (23, 12), (33, 26), (33, 40), (28, 46), (7, 46), (2, 40), (2, 26)]
    d_flask.polygon(bulb_pts, fill=(18, 20, 23, 180), outline=(108, 119, 135, 255), width=2)

    # Glowing amber liquid fill (half full)
    liquid_pts = [(5, 28), (30, 28), (31, 40), (27, 44), (8, 44), (4, 40)]
    d_flask.polygon(liquid_pts, fill=(255, 102, 0, 220))
    d_flask.line([(5, 28), (30, 28)], fill=(255, 229, 127, 255), width=2)

    # Glass specular reflection arc
    d_flask.line([(6, 20), (4, 34)], fill=(255, 255, 255, 180), width=2)

    save_ui_image(img_flask, "alchemical_flask.png")
    print("[HUD] Generated Mobility & Status suite: dash pips, age banner, elixir flask.")


# ==============================================================================
# 5. VFX PARTICLE SPRITES
# ==============================================================================

def generate_vfx_sprites():
    """Generates directional deflect sparks, crescent slash waves, and dust puffs."""
    # 1. Deflect Sparks (4 frames: 64x64)
    size = 64
    for frame in range(4):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        cx, cy = size // 2, size // 2
        num_sparks = 18
        dist = 6 + frame * 10
        tail_len = max(3, 14 - frame * 3)

        for i in range(num_sparks):
            angle = (i / float(num_sparks)) * 2 * math.pi + (i * 0.25)
            dx = math.cos(angle)
            dy = math.sin(angle)
            head_x = int(cx + dist * dx)
            head_y = int(cy + dist * dy)
            tail_x = int(cx + (dist - tail_len) * dx)
            tail_y = int(cy + (dist - tail_len) * dy)

            col = (255, 255, 255, 255) if frame < 2 else (255, 170, 0, 255)
            draw.line([(tail_x, tail_y), (head_x, head_y)], fill=col, width=max(1, 3 - frame))
            draw.point((head_x, head_y), fill=(255, 229, 127, 255))

        if frame == 0:
            draw.ellipse([cx - 9, cy - 9, cx + 9, cy + 9], fill=(255, 255, 255, 255), outline=(255, 170, 0, 255))
        elif frame == 1:
            draw.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=(255, 229, 127, 230))

        save_vfx_image(img, f"deflect_spark_{frame:02d}.png")

    # 2. Slash Arcs (4 frames: 64x64)
    for frame in range(4):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        cx, cy = size // 2, size // 2

        start_angle = 180 + frame * 15
        end_angle = start_angle + 125 - frame * 10
        r_outer = 16 + frame * 9
        r_inner = r_outer - max(2, 7 - frame * 2)

        draw.pieslice([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer],
                      start=start_angle, end=end_angle, fill=(255, 170, 0, max(60, 255 - frame * 55)))
        draw.pieslice([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner],
                      start=start_angle - 2, end=end_angle + 2, fill=(0, 0, 0, 0))

        edge_pts = []
        for a in range(start_angle, end_angle, 4):
            rad = math.radians(a)
            edge_pts.append((int(cx + r_outer * math.cos(rad)), int(cy + r_outer * math.sin(rad))))
        if len(edge_pts) > 1:
            draw.line(edge_pts, fill=(255, 255, 255, max(80, 255 - frame * 45)), width=2)

        save_vfx_image(img, f"slash_arc_{frame:02d}.png")

    # 3. Ground Dust Puffs (4 frames: 64x64)
    dust_colors = [
        (45, 52, 60), (54, 60, 70), (74, 82, 95), (34, 39, 46)
    ]
    for frame in range(4):
        img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        cx, cy = size // 2, size // 2 + 8
        alpha = max(40, 220 - frame * 50)
        spread = 4 + frame * 8

        bubbles = [
            (-spread, -frame * 3, 10 + frame * 3),
            (0, -frame * 4, 13 + frame * 4),
            (spread, -frame * 3, 11 + frame * 3),
            (-spread * 0.5, frame * 2, 8 + frame * 2),
            (spread * 0.5, frame * 2, 8 + frame * 2)
        ]
        for bx, by, br in bubbles:
            col = dust_colors[frame % len(dust_colors)]
            draw.ellipse([cx + bx - br, cy + by - br, cx + bx + br, cy + by + br],
                         fill=(col[0], col[1], col[2], alpha))

        save_vfx_image(img, f"dust_puff_{frame:02d}.png")

    print("[VFX] Generated Particle Sprites: deflect sparks, slash arcs, dust puffs.")


def main():
    print("==================================================")
    print("AUTHORING GOTHIC VECTOR HUD & VFX ASSET SUITE")
    print("==================================================")
    generate_chrono_pendant_suite()
    generate_meters_suite()
    generate_runic_sockets_suite()
    generate_status_mobility_suite()
    generate_vfx_sprites()
    print("==================================================")
    print("HUD & VFX ASSET SUITE SUCCESSFULLY GENERATED.")
    print("==================================================")


if __name__ == "__main__":
    main()
