package com.dieold.game.ecs.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;
import com.dieold.game.arena.ArenaBuilder;
import com.dieold.game.arena.ArenaBuilder.TileRenderItem;
import com.dieold.game.combat.ScreenShake;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.*;
import com.dieold.game.render.CutawayShader;
import com.dieold.game.render.IsometricProjection;
import com.dieold.game.world.DungeonLevel;
import com.dieold.game.world.FogOfWarSystem;
import com.dieold.game.world.PlacedRoom;

import java.util.Comparator;

/**
 * 2.5D Isometric Rendering Pipeline:
 * - 2:1 dimetric orthographic projection (32 pixels per meter).
 * - Dynamic Y-Sorting by baseline footprint (X + Y - 0.05*Z + offset).
 * - Ground Shadow Anchoring at (x, y, 0) with height attenuation.
 * - Atmospheric Volumetric Slag Glow, central Chrono-Rune pulse, and floating embers.
 * - Smooth dark atmospheric vignette / fog of war eliminating empty void feeling.
 * - Zero allocation steady-state loop.
 */
public class RenderSystem extends EntitySystem implements Disposable {

    public static class RenderEntry implements Pool.Poolable {
        public float depthKey;
        public float screenX;
        public float screenY;
        public float groundScreenX;
        public float groundScreenY;
        public float z;
        public float originX;
        public float originY;
        public float scaleX = 1f;
        public float scaleY = 1f;
        public float rotation = 0f;
        public boolean drawShadow = false;
        public float shadowRadius = 16f;
        public AtlasRegion region;
        public final Color tint = new Color(Color.WHITE);
        public boolean isWallOrPillar = false;
        public float worldX = 0f;
        public float worldY = 0f;

        @Override
        public void reset() {
            depthKey = 0f;
            screenX = 0f;
            screenY = 0f;
            groundScreenX = 0f;
            groundScreenY = 0f;
            z = 0f;
            originX = 0f;
            originY = 0f;
            scaleX = 1f;
            scaleY = 1f;
            rotation = 0f;
            drawShadow = false;
            shadowRadius = 16f;
            region = null;
            tint.set(Color.WHITE);
            isWallOrPillar = false;
            worldX = 0f;
            worldY = 0f;
        }
    }

    private static class EmberParticle {
        public float worldX;
        public float worldY;
        public float z;
        public float vx;
        public float vy;
        public float vz;
        public float lifetime;
        public float maxLifetime;
        public float size;
        public float phase;
        public final Color color = new Color(1f, 0.6f, 0.1f, 1f);

        public void reset(float x, float y) {
            this.worldX = x + MathUtils.random(-8f, 8f);
            this.worldY = y + MathUtils.random(-8f, 8f);
            this.z = MathUtils.random(0.1f, 1.5f);
            this.vx = MathUtils.random(-0.3f, 0.3f);
            this.vy = MathUtils.random(-0.3f, 0.3f);
            this.vz = MathUtils.random(0.8f, 1.8f);
            this.maxLifetime = MathUtils.random(2.0f, 4.5f);
            this.lifetime = MathUtils.random(0f, this.maxLifetime);
            this.size = MathUtils.random(2.0f, 4.5f);
            this.phase = MathUtils.random(0f, 6.28f);
        }
    }

    private static final Comparator<RenderEntry> DEPTH_COMPARATOR = new Comparator<RenderEntry>() {
        @Override
        public int compare(RenderEntry a, RenderEntry b) {
            return Float.compare(b.depthKey, a.depthKey);
        }
    };

    private final SpriteBatch batch;
    private final Camera camera;
    private final ArenaBuilder arenaBuilder;
    private final ScreenShake screenShake;

    private Texture shadowTexture;
    private Texture glowTexture;
    private Texture vignetteTexture;
    private Texture emberTexture;

    private float animationTime = 0f;

    private ImmutableArray<Entity> renderEntities;
    private ImmutableArray<Entity> vfxEntities;
    private ImmutableArray<Entity> playerEntities;

    // Zero-allocation pre-allocated entry pool and list
    private final Pool<RenderEntry> entryPool = new Pool<RenderEntry>(128, 512) {
        @Override
        protected RenderEntry newObject() {
            return new RenderEntry();
        }
    };
    private final Array<RenderEntry> sortedEntries = new Array<>(false, 256);

    // Atmospheric embers
    private static final int MAX_EMBERS = 40;
    private final EmberParticle[] embers = new EmberParticle[MAX_EMBERS];

    // Pre-allocated math vectors
    private final Vector2 tempScreen = new Vector2();
    private final Vector2 tempGround = new Vector2();
    private final Vector2 tempPlayerCutaway = new Vector2();
    private final Color tempFogColor = new Color();

    private final CutawayShader cutawayShader;
    private DungeonLevel dungeonLevel;
    private FogOfWarSystem fogOfWarSystem;

    public RenderSystem(SpriteBatch batch, Camera camera, ArenaBuilder arenaBuilder, ScreenShake screenShake) {
        super(80); // System Priority 80
        this.batch = batch;
        this.camera = camera;
        this.arenaBuilder = arenaBuilder;
        this.screenShake = screenShake;

        this.cutawayShader = new CutawayShader();
        if (arenaBuilder instanceof DungeonLevel dl) {
            this.dungeonLevel = dl;
            this.fogOfWarSystem = new FogOfWarSystem(dl);
        }

        createProceduralTextures();
        initEmbers();
    }

    public void setFogOfWarSystem(FogOfWarSystem fogOfWarSystem) {
        this.fogOfWarSystem = fogOfWarSystem;
    }

    public FogOfWarSystem getFogOfWarSystem() {
        return fogOfWarSystem;
    }

    public CutawayShader getCutawayShader() {
        return cutawayShader;
    }

    private void createProceduralTextures() {
        // 1. Drop Shadow Texture (32x16 oval)
        Pixmap pShadow = new Pixmap(32, 16, Pixmap.Format.RGBA8888);
        pShadow.setColor(0f, 0f, 0f, 0f);
        pShadow.fill();
        float cx = 16f, cy = 8f;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 32; x++) {
                float dx = (x - cx) / 15f;
                float dy = (y - cy) / 7f;
                float d = dx * dx + dy * dy;
                if (d <= 1.0f) {
                    pShadow.setColor(0f, 0f, 0f, MathUtils.clamp((1.0f - d) * 0.75f, 0f, 0.75f));
                    pShadow.drawPixel(x, y);
                }
            }
        }
        shadowTexture = new Texture(pShadow);
        pShadow.dispose();

        // 2. Radial Ambient Glow Texture (64x64 soft circular falloff)
        Pixmap pGlow = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pGlow.setColor(0f, 0f, 0f, 0f);
        pGlow.fill();
        float gcx = 32f, gcy = 32f;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                float dist = (float) Math.hypot(x - gcx, y - gcy) / 32f;
                if (dist <= 1.0f) {
                    float a = (float) Math.cos(dist * Math.PI * 0.5f);
                    pGlow.setColor(1f, 1f, 1f, a * a);
                    pGlow.drawPixel(x, y);
                }
            }
        }
        glowTexture = new Texture(pGlow);
        pGlow.dispose();

        // 3. Tiny Ember Particle Dot (4x4)
        Pixmap pEmber = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pEmber.setColor(1f, 1f, 1f, 1f);
        pEmber.fill();
        emberTexture = new Texture(pEmber);
        pEmber.dispose();

        // 4. Atmospheric Vignette / Fog Mask (256x256)
        Pixmap pVignette = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        pVignette.setColor(0f, 0f, 0f, 0f);
        pVignette.fill();
        float vcx = 128f, vcy = 128f;
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                float dist = (float) Math.hypot(x - vcx, y - vcy) / 128f;
                // Inner 40% transparent, outer fading to dark obsidian fog
                float a = MathUtils.clamp((dist - 0.35f) / 0.65f, 0f, 0.88f);
                pVignette.setColor(0.04f, 0.04f, 0.06f, a);
                pVignette.drawPixel(x, y);
            }
        }
        vignetteTexture = new Texture(pVignette);
        pVignette.dispose();
    }

    private void initEmbers() {
        for (int i = 0; i < MAX_EMBERS; i++) {
            embers[i] = new EmberParticle();
            embers[i].reset(10.5f, 10.5f);
        }
    }

    @Override
    public void addedToEngine(Engine engine) {
        renderEntities = engine.getEntitiesFor(
            Family.all(Transform2DComponent.class, RenderableComponent.class, AnimationComponent.class).get()
        );
        vfxEntities = engine.getEntitiesFor(
            Family.all(Transform2DComponent.class, VFXComponent.class).get()
        );
        playerEntities = engine.getEntitiesFor(
            Family.all(PlayerComponent.class, Transform2DComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        animationTime += deltaTime;

        // Apply screen shake to camera
        float prevCamX = camera.position.x;
        float prevCamY = camera.position.y;
        if (screenShake != null) {
            screenShake.update(deltaTime);
            camera.position.x += screenShake.getOffsetX();
            camera.position.y += screenShake.getOffsetY();
        }
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 1. Render Floor Tiles (Ground Layer)
        Array<TileRenderItem> floorTiles = arenaBuilder.getFloorTiles();
        int numFloor = floorTiles.size;
        for (int i = 0; i < numFloor; i++) {
            TileRenderItem tile = floorTiles.get(i);
            if (tile.region != null) {
                IsometricProjection.worldGroundToScreen(tile.worldX, tile.worldY, tempScreen);
                if (dungeonLevel != null && fogOfWarSystem != null) {
                    PlacedRoom r = dungeonLevel.getRoomAt(tile.worldX, tile.worldY);
                    fogOfWarSystem.getRoomTint(r, tempFogColor);
                    batch.setColor(tempFogColor);
                }
                batch.draw(tile.region, tempScreen.x - tile.originX, tempScreen.y - tile.originY);
            }
        }
        batch.setColor(Color.WHITE);

        // 2. Render Atmospheric Molten Slag Glow Decals (Vents & Furnace Vents)
        renderAmbientGlows();

        // 3. Prepare & Populate Depth Sorted Entries
        clearEntries();

        // Add static walls and pillars
        Array<TileRenderItem> walls = arenaBuilder.getWallAndPillarTiles();
        int numWalls = walls.size;
        for (int i = 0; i < numWalls; i++) {
            TileRenderItem wall = walls.get(i);
            if (wall.region != null) {
                IsometricProjection.worldToScreen(wall.worldX, wall.worldY, wall.z, tempScreen);
                IsometricProjection.worldGroundToScreen(wall.worldX, wall.worldY, tempGround);

                RenderEntry entry = entryPool.obtain();
                entry.depthKey = IsometricProjection.calculateDepthKey(wall.worldX, wall.worldY, wall.z, wall.ySortOffset);
                entry.screenX = tempScreen.x;
                entry.screenY = tempScreen.y;
                entry.groundScreenX = tempGround.x;
                entry.groundScreenY = tempGround.y;
                entry.z = wall.z;
                entry.originX = wall.originX;
                entry.originY = wall.originY;
                entry.region = wall.region;
                entry.drawShadow = false;
                entry.isWallOrPillar = true;
                entry.worldX = wall.worldX;
                entry.worldY = wall.worldY;
                if (dungeonLevel != null && fogOfWarSystem != null) {
                    PlacedRoom r = dungeonLevel.getRoomAt(wall.worldX, wall.worldY);
                    fogOfWarSystem.getRoomTint(r, entry.tint);
                }
                sortedEntries.add(entry);
            }
        }

        // Add dynamic entities (Player & Foundry Hounds)
        int numEntities = renderEntities.size();
        for (int i = 0; i < numEntities; i++) {
            Entity entity = renderEntities.get(i);
            Transform2DComponent transform = ComponentMappers.transform.get(entity);
            RenderableComponent renderable = ComponentMappers.renderable.get(entity);
            AnimationComponent anim = ComponentMappers.animation.get(entity);

            if (!renderable.visible || anim.currentRegion == null) {
                continue;
            }

            IsometricProjection.worldToScreen(transform.position.x, transform.position.y, transform.z, tempScreen);
            IsometricProjection.worldGroundToScreen(transform.position.x, transform.position.y, tempGround);

            RenderEntry entry = entryPool.obtain();
            entry.depthKey = IsometricProjection.calculateDepthKey(transform.position.x, transform.position.y, transform.z, renderable.ySortOffset);
            entry.screenX = tempScreen.x;
            entry.screenY = tempScreen.y;
            entry.groundScreenX = tempGround.x;
            entry.groundScreenY = tempGround.y;
            entry.z = transform.z;

            // Character/Hound sprite origin centered at feet (hounds: 25px, player: 19px)
            entry.originX = anim.currentRegion.getRegionWidth() / 2.0f;
            boolean isHound = anim.characterPrefix != null && anim.characterPrefix.contains("hound");
            entry.originY = isHound ? 25.0f : 19.0f;
            entry.scaleX = renderable.scale;
            entry.scaleY = renderable.scale;
            entry.region = anim.currentRegion;
            entry.drawShadow = renderable.drawShadow;
            entry.shadowRadius = renderable.shadowRadiusX * renderable.scale;
            entry.tint.set(renderable.tint);
            entry.isWallOrPillar = false;
            entry.worldX = transform.position.x;
            entry.worldY = transform.position.y;
            sortedEntries.add(entry);
        }

        // Add VFX entities
        int numVfx = vfxEntities.size();
        for (int i = 0; i < numVfx; i++) {
            Entity entity = vfxEntities.get(i);
            Transform2DComponent transform = ComponentMappers.transform.get(entity);
            VFXComponent vfx = ComponentMappers.vfx.get(entity);

            if (vfx.finished || vfx.currentRegion == null) {
                continue;
            }

            IsometricProjection.worldToScreen(transform.position.x + vfx.offset.x, transform.position.y + vfx.offset.y, transform.z, tempScreen);

            RenderEntry entry = entryPool.obtain();
            entry.depthKey = IsometricProjection.calculateDepthKey(transform.position.x, transform.position.y, transform.z, 0.2f);
            entry.screenX = tempScreen.x;
            entry.screenY = tempScreen.y;
            entry.originX = vfx.currentRegion.getRegionWidth() / 2.0f;
            entry.originY = vfx.currentRegion.getRegionHeight() / 2.0f;
            entry.rotation = vfx.rotation;
            entry.scaleX = vfx.scale;
            entry.scaleY = vfx.scale;
            entry.region = vfx.currentRegion;
            entry.drawShadow = false;
            entry.isWallOrPillar = false;
            entry.worldX = transform.position.x;
            entry.worldY = transform.position.y;
            sortedEntries.add(entry);
        }

        // Sort all entries descending by depth key (painter's algorithm)
        sortedEntries.sort(DEPTH_COMPARATOR);

        // 4. Render Ground Drop Shadows
        int totalSorted = sortedEntries.size;
        for (int i = 0; i < totalSorted; i++) {
            RenderEntry entry = sortedEntries.get(i);
            if (entry.drawShadow && shadowTexture != null) {
                float shadowScale = 1.0f / (1.0f + 0.35f * entry.z);
                float shadowAlpha = 0.65f / (1.0f + 0.4f * entry.z);
                float w = entry.shadowRadius * 2.0f * shadowScale;
                float h = (entry.shadowRadius) * shadowScale;

                batch.setColor(0f, 0f, 0f, shadowAlpha);
                batch.draw(shadowTexture, entry.groundScreenX - w / 2.0f, entry.groundScreenY - h / 2.0f, w, h);
                batch.setColor(Color.WHITE);
            }
        }

        // Track player screen position for foreground cutaway shader
        float playerScreenX = 0f;
        float playerScreenY = 0f;
        boolean hasPlayer = false;
        if (playerEntities != null && playerEntities.size() > 0) {
            Entity p = playerEntities.get(0);
            Transform2DComponent pTrans = ComponentMappers.transform.get(p);
            if (pTrans != null) {
                IsometricProjection.worldToScreen(pTrans.position.x, pTrans.position.y, pTrans.z, tempPlayerCutaway);
                playerScreenX = tempPlayerCutaway.x;
                playerScreenY = tempPlayerCutaway.y;
                hasPlayer = true;
            }
        }

        boolean cutawayActive = cutawayShader != null && cutawayShader.isCompiled();
        if (cutawayActive) {
            batch.setShader(cutawayShader.getShaderProgram());
        }

        // 5. Render Depth-Sorted Sprites (Entities, Walls, Pillars, VFX)
        for (int i = 0; i < totalSorted; i++) {
            RenderEntry entry = sortedEntries.get(i);
            if (entry.region != null) {
                if (cutawayActive) {
                    if (entry.isWallOrPillar && hasPlayer) {
                        float dist = (float) Math.hypot(entry.screenX - playerScreenX, entry.screenY - playerScreenY);
                        if (dist < 64.0f) {
                            cutawayShader.bindAperture(playerScreenX, playerScreenY, 52.0f, true);
                        } else {
                            cutawayShader.bindAperture(0f, 0f, 0f, false);
                        }
                    } else {
                        cutawayShader.bindAperture(0f, 0f, 0f, false);
                    }
                }

                batch.setColor(entry.tint);
                if (entry.rotation != 0f || entry.scaleX != 1f || entry.scaleY != 1f) {
                    batch.draw(entry.region,
                        entry.screenX - entry.originX, entry.screenY - entry.originY,
                        entry.originX, entry.originY,
                        entry.region.getRegionWidth(), entry.region.getRegionHeight(),
                        entry.scaleX, entry.scaleY,
                        entry.rotation
                    );
                } else {
                    batch.draw(entry.region, entry.screenX - entry.originX, entry.screenY - entry.originY);
                }
            }
        }
        if (cutawayActive) {
            batch.setShader(null);
        }
        batch.setColor(Color.WHITE);

        // 6. Render Floating Foundry Embers
        renderEmbers(deltaTime);

        // 7. Render Atmospheric Isometric Fog & Vignette (Centered on Camera / Player)
        renderVignette();

        batch.end();

        // Restore camera position
        camera.position.x = prevCamX;
        camera.position.y = prevCamY;
        camera.update();
    }

    private void renderAmbientGlows() {
        if (glowTexture == null) return;

        // Heat Vent Ambient Orange Glow
        float orangePulse = 0.35f + 0.12f * MathUtils.sin(animationTime * 3.5f);
        batch.setColor(1.0f, 0.45f, 0.05f, orangePulse);
        float ventRadius = 56f;
        for (int i = 0; i < arenaBuilder.moltenVentPositions.size; i++) {
            Vector2 pos = arenaBuilder.moltenVentPositions.get(i);
            IsometricProjection.worldGroundToScreen(pos.x, pos.y, tempScreen);
            batch.draw(glowTexture, tempScreen.x - ventRadius, tempScreen.y - ventRadius * 0.5f, ventRadius * 2f, ventRadius);
        }

        // Furnace Vent Wall Glow
        for (int i = 0; i < arenaBuilder.furnaceVentPositions.size; i++) {
            Vector2 pos = arenaBuilder.furnaceVentPositions.get(i);
            IsometricProjection.worldToScreen(pos.x, pos.y, 0.5f, tempScreen);
            batch.draw(glowTexture, tempScreen.x - 36f, tempScreen.y - 18f, 72f, 36f);
        }

        // Central Chrono-Rune Cyan Glow
        float cyanPulse = 0.28f + 0.08f * MathUtils.sin(animationTime * 2.0f);
        batch.setColor(0.0f, 0.94f, 1.0f, cyanPulse);
        IsometricProjection.worldGroundToScreen(arenaBuilder.runeCircleCenter.x, arenaBuilder.runeCircleCenter.y, tempScreen);
        batch.draw(glowTexture, tempScreen.x - 72f, tempScreen.y - 36f, 144f, 72f);

        batch.setColor(Color.WHITE);
    }

    private void renderEmbers(float dt) {
        if (emberTexture == null) return;

        float centerOriginX = 10.5f;
        float centerOriginY = 10.5f;
        if (playerEntities != null && playerEntities.size() > 0) {
            Transform2DComponent pt = ComponentMappers.transform.get(playerEntities.get(0));
            if (pt != null) {
                centerOriginX = pt.position.x;
                centerOriginY = pt.position.y;
            }
        }

        for (int i = 0; i < MAX_EMBERS; i++) {
            EmberParticle e = embers[i];
            e.lifetime += dt;
            if (e.lifetime >= e.maxLifetime) {
                e.reset(centerOriginX, centerOriginY);
            }

            e.z += e.vz * dt;
            e.worldX += (e.vx + 0.15f * MathUtils.sin(e.phase + animationTime * 2f)) * dt;
            e.worldY += (e.vy + 0.15f * MathUtils.cos(e.phase + animationTime * 2f)) * dt;

            float lifeRatio = e.lifetime / e.maxLifetime;
            float alpha = (lifeRatio < 0.2f) ? (lifeRatio / 0.2f) : (1.0f - lifeRatio);

            IsometricProjection.worldToScreen(e.worldX, e.worldY, e.z, tempScreen);
            batch.setColor(1.0f, 0.65f, 0.15f, alpha * 0.85f);
            batch.draw(emberTexture, tempScreen.x, tempScreen.y, e.size, e.size);
        }
        batch.setColor(Color.WHITE);
    }

    private void renderVignette() {
        if (vignetteTexture == null) return;
        float vw = camera.viewportWidth * 1.5f;
        float vh = camera.viewportHeight * 1.5f;
        batch.draw(vignetteTexture, camera.position.x - vw / 2.0f, camera.position.y - vh / 2.0f, vw, vh);
    }

    private void clearEntries() {
        for (int i = 0; i < sortedEntries.size; i++) {
            entryPool.free(sortedEntries.get(i));
        }
        sortedEntries.clear();
    }

    @Override
    public void dispose() {
        if (shadowTexture != null) {
            shadowTexture.dispose();
            shadowTexture = null;
        }
        if (glowTexture != null) {
            glowTexture.dispose();
            glowTexture = null;
        }
        if (vignetteTexture != null) {
            vignetteTexture.dispose();
            vignetteTexture = null;
        }
        if (emberTexture != null) {
            emberTexture.dispose();
            emberTexture = null;
        }
        if (cutawayShader != null) {
            cutawayShader.dispose();
        }
    }
}
