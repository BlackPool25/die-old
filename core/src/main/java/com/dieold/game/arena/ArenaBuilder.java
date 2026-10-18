package com.dieold.game.arena;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.physics.PhysicsConstants;

/**
 * Constructs an enclosed Obsidian Foundry arena with:
 * - Seamless North-West and North-East cast-iron backwalls with furnace heat vents.
 * - Basalt column pillars with circular collision footprints.
 * - Front obsidian perimeter drop ledge with Box2D impact boundaries for wall-slams.
 * - Central glowing Chrono-Rune circle and tactical molten slag vents.
 */
public class ArenaBuilder {

    public static class TileRenderItem {
        public float worldX;
        public float worldY;
        public float z = 0f;
        public float ySortOffset = 0f;
        public AtlasRegion region;
        public boolean isWallOrPillar = false;
        public float originX = 32f;
        public float originY = 16f;

        public TileRenderItem(float x, float y, float z, float ySortOffset, AtlasRegion region, boolean isWall, float origX, float origY) {
            this.worldX = x;
            this.worldY = y;
            this.z = z;
            this.ySortOffset = ySortOffset;
            this.region = region;
            this.isWallOrPillar = isWall;
            this.originX = origX;
            this.originY = origY;
        }
    }

    public static final int ARENA_SIZE = 22; // 22x22 grid meters (spacious tactical combat arena)

    private final Array<TileRenderItem> floorTiles = new Array<>(false, ARENA_SIZE * ARENA_SIZE);
    private final Array<TileRenderItem> wallAndPillarTiles = new Array<>(false, ARENA_SIZE * 2 + 16);
    private final Array<Body> wallBodies = new Array<>(false, ARENA_SIZE * 4 + 16);

    // Positions of special tiles for atmospheric lighting and particles
    public final Array<Vector2> moltenVentPositions = new Array<>(false, 16);
    public final Array<Vector2> furnaceVentPositions = new Array<>(false, 16);
    public final Vector2 runeCircleCenter = new Vector2(10.5f, 10.5f);

    public void buildArena(World world, TextureAtlas tilesetAtlas) {
        floorTiles.clear();
        wallAndPillarTiles.clear();
        wallBodies.clear();
        moltenVentPositions.clear();
        furnaceVentPositions.clear();

        AtlasRegion floorBasalt1 = tilesetAtlas.findRegion("floor_basalt_flagstone", 1);
        AtlasRegion floorBasalt2 = tilesetAtlas.findRegion("floor_basalt_flagstone", 2);
        if (floorBasalt1 == null) floorBasalt1 = tilesetAtlas.findRegion("floor_basalt_flagstone");
        if (floorBasalt2 == null) floorBasalt2 = floorBasalt1;

        AtlasRegion floorBorder = tilesetAtlas.findRegion("floor_obsidian_border");
        AtlasRegion floorVent = tilesetAtlas.findRegion("floor_molten_heat_vent");
        AtlasRegion floorRune = tilesetAtlas.findRegion("floor_rune_circle");

        AtlasRegion wallNW = tilesetAtlas.findRegion("wall_cast_iron_nw");
        AtlasRegion wallNE = tilesetAtlas.findRegion("wall_cast_iron_ne");
        AtlasRegion wallCorner = tilesetAtlas.findRegion("wall_corner_outer");
        AtlasRegion wallFurnace = tilesetAtlas.findRegion("wall_furnace_vent");
        AtlasRegion pillarBasalt = tilesetAtlas.findRegion("pillar_basalt_column");

        // 1. Generate 22x22 Floor Grid
        for (int x = 0; x < ARENA_SIZE; x++) {
            for (int y = 0; y < ARENA_SIZE; y++) {
                AtlasRegion region;

                // Central Chrono-Rune Circle (10,10 to 11,11)
                if ((x >= 10 && x <= 11) && (y >= 10 && y <= 11)) {
                    region = floorRune != null ? floorRune : floorBasalt1;
                }
                // Strategic Molten Slag Vents
                else if ((x == 6 && y == 10) || (x == 15 && y == 11) || (x == 10 && y == 6) || (x == 11 && y == 15)
                         || (x == 4 && y == 4) || (x == 17 && y == 17)) {
                    region = floorVent != null ? floorVent : floorBasalt1;
                    moltenVentPositions.add(new Vector2(x + 0.5f, y + 0.5f));
                }
                // Front / Outer Obsidian Borders
                else if (x == 0 || y == 0 || x == ARENA_SIZE - 2 || y == ARENA_SIZE - 2) {
                    region = floorBorder != null ? floorBorder : floorBasalt1;
                }
                // Alternating Basalt Flagstones
                else {
                    region = ((x + y) % 2 == 0) ? floorBasalt1 : floorBasalt2;
                }

                floorTiles.add(new TileRenderItem(x, y, 0f, 0f, region, false, 32f, 16f));
            }
        }

        // 2. Continuous North-West & North-East Fortress Backwalls
        BodyDef wallBodyDef = new BodyDef();
        wallBodyDef.type = BodyDef.BodyType.StaticBody;

        PolygonShape diamondBox = new PolygonShape();
        diamondBox.setAsBox(0.48f, 0.48f);

        FixtureDef wallFixDef = new FixtureDef();
        wallFixDef.shape = diamondBox;
        wallFixDef.friction = 0.5f;
        wallFixDef.restitution = 0.15f;
        wallFixDef.filter.categoryBits = PhysicsConstants.CATEGORY_WALL;
        wallFixDef.filter.maskBits = PhysicsConstants.MASK_WALL;

        int topY = ARENA_SIZE - 1;
        int rightX = ARENA_SIZE - 1;

        // North-West Wall (y = topY, x from 0 to rightX - 1)
        for (int x = 0; x < rightX; x++) {
            boolean isFurnace = (x % 4 == 2);
            AtlasRegion reg = isFurnace ? wallFurnace : wallNW;
            createWallTile(world, wallBodyDef, wallFixDef, x, topY, reg, 32f, 16f, 0.4f);
            if (isFurnace) {
                furnaceVentPositions.add(new Vector2(x + 0.5f, topY + 0.5f));
            }
        }

        // North-East Wall (x = rightX, y from 0 to topY - 1)
        for (int y = 0; y < topY; y++) {
            boolean isFurnace = (y % 4 == 2);
            AtlasRegion reg = isFurnace ? wallFurnace : wallNE;
            createWallTile(world, wallBodyDef, wallFixDef, rightX, y, reg, 32f, 16f, 0.4f);
            if (isFurnace) {
                furnaceVentPositions.add(new Vector2(rightX + 0.5f, y + 0.5f));
            }
        }

        // North Outer Corner where NW meets NE
        createWallTile(world, wallBodyDef, wallFixDef, rightX, topY, wallCorner, 32f, 16f, 0.4f);

        // 3. Front Perimeter Box2D Collision Bounds (South-West x=0 and South-East y=0)
        // Solid physics barrier along the front curb: ragdolls slam into it, but no tall sprites block the camera!
        for (int x = 0; x < ARENA_SIZE; x++) {
            createFrontCollisionBarrier(world, wallBodyDef, wallFixDef, x, 0);
        }
        for (int y = 1; y < ARENA_SIZE; y++) {
            createFrontCollisionBarrier(world, wallBodyDef, wallFixDef, 0, y);
        }

        // 4. Interior Basalt Pillars with Circular Colliders
        CircleShape pillarShape = new CircleShape();
        pillarShape.setRadius(0.58f);
        FixtureDef pillarFixDef = new FixtureDef();
        pillarFixDef.shape = pillarShape;
        pillarFixDef.friction = 0.5f;
        pillarFixDef.restitution = 0.2f;
        pillarFixDef.filter.categoryBits = PhysicsConstants.CATEGORY_WALL;
        pillarFixDef.filter.maskBits = PhysicsConstants.MASK_WALL;

        int[] pillarCoords = {6, 6, 6, 15, 15, 6, 15, 15};
        for (int p = 0; p < pillarCoords.length; p += 2) {
            int px = pillarCoords[p];
            int py = pillarCoords[p + 1];

            wallBodyDef.position.set(px + 0.5f, py + 0.5f);
            Body body = world.createBody(wallBodyDef);
            body.createFixture(pillarFixDef);
            body.setUserData("WALL");
            wallBodies.add(body);

            // Pillar height 96px, base footprint anchor at (32, 12)
            wallAndPillarTiles.add(new TileRenderItem(px + 0.5f, py + 0.5f, 0f, 0.45f, pillarBasalt, true, 32f, 12f));
        }

        diamondBox.dispose();
        pillarShape.dispose();
    }

    private void createWallTile(World world, BodyDef bodyDef, FixtureDef fixDef, float x, float y, AtlasRegion region, float ox, float oy, float ySort) {
        bodyDef.position.set(x + 0.5f, y + 0.5f);
        Body body = world.createBody(bodyDef);
        body.createFixture(fixDef);
        body.setUserData("WALL");
        wallBodies.add(body);

        wallAndPillarTiles.add(new TileRenderItem(x + 0.5f, y + 0.5f, 0f, ySort, region, true, ox, oy));
    }

    private void createFrontCollisionBarrier(World world, BodyDef bodyDef, FixtureDef fixDef, float x, float y) {
        bodyDef.position.set(x + 0.5f, y + 0.5f);
        Body body = world.createBody(bodyDef);
        body.createFixture(fixDef);
        body.setUserData("WALL");
        wallBodies.add(body);
    }

    public Array<TileRenderItem> getFloorTiles() {
        return floorTiles;
    }

    public Array<TileRenderItem> getWallAndPillarTiles() {
        return wallAndPillarTiles;
    }
}
