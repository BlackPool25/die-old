package com.dieold.game.world;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.dieold.game.render.IsometricProjection;

/**
 * Chamber Camera Tracking & Bounds Clamping System:
 * Smoothly tracks target (Player) in 2.5D dimetric space,
 * clamping camera (X, Y) strictly within the active chamber's diamond screen bounding box
 * to prevent revealing empty void borders.
 * Smoothly lerps clamp bounds across corridors and airlocks during room transitions.
 */
public class ChamberCameraSystem {

    private final OrthographicCamera camera;
    private final DungeonLevel dungeonLevel;

    private PlacedRoom currentRoom = null;

    // Current interpolated screen clamp bounds
    public float currentMinX;
    public float currentMaxX;
    public float currentMinY;
    public float currentMaxY;

    // Target chamber bounds
    public float targetMinX;
    public float targetMaxX;
    public float targetMinY;
    public float targetMaxY;

    private final Vector2 tempPlayerScreen = new Vector2();
    private boolean initialized = false;

    public float lerpRate = 5.0f; // Smooth transition speed between room bounds
    public float cameraFollowSpeed = 6.0f; // Camera smooth follow factor

    public ChamberCameraSystem(OrthographicCamera camera, DungeonLevel dungeonLevel) {
        this.camera = camera;
        this.dungeonLevel = dungeonLevel;
    }

    public void setRoom(PlacedRoom room, boolean immediate) {
        if (room == null) return;
        this.currentRoom = room;

        targetMinX = room.minScreenX;
        targetMaxX = room.maxScreenX;
        targetMinY = room.minScreenY;
        targetMaxY = room.maxScreenY;

        if (immediate || !initialized) {
            currentMinX = targetMinX;
            currentMaxX = targetMaxX;
            currentMinY = targetMinY;
            currentMaxY = targetMaxY;
            initialized = true;
        }
    }

    public void update(float deltaTime, float targetWorldX, float targetWorldY, float targetWorldZ) {
        // 1. Detect room under target
        PlacedRoom detectedRoom = dungeonLevel.getRoomAt(targetWorldX, targetWorldY);
        if (detectedRoom != null && detectedRoom != currentRoom) {
            setRoom(detectedRoom, false);
        }

        // 2. Interpolate camera screen bounds towards target chamber
        float alpha = Math.min(1.0f, deltaTime * lerpRate);
        currentMinX = MathUtils.lerp(currentMinX, targetMinX, alpha);
        currentMaxX = MathUtils.lerp(currentMaxX, targetMaxX, alpha);
        currentMinY = MathUtils.lerp(currentMinY, targetMinY, alpha);
        currentMaxY = MathUtils.lerp(currentMaxY, targetMaxY, alpha);

        // 3. Project player world position to screen space
        IsometricProjection.worldToScreen(targetWorldX, targetWorldY, targetWorldZ, tempPlayerScreen);

        // 4. Calculate clamped target camera position
        float halfW = camera.viewportWidth * 0.5f * camera.zoom;
        float halfH = camera.viewportHeight * 0.5f * camera.zoom;

        float clampedX;
        if ((currentMaxX - currentMinX) > (2f * halfW)) {
            clampedX = MathUtils.clamp(tempPlayerScreen.x, currentMinX + halfW, currentMaxX - halfW);
        } else {
            clampedX = (currentMinX + currentMaxX) * 0.5f;
        }

        float clampedY;
        if ((currentMaxY - currentMinY) > (2f * halfH)) {
            clampedY = MathUtils.clamp(tempPlayerScreen.y, currentMinY + halfH, currentMaxY - halfH);
        } else {
            clampedY = (currentMinY + currentMaxY) * 0.5f;
        }

        // 5. Smooth camera follow
        float followAlpha = Math.min(1.0f, deltaTime * cameraFollowSpeed);
        camera.position.x = MathUtils.lerp(camera.position.x, clampedX, followAlpha);
        camera.position.y = MathUtils.lerp(camera.position.y, clampedY, followAlpha);
    }

    public PlacedRoom getCurrentRoom() {
        return currentRoom;
    }

    /**
     * Helper for unit testing clamping math with arbitrary bounds and viewport.
     */
    public static Vector2 calculateClampedCamera(
        float screenX, float screenY,
        float minX, float maxX, float minY, float maxY,
        float viewportW, float viewportH
    ) {
        float halfW = viewportW * 0.5f;
        float halfH = viewportH * 0.5f;

        float cx = ((maxX - minX) > (2f * halfW)) ? MathUtils.clamp(screenX, minX + halfW, maxX - halfW) : (minX + maxX) * 0.5f;
        float cy = ((maxY - minY) > (2f * halfH)) ? MathUtils.clamp(screenY, minY + halfH, maxY - halfH) : (minY + maxY) * 0.5f;

        return new Vector2(cx, cy);
    }
}
