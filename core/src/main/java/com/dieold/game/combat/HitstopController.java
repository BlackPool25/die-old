package com.dieold.game.combat;

/**
 * Manages frame freeze (hitstop) on visceral impacts and deflections.
 * During hitstop, Box2D simulation and animation frame ticks halt.
 */
public class HitstopController {
    public static final int FRAMES_NORMAL_HIT = 4;      // ~66ms
    public static final int FRAMES_DEFLECTION = 8;      // ~133ms
    public static final int FRAMES_GUARD_CRUSH = 12;    // ~200ms

    private float freezeTimer = 0f;

    public void triggerHitstop(int frames) {
        float duration = frames * (1f / 60f);
        if (duration > freezeTimer) {
            freezeTimer = duration;
        }
    }

    public void triggerSeconds(float seconds) {
        if (seconds > freezeTimer) {
            freezeTimer = seconds;
        }
    }

    /**
     * Updates hitstop countdown.
     * @param dt delta time in seconds
     * @return true if currently frozen in hitstop, false if running normally
     */
    public boolean update(float dt) {
        if (freezeTimer > 0f) {
            freezeTimer -= dt;
            return freezeTimer > 0f;
        }
        return false;
    }

    public boolean isFrozen() {
        return freezeTimer > 0f;
    }

    public float getRemaining() {
        return Math.max(0f, freezeTimer);
    }

    public void reset() {
        freezeTimer = 0f;
    }
}
