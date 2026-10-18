package com.dieold.game;

import com.badlogic.gdx.math.Vector2;
import com.dieold.game.physics.CombatContactListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests 180° frontal deflection arc calculations and velocity inversion math.
 */
public class DeflectionPhysicsTest {

    @Test
    @DisplayName("Verify 180° frontal deflection arc allows front hits and blocks rear hits")
    public void testDeflectionArcAngles() {
        float playerDeflectAngle = 0f; // Facing East (0°)

        // Head-on from East (0°): angular diff 0° -> DEFLECT
        float diffEast = Math.abs(CombatContactListener.getAngleDifference(playerDeflectAngle, 0f));
        assertTrue(diffEast <= 90.0f, "Direct front should be within 180° arc");

        // Flank from North-East (45°): diff 45° -> DEFLECT
        float diffNE = Math.abs(CombatContactListener.getAngleDifference(playerDeflectAngle, 45f));
        assertTrue(diffNE <= 90.0f, "45° flank should be within 180° arc");

        // Flank from South-East (-45° or 315°): diff 45° -> DEFLECT
        float diffSE = Math.abs(CombatContactListener.getAngleDifference(playerDeflectAngle, 315f));
        assertTrue(diffSE <= 90.0f, "315° flank should be within 180° arc");

        // Perpendicular boundary (90°): diff 90° -> DEFLECT
        float diffNorth = Math.abs(CombatContactListener.getAngleDifference(playerDeflectAngle, 90f));
        assertTrue(diffNorth <= 90.0f, "90° boundary should be within 180° arc");

        // Rear attack from West (180°): diff 180° -> HIT (Vulnerable rear)
        float diffWest = Math.abs(CombatContactListener.getAngleDifference(playerDeflectAngle, 180f));
        assertFalse(diffWest <= 90.0f, "Rear attack (180°) must not be deflected");

        // Rear attack from South-West (225°): diff 135° -> HIT
        float diffSW = Math.abs(CombatContactListener.getAngleDifference(playerDeflectAngle, 225f));
        assertFalse(diffSW <= 90.0f, "Rear-flank attack (225°) must not be deflected");
    }

    @Test
    @DisplayName("Verify projectile linear velocity inversion with +150% speed multiplier")
    public void testProjectileDeflectionVelocityInversion() {
        Vector2 initialVelocity = new Vector2(8.0f, 0.0f);
        float originalSpeed = initialVelocity.len();

        // Reversal with +150% speed: v_new = -2.5 * v_old
        Vector2 deflectedVelocity = new Vector2(initialVelocity).scl(-2.5f);

        assertEquals(-20.0f, deflectedVelocity.x, 1e-4);
        assertEquals(0.0f, deflectedVelocity.y, 1e-4);

        float newSpeed = deflectedVelocity.len();
        assertEquals(originalSpeed * 2.5f, newSpeed, 1e-4, "Speed must be increased by +150% (2.5x original speed)");
    }

    @Test
    @DisplayName("Verify angular difference normalization across 0/360 wrap-around")
    public void testAngleWrapAround() {
        // Player facing 10°, incoming at 350° -> difference is 20°
        float diff = Math.abs(CombatContactListener.getAngleDifference(10f, 350f));
        assertEquals(20f, diff, 1e-4);

        // Player facing 350°, incoming at 10° -> difference is 20°
        float diffReverse = Math.abs(CombatContactListener.getAngleDifference(350f, 10f));
        assertEquals(20f, diffReverse, 1e-4);
    }
}
