package com.dieold.game.ecs.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.physics.box2d.World;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.*;

/**
 * Handles attack combo timers, poise break recovery, ghost health drain,
 * hitstun countdowns, and active hitbox lifecycles.
 */
public class CombatSystem extends EntitySystem {

    private final World world;
    private ImmutableArray<Entity> combatEntities;
    private ImmutableArray<Entity> projectileEntities;
    private ImmutableArray<Entity> hitboxEntities;

    public CombatSystem(World world) {
        super(40); // System Priority 40
        this.world = world;
    }

    @Override
    public void addedToEngine(Engine engine) {
        combatEntities = engine.getEntitiesFor(
            Family.all(CombatStateComponent.class, HealthComponent.class).get()
        );
        projectileEntities = engine.getEntitiesFor(
            Family.all(ProjectileComponent.class, Transform2DComponent.class).get()
        );
        hitboxEntities = engine.getEntitiesFor(
            Family.all(HitboxComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        // 1. Update Combat States, Timers, Poise, and Health
        int count = combatEntities.size();
        for (int i = 0; i < count; i++) {
            Entity entity = combatEntities.get(i);
            CombatStateComponent combat = ComponentMappers.combat.get(entity);
            HealthComponent health = ComponentMappers.health.get(entity);
            PoiseComponent poise = ComponentMappers.poise.get(entity);
            Box2DBodyComponent bodyComp = ComponentMappers.body.get(entity);

            combat.stateTimer += deltaTime;

            // Check death
            if (health.isDead()) {
                if (combat.state != CombatStateComponent.State.DEAD) {
                    combat.transitionTo(CombatStateComponent.State.DEAD);
                    if (bodyComp != null && bodyComp.body != null) {
                        bodyComp.body.setLinearVelocity(0f, 0f);
                    }
                }
                continue;
            }

            // Ghost HP lingering drain
            if (health.ghostHp > health.currentHp) {
                health.ghostHp = Math.max(health.currentHp, health.ghostHp - health.ghostDrainRate * deltaTime);
            }

            // Poise recovery
            if (poise != null) {
                if (poise.isBroken) {
                    poise.poiseBreakTimer -= deltaTime;
                    if (poise.poiseBreakTimer <= 0f) {
                        poise.restoreFull();
                    }
                } else {
                    if (poise.recoveryDelayTimer > 0f) {
                        poise.recoveryDelayTimer -= deltaTime;
                    } else if (poise.currentPoise < poise.maxPoise) {
                        poise.currentPoise = Math.min(poise.maxPoise, poise.currentPoise + poise.recoveryRate * deltaTime);
                    }
                }
            }

            // i-Frame countdown
            if (combat.iFrameTimer > 0f) {
                combat.iFrameTimer -= deltaTime;
                health.isInvulnerable = true;
            } else {
                health.isInvulnerable = false;
            }

            // State specific logic
            switch (combat.state) {
                case DASHING:
                    combat.dashTimer -= deltaTime;
                    if (combat.dashTimer <= 0f) {
                        combat.transitionTo(CombatStateComponent.State.IDLE);
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(0f, 0f);
                        }
                    }
                    break;

                case ATTACKING:
                    combat.comboResetTimer -= deltaTime;
                    // Combo cancel window opens mid-attack
                    if (combat.stateTimer >= 0.15f) {
                        combat.comboWindowOpen = true;
                    }
                    if (combat.stateTimer >= 0.35f) {
                        combat.transitionTo(CombatStateComponent.State.IDLE);
                    }
                    if (combat.comboResetTimer <= 0f) {
                        combat.comboStep = 0;
                    }
                    break;

                case HITSTUN:
                    combat.hitstunTimer -= deltaTime;
                    if (combat.hitstunTimer <= 0f) {
                        combat.transitionTo(CombatStateComponent.State.IDLE);
                        combat.inKnockback = false;
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(0f, 0f);
                        }
                    }
                    break;

                case WALL_SPLAT:
                    combat.wallSplatTimer -= deltaTime;
                    if (combat.wallSplatTimer <= 0f) {
                        combat.transitionTo(CombatStateComponent.State.IDLE);
                        combat.inKnockback = false;
                    }
                    break;

                default:
                    break;
            }
        }

        // 2. Active Hitbox Lifecycles
        int numHitboxes = hitboxEntities.size();
        for (int i = 0; i < numHitboxes; i++) {
            Entity entity = hitboxEntities.get(i);
            HitboxComponent hitbox = ComponentMappers.hitbox.get(entity);
            if (hitbox.active) {
                hitbox.timer -= deltaTime;
                if (hitbox.timer <= 0f) {
                    hitbox.active = false;
                    hitbox.hitEntityIds.clear();
                }
            }
        }

        // 3. Projectile Lifetimes
        int numProj = projectileEntities.size();
        Engine engine = getEngine();
        for (int i = 0; i < numProj; i++) {
            Entity entity = projectileEntities.get(i);
            ProjectileComponent proj = ComponentMappers.projectile.get(entity);
            proj.timer += deltaTime;
            if (proj.timer >= proj.lifetime) {
                Box2DBodyComponent bodyComp = ComponentMappers.body.get(entity);
                if (bodyComp != null && bodyComp.body != null) {
                    world.destroyBody(bodyComp.body);
                    bodyComp.body = null;
                }
                if (engine != null) {
                    engine.removeEntity(entity);
                }
            }
        }
    }
}
