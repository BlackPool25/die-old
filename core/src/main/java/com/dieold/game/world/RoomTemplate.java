package com.dieold.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Handcrafted architectural blueprint for a procedural chamber:
 * Grid dimensions (W x H), 2D tile layout, Box2D collision shapes, spawn anchors, and connection sockets.
 */
public class RoomTemplate {
    public final String id;
    public final int width;
    public final int height;
    public final TileType[][] tiles;
    public final Array<Socket> sockets = new Array<>(false, 8);
    public final Array<Vector2> playerSpawns = new Array<>(false, 4);
    public final Array<Vector2> enemySpawns = new Array<>(false, 8);
    public final Array<Vector2> hazardPositions = new Array<>(false, 16);
    public final Array<Vector2> furnacePositions = new Array<>(false, 8);
    public final Array<Vector2> pillarPositions = new Array<>(false, 8);
    public final boolean isCombatRoom;

    public RoomTemplate(String id, int width, int height, boolean isCombatRoom) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.isCombatRoom = isCombatRoom;
        this.tiles = new TileType[width][height];

        // Initialize all tiles to EMPTY
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = TileType.EMPTY;
            }
        }
    }

    public void setTile(int x, int y, TileType type) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            tiles[x][y] = type;
        }
    }

    public TileType getTile(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return tiles[x][y];
        }
        return TileType.EMPTY;
    }

    public void addSocket(Socket socket) {
        sockets.add(socket);
    }

    public void addPlayerSpawn(float x, float y) {
        playerSpawns.add(new Vector2(x, y));
    }

    public void addEnemySpawn(float x, float y) {
        enemySpawns.add(new Vector2(x, y));
    }

    public void addHazard(float x, float y) {
        hazardPositions.add(new Vector2(x, y));
    }

    public void addFurnace(float x, float y) {
        furnacePositions.add(new Vector2(x, y));
    }

    public void addPillar(float x, float y) {
        pillarPositions.add(new Vector2(x, y));
    }

    public Rectangle getBounds(Rectangle out) {
        return out.set(0, 0, width, height);
    }

    @Override
    public String toString() {
        return "RoomTemplate{" +
            "id='" + id + '\'' +
            ", size=" + width + "x" + height +
            ", sockets=" + sockets.size +
            ", isCombatRoom=" + isCombatRoom +
            '}';
    }
}
