package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Poise meter, posture break stun, and regeneration timers.
 */
public class PoiseComponent implements Component, Poolable {
    public float maxPoise = 100f;
    public float currentPoise = 100f;
    public float poiseBreakTimer = 0f;
    public float recoveryRate = 25f; // poise recovered per second
    public float recoveryDelay = 1.5f; // seconds without damage before recovery begins
    public float recoveryDelayTimer = 0f;
    public boolean isBroken = false;

    public void damagePoise(float amount) {
        recoveryDelayTimer = recoveryDelay;
        currentPoise = Math.max(0f, currentPoise - amount);
        if (currentPoise <= 0f) {
            isBroken = true;
            poiseBreakTimer = 1.5f; // default break stun duration
        }
    }

    public void breakPoise(float duration) {
        currentPoise = 0f;
        isBroken = true;
        poiseBreakTimer = duration;
        recoveryDelayTimer = recoveryDelay;
    }

    public void restoreFull() {
        currentPoise = maxPoise;
        isBroken = false;
        poiseBreakTimer = 0f;
        recoveryDelayTimer = 0f;
    }

    @Override
    public void reset() {
        maxPoise = 100f;
        currentPoise = 100f;
        poiseBreakTimer = 0f;
        recoveryRate = 25f;
        recoveryDelay = 1.5f;
        recoveryDelayTimer = 0f;
        isBroken = false;
    }
}
