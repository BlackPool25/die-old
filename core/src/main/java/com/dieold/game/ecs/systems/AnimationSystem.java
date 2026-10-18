package com.dieold.game.ecs.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.dieold.game.combat.HitstopController;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.*;

/**
 * Manages 8-directional frame animations and region indexing from production atlases.
 * Pauses animation frame advance during hitstop freeze.
 */
public class AnimationSystem extends EntitySystem {

    private final TextureAtlas charactersAtlas;
    private final TextureAtlas vfxAtlas;
    private final HitstopController hitstop;

    private ImmutableArray<Entity> animatedEntities;
    private ImmutableArray<Entity> vfxEntities;

    // Fast animation frame cache to avoid repeated string concatenations and atlas lookups
    private final ObjectMap<String, Array<AtlasRegion>> regionCache = new ObjectMap<>();

    public AnimationSystem(TextureAtlas charactersAtlas, TextureAtlas vfxAtlas, HitstopController hitstop) {
        super(70); // System Priority 70
        this.charactersAtlas = charactersAtlas;
        this.vfxAtlas = vfxAtlas;
        this.hitstop = hitstop;
    }

    @Override
    public void addedToEngine(Engine engine) {
        animatedEntities = engine.getEntitiesFor(
            Family.all(AnimationComponent.class, DirectionComponent.class).get()
        );
        vfxEntities = engine.getEntitiesFor(
            Family.all(VFXComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        // Halt animation ticks during hitstop
        if (hitstop != null && hitstop.isFrozen()) {
            return;
        }

        // 1. Character & Enemy Animations
        int count = animatedEntities.size();
        for (int i = 0; i < count; i++) {
            Entity entity = animatedEntities.get(i);
            AnimationComponent anim = ComponentMappers.animation.get(entity);
            DirectionComponent dir = ComponentMappers.direction.get(entity);
            CombatStateComponent combat = ComponentMappers.combat.get(entity);
            EnemyComponent enemy = ComponentMappers.enemy.get(entity);

            // Determine appropriate action based on combat & AI state with velocity stride matching
            syncActionFromState(anim, combat, enemy, entity);

            anim.directionIndex = dir.directionIndex;
            anim.stateTime += deltaTime;

            Array<AtlasRegion> frames = getFrames(charactersAtlas, anim.characterPrefix, anim.action, anim.directionIndex);
            if (frames != null && frames.size > 0) {
                int frameIndex;
                if (anim.looping) {
                    frameIndex = ((int) (anim.stateTime / anim.frameDuration)) % frames.size;
                } else {
                    frameIndex = (int) (anim.stateTime / anim.frameDuration);
                    if (frameIndex >= frames.size) {
                        frameIndex = frames.size - 1;
                        anim.finished = true;
                    }
                }
                anim.currentFrameIndex = frameIndex;
                anim.currentRegion = frames.get(frameIndex);
            }
        }

        // 2. VFX Animations
        int numVfx = vfxEntities.size();
        Engine engine = getEngine();
        for (int i = 0; i < numVfx; i++) {
            Entity entity = vfxEntities.get(i);
            VFXComponent vfx = ComponentMappers.vfx.get(entity);
            vfx.stateTime += deltaTime;

            Array<AtlasRegion> frames = getVfxFrames(vfxAtlas, vfx.vfxName);
            if (frames != null && frames.size > 0) {
                int frameIndex = (int) (vfx.stateTime / vfx.frameDuration);
                if (frameIndex >= frames.size) {
                    vfx.finished = true;
                    if (engine != null) {
                        engine.removeEntity(entity);
                    }
                } else {
                    vfx.currentFrameIndex = frameIndex;
                    vfx.currentRegion = frames.get(frameIndex);
                }
            }
        }
    }

    private void syncActionFromState(AnimationComponent anim, CombatStateComponent combat, EnemyComponent enemy, Entity entity) {
        if (combat == null) return;

        Box2DBodyComponent bodyComp = ComponentMappers.body.get(entity);
        float currentSpeed = (bodyComp != null && bodyComp.body != null) ? bodyComp.body.getLinearVelocity().len() : 0f;
        boolean isHound = anim.characterPrefix != null && anim.characterPrefix.contains("hound");

        if (combat.state == CombatStateComponent.State.DEAD) {
            anim.setAction("death", false, 0.10f);
        } else if (combat.state == CombatStateComponent.State.HITSTUN || combat.state == CombatStateComponent.State.WALL_SPLAT) {
            anim.setAction("hit_react", false, 0.08f);
        } else if (combat.state == CombatStateComponent.State.DEFLECTING) {
            anim.setAction("deflect_stance", true, 0.10f);
        } else if (combat.state == CombatStateComponent.State.ATTACKING) {
            anim.setAction("attack_light_combo", false, 0.05f);
        } else if (enemy != null && enemy.aiState == EnemyComponent.AIState.TELEGRAPHING) {
            anim.setAction("pounce_telegraph", false, 0.080f);
        } else if (combat.state == CombatStateComponent.State.SPRINT || combat.state == CombatStateComponent.State.DASHING) {
            if (isHound) {
                // Quadruped stride matching: 1.0m stride across 8 frames
                // Scales dynamically with velocity so paws plant solidly without skating
                float frameDur = MathUtils.clamp(0.24f / Math.max(1.2f, currentSpeed), 0.085f, 0.14f);
                anim.setAction("trot", true, frameDur);
            } else {
                // Player sprint
                float frameDur = MathUtils.clamp(0.42f / Math.max(2.0f, currentSpeed), 0.065f, 0.10f);
                anim.setAction("sprint", true, frameDur);
            }
        } else {
            // Idle breathing cadence
            float idleDur = isHound ? 0.14f : 0.12f;
            anim.setAction("idle", true, idleDur);
        }
    }

    private Array<AtlasRegion> getFrames(TextureAtlas atlas, String character, String action, int dir) {
        String key = character + "_" + action + "_dir" + dir;
        Array<AtlasRegion> frames = regionCache.get(key);
        if (frames == null) {
            frames = atlas.findRegions(key);
            if (frames == null || frames.size == 0) {
                // Fallback to dir0 if specific angle is missing
                frames = atlas.findRegions(character + "_" + action + "_dir0");
            }
            if (frames != null && frames.size > 0) {
                regionCache.put(key, frames);
            }
        }
        return frames;
    }

    private Array<AtlasRegion> getVfxFrames(TextureAtlas atlas, String vfxName) {
        Array<AtlasRegion> frames = regionCache.get(vfxName);
        if (frames == null) {
            frames = atlas.findRegions(vfxName);
            if (frames != null && frames.size > 0) {
                regionCache.put(vfxName, frames);
            }
        }
        return frames;
    }
}
