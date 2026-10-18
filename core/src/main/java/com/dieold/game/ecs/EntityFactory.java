package com.dieold.game.ecs;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.dieold.game.ecs.components.*;
import com.dieold.game.physics.PhysicsConstants;

/**
 * Factory for creating gameplay entities with zero-allocation component pools
 * and configured Box2D physics fixtures.
 */
public class EntityFactory {

    private final Engine engine;
    private final World world;

    public EntityFactory(Engine engine, World world) {
        this.engine = engine;
        this.world = world;
    }

    /**
     * Creates Player Youth ("The Acrobat").
     */
    public Entity createPlayer(float x, float y) {
        Entity entity = engine.createEntity();

        Transform2DComponent transform = engine.createComponent(Transform2DComponent.class);
        transform.position.set(x, y);

        VelocityComponent vel = engine.createComponent(VelocityComponent.class);
        DirectionComponent dir = engine.createComponent(DirectionComponent.class);
        dir.setFromAngle(0f); // Face East by default

        CombatStateComponent combat = engine.createComponent(CombatStateComponent.class);
        HealthComponent health = engine.createComponent(HealthComponent.class);
        health.maxHp = 100f;
        health.currentHp = 100f;
        health.ghostHp = 100f;

        PoiseComponent poise = engine.createComponent(PoiseComponent.class);
        poise.maxPoise = 100f;
        poise.currentPoise = 100f;

        PlayerComponent playerComp = engine.createComponent(PlayerComponent.class);

        AnimationComponent anim = engine.createComponent(AnimationComponent.class);
        anim.characterPrefix = "player_youth";
        anim.setAction("idle", true, 0.10f);

        RenderableComponent renderable = engine.createComponent(RenderableComponent.class);
        renderable.shadowRadiusX = 14f;

        // Box2D Dynamic Body
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.position.set(x, y);
        bdef.linearDamping = 8.0f; // Responsive action movement
        bdef.fixedRotation = true;

        Body body = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        shape.setRadius(0.38f);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 2.0f;
        fdef.friction = 0.2f;
        fdef.restitution = 0.0f;
        fdef.filter.categoryBits = PhysicsConstants.CATEGORY_PLAYER;
        fdef.filter.maskBits = PhysicsConstants.MASK_PLAYER;

        Fixture fixture = body.createFixture(fdef);
        fixture.setUserData(entity);
        body.setUserData(entity);
        shape.dispose();

        Box2DBodyComponent bodyComp = engine.createComponent(Box2DBodyComponent.class);
        bodyComp.body = body;

        entity.add(transform);
        entity.add(vel);
        entity.add(dir);
        entity.add(combat);
        entity.add(health);
        entity.add(poise);
        entity.add(playerComp);
        entity.add(anim);
        entity.add(renderable);
        entity.add(bodyComp);

        engine.addEntity(entity);
        return entity;
    }

    /**
     * Creates Biome 1 Automaton ("Foundry Hound").
     */
    public Entity createFoundryHound(float x, float y) {
        Entity entity = engine.createEntity();

        Transform2DComponent transform = engine.createComponent(Transform2DComponent.class);
        transform.position.set(x, y);

        VelocityComponent vel = engine.createComponent(VelocityComponent.class);
        DirectionComponent dir = engine.createComponent(DirectionComponent.class);
        dir.setFromAngle(180f); // Face West toward center

        CombatStateComponent combat = engine.createComponent(CombatStateComponent.class);
        HealthComponent health = engine.createComponent(HealthComponent.class);
        health.maxHp = 60f;
        health.currentHp = 60f;
        health.ghostHp = 60f;

        PoiseComponent poise = engine.createComponent(PoiseComponent.class);
        poise.maxPoise = 50f;
        poise.currentPoise = 50f;
        poise.recoveryRate = 20f;

        EnemyComponent enemy = engine.createComponent(EnemyComponent.class);
        enemy.targetDistance = 3.5f;

        AnimationComponent anim = engine.createComponent(AnimationComponent.class);
        anim.characterPrefix = "foundry_hound";
        anim.setAction("idle", true, 0.10f);

        RenderableComponent renderable = engine.createComponent(RenderableComponent.class);
        renderable.shadowRadiusX = 16f;

        // Box2D Dynamic Body
        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.position.set(x, y);
        bdef.linearDamping = 6.0f;
        bdef.fixedRotation = true;

        Body body = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        shape.setRadius(0.44f);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.density = 2.5f;
        fdef.friction = 0.3f;
        fdef.restitution = 0.1f;
        fdef.filter.categoryBits = PhysicsConstants.CATEGORY_ENEMY;
        fdef.filter.maskBits = PhysicsConstants.MASK_ENEMY;

        Fixture fixture = body.createFixture(fdef);
        fixture.setUserData(entity);
        body.setUserData(entity);
        shape.dispose();

        Box2DBodyComponent bodyComp = engine.createComponent(Box2DBodyComponent.class);
        bodyComp.body = body;

        entity.add(transform);
        entity.add(vel);
        entity.add(dir);
        entity.add(combat);
        entity.add(health);
        entity.add(poise);
        entity.add(enemy);
        entity.add(anim);
        entity.add(renderable);
        entity.add(bodyComp);

        engine.addEntity(entity);
        return entity;
    }

    /**
     * Creates a kinetic projectile bolt.
     */
    public Entity createProjectile(float x, float y, float vx, float vy, int faction) {
        Entity entity = engine.createEntity();

        Transform2DComponent transform = engine.createComponent(Transform2DComponent.class);
        transform.position.set(x, y);

        ProjectileComponent proj = engine.createComponent(ProjectileComponent.class);
        proj.faction = faction;
        proj.speed = (float) Math.hypot(vx, vy);
        proj.damage = (faction == 0) ? 35f : 18f;
        proj.poiseDamage = 25f;
        proj.parriable = true;

        VFXComponent vfx = engine.createComponent(VFXComponent.class);
        vfx.vfxName = "slash_arc";
        vfx.frameDuration = 0.08f;
        vfx.scale = 0.7f;

        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.position.set(x, y);
        bdef.bullet = true; // Continuous Collision Detection
        bdef.linearDamping = 0f;

        Body body = world.createBody(bdef);
        body.setLinearVelocity(vx, vy);

        CircleShape shape = new CircleShape();
        shape.setRadius(0.22f);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.isSensor = true;
        fdef.filter.categoryBits = PhysicsConstants.CATEGORY_PROJECTILE;
        fdef.filter.maskBits = (faction == 0) ? PhysicsConstants.MASK_PROJECTILE_DEFLECTED : PhysicsConstants.MASK_PROJECTILE_ENEMY;

        Fixture fixture = body.createFixture(fdef);
        fixture.setUserData(entity);
        body.setUserData(entity);
        shape.dispose();

        Box2DBodyComponent bodyComp = engine.createComponent(Box2DBodyComponent.class);
        bodyComp.body = body;

        entity.add(transform);
        entity.add(proj);
        entity.add(vfx);
        entity.add(bodyComp);

        engine.addEntity(entity);
        return entity;
    }

    /**
     * Creates a melee attack swing hitbox.
     */
    public Entity createMeleeHitbox(Entity owner, float x, float y, float angle, float damage, float poiseDamage, float knockbackSpeed, int faction) {
        Entity entity = engine.createEntity();

        Transform2DComponent transform = engine.createComponent(Transform2DComponent.class);
        transform.position.set(x, y);

        HitboxComponent hitbox = engine.createComponent(HitboxComponent.class);
        hitbox.active = true;
        hitbox.damage = damage;
        hitbox.poiseDamage = poiseDamage;
        hitbox.faction = faction;
        hitbox.duration = 0.12f;
        hitbox.knockbackImpulse.set(
            (float) Math.cos(Math.toRadians(angle)) * knockbackSpeed,
            (float) Math.sin(Math.toRadians(angle)) * knockbackSpeed
        );

        BodyDef bdef = new BodyDef();
        bdef.type = BodyDef.BodyType.DynamicBody;
        bdef.position.set(x, y);

        Body body = world.createBody(bdef);

        CircleShape shape = new CircleShape();
        shape.setRadius(0.75f);

        FixtureDef fdef = new FixtureDef();
        fdef.shape = shape;
        fdef.isSensor = true;
        fdef.filter.categoryBits = PhysicsConstants.CATEGORY_HITBOX;
        fdef.filter.maskBits = (faction == 0) ? PhysicsConstants.CATEGORY_ENEMY : PhysicsConstants.CATEGORY_PLAYER;

        Fixture fixture = body.createFixture(fdef);
        fixture.setUserData(entity);
        body.setUserData(entity);
        shape.dispose();

        Box2DBodyComponent bodyComp = engine.createComponent(Box2DBodyComponent.class);
        bodyComp.body = body;

        entity.add(transform);
        entity.add(hitbox);
        entity.add(bodyComp);

        engine.addEntity(entity);
        return entity;
    }

    /**
     * Spawns a directional VFX sprite (deflect spark burst, slash arc, or ground dust puff).
     */
    public Entity createVFX(String vfxName, float x, float y, float angle, float scale) {
        Entity entity = engine.createEntity();

        Transform2DComponent transform = engine.createComponent(Transform2DComponent.class);
        transform.position.set(x, y);

        VFXComponent vfx = engine.createComponent(VFXComponent.class);
        vfx.vfxName = vfxName;
        vfx.rotation = angle;
        vfx.scale = scale;
        vfx.frameDuration = 0.05f;

        entity.add(transform);
        entity.add(vfx);

        engine.addEntity(entity);
        return entity;
    }
}
