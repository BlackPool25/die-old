"""
tools/postprocess_normals.py
Ensures mathematical integrity on all baked normal maps:
- Blue channel on opaque/semi-opaque pixels (alpha > 0) is guaranteed >= 128 (Z >= 0 facing camera).
- Fixes antialiased edge pixels where background black bled into the blue channel.
"""

import os
import glob
import numpy as np
from PIL import Image


def fix_normals(search_pattern="assets/sprites_raw/characters/*_n.png"):
    files = glob.glob(search_pattern)
    print(f"[Normals] Validating & enforcing Z >= 0 on {len(files)} normal textures ({search_pattern})...")
    fixed_count = 0
    total_px_fixed = 0

    for path in files:
        with Image.open(path) as img:
            arr = np.array(img.convert("RGBA"))
            # Identify edge pixels where alpha > 0 and blue < 128
            alpha = arr[:, :, 3] > 0
            bad_mask = alpha & (arr[:, :, 2] < 128)
            bad_count = np.sum(bad_mask)
            if bad_count > 0:
                arr[bad_mask, 2] = 130
                # Also ensure R and G are centered if completely dark
                zero_rgb = bad_mask & (arr[:, :, 0] < 64) & (arr[:, :, 1] < 64)
                arr[zero_rgb, 0] = 128
                arr[zero_rgb, 1] = 128
                Image.fromarray(arr).save(path)
                fixed_count += 1
                total_px_fixed += bad_count

    print(f"[Normals] Finished. Fixed {total_px_fixed} boundary pixels across {fixed_count} files.")


if __name__ == "__main__":
    fix_normals("assets/sprites_raw/characters/*_n.png")
    fix_normals("assets/sprites_raw/tilesets/*_n.png")
