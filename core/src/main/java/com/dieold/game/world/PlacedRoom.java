package com.dieold.game.world;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.dieold.game.render.IsometricProjection;

/**
 * An instantiated chamber placed into the global dungeon grid:
 * Tracks physical bounding box, Box2D collision bodies, combat lockdown state,
 * enemy roster, fog-of-war discovery alpha, and clamped isometric screen bounds.
 */
public class PlacedRoom {

    public enum RoomState {
        UNVISITED,
        DISCOVERED,
        ACTIVE_LOCKDOWN,
        CLEARED
    }

    public final RoomTemplate template;
    public final int gridX;
    public final int gridY;
    public final Rectangle bounds;
    public final Array<PlacedSocket> sockets = new Array<>(false, 8);

    public final Array<Body> wallBodies = new Array<>(false, 64);
    public final Array<Body> gateBodies = new Array<>(false, 8);
    public final Array<Entity> activeEnemies = new Array<>(false, 8);

    public RoomState state = RoomState.UNVISITED;
    public float discoveryAlpha = 0.0f; // 0.0f (deep darkness #040406) -> 1.0f (full visual reveal)

    // Pre-calculated dimetric screen bounding box for camera clamping
    public float minScreenX;
    public float maxScreenX;
    public float minScreenY;
    public float maxScreenY;

    private final Vector2 tempCorner = new Vector2();

    public PlacedRoom(RoomTemplate template, int gridX, int gridY) {
        this.template = template;
        this.gridX = gridX;
        this.gridY = gridY;
        this.bounds = new Rectangle(gridX, gridY, template.width, template.height);

        // Instantiate sockets
        for (int i = 0; i < template.sockets.size; i++) {
            Socket s = template.sockets.get(i);
            sockets.add(new PlacedSocket(s, this, gridX + s.x, gridY + s.y));
        }

        // Safe initial state for non-combat rooms
        if (!template.isCombatRoom) {
            this.state = RoomState.CLEARED;
        }

        calculateScreenBounds();
    }

    /**
     * Calculates the diamond footprint screen bounding box in 2:1 dimetric projection:
     * Evaluates all 4 corners of the room in world space to find min/max screen bounds.
     */
    public void calculateScreenBounds() {
        float x0 = gridX;
        float y0 = gridY;
        float x1 = gridX + template.width;
        float y1 = gridY + template.height;

        // Corner 0: (x0, y0) South
        IsometricProjection.worldGroundToScreen(x0, y0, tempCorner);
        minScreenX = tempCorner.x;
        maxScreenX = tempCorner.x;
        minScreenY = tempCorner.y;
        maxScreenY = tempCorner.y;

        // Corner 1: (x1, y0) East
        IsometricProjection.worldGroundToScreen(x1, y0, tempCorner);
        minScreenX = Math.min(minScreenX, tempCorner.x);
        maxScreenX = Math.max(maxScreenX, tempCorner.x);
        minScreenY = Math.min(minScreenY, tempCorner.y);
        maxScreenY = Math.max(maxScreenY, tempCorner.y);

        // Corner 2: (x0, y1) West
        IsometricProjection.worldGroundToScreen(x0, y1, tempCorner);
        minScreenX = Math.min(minScreenX, tempCorner.x);
        maxScreenX = Math.max(maxScreenX, tempCorner.x);
        minScreenY = Math.min(minScreenY, tempCorner.y);
        maxScreenY = Math.max(maxScreenY, tempCorner.y);

        // Corner 3: (x1, y1) North
        IsometricProjection.worldGroundToScreen(x1, y1, tempCorner);
        minScreenX = Math.min(minScreenX, tempCorner.x);
        maxScreenX = Math.max(maxScreenX, tempCorner.x);
        minScreenY = Math.min(minScreenY, tempCorner.y);
        maxScreenY = Math.max(maxScreenY, tempCorner.y);
    }

    public boolean contains(float worldX, float worldY) {
        return bounds.contains(worldX, worldY);
    }

    public Vector2 getCenter(Vector2 out) {
        out.set(gridX + template.width * 0.5f, gridY + template.height * 0.5f);
        return out;
    }

    public boolean isCleared() {
        return state == RoomState.CLEARED;
    }

    public boolean isLockdownActive() {
        return state == RoomState.ACTIVE_LOCKDOWN;
    }

    @Override
    public String toString() {
        return "PlacedRoom{" +
            "id='" + template.id + '\'' +
            ", bounds=" + bounds +
            ", state=" + state +
            '}';
    }
}
