package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Linear ground velocity (vx, vy) in m/s, vertical velocity vz in m/s, and gravity scale for elevation.
 */
public class VelocityComponent implements Component, Poolable {
    public final Vector2 linear = new Vector2();
    public float vz = 0f;
    public float gravityScale = 14.0f;

    @Override
    public void reset() {
        linear.setZero();
        vz = 0f;
        gravityScale = 14.0f;
    }
}
