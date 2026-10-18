package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Kinetic projectile component (faction, speed, deflection status, lifetime).
 */
public class ProjectileComponent implements Component, Poolable {
    public int faction = 1; // 0: Player, 1: Enemy
    public float speed = 8.0f;
    public float damage = 18.0f;
    public float poiseDamage = 25.0f;
    public boolean parriable = true;
    public boolean deflected = false;
    public float lifetime = 4.0f;
    public float timer = 0f;

    @Override
    public void reset() {
        faction = 1;
        speed = 8.0f;
        damage = 18.0f;
        poiseDamage = 25.0f;
        parriable = true;
        deflected = false;
        lifetime = 4.0f;
        timer = 0f;
    }
}
