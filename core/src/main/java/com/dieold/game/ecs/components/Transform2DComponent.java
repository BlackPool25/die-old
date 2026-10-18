package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * World position (x, y) in meters on ground plane, plus simulated vertical elevation z in meters.
 */
public class Transform2DComponent implements Component, Poolable {
    public final Vector2 position = new Vector2();
    public float z = 0f;
    public float rotation = 0f;
    public float scaleX = 1f;
    public float scaleY = 1f;

    @Override
    public void reset() {
        position.setZero();
        z = 0f;
        rotation = 0f;
        scaleX = 1f;
        scaleY = 1f;
    }
}
