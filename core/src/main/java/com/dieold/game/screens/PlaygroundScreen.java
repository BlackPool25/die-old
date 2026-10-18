package com.dieold.game.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.dieold.game.arena.ArenaBuilder;
import com.dieold.game.combat.HitstopController;
import com.dieold.game.combat.ScreenShake;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.EntityFactory;
import com.dieold.game.ecs.components.*;
import com.dieold.game.ecs.systems.*;
import com.dieold.game.physics.CombatContactListener;
import com.dieold.game.render.IsometricProjection;
import com.dieold.game.ui.CombatHUD;
import com.dieold.game.util.AssetResolver;
import com.dieold.game.world.*;

/**
 * 2.5D Isometric Combat Engine Playground Screen:
 * Ingests production atlases, simulates Box2D top-down physics, executes 180° deflection,
 * wall-slam ragdoll impacts, hitstop freeze, raycast LOS enemy agro, and real-time Scene2D HUD.
 * Milestone 3: Procedural Jigsaw Dungeon, Chamber Lockdown Gates, Clamped Camera, and Fog of War.
 */
public class PlaygroundScreen implements Screen,
        InputSystem.InputEventListener,
        EnemyAISystem.HoundEventListener,
        CombatContactListener.CombatEventListener {

    private final SpriteBatch batch;
    private final OrthographicCamera gameCamera;
    private final Viewport gameViewport;

    private TextureAtlas charactersAtlas;
    private TextureAtlas tilesetAtlas;
    private TextureAtlas uiAtlas;
    private TextureAtlas vfxAtlas;

    private World world;
    private Engine engine;
    private EntityFactory entityFactory;
    private ArenaBuilder arenaBuilder;
    private DungeonLevel dungeonLevel;
    private JigsawDungeonGenerator dungeonGenerator;
    private Array<PlacedRoom> dungeonRooms;
    private ChamberCameraSystem cameraSystem;
    private FogOfWarSystem fogOfWar;
    private RoomLockdownManager lockdownManager;

    private HitstopController hitstop;
    private ScreenShake screenShake;
    private CombatContactListener contactListener;
    private CombatHUD hud;

    private Entity player;
    private final ArrayHoundList hounds = new ArrayHoundList();

    private final Vector2 tempPlayerScreen = new Vector2();

    public PlaygroundScreen() {
        this.batch = new SpriteBatch();

        // Balanced action perspective: 640x360 virtual units gives clean 3x pixel scaling on 1080p
        // Characters scaled to ~0.70x feel agile and match the 64x32 floor tiles and 64x96 pillars
        this.gameCamera = new OrthographicCamera();
        this.gameViewport = new ExtendViewport(640, 360, gameCamera);

        initPlayground();
    }

    private void initPlayground() {
        // 1. Load Genuine Production Atlases
        charactersAtlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/characters.atlas"));
        tilesetAtlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/tileset.atlas"));
        uiAtlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/ui.atlas"));
        vfxAtlas = new TextureAtlas(AssetResolver.resolve("assets/atlases/vfx.atlas"));

        // 2. Physics & Combat Controllers
        world = new World(new Vector2(0f, 0f), true);
        hitstop = new HitstopController();
        screenShake = new ScreenShake();
        contactListener = new CombatContactListener(hitstop, screenShake);
        contactListener.setEventListener(this);
        world.setContactListener(contactListener);

        // 3. Ashley ECS Engine
        engine = new Engine();
        entityFactory = new EntityFactory(engine, world);

        // 4. Procedural Jigsaw Dungeon Generation (12 Chambers, Biome 1 Obsidian Foundry)
        dungeonGenerator = new JigsawDungeonGenerator();
        dungeonRooms = dungeonGenerator.generate(20261018L, 12);
        dungeonLevel = new DungeonLevel();
        dungeonLevel.buildDungeon(world, tilesetAtlas, dungeonRooms);
        arenaBuilder = dungeonLevel;

        // 5. Chamber Level Engine Systems
        cameraSystem = new ChamberCameraSystem(gameCamera, dungeonLevel);
        fogOfWar = new FogOfWarSystem(dungeonLevel);
        lockdownManager = new RoomLockdownManager(dungeonLevel, world, entityFactory, screenShake);

        // 6. Register Ashley Systems in strict sequence
        InputSystem inputSystem = new InputSystem(gameCamera);
        inputSystem.setListener(this);

        EnemyAISystem enemySystem = new EnemyAISystem(world);
        enemySystem.setEventListener(this);

        CombatSystem combatSystem = new CombatSystem(world);
        PhysicsSystem physicsSystem = new PhysicsSystem(world, hitstop, contactListener);
        AnimationSystem animationSystem = new AnimationSystem(charactersAtlas, vfxAtlas, hitstop);
        RenderSystem renderSystem = new RenderSystem(batch, gameCamera, dungeonLevel, screenShake);
        renderSystem.setFogOfWarSystem(fogOfWar);

        engine.addSystem(inputSystem);     // Priority 10
        engine.addSystem(enemySystem);    // Priority 30
        engine.addSystem(combatSystem);   // Priority 40
        engine.addSystem(physicsSystem);  // Priority 50
        engine.addSystem(animationSystem);// Priority 70
        engine.addSystem(renderSystem);   // Priority 80

        // 7. Spawn Player Youth in IngressHub spawn chamber
        PlacedRoom ingressHub = dungeonRooms.get(0);
        float spawnX = ingressHub.gridX + 8.0f;
        float spawnY = ingressHub.gridY + 8.0f;
        if (ingressHub.template.playerSpawns.size > 0) {
            Vector2 sp = ingressHub.template.playerSpawns.get(0);
            spawnX = ingressHub.gridX + sp.x;
            spawnY = ingressHub.gridY + sp.y;
        }
        player = entityFactory.createPlayer(spawnX, spawnY);
        cameraSystem.setRoom(ingressHub, true);
        fogOfWar.discoverRoom(ingressHub);

        // 8. Connect Production Scene2D HUD
        Viewport hudViewport = new ScreenViewport();
        hud = new CombatHUD(hudViewport, uiAtlas);

        // Center camera initially on player
        IsometricProjection.worldToScreen(spawnX, spawnY, 0f, tempPlayerScreen);
        gameCamera.position.set(tempPlayerScreen.x, tempPlayerScreen.y, 0f);
        gameCamera.update();
    }

    private void spawnHound(float x, float y, float flankOffset) {
        Entity hound = entityFactory.createFoundryHound(x, y);
        EnemyComponent ai = ComponentMappers.enemy.get(hound);
        if (ai != null) {
            ai.patrolOrigin.set(x, y);
            ai.flankAngleOffset = flankOffset;
            ai.agroDistance = 8.5f;
            ai.proximitySoundDistance = 3.2f;
        }
        hounds.add(hound);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.06f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. Camera Tracking & Chamber Clamping, Fog of War, and Chamber Lockdown
        if (player != null) {
            Transform2DComponent pTransform = ComponentMappers.transform.get(player);
            if (pTransform != null) {
                cameraSystem.update(delta, pTransform.position.x, pTransform.position.y, pTransform.z);
                fogOfWar.update(delta, pTransform.position.x, pTransform.position.y);
                lockdownManager.update(delta, pTransform.position.x, pTransform.position.y);
            }
        }

        // 2. Update ECS Engine
        engine.update(delta);

        // 3. Update & Draw HUD
        if (player != null) {
            PlayerComponent playerComp = ComponentMappers.player.get(player);
            HealthComponent health = ComponentMappers.health.get(player);
            PoiseComponent poise = ComponentMappers.poise.get(player);
            hud.update(delta, playerComp, health, poise);
            hud.draw();
        }
    }

    // InputSystem Callbacks
    @Override
    public void onPlayerAttack(Entity player, int comboStep, float angle) {
        Transform2DComponent transform = ComponentMappers.transform.get(player);
        if (transform == null) return;

        // Spawn slash arc VFX
        float spawnDist = 0.8f;
        float sx = transform.position.x + MathUtils.cosDeg(angle) * spawnDist;
        float sy = transform.position.y + MathUtils.sinDeg(angle) * spawnDist;
        entityFactory.createVFX("slash_arc", sx, sy, angle, 1.2f);

        // Create melee damage hitbox
        float damage = (comboStep == 3) ? 35f : 20f;
        float poiseDamage = (comboStep == 3) ? 50f : 30f;
        float knockback = (comboStep == 3) ? 10.0f : 5.0f;
        entityFactory.createMeleeHitbox(player, sx, sy, angle, damage, poiseDamage, knockback, 0);
    }

    @Override
    public void onPlayerDash(Entity player, float x, float y, float angle) {
        // Spawn ground dust puff at dash origin
        entityFactory.createVFX("dust_puff", x, y, angle + 180f, 1.0f);
    }

    // Hound Callbacks
    @Override
    public void onHoundPounce(Entity hound, Vector2 start, Vector2 target) {
        float angle = MathUtils.radiansToDegrees * MathUtils.atan2(target.y - start.y, target.x - start.x);
        float hx = start.x + MathUtils.cosDeg(angle) * 0.8f;
        float hy = start.y + MathUtils.sinDeg(angle) * 0.8f;
        entityFactory.createMeleeHitbox(hound, hx, hy, angle, 22f, 35f, 6.0f, 1);
    }

    @Override
    public void onHoundShoot(Entity hound, Vector2 from, float angle) {
        float speed = 8.5f;
        float vx = MathUtils.cosDeg(angle) * speed;
        float vy = MathUtils.sinDeg(angle) * speed;
        entityFactory.createProjectile(from.x, from.y, vx, vy, 1);
        entityFactory.createVFX("slash_arc", from.x, from.y, angle, 0.6f);
    }

    @Override
    public void onHoundAlert(Entity hound, Vector2 pos) {
        // Amber ocular flare on alert
        entityFactory.createVFX("deflect_spark", pos.x, pos.y, 90f, 0.8f);
    }

    // Combat Contact Callbacks
    @Override
    public void onDeflect(float x, float y, float angle) {
        // Visceral deflect spark burst
        entityFactory.createVFX("deflect_spark", x, y, angle, 1.5f);
    }

    @Override
    public void onHit(float x, float y) {
        entityFactory.createVFX("deflect_spark", x, y, 0f, 1.0f);
    }

    @Override
    public void onWallSplat(float x, float y, float damage) {
        // Dust puff and violent impact sparks against the wall
        entityFactory.createVFX("dust_puff", x, y, 0f, 1.4f);
        entityFactory.createVFX("deflect_spark", x, y, 90f, 1.2f);
    }

    @Override
    public void onProjectileDestroyed(Entity projectile) {}

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height);
        hud.resize(width, height);
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        charactersAtlas.dispose();
        tilesetAtlas.dispose();
        uiAtlas.dispose();
        vfxAtlas.dispose();
        if (lockdownManager != null) {
            lockdownManager.dispose();
        }
        world.dispose();
    }

    private static class ArrayHoundList {
        private final Entity[] items = new Entity[8];
        private int size = 0;
        public void add(Entity e) {
            if (size < items.length) items[size++] = e;
        }
    }
}
