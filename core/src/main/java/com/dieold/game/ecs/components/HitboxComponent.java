package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Combat collision hitbox for melee attacks or impact triggers.
 */
public class HitboxComponent implements Component, Poolable {
    public boolean active = false;
    public float damage = 20f;
    public float poiseDamage = 35f;
    public final Vector2 knockbackImpulse = new Vector2();
    public boolean parriable = true;
    public int faction = 0; // 0: Player, 1: Enemy
    public float duration = 0.15f;
    public float timer = 0f;
    public float radius = 0.8f;
    public final Vector2 offset = new Vector2();
    public final IntSet hitEntityIds = new IntSet();

    @Override
    public void reset() {
        active = false;
        damage = 20f;
        poiseDamage = 35f;
        knockbackImpulse.setZero();
        parriable = true;
        faction = 0;
        duration = 0.15f;
        timer = 0f;
        radius = 0.8f;
        offset.setZero();
        hitEntityIds.clear();
    }
}
