package com.dieold.game.world;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.arena.ArenaBuilder;
import com.dieold.game.physics.PhysicsConstants;

/**
 * Procedural Multi-Room Dungeon Level:
 * Extends ArenaBuilder to seamlessly integrate with RenderSystem,
 * while managing multi-room tile stitching, Box2D impact wall colliders,
 * basalt column circular colliders, portcullis lockdown gates, and spatial queries.
 */
public class DungeonLevel extends ArenaBuilder {

    private final Array<PlacedRoom> rooms = new Array<>(false, 32);
    private World box2DWorld;
    private TextureAtlas tilesetAtlas;

    private AtlasRegion floorBasalt1;
    private AtlasRegion floorBasalt2;
    private AtlasRegion floorBorder;
    private AtlasRegion floorVent;
    private AtlasRegion floorRune;
    private AtlasRegion floorMagmaFissure;
    private AtlasRegion floorIronGrate;
    private AtlasRegion wallNW;
    private AtlasRegion wallNE;
    private AtlasRegion wallCorner;
    private AtlasRegion wallFurnace;
    private AtlasRegion pillarBasalt;
    private AtlasRegion propAnvil;
    private AtlasRegion propCauldron;
    private AtlasRegion propChest;
    private AtlasRegion propChainSpool;

    public DungeonLevel() {
        super();
    }

    public void buildDungeon(World world, TextureAtlas atlas, Array<PlacedRoom> placedRooms) {
        this.box2DWorld = world;
        this.tilesetAtlas = atlas;
        this.rooms.clear();
        this.rooms.addAll(placedRooms);

        getFloorTiles().clear();
        getWallAndPillarTiles().clear();
        moltenVentPositions.clear();
        furnaceVentPositions.clear();

        // 1. Resolve Atlas Regions
        floorBasalt1 = atlas.findRegion("floor_basalt_flagstone", 1);
        floorBasalt2 = atlas.findRegion("floor_basalt_flagstone", 2);
        if (floorBasalt1 == null) floorBasalt1 = atlas.findRegion("floor_basalt_flagstone");
        if (floorBasalt2 == null) floorBasalt2 = floorBasalt1;

        floorBorder = atlas.findRegion("floor_obsidian_border");
        floorVent = atlas.findRegion("floor_molten_heat_vent");
        floorRune = atlas.findRegion("floor_rune_circle");
        floorMagmaFissure = atlas.findRegion("floor_magma_fissure");
        if (floorMagmaFissure == null) floorMagmaFissure = floorVent;
        floorIronGrate = atlas.findRegion("floor_iron_grate");
        if (floorIronGrate == null) floorIronGrate = floorBorder;

        wallNW = atlas.findRegion("wall_cast_iron_nw");
        wallNE = atlas.findRegion("wall_cast_iron_ne");
        wallCorner = atlas.findRegion("wall_corner_outer");
        wallFurnace = atlas.findRegion("wall_furnace_vent");
        pillarBasalt = atlas.findRegion("pillar_basalt_column");

        propAnvil = atlas.findRegion("prop_forge_anvil");
        if (propAnvil == null) propAnvil = pillarBasalt;
        propCauldron = atlas.findRegion("prop_slag_cauldron");
        if (propCauldron == null) propCauldron = pillarBasalt;
        propChest = atlas.findRegion("prop_chrono_chest");
        if (propChest == null) propChest = pillarBasalt;
        propChainSpool = atlas.findRegion("prop_chain_spool");
        if (propChainSpool == null) propChainSpool = pillarBasalt;

        // 2. Box2D Shared Physics Definitions
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

        CircleShape pillarShape = new CircleShape();
        pillarShape.setRadius(0.58f);

        FixtureDef pillarFixDef = new FixtureDef();
        pillarFixDef.shape = pillarShape;
        pillarFixDef.friction = 0.5f;
        pillarFixDef.restitution = 0.2f;
        pillarFixDef.filter.categoryBits = PhysicsConstants.CATEGORY_WALL;
        pillarFixDef.filter.maskBits = PhysicsConstants.MASK_WALL;

        CircleShape propShape = new CircleShape();
        propShape.setRadius(0.42f);

        FixtureDef propFixDef = new FixtureDef();
        propFixDef.shape = propShape;
        propFixDef.friction = 0.5f;
        propFixDef.restitution = 0.15f;
        propFixDef.filter.categoryBits = PhysicsConstants.CATEGORY_WALL;
        propFixDef.filter.maskBits = PhysicsConstants.MASK_WALL;

        // 3. Stitch Each Placed Room
        for (int r = 0; r < rooms.size; r++) {
            PlacedRoom room = rooms.get(r);
            stitchRoom(room, wallBodyDef, wallFixDef, pillarShape, pillarFixDef, propFixDef);
        }

        diamondBox.dispose();
        pillarShape.dispose();
        propShape.dispose();
    }

    private void stitchRoom(
        PlacedRoom room,
        BodyDef wallBodyDef, FixtureDef wallFixDef,
        CircleShape pillarShape, FixtureDef pillarFixDef,
        FixtureDef propFixDef
    ) {
        RoomTemplate t = room.template;
        int rx = room.gridX;
        int ry = room.gridY;
        int rw = t.width;
        int rh = t.height;

        for (int lx = 0; lx < rw; lx++) {
            for (int ly = 0; ly < rh; ly++) {
                TileType tile = t.getTile(lx, ly);
                int gx = rx + lx;
                int gy = ry + ly;

                // Check if this cell is a socket doorway
                PlacedSocket socketAtCell = getSocketAt(room, gx, gy);

                if (socketAtCell != null) {
                    if (socketAtCell.isCapped) {
                        // Capped Socket: Place solid wall on North/East, low curb on South/West
                        if (socketAtCell.socket.direction == Direction.NORTH || socketAtCell.socket.direction == Direction.EAST) {
                            AtlasRegion reg = (socketAtCell.socket.direction == Direction.NORTH) ? wallNW : wallNE;
                            createWallTile(room, wallBodyDef, wallFixDef, gx, gy, reg, 32f, 16f, 0.4f);
                        } else {
                            getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBorder != null ? floorBorder : floorBasalt1, false, 32f, 16f));
                            createBoundaryCollider(room, wallBodyDef, wallFixDef, gx, gy);
                        }
                        continue;
                    } else {
                        // Open Connected Socket: Place seamless open floor tile
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBorder != null ? floorBorder : floorBasalt1, false, 32f, 16f));
                        continue;
                    }
                }

                // Standard Room Tiles
                switch (tile) {
                    case FLOOR_BASALT_1 ->
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBasalt1, false, 32f, 16f));
                    case FLOOR_BASALT_2 ->
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBasalt2, false, 32f, 16f));
                    case FLOOR_BORDER ->
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBorder != null ? floorBorder : floorBasalt1, false, 32f, 16f));
                    case FLOOR_RUNE_CIRCLE ->
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorRune != null ? floorRune : floorBasalt1, false, 32f, 16f));
                    case FLOOR_MAGMA_FISSURE -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorMagmaFissure, false, 32f, 16f));
                        moltenVentPositions.add(new Vector2(gx + 0.5f, gy + 0.5f));
                    }
                    case FLOOR_IRON_GRATE ->
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorIronGrate, false, 32f, 16f));
                    case FLOOR_MOLTEN_VENT -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorVent != null ? floorVent : floorBasalt1, false, 32f, 16f));
                        moltenVentPositions.add(new Vector2(gx + 0.5f, gy + 0.5f));
                    }
                    case BOUNDARY_LOW_CURB -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBorder != null ? floorBorder : floorBasalt1, false, 32f, 16f));
                        createBoundaryCollider(room, wallBodyDef, wallFixDef, gx, gy);
                    }
                    case WALL_CAST_IRON_NW ->
                        createWallTile(room, wallBodyDef, wallFixDef, gx, gy, wallNW, 32f, 16f, 0.4f);
                    case WALL_CAST_IRON_NE ->
                        createWallTile(room, wallBodyDef, wallFixDef, gx, gy, wallNE, 32f, 16f, 0.4f);
                    case WALL_CORNER_OUTER ->
                        createWallTile(room, wallBodyDef, wallFixDef, gx, gy, wallCorner, 32f, 16f, 0.4f);
                    case WALL_FURNACE_VENT -> {
                        createWallTile(room, wallBodyDef, wallFixDef, gx, gy, wallFurnace, 32f, 16f, 0.4f);
                        furnaceVentPositions.add(new Vector2(gx + 0.5f, gy + 0.5f));
                    }
                    case PILLAR_BASALT -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBasalt1, false, 32f, 16f));
                        wallBodyDef.position.set(gx + 0.5f, gy + 0.5f);
                        Body pBody = box2DWorld.createBody(wallBodyDef);
                        pBody.createFixture(pillarFixDef);
                        pBody.setUserData("WALL");
                        room.wallBodies.add(pBody);
                        getWallAndPillarTiles().add(new TileRenderItem(gx + 0.5f, gy + 0.5f, 0f, 0.45f, pillarBasalt, true, 32f, 12f));
                    }
                    case PROP_FORGE_ANVIL -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBasalt1, false, 32f, 16f));
                        createWallTile(room, wallBodyDef, propFixDef, gx, gy, propAnvil, 32f, 16f, 0.35f);
                    }
                    case PROP_SLAG_CAULDRON -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBasalt1, false, 32f, 16f));
                        createWallTile(room, wallBodyDef, propFixDef, gx, gy, propCauldron, 32f, 16f, 0.35f);
                        moltenVentPositions.add(new Vector2(gx + 0.5f, gy + 0.5f));
                    }
                    case PROP_CHRONO_CHEST -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBasalt1, false, 32f, 16f));
                        createWallTile(room, wallBodyDef, propFixDef, gx, gy, propChest, 32f, 16f, 0.35f);
                    }
                    case PROP_CHAIN_SPOOL -> {
                        getFloorTiles().add(new TileRenderItem(gx, gy, 0f, 0f, floorBasalt1, false, 32f, 16f));
                        createWallTile(room, wallBodyDef, propFixDef, gx, gy, propChainSpool, 32f, 16f, 0.35f);
                    }
                    default -> {}
                }
            }
        }
    }

    private void createBoundaryCollider(PlacedRoom room, BodyDef bodyDef, FixtureDef fixDef, float x, float y) {
        bodyDef.position.set(x + 0.5f, y + 0.5f);
        Body body = box2DWorld.createBody(bodyDef);
        body.createFixture(fixDef);
        body.setUserData("WALL");
        room.wallBodies.add(body);
    }

    private void createWallTile(
        PlacedRoom room,
        BodyDef bodyDef, FixtureDef fixDef,
        float x, float y, AtlasRegion region, float ox, float oy, float ySort
    ) {
        bodyDef.position.set(x + 0.5f, y + 0.5f);
        Body body = box2DWorld.createBody(bodyDef);
        body.createFixture(fixDef);
        body.setUserData("WALL");
        room.wallBodies.add(body);

        getWallAndPillarTiles().add(new TileRenderItem(x + 0.5f, y + 0.5f, 0f, ySort, region, true, ox, oy));
    }

    private PlacedSocket getSocketAt(PlacedRoom room, int worldX, int worldY) {
        for (int s = 0; s < room.sockets.size; s++) {
            PlacedSocket ps = room.sockets.get(s);
            int sw = ps.socket.clearanceWidth;
            for (int k = 0; k < sw; k++) {
                int sx = ps.worldX;
                int sy = ps.worldY;
                if (ps.socket.direction == Direction.NORTH || ps.socket.direction == Direction.SOUTH) {
                    sx += k;
                } else {
                    sy += k;
                }
                if (sx == worldX && sy == worldY) {
                    return ps;
                }
            }
        }
        return null;
    }

    /**
     * Spatial query: returns the PlacedRoom containing the given world position.
     */
    public PlacedRoom getRoomAt(float worldX, float worldY) {
        for (int i = 0; i < rooms.size; i++) {
            PlacedRoom r = rooms.get(i);
            if (r.contains(worldX, worldY)) {
                return r;
            }
        }
        return null;
    }

    public Array<PlacedRoom> getRooms() {
        return rooms;
    }

    public World getBox2DWorld() {
        return box2DWorld;
    }

    public TextureAtlas getTilesetAtlas() {
        return tilesetAtlas;
    }
}
