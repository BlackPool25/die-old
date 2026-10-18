package com.dieold.game.ecs;

import com.badlogic.ashley.core.ComponentMapper;
import com.dieold.game.ecs.components.*;

/**
 * Fast zero-allocation component mappers for Ashley ECS.
 */
public final class ComponentMappers {
    public static final ComponentMapper<Transform2DComponent> transform = ComponentMapper.getFor(Transform2DComponent.class);
    public static final ComponentMapper<VelocityComponent> velocity = ComponentMapper.getFor(VelocityComponent.class);
    public static final ComponentMapper<Box2DBodyComponent> body = ComponentMapper.getFor(Box2DBodyComponent.class);
    public static final ComponentMapper<AnimationComponent> animation = ComponentMapper.getFor(AnimationComponent.class);
    public static final ComponentMapper<DirectionComponent> direction = ComponentMapper.getFor(DirectionComponent.class);
    public static final ComponentMapper<CombatStateComponent> combat = ComponentMapper.getFor(CombatStateComponent.class);
    public static final ComponentMapper<PoiseComponent> poise = ComponentMapper.getFor(PoiseComponent.class);
    public static final ComponentMapper<HealthComponent> health = ComponentMapper.getFor(HealthComponent.class);
    public static final ComponentMapper<HitboxComponent> hitbox = ComponentMapper.getFor(HitboxComponent.class);
    public static final ComponentMapper<PlayerComponent> player = ComponentMapper.getFor(PlayerComponent.class);
    public static final ComponentMapper<EnemyComponent> enemy = ComponentMapper.getFor(EnemyComponent.class);
    public static final ComponentMapper<ProjectileComponent> projectile = ComponentMapper.getFor(ProjectileComponent.class);
    public static final ComponentMapper<RenderableComponent> renderable = ComponentMapper.getFor(RenderableComponent.class);
    public static final ComponentMapper<VFXComponent> vfx = ComponentMapper.getFor(VFXComponent.class);

    private ComponentMappers() {}
}
