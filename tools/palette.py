"""
tools/palette.py
Defines the strict 32-color Obsidian Foundry palette for Biome 1 in Die Old.
"""

OBSIDIAN_FOUNDRY_PALETTE_HEX = [
    # 1-6: Basalt & Obsidian Deep Darks
    "#0B0C0E",
    "#121417",
    "#181B20",
    "#1B1E23",
    "#22272E",
    "#282D35",
    
    # 7-10: Basalt Midtones & Slate Highlights
    "#363C46",
    "#454D59",
    "#57606E",
    "#6C7787",
    
    # 11-16: Cast Iron & Rusted Bronze Darks
    "#201A17",
    "#2E2520",
    "#3B312B",
    "#4D3E36",
    "#5E493E",
    "#735B4D",
    
    # 17-20: Rusted Bronze Warm Highlights
    "#8B6D5A",
    "#A5836D",
    "#BE9B83",
    "#D4B49C",
    
    # 21-26: Chrono Heat & Molten Slag Core
    "#8C2200",
    "#C43B00",
    "#FF6600",
    "#FF8800",
    "#FFAA00",
    "#FFE57F",
    
    # 27-30: Cooling Blue Soot / Atmospheric Accents
    "#2D3A4A",
    "#3A4B5F",
    "#4A607A",
    "#627D9E",
    
    # 31-32: Incandescent Spark Highlights
    "#FFF3E0",
    "#FFFFFF"
]

def hex_to_rgb(hex_str):
    hex_str = hex_str.lstrip('#')
    return (
        int(hex_str[0:2], 16),
        int(hex_str[2:4], 16),
        int(hex_str[4:6], 16)
    )

OBSIDIAN_FOUNDRY_PALETTE_RGB = [hex_to_rgb(h) for h in OBSIDIAN_FOUNDRY_PALETTE_HEX]

def get_closest_palette_color(rgb):
    """Finds the closest palette color using Euclidean distance in RGB space."""
    r, g, b = int(rgb[0]), int(rgb[1]), int(rgb[2])
    best_dist = float('inf')
    best_color = OBSIDIAN_FOUNDRY_PALETTE_RGB[0]
    for p in OBSIDIAN_FOUNDRY_PALETTE_RGB:
        pr, pg, pb = int(p[0]), int(p[1]), int(p[2])
        dist = (r - pr) ** 2 + (g - pg) ** 2 + (b - pb) ** 2
        if dist < best_dist:
            best_dist = dist
            best_color = p
    return best_color
