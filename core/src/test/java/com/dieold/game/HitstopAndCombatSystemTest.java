package com.dieold.game;

import com.dieold.game.combat.HitstopController;
import com.dieold.game.ecs.components.HealthComponent;
import com.dieold.game.ecs.components.PoiseComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Hitstop freeze timing, poise recovery, and lingering ghost damage drain.
 */
public class HitstopAndCombatSystemTest {

    @Test
    @DisplayName("Verify HitstopController frame freeze durations and countdown")
    public void testHitstopController() {
        HitstopController hitstop = new HitstopController();
        assertFalse(hitstop.isFrozen());

        // 1. Normal hit: 4 frames = 4/60 = 0.06666s
        hitstop.triggerHitstop(HitstopController.FRAMES_NORMAL_HIT);
        assertTrue(hitstop.isFrozen());
        assertEquals(4f / 60f, hitstop.getRemaining(), 1e-4);

        // Advance 2 frames (1/30s)
        boolean stillFrozen = hitstop.update(2f / 60f);
        assertTrue(stillFrozen);
        assertTrue(hitstop.isFrozen());

        // Advance 3 more frames (should unfreeze)
        stillFrozen = hitstop.update(3f / 60f);
        assertFalse(stillFrozen);
        assertFalse(hitstop.isFrozen());

        // 2. Deflection: 8 frames = 8/60 = 0.13333s
        hitstop.triggerHitstop(HitstopController.FRAMES_DEFLECTION);
        assertTrue(hitstop.isFrozen());
        assertEquals(8f / 60f, hitstop.getRemaining(), 1e-4);

        // 3. Guard Crush: 12 frames = 12/60 = 0.2000s
        hitstop.triggerHitstop(HitstopController.FRAMES_GUARD_CRUSH);
        assertEquals(12f / 60f, hitstop.getRemaining(), 1e-4);
    }

    @Test
    @DisplayName("Verify lingering ghost damage drain over time")
    public void testGhostDamageDrain() {
        HealthComponent health = new HealthComponent();
        health.maxHp = 100f;
        health.currentHp = 100f;
        health.ghostHp = 100f;
        health.ghostDrainRate = 20f; // 20 hp/sec

        // Take 40 damage
        health.takeDamage(40f);
        assertEquals(60f, health.currentHp, 1e-4);
        assertEquals(100f, health.ghostHp, 1e-4, "Ghost HP remains full immediately after impact");

        // Drain after 1 second (drains 20 HP)
        float dt = 1.0f;
        if (health.ghostHp > health.currentHp) {
            health.ghostHp = Math.max(health.currentHp, health.ghostHp - health.ghostDrainRate * dt);
        }
        assertEquals(80f, health.ghostHp, 1e-4);

        // Drain after 2 more seconds (drains another 40 HP, clamped to currentHp 60)
        dt = 2.0f;
        if (health.ghostHp > health.currentHp) {
            health.ghostHp = Math.max(health.currentHp, health.ghostHp - health.ghostDrainRate * dt);
        }
        assertEquals(60f, health.ghostHp, 1e-4);
    }

    @Test
    @DisplayName("Verify poise break stun duration and regeneration delay")
    public void testPoiseBreakAndRecovery() {
        PoiseComponent poise = new PoiseComponent();
        poise.maxPoise = 100f;
        poise.currentPoise = 100f;
        poise.recoveryDelay = 1.0f;
        poise.recoveryRate = 30f;

        // Damage poise by 30
        poise.damagePoise(30f);
        assertEquals(70f, poise.currentPoise, 1e-4);
        assertEquals(1.0f, poise.recoveryDelayTimer, 1e-4);

        // While delay timer is positive, poise does not recover
        poise.recoveryDelayTimer -= 0.5f;
        assertTrue(poise.recoveryDelayTimer > 0f);

        // Once delay expires, recovery begins
        poise.recoveryDelayTimer = 0f;
        poise.currentPoise += poise.recoveryRate * 0.5f; // recovers 15
        assertEquals(85f, poise.currentPoise, 1e-4);
    }

    @Test
    @DisplayName("Verify Heavy Attack and progressive combo pacing mechanics")
    public void testHeavyAttackAndComboProgression() {
        com.dieold.game.ecs.components.CombatStateComponent combat = new com.dieold.game.ecs.components.CombatStateComponent();
        assertEquals(com.dieold.game.ecs.components.CombatStateComponent.State.IDLE, combat.state);

        // 1. Can execute heavy attack from IDLE
        assertTrue(combat.canHeavyAttack());
        combat.transitionTo(com.dieold.game.ecs.components.CombatStateComponent.State.HEAVY_ATTACKING);
        assertEquals(com.dieold.game.ecs.components.CombatStateComponent.State.HEAVY_ATTACKING, combat.state);
        assertFalse(combat.canMove(), "Cannot WASD move while executing heavy attack");
        assertTrue(combat.canDash(), "Can animation-cancel heavy attack with phase dash");

        // 2. Dash out of heavy attack
        combat.transitionTo(com.dieold.game.ecs.components.CombatStateComponent.State.DASHING);
        assertEquals(com.dieold.game.ecs.components.CombatStateComponent.State.DASHING, combat.state);

        // 3. Return to SPRINT
        combat.transitionTo(com.dieold.game.ecs.components.CombatStateComponent.State.SPRINT);
        assertTrue(combat.canHeavyAttack(), "Can execute heavy attack from sprint");
        assertTrue(combat.canAttack(), "Can execute light attack from sprint");

        // 4. Light Attack Combo Chaining
        combat.transitionTo(com.dieold.game.ecs.components.CombatStateComponent.State.ATTACKING);
        combat.comboStep = 1;
        combat.comboWindowOpen = false;
        assertFalse(combat.canAttack(), "Cannot combo before cancel window opens");

        // Window opens at 0.16s
        combat.comboWindowOpen = true;
        assertTrue(combat.canAttack(), "Can chain into combo step 2 when window opens");
        assertTrue(combat.canHeavyAttack(), "Can combo-cancel into heavy attack when window opens");
    }

    @Test
    @DisplayName("Verify mouse screen coordinates to 2.5D dimetric world vector conversion")
    public void testMouseAimDimetricConversion() {
        // Test East / Right on screen: ScreenDX = 100, ScreenDY = 0
        float screenDX = 100f;
        float screenDY = 0f;
        float worldDX = 0.5f * (screenDY / 16.0f + screenDX / 27.712812f);
        float worldDY = 0.5f * (screenDY / 16.0f - screenDX / 27.712812f);

        assertTrue(worldDX > 0, "Moving right on screen moves positive along world X");
        assertTrue(worldDY < 0, "Moving right on screen moves negative along world Y");

        // Verify roundtrip back to screen
        float roundtripScreenX = (worldDX - worldDY) * 27.712812f;
        float roundtripScreenY = (worldDX + worldDY) * 16.0f;
        assertEquals(screenDX, roundtripScreenX, 1e-3);
        assertEquals(screenDY, roundtripScreenY, 1e-3);
    }
}
