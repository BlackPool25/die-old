package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Reference to Box2D Body with custom user data.
 */
public class Box2DBodyComponent implements Component, Poolable {
    public Body body;
    public Object userData;

    @Override
    public void reset() {
        body = null;
        userData = null;
    }
}
