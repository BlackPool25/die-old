package com.dieold.game.world;

/**
 * Biome 1 (Obsidian Foundry) Tile Types mapped to genuine production atlas regions
 * and Box2D physics behavior.
 */
public enum TileType {
    EMPTY(false, false, false, false, null),
    FLOOR_BASALT_1(true, false, false, false, "floor_basalt_flagstone"),
    FLOOR_BASALT_2(true, false, false, false, "floor_basalt_flagstone"),
    FLOOR_BORDER(true, false, false, false, "floor_obsidian_border"),
    FLOOR_MOLTEN_VENT(true, true, false, false, "floor_molten_heat_vent"),
    FLOOR_RUNE_CIRCLE(true, false, false, false, "floor_rune_circle"),
    FLOOR_MAGMA_FISSURE(true, true, false, false, "floor_magma_fissure"),
    FLOOR_IRON_GRATE(true, false, false, false, "floor_iron_grate"),
    WALL_CAST_IRON_NW(false, false, true, false, "wall_cast_iron_nw"),
    WALL_CAST_IRON_NE(false, false, true, false, "wall_cast_iron_ne"),
    WALL_CORNER_OUTER(false, false, true, false, "wall_corner_outer"),
    WALL_FURNACE_VENT(false, false, true, false, "wall_furnace_vent"),
    BOUNDARY_LOW_CURB(false, false, true, false, "floor_obsidian_border"),
    PILLAR_BASALT(false, false, false, true, "pillar_basalt_column"),
    PROP_FORGE_ANVIL(false, false, false, true, "prop_forge_anvil"),
    PROP_SLAG_CAULDRON(false, true, false, true, "prop_slag_cauldron"),
    PROP_CHRONO_CHEST(false, false, false, true, "prop_chrono_chest"),
    PROP_CHAIN_SPOOL(false, false, false, true, "prop_chain_spool"),
    GATE_BARRIER(false, false, true, false, "wall_cast_iron_nw"); // Visually uses fortified iron gate / wall

    public final boolean isWalkable;
    public final boolean isHazard;
    public final boolean isWall;
    public final boolean isPillar;
    public final String regionName;

    TileType(boolean isWalkable, boolean isHazard, boolean isWall, boolean isPillar, String regionName) {
        this.isWalkable = isWalkable;
        this.isHazard = isHazard;
        this.isWall = isWall;
        this.isPillar = isPillar;
        this.regionName = regionName;
    }
}
