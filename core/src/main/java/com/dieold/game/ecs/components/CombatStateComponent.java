package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Finite State Machine for combat actions, timers, combo chains, and impact reactions.
 */
public class CombatStateComponent implements Component, Poolable {

    public enum State {
        IDLE,
        SPRINT,
        ATTACKING,
        HEAVY_ATTACKING,
        DEFLECTING,
        DASHING,
        HITSTUN,
        WALL_SPLAT,
        DEAD
    }

    public State state = State.IDLE;
    public float stateTimer = 0f;

    // Combo progression (1, 2, 3)
    public int comboStep = 0;
    public float comboResetTimer = 0f;
    public boolean comboWindowOpen = false;
    public boolean bufferedNextAttack = false;

    // Deflect stance & parry window
    public float deflectTimer = 0f;
    public boolean isDeflecting = false;
    public float deflectAngle = 0f; // degrees

    // Reaction timers
    public float hitstunTimer = 0f;
    public float wallSplatTimer = 0f;
    public float dashTimer = 0f;
    public float iFrameTimer = 0f;
    public float heavyAttackCooldownTimer = 0f;
    public static final float HEAVY_ATTACK_COOLDOWN = 1.25f;

    // Knockback
    public boolean inKnockback = false;
    public final Vector2 knockbackVelocity = new Vector2();

    public void transitionTo(State newState) {
        this.state = newState;
        this.stateTimer = 0f;
        if (newState == State.DEFLECTING) {
            this.isDeflecting = true;
        } else if (newState != State.DEFLECTING) {
            this.isDeflecting = false;
        }
        if (newState == State.DEAD) {
            this.inKnockback = false;
        }
    }

    public boolean canMove() {
        return state == State.IDLE || state == State.SPRINT;
    }

    public boolean canAttack() {
        return state == State.IDLE || state == State.SPRINT || (state == State.ATTACKING && comboWindowOpen);
    }

    public boolean canHeavyAttack() {
        return heavyAttackCooldownTimer <= 0f && (state == State.IDLE || state == State.SPRINT || (state == State.ATTACKING && comboWindowOpen));
    }

    public boolean canDeflect() {
        return state == State.IDLE || state == State.SPRINT;
    }

    public boolean canDash() {
        return state == State.IDLE || state == State.SPRINT || state == State.ATTACKING || state == State.HEAVY_ATTACKING;
    }

    @Override
    public void reset() {
        state = State.IDLE;
        stateTimer = 0f;
        comboStep = 0;
        comboResetTimer = 0f;
        comboWindowOpen = false;
        bufferedNextAttack = false;
        deflectTimer = 0f;
        isDeflecting = false;
        deflectAngle = 0f;
        hitstunTimer = 0f;
        wallSplatTimer = 0f;
        dashTimer = 0f;
        iFrameTimer = 0f;
        heavyAttackCooldownTimer = 0f;
        inKnockback = false;
        knockbackVelocity.setZero();
    }
}
