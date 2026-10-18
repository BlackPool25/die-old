package com.dieold.game.ecs.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.*;
import com.dieold.game.physics.PhysicsConstants;

/**
 * Advanced Foundry Hound Automaton AI:
 * - Line-of-sight raycasting via Box2D against environment walls/pillars.
 * - Distance agro & sound proximity detection.
 * - Coordinated multi-hound flanking rings (staggered angular offsets to avoid clumping).
 * - Telegraph wind-up, violent pounce lunges, recovery lag, and staggered combat turns.
 */
public class EnemyAISystem extends EntitySystem {

    public interface HoundEventListener {
        void onHoundPounce(Entity hound, Vector2 start, Vector2 target);
        void onHoundShoot(Entity hound, Vector2 from, float angle);
        void onHoundAlert(Entity hound, Vector2 pos);
    }

    private final World world;
    private ImmutableArray<Entity> enemyEntities;
    private ImmutableArray<Entity> playerEntities;
    private HoundEventListener eventListener;

    private float globalOrbitTimer = 0f;

    // Zero-allocation raycast callback for line-of-sight
    private boolean raycastHitWall = false;
    private final RayCastCallback losCallback = (fixture, point, normal, fraction) -> {
        if (fixture.getFilterData().categoryBits == PhysicsConstants.CATEGORY_WALL) {
            raycastHitWall = true;
            return 0f; // Terminate raycast
        }
        return 1f; // Continue
    };

    // Pre-allocated math vectors
    private final Vector2 toPlayer = new Vector2();
    private final Vector2 desiredPos = new Vector2();
    private final Vector2 moveVel = new Vector2();
    private final Vector2 houndPos = new Vector2();

    public EnemyAISystem(World world) {
        super(30); // System Priority 30
        this.world = world;
    }

    public void setEventListener(HoundEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public void addedToEngine(Engine engine) {
        enemyEntities = engine.getEntitiesFor(
            Family.all(EnemyComponent.class, Transform2DComponent.class, Box2DBodyComponent.class, CombatStateComponent.class, DirectionComponent.class).get()
        );
        playerEntities = engine.getEntitiesFor(
            Family.all(PlayerComponent.class, Transform2DComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        if (playerEntities.size() == 0) {
            return;
        }

        Entity player = playerEntities.get(0);
        Transform2DComponent playerTransform = ComponentMappers.transform.get(player);
        if (playerTransform == null) {
            return;
        }

        globalOrbitTimer += deltaTime;

        int count = enemyEntities.size();
        for (int i = 0; i < count; i++) {
            Entity hound = enemyEntities.get(i);
            EnemyComponent ai = ComponentMappers.enemy.get(hound);
            Transform2DComponent transform = ComponentMappers.transform.get(hound);
            Box2DBodyComponent bodyComp = ComponentMappers.body.get(hound);
            CombatStateComponent combat = ComponentMappers.combat.get(hound);
            DirectionComponent direction = ComponentMappers.direction.get(hound);

            if (combat.state == CombatStateComponent.State.DEAD) {
                if (bodyComp != null && bodyComp.body != null) {
                    bodyComp.body.setLinearVelocity(0f, 0f);
                }
                continue;
            }

            if (combat.state == CombatStateComponent.State.HITSTUN || combat.state == CombatStateComponent.State.WALL_SPLAT) {
                ai.aiState = EnemyComponent.AIState.STAGGERED;
                continue;
            }

            houndPos.set(transform.position);
            toPlayer.set(playerTransform.position).sub(houndPos);
            float distToPlayer = toPlayer.len();
            float angleToPlayer = MathUtils.radiansToDegrees * MathUtils.atan2(toPlayer.y, toPlayer.x);

            // 1. Line-of-Sight & Agro Check
            if (!ai.isAgroed) {
                boolean detected = false;
                if (distToPlayer <= ai.agroDistance) {
                    // Raycast against walls and pillars
                    raycastHitWall = false;
                    world.rayCast(losCallback, houndPos, playerTransform.position);
                    ai.hasLineOfSight = !raycastHitWall;

                    if (ai.hasLineOfSight || distToPlayer <= ai.proximitySoundDistance) {
                        detected = true;
                    }
                }

                if (detected) {
                    ai.isAgroed = true;
                    ai.aiState = EnemyComponent.AIState.ALERT;
                    ai.alertTimer = 0.40f;
                    direction.setFromWorldVector(toPlayer);
                    if (bodyComp != null && bodyComp.body != null) {
                        bodyComp.body.setLinearVelocity(0f, 0f);
                    }
                    if (eventListener != null) {
                        eventListener.onHoundAlert(hound, houndPos);
                    }
                }
            }

            // 2. AI Finite State Machine
            switch (ai.aiState) {
                case PATROL:
                    // Hound patrols around its designated origin point at a steady, grounded pace
                    ai.patrolAngle += deltaTime * 24.0f;
                    float patRad = ai.patrolAngle * MathUtils.degreesToRadians;
                    desiredPos.set(
                        ai.patrolOrigin.x + MathUtils.cos(patRad) * ai.patrolRadius,
                        ai.patrolOrigin.y + MathUtils.sin(patRad) * ai.patrolRadius
                    );

                    moveVel.set(desiredPos).sub(houndPos);
                    if (moveVel.len() > 0.2f) {
                        moveVel.nor().scl(1.5f); // 1.5 m/s patrol pace
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(moveVel);
                        }
                        direction.setFromWorldVector(moveVel);
                        combat.transitionTo(CombatStateComponent.State.SPRINT);
                    } else {
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(0f, 0f);
                        }
                        combat.transitionTo(CombatStateComponent.State.IDLE);
                    }
                    break;

                case ALERT:
                    direction.setFromWorldVector(toPlayer);
                    if (bodyComp != null && bodyComp.body != null) {
                        bodyComp.body.setLinearVelocity(0f, 0f);
                    }
                    ai.alertTimer -= deltaTime;
                    if (ai.alertTimer <= 0f) {
                        ai.aiState = EnemyComponent.AIState.FLANKING;
                        ai.attackCooldown = MathUtils.random(1.2f, 2.5f);
                    }
                    break;

                case FLANKING:
                    ai.attackCooldown -= deltaTime;

                    // Coordinated flanking ring: orbit angle includes individual offset (+0°, +120°, -120°)
                    float currentFlankAngle = (globalOrbitTimer * ai.circleSpeed) + ai.flankAngleOffset;
                    float flankRad = currentFlankAngle * MathUtils.degreesToRadians;
                    desiredPos.set(
                        playerTransform.position.x + MathUtils.cos(flankRad) * ai.targetDistance,
                        playerTransform.position.y + MathUtils.sin(flankRad) * ai.targetDistance
                    );

                    moveVel.set(desiredPos).sub(houndPos);
                    float targetDist = moveVel.len();

                    if (targetDist > 0.35f) {
                        // Tactical stalking trot: 2.2 m/s with paws gripping stone
                        moveVel.nor().scl(2.2f);
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(moveVel);
                        }
                        direction.setFromWorldVector(moveVel);
                        combat.transitionTo(CombatStateComponent.State.SPRINT);
                    } else {
                        // In position: face player in menacing stance
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(0f, 0f);
                        }
                        direction.setFromWorldVector(toPlayer);
                        combat.transitionTo(CombatStateComponent.State.IDLE);
                    }

                    // Ready to attack when cooldown expires and within range
                    if (ai.attackCooldown <= 0f && distToPlayer <= 5.2f) {
                        ai.aiState = EnemyComponent.AIState.TELEGRAPHING;
                        ai.telegraphTimer = 0.80f; // Clear 0.8s reaction window
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(0f, 0f);
                        }
                    }
                    break;

                case TELEGRAPHING:
                    direction.setFromWorldVector(toPlayer);
                    if (bodyComp != null && bodyComp.body != null) {
                        bodyComp.body.setLinearVelocity(0f, 0f);
                    }
                    ai.telegraphTimer -= deltaTime;

                    if (ai.telegraphTimer <= 0f) {
                        // 35% chance kinetic projectile, 65% pounce bite
                        if (MathUtils.randomBoolean(0.35f) && distToPlayer > 2.8f) {
                            if (eventListener != null) {
                                eventListener.onHoundShoot(hound, houndPos, angleToPlayer);
                            }
                            ai.aiState = EnemyComponent.AIState.RECOVERING;
                            ai.recoveryTimer = 0.8f;
                        } else {
                            ai.aiState = EnemyComponent.AIState.POUNCING;
                            ai.pounceTimer = 0.35f;
                            ai.pounceDamageDealt = false;
                            ai.pounceTarget.set(playerTransform.position);

                            // Lunge velocity impulse toward player
                            float pounceSpeed = 8.5f;
                            float vx = MathUtils.cosDeg(angleToPlayer) * pounceSpeed;
                            float vy = MathUtils.sinDeg(angleToPlayer) * pounceSpeed;
                            if (bodyComp != null && bodyComp.body != null) {
                                bodyComp.body.setLinearVelocity(vx, vy);
                            }

                            if (eventListener != null) {
                                eventListener.onHoundPounce(hound, houndPos, ai.pounceTarget);
                            }
                        }
                    }
                    break;

                case POUNCING:
                    ai.pounceTimer -= deltaTime;
                    direction.setFromWorldVector(bodyComp.body.getLinearVelocity());

                    if (ai.pounceTimer <= 0f) {
                        if (bodyComp != null && bodyComp.body != null) {
                            bodyComp.body.setLinearVelocity(0f, 0f);
                        }
                        ai.aiState = EnemyComponent.AIState.RECOVERING;
                        ai.recoveryTimer = 0.65f;
                    }
                    break;

                case RECOVERING:
                    if (bodyComp != null && bodyComp.body != null) {
                        bodyComp.body.setLinearVelocity(0f, 0f);
                    }
                    direction.setFromWorldVector(toPlayer);
                    ai.recoveryTimer -= deltaTime;
                    if (ai.recoveryTimer <= 0f) {
                        ai.aiState = EnemyComponent.AIState.FLANKING;
                        ai.attackCooldown = MathUtils.random(1.8f, 3.2f);
                    }
                    break;

                case STAGGERED:
                    if (combat.state == CombatStateComponent.State.IDLE) {
                        ai.aiState = EnemyComponent.AIState.FLANKING;
                        ai.attackCooldown = MathUtils.random(1.5f, 2.8f);
                    }
                    break;
            }
        }
    }
}
