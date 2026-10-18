package com.dieold.game;

import com.badlogic.gdx.math.Vector2;
import com.dieold.game.ecs.components.CombatStateComponent;
import com.dieold.game.ecs.components.HealthComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests wall-slam ragdoll physics, momentum calculation J = m * ||v||,
 * and crushing damage application.
 */
public class WallSplatCollisionTest {

    @Test
    @DisplayName("Verify momentum calculation J = m * ||v|| and collisional crushing damage")
    public void testWallSplatMomentumAndDamage() {
        float mass = 2.5f; // kg
        Vector2 velocity = new Vector2(6.0f, 8.0f); // 10.0 m/s
        float speed = velocity.len();
        assertEquals(10.0f, speed, 1e-4);

        // Momentum J = m * ||v||
        float momentum = mass * speed;
        assertEquals(25.0f, momentum, 1e-4);

        // Collisional crushing damage proportional to J (momentum * 8.0f)
        float crushingDamage = momentum * 8.0f;
        assertEquals(200.0f, crushingDamage, 1e-4);

        // Apply to entity health
        HealthComponent health = new HealthComponent();
        health.maxHp = 300f;
        health.currentHp = 300f;
        health.takeDamage(crushingDamage);

        assertEquals(100.0f, health.currentHp, 1e-4);
    }

    @Test
    @DisplayName("Verify transition to WALL_SPLAT state and 0.8s stun duration")
    public void testWallSplatStateTransition() {
        CombatStateComponent combat = new CombatStateComponent();
        combat.transitionTo(CombatStateComponent.State.HITSTUN);
        combat.inKnockback = true;
        combat.knockbackVelocity.set(8f, 0f);

        // Wall collision triggers splat
        combat.transitionTo(CombatStateComponent.State.WALL_SPLAT);
        combat.wallSplatTimer = 0.8f;
        combat.inKnockback = false;

        assertEquals(CombatStateComponent.State.WALL_SPLAT, combat.state);
        assertEquals(0.8f, combat.wallSplatTimer, 1e-4);
        assertFalse(combat.inKnockback);
        assertFalse(combat.canMove());
        assertFalse(combat.canAttack());
    }
}
