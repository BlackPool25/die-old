package com.dieold.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.dieold.game.ecs.components.HealthComponent;
import com.dieold.game.ecs.components.PlayerComponent;
import com.dieold.game.ecs.components.PoiseComponent;

/**
 * Production HUD built via Scene2D.ui:
 * - Chrono-Pendant life dial with rotating needle reflecting age.
 * - Health and lingering ghost damage meters.
 * - Poise meter.
 * - Kinetic dash diamond pips.
 * - Alchemical runic socket strip.
 */
public class CombatHUD {

    private final Stage stage;
    private final TextureAtlas uiAtlas;

    private ChronoPendantActor pendantActor;
    private HealthMeterActor healthMeterActor;
    private DashPipsActor dashPipsActor;
    private RunicMatrixActor runicMatrixActor;

    public CombatHUD(Viewport viewport, TextureAtlas uiAtlas) {
        this.stage = new Stage(viewport);
        this.uiAtlas = uiAtlas;

        buildHUD();
    }

    private void buildHUD() {
        pendantActor = new ChronoPendantActor(uiAtlas);
        pendantActor.setPosition(24, stage.getViewport().getWorldHeight() - 152);

        healthMeterActor = new HealthMeterActor(uiAtlas);
        healthMeterActor.setPosition(164, stage.getViewport().getWorldHeight() - 68);

        dashPipsActor = new DashPipsActor(uiAtlas);
        dashPipsActor.setPosition(164, stage.getViewport().getWorldHeight() - 110);

        runicMatrixActor = new RunicMatrixActor(uiAtlas);
        runicMatrixActor.setPosition(stage.getViewport().getWorldWidth() - 360, 24);

        stage.addActor(pendantActor);
        stage.addActor(healthMeterActor);
        stage.addActor(dashPipsActor);
        stage.addActor(runicMatrixActor);
    }

    public void update(float dt, PlayerComponent player, HealthComponent health, PoiseComponent poise) {
        if (player != null) {
            pendantActor.setAge(player.age);
            dashPipsActor.setDashPips(player.dashPips, player.maxDashPips);
        }
        if (health != null) {
            healthMeterActor.setHealth(health.currentHp, health.ghostHp, health.maxHp);
        }
        if (poise != null) {
            healthMeterActor.setPoise(poise.currentPoise, poise.maxPoise);
        }
        stage.act(dt);
    }

    public void draw() {
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        pendantActor.setPosition(24, stage.getViewport().getWorldHeight() - 152);
        healthMeterActor.setPosition(164, stage.getViewport().getWorldHeight() - 68);
        dashPipsActor.setPosition(164, stage.getViewport().getWorldHeight() - 110);
        runicMatrixActor.setPosition(stage.getViewport().getWorldWidth() - 360, 24);
    }

    public Stage getStage() {
        return stage;
    }

    /**
     * Modular Chrono-Pendant: dial base, 360° rotating needle, glass overlay.
     */
    public static class ChronoPendantActor extends Actor {
        private final AtlasRegion dialBase;
        private final AtlasRegion needle;
        private final AtlasRegion glassOverlay;
        private float needleAngle = 0f;
        private float targetAngle = 0f;

        public ChronoPendantActor(TextureAtlas atlas) {
            this.dialBase = atlas.findRegion("chrono_dial_base");
            this.needle = atlas.findRegion("chrono_needle");
            this.glassOverlay = atlas.findRegion("chrono_glass_overlay");
            setSize(128, 128);
        }

        public void setAge(int age) {
            // Age 20: 0°, Age 80: 360°
            float normalized = (Math.max(20, Math.min(80, age)) - 20) / 60.0f;
            this.targetAngle = -normalized * 360f; // clockwise
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            // Smooth needle interpolation
            needleAngle += (targetAngle - needleAngle) * Math.min(1.0f, delta * 4.0f);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float x = getX();
            float y = getY();

            if (dialBase != null) {
                batch.draw(dialBase, x, y, 128, 128);
            }
            if (needle != null) {
                batch.draw(needle, x, y, 64, 64, 128, 128, 1f, 1f, needleAngle);
            }
            if (glassOverlay != null) {
                batch.draw(glassOverlay, x, y, 128, 128);
            }
        }
    }

    /**
     * NinePatch-style beveled cast-iron meter frame, lingering ghost damage, and poise fill.
     */
    public static class HealthMeterActor extends Actor {
        private final AtlasRegion meterFrame;
        private final AtlasRegion healthFill;
        private final AtlasRegion ghostFill;
        private final AtlasRegion poiseFill;

        private float currentHp = 100f;
        private float ghostHp = 100f;
        private float maxHp = 100f;
        private float currentPoise = 100f;
        private float maxPoise = 100f;

        public HealthMeterActor(TextureAtlas atlas) {
            this.meterFrame = atlas.findRegion("meter_frame");
            this.healthFill = atlas.findRegion("meter_health_fill");
            this.ghostFill = atlas.findRegion("meter_ghost_fill");
            this.poiseFill = atlas.findRegion("meter_poise_fill");
            setSize(256, 38);
        }

        public void setHealth(float current, float ghost, float max) {
            this.currentHp = current;
            this.ghostHp = ghost;
            this.maxHp = Math.max(1f, max);
        }

        public void setPoise(float current, float max) {
            this.currentPoise = current;
            this.maxPoise = Math.max(1f, max);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float x = getX();
            float y = getY();
            float maxBarWidth = 246f;

            // 1. Ghost HP fill (lingering drain)
            if (ghostFill != null) {
                float ghostWidth = maxBarWidth * Math.max(0f, Math.min(1f, ghostHp / maxHp));
                batch.draw(ghostFill, x + 5, y + 18, ghostWidth, 14);
            }

            // 2. Health fill (crisp crimson to amber)
            if (healthFill != null) {
                float hpWidth = maxBarWidth * Math.max(0f, Math.min(1f, currentHp / maxHp));
                batch.draw(healthFill, x + 5, y + 18, hpWidth, 14);
            }

            // 3. Poise fill (electric blue below health)
            if (poiseFill != null) {
                float poiseWidth = maxBarWidth * Math.max(0f, Math.min(1f, currentPoise / maxPoise));
                batch.draw(poiseFill, x + 5, y + 6, poiseWidth, 8);
            }

            // 4. Meter frame
            if (meterFrame != null) {
                batch.draw(meterFrame, x, y + 14, 256, 24);
            }
        }
    }

    /**
     * Kinetic dash diamond pips (ready vs empty).
     */
    public static class DashPipsActor extends Actor {
        private final AtlasRegion pipReady;
        private final AtlasRegion pipEmpty;
        private int pips = 3;
        private int maxPips = 3;

        public DashPipsActor(TextureAtlas atlas) {
            this.pipReady = atlas.findRegion("dash_pip_ready");
            this.pipEmpty = atlas.findRegion("dash_pip_empty");
            setSize(80, 20);
        }

        public void setDashPips(int pips, int max) {
            this.pips = pips;
            this.maxPips = max;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float x = getX();
            float y = getY();

            for (int i = 0; i < maxPips; i++) {
                AtlasRegion pip = (i < pips) ? pipReady : pipEmpty;
                if (pip != null) {
                    batch.draw(pip, x + (i * 26), y, 20, 20);
                }
            }
        }
    }

    /**
     * Alchemical runic socket matrix.
     */
    public static class RunicMatrixActor extends Actor {
        private final AtlasRegion socketEmpty;
        private final AtlasRegion socketCyan;
        private final AtlasRegion socketAmber;
        private final AtlasRegion socketPurple;
        private final AtlasRegion socketLocked;

        public RunicMatrixActor(TextureAtlas atlas) {
            this.socketEmpty = atlas.findRegion("socket_empty");
            this.socketCyan = atlas.findRegion("socket_active_cyan");
            this.socketAmber = atlas.findRegion("socket_active_amber");
            this.socketPurple = atlas.findRegion("socket_active_purple");
            this.socketLocked = atlas.findRegion("socket_locked");
            setSize(340, 44);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float x = getX();
            float y = getY();

            // Render 8-socket runic array
            AtlasRegion[] sockets = {
                socketCyan != null ? socketCyan : socketEmpty,
                socketAmber != null ? socketAmber : socketEmpty,
                socketPurple != null ? socketPurple : socketEmpty,
                socketEmpty,
                socketEmpty,
                socketLocked != null ? socketLocked : socketEmpty,
                socketLocked != null ? socketLocked : socketEmpty,
                socketLocked != null ? socketLocked : socketEmpty
            };

            for (int i = 0; i < sockets.length; i++) {
                if (sockets[i] != null) {
                    batch.draw(sockets[i], x + (i * 42), y, 38, 38);
                }
            }
        }
    }
}
