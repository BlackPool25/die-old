package com.dieold.game.ecs.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.dieold.game.combat.HitstopController;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.Box2DBodyComponent;
import com.dieold.game.ecs.components.Transform2DComponent;
import com.dieold.game.ecs.components.VelocityComponent;
import com.dieold.game.physics.CombatContactListener;

/**
 * Box2D physics simulation running on a fixed 60 FPS timestep accumulator.
 * Synchronizes Box2D rigid bodies with 2.5D Transform2D components and manages simulated Z elevation.
 * Halts stepping during hitstop freeze.
 */
public class PhysicsSystem extends EntitySystem {
    public static final float TIME_STEP = 1.0f / 60.0f;
    public static final int VELOCITY_ITERATIONS = 8;
    public static final int POSITION_ITERATIONS = 3;

    private final World world;
    private final HitstopController hitstop;
    private final CombatContactListener contactListener;
    private float accumulator = 0f;

    private ImmutableArray<Entity> physicsEntities;
    private ImmutableArray<Entity> velocityEntities;

    public PhysicsSystem(World world, HitstopController hitstop, CombatContactListener contactListener) {
        super(50); // System Priority 50
        this.world = world;
        this.hitstop = hitstop;
        this.contactListener = contactListener;
    }

    @Override
    public void addedToEngine(Engine engine) {
        physicsEntities = engine.getEntitiesFor(Family.all(Transform2DComponent.class, Box2DBodyComponent.class).get());
        velocityEntities = engine.getEntitiesFor(Family.all(Transform2DComponent.class, VelocityComponent.class).get());
    }

    @Override
    public void update(float deltaTime) {
        // Clamp large delta times to prevent physics spiral
        float frameTime = Math.min(deltaTime, 0.25f);

        // Update hitstop countdown
        boolean frozen = hitstop.update(frameTime);
        if (frozen) {
            // During hitstop, Box2D physics simulation is frozen
            return;
        }

        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            accumulator -= TIME_STEP;

            // Clean up entities marked for destruction outside world.step()
            processDeferredDestructions();
        }

        // Sync Box2D body positions to Transform2DComponent
        int numPhysics = physicsEntities.size();
        for (int i = 0; i < numPhysics; i++) {
            Entity entity = physicsEntities.get(i);
            Box2DBodyComponent bodyComp = ComponentMappers.body.get(entity);
            Transform2DComponent transform = ComponentMappers.transform.get(entity);

            if (bodyComp != null && bodyComp.body != null && transform != null) {
                Vector2 bodyPos = bodyComp.body.getPosition();
                transform.position.set(bodyPos.x, bodyPos.y);
            }
        }

        // Update simulated vertical elevation (Z-axis) and gravity
        int numVelocity = velocityEntities.size();
        for (int i = 0; i < numVelocity; i++) {
            Entity entity = velocityEntities.get(i);
            VelocityComponent vel = ComponentMappers.velocity.get(entity);
            Transform2DComponent transform = ComponentMappers.transform.get(entity);

            if (vel != null && transform != null) {
                if (transform.z > 0f || vel.vz != 0f) {
                    transform.z += vel.vz * frameTime;
                    vel.vz -= vel.gravityScale * frameTime;
                    if (transform.z <= 0f) {
                        transform.z = 0f;
                        vel.vz = 0f;
                    }
                }
            }
        }
    }

    private void processDeferredDestructions() {
        if (contactListener == null || contactListener.pendingDestroyEntities.isEmpty()) {
            return;
        }

        Engine engine = getEngine();
        for (int i = 0; i < contactListener.pendingDestroyEntities.size; i++) {
            Entity entity = contactListener.pendingDestroyEntities.get(i);
            Box2DBodyComponent bodyComp = ComponentMappers.body.get(entity);
            if (bodyComp != null && bodyComp.body != null) {
                world.destroyBody(bodyComp.body);
                bodyComp.body = null;
            }
            if (engine != null) {
                engine.removeEntity(entity);
            }
        }
        contactListener.pendingDestroyEntities.clear();
    }
}
