package com.dieold.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.dieold.game.ecs.components.CombatStateComponent;
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
    private CrosshairReticleActor crosshairActor;

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

        crosshairActor = new CrosshairReticleActor(uiAtlas);

        stage.addActor(pendantActor);
        stage.addActor(healthMeterActor);
        stage.addActor(dashPipsActor);
        stage.addActor(runicMatrixActor);
        stage.addActor(crosshairActor);
    }

    public void update(float dt, PlayerComponent player, HealthComponent health, PoiseComponent poise) {
        update(dt, player, health, poise, null, false);
    }

    public void update(
        float dt,
        PlayerComponent player,
        HealthComponent health,
        PoiseComponent poise,
        com.dieold.game.ecs.components.CombatStateComponent combat,
        boolean nearChest
    ) {
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
        if (crosshairActor != null) {
            crosshairActor.updateStatus(combat, nearChest);
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

    /**
     * Precision Gothic Combat Reticle with dynamic mouse tracking, primary attack recovery arc,
     * RMB Heavy Attack charge indicator, and contextual Chrono-Chest interaction icon.
     */
    public static class CrosshairReticleActor extends Actor {
        private final AtlasRegion notch;
        private final AtlasRegion diamondReady;
        private final AtlasRegion diamondEmpty;
        private final AtlasRegion dot;
        private final Vector2 mousePos = new Vector2();

        private boolean attacking = false;
        private float attackRatio = 1.0f;
        private boolean heavyReady = true;
        private float heavyCooldownRatio = 1.0f;
        private boolean nearChest = false;

        public CrosshairReticleActor(TextureAtlas atlas) {
            this.notch = atlas.findRegion("meter_segment_notch");
            this.diamondReady = atlas.findRegion("dash_pip_ready");
            this.diamondEmpty = atlas.findRegion("dash_pip_empty");
            this.dot = atlas.findRegion("meter_health_fill");
        }

        public void updateStatus(CombatStateComponent combat, boolean nearChest) {
            this.nearChest = nearChest;
            if (combat != null) {
                this.heavyReady = combat.canHeavyAttack();
                if (combat.heavyAttackCooldownTimer > 0f) {
                    this.heavyCooldownRatio = 1.0f - (combat.heavyAttackCooldownTimer / CombatStateComponent.HEAVY_ATTACK_COOLDOWN);
                } else {
                    this.heavyCooldownRatio = 1.0f;
                }
                this.attacking = (combat.state == CombatStateComponent.State.ATTACKING || combat.state == CombatStateComponent.State.HEAVY_ATTACKING);
                if (combat.state == CombatStateComponent.State.ATTACKING) {
                    float totalDur = (combat.comboStep == 3) ? 0.625f : (combat.comboStep == 2 ? 0.42f : 0.38f);
                    this.attackRatio = Math.min(1.0f, combat.stateTimer / totalDur);
                } else if (combat.state == CombatStateComponent.State.HEAVY_ATTACKING) {
                    this.attackRatio = Math.min(1.0f, combat.stateTimer / 0.675f);
                } else {
                    this.attackRatio = 1.0f;
                }
            } else {
                this.heavyReady = true;
                this.heavyCooldownRatio = 1.0f;
                this.attacking = false;
                this.attackRatio = 1.0f;
            }
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            if (getStage() == null) return;
            mousePos.set(Gdx.input.getX(), Gdx.input.getY());
            getStage().screenToStageCoordinates(mousePos);

            float mx = mousePos.x;
            float my = mousePos.y;

            // Reticle center dot
            if (dot != null) {
                batch.setColor(1.0f, 0.85f, 0.4f, 0.9f);
                batch.draw(dot, mx - 1.5f, my - 1.5f, 3f, 3f);
            }

            // 4 directional brackets around reticle (radius ~12px)
            if (notch != null) {
                // When recovering from attack, brackets tint darker bronze, when ready bright amber
                if (attacking && attackRatio < 1.0f) {
                    batch.setColor(0.6f, 0.45f, 0.25f, 0.75f);
                } else {
                    batch.setColor(0.95f, 0.75f, 0.30f, 0.95f);
                }

                float r = attacking ? (10f + (1.0f - attackRatio) * 6f) : 12f;
                // Top tick
                batch.draw(notch, mx - 1f, my + r, 2f, 6f);
                // Bottom tick
                batch.draw(notch, mx - 1f, my - r - 6f, 2f, 6f);
                // Left tick (rotated 90)
                batch.draw(notch, mx - r - 6f, my - 1f, 0f, 0f, 2f, 6f, 1f, 1f, 90f);
                // Right tick (rotated 90)
                batch.draw(notch, mx + r, my - 1f, 0f, 0f, 2f, 6f, 1f, 1f, 90f);
            }

            // Heavy Attack (RMB) Ready Pip on right flank of crosshair
            if (heavyReady) {
                if (diamondReady != null) {
                    batch.setColor(1.0f, 0.75f, 0.2f, 1.0f);
                    batch.draw(diamondReady, mx + 18f, my - 6f, 12f, 12f);
                }
            } else {
                if (diamondEmpty != null) {
                    batch.setColor(0.6f, 0.6f, 0.7f, 0.6f);
                    batch.draw(diamondEmpty, mx + 18f, my - 5f, 10f, 10f);
                }
                // Cooldown charge indicator
                if (dot != null && heavyCooldownRatio > 0.05f) {
                    batch.setColor(0.3f, 0.8f, 1.0f, 0.85f);
                    batch.draw(dot, mx + 19f, my - 7f, 8f * heavyCooldownRatio, 2f);
                }
            }

            // Contextual Chrono-Chest interaction indicator: [E] OPEN
            if (nearChest) {
                if (diamondReady != null) {
                    float bob = (float) Math.sin(Gdx.graphics.getFrameId() * 0.15f) * 3f;
                    batch.setColor(0.3f, 0.95f, 0.85f, 1.0f);
                    batch.draw(diamondReady, mx - 7f, my + 20f + bob, 14f, 14f);
                }
            }

            batch.setColor(Color.WHITE);
        }
    }
}
