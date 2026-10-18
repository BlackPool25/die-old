package com.dieold.game.world;

import com.badlogic.gdx.utils.Array;

/**
 * Handcrafted Room Prefab Registry for Biome 1: The Obsidian Foundry.
 * Contains at least 8 distinct, authentic room templates:
 * 1. IngressHub (Spawn chamber with central Chrono-Rune circle)
 * 2. CombatArenaSmall (14x14 close-quarters encounter room)
 * 3. CombatArenaMedium (20x20 tactical arena with 4 basalt pillars and molten vents)
 * 4. HazardConduitVertical (10x20 elongated corridor lined with molten slag vents)
 * 5. HazardConduitHorizontal (20x10 elongated corridor lined with molten slag vents)
 * 6. PuzzleAlcove (14x14 age-gated chamber with crawlspace alcove)
 * 7. LockdownChamber (16x16 enclosed arena with 2 portcullis combat gates)
 * 8. SanctuaryAirlock (12x12 safe rest/shop zone)
 * 9. Antechamber (20x18 grand hall leading to Gatekeeper Vault)
 */
public class RoomPrefabRegistry {

    private final Array<RoomTemplate> templates = new Array<>();

    public RoomPrefabRegistry() {
        registerTemplates();
    }

    private void registerTemplates() {
        templates.add(createIngressHub());
        templates.add(createCombatArenaSmall());
        templates.add(createCombatArenaMedium());
        templates.add(createHazardConduitVertical());
        templates.add(createHazardConduitHorizontal());
        templates.add(createPuzzleAlcove());
        templates.add(createLockdownChamber());
        templates.add(createSanctuaryAirlock());
        templates.add(createAntechamber());
    }

    public Array<RoomTemplate> getAllTemplates() {
        return templates;
    }

    public RoomTemplate getTemplate(String id) {
        for (int i = 0; i < templates.size; i++) {
            RoomTemplate t = templates.get(i);
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    /**
     * 1. IngressHub: Safe spawn chamber with central Chrono-Rune circle and iron catwalks (16x16).
     */
    public RoomTemplate createIngressHub() {
        RoomTemplate t = new RoomTemplate("IngressHub", 16, 16, false);
        t.addSocket(new Socket("ingress_n", 7, 15, Direction.NORTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("ingress_e", 15, 7, Direction.EAST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("ingress_s", 7, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("ingress_w", 0, 7, Direction.WEST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Central Chrono-Rune Dais (7,7 to 8,8)
        for (int x = 7; x <= 8; x++) {
            for (int y = 7; y <= 8; y++) {
                t.setTile(x, y, TileType.FLOOR_RUNE_CIRCLE);
            }
        }

        // Cross-axial Cast-Iron Grate catwalks leading to the 4 portals
        for (int y = 1; y <= 6; y++) {
            t.setTile(7, y, TileType.FLOOR_IRON_GRATE);
            t.setTile(8, y, TileType.FLOOR_IRON_GRATE);
        }
        for (int y = 9; y <= 14; y++) {
            t.setTile(7, y, TileType.FLOOR_IRON_GRATE);
            t.setTile(8, y, TileType.FLOOR_IRON_GRATE);
        }
        for (int x = 1; x <= 6; x++) {
            t.setTile(x, 7, TileType.FLOOR_IRON_GRATE);
            t.setTile(x, 8, TileType.FLOOR_IRON_GRATE);
        }
        for (int x = 9; x <= 14; x++) {
            t.setTile(x, 7, TileType.FLOOR_IRON_GRATE);
            t.setTile(x, 8, TileType.FLOOR_IRON_GRATE);
        }

        // Corner Foundry Logistics Props
        t.setTile(3, 12, TileType.PROP_CHAIN_SPOOL);
        t.setTile(12, 12, TileType.PROP_FORGE_ANVIL);
        t.setTile(3, 3, TileType.PROP_CHRONO_CHEST);
        t.setTile(12, 3, TileType.PROP_CHAIN_SPOOL);

        t.addPlayerSpawn(8.0f, 8.0f);
        return t;
    }

    /**
     * 2. CombatArenaSmall: 14x14 close-quarters encounter room with tactical anvils and clean combat floor.
     */
    public RoomTemplate createCombatArenaSmall() {
        RoomTemplate t = new RoomTemplate("CombatArenaSmall", 14, 14, true);
        t.addSocket(new Socket("arena_s_s", 6, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_s_n", 6, 13, Direction.NORTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_s_e", 13, 6, Direction.EAST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_s_w", 0, 6, Direction.WEST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Localized corner slag hazard pool (leaves 88% floor clean for dodging)
        t.setTile(2, 10, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(3, 10, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(2.5f, 10.5f);
        t.addHazard(3.5f, 10.5f);

        // Tactical perimeter workstation alcoves (keeps center clear for dodging)
        t.setTile(2, 3, TileType.PROP_FORGE_ANVIL);
        t.setTile(11, 11, TileType.PROP_FORGE_ANVIL);
        t.setTile(11, 3, TileType.PROP_SLAG_CAULDRON);

        // Enemy Hound spawns
        t.addEnemySpawn(3.5f, 6.5f);
        t.addEnemySpawn(10.5f, 6.5f);

        return t;
    }

    /**
     * 3. CombatArenaMedium: 20x20 tactical arena with 4 basalt pillars, central forge platform, and slag cauldrons.
     */
    public RoomTemplate createCombatArenaMedium() {
        RoomTemplate t = new RoomTemplate("CombatArenaMedium", 20, 20, true);
        t.addSocket(new Socket("arena_m_s", 9, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_m_n", 9, 19, Direction.NORTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_m_e", 19, 9, Direction.EAST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_m_w", 0, 9, Direction.WEST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // 4 Monolithic Basalt Columns for tactical cover
        int[] pillars = {5, 5, 5, 14, 14, 5, 14, 14};
        for (int i = 0; i < pillars.length; i += 2) {
            int px = pillars[i];
            int py = pillars[i + 1];
            t.setTile(px, py, TileType.PILLAR_BASALT);
            t.addPillar(px + 0.5f, py + 0.5f);
        }

        // Central Chrono-Rune Dais (clean open floor for 360° combat mobility)
        t.setTile(9, 9, TileType.FLOOR_RUNE_CIRCLE);
        t.setTile(10, 9, TileType.FLOOR_RUNE_CIRCLE);
        t.setTile(9, 10, TileType.FLOOR_RUNE_CIRCLE);
        t.setTile(10, 10, TileType.FLOOR_RUNE_CIRCLE);

        // Perimeter Master Blacksmith Workstation (along north alcove wall)
        t.setTile(3, 17, TileType.PROP_FORGE_ANVIL);
        t.setTile(4, 17, TileType.PROP_SLAG_CAULDRON);

        // Localized perimeter hazard trenches
        t.setTile(2, 9, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(2, 10, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(2.5f, 9.5f);
        t.addHazard(2.5f, 10.5f);

        t.setTile(17, 9, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(17, 10, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(17.5f, 9.5f);
        t.addHazard(17.5f, 10.5f);

        // Wall Alcove Chain Hoists
        t.setTile(3, 16, TileType.PROP_CHAIN_SPOOL);
        t.setTile(16, 3, TileType.PROP_CHAIN_SPOOL);

        // Enemy spawns
        t.addEnemySpawn(6.5f, 6.5f);
        t.addEnemySpawn(13.5f, 6.5f);
        t.addEnemySpawn(10.0f, 15.0f);

        return t;
    }

    /**
     * 4. HazardConduitVertical: 10x20 corridor with generous 4-tile wide walkway and perimeter hazard alcoves.
     */
    public RoomTemplate createHazardConduitVertical() {
        RoomTemplate t = new RoomTemplate("HazardConduitVertical", 10, 20, false);
        t.addSocket(new Socket("conduit_v_s", 4, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("conduit_v_n", 4, 19, Direction.NORTH, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Wide, clear walkway down center (x=4..5 clean basalt, x=3 & x=6 border runners)
        for (int y = 1; y <= 18; y++) {
            t.setTile(3, y, TileType.FLOOR_BORDER);
            t.setTile(6, y, TileType.FLOOR_BORDER);
        }

        // Localized perimeter hazard alcoves (recessed, not obstructing movement)
        t.setTile(1, 8, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(1, 9, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(1.5f, 8.5f);
        t.addHazard(1.5f, 9.5f);

        t.setTile(8, 14, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(8, 15, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(8.5f, 14.5f);
        t.addHazard(8.5f, 15.5f);

        // Wall hoist winch drums
        t.setTile(1, 14, TileType.PROP_CHAIN_SPOOL);
        t.setTile(8, 8, TileType.PROP_CHAIN_SPOOL);

        return t;
    }

    /**
     * 5. HazardConduitHorizontal: 20x10 corridor with generous 4-tile wide walkway and perimeter hazard alcoves.
     */
    public RoomTemplate createHazardConduitHorizontal() {
        RoomTemplate t = new RoomTemplate("HazardConduitHorizontal", 20, 10, false);
        t.addSocket(new Socket("conduit_h_w", 0, 4, Direction.WEST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("conduit_h_e", 19, 4, Direction.EAST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Wide, clear walkway across center (y=4..5 clean basalt, y=3 & y=6 border runners)
        for (int x = 1; x <= 18; x++) {
            t.setTile(x, 3, TileType.FLOOR_BORDER);
            t.setTile(x, 6, TileType.FLOOR_BORDER);
        }

        // Localized perimeter hazard alcoves
        t.setTile(8, 1, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(9, 1, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(8.5f, 1.5f);
        t.addHazard(9.5f, 1.5f);

        t.setTile(14, 8, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(15, 8, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(14.5f, 8.5f);
        t.addHazard(15.5f, 8.5f);

        // Props in alcoves
        t.setTile(14, 1, TileType.PROP_CHAIN_SPOOL);
        t.setTile(8, 8, TileType.PROP_CHAIN_SPOOL);

        return t;
    }

    /**
     * 6. PuzzleAlcove: 14x14 age-gated chamber with ventilation crawlspace and Chrono-Chest cache.
     */
    public RoomTemplate createPuzzleAlcove() {
        RoomTemplate t = new RoomTemplate("PuzzleAlcove", 14, 14, false);
        t.addSocket(new Socket("puzzle_s", 6, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("puzzle_n", 6, 13, Direction.NORTH, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Cast-iron divider partition with tactical anvil & wide crawlspace
        t.setTile(3, 7, TileType.PROP_FORGE_ANVIL);
        t.setTile(10, 7, TileType.PROP_FORGE_ANVIL);
        t.setTile(2, 7, TileType.WALL_CAST_IRON_NW);
        t.setTile(11, 7, TileType.WALL_CAST_IRON_NW);

        // Ventilation crawlspace catwalk
        t.setTile(6, 7, TileType.FLOOR_IRON_GRATE);
        t.setTile(7, 7, TileType.FLOOR_IRON_GRATE);

        // Secret Chrono-Chest Cache on Runic Dais
        t.setTile(6, 10, TileType.FLOOR_RUNE_CIRCLE);
        t.setTile(7, 10, TileType.FLOOR_RUNE_CIRCLE);
        t.setTile(6, 11, TileType.FLOOR_RUNE_CIRCLE);
        t.setTile(7, 11, TileType.PROP_CHRONO_CHEST);
        t.setTile(4, 11, TileType.PROP_CHAIN_SPOOL);

        return t;
    }

    /**
     * 7. LockdownChamber: 16x16 enclosed arena with 2 portcullis combat gates and slag braziers.
     */
    public RoomTemplate createLockdownChamber() {
        RoomTemplate t = new RoomTemplate("LockdownChamber", 16, 16, true);
        t.addSocket(new Socket("lockdown_s", 7, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("lockdown_n", 7, 15, Direction.NORTH, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Flanking Slag Cauldrons at combat arena thresholds
        t.setTile(4, 8, TileType.PROP_SLAG_CAULDRON);
        t.setTile(11, 8, TileType.PROP_SLAG_CAULDRON);

        // Tactical perimeter workstation alcoves
        t.setTile(2, 7, TileType.PROP_FORGE_ANVIL);
        t.setTile(13, 7, TileType.PROP_FORGE_ANVIL);

        // Magma fissures & vents
        t.setTile(5, 5, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(10, 10, TileType.FLOOR_MAGMA_FISSURE);
        t.setTile(5, 10, TileType.FLOOR_MOLTEN_VENT);
        t.setTile(10, 5, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(5.5f, 5.5f);
        t.addHazard(10.5f, 10.5f);
        t.addHazard(5.5f, 10.5f);
        t.addHazard(10.5f, 5.5f);

        // 3 Hound spawns
        t.addEnemySpawn(5.0f, 8.0f);
        t.addEnemySpawn(11.0f, 8.0f);
        t.addEnemySpawn(8.0f, 12.0f);

        return t;
    }

    /**
     * 8. SanctuaryAirlock: 12x12 safe shop/rest zone between sectors.
     */
    public RoomTemplate createSanctuaryAirlock() {
        RoomTemplate t = new RoomTemplate("SanctuaryAirlock", 12, 12, false);
        t.addSocket(new Socket("sanctuary_s", 5, 0, Direction.SOUTH, SocketType.AIRLOCK, 2));
        t.addSocket(new Socket("sanctuary_n", 5, 11, Direction.NORTH, SocketType.AIRLOCK, 2));
        t.addSocket(new Socket("sanctuary_w", 0, 5, Direction.WEST, SocketType.AIRLOCK, 2));
        t.addSocket(new Socket("sanctuary_e", 11, 5, Direction.EAST, SocketType.AIRLOCK, 2));

        buildBaseRoom(t);

        // Central Sanctuary Chrono-Rune Dais (5,5 to 6,6)
        for (int x = 5; x <= 6; x++) {
            for (int y = 5; y <= 6; y++) {
                t.setTile(x, y, TileType.FLOOR_RUNE_CIRCLE);
            }
        }

        // Iron grate access runners
        t.setTile(5, 1, TileType.FLOOR_IRON_GRATE);
        t.setTile(6, 1, TileType.FLOOR_IRON_GRATE);
        t.setTile(5, 10, TileType.FLOOR_IRON_GRATE);
        t.setTile(6, 10, TileType.FLOOR_IRON_GRATE);
        t.setTile(1, 5, TileType.FLOOR_IRON_GRATE);
        t.setTile(1, 6, TileType.FLOOR_IRON_GRATE);
        t.setTile(10, 5, TileType.FLOOR_IRON_GRATE);
        t.setTile(10, 6, TileType.FLOOR_IRON_GRATE);

        // Chrono Chest & Chain Spools
        t.setTile(5, 9, TileType.PROP_CHRONO_CHEST);
        t.setTile(2, 5, TileType.PROP_CHAIN_SPOOL);
        t.setTile(9, 5, TileType.PROP_CHAIN_SPOOL);

        return t;
    }

    /**
     * 9. Antechamber: 20x18 grand hall leading to Gatekeeper Vault.
     */
    public RoomTemplate createAntechamber() {
        RoomTemplate t = new RoomTemplate("Antechamber", 20, 18, true);
        t.addSocket(new Socket("ante_s", 9, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("ante_n", 9, 17, Direction.NORTH, SocketType.VAULT_DOOR, 2));

        buildBaseRoom(t);

        // Central Processional Iron Grate Aisle leading to Boss Vault Door
        for (int y = 1; y <= 16; y++) {
            t.setTile(9, y, TileType.FLOOR_IRON_GRATE);
            t.setTile(10, y, TileType.FLOOR_IRON_GRATE);
        }

        // Processional Basalt Columns
        int[] pillars = {5, 5, 5, 12, 14, 5, 14, 12};
        for (int i = 0; i < pillars.length; i += 2) {
            int px = pillars[i];
            int py = pillars[i + 1];
            t.setTile(px, py, TileType.PILLAR_BASALT);
            t.addPillar(px + 0.5f, py + 0.5f);
        }

        // Perimeter Master Anvils flanking outer walls
        t.setTile(3, 8, TileType.PROP_FORGE_ANVIL);
        t.setTile(16, 8, TileType.PROP_FORGE_ANVIL);

        // Slag Cauldrons framing the Boss Vault Door
        t.setTile(6, 15, TileType.PROP_SLAG_CAULDRON);
        t.setTile(13, 15, TileType.PROP_SLAG_CAULDRON);

        // Enemy guardians
        t.addEnemySpawn(7.0f, 9.0f);
        t.addEnemySpawn(12.0f, 9.0f);
        t.addEnemySpawn(9.5f, 13.0f);

        return t;
    }

    /**
     * Builds standard enclosed room perimeter walls and alternating basalt floor,
     * leaving open floor tiles at all declared socket doorway locations.
     * North and East boundaries feature tall cast-iron fortress walls with furnace vents.
     * South and West boundaries feature low border curbs with Box2D static impact colliders,
     * preventing 2.5D visual occlusion into the room interior.
     */
    private void buildBaseRoom(RoomTemplate t) {
        int w = t.width;
        int h = t.height;

        // 1. Fill interior floor
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (x == 1 || y == 1 || x == w - 2 || y == h - 2) {
                    t.setTile(x, y, TileType.FLOOR_BORDER);
                } else {
                    t.setTile(x, y, ((x + y) % 2 == 0) ? TileType.FLOOR_BASALT_1 : TileType.FLOOR_BASALT_2);
                }
            }
        }

        // 2. Build Foreground Boundaries (South and West low curbs with Box2D colliders)
        // South boundary (y = 0)
        for (int x = 0; x < w; x++) {
            t.setTile(x, 0, TileType.BOUNDARY_LOW_CURB);
        }

        // West boundary (x = 0, y from 1 to h - 2)
        for (int y = 1; y < h - 1; y++) {
            t.setTile(0, y, TileType.BOUNDARY_LOW_CURB);
        }

        // 3. Build Back Walls (North and East facing camera)
        // North Wall (y = h - 1, x from 0 to w - 2)
        int furnaceNorthX = (w >= 14) ? (w / 2) : -1;
        for (int x = 0; x < w - 1; x++) {
            boolean isFurnace = (x == furnaceNorthX);
            t.setTile(x, h - 1, isFurnace ? TileType.WALL_FURNACE_VENT : TileType.WALL_CAST_IRON_NW);
            if (isFurnace) t.addFurnace(x + 0.5f, h - 0.5f);
        }

        // East Wall (x = w - 1, y from 1 to h - 2)
        int furnaceEastY = (h >= 14) ? (h / 2) : -1;
        for (int y = 1; y < h - 1; y++) {
            boolean isFurnace = (y == furnaceEastY);
            t.setTile(w - 1, y, isFurnace ? TileType.WALL_FURNACE_VENT : TileType.WALL_CAST_IRON_NE);
            if (isFurnace) t.addFurnace(w - 0.5f, y + 0.5f);
        }

        // Outer corner junction
        t.setTile(w - 1, h - 1, TileType.WALL_CORNER_OUTER);

        // 4. Clear open doorways at socket locations
        for (int i = 0; i < t.sockets.size; i++) {
            Socket s = t.sockets.get(i);
            for (int k = 0; k < s.clearanceWidth; k++) {
                int sx = s.x;
                int sy = s.y;
                if (s.direction == Direction.NORTH || s.direction == Direction.SOUTH) {
                    sx += k;
                } else {
                    sy += k;
                }
                t.setTile(sx, sy, TileType.FLOOR_BORDER);
            }
        }
    }
}
