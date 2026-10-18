package com.dieold.game;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Ashley ECS data-only component lifecycles, pooling, and fast mapper access.
 */
public class ECSComponentLifecycleTest {

    private Engine engine;

    @BeforeEach
    public void setUp() {
        engine = new Engine();
    }

    @Test
    @DisplayName("Verify Transform2DComponent pooling and simulated Z elevation")
    public void testTransformComponent() {
        Transform2DComponent transform = engine.createComponent(Transform2DComponent.class);
        transform.position.set(12.5f, 25.0f);
        transform.z = 2.4f;

        assertEquals(12.5f, transform.position.x, 1e-4);
        assertEquals(25.0f, transform.position.y, 1e-4);
        assertEquals(2.4f, transform.z, 1e-4);

        transform.reset();
        assertEquals(0f, transform.position.x, 1e-4);
        assertEquals(0f, transform.position.y, 1e-4);
        assertEquals(0f, transform.z, 1e-4);
    }

    @Test
    @DisplayName("Verify DirectionComponent 8-directional discrete mapping")
    public void testDirectionComponent() {
        DirectionComponent dir = engine.createComponent(DirectionComponent.class);

        // Screen angle mapping:
        // East: 0° -> dir 3
        dir.setFromScreenAngle(0f);
        assertEquals(3, dir.directionIndex);

        // North-East: 45° -> dir 4
        dir.setFromScreenAngle(45f);
        assertEquals(4, dir.directionIndex);

        // North: 90° -> dir 5
        dir.setFromScreenAngle(90f);
        assertEquals(5, dir.directionIndex);

        // North-West: 135° -> dir 6
        dir.setFromScreenAngle(135f);
        assertEquals(6, dir.directionIndex);

        // West: 180° -> dir 7
        dir.setFromScreenAngle(180f);
        assertEquals(7, dir.directionIndex);

        // South-West: 225° -> dir 0
        dir.setFromScreenAngle(225f);
        assertEquals(0, dir.directionIndex);

        // South: 270° -> dir 1
        dir.setFromScreenAngle(270f);
        assertEquals(1, dir.directionIndex);

        // South-East: 315° -> dir 2
        dir.setFromScreenAngle(315f);
        assertEquals(2, dir.directionIndex);

        // 2.5D World-to-Screen Vector Projection:
        // Moving Screen UP (World +1, +1) -> Screen North -> dir 5
        dir.setFromWorldVector(1f, 1f);
        assertEquals(5, dir.directionIndex);

        // Moving Screen DOWN (World -1, -1) -> Screen South -> dir 1
        dir.setFromWorldVector(-1f, -1f);
        assertEquals(1, dir.directionIndex);

        // Moving Screen RIGHT (World +1, -1) -> Screen East -> dir 3
        dir.setFromWorldVector(1f, -1f);
        assertEquals(3, dir.directionIndex);

        // Moving Screen LEFT (World -1, +1) -> Screen West -> dir 7
        dir.setFromWorldVector(-1f, 1f);
        assertEquals(7, dir.directionIndex);
    }

    @Test
    @DisplayName("Verify CombatStateComponent transitions and state guards")
    public void testCombatStateComponent() {
        CombatStateComponent combat = engine.createComponent(CombatStateComponent.class);
        assertEquals(CombatStateComponent.State.IDLE, combat.state);
        assertTrue(combat.canMove());
        assertTrue(combat.canAttack());
        assertTrue(combat.canDeflect());

        combat.transitionTo(CombatStateComponent.State.DEFLECTING);
        assertEquals(CombatStateComponent.State.DEFLECTING, combat.state);
        assertTrue(combat.isDeflecting);
        assertFalse(combat.canMove());

        combat.transitionTo(CombatStateComponent.State.HITSTUN);
        assertFalse(combat.isDeflecting);
        assertFalse(combat.canMove());
        assertFalse(combat.canAttack());

        combat.transitionTo(CombatStateComponent.State.DEAD);
        assertEquals(CombatStateComponent.State.DEAD, combat.state);
        assertFalse(combat.canMove());
    }

    @Test
    @DisplayName("Verify PoiseComponent damage, posture break, and restoration")
    public void testPoiseComponent() {
        PoiseComponent poise = engine.createComponent(PoiseComponent.class);
        assertEquals(100f, poise.currentPoise, 1e-4);
        assertFalse(poise.isBroken);

        // Damage poise by 40
        poise.damagePoise(40f);
        assertEquals(60f, poise.currentPoise, 1e-4);
        assertFalse(poise.isBroken);

        // Break poise with 70 more damage
        poise.damagePoise(70f);
        assertEquals(0f, poise.currentPoise, 1e-4);
        assertTrue(poise.isBroken);
        assertEquals(1.5f, poise.poiseBreakTimer, 1e-4);

        // Restore
        poise.restoreFull();
        assertEquals(100f, poise.currentPoise, 1e-4);
        assertFalse(poise.isBroken);
    }

    @Test
    @DisplayName("Verify HealthComponent damage and death status")
    public void testHealthComponent() {
        HealthComponent health = engine.createComponent(HealthComponent.class);
        health.takeDamage(35f);
        assertEquals(65f, health.currentHp, 1e-4);
        assertFalse(health.isDead());

        health.takeDamage(70f);
        assertEquals(0f, health.currentHp, 1e-4);
        assertTrue(health.isDead());
    }

    @Test
    @DisplayName("Verify Entity component attachment and ComponentMappers retrieval")
    public void testEntityMapperIntegration() {
        Entity entity = engine.createEntity();
        Transform2DComponent t = engine.createComponent(Transform2DComponent.class);
        HealthComponent h = engine.createComponent(HealthComponent.class);

        entity.add(t);
        entity.add(h);
        engine.addEntity(entity);

        assertNotNull(ComponentMappers.transform.get(entity));
        assertNotNull(ComponentMappers.health.get(entity));
        assertNull(ComponentMappers.velocity.get(entity));
    }
}
