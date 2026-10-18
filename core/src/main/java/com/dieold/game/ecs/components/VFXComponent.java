package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Visual effects component (sparks, dust puffs, slash arcs).
 */
public class VFXComponent implements Component, Poolable {
    public String vfxName = "deflect_spark";
    public float stateTime = 0f;
    public float frameDuration = 0.05f;
    public int currentFrameIndex = 0;
    public boolean finished = false;
    public final Vector2 offset = new Vector2();
    public float scale = 1f;
    public float rotation = 0f;
    public AtlasRegion currentRegion = null;

    @Override
    public void reset() {
        vfxName = "deflect_spark";
        stateTime = 0f;
        frameDuration = 0.05f;
        currentFrameIndex = 0;
        finished = false;
        offset.setZero();
        scale = 1f;
        rotation = 0f;
        currentRegion = null;
    }
}
