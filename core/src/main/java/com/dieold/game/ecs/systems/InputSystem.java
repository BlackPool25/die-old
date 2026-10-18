package com.dieold.game.ecs.systems;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.dieold.game.ecs.ComponentMappers;
import com.dieold.game.ecs.components.*;
import com.dieold.game.render.IsometricProjection;

/**
 * Handles keyboard and mouse hardware input, translating into camera-aligned 2.5D movement,
 * aiming reticle orientation, combo chains, deflection stance, and phase dash.
 */
public class InputSystem extends EntitySystem {

    public interface InputEventListener {
        void onPlayerAttack(Entity player, int comboStep, float worldAngle, float screenAngle);
        void onPlayerHeavyAttack(Entity player, float worldAngle, float screenAngle);
        void onPlayerDash(Entity player, float x, float y, float angle);
    }

    private final Camera camera;
    private InputEventListener listener;

    private ImmutableArray<Entity> playerEntities;

    // Zero-allocation pre-allocated vectors
    private final Vector2 inputDir = new Vector2();
    private final Vector2 worldMoveDir = new Vector2();
    private final Vector3 screenCoords = new Vector3();
    private final Vector2 mouseWorld = new Vector2();
    private final Vector2 aimVector = new Vector2();
    private final Vector2 tempPlayerScreen = new Vector2();

    public InputSystem(Camera camera) {
        super(10); // System Priority 10
        this.camera = camera;
    }

    public void setListener(InputEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void addedToEngine(Engine engine) {
        playerEntities = engine.getEntitiesFor(
            Family.all(PlayerComponent.class, Transform2DComponent.class, CombatStateComponent.class, DirectionComponent.class).get()
        );
    }

    @Override
    public void update(float deltaTime) {
        if (playerEntities.size() == 0) {
            return;
        }

        Entity player = playerEntities.get(0);
        Transform2DComponent transform = ComponentMappers.transform.get(player);
        CombatStateComponent combat = ComponentMappers.combat.get(player);
        DirectionComponent direction = ComponentMappers.direction.get(player);
        Box2DBodyComponent bodyComp = ComponentMappers.body.get(player);
        PlayerComponent playerComp = ComponentMappers.player.get(player);

        if (combat.state == CombatStateComponent.State.DEAD) {
            if (bodyComp != null && bodyComp.body != null) {
                bodyComp.body.setLinearVelocity(0f, 0f);
            }
            return;
        }

        // 1. Mouse Aiming Reticle (Screen Space and World Space)
        screenCoords.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        camera.unproject(screenCoords);

        IsometricProjection.worldGroundToScreen(transform.position.x, transform.position.y, tempPlayerScreen);
        float screenDX = screenCoords.x - tempPlayerScreen.x;
        float screenDY = screenCoords.y - tempPlayerScreen.y;
        float screenAngle = MathUtils.radiansToDegrees * MathUtils.atan2(screenDY, screenDX);

        // Convert screen delta into dimetric world ground delta for Box2D velocity and hitboxes:
        float worldDX = 0.5f * (screenDY / 16.0f + screenDX / 27.712812f);
        float worldDY = 0.5f * (screenDY / 16.0f - screenDX / 27.712812f);
        float worldAngle = MathUtils.radiansToDegrees * MathUtils.atan2(worldDY, worldDX);
        combat.deflectAngle = worldAngle;

        // In deflect stance or attacking, aim overrides facing directly towards the mouse cursor
        if (combat.state == CombatStateComponent.State.DEFLECTING
            || combat.state == CombatStateComponent.State.ATTACKING
            || combat.state == CombatStateComponent.State.HEAVY_ATTACKING) {
            direction.setFromScreenAngle(screenAngle);
            direction.angleDegrees = worldAngle;
        }

        // 2. Dash Pip Recharge
        if (playerComp.dashPips < playerComp.maxDashPips) {
            playerComp.dashRechargeTimer += deltaTime;
            if (playerComp.dashRechargeTimer >= playerComp.dashRechargeDelay) {
                playerComp.dashPips++;
                playerComp.dashRechargeTimer = 0f;
            }
        }

        // 3. Phase Dash (Spacebar)
        if (Gdx.input.isKeyJustPressed(Keys.SPACE) && combat.canDash() && playerComp.dashPips > 0) {
            playerComp.dashPips--;
            combat.transitionTo(CombatStateComponent.State.DASHING);
            combat.dashTimer = 0.25f; // 15 frames @ 60fps
            combat.iFrameTimer = 0.25f;

            // Dash direction follows WASD if moving, else aim direction
            float dashAngle = inputDir.isZero() ? worldAngle : (MathUtils.radiansToDegrees * MathUtils.atan2(worldMoveDir.y, worldMoveDir.x));
            float speed = 12.0f;
            float vx = MathUtils.cosDeg(dashAngle) * speed;
            float vy = MathUtils.sinDeg(dashAngle) * speed;

            if (bodyComp != null && bodyComp.body != null) {
                bodyComp.body.setLinearVelocity(vx, vy);
            }

            if (listener != null) {
                listener.onPlayerDash(player, transform.position.x, transform.position.y, dashAngle);
            }
            return;
        }

        // 4. Deflection Stance (Left Shift or Q key)
        boolean deflectPressed = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.Q);
        if (deflectPressed && combat.canDeflect()) {
            if (combat.state != CombatStateComponent.State.DEFLECTING) {
                combat.transitionTo(CombatStateComponent.State.DEFLECTING);
            }
            combat.isDeflecting = true;
            direction.setFromScreenAngle(screenAngle);
            direction.angleDegrees = worldAngle;
            if (bodyComp != null && bodyComp.body != null) {
                bodyComp.body.setLinearVelocity(0f, 0f);
            }
            return;
        } else if (combat.state == CombatStateComponent.State.DEFLECTING && !deflectPressed) {
            combat.transitionTo(CombatStateComponent.State.IDLE);
            combat.isDeflecting = false;
        }

        // 5. Secondary / Heavy Attack (Right Mouse Button)
        if (Gdx.input.isButtonJustPressed(Buttons.RIGHT) && combat.canHeavyAttack()) {
            direction.setFromScreenAngle(screenAngle);
            direction.angleDegrees = worldAngle;

            combat.transitionTo(CombatStateComponent.State.HEAVY_ATTACKING);
            combat.comboStep = 3;
            combat.stateTimer = 0f;

            // Powerful forward lunge
            float heavyLunge = 5.5f;
            float lx = MathUtils.cosDeg(worldAngle) * heavyLunge;
            float ly = MathUtils.sinDeg(worldAngle) * heavyLunge;
            if (bodyComp != null && bodyComp.body != null) {
                bodyComp.body.setLinearVelocity(lx, ly);
            }

            if (listener != null) {
                listener.onPlayerHeavyAttack(player, worldAngle, screenAngle);
            }
            return;
        }

        // 6. Primary Attack Combo (Left Mouse Button)
        if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
            if (combat.canAttack()) {
                direction.setFromScreenAngle(screenAngle);
                direction.angleDegrees = worldAngle;

                if (combat.state != CombatStateComponent.State.ATTACKING) {
                    combat.transitionTo(CombatStateComponent.State.ATTACKING);
                    combat.comboStep = 1;
                } else {
                    combat.comboStep = (combat.comboStep % 3) + 1;
                }
                combat.stateTimer = 0f;
                combat.comboWindowOpen = false;
                combat.comboResetTimer = 0.65f;

                // Stop linear velocity and lunge forward towards mouse cursor
                float lungeSpeed = (combat.comboStep == 3) ? 5.8f : (combat.comboStep == 2 ? 4.2f : 3.5f);
                float lx = MathUtils.cosDeg(worldAngle) * lungeSpeed;
                float ly = MathUtils.sinDeg(worldAngle) * lungeSpeed;
                if (bodyComp != null && bodyComp.body != null) {
                    bodyComp.body.setLinearVelocity(lx, ly);
                }

                if (listener != null) {
                    listener.onPlayerAttack(player, combat.comboStep, worldAngle, screenAngle);
                }
                return;
            } else if (combat.state == CombatStateComponent.State.ATTACKING || combat.state == CombatStateComponent.State.HEAVY_ATTACKING) {
                combat.bufferedNextAttack = true;
            }
        }

        // 6. Camera-Aligned WASD Movement
        inputDir.setZero();
        if (Gdx.input.isKeyPressed(Keys.W)) inputDir.y += 1f;
        if (Gdx.input.isKeyPressed(Keys.S)) inputDir.y -= 1f;
        if (Gdx.input.isKeyPressed(Keys.A)) inputDir.x -= 1f;
        if (Gdx.input.isKeyPressed(Keys.D)) inputDir.x += 1f;

        if (combat.canMove()) {
            if (!inputDir.isZero()) {
                inputDir.nor();
                // Map screen directions to isometric world ground directions:
                // Screen Up (0, 1) -> World (+1, +1)
                // Screen Right (1, 0) -> World (+1, -1)
                worldMoveDir.x = inputDir.y + inputDir.x;
                worldMoveDir.y = inputDir.y - inputDir.x;
                worldMoveDir.nor();

                float speed = 5.2f;
                if (bodyComp != null && bodyComp.body != null) {
                    bodyComp.body.setLinearVelocity(worldMoveDir.x * speed, worldMoveDir.y * speed);
                }

                combat.transitionTo(CombatStateComponent.State.SPRINT);
                direction.setFromWorldVector(worldMoveDir);
            } else {
                if (bodyComp != null && bodyComp.body != null) {
                    bodyComp.body.setLinearVelocity(0f, 0f);
                }
                combat.transitionTo(CombatStateComponent.State.IDLE);
            }
        }
    }
}
