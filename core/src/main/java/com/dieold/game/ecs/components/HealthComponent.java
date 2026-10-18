package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Health points, lingering ghost damage drain bar, and invulnerability state.
 */
public class HealthComponent implements Component, Poolable {
    public float maxHp = 100f;
    public float currentHp = 100f;
    public float ghostHp = 100f;
    public float ghostDrainRate = 25f; // hp drained per second
    public boolean isInvulnerable = false;

    public void takeDamage(float amount) {
        if (isInvulnerable || currentHp <= 0f) {
            return;
        }
        currentHp = Math.max(0f, currentHp - amount);
    }

    public boolean isDead() {
        return currentHp <= 0f;
    }

    @Override
    public void reset() {
        maxHp = 100f;
        currentHp = 100f;
        ghostHp = 100f;
        ghostDrainRate = 25f;
        isInvulnerable = false;
    }
}
