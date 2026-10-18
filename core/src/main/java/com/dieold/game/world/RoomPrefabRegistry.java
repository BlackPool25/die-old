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
     * 1. IngressHub: Safe spawn chamber with central Chrono-Rune circle (16x16).
     */
    public RoomTemplate createIngressHub() {
        RoomTemplate t = new RoomTemplate("IngressHub", 16, 16, false);
        t.addSocket(new Socket("ingress_n", 7, 15, Direction.NORTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("ingress_e", 15, 7, Direction.EAST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("ingress_s", 7, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("ingress_w", 0, 7, Direction.WEST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Central Chrono-Rune Circle (7,7 to 8,8)
        for (int x = 7; x <= 8; x++) {
            for (int y = 7; y <= 8; y++) {
                t.setTile(x, y, TileType.FLOOR_RUNE_CIRCLE);
            }
        }
        t.addPlayerSpawn(8.0f, 8.0f);
        return t;
    }

    /**
     * 2. CombatArenaSmall: 14x14 close-quarters encounter room.
     */
    public RoomTemplate createCombatArenaSmall() {
        RoomTemplate t = new RoomTemplate("CombatArenaSmall", 14, 14, true);
        t.addSocket(new Socket("arena_s_s", 6, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_s_n", 6, 13, Direction.NORTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_s_e", 13, 6, Direction.EAST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_s_w", 0, 6, Direction.WEST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Molten heat hazards
        t.setTile(4, 9, TileType.FLOOR_MOLTEN_VENT);
        t.setTile(9, 4, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(4.5f, 9.5f);
        t.addHazard(9.5f, 4.5f);

        // Enemy Hound spawns
        t.addEnemySpawn(4.5f, 4.5f);
        t.addEnemySpawn(9.5f, 9.5f);

        return t;
    }

    /**
     * 3. CombatArenaMedium: 20x20 tactical arena with 4 basalt pillars and molten vents.
     */
    public RoomTemplate createCombatArenaMedium() {
        RoomTemplate t = new RoomTemplate("CombatArenaMedium", 20, 20, true);
        t.addSocket(new Socket("arena_m_s", 9, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_m_n", 9, 19, Direction.NORTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_m_e", 19, 9, Direction.EAST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("arena_m_w", 0, 9, Direction.WEST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // 4 Basalt Columns
        int[] pillars = {5, 5, 5, 14, 14, 5, 14, 14};
        for (int i = 0; i < pillars.length; i += 2) {
            int px = pillars[i];
            int py = pillars[i + 1];
            t.setTile(px, py, TileType.PILLAR_BASALT);
            t.addPillar(px + 0.5f, py + 0.5f);
        }

        // Molten heat hazards
        int[] vents = {7, 9, 12, 9, 9, 7, 9, 12};
        for (int i = 0; i < vents.length; i += 2) {
            int vx = vents[i];
            int vy = vents[i + 1];
            t.setTile(vx, vy, TileType.FLOOR_MOLTEN_VENT);
            t.addHazard(vx + 0.5f, vy + 0.5f);
        }

        // Enemy spawns
        t.addEnemySpawn(5.5f, 10.0f);
        t.addEnemySpawn(14.5f, 10.0f);
        t.addEnemySpawn(10.0f, 14.5f);

        return t;
    }

    /**
     * 4. HazardConduitVertical: 10x20 elongated corridor lined with molten slag vents.
     */
    public RoomTemplate createHazardConduitVertical() {
        RoomTemplate t = new RoomTemplate("HazardConduitVertical", 10, 20, false);
        t.addSocket(new Socket("conduit_v_s", 4, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("conduit_v_n", 4, 19, Direction.NORTH, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Linear hazard sequence with safe stepping tiles
        int[] hazardY = {4, 7, 10, 13, 16};
        for (int hy : hazardY) {
            t.setTile(4, hy, TileType.FLOOR_MOLTEN_VENT);
            t.addHazard(4.5f, hy + 0.5f);
        }

        return t;
    }

    /**
     * 5. HazardConduitHorizontal: 20x10 elongated corridor lined with molten slag vents.
     */
    public RoomTemplate createHazardConduitHorizontal() {
        RoomTemplate t = new RoomTemplate("HazardConduitHorizontal", 20, 10, false);
        t.addSocket(new Socket("conduit_h_w", 0, 4, Direction.WEST, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("conduit_h_e", 19, 4, Direction.EAST, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Linear hazard sequence with safe stepping tiles
        int[] hazardX = {4, 7, 10, 13, 16};
        for (int hx : hazardX) {
            t.setTile(hx, 4, TileType.FLOOR_MOLTEN_VENT);
            t.addHazard(hx + 0.5f, 4.5f);
        }

        return t;
    }

    /**
     * 6. PuzzleAlcove: 14x14 age-gated chamber with internal divider wall & ventilation crawlspace.
     */
    public RoomTemplate createPuzzleAlcove() {
        RoomTemplate t = new RoomTemplate("PuzzleAlcove", 14, 14, false);
        t.addSocket(new Socket("puzzle_s", 6, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("puzzle_n", 6, 13, Direction.NORTH, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Internal divider wall at y = 7, leaving 1-tile crawlspace at x = 6
        for (int x = 1; x < 13; x++) {
            if (x != 6) { // 1-tile crawlspace opening
                t.setTile(x, 7, TileType.WALL_CAST_IRON_NW);
            }
        }

        // Secret alcove rune floor at (9, 10)
        t.setTile(9, 10, TileType.FLOOR_RUNE_CIRCLE);

        return t;
    }

    /**
     * 7. LockdownChamber: 16x16 enclosed arena with 2 portcullis combat gates.
     */
    public RoomTemplate createLockdownChamber() {
        RoomTemplate t = new RoomTemplate("LockdownChamber", 16, 16, true);
        t.addSocket(new Socket("lockdown_s", 7, 0, Direction.SOUTH, SocketType.CORRIDOR, 2));
        t.addSocket(new Socket("lockdown_n", 7, 15, Direction.NORTH, SocketType.CORRIDOR, 2));

        buildBaseRoom(t);

        // Molten vents flanking center
        t.setTile(4, 8, TileType.FLOOR_MOLTEN_VENT);
        t.setTile(11, 8, TileType.FLOOR_MOLTEN_VENT);
        t.addHazard(4.5f, 8.5f);
        t.addHazard(11.5f, 8.5f);

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

        // Decorative Chrono-Rune floor at center (5,5 to 6,6)
        for (int x = 5; x <= 6; x++) {
            for (int y = 5; y <= 6; y++) {
                t.setTile(x, y, TileType.FLOOR_RUNE_CIRCLE);
            }
        }

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

        // Processional Basalt Columns
        int[] pillars = {5, 5, 5, 12, 14, 5, 14, 12};
        for (int i = 0; i < pillars.length; i += 2) {
            int px = pillars[i];
            int py = pillars[i + 1];
            t.setTile(px, py, TileType.PILLAR_BASALT);
            t.addPillar(px + 0.5f, py + 0.5f);
        }

        // Enemy guardians
        t.addEnemySpawn(7.0f, 9.0f);
        t.addEnemySpawn(12.0f, 9.0f);
        t.addEnemySpawn(9.5f, 13.0f);

        return t;
    }

    /**
     * Builds standard enclosed room perimeter walls and alternating basalt floor,
     * leaving open floor tiles at all declared socket doorway locations.
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

        // 2. Build perimeter walls
        // North Wall (y = h - 1)
        for (int x = 0; x < w - 1; x++) {
            boolean isFurnace = (x % 4 == 2);
            t.setTile(x, h - 1, isFurnace ? TileType.WALL_FURNACE_VENT : TileType.WALL_CAST_IRON_NW);
            if (isFurnace) t.addFurnace(x + 0.5f, h - 0.5f);
        }

        // East Wall (x = w - 1)
        for (int y = 0; y < h - 1; y++) {
            boolean isFurnace = (y % 4 == 2);
            t.setTile(w - 1, y, isFurnace ? TileType.WALL_FURNACE_VENT : TileType.WALL_CAST_IRON_NE);
            if (isFurnace) t.addFurnace(w - 0.5f, y + 0.5f);
        }

        // Outer corner
        t.setTile(w - 1, h - 1, TileType.WALL_CORNER_OUTER);

        // South boundary
        for (int x = 0; x < w; x++) {
            t.setTile(x, 0, TileType.WALL_CAST_IRON_NW);
        }

        // West boundary
        for (int y = 0; y < h; y++) {
            t.setTile(0, y, TileType.WALL_CAST_IRON_NE);
        }

        // 3. Clear open doorways at socket locations
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
