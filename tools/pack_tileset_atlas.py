"""
tools/pack_tileset_atlas.py
Packs new foundry props and floor tiles into assets/atlases/tileset.png (1024x256)
and updates assets/atlases/tileset.atlas.
"""

import os
from PIL import Image

def main():
    atlas_png_path = "assets/atlases/tileset.png"
    atlas_txt_path = "assets/atlases/tileset.atlas"
    raw_dir = "assets/sprites_raw/tilesets"

    orig_sheet = Image.open(atlas_png_path).convert("RGBA")
    new_w, new_h = 1024, 256
    new_sheet = Image.new("RGBA", (new_w, new_h), (0, 0, 0, 0))
    new_sheet.paste(orig_sheet, (0, 0))

    new_items = [
        ("floor_magma_fissure", 64, 32),
        ("floor_magma_fissure_n", 64, 32),
        ("floor_iron_grate", 64, 32),
        ("floor_iron_grate_n", 64, 32),
        ("prop_forge_anvil", 64, 64),
        ("prop_forge_anvil_n", 64, 64),
        ("prop_slag_cauldron", 64, 64),
        ("prop_slag_cauldron_n", 64, 64),
        ("prop_chrono_chest", 64, 64),
        ("prop_chrono_chest_n", 64, 64),
        ("prop_chain_spool", 64, 64),
        ("prop_chain_spool_n", 64, 64),
    ]

    cur_x = 1
    y_pos = 130
    atlas_entries = []

    for name, w, h in new_items:
        img_path = os.path.join(raw_dir, f"{name}.png")
        if not os.path.exists(img_path):
            raise FileNotFoundError(f"Missing {img_path}")
        img = Image.open(img_path).convert("RGBA")
        new_sheet.paste(img, (cur_x, y_pos))

        entry = f"""{name}
  rotate: false
  xy: {cur_x}, {y_pos}
  size: {w}, {h}
  orig: {w}, {h}
  offset: 0, 0
  index: -1"""
        atlas_entries.append(entry)
        cur_x += w + 2

    new_sheet.save(atlas_png_path)
    print(f"Updated {atlas_png_path} to {new_w}x{new_h}.")

    # Read existing atlas text, update size to 1024, 256
    with open(atlas_txt_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Replace size
    lines = content.splitlines()
    for i, line in enumerate(lines):
        if line.startswith("size:"):
            lines[i] = f"size: {new_w}, {new_h}"
            break

    # Strip existing instances of new_items if any
    filtered_lines = []
    skip = False
    new_names_set = {name for name, _, _ in new_items}

    i = 0
    while i < len(lines):
        line = lines[i]
        if line.strip() in new_names_set:
            # skip this block (6 lines)
            i += 7
            continue
        filtered_lines.append(line)
        i += 1

    final_content = "\n".join(filtered_lines).rstrip() + "\n" + "\n".join(atlas_entries) + "\n"
    with open(atlas_txt_path, "w", encoding="utf-8") as f:
        f.write(final_content)

    print(f"Updated {atlas_txt_path} with {len(new_items)} regions.")

if __name__ == "__main__":
    main()
