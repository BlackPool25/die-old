package com.dieold.game.ecs.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * Player marker and meta-stats (age, dash pips, recharge timers).
 */
public class PlayerComponent implements Component, Poolable {
    public int age = 20; // Starts in Youth
    public int maxDashPips = 3;
    public int dashPips = 3;
    public float dashRechargeTimer = 0f;
    public float dashRechargeDelay = 1.2f;

    @Override
    public void reset() {
        age = 20;
        maxDashPips = 3;
        dashPips = 3;
        dashRechargeTimer = 0f;
        dashRechargeDelay = 1.2f;
    }
}
