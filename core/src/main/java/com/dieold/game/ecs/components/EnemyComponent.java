package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Foundry Hound Automaton AI component:
 * Line-of-sight raycasting, agro range, patrol waypoints, coordinated flanking angles,
 * telegraph windup, pounce lunges, and recovery states.
 */
public class EnemyComponent implements Component, Poolable {

    public enum AIState {
        PATROL,
        ALERT,
        FLANKING,
        TELEGRAPHING,
        POUNCING,
        RECOVERING,
        STAGGERED
    }

    public AIState aiState = AIState.PATROL;

    // Perception & Agro
    public float agroDistance = 8.0f;
    public float proximitySoundDistance = 3.2f;
    public boolean hasLineOfSight = false;
    public boolean isAgroed = false;
    public float alertTimer = 0f;

    // Patrol waypoint
    public final Vector2 patrolOrigin = new Vector2();
    public float patrolAngle = 0f;
    public float patrolRadius = 2.0f;
    public float patrolWaitTimer = 0f;

    // Coordinated Flanking
    public float flankAngleOffset = 0f; // e.g. 0°, +120°, -120°
    public float targetDistance = 3.6f;
    public float circleSpeed = 18.0f; // deg/sec (deliberate tactical stalking pace)

    // Combat timers
    public float attackCooldown = 2.0f;
    public float telegraphTimer = 0f;
    public float pounceTimer = 0f;
    public float recoveryTimer = 0f;
    public final Vector2 pounceTarget = new Vector2();
    public boolean pounceDamageDealt = false;

    @Override
    public void reset() {
        aiState = AIState.PATROL;
        agroDistance = 8.0f;
        proximitySoundDistance = 3.2f;
        hasLineOfSight = false;
        isAgroed = false;
        alertTimer = 0f;
        patrolOrigin.setZero();
        patrolAngle = 0f;
        patrolRadius = 2.0f;
        patrolWaitTimer = 0f;
        flankAngleOffset = 0f;
        targetDistance = 3.6f;
        circleSpeed = 18.0f;
        attackCooldown = 2.0f;
        telegraphTimer = 0f;
        pounceTimer = 0f;
        recoveryTimer = 0f;
        pounceTarget.setZero();
        pounceDamageDealt = false;
    }
}
