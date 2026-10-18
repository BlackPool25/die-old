package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Discrete 8-directional facing angle (0° East to 315° South-East) and continuous angle.
 * 0: East (0°)
 * 1: North-East (45°)
 * 2: North (90°)
 * 3: North-West (135°)
 * 4: West (180°)
 * 5: South-West (225°)
 * 6: South (270°)
 * 7: South-East (315°)
 */
public class DirectionComponent implements Component, Poolable {
    public float angleDegrees = 0f;       // World angle (degrees)
    public float screenAngleDegrees = 0f; // Screen angle (degrees, 0° Right, 90° Up)
    public int directionIndex = 3;        // Default facing East (dir3)

    /**
     * Sets direction directly from a screen-space angle (0° = Right, 90° = Up, 180° = Left, 270° = Down).
     * Maps to baked atlas indices:
     * 0° (East) -> dir3, 45° (North-East) -> dir4, 90° (North) -> dir5, 135° (North-West) -> dir6,
     * 180° (West) -> dir7, 225° (South-West) -> dir0, 270° (South) -> dir1, 315° (South-East) -> dir2.
     */
    public void setFromScreenAngle(float screenDegrees) {
        float normalized = (screenDegrees % 360f + 360f) % 360f;
        this.screenAngleDegrees = normalized;
        int octant = ((int) Math.floor((normalized + 22.5f) / 45f)) % 8;
        this.directionIndex = (octant + 3) % 8;
    }

    /**
     * Sets direction from a 2D screen movement vector (screenDX, screenDY).
     */
    public void setFromScreenVector(float screenDX, float screenDY) {
        if (MathUtils.isZero(screenDX) && MathUtils.isZero(screenDY)) {
            return;
        }
        float deg = MathUtils.radiansToDegrees * MathUtils.atan2(screenDY, screenDX);
        setFromScreenAngle(deg);
    }

    /**
     * Sets direction from a world-space movement vector (worldDX, worldDY) by projecting to screen coordinates.
     */
    public void setFromWorldVector(float worldDX, float worldDY) {
        if (MathUtils.isZero(worldDX) && MathUtils.isZero(worldDY)) {
            return;
        }
        this.angleDegrees = (MathUtils.radiansToDegrees * MathUtils.atan2(worldDY, worldDX) % 360f + 360f) % 360f;

        // Dimetric projection from world delta to screen delta:
        // ScreenX = (dx - dy) * X_STEP
        // ScreenY = (dx + dy) * Y_STEP
        float screenDX = (worldDX - worldDY) * 27.712812f;
        float screenDY = (worldDX + worldDY) * 16.0f;
        setFromScreenVector(screenDX, screenDY);
    }

    public void setFromWorldVector(Vector2 worldDir) {
        setFromWorldVector(worldDir.x, worldDir.y);
    }

    /**
     * Sets direction from world angle (degrees, where 0° is +X East, 90° is +Y North in world).
     */
    public void setFromAngle(float worldDegrees) {
        float rad = worldDegrees * MathUtils.degreesToRadians;
        setFromWorldVector(MathUtils.cos(rad), MathUtils.sin(rad));
    }

    public void setFromVector(float dx, float dy) {
        setFromWorldVector(dx, dy);
    }

    public void setFromVector(Vector2 dir) {
        setFromWorldVector(dir.x, dir.y);
    }

    @Override
    public void reset() {
        angleDegrees = 0f;
        screenAngleDegrees = 0f;
        directionIndex = 3;
    }
}
