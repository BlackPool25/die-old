package com.dieold.game.render;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * 2.5D Dimetric Isometric Projection:
 * ScreenX = (x - y) * 32 * cos(30°)
 * ScreenY = (x + y) * 16 + z * 32
 * Texel density: 32 pixels per world meter.
 */
public final class IsometricProjection {
    public static final float PPM = 32.0f; // Pixels per meter
    public static final float COS_30 = MathUtils.cosDeg(30f); // ~0.8660254f
    public static final float X_STEP = PPM * COS_30; // ~27.712812f
    public static final float Y_STEP = 16.0f;
    public static final float Z_STEP = PPM; // 32.0f

    private IsometricProjection() {}

    /**
     * Converts world (x, y, z) in meters to screen coordinates in pixels.
     */
    public static void worldToScreen(float x, float y, float z, Vector2 out) {
        out.x = (x - y) * X_STEP;
        out.y = (x + y) * Y_STEP + z * Z_STEP;
    }

    /**
     * Converts world ground plane (x, y, 0) in meters to screen shadow coordinates.
     */
    public static void worldGroundToScreen(float x, float y, Vector2 out) {
        out.x = (x - y) * X_STEP;
        out.y = (x + y) * Y_STEP;
    }

    /**
     * Converts screen coordinates in pixels to world ground plane (x, y) with z=0.
     */
    public static void screenToWorld(float screenX, float screenY, Vector2 out) {
        float xMinusY = screenX / X_STEP;
        float xPlusY = screenY / Y_STEP;
        out.x = 0.5f * (xPlusY + xMinusY);
        out.y = 0.5f * (xPlusY - xMinusY);
    }

    /**
     * Calculates baseline depth sorting key for painter's algorithm.
     * Higher key is drawn first (further in background), lower key drawn later (foreground).
     */
    public static float calculateDepthKey(float x, float y, float z, float ySortOffset) {
        return (x + y) + ySortOffset - (z * 0.05f);
    }
}
