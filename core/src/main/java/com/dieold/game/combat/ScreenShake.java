package com.dieold.game.combat;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Screen shake impulse generator for combat feedback.
 */
public class ScreenShake {
    private float shakeTime = 0f;
    private float totalDuration = 0f;
    private float intensity = 0f;
    private final Vector2 offset = new Vector2();

    public void addShake(float intensity, float duration) {
        if (intensity > this.intensity || shakeTime <= 0f) {
            this.intensity = intensity;
            this.shakeTime = duration;
            this.totalDuration = duration;
        }
    }

    public void update(float dt) {
        if (shakeTime > 0f) {
            shakeTime -= dt;
            float currentPower = intensity * (shakeTime / totalDuration);
            offset.x = MathUtils.random(-1f, 1f) * currentPower;
            offset.y = MathUtils.random(-1f, 1f) * currentPower;
            if (shakeTime <= 0f) {
                offset.setZero();
                intensity = 0f;
            }
        } else {
            offset.setZero();
        }
    }

    public Vector2 getOffset() {
        return offset;
    }

    public float getOffsetX() {
        return offset.x;
    }

    public float getOffsetY() {
        return offset.y;
    }

    public void reset() {
        shakeTime = 0f;
        totalDuration = 0f;
        intensity = 0f;
        offset.setZero();
    }
}
