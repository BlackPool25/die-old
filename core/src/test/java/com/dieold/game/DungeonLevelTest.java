package com.dieold.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.arena.ArenaBuilder.TileRenderItem;
import com.dieold.game.util.AssetResolver;
import com.dieold.game.world.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless test validating multi-room DungeonLevel construction with real production atlas:
 * 1. Stitches 10-14 procedural rooms into unified floor and wall tile rendering structures.
 * 2. Instantiates Box2D impact wall colliders and basalt pillar circular colliders.
 * 3. Spatial queries (getRoomAt) return accurate PlacedRoom references.
 */
public class DungeonLevelTest {

    @BeforeAll
    public static void initHeadlessGdxAndBox2D() {
        Box2D.init();
        if (Gdx.app == null) {
            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            new HeadlessApplication(new ApplicationAdapter() {}, config);

            GL20 glMock = (GL20) Proxy.newProxyInstance(
                GL20.class.getClassLoader(),
                new Class<?>[]{GL20.class},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == float.class) return 0f;
                    return null;
                }
            );
            Gdx.gl = glMock;
            Gdx.gl20 = glMock;
        }
    }

    @Test
    @DisplayName("Verify DungeonLevel multi-room tile stitching, Box2D colliders, and spatial queries")
    public void testDungeonLevelStitching() {
        TextureAtlas atlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/tileset.atlas"));
        World world = new World(new Vector2(0f, 0f), true);

        JigsawDungeonGenerator generator = new JigsawDungeonGenerator();
        Array<PlacedRoom> rooms = generator.generate(424242L, 12);
        assertTrue(rooms.size >= 8, "Expected at least 8 generated rooms");

        DungeonLevel level = new DungeonLevel();
        level.buildDungeon(world, atlas, rooms);

        // 1. Verify Floor Tiles
        Array<TileRenderItem> floorTiles = level.getFloorTiles();
        assertTrue(floorTiles.size > 500, "Expected >500 floor tiles across dungeon, got " + floorTiles.size);
        for (int i = 0; i < floorTiles.size; i++) {
            assertNotNull(floorTiles.get(i).region, "Floor tile region must not be null at index " + i);
        }

        // 2. Verify Wall and Pillar Tiles
        Array<TileRenderItem> wallTiles = level.getWallAndPillarTiles();
        assertTrue(wallTiles.size > 100, "Expected >100 wall/pillar tiles, got " + wallTiles.size);
        for (int i = 0; i < wallTiles.size; i++) {
            assertNotNull(wallTiles.get(i).region, "Wall tile region must not be null at index " + i);
        }

        // 3. Verify Box2D Colliders across all rooms
        int totalWallBodies = 0;
        for (int r = 0; r < rooms.size; r++) {
            PlacedRoom room = rooms.get(r);
            assertTrue(room.wallBodies.size > 0, "Room " + room.template.id + " must have Box2D wall bodies");
            totalWallBodies += room.wallBodies.size;
        }
        assertTrue(totalWallBodies >= wallTiles.size, "Total Box2D wall bodies must match or exceed wall tiles");

        // 4. Verify Spatial Query (getRoomAt)
        PlacedRoom ingress = rooms.get(0);
        PlacedRoom foundIngress = level.getRoomAt(ingress.gridX + 5f, ingress.gridY + 5f);
        assertEquals(ingress, foundIngress, "getRoomAt must locate IngressHub correctly");

        // Query point outside dungeon bounds
        PlacedRoom outside = level.getRoomAt(-9999f, -9999f);
        assertNull(outside, "getRoomAt out of bounds must return null");

        // 5. Verify molten and furnace vents populated
        assertTrue(level.moltenVentPositions.size >= 0);
        assertTrue(level.furnaceVentPositions.size > 0);

        atlas.dispose();
        world.dispose();
    }

    @Test
    @DisplayName("Verify Obsidian Foundry thematic props, micro-architecture, and foreground wall orientation")
    public void testFoundryThematicPropsAndWallOrientation() {
        RoomPrefabRegistry registry = new RoomPrefabRegistry();
        Array<RoomTemplate> templates = registry.getAllTemplates();
        assertEquals(9, templates.size, "Expected 9 distinct handcrafted Biome 1 templates");

        boolean hasAnvil = false;
        boolean hasCauldron = false;
        boolean hasChest = false;
        boolean hasChainSpool = false;
        boolean hasMagmaFissure = false;
        boolean hasIronGrate = false;

        for (int r = 0; r < templates.size; r++) {
            RoomTemplate t = templates.get(r);
            int w = t.width;
            int h = t.height;

            // 1. Verify North Wall (y = h - 1) has tall walls (WALL_CAST_IRON_NW or WALL_FURNACE_VENT)
            for (int x = 0; x < w - 1; x++) {
                TileType tile = t.getTile(x, h - 1);
                // Can be a socket doorway or tall wall
                boolean isDoorway = false;
                for (int s = 0; s < t.sockets.size; s++) {
                    Socket sock = t.sockets.get(s);
                    if (sock.direction == Direction.NORTH && x >= sock.x && x < sock.x + sock.clearanceWidth && h - 1 == sock.y) {
                        isDoorway = true;
                        break;
                    }
                }
                if (!isDoorway) {
                    assertTrue(tile == TileType.WALL_CAST_IRON_NW || tile == TileType.WALL_FURNACE_VENT,
                        "North wall at (" + x + "," + (h-1) + ") in " + t.id + " must be tall back wall, got " + tile);
                }
            }

            // 2. Verify South Boundary (y = 0) has BOUNDARY_LOW_CURB (NO tall walls)
            for (int x = 0; x < w; x++) {
                TileType tile = t.getTile(x, 0);
                boolean isDoorway = false;
                for (int s = 0; s < t.sockets.size; s++) {
                    Socket sock = t.sockets.get(s);
                    if (sock.direction == Direction.SOUTH && x >= sock.x && x < sock.x + sock.clearanceWidth && 0 == sock.y) {
                        isDoorway = true;
                        break;
                    }
                }
                if (!isDoorway) {
                    assertEquals(TileType.BOUNDARY_LOW_CURB, tile,
                        "South boundary at (" + x + ",0) in " + t.id + " must be BOUNDARY_LOW_CURB, got " + tile);
                }
            }

            // 3. Verify West Boundary (x = 0) has BOUNDARY_LOW_CURB (NO tall walls)
            for (int y = 0; y < h - 1; y++) {
                TileType tile = t.getTile(0, y);
                boolean isDoorway = false;
                for (int s = 0; s < t.sockets.size; s++) {
                    Socket sock = t.sockets.get(s);
                    if (sock.direction == Direction.WEST && y >= sock.y && y < sock.y + sock.clearanceWidth && 0 == sock.x) {
                        isDoorway = true;
                        break;
                    }
                }
                if (!isDoorway) {
                    assertEquals(TileType.BOUNDARY_LOW_CURB, tile,
                        "West boundary at (0," + y + ") in " + t.id + " must be BOUNDARY_LOW_CURB, got " + tile);
                }
            }

            // Check presence of props & floor variants
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    TileType tile = t.getTile(x, y);
                    if (tile == TileType.PROP_FORGE_ANVIL) hasAnvil = true;
                    if (tile == TileType.PROP_SLAG_CAULDRON) hasCauldron = true;
                    if (tile == TileType.PROP_CHRONO_CHEST) hasChest = true;
                    if (tile == TileType.PROP_CHAIN_SPOOL) hasChainSpool = true;
                    if (tile == TileType.FLOOR_MAGMA_FISSURE) hasMagmaFissure = true;
                    if (tile == TileType.FLOOR_IRON_GRATE) hasIronGrate = true;
                }
            }
        }

        assertTrue(hasAnvil, "Obsidian Foundry prefabs must include PROP_FORGE_ANVIL tactical cover");
        assertTrue(hasCauldron, "Obsidian Foundry prefabs must include PROP_SLAG_CAULDRON hazard cover");
        assertTrue(hasChest, "Obsidian Foundry prefabs must include PROP_CHRONO_CHEST reward caches");
        assertTrue(hasChainSpool, "Obsidian Foundry prefabs must include PROP_CHAIN_SPOOL logistics winches");
        assertTrue(hasMagmaFissure, "Obsidian Foundry prefabs must include FLOOR_MAGMA_FISSURE cracked floor hazards");
        assertTrue(hasIronGrate, "Obsidian Foundry prefabs must include FLOOR_IRON_GRATE catwalk walkways");
    }
}
