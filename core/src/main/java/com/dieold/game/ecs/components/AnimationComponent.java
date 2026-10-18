package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Tracks active animation action state, frame time, direction index (0-7), and current region.
 */
public class AnimationComponent implements Component, Poolable {
    public String characterPrefix = "player_youth";
    public String action = "idle";
    public int directionIndex = 0;
    public float stateTime = 0f;
    public float frameDuration = 0.08f;
    public int currentFrameIndex = 0;
    public boolean looping = true;
    public boolean finished = false;
    public AtlasRegion currentRegion = null;

    public void setAction(String newAction, boolean loop, float duration) {
        if (!action.equals(newAction)) {
            this.action = newAction;
            this.stateTime = 0f;
            this.currentFrameIndex = 0;
            this.finished = false;
        }
        this.looping = loop;
        this.frameDuration = duration;
    }

    @Override
    public void reset() {
        characterPrefix = "player_youth";
        action = "idle";
        directionIndex = 0;
        stateTime = 0f;
        frameDuration = 0.08f;
        currentFrameIndex = 0;
        looping = true;
        finished = false;
        currentRegion = null;
    }
}
