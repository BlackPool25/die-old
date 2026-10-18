package com.dieold.game.physics;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.dieold.game.combat.HitstopController;
import com.dieold.game.combat.ScreenShake;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.*;

/**
 * Box2D ContactListener handling melee hitboxes, kinetic projectile deflection,
 * and wall-slam ragdoll collisional physics with zero allocations.
 */
public class CombatContactListener implements ContactListener {

    public interface CombatEventListener {
        void onDeflect(float x, float y, float angle);
        void onHit(float x, float y);
        void onWallSplat(float x, float y, float damage);
        void onProjectileDestroyed(Entity projectile);
    }

    private final HitstopController hitstop;
    private final ScreenShake screenShake;
    private CombatEventListener eventListener;

    // Zero-allocation pre-allocated math vectors
    private final Vector2 tempVec1 = new Vector2();
    private final Vector2 tempVec2 = new Vector2();
    private final Vector2 tempVec3 = new Vector2();

    // Deferred projectile destruction list to avoid deleting during physics step
    public final Array<Entity> pendingDestroyEntities = new Array<>(false, 32);

    public CombatContactListener(HitstopController hitstop, ScreenShake screenShake) {
        this.hitstop = hitstop;
        this.screenShake = screenShake;
    }

    public void setEventListener(CombatEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fixA = contact.getFixtureA();
        Fixture fixB = contact.getFixtureB();

        Object dataA = fixA.getUserData();
        Object dataB = fixB.getUserData();
        Object bodyDataA = fixA.getBody().getUserData();
        Object bodyDataB = fixB.getBody().getUserData();

        // 1. Wall-Slam Collision Detection
        checkWallSlam(fixA, fixB, bodyDataA, bodyDataB);
        checkWallSlam(fixB, fixA, bodyDataB, bodyDataA);

        // 2. Projectile Collision & Deflection
        checkProjectileContact(fixA, fixB, bodyDataA, bodyDataB);
        checkProjectileContact(fixB, fixA, bodyDataB, bodyDataA);

        // 3. Melee Hitbox Contact
        checkHitboxContact(fixA, fixB, bodyDataA, bodyDataB);
        checkHitboxContact(fixB, fixA, bodyDataB, bodyDataA);
    }

    /**
     * Checks if an entity in knockback violently slams into a CATEGORY_WALL.
     */
    private void checkWallSlam(Fixture entityFix, Fixture wallFix, Object entityData, Object wallData) {
        if (wallFix.getFilterData().categoryBits != PhysicsConstants.CATEGORY_WALL) {
            return;
        }
        if (!(entityData instanceof Entity entity)) {
            return;
        }

        CombatStateComponent combat = ComponentMappers.combat.get(entity);
        HealthComponent health = ComponentMappers.health.get(entity);
        Box2DBodyComponent bodyComp = ComponentMappers.body.get(entity);

        if (combat == null || health == null || bodyComp == null || bodyComp.body == null) {
            return;
        }

        Vector2 vel = bodyComp.body.getLinearVelocity();
        float speed = vel.len();

        // If in knockback or experiencing high velocity impact
        if (combat.inKnockback || (combat.state == CombatStateComponent.State.HITSTUN && speed > 2.0f)) {
            float mass = bodyComp.body.getMass();
            // Impact momentum J = m * ||v||
            float momentum = mass * speed;
            // Crushing damage proportional to J
            float crushingDamage = momentum * 8.0f;
            health.takeDamage(crushingDamage);

            // Transition to WALL_SPLAT state (0.8s stun)
            combat.transitionTo(CombatStateComponent.State.WALL_SPLAT);
            combat.wallSplatTimer = 0.8f;
            combat.inKnockback = false;

            // Zero out body velocity so entity stops against the wall
            bodyComp.body.setLinearVelocity(0f, 0f);

            // Screen shake & hitstop
            screenShake.addShake(0.35f, 0.25f);
            hitstop.triggerHitstop(HitstopController.FRAMES_NORMAL_HIT);

            if (eventListener != null) {
                Vector2 pos = bodyComp.body.getPosition();
                eventListener.onWallSplat(pos.x, pos.y, crushingDamage);
            }
        }
    }

    /**
     * Handles projectile impacts and 180° frontal deflection arc.
     */
    private void checkProjectileContact(Fixture projFix, Fixture otherFix, Object projData, Object otherData) {
        if (projFix.getFilterData().categoryBits != PhysicsConstants.CATEGORY_PROJECTILE) {
            return;
        }
        if (!(projData instanceof Entity projEntity)) {
            return;
        }

        ProjectileComponent projectile = ComponentMappers.projectile.get(projEntity);
        Box2DBodyComponent projBodyComp = ComponentMappers.body.get(projEntity);
        if (projectile == null || projBodyComp == null || projBodyComp.body == null) {
            return;
        }

        // Projectile hitting wall
        if (otherFix.getFilterData().categoryBits == PhysicsConstants.CATEGORY_WALL) {
            pendingDestroyEntities.add(projEntity);
            if (eventListener != null) {
                Vector2 pos = projBodyComp.body.getPosition();
                eventListener.onHit(pos.x, pos.y);
            }
            return;
        }

        // Projectile hitting entity
        if (otherData instanceof Entity targetEntity) {
            HealthComponent targetHealth = ComponentMappers.health.get(targetEntity);
            CombatStateComponent targetCombat = ComponentMappers.combat.get(targetEntity);
            Transform2DComponent targetTransform = ComponentMappers.transform.get(targetEntity);
            DirectionComponent targetDir = ComponentMappers.direction.get(targetEntity);
            Box2DBodyComponent targetBody = ComponentMappers.body.get(targetEntity);

            if (targetHealth == null || targetCombat == null || targetTransform == null) {
                return;
            }

            // Check if player deflecting enemy projectile
            if (projectile.faction == 1 && targetCombat.isDeflecting && projectile.parriable) {
                // Vector from player to incoming projectile
                Vector2 projPos = projBodyComp.body.getPosition();
                tempVec1.set(projPos).sub(targetTransform.position);
                float incomingAngle = MathUtils.radiansToDegrees * MathUtils.atan2(tempVec1.y, tempVec1.x);

                // Check 180° frontal deflection arc
                float diff = getAngleDifference(targetCombat.deflectAngle, incomingAngle);
                if (Math.abs(diff) <= 90.0f) {
                    // DEFLECTED!
                    projectile.faction = 0; // Friendly to player, hostile to enemies
                    projectile.deflected = true;

                    // Reverses linear velocity with +150% speed (-2.5 * v)
                    Vector2 currentVel = projBodyComp.body.getLinearVelocity();
                    tempVec2.set(currentVel).scl(-2.5f);
                    projBodyComp.body.setLinearVelocity(tempVec2);

                    // Update collision filter mask to hit enemies & walls
                    Filter filter = projFix.getFilterData();
                    filter.maskBits = PhysicsConstants.MASK_PROJECTILE_DEFLECTED;
                    projFix.setFilterData(filter);

                    // Hitstop (8 frames = 133ms) & Screen shake
                    hitstop.triggerHitstop(HitstopController.FRAMES_DEFLECTION);
                    screenShake.addShake(0.2f, 0.15f);

                    if (eventListener != null) {
                        eventListener.onDeflect(projPos.x, projPos.y, targetCombat.deflectAngle);
                    }
                    return;
                }
            }

            // Normal projectile hit on target
            // Player projectile hits enemy, or Enemy projectile hits player
            int targetFaction = (ComponentMappers.player.has(targetEntity)) ? 0 : 1;
            if (projectile.faction != targetFaction && !targetHealth.isInvulnerable) {
                float dmg = projectile.deflected ? (projectile.damage * 1.5f) : projectile.damage;
                targetHealth.takeDamage(dmg);

                PoiseComponent targetPoise = ComponentMappers.poise.get(targetEntity);
                if (targetPoise != null) {
                    targetPoise.damagePoise(projectile.poiseDamage);
                }

                if (targetBody != null && targetBody.body != null) {
                    tempVec3.set(projBodyComp.body.getLinearVelocity()).nor().scl(4.0f);
                    targetBody.body.setLinearVelocity(tempVec3);
                    targetCombat.inKnockback = true;
                    targetCombat.transitionTo(CombatStateComponent.State.HITSTUN);
                    targetCombat.hitstunTimer = 0.35f;
                }

                hitstop.triggerHitstop(HitstopController.FRAMES_NORMAL_HIT);
                pendingDestroyEntities.add(projEntity);

                if (eventListener != null) {
                    Vector2 pos = projBodyComp.body.getPosition();
                    eventListener.onHit(pos.x, pos.y);
                }
            }
        }
    }

    /**
     * Handles melee hitbox contacts (combo strikes, hound pounces).
     */
    private void checkHitboxContact(Fixture hitFix, Fixture targetFix, Object hitData, Object targetData) {
        if (hitFix.getFilterData().categoryBits != PhysicsConstants.CATEGORY_HITBOX) {
            return;
        }
        if (!(hitData instanceof Entity hitEntity) || !(targetData instanceof Entity targetEntity)) {
            return;
        }

        HitboxComponent hitbox = ComponentMappers.hitbox.get(hitEntity);
        if (hitbox == null || !hitbox.active) {
            return;
        }

        // Avoid self-damage
        if (hitEntity == targetEntity) {
            return;
        }

        // Avoid multi-hitting same entity during single swing
        if (hitbox.hitEntityIds.contains(targetEntity.hashCode())) {
            return;
        }

        HealthComponent targetHealth = ComponentMappers.health.get(targetEntity);
        CombatStateComponent targetCombat = ComponentMappers.combat.get(targetEntity);
        PoiseComponent targetPoise = ComponentMappers.poise.get(targetEntity);
        Box2DBodyComponent targetBody = ComponentMappers.body.get(targetEntity);

        if (targetHealth == null || targetCombat == null) {
            return;
        }

        int targetFaction = (ComponentMappers.player.has(targetEntity)) ? 0 : 1;
        if (hitbox.faction == targetFaction) {
            return; // Friendly fire disabled
        }

        // Check if target successfully deflected melee attack
        if (targetCombat.isDeflecting && hitbox.parriable) {
            Transform2DComponent hitTransform = ComponentMappers.transform.get(hitEntity);
            Transform2DComponent targetTransform = ComponentMappers.transform.get(targetEntity);
            if (hitTransform != null && targetTransform != null) {
                tempVec1.set(hitTransform.position).sub(targetTransform.position);
                float incomingAngle = MathUtils.radiansToDegrees * MathUtils.atan2(tempVec1.y, tempVec1.x);
                float diff = getAngleDifference(targetCombat.deflectAngle, incomingAngle);

                if (Math.abs(diff) <= 90.0f) {
                    // MELEE DEFLECTED!
                    hitbox.hitEntityIds.add(targetEntity.hashCode());
                    hitbox.active = false;

                    // Attacker's poise breaks immediately and applies staggering hitstun
                    PoiseComponent attackerPoise = ComponentMappers.poise.get(hitEntity);
                    CombatStateComponent attackerCombat = ComponentMappers.combat.get(hitEntity);
                    Box2DBodyComponent attackerBody = ComponentMappers.body.get(hitEntity);

                    if (attackerPoise != null) {
                        attackerPoise.breakPoise(1.2f);
                    }
                    if (attackerCombat != null) {
                        attackerCombat.transitionTo(CombatStateComponent.State.HITSTUN);
                        attackerCombat.hitstunTimer = 1.0f;
                        attackerCombat.inKnockback = true;
                    }
                    if (attackerBody != null && attackerBody.body != null) {
                        // Push attacker back slightly
                        tempVec2.set(hitTransform.position).sub(targetTransform.position).nor().scl(3.5f);
                        attackerBody.body.setLinearVelocity(tempVec2);
                    }

                    hitstop.triggerHitstop(HitstopController.FRAMES_DEFLECTION);
                    screenShake.addShake(0.25f, 0.2f);

                    if (eventListener != null) {
                        eventListener.onDeflect(targetTransform.position.x, targetTransform.position.y, targetCombat.deflectAngle);
                    }
                    return;
                }
            }
        }

        // Melee hit connects
        hitbox.hitEntityIds.add(targetEntity.hashCode());
        targetHealth.takeDamage(hitbox.damage);

        boolean guardCrushed = false;
        if (targetPoise != null) {
            targetPoise.damagePoise(hitbox.poiseDamage);
            if (targetPoise.isBroken) {
                guardCrushed = true;
            }
        }

        // Apply knockback
        if (targetBody != null && targetBody.body != null) {
            targetBody.body.setLinearVelocity(hitbox.knockbackImpulse);
            targetCombat.inKnockback = true;
            targetCombat.knockbackVelocity.set(hitbox.knockbackImpulse);
            targetCombat.transitionTo(CombatStateComponent.State.HITSTUN);
            targetCombat.hitstunTimer = guardCrushed ? 1.0f : 0.4f;
        }

        // Hitstop: 12 frames for guard crush, 4 frames for normal hit
        if (guardCrushed) {
            hitstop.triggerHitstop(HitstopController.FRAMES_GUARD_CRUSH);
            screenShake.addShake(0.3f, 0.2f);
        } else {
            hitstop.triggerHitstop(HitstopController.FRAMES_NORMAL_HIT);
        }

        if (eventListener != null && targetBody != null && targetBody.body != null) {
            Vector2 pos = targetBody.body.getPosition();
            eventListener.onHit(pos.x, pos.y);
        }
    }

    /**
     * Calculates the shortest signed angular difference between two angles in degrees (-180 to +180).
     */
    public static float getAngleDifference(float angleA, float angleB) {
        float diff = (angleB - angleA + 180f) % 360f - 180f;
        return diff < -180f ? diff + 360f : diff;
    }

    @Override public void endContact(Contact contact) {}
    @Override public void preSolve(Contact contact, Manifold oldManifold) {}
    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}
}
