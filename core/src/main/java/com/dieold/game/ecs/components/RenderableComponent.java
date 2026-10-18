package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Visual rendering parameters, Y-sort baseline offset, and drop shadow anchors.
 */
public class RenderableComponent implements Component, Poolable {
    public boolean visible = true;
    public float ySortOffset = 0f;
    public boolean drawShadow = true;
    public float shadowRadiusX = 16f;
    public float shadowRadiusY = 8f;
    public float shadowScale = 1f;
    public float scale = 0.70f;
    public final Color tint = new Color(1f, 1f, 1f, 1f);

    @Override
    public void reset() {
        visible = true;
        ySortOffset = 0f;
        drawShadow = true;
        shadowRadiusX = 16f;
        shadowRadiusY = 8f;
        shadowScale = 1f;
        scale = 0.70f;
        tint.set(1f, 1f, 1f, 1f);
    }
}
